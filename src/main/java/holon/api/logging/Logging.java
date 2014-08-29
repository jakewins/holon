package holon.api.logging;

public interface Logging
{
    interface Logger
    {
        void debug(String message);
        void info(String message);
        void warn(String message);
        void error(String message);

        void debug(String message, Throwable cause);
        void info(String message, Throwable cause);
        void warn(String message, Throwable cause);
        void error(String message, Throwable cause);
    }

    Logger logger( String label );
}
