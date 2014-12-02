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
package holon.internal;

import holon.Holon;
import holon.api.config.Config;
import holon.api.logging.Logging;
import holon.contrib.session.Sessions;
import holon.contrib.template.mustache.MustacheTemplateEngine;
import holon.internal.di.Components;
import holon.internal.http.common.StaticContentRoute;
import holon.internal.http.netty.NettyEngine;
import holon.internal.logging.printstream.PrintStreamLogging;
import holon.internal.routing.discovery.ExplicitClassRouteDiscovery;
import holon.internal.routing.discovery.PackageScanRouteDiscovery;
import holon.internal.routing.discovery.RouteDiscoveryStrategy;
import holon.spi.Route;
import holon.util.scheduling.Scheduler;
import holon.util.scheduling.StandardScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static holon.Holon.Configuration.endpoint_packages;
import static java.util.Arrays.asList;

public class HolonFactory
{
    /** Create a Holon instance using routes discovered based on the configuration passed in. */
    public Holon newHolon( Config config, Object[] injectables, Class[] globalMiddleware )
    {
        Components components = createInjectableComponents( config, injectables );
        return newHolon( config, new PackageScanRouteDiscovery( components, config.get( endpoint_packages ),
                asList(globalMiddleware)));
    }

    /** Create a Holon instance using routes from explicit endpoint classes. */
    public Holon newHolon( Config config, Object[] injectables, Class[] endpointClasses, Class[] globalMiddleware )
    {
        Components components = createInjectableComponents( config, injectables );
        return newHolon( config, new ExplicitClassRouteDiscovery( "", components, endpointClasses, asList(globalMiddleware)));
    }

    /** Create a Holon instance using a custom route discovery strategy. */
    public Holon newHolon( Config config, RouteDiscoveryStrategy routeStrategy )
    {
        Logging logging = new PrintStreamLogging( System.out );
        Scheduler scheduler = new StandardScheduler();
        Supplier<Iterable<Route>> routes = loadRoutes( config, scheduler, routeStrategy );
        return new Holon( new NettyEngine(config, logging.logger( "holon.engine" )), config, logging, routes );
    }

    private Components createInjectableComponents( Config config, Object[] injectables )
    {
        Components components = new Components();
        components.register( config );

        // TODO: These two should get loaded via some sort of plugin mechanism
        components.register( new MustacheTemplateEngine( config ) );
        components.register( new Sessions() );

        for ( Object injectable : injectables )
        {
            components.register( injectable );
        }
        return components;
    }

    private Supplier<Iterable<Route>> loadRoutes( Config config, Scheduler scheduler, RouteDiscoveryStrategy routeStrategy )
    {
        return () -> {
            List<Route> routes = new ArrayList<>();
            routeStrategy.loadRoutes().forEach( routes::add );
            routes.add( new StaticContentRoute( config.get( Holon.Configuration.static_path ), scheduler ) );
            return routes;
        };
    }

}
