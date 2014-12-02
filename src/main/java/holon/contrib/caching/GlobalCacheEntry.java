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

import holon.api.http.Status;
import holon.util.Digest;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.unmodifiableMap;

public class GlobalCacheEntry
{
    private static final byte STATE_LOADING = 0x0;
    private static final byte STATE_LOADED = 0x1;
    private static final byte STATE_EVICTED = 0x2;

    private final Set<LocalCacheEntry> backReferences = new HashSet<>();

    private Status status;
    private Map<String,String> headers;
    private Path cacheFilePath;
    private String etag;
    private String path;

    private volatile byte state = 0;

    public GlobalCacheEntry populate( String path, Status status, Map<String,String> headers,
            Path cacheFilePath )
            throws IOException
    {
        this.status = status;
        this.path = path;
        this.headers = unmodifiableMap( headers );
        this.cacheFilePath = cacheFilePath;
        if(cacheFilePath != null)
        {
            try(FileChannel channel = newChannel())
            {
                this.etag = Digest.md5( channel );
            }
        }
        else
        {
            this.etag = Digest.md5( "" );
        }
        return this;
    }

    public Status status()
    {
        return status;
    }

    public Map<String,String> headers()
    {
        return headers;
    }

    public FileChannel newChannel() throws IOException
    {
        if(cacheFilePath != null)
        {
            return FileChannel.open( cacheFilePath, StandardOpenOption.READ );
        }
        return null;
    }

    public String etag()
    {
        return etag;
    }

    public synchronized boolean register( LocalCacheEntry entry )
    {
        if(state == STATE_EVICTED)
        {
            return false;
        }
        backReferences.add( entry );
        return state == STATE_LOADED;
    }

    public synchronized void unregiser( LocalCacheEntry entry ) throws IOException
    {
        int initialReferences = backReferences.size();
        backReferences.remove( entry );
        if(backReferences.size() == 0 && initialReferences > 0)
        {
            if(cacheFilePath != null)
            {
                Files.delete( cacheFilePath );
            }
        }
    }

    public synchronized void requestEviction()
    {
        state = STATE_EVICTED;
        for ( LocalCacheEntry entry : backReferences )
        {
            entry.requestEviction();
        }
        backReferences.clear();
    }


    public synchronized void evict() throws IOException
    {
        state = STATE_EVICTED;
        for ( LocalCacheEntry entry : backReferences )
        {
            entry.evict();
        }
        backReferences.clear();
    }

    public boolean awaitLoaded()
    {
        long timeout = System.currentTimeMillis() + 1000 * 60;
        while(state != STATE_LOADED)
        {
            if(timeout < System.currentTimeMillis() || state == STATE_EVICTED)
            {
                return false;
            }
        }
        return true;
    }

    public void markAsLoaded()
    {
        if(state == STATE_LOADING)
        {
            state = STATE_LOADED;
        }
    }

    public String path()
    {
        return path;
    }
}
