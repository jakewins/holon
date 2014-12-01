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

import holon.api.http.Cookie;
import holon.api.http.CookieParam;
import holon.api.http.Cookies;
import holon.api.http.Request;
import holon.api.middleware.MiddlewareHandler;
import holon.api.middleware.Pipeline;

/**
 * Middleware that keeps in-memory session objects using a cookie to track the user.
 */
public class SimpleSessionMiddleware
{
    private final String cookieName = "__hsess";

    private final Sessions sessions;

    public SimpleSessionMiddleware( Sessions sessions )
    {
        this.sessions = sessions;
    }

    @MiddlewareHandler
    public void handle( Pipeline pipeline, Request req, @CookieParam Cookies cookies ) throws Exception
    {
        Session session;
        Cookie cookie = cookies.get( cookieName );
        if(cookie == null)
        {
            session = sessions.newSession();
            req.addCookie( cookieName, session.key() );
        }
        else
        {
            session = sessions.getOrCreate( cookie.value() );
        }

        pipeline.satisfyDependency( Session.class, session );
        pipeline.call();
    }
}
