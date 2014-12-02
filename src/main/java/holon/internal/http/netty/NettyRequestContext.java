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
package holon.internal.http.netty;

import holon.api.exception.HolonException;
import holon.api.http.Content;
import holon.api.http.Cookie;
import holon.api.http.Cookies;
import holon.api.http.Request;
import holon.api.http.RequestHeaders;
import holon.api.http.Status;
import holon.internal.http.common.StandardCookie;
import holon.internal.routing.path.Path;
import holon.spi.RequestContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieEncoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.COOKIE;

public class NettyRequestContext implements RequestContext
{
    private final NettyCookies cookies = new NettyCookies();
    private final NettyOutput output = new NettyOutput();
    private final NettyRequestHeaders headers = new NettyRequestHeaders();

    private final Map<String, String> responseHeaders = new HashMap<>();
    private final Map<String,holon.api.http.Cookie> responseCookies = new HashMap<>();
    private final Set<String> discardCookies = new HashSet<>();

    private Path path;
    private HttpRequest request;
    private Channel channel;
    private Map<String,Object> formData;
    private boolean contentTypeOverridden;
    private boolean cookiesDecoded = false;
    private Map<String,List<String>> queryParams;

    /** Initialization called by the Holon stack */
    @Override
    public final RequestContext initialize( Path path )
    {
        this.path = path;
        return this;
    }

    /** Initialization called by the netty engine */
    public NettyRequestContext initialize( HttpRequest request, Channel channel, Map<String, Object> formData )
    {
        this.request = request;
        this.channel = channel;
        this.formData = formData;
        this.cookiesDecoded = false;
        this.contentTypeOverridden = false;
        this.queryParams = null;
        this.headers.initialize( request.headers() );
        responseHeaders.clear();
        responseCookies.clear();
        discardCookies.clear();
        return this;
    }

    @Override
    public Path path()
    {
        return path;
    }

    @Override
    public Map<String,Object> formData() throws IOException
    {
        return formData;
    }

    @Override
    public RequestHeaders headers()
    {
        return headers;
    }

    @Override
    public Cookies cookies()
    {
        if(!cookiesDecoded)
        {
            cookiesDecoded = true;
            String value = request.headers().get( COOKIE );
            if ( value == null )
            {
                cookies.initialize(null);
            }
            else
            {
                cookies.initialize( CookieDecoder.decode( value ) );
            }
        }
        return cookies;
    }

    @Override
    public Map<String,Iterable<String>> queryParams()
    {
        if(queryParams == null)
        {
            QueryStringDecoder decoderQuery = new QueryStringDecoder( request.getUri() );
            queryParams = decoderQuery.parameters();
        }
        return (Map) queryParams;
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
        ByteBuf buffer = renderContent( content, context );

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf( status.code() ),
                buffer);

        renderHeaders( response, contentType( content, context ) );
        renderCookies( response );

        response.headers().set( CONTENT_LENGTH, buffer.readableBytes() );

        channel.writeAndFlush( response );
    }

    private ByteBuf renderContent( Content content, Object context )
    {
        try
        {
            if(content == null)
            {
                return Unpooled.EMPTY_BUFFER;
            }

            content.render( output.initialize( channel), context );
            return output.buffer();
        }
        catch ( IOException e )
        {
            throw new HolonException( "Failed to render response.", e );
        }
    }

    private void renderCookies( FullHttpResponse response )
    {
        for ( Map.Entry<String,Cookie> cookieToAdd : this.responseCookies.entrySet() )
        {
            Cookie holonCookie = cookieToAdd.getValue();
            DefaultCookie cookie = new DefaultCookie( cookieToAdd.getKey(), cookieToAdd.getValue().value() );

            cookie.setPath( holonCookie.path() == null ? "/" : holonCookie.path() );
            cookie.setSecure( holonCookie.secure() );
            cookie.setDomain( holonCookie.domain() == null ? "" : holonCookie.domain());
            cookie.setHttpOnly( holonCookie.httpOnly() );

            response.headers().add( "Set-Cookie", ServerCookieEncoder.encode( cookie ));
        }
        for ( String name : this.discardCookies )
        {
            DefaultCookie cookie = new DefaultCookie( name, "" );
            cookie.setDiscard( true );
            response.headers().add( "Set-Cookie", ServerCookieEncoder.encode( cookie ) );
        }
    }

    private void renderHeaders( FullHttpResponse response, String defaultContentType )
    {
        HttpHeaders headers = response.headers();
        for ( Map.Entry<String, String> header : responseHeaders.entrySet() )
        {
            headers.add( header.getKey(), header.getValue() );
        }
        if(!contentTypeOverridden)
        {
            headers.set( HttpHeaders.Names.CONTENT_TYPE, defaultContentType );
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

    private static final class NettyRequestHeaders implements RequestHeaders
    {
        private HttpHeaders headers;

        @Override
        public String getFirst( String name )
        {
            return headers.get( name );
        }

        public NettyRequestHeaders initialize(HttpHeaders headers)
        {
            this.headers = headers;
            return this;
        }
    }
}
