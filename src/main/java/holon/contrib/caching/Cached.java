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


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import holon.api.middleware.MiddlewareAnnotation;

@Retention(RetentionPolicy.RUNTIME)
@MiddlewareAnnotation(CachingMiddleware.class)
public @interface Cached
{
    /**
     * Key for the cached response, defaults to the route path. This may be used, depending on caching backend,
     * to manipulate cached entities, such as invalidating cache entries before the timeout.
     */
    String cacheKey() default "";

    /** Time, in seconds, to cache responses */
    int time() default 120;
}
