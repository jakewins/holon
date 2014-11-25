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

import java.util.concurrent.atomic.AtomicReference;

import holon.api.http.Content;
import holon.api.http.Cookie;
import holon.api.http.CookieParam;
import holon.api.http.Cookies;
import holon.api.http.GET;
import holon.api.http.Request;
import holon.contrib.http.StringContent;
import holon.util.HTTP;
import holon.util.HolonRule;
import org.junit.Rule;
import org.junit.Test;

import static holon.api.http.Status.Code.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CookieIntegrationTest
{
    @Rule
    public HolonRule holon = new HolonRule(Endpoint.class);

    public static final AtomicReference<String> cookieVal = new AtomicReference<>();
    public static final AtomicReference<String> valFromAllCookies = new AtomicReference<>();

    public static class Endpoint
    {
        private final Content someContent = new StringContent("Hello, world!");

        @GET("/set")
        public void set( Request req )
        {
            req.addCookie( "banana", "somevalue" ).respond( OK, someContent );
        }

        @GET("/read")
        public void read( Request req, @CookieParam Cookies allCookies, @CookieParam("banana") Cookie cookie )
        {
            cookieVal.set( cookie.value() );
            valFromAllCookies.set( allCookies.get("banana").value() );
            req.respond( OK, someContent );
        }

        @GET("/discard")
        public void discard( Request req )
        {
            req.discardCookie("banana").respond( OK, someContent );
        }
    }

    @Test
    public void shouldSetCookies() throws Exception
    {
        // When
        HTTP.Response response = HTTP.GET( holon.httpUrl() + "/set" );

        // Then
        assertThat(response.status(), equalTo(200));
        assertThat(response.cookies().get( "banana" ).value(), equalTo("somevalue"));
    }

    @Test
    public void shouldDiscardCookie() throws Exception
    {
        // Given
        HTTP.HttpExchange http = HTTP.exchange();
        http.GET( holon.httpUrl() + "/set" );

        // When
        HTTP.Response response = http.GET( holon.httpUrl() + "/discard" );

        // Then
        assertThat(response.status(), equalTo(200));
        assertThat(response.cookies().size(), equalTo(0));
    }

    @Test
    public void shouldReadCookies() throws Exception
    {
        // Given
        HTTP.HttpExchange http = HTTP.exchange();
        http.GET( holon.httpUrl() + "/set" );

        // When
        HTTP.Response response = http.GET( holon.httpUrl() + "/read" );

        // Then
        assertThat(response.status(), equalTo(200));
        assertThat(cookieVal.get(), equalTo( "somevalue" ));
        assertThat(valFromAllCookies.get(), equalTo( "somevalue" ));
    }
}
