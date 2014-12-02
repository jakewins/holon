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

import holon.api.http.Output;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class NettyOutput implements Output
{
    private final Charset UTF_8 = Charset.forName( "UTF-8" );

    private Channel channel;
    private ByteBuf buffer;

    @Override
    public Writer asWriter()
    {
        buffer = channel.alloc().buffer();
        return new OutputStreamWriter( new ByteBufOutputStream( buffer ), UTF_8 );
    }

    @Override
    public void write( FileChannel channel ) throws IOException
    {
        int size = (int) channel.size();
        ByteBuffer bb = ByteBuffer.allocateDirect( size );
        while(bb.position() < bb.capacity())
        {
            channel.read( bb );
        }
        bb.position(0);
        buffer = Unpooled.wrappedBuffer(bb);
    }

    public NettyOutput initialize( Channel channel )
    {
        this.channel = channel;
        this.buffer = Unpooled.EMPTY_BUFFER;
        return this;
    }

    public ByteBuf buffer()
    {
        return buffer;
    }
}
