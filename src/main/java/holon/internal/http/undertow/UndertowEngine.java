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
package holon.internal.http.undertow;

import holon.Holon;
import holon.api.config.Config;
import holon.api.logging.Logging;
import holon.internal.http.common.FourOhFourRoute;
import holon.internal.routing.basic.TreeRouter;
import holon.spi.HolonEngine;
import holon.spi.Route;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;

import java.util.function.Supplier;

public class UndertowEngine implements HolonEngine
{
    private final Logging.Logger logger;
    private final int httpPort;

    private Undertow server;
    private volatile boolean running = false;

    public UndertowEngine( Config config, Logging.Logger logger )
    {
        this.logger = logger;
        this.httpPort = config.get( Holon.Configuration.http_port );
    }

    @Override
    public void serve( Supplier<Iterable<Route>> routes )
    {
        running = true;
        server = Undertow.builder()
            .addHttpListener( httpPort, "0.0.0.0" )
            .setHandler( new EncodingHandler( new ContentEncodingRepository()
                    .addEncodingHandler( "gzip", new GzipEncodingProvider(), 50 ))
                    .setNext( new HttpHandler()
                    {
                        class ThreadContext
                        {
                            UndertowRequestContext ctx = new UndertowRequestContext();
                            TreeRouter router = new TreeRouter( routes.get(), new FourOhFourRoute() );
                        }

                        private final ThreadLocal<ThreadContext> threadCtx = new ThreadLocal<ThreadContext>()
                        {
                            @Override
                            protected ThreadContext initialValue()
                            {
                                return new ThreadContext();
                            }
                        };

                        @Override
                        public void handleRequest( HttpServerExchange exchange ) throws Exception
                        {
                            ThreadContext threadContext = threadCtx.get();
                            threadContext.router.invoke( exchange.getRequestMethod().toString().toLowerCase(),
                                    exchange.getRequestPath(), threadContext.ctx.initialize( exchange ) );
                        }
                    } ) ).build();
        server.start();
    }

    @Override
    public void shutdown()
    {
        if(server != null)
        {
            server.stop();
        }
        running = false;
    }

    @Override
    public void join()
    {
        while(running)
        {
            try
            {
                Thread.sleep( 50 );
            }
            catch ( InterruptedException e )
            {
                break;
            }
        }
    }
}
