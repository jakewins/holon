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
package holon.internal.routing.discovery;

import java.util.ArrayList;
import java.util.List;

import holon.internal.di.Components;
import holon.internal.routing.annotated.RouteCompiler;
import holon.internal.routing.annotated.RouteScanner;
import holon.spi.Route;

public class ExplicitClassRouteDiscovery implements RouteDiscoveryStrategy
{
    private final String basePath;
    private final Components components;
    private final Class[] endpointClasses;
    private final List<Class<?>> globalMiddlewareClasses;

    public ExplicitClassRouteDiscovery(String basePath, Components components, Class[] endpointClasses, List<Class<?>> globalMiddlewareClasses )
    {
        this.basePath = basePath;
        this.components = components;
        this.endpointClasses = endpointClasses;
        this.globalMiddlewareClasses = globalMiddlewareClasses;
    }

    @Override
    public Iterable<Route> loadRoutes()
    {
        List<Route> routes = new ArrayList<>();
        RouteScanner routeScanner = new RouteScanner( new RouteCompiler( components, globalMiddlewareClasses ) );
        for ( Class endpointClass : endpointClasses )
        {
            routeScanner.scan( basePath, endpointClass, routes::add );
        }
        return routes;
    }
}
