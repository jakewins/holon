package holon.internal.http.netty;

import holon.api.config.Config;
import holon.api.config.Setting;
import holon.api.logging.Logging;
import holon.internal.http.common.FourOhFourRoute;
import holon.internal.routing.basic.BasicRouter;
import holon.spi.HolonEngine;
import holon.spi.Route;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import static holon.api.config.Setting.defaultValue;
import static holon.api.config.Setting.setting;
import static holon.api.config.SettingConverters.integer;

public class Netty5Engine implements HolonEngine
{
    private final Config config;
    private final Logging.Logger logger;

    public static class Configuration
    {
        public static final Setting<Integer> http_port = setting( "application.http.port", integer(), defaultValue( "8080" ) );
    }

    private final int httpPort;

    private Channel channel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public Netty5Engine( Config config, Logging.Logger logger )
    {
        this.config = config;
        this.logger = logger;
        this.httpPort = config.get( Configuration.http_port );
    }

    @Override
    public void serve( Iterable<Route> routes )
    {
        BasicRouter router = new BasicRouter( routes, new FourOhFourRoute() );
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try
        {
            ServerBootstrap b = new ServerBootstrap();
            b.group( bossGroup, workerGroup )
                    .channel( NioServerSocketChannel.class )
                    .childHandler( new Netty5Initializer( router, logger ) );
            channel = b.bind( httpPort ).sync().channel();
            logger.info( "Listening for requests on " + channel.localAddress().toString() );
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void shutdown()
    {
        channel.close();
        join();
    }

    @Override
    public void join()
    {
        try
        {
            channel.closeFuture().sync();
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
