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
package holon.contrib.http;

import java.io.IOException;
import java.io.Writer;

import holon.api.http.Content;
import holon.api.http.Output;

public class StringContent implements Content
{
    private final String value;

    public StringContent( String value )
    {
        this.value = value;
    }

    @Override
    public String contentType(Object ctx)
    {
        return "text/plain";
    }

    @Override
    public void render( Output out, Object context ) throws IOException
    {
        Writer writer = out.asWriter();
        try
        {
            if ( context != null && context instanceof String )
            {
                writer.append( (String) context );
            }
            else
            {
                writer.append( value );
            }
        } finally
        {
            writer.flush();
        }
    }
}
