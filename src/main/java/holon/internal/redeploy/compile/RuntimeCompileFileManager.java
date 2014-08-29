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

import java.io.IOException;
import java.net.URI;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import holon.internal.redeploy.HolonClassLoader;

public class RuntimeCompileFileManager extends ForwardingJavaFileManager<JavaFileManager>
{
    private final HolonClassLoader classLoader;

    RuntimeCompileFileManager( JavaFileManager fileManager, HolonClassLoader classLoader )
    {
        super( fileManager );
        this.classLoader = classLoader;
    }

    @Override
    public JavaFileObject getJavaFileForOutput( Location location, String className, JavaFileObject.Kind kind, FileObject sibling ) throws IOException
    {
        return new JavaCompileObject( URI.create( className ), kind, classLoader );
    }
}
