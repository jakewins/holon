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
import holon.api.http.UploadedFile;
import holon.internal.http.netty.work.NettyWorkEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NettyServerHandler extends SimpleChannelInboundHandler<HttpObject>
{
    private final RingBuffer<NettyWorkEvent> ringBuffer;

    private HttpRequest request;
    private Map<String, Object> formParams;

    private static final HttpDataFactory factory =
            new DefaultHttpDataFactory( DefaultHttpDataFactory.MINSIZE ); // Disk if size exceed

    private HttpPostRequestDecoder decoder;

    static
    {
        DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file on exit (in normal exit)
        DiskFileUpload.baseDirectory = null; // system temp directory
        DiskAttribute.deleteOnExitTemporaryFile = true; // should delete file on exit (in normal exit)
        DiskAttribute.baseDirectory = null; // system temp directory
    }

    public NettyServerHandler( RingBuffer<NettyWorkEvent> ringBuffer )
    {
        this.ringBuffer = ringBuffer;
    }


    public static void translate(NettyWorkEvent event, long sequence, HttpRequest request, Channel ch,
            Map<String, Object> formParams)
    {
        event.initialize( request, ch, formParams );
    }

    @Override
    public void channelRead0( ChannelHandlerContext ctx, HttpObject msg ) throws Exception
    {
        if ( msg instanceof HttpRequest )
        {
            HttpRequest request = this.request = (HttpRequest) msg;

            if ( request.getMethod().equals( HttpMethod.GET ) )
            {
                // No more work to do here, we will get a follow-up message which will trigger the else branch
                // at the bottom of this method.
                return;
            }

            try
            {
                formParams = new HashMap<>();
                decoder = new HttpPostRequestDecoder( factory, request );
            }
            catch ( ErrorDataDecoderException e1 )
            {
                e1.printStackTrace();
                ctx.channel().close();
                return;
            }
        }

        // check if the decoder was constructed before
        // if not it handles the form get
        if ( decoder != null )
        {
            if ( msg instanceof HttpContent )
            {
                // New chunk is received
                HttpContent chunk = (HttpContent) msg;
                try
                {
                    decoder.offer( chunk );
                }
                catch ( ErrorDataDecoderException e1 )
                {
                    e1.printStackTrace();
                    ctx.channel().close();
                    return;
                }
                // example of reading chunk by chunk (minimize memory usage due to Factory)
                readFormUploadChunked();
                // example of reading only if at the end
                if ( chunk instanceof LastHttpContent )
                {
                    ringBuffer.publishEvent( NettyServerHandler::translate, request, ctx.channel(), formParams );
                    reset();
                }
            }
        }
        else
        {
            ringBuffer.publishEvent( NettyServerHandler::translate, request, ctx.channel(), formParams );
        }
    }

    private void reset()
    {
        request = null;

        // destroy the decoder to release all resources
        decoder.destroy();
        formParams = Collections.emptyMap();
        decoder = null;
    }

    /**
     * Example of reading request by chunk and getting values from chunk to chunk
     */
    private void readFormUploadChunked()
    {
        try
        {
            while ( decoder.hasNext() )
            {
                InterfaceHttpData data = decoder.next();
                if ( data != null )
                {
                    try
                    {
                        // new value
                        storeFormData( data );
                    }
                    finally
                    {
                        data.release();
                    }
                }
            }
        }
        catch ( EndOfDataDecoderException e1 )
        {
            // This is fine (and a dumb way to signal that the decoder is done..)
        }
    }

    private void storeFormData( InterfaceHttpData data )
    {
        try
        {
            if ( data.getHttpDataType() == HttpDataType.Attribute )
            {
                Attribute attribute = (Attribute) data;
                    formParams.put( attribute.getName(), attribute.getValue() );
            }
            else
            {
                if ( data.getHttpDataType() == HttpDataType.FileUpload )
                {
                    FileUpload fileUpload = (FileUpload) data;
                    if ( fileUpload.isCompleted() )
                    {
                        File tempFile = File.createTempFile( "holon", "upload" );
                        fileUpload.renameTo( tempFile );
                        formParams.put( fileUpload.getName(), new UploadedFile()
                        {
                            @Override
                            public String fileName()
                            {
                                return fileUpload.getFilename();
                            }

                            @Override
                            public String contentType()
                            {
                                return fileUpload.getContentType();
                            }

                            @Override
                            public File file()
                            {
                                return tempFile;
                            }
                        });
                    }
                    else
                    {
                        System.out.println("File upload should have been completed here!");
                    }
                }
            }
        }
        catch ( IOException e1 )
        {
            // Error while reading data from File, only print name and error
            e1.printStackTrace();
            return;
        }
    }

    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception
    {
        cause.printStackTrace();
        ctx.channel().close();
    }

    @Override
    public void channelUnregistered( ChannelHandlerContext ctx ) throws Exception
    {
        if ( decoder != null )
        {
            decoder.cleanFiles();
        }
    }
}
