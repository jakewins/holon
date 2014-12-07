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

import holon.api.config.Config;
import holon.api.http.Default;
import holon.api.http.HeaderParam;
import holon.api.http.PathParam;
import holon.api.middleware.MiddlewareHandler;
import holon.api.middleware.Pipeline;
import holon.spi.RequestContext;

import java.io.IOException;

import static io.netty.handler.codec.http.HttpHeaders.Names.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaders.Names.IF_NONE_MATCH;

public class CachingMiddleware
{
    private final HttpCache cache;
    private final String cacheKey;
    private final boolean enabled;

    public CachingMiddleware( Cached annotation, HttpCache cache, Config config )
    {
        this.cache = cache;
        this.cacheKey = annotation.cacheKey();
        this.enabled = config.get( HttpCache.Configuration.cache_enabled );
    }

    @MiddlewareHandler
    public void handle(Pipeline pipeline, RequestContext req,
            @HeaderParam(IF_NONE_MATCH) @Default("") String etag,
            @HeaderParam(CACHE_CONTROL) @Default("") String cacheControl,
            @PathParam String path) throws IOException
    {
        if(enabled)
        {
            cache.respond( req, key( path ), etag, cacheControl.equalsIgnoreCase( "no-cache" ), pipeline );
        }
        else
        {
            pipeline.call();
        }
    }

    private String key( String path )
    {
        if(cacheKey.length() > 0)
        {
            return cacheKey;
        }
        else
        {
            return path;
        }
    }
}
