package holon.internal.routing.annotated;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import holon.api.http.DELETE;
import holon.api.http.GET;
import holon.api.http.HEAD;
import holon.api.http.OPTIONS;
import holon.api.http.POST;
import holon.api.http.PUT;
import holon.internal.routing.annotated.RouteCompiler;
import holon.spi.Route;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

/**
 * Goes through configured packages and finds routes based on annotations in classes.
 */
public class RouteScanner
{
    private final RouteCompiler routeCompiler;

    public RouteScanner( RouteCompiler routeCompiler )
    {
        this.routeCompiler = routeCompiler;
    }

    public void scan( String basePath, String packageName, Consumer<Route> routeProcessor )
    {
        listClassesIn( packageName ).forEach( cls -> {
            for ( Method method : cls.getDeclaredMethods() )
            {
                for ( Annotation annotation : method.getAnnotations() )
                {
                    if ( isRouteAnnotation( annotation ) )
                    {
                        routeProcessor.accept( routeCompiler.compile( basePath, cls, method, annotation ) );
                    }
                }
            }
        });
    }

    private Set<Class<?>> listClassesIn( String packageName )
    {
        List<ClassLoader> classLoadersList = new LinkedList<>();
        classLoadersList.add( ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false /* don't exclude Object.class */), new ResourcesScanner())
                .setUrls( ClasspathHelper.forClassLoader( classLoadersList.toArray( new ClassLoader[0] ) ))
                .filterInputsBy(new FilterBuilder().include( FilterBuilder.prefix( packageName ))));

        return reflections.getSubTypesOf( Object.class );
    }

    private boolean isRouteAnnotation( Annotation annotation ) {
        return     annotation instanceof GET
                || annotation instanceof POST
                || annotation instanceof PUT
                || annotation instanceof DELETE
                || annotation instanceof OPTIONS
                || annotation instanceof HEAD;
    }
}
