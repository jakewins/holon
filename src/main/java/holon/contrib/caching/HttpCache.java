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
import holon.api.middleware.Pipeline;
import holon.contrib.http.NoContent;
import holon.contrib.http.RecordingRequest;
import holon.internal.http.common.files.FileContent;
import holon.internal.io.FileOutput;
import holon.spi.RequestContext;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * An initial iteration on a basic http cache. This cache depends on the OS file cache to keep relevant
 * entries in RAM. It uses per-thread local caches to avoid coordination, but allows globally evicting cached
 * entries by their cache keys.
 */
public class HttpCache
{
    public static class CacheEntry
    {
        private Status status;
        private Map<String, String> headers;
        private Content content;

        private FileChannel channel;
        private final Path cacheFilePath;
        private volatile boolean evicted;
        public int count = 0;

        public CacheEntry( Status status, Map<String,String> headers, FileChannel channel, Path cacheFilePath )
        {
            this.status = status;
            this.headers = headers;
            this.channel = channel;
            this.cacheFilePath = cacheFilePath;
            this.content = channel == null ? new NoContent() : new FileContent( channel );
        }

        public synchronized void respondTo( RequestContext req )
        {
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
            evicted = true;
            if(channel != null)
            {
                channel.close();
                cacheFilePath.toFile().delete();
                channel = null;
            }
        }
    }

    private final Path cacheDir;

    /**
     * This is a bit complex.
     *
     * This cache has two competing objectives:
     *   1) We do not want coordination on the hot path and;
     *   2) We want to be able to globally evict arbitrary entries
     *
     * The way we achieve this is by having each thread run it's own cache map, but we retain a reference to all thread-local
     * maps in a global set (globalCacheReferences, below). This way, during regular operation each thread manages its
     * own caching. However, we can access these thread-local caches via the global set, and that way we can evict things.
     *
     * We use weak references below because threads may die, and we want the GC to clean up our mess if that happens.
     */
    private final Set<WeakReference<Map<String, CacheEntry>>> globalCacheReferences = Collections.newSetFromMap(new ConcurrentHashMap<>() );

    /**
     * This is the thread local map.
     */
    private final ThreadLocal<Map<String, CacheEntry>> caches = new ThreadLocal<Map<String,CacheEntry>>()
    {
        @Override
        protected Map<String,CacheEntry> initialValue()
        {
            HashMap<String, CacheEntry> cache = new HashMap<>();
            globalCacheReferences.add( new WeakReference<>(cache) );
            return cache;
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
        Iterator<WeakReference<Map<String, CacheEntry>>> iterator = globalCacheReferences.iterator();
        while(iterator.hasNext())
        {
            Map<String, CacheEntry> cache = iterator.next().get();
            if(cache == null)
            {
                iterator.remove();
                continue;
            }

            CacheEntry entry = cache.get( cacheKey );
            if(entry != null)
            {
                entry.requestEviction();
            }
        }
    }

    public void respond( RequestContext req, String cacheKey, Pipeline pipeline ) throws IOException
    {
        Map<String, CacheEntry> cache = caches.get();
        CacheEntry cacheEntry = cache.get( cacheKey );
        if ( cacheEntry == null || cacheEntry.isEvicted() )
        {
            // We won, now we need to populate the cache entry
            RecordingRequest recorder = new RecordingRequest( req );
            pipeline.call( recorder );

            FileChannel ch = null;
            Path cacheFilePath = newCachedFile();
            if ( recorder.hasContent() )
            {
                ch = FileChannel.open( cacheFilePath, CREATE_NEW, WRITE, READ );
                recorder.replay( new FileOutput( ch ) );
                ch.force( true );
            }

            cacheEntry = new CacheEntry( recorder.recordedStatus(), recorder.recordedHeaders(), ch, cacheFilePath );
            cache.put( cacheKey, cacheEntry );
        }

        cacheEntry.respondTo( req );
    }

    public void stop()
    {
        Iterator<WeakReference<Map<String, CacheEntry>>> iterator = globalCacheReferences.iterator();
        while(iterator.hasNext())
        {
            Map<String, CacheEntry> cache = iterator.next().get();
            if(cache == null)
            {
                iterator.remove();
                continue;
            }

            for ( CacheEntry cacheEntry : cache.values() )
            {
                try
                {
                    cacheEntry.evict();
                }
                catch ( IOException e )
                {
                    // TODO: Log a warning
                }
            }
        }
    }

    private Path newCachedFile() throws IOException
    {
        String name = UUID.randomUUID().toString();
        return cacheDir.resolve( name );
    }
}
