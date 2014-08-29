package holon.internal.routing.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import holon.spi.Route;

/**
 * A simple router implementation.
 */
public class BasicRouter
{
    private final Map<String, List<Route>> routes = new HashMap<>(1024, 0.5f);
    private final Route notFoundRoute;

    public BasicRouter( Iterable<Route> routes, Route notFoundRoute )
    {
        this.notFoundRoute = notFoundRoute;
        routes.forEach( (route) ->{
            if(!this.routes.containsKey( route.method() ))
            {
                this.routes.put(route.method(), new ArrayList<>());
            }
            this.routes.get( route.method() ).add( route );
        });
    }

    public Route route( String method, String path )
    {
        List<Route> list = routes.get( method );
        if(list != null)
        {
            for ( Route route : list )
            {
                if(route.pattern().matches( path ))
                {
                    return route;
                }
            }
        }

        return notFoundRoute;
    }

}
