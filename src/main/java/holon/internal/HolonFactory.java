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
import holon.contrib.template.mustache.MustacheTemplateEngine;
import holon.internal.di.Components;
import holon.internal.http.netty.Netty5Engine;
import holon.internal.logging.printstream.PrintStreamLogging;

public class HolonFactory
{

    public Holon newHolon(Config config, Object[] injectables)
    {
        Logging logging = new PrintStreamLogging( System.out );

        Components components = new Components();
        components.register( new MustacheTemplateEngine( config ) );

        for ( Object injectable : injectables )
        {
            components.register( injectable );
        }

        return new Holon( new Netty5Engine(config, logging.logger( "engine" )), components, config, logging );
    }

}
