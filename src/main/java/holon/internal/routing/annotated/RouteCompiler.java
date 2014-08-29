package holon.internal.routing.annotated;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import holon.api.exception.HolonException;
import holon.api.http.DELETE;
import holon.api.http.GET;
import holon.api.http.HEAD;
import holon.api.http.OPTIONS;
import holon.api.http.POST;
import holon.api.http.PUT;
import holon.api.http.Request;
import holon.internal.di.Components;
import holon.internal.routing.HttpMethod;
import holon.internal.routing.annotated.injection.ComponentInjectionStrategy;
import holon.internal.routing.annotated.injection.PostParamInjectionStrategy;
import holon.internal.routing.annotated.injection.RequestInjectionStrategy;
import holon.internal.routing.basic.CallbackRoute;
import holon.spi.Route;

import static holon.internal.routing.annotated.ArgInjectionStrategy.ArgumentInjector;

/**
 * Takes an annotation-based route and compiles it to a callable route object.
 */
public class RouteCompiler
{
    private final Components components;
    private final ArgInjectionStrategy[] endpointInjectionStrategies;

    public RouteCompiler( Components components )
    {
        this.components = components;
        endpointInjectionStrategies = new ArgInjectionStrategy[]{
            new ComponentInjectionStrategy(components),
            new RequestInjectionStrategy(),
            new PostParamInjectionStrategy()
        };
    }

    public Route compile( String basePath, Class<?> cls, Method method, Annotation annotation )
    {
        Consumer<Request> endpoint = createEndpoint( instantiate( cls ), method );
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

    private Consumer<Request> createEndpoint( Object obj, Method method )
    {
        Object[] args = new Object[method.getParameterCount()];
        List<ArgumentInjector> injectorList = new ArrayList<>();

        Class<?>[] types = method.getParameterTypes();
        Annotation[][] annotations = method.getParameterAnnotations();

        for ( int i = 0; i < args.length; i++ )
        {
            for ( ArgInjectionStrategy injectionStrategy : endpointInjectionStrategies )
            {
                if(injectionStrategy.appliesTo( types[i], annotations[i] ))
                {
                    ArgumentInjector injector = injectionStrategy.satisfyArgument( args, i, types[i], annotations[i] );
                    if(injector != null)
                    {
                        injectorList.add( injector );
                    }
                }
            }
        }

        // Convert to primitive array to avoid creating iterators during runtime
        ArgumentInjector[] injectors = injectorList.toArray(new ArgumentInjector[injectorList.size()]);

        return ( ctx ) -> {
            try
            {
                for ( ArgumentInjector argumentInjector : injectors )
                {
                    argumentInjector.apply( args, ctx );
                }

                method.invoke( obj, args );
            }
            catch ( IllegalAccessException | InvocationTargetException e )
            {
                throw new HolonException( "Unable to invoke endpoint.", e );
            }
        };
    }

    private Object instantiate( Class<?> cls )
    {
        try
        {
            constructorLoop: for ( Constructor<?> constructor : cls.getConstructors() )
            {
                List<Object> args = new ArrayList<>();
                for ( Class<?> dependency : constructor.getParameterTypes() )
                {
                    if(!components.contains(dependency))
                    {
                        continue constructorLoop;
                    }

                    args.add( components.resolve( dependency ) );
                }

                // Found a constructor we can use.
                return constructor.newInstance( args.toArray() );
            }

            throw new HolonException( "Cannot find a usable constructor for '" + cls.getSimpleName()
                    + "'. Make sure there is a constructor, and that all arguments to it are registered for " +
                    "dependency injection.", null );
        }
        catch ( InstantiationException | IllegalAccessException | InvocationTargetException e)
        {
            throw new HolonException( "Unable to instantiate route.", e);
        }
    }
}
