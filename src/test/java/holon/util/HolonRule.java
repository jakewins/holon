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
package holon.util;

import holon.Holon;
import holon.api.config.Config;
import holon.api.exception.HolonException;
import holon.internal.HolonFactory;
import holon.internal.config.MapConfig;
import holon.util.io.FileTools;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.IOException;

import static holon.Holon.Configuration.app_name;
import static holon.Holon.Configuration.home_dir;
import static holon.Holon.Configuration.http_port;
import static holon.util.io.Ports.findUnusedPort;
import static junit.framework.TestCase.assertTrue;

public class HolonRule extends ExternalResource
{
    private final Class<?>[] endpoints;
    private final Object[] injectables;
    private final Config config;
    private final Class<?>[] middleware;

    private Holon holon;
    private File home;

    public HolonRule( Class<?> endpointClass )
    {
        this(new Class[]{endpointClass}, new Object[]{}, new MapConfig());
    }

    public HolonRule( Class<?>[] endpointClasses, Class<?>[] middleware )
    {
        this( endpointClasses, new Object[]{}, new MapConfig(), middleware );
    }

    public HolonRule( Class<?>[] endpointClasses, Object[] injectables, Config config )
    {
        this(endpointClasses, injectables, config, new Class[0]);
    }

    public HolonRule( Class<?>[] endpointClasses, Object[] injectables, Config config, Class<?>[] middleware )
    {
        this.endpoints = endpointClasses;
        this.injectables = injectables;
        this.config = config;
        this.middleware = middleware;
    }

    @Override
    protected void before() throws Throwable
    {
        newHomeDir();

        this.config.set( http_port, "" + findUnusedPort() );
        this.config.set( app_name,  "testapp" );
        this.config.set( home_dir,  home.getAbsolutePath() );

        holon = new HolonFactory().newHolon( config, injectables, endpoints, middleware );
        holon.start();
    }

    @Override
    protected void after()
    {
        holon.stop();
        FileTools.deleteRecursively( home );
    }

    public String httpUrl()
    {
        return "http://localhost:" + config.get( http_port );
    }

    private void newHomeDir()
    {
        try
        {
            home = File.createTempFile( "holon", null );
            assertTrue( home.delete() );
            assertTrue( home.mkdirs() );
            assertTrue( new File( home, "public").mkdir() );
        }
        catch ( IOException e )
        {
            throw new HolonException("Unable to create temporary Holon home folder.", e );
        }

    }
}
