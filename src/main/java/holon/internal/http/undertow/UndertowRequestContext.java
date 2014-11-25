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
package holon.internal.http.undertow;

import holon.api.http.Content;
import holon.api.http.Cookie;
import holon.api.http.Cookies;
import holon.api.http.Request;
import holon.api.http.Status;
import holon.api.http.UploadedFile;
import holon.internal.http.common.StandardCookie;
import holon.internal.routing.path.Path;
import holon.spi.RequestContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.form.MultiPartParserDefinition;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static holon.internal.routing.HttpMethod.Standard.POST;
import static holon.internal.routing.HttpMethod.Standard.PUT;
import static java.util.Collections.emptyMap;

public class UndertowRequestContext implements RequestContext
{
    public static final HttpString CONTENT_TYPE = HttpString.tryFromString( "Content-type" );
    private final UndertowOutput output = new UndertowOutput();
    private final UndertowCookies requestCookies = new UndertowCookies();
    private final FormParserFactory formParserFactory = FormParserFactory.builder().addParser( new MultiPartParserDefinition() ).build();

    private boolean contentTypeOverridden = false;

    private final Map<String, String> responseHeaders = new HashMap<>();
    private final Map<String, Cookie> responseCookies = new HashMap<>();
    private final Set<String> discardCookies = new HashSet<>();

    private HttpServerExchange exchange;
    private Path path;

    /** Initialization called by the Undertow Engine. */
    public final UndertowRequestContext initialize( HttpServerExchange exchange )
    {
        this.exchange = exchange;
        this.contentTypeOverridden = false;
        responseHeaders.clear();
        responseCookies.clear();
        discardCookies.clear();
        return this;
    }

    /** Initialization called by the Holon stack */
    @Override
    public final RequestContext initialize( Path path )
    {
        this.path = path;
        return this;
    }

    @Override
    public void respond( Status status )
    {
        respond( status, null, null );
    }

    @Override
    public void respond( Status status, Content content )
    {
        respond( status, content, null );
    }

    @Override
    public void respond( Status status, Content content, Object context )
    {
        try
        {
            exchange.setResponseCode( status.code() );

            renderHeaders(contentType(content, context));
            renderCookies();
            renderContent( content, context );
        }
        catch(IOException e)
        {
            e.printStackTrace();//TODO
        }
    }

    @Override
    public Request addCookie( String name, String value )
    {
        return addCookie( name, value, null, null, -1, false, false );
    }

    @Override
    public Request addCookie( String name, String value, String path, String domain, int maxAge, boolean secure,
                              boolean httpOnly )
    {
        responseCookies.put( name, new StandardCookie( name, value, domain, path, maxAge, secure, httpOnly ));
        return this;
    }

    @Override
    public Request discardCookie( String name )
    {
        discardCookies.add( name );
        return this;
    }

    @Override
    public Request addHeader( String header, String value )
    {
        if(header.equalsIgnoreCase( "Content-type" ))
        {
            contentTypeOverridden = true;
        }
        responseHeaders.put( header, value );
        return this;
    }

    @Override
    public Path path()
    {
        return path;
    }

    @Override
    public Map<String, Object> formData() throws IOException
    {
        if(exchange.getRequestMethod().equalToString( POST.name() )
            || exchange.getRequestMethod().equalToString( PUT.name() ))
        {
            exchange.startBlocking();

            FormDataParser parser = formParserFactory.createParser( exchange );
            FormData form = parser.parseBlocking();

            // For now, just create a map. In the future, make this more elaborate - ergo make this code aware of
            // which specific arguments we need and pull only those out.
            Map<String, Object> formData = new HashMap<>();
            for ( String key : form )
            {
                FormData.FormValue value = form.getFirst( key );
                if(value.isFile())
                {
                    formData.put( key, new UploadedFile()
                    {
                        @Override
                        public String fileName()
                        {
                            return value.getFileName();
                        }

                        @Override
                        public String contentType()
                        {
                            return value.getHeaders().get( "Content-type" ).getFirst();
                        }

                        @Override
                        public File file()
                        {
                            return value.getFile();
                        }
                    });
                }
                else
                {
                    formData.put( key, value.getValue() );
                }
            }
            return formData;
        }
        return emptyMap();
    }

    @Override
    public Map<String, Iterable<String>> queryParams()
    {
        return (Map)exchange.getQueryParameters();
    }

    @Override
    public Cookies cookies()
    {
        return requestCookies.initialize( exchange );
    }

    private void renderCookies()
    {
        for ( Cookie cookie : responseCookies.values() )
        {
            // TODO set other cookie fields
            exchange.setResponseCookie( new CookieImpl( cookie.name(), cookie.value() ) );
        }
        for ( String name : discardCookies )
        {
            exchange.setResponseCookie(new CookieImpl( name, "" ).setMaxAge( 0 ));
        }
    }

    private void renderContent( Content content, Object context ) throws IOException
    {
        if(content != null)
        {
            content.render( output.initialize( exchange ), context );
        }
    }

    private void renderHeaders(String defaultContentType)
    {
        HeaderMap headers = exchange.getResponseHeaders();
        for ( Map.Entry<String, String> header : responseHeaders.entrySet() )
        {
            headers.add( HttpString.tryFromString( header.getKey() ), header.getValue() );
        }
        if(!contentTypeOverridden)
        {
            headers.add( CONTENT_TYPE, defaultContentType);
        }
    }

    private String contentType( Content content, Object ctx )
    {
        if(content != null)
        {
            return content.contentType(ctx);
        }
        else
        {
            return "text/html";
        }
    }
}
