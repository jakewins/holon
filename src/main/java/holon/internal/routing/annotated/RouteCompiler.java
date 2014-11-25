package holon.internal.routing.annotated;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import holon.api.exception.HolonException;
import holon.api.http.DELETE;
import holon.api.http.GET;
import holon.api.http.HEAD;
import holon.api.http.OPTIONS;
import holon.api.http.POST;
import holon.api.http.PUT;
import holon.api.middleware.MiddlewareAnnotation;
import holon.api.middleware.MiddlewareHandler;
import holon.api.middleware.Pipeline;
import holon.internal.di.Components;
import holon.internal.di.DependencyInjector;
import holon.internal.routing.HttpMethod;
import holon.internal.routing.annotated.injection.ComponentInjectionStrategy;
import holon.internal.routing.annotated.injection.CookieParamInjectionStrategy;
import holon.internal.routing.annotated.injection.FormParamInjectionStrategy;
import holon.internal.routing.annotated.injection.MiddlewareProvidedArgStrategy;
import holon.internal.routing.annotated.injection.PathParamInjectionStrategy;
import holon.internal.routing.annotated.injection.QueryParamInjectionStrategy;
import holon.internal.routing.annotated.injection.RequestInjectionStrategy;
import holon.internal.routing.basic.CallbackRoute;
import holon.spi.RequestContext;
import holon.spi.Route;
import holon.util.collection.Pair;

import static holon.internal.routing.annotated.ArgInjectionStrategy.ArgumentInjector;
import static holon.util.collection.ArrayTools.concat;
import static holon.util.collection.ArrayTools.reverse;
import static java.util.Arrays.asList;

/**
 * Takes an annotation-based route and compiles it to a callable route object, each one contains a full stack of
 * middleware for the route in question.
 */
public class RouteCompiler
{
    private final ArgInjectionStrategy[] endpointInjectionStrategies;
    private final DependencyInjector dependencyInjector;
    private final List<Class<?>> globalMiddleware;

    public RouteCompiler( Components components, List<Class<?>> globalMiddleware )
    {
        this.globalMiddleware = reverse(new ArrayList<>(globalMiddleware));
        this.dependencyInjector = new DependencyInjector( components );
        endpointInjectionStrategies = new ArgInjectionStrategy[]{
            new ComponentInjectionStrategy(components),
            new RequestInjectionStrategy(),
            new FormParamInjectionStrategy(),
            new PathParamInjectionStrategy(),
            new QueryParamInjectionStrategy(),
            new CookieParamInjectionStrategy()
        };
    }

    public Route compile( String basePath, Class<?> cls, Method method, Annotation annotation )
    {
        MiddlewareContext mwProvidedDeps = new MiddlewareContext();
        MiddlewareProvidedArgStrategy middleDepsInjectionStrategy = new MiddlewareProvidedArgStrategy( mwProvidedDeps );

        Consumer<RequestContext> endpoint = createEndpoint( dependencyInjector.instantiate( cls ), method,
               middleDepsInjectionStrategy );

        endpoint = buildMiddlewareStack( cls, method, mwProvidedDeps, middleDepsInjectionStrategy, endpoint );

        if(annotation instanceof GET )
        {
            return new CallbackRoute( HttpMethod.Standard.GET, basePath + ((GET)annotation).value(), endpoint );
        }
        else if(annotation instanceof POST )
        {
            return new CallbackRoute( HttpMethod.Standard.POST, basePath + ((POST)annotation).value(), endpoint );
        }
        else if(annotation instanceof PUT )
        {
            return new CallbackRoute( HttpMethod.Standard.PUT, basePath + ((PUT)annotation).value(), endpoint );
        }
        else if(annotation instanceof DELETE )
        {
            return new CallbackRoute( HttpMethod.Standard.DELETE, basePath + ((DELETE)annotation).value(), endpoint );
        }
        else if(annotation instanceof HEAD )
        {
            return new CallbackRoute( HttpMethod.Standard.HEAD, basePath + ((HEAD)annotation).value(), endpoint );
        }
        else if(annotation instanceof OPTIONS )
        {
            return new CallbackRoute( HttpMethod.Standard.OPTIONS, basePath + ((OPTIONS)annotation).value(), endpoint );
        }
        else
        {
            throw new IllegalStateException( "Unknown route annotation: " + annotation );
        }
    }

