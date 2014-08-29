package holon.internal.routing.basic;

import java.util.function.Consumer;

import holon.api.http.Request;
import holon.internal.routing.HttpMethod;
import holon.spi.Route;

/**
 * A route that delegates to a consumer callback to handle requests.
 */
public class CallbackRoute implements Route
{
    private final HttpMethod httpMethod;
    private final PathPattern path;
    private final Consumer<Request> endpoint;

    public CallbackRoute( HttpMethod httpMethod, String path, Consumer<Request> endpoint )
    {
        this.httpMethod = httpMethod;
        this.path = BasicPathPattern.compile( path );
        this.endpoint = endpoint;
    }

    @Override
    public String method()
    {
        return httpMethod.name();
    }

    @Override
    public PathPattern pattern()
    {
        return path;
    }

    @Override
    public void call( Request context )
    {
        endpoint.accept( context );
    }
}
