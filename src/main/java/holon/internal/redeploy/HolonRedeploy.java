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
package holon.internal.redeploy;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

import holon.Holon;
import holon.api.Bootstrap;
import holon.api.config.Config;
import holon.internal.HolonFactory;
import holon.internal.redeploy.compile.RuntimeCompiler;
import holon.util.io.FileSystemWatcher;
import holon.util.scheduling.StandardScheduler;

import static holon.util.io.FileTools.findFiles;

/**
 * A development utility that recompiles user code when it changes, and restarts Holon.
 *
 * This is very much an experiment, meant to make development easier. What this does, specifically, is that it watches
 * for file changes in a specified source dir, and when changes occur, it compiles all java files in that directory
 * and restarts holon in the same JVM with the newly compiled files.
 *
 * This has a number of problems - it does not invoke user-specific tooling (code generation, annotation processing),
 * and it ties us down to Java only.
 *
 * In all likelyhood, it would be better to use an external tool for this redeployment, that would restart the whole
 * Holon process. However, this is a fun experiment, and it may pan out to work as a simple out-of-the-box recompiler
 * that remains agnostic to the users build system. It does not inherently exclude the user running without this thing
 * and with their own redeploy code.
 */
public class HolonRedeploy
{
    private final Class<? extends Bootstrap> bootstrapClass;
    private final Config config;
    private final File appFiles;
    private final RuntimeCompiler compiler;

    private volatile boolean redeployNeeded = true; // Set when files change
    private Holon holon;
    private HolonClassLoader classLoader;

    public HolonRedeploy( Class<? extends Bootstrap> bootstrapClass, Config config, File appFiles )
    {
        this.bootstrapClass = bootstrapClass;
        this.config = config;
        this.appFiles = appFiles;
        this.compiler = new RuntimeCompiler();
    }

    public void runWithAutoRecompile()
    {
        startBackgroundFileSystemWatcher();
        runRedeployLoop();
    }

    private void runRedeployLoop()
    {
        try
        {
            while(!Thread.interrupted())
            {
                redeployNeeded = false;

                recompile();

                stopHolon();

                startHolon();

                while(!redeployNeeded)
                {
                    Thread.sleep( 10 );
                }
            }
        }
        catch ( ClassNotFoundException | InstantiationException | IllegalAccessException e )
        {
            e.printStackTrace(); // TODO
        }
        catch ( InterruptedException e )
        {
            // Just exit
        }
        finally
        {
            stopHolon();
        }
    }

    private void recompile()
    {
        classLoader = new HolonClassLoader( getClass().getClassLoader() );
        compiler.compile( appFiles, findFiles( appFiles, "**.java" ), classLoader );
        Thread.currentThread().setContextClassLoader( classLoader );
    }

    private void stopHolon()
    {
        if(holon != null)
        {
            holon.stop();
        }
    }

    private void startHolon() throws ClassNotFoundException, InstantiationException,
            IllegalAccessException
    {
        Class<? extends Bootstrap> aClass = (Class<? extends Bootstrap>) classLoader.loadClass( bootstrapClass
        .getCanonicalName() );
        holon = new HolonFactory().newHolon( config, aClass.newInstance().bootstrap().toArray() );
        holon.start();
    }

    private void startBackgroundFileSystemWatcher()
    {
        StandardScheduler scheduler = new StandardScheduler();
        FileSystem fs = FileSystems.getDefault();
        FileSystemWatcher watcher = new FileSystemWatcher( fs, appFiles.toPath(), ( kind, path ) -> {
            redeployNeeded = true;
        });
        scheduler.schedule( watcher );
    }
}
