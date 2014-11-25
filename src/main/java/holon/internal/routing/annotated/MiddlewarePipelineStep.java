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

import holon.api.middleware.Pipeline;
import holon.spi.RequestContext;

/**
 * This is.. very complicated, and I'm not happy with it at all. I wanted a working implementation to weed out
 * implementation kinks, and this is still that first spike. Will need to revisit this.
 *
 *
 * Middleware is part of the endpoint calling pipeline, and each layer has exactly the same basic powers as endpoints
 * do - they can access global and request-specific components through dependency injection, and they can reply to
 * requests.
 *
 * Middleware has two special powers: They can forward requests up the middleware pipeline and they can inject new
 * request-specific components that become available for dependency injection to components further up the chain.
 *
 * This class represents one step in the pipeline, associated with one middleware handler. It implements the API that
 * the middleware handler can use to interact with its special powers.
 */
public class MiddlewarePipelineStep implements Pipeline, Consumer<RequestContext>
{
    private final Consumer<RequestContext> nextInChain;
    private final BiConsumer<RequestContext, Pipeline> middleware;
    protected final MiddlewareContext ctx;

    private RequestContext request;

    public MiddlewarePipelineStep( Consumer<RequestContext> nextInChain, BiConsumer<RequestContext, Pipeline> middleware,
                                   MiddlewareContext ctx )
    {
        this.nextInChain = nextInChain;
        this.middleware = middleware;
        this.ctx = ctx;
    }

    @Override
    public <T> void satisfyDependency( Class<T> cls, T component )
    {
        ctx.satisfy( cls, component );
    }

    @Override
    public void call()
    {
        nextInChain.accept( request );
    }

    @Override
    public void call( RequestContext req )
    {
        nextInChain.accept( req );
    }

    @Override
    public void accept( RequestContext request )
    {
        this.request = request;
        middleware.accept( request, this );
    }
}
