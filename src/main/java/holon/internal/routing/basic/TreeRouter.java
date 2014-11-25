package holon.internal.routing.basic;

import holon.internal.routing.path.PathTreeCompiler;
import holon.internal.routing.path.PatternSegment;
import holon.spi.RequestContext;
import holon.spi.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A router implementation, this is single threaded, and does not support recursive
 * calls (eg. you can't call invoke another route while executing one).
 */
public class TreeRouter
{
    public static final String[] NO_SEGMENTS = new String[]{};
    private final PatternSegment[] routeTree;
    private final Map<String, List<Route>> dynamicRoutes;

    private final Route notFoundRoute;
    private final PatternSegment.ParamHandlingPath reusablePath = new PatternSegment.ParamHandlingPath();

    public TreeRouter( Iterable<Route> routes, Route notFoundRoute )
    {
        this.notFoundRoute = notFoundRoute;
        dynamicRoutes = buildDynamicRoutes( routes );
        routeTree = buildRouteTree( routes );
    }

    public void invoke( String method, String path, RequestContext ctx )
    {
        // First, try routing through the pattern tree
        String[] pathSegments = segmentPath( path );
        PatternSegment found = traverseRouteTree( pathSegments, method, 0, routeTree );
        if(found != null)
        {
            found.invoke( pathSegments, path, ctx );
            return;
        }

        // Route was not in our tree, fall back to dynamic path matching (routes that match using custom java code)
        ctx.initialize( reusablePath.initialize( path, pathSegments ) );

        // And then find a dynamic path that likes this request
        List<Route> list = dynamicRoutes.get( method );
        if(list != null)
        {
            for ( Route route : list )
            {
                if(route.pattern().matches( path ))
                {
                    route.call( ctx );
                    return;
                }
            }
        }

        notFoundRoute.call( ctx );
    }

    private String[] segmentPath( String path )
    {
        if(path.startsWith( "/" ))
        {
            path = path.substring( 1 );
        }

        if(path.length() == 0)
        {
            return NO_SEGMENTS;
        }

        return path.split( "/" );
    }

    private PatternSegment traverseRouteTree( String[] segments, String currentSegment, int currentSegmentIndex, PatternSegment[] tree )
    {
        for ( PatternSegment patternSegment : tree )
        {
            if(patternSegment.matches( currentSegment ))
            {
                if( currentSegmentIndex == segments.length )
                {
                    if(patternSegment.isCompleteRoute())
                    {
                        return patternSegment;
                    }
                }
                else
                {
                    PatternSegment found = traverseRouteTree(
                            segments, segments[currentSegmentIndex],
                            currentSegmentIndex+1, patternSegment.children() );
                    if(found != null)
                    {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    private PatternSegment[] buildRouteTree( Iterable<Route> routes )
    {
        return new PathTreeCompiler().compile( routes );
    }

    private Map<String, List<Route>> buildDynamicRoutes( Iterable<Route> routes )
    {
        Map<String, List<Route>> rs = new HashMap<>(1024, 0.5f);
        routes.forEach( (route) ->{
            if(route.pattern().isDynamic())
            {
                if(!rs.containsKey( route.method() ))
                {
                    rs.put( route.method().toLowerCase(), new ArrayList<>() );
                }
                rs.get( route.method().toLowerCase() ).add( route );
            }
        });
        return rs;
    }

}
