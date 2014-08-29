package holon.internal.http.netty;

import holon.api.logging.Logging;
import holon.internal.routing.basic.BasicRouter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class Netty5Initializer extends ChannelInitializer<SocketChannel>
{
    private final BasicRouter router;
    private final Logging.Logger log;

    public Netty5Initializer( BasicRouter router, Logging.Logger requestLog )
    {
        this.router = router;
        this.log = requestLog;
    }

    @Override
    protected void initChannel( SocketChannel socketChannel ) throws Exception
    {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast(new HttpRequestDecoder());
        pipeline.addLast(new HttpResponseEncoder());

        // Remove the following line if you don't want automatic content compression.
        pipeline.addLast(new HttpContentCompressor());
        pipeline.addLast(new Netty5Handler(router, log));
    }
}
