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
package holon.internal.io;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import holon.api.http.Output;

import static java.nio.channels.Channels.newOutputStream;

public class FileOutput implements Output
{
    private final FileChannel ch;

    public FileOutput( FileChannel ch )
    {
        this.ch = ch;
    }

    @Override
    public Writer asWriter()
    {
        return new OutputStreamWriter( newOutputStream( ch ), Charset.forName( "UTF-8" ) );
    }

    @Override
    public void write( FileChannel channel ) throws IOException
    {
        channel.transferTo( 0, channel.size(), ch );
    }
}
