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
package holon.api.middleware;

import holon.api.http.Request;
import holon.spi.RequestContext;

public interface Pipeline
{
    /**
     * Satisfy an injectable dependency for middlewares and the endpoint that are after us in the pipeline, this allows
     * setting request-specific injectable components through your middleware.
     */
    <T> void satisfyDependency( Class<T> cls, T component );

    /**
     * Forward the call to the next step in the pipeline.
     */
    void call();

    /**
     * Forward the call to the next step in the pipeline - but override the request subsequent pipeline steps get.
     * This can be used to intercept the response, which you would do by sending in a Request object that wraps the
     * request you got, but overrides the {@link Request#respond(holon.api.http.Status, holon.api.http.Content)} method.
     */
    void call(RequestContext req);
}
