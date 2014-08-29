package holon.spi;

/**
 * A Holon engine serves a set of user-defined routes. Generally it would use some pre-existing components for the
 * various services it needs to fulfill, like routing and dependency injection.
 */
public interface HolonEngine
{
    void serve( Iterable<Route> routes );

    void shutdown();

    void join();

}
