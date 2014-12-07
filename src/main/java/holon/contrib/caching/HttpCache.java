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

import holon.api.config.Setting;
import holon.api.http.Status;
import holon.api.middleware.Pipeline;
import holon.contrib.http.RecordingRequest;
import holon.internal.io.FileOutput;
import holon.spi.RequestContext;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static holon.api.config.Setting.defaultValue;
import static holon.api.config.Setting.setting;
import static holon.api.config.SettingConverters.bool;
import static io.netty.handler.codec.http.HttpHeaders.Names;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * An initial iteration on a basic http cache. This cache depends on the OS file cache to keep relevant
 * entries in RAM. It uses per-thread local caches to avoid coordination, but allows globally evicting cached
 * entries by their cache keys.
 *
 * We currently dont have a bounds for how large the cache can be (need to implement something like LRU-K) and we
 * dont handle query parameters or other mechanism to have more complex cache keys.
 *
 * Also, now that we've swapped back to netty, this could be modified to cache the full chunked and gzipped response,
 * simply delegating to the OS's transferTo.
 */
public class HttpCache
{
    public static class Configuration
    {
        public static final Setting<Boolean> cache_enabled = setting( "application.cache.enabled", bool(),
                defaultValue( "true" ) );
    }

    private final Path cacheDir;

    /**
     * Global cache entries. If there is a cache miss on the local level, the handler will check here before trying to
     * actually invoke the endpoint.
     */
    private final ConcurrentMap<String, GlobalCacheEntry> globalEntriesByPath = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, GlobalCacheEntry> globalEntriesByKey = new ConcurrentHashMap<>();

    /**
     * This is the thread local map.
     */
    private final ThreadLocal<Map<String,LocalCacheEntry>> caches = new ThreadLocal<Map<String,LocalCacheEntry>>()
    {
        @Override
        protected Map<String,LocalCacheEntry> initialValue()
        {
            return new HashMap<>();
        }
    };

    public HttpCache( Path cacheDir ) throws IOException
    {
        this.cacheDir = cacheDir;
        if(!Files.exists(cacheDir))
        {
            Files.createDirectories( cacheDir );
        }
    }

    public void evict( String cacheKey )
    {
        GlobalCacheEntry global = globalEntriesByKey.remove( cacheKey );
        if(global == null)
        {
            return;
        }
        globalEntriesByPath.remove( global.path() );
        global.requestEviction();
    }

    public void respond( RequestContext req, String cacheKey, String etag, boolean browserCacheBypass,
            Pipeline pipeline ) throws
            IOException
    {
        String cachePath = cachePath(req);

        Map<String,LocalCacheEntry> cache = caches.get();
        LocalCacheEntry cacheEntry = cache.get( cachePath );
        if ( cacheEntry == null || cacheEntry.isEvicted() )
        {
            cacheEntry = cacheMiss( req, cachePath, cacheKey, pipeline, cache );
        }

        if( cacheEntry.etagEquals( etag ) && !browserCacheBypass)
        {
            req.respond( Status.Code.NOT_MODIFIED );
        }
        else
        {
            cacheEntry.respondTo( req );
        }
    }

    private LocalCacheEntry cacheMiss( RequestContext req, String cachePath, String cacheKey, Pipeline pipeline,
            Map<String,LocalCacheEntry> cache ) throws IOException
    {
        GlobalCacheEntry global = getFromGlobalCache( req, cachePath, cacheKey, pipeline );

        if(!global.awaitLoaded())
        {
            return cacheMiss( req, cachePath, cacheKey, pipeline, cache );
        }

        LocalCacheEntry cacheEntry = new LocalCacheEntry( global );

        if(!global.register( cacheEntry ))
        {
            return cacheMiss( req, cachePath, cacheKey, pipeline, cache );
        }
        cache.put( cachePath, cacheEntry );
        return cacheEntry;
    }

    private GlobalCacheEntry getFromGlobalCache( RequestContext req, String cachePath, String cacheKey,
            Pipeline pipeline )
            throws IOException
    {
        GlobalCacheEntry global = globalEntriesByPath.get( cachePath );
        if(global == null)
        {
            global = new GlobalCacheEntry();
            GlobalCacheEntry previous = globalEntriesByPath.putIfAbsent( cachePath, global );
            if ( previous != null )
            {
                global = previous;
            }
            else
            {
                globalEntriesByKey.put( cacheKey, global );
                populateGlobalEntry( req, cachePath, cacheKey, pipeline, global );
            }
        }
        return global;
    }

    private void populateGlobalEntry( RequestContext req, String cachePath, String cacheKey, Pipeline pipeline,
            GlobalCacheEntry global )
            throws IOException
    {
        // We've won a race to populate the global cache
        boolean success = false;
        try
        {
            RecordingRequest recorder = new RecordingRequest( req );
            pipeline.call( recorder );

            Path cacheFilePath = null;
            if ( recorder.hasContent() )
            {
                cacheFilePath = newCachedFile();
                try(FileChannel ch = FileChannel.open( cacheFilePath, CREATE, WRITE, READ ))
                {
                    recorder.replay( new FileOutput( ch ) );
                    ch.force( true );
                }
            }

            global.populate(
                    cachePath,
                    recorder.recordedStatus(),
                    stripPrivateHeaders( recorder.recordedHeaders() ),
                    cacheFilePath );
            success = true;
        }
        finally
        {
            if(!success)
            {
                global.requestEviction();
                globalEntriesByKey.remove( cacheKey );
                globalEntriesByPath.remove( cachePath );
            }
            else
            {
                global.markAsLoaded();
            }
        }
    }

    public void stop()
    {
        for ( GlobalCacheEntry entry : globalEntriesByKey.values() )
        {
            try
            {
                entry.evict();
            }
            catch ( IOException e )
            {
                e.printStackTrace(); // TODO
            }
        }
    }

    private Path newCachedFile() throws IOException
    {
        return cacheDir.resolve( UUID.randomUUID().toString() );
    }

    private static final Set<String> headerWhitelist = new HashSet<>();

    {{
        headerWhitelist.add( Names.CONTENT_BASE.toLowerCase() );
        headerWhitelist.add( Names.CONTENT_ENCODING.toLowerCase() );
        headerWhitelist.add( Names.CONTENT_LANGUAGE.toLowerCase() );
        headerWhitelist.add( Names.CONTENT_LENGTH.toLowerCase() );
        headerWhitelist.add( Names.CONTENT_LOCATION.toLowerCase() );
        headerWhitelist.add( Names.CONTENT_TRANSFER_ENCODING.toLowerCase() );
        headerWhitelist.add( Names.CONTENT_MD5.toLowerCase() );
        headerWhitelist.add( Names.CONTENT_RANGE.toLowerCase() );
        headerWhitelist.add( Names.CONTENT_TYPE.toLowerCase() );
        headerWhitelist.add( Names.LOCATION.toLowerCase() );
        headerWhitelist.add( Names.SERVER.toLowerCase() );
        headerWhitelist.add( Names.TRANSFER_ENCODING.toLowerCase() );
    }}

    private Map<String, String> stripPrivateHeaders( Map<String,String> original )
    {
        Map<String, String> cachedHeaders = new HashMap<>();
        original.entrySet().stream()
            .filter( entry -> headerWhitelist.contains( entry.getKey().toLowerCase() ) )
            .forEach( entry -> cachedHeaders.put( entry.getKey(), entry.getValue() ) );
        return cachedHeaders;
    }

    private final String cachePath(RequestContext req)
    {
        return req.path().fullPath();
    }
}
