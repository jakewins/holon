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
package holon.internal.http.netty;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;
import holon.Holon;
import holon.api.config.Config;
import holon.api.logging.Logging;
import holon.internal.http.netty.work.NettyWorkEvent;
import holon.internal.http.netty.work.NettyWorkHandler;
import holon.spi.HolonEngine;
import holon.spi.Route;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class NettyEngine implements HolonEngine
{
    private final Logging.Logger logger;
    private final int httpPort;
    private final int workers;

    private volatile boolean running = false;
    private ExecutorService executor;
    private Disruptor<NettyWorkEvent> disruptor;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyEngine( Config config, Logging.Logger logger )
    {
        this.logger = logger;
        this.workers = config.get(Holon.Configuration.workers);
        this.httpPort = config.get( Holon.Configuration.http_port );
    }

    @Override
    public void serve( Supplier<Iterable<Route>> routes )
    {
        running = true;
        executor = Executors.newCachedThreadPool();

        disruptor = new Disruptor<>( NettyWorkEvent::new, 1024, executor );
        disruptor.handleEventsWithWorkerPool( createWorkers( workers, routes ) );
        disruptor.start();

        RingBuffer<NettyWorkEvent> ringBuffer = disruptor.getRingBuffer();

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group( bossGroup, workerGroup );
        b.channel( NioServerSocketChannel.class );
        b.childHandler(new Initializer(ringBuffer));

        b.bind(httpPort);

    }

    private WorkHandler<NettyWorkEvent>[] createWorkers( int count, Supplier<Iterable<Route>> routes )
    {
        WorkHandler[] handlers = new WorkHandler[count];
        for ( int i = 0; i < count; i++ )
        {
            handlers[i] = new NettyWorkHandler(routes);
        }
        return handlers;
    }

    @Override
    public void shutdown()
    {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        if(disruptor != null)
        {
            disruptor.shutdown();
            disruptor = null;
        }
        if( executor != null)
        {
            executor.shutdown();
            executor = null;
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

    private final static class Initializer extends ChannelInitializer<SocketChannel>
    {
        private final RingBuffer<NettyWorkEvent> ringBuffer;

        public Initializer( RingBuffer<NettyWorkEvent> ringBuffer )
        {
            this.ringBuffer = ringBuffer;
        }

        @Override
        public void initChannel( SocketChannel ch )
        {
            ChannelPipeline pipeline = ch.pipeline();

            pipeline.addLast( new HttpRequestDecoder() );
            pipeline.addLast( new HttpResponseEncoder() );

            pipeline.addLast( new HttpContentCompressor() );

            pipeline.addLast( new NettyServerHandler(ringBuffer) );
        }
    }
}
