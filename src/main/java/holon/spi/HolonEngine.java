package holon.spi;

import java.util.function.Supplier;

/**
 * A Holon engine serves a set of user-defined routes. Generally it would use some pre-existing components for the
 * various services it needs to fulfill, like routing and dependency injection.
 */
public interface HolonEngine
{
    /**
     * Serve the specified set of routes. The implementation will call the supplier once for each thread it runs,
     * and it is up to the supplier to decide to return the same routes (in which case they must be thread safe) or to
     * return new route instances upon each call.
     */
    void serve( Supplier<Iterable<Route>> routes );

    void shutdown();

    void join();

}
