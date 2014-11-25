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

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * This is used to mark a method in an arbitrary class as a middleware handler. A class can have multiple of these,
 * and they will then be invoked in undefined order.
 *
 * Note that middleware classes are single threaded and shared-nothing. This means the same middleware applied to two
 * separate routes will be two distinct instances. If you want global data available to be shared across middleware
 * instances, you need to provide an injectable component to contain that global state.
 */
@java.lang.annotation.Target({ElementType.METHOD})
@java.lang.annotation.Retention(RetentionPolicy.RUNTIME)
public @interface MiddlewareHandler
{

}
