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

import holon.api.http.Content;
import holon.api.http.FormParam;
import holon.api.http.GET;
import holon.api.http.POST;
import holon.api.http.Request;
import holon.contrib.http.StringContent;
import holon.contrib.session.Session;
import holon.contrib.session.SimpleSessionMiddleware;
import holon.util.HTTP;
import holon.util.HolonRule;
import org.junit.Rule;
import org.junit.Test;

import static holon.api.http.Status.Code.OK;
import static holon.util.HTTP.form;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class SessionIntegrationTest
{
    @Rule
    public HolonRule holon = new HolonRule(new Class[]{Endpoint.class}, new Class[]{SimpleSessionMiddleware.class});

    public static class Endpoint
    {
        private final Content someContent = new StringContent("Hello, world!");

        @POST("/")
        public void set( Request req, @FormParam("key") String value, Session session )
        {
            session.set("myKey", value);
            req.respond( OK, someContent );
        }

        @GET("/")
        public void get( Request req, Session session )
        {
            Object value = session.get( "myKey" );
            req.respond( OK, someContent, value );
        }
    }

    @Test
    public void shouldSession() throws Exception
    {
        // Given
        HTTP.HttpExchange http = HTTP.exchange();
        http.POST( holon.httpUrl() + "/", form( "key", "bananaphone!" ));

        // When
        HTTP.Response response = http.GET( holon.httpUrl() + "/" );

        // Then
        assertThat(response.status(), equalTo(200));
        assertThat(response.contentAsString(), equalTo("bananaphone!"));
    }
}
