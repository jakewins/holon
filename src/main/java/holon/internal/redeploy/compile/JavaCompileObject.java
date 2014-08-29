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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

import holon.internal.redeploy.HolonClassLoader;

/**
 * Very much a hack, this is a java object we use to immediately trick javac into piping its compile output directly
 * into a class loader.
 */
public class JavaCompileObject extends SimpleJavaFileObject
{
    private final URI uri;
    private final HolonClassLoader targetClassLoader;

    JavaCompileObject( URI uri, Kind kind, HolonClassLoader targetClassLoader )
    {
        super( uri, kind );
        this.uri = uri;
        this.targetClassLoader = targetClassLoader;
    }

    @Override
    public OutputStream openOutputStream() throws IOException
    {
        return new ByteArrayOutputStream()
        {
            @Override
            public void close() throws IOException
            {
                targetClassLoader.define( uri.toASCIIString(), toByteArray() );
            }
        };
    }
}
