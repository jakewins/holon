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
package holon.internal.http.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import holon.api.http.Content;
import holon.api.http.Output;

public class ErrorContent implements Content
{
    @Override
    public void render( Output out, Object error ) throws IOException
    {
        Writer writer = out.asWriter();
        writer.write(
                "<html><body>" +
                        "<h1>500 Server Error</h1>" +
                        "<pre>"
        );
        ((Throwable)error).printStackTrace( new PrintWriter( writer ) );
        writer.write( "</pre></body></html>" );
        writer.flush();
    }
}