    private Consumer<RequestContext> buildMiddlewareStack( Class<?> cls, Method method, MiddlewareContext mwProvidedDeps, MiddlewareProvidedArgStrategy middleDepsInjectionStrategy, Consumer<RequestContext> endpoint )
    {
        for ( Pair<Pair<Class<?>, Annotation>, Method> middleware : listMiddleware( cls, method ) )
        {
            Class<?> middlewareClass = middleware.first().first();
            Object middlewareAnnotation = middleware.first().second();
            BiConsumer<RequestContext, Pipeline> handler = createPipelineHandler(
                    dependencyInjector.instantiate( middlewareClass, asList( middlewareAnnotation ) ),
                    middleware.second(), middleDepsInjectionStrategy );

            endpoint = new MiddlewarePipelineStep( endpoint, handler, mwProvidedDeps );
        }
        return endpoint;
    }

    private Consumer<RequestContext> createEndpoint( Object obj, Method method, MiddlewareProvidedArgStrategy middleDeps )
    {
        BiConsumer<RequestContext, Pipeline> endpoint = createPipelineHandler( obj, method, middleDeps );
        return (ctx) -> endpoint.accept( ctx, null );
    }

    private BiConsumer<RequestContext, Pipeline> createPipelineHandler( Object obj, Method method,
                                                                 MiddlewareProvidedArgStrategy middleDeps )
    {
        // middleDeps handled separately as they are stateful, so we need one per endpoint stack.
        ArgInjectionStrategy[] injectionStrategies = concat( endpointInjectionStrategies, new ArgInjectionStrategy[]{middleDeps} );

        Object[] args = new Object[method.getParameterCount()];
        List<ArgumentInjector> injectorList = new ArrayList<>();

        Class<?>[] types = method.getParameterTypes();
        Annotation[][] annotations = method.getParameterAnnotations();

        argloop: for ( int i = 0; i < args.length; i++ )
        {
            for ( ArgInjectionStrategy injectionStrategy : injectionStrategies )
            {
                if(injectionStrategy.appliesTo( types[i], annotations[i] ))
                {
                    ArgumentInjector injector = injectionStrategy.satisfyArgument( args, i, types[i], annotations[i] );
                    if(injector != null)
                    {
                        injectorList.add( injector );
                    }
                    continue argloop;
                }
            }
        }

        // Convert to primitive array to avoid creating iterators during runtime
        ArgumentInjector[] injectors = injectorList.toArray(new ArgumentInjector[injectorList.size()]);

        return ( ctx, pipe ) -> {
            try
            {
                for ( ArgumentInjector argumentInjector : injectors )
                {
                    argumentInjector.apply( args, ctx, pipe );
                }

                method.invoke( obj, args );
            }
            catch ( IllegalAccessException | InvocationTargetException e )
            {
                // To avoid huge exception chains caused by an exception thrown deep inside a route, where each
                // wrapping middleware would add two exceptions, we unwrap exceptions here if possible.
                if(e.getCause() != null && e.getCause() instanceof RuntimeException)
                {
                    throw (RuntimeException)e.getCause();
                }
                throw new HolonException( "Failed to handle request.", e );
            }
        };
    }

    private List<Pair<Pair<Class<?>, Annotation>, Method>> listMiddleware( Class<?> cls, Method method )
    {
        List<Pair<Class<?>, Annotation>> middlewareClasses = new ArrayList<>();
        for ( Annotation a : reverse( concat( cls.getAnnotations(), method.getAnnotations() ) ) )
        {
            for ( Annotation aa : a.annotationType().getAnnotations() )
            {
                if(aa instanceof MiddlewareAnnotation )
                {
                    middlewareClasses.add( Pair.pair( ((MiddlewareAnnotation) aa).value(), a ) );
                }
            }
        }

        globalMiddleware.forEach( ( c ) -> middlewareClasses.add( Pair.pair( c, null ) ) );

        List<Pair<Pair<Class<?>, Annotation>, Method>> middlewares = new ArrayList<>();
        for ( Pair<Class<?>, Annotation> middlewareClass : middlewareClasses )
        {
            for ( Method candidate : middlewareClass.first().getDeclaredMethods() )
            {
                for ( Annotation candidateAnnotation : candidate.getAnnotations() )
                {
                    if(candidateAnnotation instanceof MiddlewareHandler )
                    {
                        middlewares.add( new Pair<>(middlewareClass, candidate) );
                    }
                }

            }
        }

        return middlewares;
    }
}
