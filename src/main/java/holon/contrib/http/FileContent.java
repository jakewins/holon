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

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import holon.api.exception.HolonException;
import holon.api.http.Content;
import holon.api.http.Output;
import holon.internal.io.ContentTypes;

public class FileContent implements Content
{
    @Override
    public String contentType(Object context)
    {
        String[] split = toFile(context).toURI().getRawPath().split( "\\." );
        return ContentTypes.contentTypeForSuffix( split[split.length - 1] );
    }

    @Override
    public void render( Output out, Object context ) throws IOException
    {
        File file = toFile( context );
        try(FileChannel open = FileChannel.open( file.toPath(), StandardOpenOption.READ ))
        {
            out.write( open );
        }
    }

    private File toFile( Object context )
    {
        File file;
        if(context instanceof File)
        {
            file = (File) context;
        }
        else
        {
            throw new HolonException( "FileContent needs a single file as its context." );
        }
        return file;
    }
}
