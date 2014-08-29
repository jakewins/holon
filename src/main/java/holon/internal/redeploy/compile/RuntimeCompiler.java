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
import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import holon.internal.redeploy.HolonClassLoader;

import static holon.internal.redeploy.compile.JavaSourceObject.javaSourceFromFile;
import static javax.tools.ToolProvider.getSystemJavaCompiler;

public class RuntimeCompiler
{
    /**
     * Compile a list of java source files into the specified class loader.
     * @return true if compilation succeeded
     */
    public boolean compile( File basePath, List<File> sourceFiles, HolonClassLoader targetClassLoader )
    {
        JavaCompiler compiler = getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();

        JavaFileManager fileManager = new RuntimeCompileFileManager(
                compiler.getStandardFileManager( diagnosticsCollector, null, null ), targetClassLoader );

        JavaCompiler.CompilationTask task = compiler.getTask( null, fileManager, diagnosticsCollector, null, null,
                asJavaSourceObjects( basePath, sourceFiles ) );

        Boolean compilationSucceeded = task.call();

        if ( !compilationSucceeded )
        {
            System.out.println( "Compilation failed" );
            List<Diagnostic<? extends JavaFileObject>> diagnostics = diagnosticsCollector.getDiagnostics();
            for ( Diagnostic<?> d : diagnostics )
            {
                System.out.println(d);
            }
        }

        return compilationSucceeded;
    }

    private Iterable<? extends JavaFileObject> asJavaSourceObjects( File basePath, List<File> sourceFiles )
    {
        List<JavaFileObject> sourceObjects = new ArrayList<>();
        for ( File sourceFile : sourceFiles )
        {
            sourceObjects.add( javaSourceFromFile( basePath, sourceFile ));
        }
        return sourceObjects;
    }
}
