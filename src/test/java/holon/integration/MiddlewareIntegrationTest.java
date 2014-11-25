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
package holon.integration;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicBoolean;

import holon.api.http.GET;
import holon.api.http.Request;
import holon.api.http.Status;
import holon.api.middleware.MiddlewareAnnotation;
import holon.api.middleware.MiddlewareHandler;
import holon.api.middleware.Pipeline;
import holon.util.HTTP;
import holon.util.HolonRule;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class MiddlewareIntegrationTest
{
    @Rule
    public HolonRule holon = new HolonRule(new Class[]{Endpoint.class}, new Class[]{GlobalMiddleware.class});

    private static AtomicBoolean middlewareCalled = new AtomicBoolean(false);
    private static AtomicBoolean globalMiddlewareCalled = new AtomicBoolean(false);

    /**
     * This defines an annotation to use on endpoint methods, indicating the middleware should be applied.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @MiddlewareAnnotation(AnnotationBasedMiddleware.class)
    public @interface DoApplyMiddlewareHere
    {
        String value() default "";
    }

    /**
     * This is the middleware class itself.
     */
    public static class GlobalMiddleware
    {
        @MiddlewareHandler
        public void handle( Pipeline pipeline )
        {
            globalMiddlewareCalled.set( true );
            pipeline.satisfyDependency( GlobalMiddleware.class, this );
            pipeline.call();
            globalMiddlewareCalled.set( false );
        }
    }

    /**
     * This is the middleware class itself.
     */
    public static class AnnotationBasedMiddleware
    {
        @MiddlewareHandler
        public void handle( Pipeline pipeline, GlobalMiddleware injectedFromGlobal )
        {
            assertTrue(globalMiddlewareCalled.get());
            assertNotNull( injectedFromGlobal );
            middlewareCalled.set( true );
            pipeline.satisfyDependency( AnnotationBasedMiddleware.class, this );
            pipeline.call();
            middlewareCalled.set( false );
        }
    }

    /**
     * And this is an endpoint that uses the middleware.
     */
    public static class Endpoint
    {
        @GET("/")
        @DoApplyMiddlewareHere
        public void handle( Request req, GlobalMiddleware injectedFromGlobal, AnnotationBasedMiddleware injectedFromAnnotation )
        {
            assertTrue(globalMiddlewareCalled.get());
            assertTrue( middlewareCalled.get() );
            assertNotNull( injectedFromGlobal );
            assertNotNull( injectedFromAnnotation );
            req.respond( Status.Code.OK );
        }
    }

    @Test
    public void shouldIncludeAnnotationBasedMiddleware() throws Exception
    {
        // When
        HTTP.Response response = HTTP.GET( holon.httpUrl() );

        // Then
        assertThat(response.status(), equalTo(200));
    }
}
