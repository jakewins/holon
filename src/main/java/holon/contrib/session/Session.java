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
package holon.contrib.session;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Sessions are semi-persisted state on the server that is associated with a user session. They must be thread-safe,
 * as multiple requests may come in using the same session simultaneously.
 */
public class Session
{
    private String key;
    private ConcurrentMap<String, Object> content = new ConcurrentHashMap<>();

    public Session( String key )
    {
        this.key = key;
    }

    public <T> T get( String key )
    {
        return (T) content.get( key );
    }

    public void set( String key, Object value )
    {
        content.put( key, value );
    }

    public String key()
    {
        return key;
    }

    void setKey( String key )
    {
        this.key = key;
    }
}
