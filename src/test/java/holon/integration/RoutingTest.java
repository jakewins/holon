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

import holon.api.http.GET;
import holon.api.http.PathParam;
import holon.api.http.Request;
import holon.util.HTTP;
import holon.util.HolonRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class RoutingTest
{
    @Rule
    public HolonRule holon = new HolonRule(new Class[]{Endpoint.class}, new Class[]{});

    public static AtomicReference<String> pathData = new AtomicReference<>();
    public static AtomicReference<String> endpointCalled = new AtomicReference<>();

    public static class Endpoint
    {
        // Test that specific paths override less specific paths

        @GET("/specific/specific")
        public void specific( Request req )
        {
            endpointCalled.set( "specific" );
        }

        @GET("/specific/{open}")
        public void lessSpecific( Request req )
        {
            endpointCalled.set( "less specific" );
        }

        // Test that two paths with overlapping dynamic segments work independently

        @GET("/sharedynamic/{id}/blah")
        public void sharedDynamic1( Request req, @PathParam("id") String data )
        {
            pathData.set( data );
        }

        @GET("/sharedynamic/{do}/bleh")
        public void sharedDynamic2( Request req, @PathParam("do") String data )
        {
            pathData.set( data );
        }
    }

    @Test
    public void shouldNotBeBotheredByRoutesWithSharedDynamicSegments() throws Exception
    {
        // When
        System.out.println(HTTP.GET( holon.httpUrl() + "/sharedynamic/helloworld/blah" ).status());

        // Then
        assertThat( pathData.get(), equalTo("helloworld" ));

        // And When
        HTTP.GET( holon.httpUrl() + "/sharedynamic/worldhello/bleh" );

        // Then
        assertThat( pathData.get(), equalTo("worldhello" ));
    }

    @Test
    public void specificTrumpsUnspecific() throws Exception
    {
        // When
        HTTP.GET( holon.httpUrl() + "/specific/specific" );

        // Then
        assertThat( endpointCalled.get(), equalTo("specific" ));

        // And When
        HTTP.GET( holon.httpUrl() + "/specific/worldhello" );

        // Then
        assertThat( endpointCalled.get(), equalTo("less specific" ));
    }
}
