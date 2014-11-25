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
package holon.internal.routing.annotated;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.function.Supplier;

public class MiddlewareContext
{
    // Temporary, to be expanded to something smarter
    private final HashMap<Class<?>, Object> components = new HashMap<>();

    /**
     * Create an object that can be used later to supply a dependency that satisfies the type and annotations
     * given here.
     */
    public Supplier<Object> newSupplier( Class<?> type, Annotation[] annotations )
    {
        return () -> components.get( type );
    }

    public void satisfy( Class<?> cls, Object component )
    {
        components.put( cls, component );
    }
}
