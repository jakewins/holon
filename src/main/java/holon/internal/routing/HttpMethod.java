package holon.internal.routing;

public interface HttpMethod
{
    enum Standard implements HttpMethod
    {
        GET,
        PUT,
        POST,
        DELETE,
        OPTIONS,
        HEAD,
    }

    String name();
}
