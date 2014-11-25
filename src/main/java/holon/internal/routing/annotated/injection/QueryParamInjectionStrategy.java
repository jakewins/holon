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
import java.util.Map;

import holon.api.exception.HolonException;
import holon.api.http.QueryParam;
import holon.api.middleware.Pipeline;
import holon.internal.routing.annotated.ArgInjectionStrategy;
import holon.spi.RequestContext;

public class QueryParamInjectionStrategy implements ArgInjectionStrategy
{
    @Override
    public boolean appliesTo( Class<?> type, Annotation[] annotations )
    {
        for ( Annotation annotation : annotations )
        {
            if(annotation instanceof QueryParam )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public ArgInjectionStrategy.ArgumentInjector satisfyArgument( Object[] args, int position, Class<?> type, Annotation[] annotations )
    {
        // First, check if the annotation specifies a specific attribute to use, and if it does, set things up
        // to inject the value of that attribute directly
        for ( Annotation annotation : annotations )
        {
            if(annotation instanceof QueryParam )
            {
                final QueryParam paramAnnotation = (QueryParam) annotation;
                final String attribute = paramAnnotation.value();
                final String defaultValue = paramAnnotation.defaultVal().length() == 0 ? null : paramAnnotation.defaultVal();
                if(!attribute.equals( "" ))
                {
                    if(type == String.class)
                    {
                        return new ArgInjectionStrategy.ArgumentInjector(position)
                        {
                            @Override
                            public Object generateArgument( RequestContext ctx, Pipeline pipeline ) throws IOException
                            {
                                Iterable<String> o = ctx.queryParams().get( attribute );
                                if(o == null || !o.iterator().hasNext())
                                {
                                    if( defaultValue != null )
                                    {
                                        return defaultValue;
                                    }
                                    throw new HolonException( "Missing required query parameter '" + attribute + "'" );
                                }
                                return o.iterator().next();
                            }
                        };
                    }

                    throw new IllegalArgumentException( "QueryParam attribute needs to be a string, but was " + type + "." );
                }
            }
        }

        // If a specific attribute was not asked for, the user should get the full attribute map
        if(type != Map.class)
        {
            throw new IllegalArgumentException( "QueryParam attribute needs to be a map, but was " + type + "." );
        }

        return new ArgInjectionStrategy.ArgumentInjector(position)
        {
            @Override
            public Object generateArgument( RequestContext ctx, Pipeline pipeline ) throws IOException
            {
                return ctx.queryParams();
            }
        };
    }
}
