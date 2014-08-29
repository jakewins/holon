package holon.spi;

import holon.api.http.Request;

public interface Route
{
    public interface PathPattern
    {
        /**
         * This pattern is implemented in dynamic code, the engine is not allowed to try and bypass the pattern
         * matching with its own mechanisms.
         */
        boolean isDynamic();

        /**
         * The string pattern for this path. If {@link #isDynamic()} is true, this may be any user-understandable
         * specification of the pattern. If {@link #isDynamic()} is false, however, it *must* follow a strict
         * grammar. Specifically, it is allowed to use "*" as wildcards and "{key}" path parameters.
         *
         * Engines are allowed to bypass the pattern matching provided by implementers of this interface if the
         * pattern is not dynamic, which they may choose to do to allow certain low level routing optimizations.
         */
        String pattern();

        /**
         * Determine if this pattern matches a specific path.
         */
        boolean matches(String path);
    }

    String method();
    PathPattern pattern();

    void call( Request req );
}
