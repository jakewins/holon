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
package holon.internal.http.netty.work;

import com.lmax.disruptor.WorkHandler;
import holon.internal.http.common.FourOhFourRoute;
import holon.internal.http.netty.NettyRequestContext;
import holon.internal.routing.basic.TreeRouter;
import holon.spi.Route;
import io.netty.handler.codec.http.HttpRequest;

import java.util.function.Supplier;

public class NettyWorkHandler implements WorkHandler<NettyWorkEvent>
{
    private final NettyRequestContext ctx;
    private final TreeRouter router;

    public NettyWorkHandler( Supplier<Iterable<Route>> routes )
    {
        ctx = new NettyRequestContext();
        router = new TreeRouter( routes.get(), new FourOhFourRoute() );
    }

    @Override
    public void onEvent( NettyWorkEvent event ) throws Exception
    {
        HttpRequest req = event.request();
        router.invoke( req.getMethod().name().toLowerCase(), req.getUri().split( "\\?" )[0],
                ctx.initialize( req, event.channel(), event.formParams() ) );
    }
}
