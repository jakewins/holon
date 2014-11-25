package holon.api;

import java.util.Collection;
import java.util.Collections;

/**
 * Bootstrap is an optional interface used to give {@link holon.Holon} control of your application life cycle.
 * Specifically, it lets {@link holon.Holon} decide when to start up your components. Depending on your use case,
 * you may not want to use this at all - rather have you be the one that decides when to start the application.
 *
 * The reason you'd want to use this is that it enables {@link holon.Holon} to auto-restart your application whenever
 * it detects changes to your source files, which can be a big time saver in development mode.
 *
 * Implementers of this interface MUST have a no-arg constructor available for Holon to call.
 */
public interface Application
{
    /**
     * Any object returned here will become available for dependency injection.
     */
    default Collection<Object> bootstrap() { return Collections.emptyList(); };

    /**
     * Called when the application is shut down.
     */
    default void shutdown() { }
}
