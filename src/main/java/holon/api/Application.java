package holon.api;

import java.util.Collection;
import java.util.Collections;

import holon.api.config.Config;

/**
 * Application is an optional interface used to give {@link holon.Holon} control of your application life cycle.
 * Specifically, it lets {@link holon.Holon} decide when to start up your components. Depending on your use case,
 * you may not want to use this at all and rather have you be the one that decides when to start the application.
 *
 * The reason you'd want to use this is that it enables {@link holon.Holon} to auto-restart your application whenever
 * it detects changes to your source files, which can be a big time saver in development mode.
 *
 * Implementers of this interface must either have a no-arg constructor, or no constructors at all.
 */
public interface Application
{
    /**
     * Initialize the application. This method is called on startup, and may get called several times
     * (with {@link #shutdown()} called in between) if you have auto-redeploy enabled.
     *
     * Any object returned here will become available for dependency injection.
     */
    default Collection<Object> startup( Config config ) throws Exception { return Collections.emptyList(); };

    /**
     * Called when the application is shut down and before it's restarted if auto-redeploy is enabled.
     */
    default void shutdown() { }
}
