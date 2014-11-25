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
package holon.internal.routing.annotated.injection;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import holon.api.middleware.Pipeline;
import holon.internal.routing.annotated.ArgInjectionStrategy;
import holon.internal.routing.annotated.MiddlewareContext;
import holon.spi.RequestContext;

/**
 * This satisfies dependency injection by injecting request-specific components provided by middleware layers.
 */
public class MiddlewareProvidedArgStrategy implements ArgInjectionStrategy
{
    private final MiddlewareContext deps;

    public MiddlewareProvidedArgStrategy( MiddlewareContext deps )
    {
        this.deps = deps;
    }

    @Override
    public boolean appliesTo( Class<?> type, Annotation[] annotations )
    {
        // We don't know at this point if this will work, so we optimistically assume that it will.
        return true;
    }

    @Override
    public ArgumentInjector satisfyArgument( Object[] args, int position, Class<?> type, Annotation[] annotations )
    {
        Supplier<Object> supplier = deps.newSupplier( type, annotations );
        return new ArgumentInjector(position)
        {
            @Override
            public Object generateArgument( RequestContext ctx, Pipeline pipeline ) throws IOException
            {
                return supplier.get();
            }
        };
    }
}
