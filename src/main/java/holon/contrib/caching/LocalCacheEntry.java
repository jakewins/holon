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
package holon.contrib.caching;

import holon.api.http.Content;
import holon.api.http.Status;
import holon.contrib.http.NoContent;
import holon.internal.http.common.files.FileContent;
import holon.spi.RequestContext;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Map;

public class LocalCacheEntry
{
    private final Status status;
    private final Map<String, String> headers;
    private final Content content;
    private final String etag;
    private final GlobalCacheEntry global;

    private FileChannel channel;
    private volatile boolean evicted;

    public LocalCacheEntry( GlobalCacheEntry global )
            throws IOException
    {
        this.global = global;
        this.status = global.status();
        this.headers = global.headers();
        this.channel = global.newChannel();
        this.content = channel == null ? new NoContent() : new FileContent( channel );
        this.etag = global.etag();
    }

    public synchronized void respondTo( RequestContext req )
    {
        req.addHeader( HttpHeaders.Names.ETAG, this.etag );
        req.addHeader( HttpHeaders.Names.CACHE_CONTROL, "public, max-age=86400" );

        for ( Map.Entry<String, String> header : headers.entrySet() )
        {
            req.addHeader( header.getKey(), header.getValue() );
        }

        req.respond( status, content );
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        evict();
    }

    public boolean isEvicted()
    {
        return evicted;
    }

    public void requestEviction()
    {
        evicted = true;
    }

    public void evict() throws IOException
    {
        if(!evicted)
        {
            evicted = true;
            if ( channel != null )
            {
                channel.close();
                channel = null;
            }
            global.unregiser( this );
        }
    }

    public boolean etagEquals( String etag )
    {
        return this.etag.equalsIgnoreCase( etag );
    }
}
