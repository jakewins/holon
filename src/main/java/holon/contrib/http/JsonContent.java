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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import holon.api.http.Content;
import holon.api.http.Output;

/**
 * Content that renders JSON structures.
 */
public class JsonContent implements Content
{
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void render( Output out, Object context ) throws IOException
    {
        JsonGenerator generator = mapper.getFactory().createGenerator( out.asWriter() );
        generator.writeObject( context );
        generator.flush();
    }

    @Override
    public String contentType(Object ctx)
    {
        return "application/json";
    }
}
