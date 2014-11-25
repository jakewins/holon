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
package holon.internal.routing.annotated;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import holon.api.http.Request;
import holon.api.middleware.Pipeline;

/**
 * This is.. very complicated, and I'm not happy with it at all. I wanted a working implementation to weed out
 * implementation kinks, and this is still that first spike. Will need to revisit this.
 *
 * Middleware is part of the endpoint calling pipeline, and each layer has exactly the same basic powers as endpoints
 * do - they can access global and request-specific components through dependency injection, and they can reply to
 * requests.
 *
 * Middleware has two special powers: They can forward requests up the middleware pipeline and they can inject new
 * request-specific components that become available for dependency injection to components further up the chain.
 *
 *
 */
public class MiddlewarePipelineStep implements Pipeline, Consumer<Request>
{
    private final Consumer<Request> nextInChain;
    private final BiConsumer<Request, Pipeline> middleware;
    private final MiddlewareProvidedDependencies deps;

    private Request request;

    public MiddlewarePipelineStep( Consumer<Request> nextInChain, BiConsumer<Request, Pipeline> middleware,
                                   MiddlewareProvidedDependencies deps )
    {
        this.nextInChain = nextInChain;
        this.middleware = middleware;
        this.deps = deps;
    }

    @Override
    public void satisfyDependency( Object component )
    {
        deps.satisfy(component);
    }

    @Override
    public void call()
    {
        nextInChain.accept( request );
    }

    @Override
    public void accept( Request request )
    {
        middleware.accept( request, this );
    }
}
