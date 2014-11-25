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
package holon.internal.di;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import holon.api.exception.HolonException;

public class DependencyInjector
{
    private final Components components;

    public DependencyInjector( Components components )
    {
        this.components = components;
    }

    public <T> T instantiate( Class<T> cls )
    {
        return instantiate( cls, Collections.emptyList() );
    }

    public <T> T instantiate( Class<T> cls, Collection<Object> additionalInjectables )
    {
        try
        {
            Components additionalComponents = new Components();
            if(additionalInjectables.size() > 0)
            {
                for ( Object additionalInjectable : additionalInjectables )
                {
                    if(additionalInjectable != null)
                    {
                        additionalComponents.register( additionalInjectable );
                    }
                }

            }
            Set<String> unusableConstructors = new HashSet<>();
            constructorLoop: for ( Constructor<?> constructor : cls.getConstructors() )
            {
                List<Object> args = new ArrayList<>();
                for ( Class<?> dependency : constructor.getParameterTypes() )
                {
                    if(additionalComponents.contains( dependency ))
                    {
                        args.add( additionalComponents.resolve( dependency ) );
                        continue;
                    }

                    if(!components.contains(dependency))
                    {
                        unusableConstructors.add( dependency.getName() );
                        continue constructorLoop;
                    }

                    args.add( components.resolve( dependency ) );
                }

                // Found a constructor we can use.
                return (T)constructor.newInstance( args.toArray() );
            }

            throw new HolonException( "Cannot find a usable constructor for '" + cls.getSimpleName()
                    + "'. Make sure there is a constructor, and that all arguments to it are registered for " +
                    "dependency injection. " + unusableConstructors, null );
        }
        catch ( InstantiationException | IllegalAccessException | InvocationTargetException e)
        {
            throw new HolonException( "Unable to instantiate route.", e);
        }
    }
}
