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
package holon.internal.http.undertow.work;

import com.lmax.disruptor.WorkHandler;
import holon.internal.http.common.FourOhFourRoute;
import holon.internal.http.undertow.UndertowRequestContext;
import holon.internal.routing.basic.TreeRouter;
import holon.spi.Route;
import io.undertow.server.HttpServerExchange;

import java.util.function.Supplier;

public class HttpWorkHandler implements WorkHandler<HttpWorkEvent>
{
    private final UndertowRequestContext ctx;
    private final TreeRouter router;

    public HttpWorkHandler( Supplier<Iterable<Route>> routes )
    {
        ctx = new UndertowRequestContext();
        router = new TreeRouter( routes.get(), new FourOhFourRoute() );
    }

    @Override
    public void onEvent( HttpWorkEvent event ) throws Exception
    {
        HttpServerExchange exchange = event.exchange();
        try
        {
            router.invoke( exchange.getRequestMethod().toString().toLowerCase(),
                    exchange.getRequestPath(), ctx.initialize( exchange ) );
        }
        finally
        {
            exchange.endExchange();
        }
    }
}
