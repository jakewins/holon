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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import holon.Holon;
import holon.api.http.GET;
import holon.api.http.Request;
import holon.api.http.Status;
import holon.contrib.http.StringContent;
import holon.internal.HolonFactory;
import holon.util.HTTP;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static holon.Holon.Configuration.home_dir;
import static holon.internal.config.MapConfig.config;
import static holon.util.collection.Maps.map;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CachingMiddlewareTest
{
    @Rule public TemporaryFolder testDir = new TemporaryFolder();

    private static AtomicInteger endpointCalled = new AtomicInteger(0);
    private static AtomicReference<String> simpleContent = new AtomicReference<>( "hello, world!" );
    private HttpCache httpCache;
    private Holon holon;

    /**
     * And this is an endpoint that uses the middleware.
     */
    public static class Endpoint
    {
        @GET("/")
        @Cached(time=100)
        public void noContent( Request req )
        {
            endpointCalled.incrementAndGet();
            req.respond( Status.Code.OK );
        }

        @GET("/simple")
        @Cached(time=100)
        public void simpleContent( Request req )
        {
            endpointCalled.incrementAndGet();
            req.respond( Status.Code.OK, new StringContent(simpleContent.get()) );
        }
    }

    @Before
    public void setup() throws IOException
    {
        httpCache = new HttpCache( Paths.get(testDir.newFolder().toURI()) );
        holon = new HolonFactory().newHolon( config( map( home_dir, testDir.getRoot().getAbsolutePath() ) ), new Object[]{httpCache}, new Class[]{Endpoint.class} );
        holon.start();
    }

    @Test
    public void shouldCacheReponseWithOnlyHeaderAndStatus() throws Exception
    {
        // When
        HTTP.Response response1 = HTTP.GET( holon.httpUrl() );
        HTTP.Response response2 = HTTP.GET( holon.httpUrl() );

        // Then
        assertThat(endpointCalled.get(), equalTo( 1 ));
        assertThat(response1.status(), equalTo(200));
        assertThat(response2.status(), equalTo(200));
    }

    @Test
    public void shouldCacheSimpleTextResponse() throws Exception
    {
        // When
        HTTP.Response response1 = HTTP.GET( holon.httpUrl() + "/simple" );
        HTTP.Response response2 = HTTP.GET( holon.httpUrl() + "/simple" );

        // Then
        assertThat(endpointCalled.get(), equalTo( 1 ));
        assertThat(response1.status(), equalTo(200));
        assertThat(response2.status(), equalTo(200));
    }

    @Test
    public void shouldEvictStuff() throws Exception
    {
        // Given
        HTTP.GET( holon.httpUrl() + "/simple" );
        simpleContent.set( "Something else" );

        httpCache.evict( "/simple" );

        // When
        HTTP.Response response = HTTP.GET( holon.httpUrl() + "/simple" );

        // Then
        assertThat(endpointCalled.get(), equalTo( 2 ));
        assertThat(response.status(), equalTo(200));
        assertThat(response.contentAsString(), equalTo("Something else"));
    }
}
