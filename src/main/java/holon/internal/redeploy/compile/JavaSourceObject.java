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
package holon.internal.redeploy.compile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;

import javax.tools.SimpleJavaFileObject;

import holon.util.io.FileTools;

/**
 * A bit of a hack, this is the {@link javax.tools.JavaFileObject} use use to contain the uncompiled java sources.
 */
public class JavaSourceObject extends SimpleJavaFileObject
{
    private final String contents;

    public static JavaSourceObject javaSourceFromFile( File basePath, File file )
    {
        try
        {
            String fileName = file.getCanonicalPath().substring( basePath.getCanonicalPath().length() + 1 );
            return new JavaSourceObject( fileName, FileTools.read( file, Charset.forName("UTF-8") ));
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Failed to read source file: " + file.getAbsolutePath(), e );
        }
    }

    public JavaSourceObject( String className, String contents )
    {
        super( URI.create( className ), Kind.SOURCE );
        this.contents = contents;
    }

    public CharSequence getCharContent( boolean ignoreEncodingErrors )
    {
        return contents;
    }
}
