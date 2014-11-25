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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import holon.api.http.Output;
import io.undertow.server.HttpServerExchange;
import org.xnio.channels.StreamSinkChannel;

public class UndertowOutput implements Output
{
    private final Charset UTF_8 = Charset.forName( "UTF-8" );
    private HttpServerExchange exchange;

    public UndertowOutput initialize( HttpServerExchange exchange )
    {
        this.exchange = exchange;
        return this;
    }

    @Override
    public Writer asWriter()
    {
        exchange.startBlocking();
        return new OutputStreamWriter( exchange.getOutputStream(), UTF_8 );
    }

    @Override
    public void write( FileChannel channel ) throws IOException
    {
        StreamSinkChannel responseChannel = exchange.getResponseChannel();
        responseChannel.transferFrom( channel, 0, channel.size() );
    }
}
