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

import java.util.Iterator;
import java.util.Map;

import holon.api.http.Cookie;
import holon.api.http.Cookies;
import holon.internal.http.common.StandardCookie;
import io.undertow.server.HttpServerExchange;

public class UndertowCookies implements Cookies
{
    private Map<String, io.undertow.server.handlers.Cookie> rawCookies;

    @Override
    public Cookie get( String name )
    {
        io.undertow.server.handlers.Cookie cookie = rawCookies.get( name );
        if(cookie != null)
        {
            return new StandardCookie( name, cookie.getValue() );
        }
        return null;
    }

    @Override
    public Iterator<Cookie> iterator()
    {
        Iterator<io.undertow.server.handlers.Cookie> iterator = rawCookies.values().iterator();
        return new Iterator<Cookie>()
        {
            @Override
            public boolean hasNext()
            {
                return iterator.hasNext();
            }

            @Override
            public Cookie next()
            {
                io.undertow.server.handlers.Cookie next = iterator.next();
                return new StandardCookie( next.getName(), next.getValue() );
            }
        };
    }

    public Cookies initialize( HttpServerExchange exchange )
    {
        rawCookies = exchange.getRequestCookies();
        return this;
    }
}
