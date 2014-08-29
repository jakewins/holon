package holon.internal.http.netty;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import holon.api.logging.Logging;
import holon.internal.routing.basic.BasicRouter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import static io.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * This is temporary, and so is somewhat messy. It's meant to be replaced once I've got a good view of what is needed
 * from the rest of the system from the HTTP connector component.
 */
public class Netty5Handler extends SimpleChannelInboundHandler<Object>
{
    private final BasicRouter router;
    private final Logging.Logger logger;

    private HttpPostRequestDecoder decoder;
    private HttpRequest req;
    private Map<String, String> postData;

    public Netty5Handler( BasicRouter router, Logging.Logger requestLogger )
    {
        this.router = router;
        this.logger = requestLogger;
    }

    @Override
    protected void messageReceived( ChannelHandlerContext ctx, Object msg ) throws Exception
    {
        try
        {
            if( decoder == null)
            {
                if ( msg instanceof HttpRequest )
                {
                    req = (HttpRequest) msg;

                    if ( is100ContinueExpected( req ) )
                    {
                        ctx.write( new DefaultFullHttpResponse( HTTP_1_1, CONTINUE ) );
                    }

                    if ( req.getMethod() == HttpMethod.POST )
                    {
                        decoder = new HttpPostRequestDecoder( new DefaultHttpDataFactory( false ), req );
                        postData = new HashMap<>();
                    }
                }

                if( msg instanceof LastHttpContent )
                {
                    router.route( req.getMethod().name(), req.getUri() ).call( new Netty5RequestContext( req, ctx,
                            logger, postData ) );
                }
            }
            else
            {
                if( msg instanceof HttpContent )
                {
                    HttpContent chunk = (HttpContent) msg;
                    decoder.offer(chunk);

                    // Read data as it becomes available, chunk by chunk.
                    readChunkByChunk(ctx);

                    if (chunk instanceof LastHttpContent ) {
                        readChunkByChunk(ctx);
                        router.route( req.getMethod().name(), req.getUri() ).call( new Netty5RequestContext( req,
                                ctx, logger, postData ) );
                        resetPostRequestDecoder();
                    }
                }
            }
        }
        catch(Exception e)
        {
            logger.error("Failed to handle request.", e);
        }
    }

    private void readChunkByChunk(ChannelHandlerContext ctx) throws IOException
    {
        try
        {
            while (decoder.hasNext()) {
                InterfaceHttpData data = decoder.next();
                if (data != null) {
                    try {
                        processChunk(ctx, data);
                    } finally {
                        data.release();
                    }
                }
            }
        } catch (HttpPostRequestDecoder.EndOfDataDecoderException e) {
            // No more data to decode, that's fine
        }
    }

    private void processChunk(ChannelHandlerContext ctx, InterfaceHttpData data) throws IOException
    {
        switch(data.getHttpDataType()) {
            case Attribute:
                Attribute attrib = (Attribute) data;
                postData.put( attrib.getName(), attrib.getValue() );
                break;
            case FileUpload:
                FileUpload fileUpload = (FileUpload) data;
                // handle upload
                break;
            default:
                break;
        }
    }

    private void resetPostRequestDecoder() {
        req = null;
        postData = null;

        // clean previous FileUpload if Any
        decoder.destroy();
        decoder = null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error( "Error in network layer.", cause );
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

}
