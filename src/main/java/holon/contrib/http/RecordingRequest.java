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
package holon.contrib.http;

import holon.api.http.Content;
import holon.api.http.Cookie;
import holon.api.http.Cookies;
import holon.api.http.Output;
import holon.api.http.Request;
import holon.api.http.RequestHeaders;
import holon.api.http.Status;
import holon.internal.http.common.StandardCookie;
import holon.internal.routing.path.Path;
import holon.spi.RequestContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Wrap an existing request, but catch all response-related calls to it and allow those to be
 * inspected.
 */
public class RecordingRequest implements RequestContext
{
    private RequestContext delegate;
    private Status status;
    private Content content;
    private Object ctx;
    private boolean contentTypeSet = false;
    private HashMap<String, Cookie> responseCookies = new HashMap<>();
    private HashMap<String, String> responseHeaders = new HashMap<>();
    private Set<String> discardCookies = new HashSet<>();

    public RecordingRequest( RequestContext delegate )
    {
        this.delegate = delegate;
    }

    @Override
    public RequestContext initialize( Path path )
    {
        return delegate.initialize( path );
    }

    @Override
    @Deprecated
    public Path path()
    {
        return delegate.path();
    }

    @Override
    @Deprecated
    public Map<String, Object> formData() throws IOException
    {
        return delegate.formData();
    }

    @Override
    @Deprecated
    public Cookies cookies()
    {
        return delegate.cookies();
    }

    @Override
    @Deprecated
    public Map<String, Iterable<String>> queryParams()
    {
        return delegate.queryParams();
    }

    @Override
    public void respond( Status status )
    {
        respond( status, null, null );
    }

    @Override
    public void respond( Status status, Content content )
    {
        respond(status, content, null);
    }

    @Override
    public void respond( Status status, Content content, Object context )
    {
        this.status = status;
        this.content = content;
        this.ctx = context;
        if(!contentTypeSet && content != null)
        {
            addHeader( "Content-type", content.contentType( ctx ) );
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
        responseCookies.put( name, new StandardCookie( name, value, domain, path, maxAge, secure, httpOnly ) );
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
        responseHeaders.put( header, value );
        return this;
    }

    @Override
    public RequestHeaders headers()
    {
        return null;
    }

    public void replay( Output output ) throws IOException
    {
        if(content != null)
        {
            content.render( output, ctx );
        }
    }

    public Map<String, String> recordedHeaders()
    {
        return responseHeaders;
    }

    public Map<String, Cookie> recordedCookies()
    {
        return responseCookies;
    }

    public Set<String> recordedDiscardCookies()
    {
        return discardCookies;
    }

    public Status recordedStatus()
    {
        return status;
    }

    public boolean hasContent()
    {
        return content != null;
    }
}
