/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package holon.util;

import holon.api.exception.HolonException;
import holon.api.http.Cookie;
import holon.internal.http.common.StandardCookie;
import holon.util.collection.Maps;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class HTTP
{
    public static HttpExchange withHeaders( String ... alternatingKeyValue )
    {
        return exchange().withHeaders( alternatingKeyValue );
    }

    public static class Response
    {
        private final HttpResponse rawResponse;
        private final HttpClientContext context;

        public Response( HttpResponse rawResponse, HttpClientContext context )
        {
            this.rawResponse = rawResponse;
            this.context = context;
        }

        public int status()
        {
            return rawResponse.getStatusLine().getStatusCode();
        }

        public Map<String, Cookie> cookies()
        {
            Map<String, Cookie> cookies = new HashMap<>();
            for ( org.apache.http.cookie.Cookie cookie : context.getCookieStore().getCookies() )
            {
                cookies.put( cookie.getName(), new StandardCookie(cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getExpiryDate()) );
            }
            return cookies;
        }

        public String header(String name)
        {
            Header header = rawResponse.getLastHeader( name );
            return header == null ? null : header.getValue();
        }

        @Override
        public String toString()
        {
            return String.format( "%d\n%s\n%s", status(), headersAsString(), contentAsString() );
        }

        private String headersAsString()
        {
            StringBuilder sb = new StringBuilder();
            HeaderIterator headers = rawResponse.headerIterator();
            while(headers.hasNext())
            {
                Header header = headers.nextHeader();
                sb.append( header.getName() ).append( ": " ).append( header.getValue() ).append( "\n" );
            }
            return sb.toString();
        }

        public String contentAsString()
        {
            if(rawResponse.getEntity() == null)
            {
                return "";
            }
            try(InputStream stream = rawResponse.getEntity().getContent())
            {
                Scanner scanner = new Scanner( stream ).useDelimiter( "\\A" );

                return scanner.hasNext() ? scanner.next() : "";
            }
            catch(IOException e)
            {
                throw new HolonException( "Failed to read http response body.", e );
            }
        }
    }

    public interface Payload
    {
        HttpEntity createEntity();
    }

    public static class HttpExchange
    {
        private final BasicCookieStore cookieStore = new BasicCookieStore();
        private final HttpClientContext context = HttpClientContext.create();
        private final Map<String, String> headers = new HashMap<>();

        private final HttpClient client = HttpClients.custom()
                .setDefaultRequestConfig( RequestConfig.custom().setCookieSpec( CookieSpecs.BEST_MATCH).build() )
                .setDefaultCookieStore( cookieStore ).build();

        public HttpExchange()
        {
            context.setCookieStore(cookieStore);
        }

        public HttpExchange withHeaders(String ... alternatingKeyValue )
        {
            for ( int i = 0; i < alternatingKeyValue.length; i+=2 )
            {
                headers.put( alternatingKeyValue[i], alternatingKeyValue[i+1] );
            }
            return this;
        }

        public Response POST( String path, Payload payload )
        {
            HttpPost post = new HttpPost( path );
            post.setEntity(payload.createEntity());
            return send( post );
        }

        public Response GET( String path )
        {
            return send(new HttpGet( path ));
        }

        private Response send( HttpUriRequest req )
        {
            try
            {
                for ( Map.Entry<String,String> header : headers.entrySet() )
                {
                    req.addHeader( header.getKey(), header.getValue() );
                }

                return new Response(client.execute(req, context), context);
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
    }

    public static Payload form( Object ... alternatingKeyValue )
    {
        return form( Maps.map(alternatingKeyValue));
    }

    public static Payload form( Map<String, Object> fields )
    {
        return () -> {
            MultipartEntity entity = new MultipartEntity();
            for ( Map.Entry<String, Object> entry : fields.entrySet() )
            {
                if(entry.getValue() instanceof File )
                {
                    entity.addPart(entry.getKey(), new FileBody( (File) entry.getValue() ));
                }
                else
                {
                    try
                    {
                        entity.addPart( entry.getKey(), new StringBody( (String) entry.getValue() ) );
                    }
                    catch ( UnsupportedEncodingException e )
                    {
                        throw new RuntimeException( e );
                    }
                }
            }
            return entity;
        };
    }

    public static HttpExchange exchange()
    {
        return new HttpExchange();
    }

    public static Response POST( String path, Payload payload )
    {
        return exchange().POST(path, payload);
    }

    public static Response GET( String path )
    {
        return exchange().GET( path );
    }


}
