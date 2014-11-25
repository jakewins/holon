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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Sessions
{
    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();

    public Session newSession()
    {
        Session session = new Session(UUID.randomUUID().toString());
        while( sessions.putIfAbsent( session.key(), session ) != null )
        {
            session.setKey( UUID.randomUUID().toString() );
        }
        return session;
    }

    public Session getOrCreate( String sessionKey )
    {
        Session session = sessions.get( sessionKey );
        if(session == null)
        {
            session = new Session( sessionKey );
            Session preExisting = sessions.putIfAbsent( sessionKey, session );
            if(preExisting != null)
            {
                session = preExisting;
            }
        }
        return session;
    }
}
