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
package holon;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import static java.util.Arrays.asList;

public class CompileTest
{

    static class JavaObjectFromString extends SimpleJavaFileObject
    {
        private final String contents;

        public JavaObjectFromString( String className, String contents )
        {
            super( URI.create( className ), Kind.SOURCE );
            this.contents = contents;
        }

        public CharSequence getCharContent( boolean ignoreEncodingErrors )
        {
            return contents;
        }
    }

    static class HackClassLoader extends ClassLoader
    {
        public HackClassLoader(ClassLoader parentClassLoader)
        {
            super(parentClassLoader);
        }

        public void define( String className, byte[] byteCodes )
        {
            defineClass( className, byteCodes, 0, byteCodes.length );
        }
    }

    static class HackJavaFileObject extends SimpleJavaFileObject
    {
        private final URI uri;
        private final HackClassLoader targetClassLoader;

        protected HackJavaFileObject( URI uri, Kind kind, HackClassLoader targetClassLoader )
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

    static class HackFileManager extends ForwardingJavaFileManager<JavaFileManager>
    {
        private final HackClassLoader classLoader;

        protected HackFileManager( JavaFileManager fileManager, HackClassLoader classLoader )
        {
            super( fileManager );
            this.classLoader = classLoader;
        }

        @Override
        public JavaFileObject getJavaFileForOutput( Location location, String className, JavaFileObject.Kind kind, FileObject sibling ) throws IOException
        {
            return new HackJavaFileObject( URI.create( className ), kind, classLoader );
        }
    }

    public static void main( String[] args ) throws Exception
    {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
        HackClassLoader classLoader = new HackClassLoader( CompileTest.class.getClassLoader() );
        JavaFileManager fileManager = new HackFileManager(compiler.getStandardFileManager( diagnosticsCollector, null, null ), classLoader );

        JavaFileObject fileToCompile = new JavaObjectFromString( "TestClass.java",
                "public class TestClass{" +
                "	public void testMethod(){" +
                "		System.out.println(" + "\"test\"" + ");" +
                "}" +
                "}" );

        CompilationTask task = compiler.getTask( null, fileManager, diagnosticsCollector, null, null, asList(
                fileToCompile ) );

        Boolean compilationSucceeded = task.call();

        if ( compilationSucceeded )
        {
            System.out.println( "Compilation has succeeded" );

            Class<?> testClass = classLoader.loadClass( "TestClass" );
            Object o = testClass.newInstance();
            Method testMethod = testClass.getDeclaredMethod( "testMethod" );

            testMethod.invoke( o );
        }
        else
        {
            System.out.println( "Compilation failed" );
            List<Diagnostic<? extends JavaFileObject>> diagnostics = diagnosticsCollector.getDiagnostics();
            for ( Diagnostic<?> d : diagnostics )
            {
                System.out.println(d);
            }
        }
    }

}
