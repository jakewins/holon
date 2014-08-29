package holon.internal.http.netty;

import java.io.IOException;
import java.util.Map;

import holon.api.http.Content;
import holon.api.http.Request;
import holon.api.http.Status;
import holon.api.logging.Logging;
import holon.internal.io.ByteArrayOutput;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_ENCODING;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class Netty5RequestContext implements Request
{
    private static final Content NO_CONTENT = ( a, b ) -> {};

    private final HttpRequest req;
    private final ChannelHandlerContext ctx;
    private final Logging.Logger logger;
    private final Map<String, String> postData;

    public Netty5RequestContext( HttpRequest req, ChannelHandlerContext ctx, Logging.Logger logger, Map<String,
            String> postData )
    {
        this.req = req;
        this.ctx = ctx;
        this.logger = logger;
        this.postData = postData;
    }

    @Override
    public void respond( Status status, Content content, Map<String, Object> context )
    {
        try
        {
            boolean keepAlive = isKeepAlive( req );

            ByteArrayOutput out = new ByteArrayOutput();
            content.render( context, out );

            byte[] array = out.toByteArray();
            FullHttpResponse response = new DefaultFullHttpResponse( HTTP_1_1,
                    HttpResponseStatus.valueOf( status.code() ),
                    Unpooled.wrappedBuffer( array ) );


            logger.debug( req.getMethod() + " " + req.getUri() + " -> " + status.code() );

            response.headers().set( CONTENT_TYPE, content.type() );
            response.headers().set( CONTENT_LENGTH, response.content().readableBytes() );
            response.headers().set( CONTENT_ENCODING, "UTF-8" );

            Map<String, String> headers = content.headers();
            if(headers != null)
            {
                for ( Map.Entry<String, String> header : headers.entrySet() )
                {
                    response.headers().set( header.getKey(), header.getValue() );
                }
            }

            if ( !keepAlive )
            {
                ctx.write( response ).addListener( ChannelFutureListener.CLOSE );
            }
            else
            {
                response.headers().set( CONNECTION, HttpHeaders.Values.KEEP_ALIVE );
                ctx.write( response );
            }
        }
        catch(IOException e)
        {
            logger.error( "Failed to handle request.", e );
        }
    }

    @Override
    public void respond( Status status )
    {
        respond( status, NO_CONTENT, null );
    }

    @Override
    public void respond( Status status, Content content )
    {
        respond( status, content, null );
    }

    @Override
    public String path()
    {
        return req.getUri();
    }

    @Override
    public Map<String, String> postData()
    {
        return postData;
    }
}
