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

import holon.api.exception.HolonException;
import holon.api.http.Cookie;
import holon.api.http.CookieParam;
import holon.api.http.Cookies;
import holon.api.middleware.Pipeline;
import holon.internal.routing.annotated.ArgInjectionStrategy;
import holon.spi.RequestContext;

public class CookieParamInjectionStrategy implements ArgInjectionStrategy
{
    @Override
    public boolean appliesTo( Class<?> type, Annotation[] annotations )
    {
        for ( Annotation annotation : annotations )
        {
            if(annotation instanceof CookieParam )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public ArgumentInjector satisfyArgument( Object[] args, int position, Class<?> type, Annotation[] annotations )
    {
        // First, check if the annotation specifies a specific attribute to use, and if it does, set things up
        // to inject the value of that attribute directly
        for ( Annotation annotation : annotations )
        {
            if(annotation instanceof CookieParam )
            {
                final String name = ((CookieParam)annotation).value();
                if(!name.equals( "" ))
                {
                    if(type == Cookie.class)
                    {
                        return new ArgumentInjector(position)
                        {
                            @Override
                            public Object generateArgument( RequestContext ctx, Pipeline pipeline ) throws IOException
                            {
                                Cookie o = ctx.cookies().get( name );
                                if(o == null)
                                {
                                    throw new HolonException( "Missing required cookie '" + name + "'" );
                                }
                                return o;
                            }
                        };
                    }

                    throw new IllegalArgumentException( "Cookie attribute needs to be a Cookie, but was " + type + "." );
                }
            }
        }

        // If a specific attribute was not asked for, the user should get the full attribute map
        if(type != Cookies.class)
        {
            throw new IllegalArgumentException( "CookieParam attribute needs to be Cookies, but was " + type + "." );
        }

        return new ArgumentInjector(position)
        {
            @Override
            public Object generateArgument( RequestContext ctx, Pipeline pipeline ) throws IOException
            {
                return ctx.cookies();
            }
        };
    }
}
