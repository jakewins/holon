package holon.internal.logging.printstream;

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;

import holon.api.logging.Logging;

public class PrintStreamLogger implements Logging.Logger
{
    private final String label;
    private final PrintStream stream;

    public PrintStreamLogger( String label, PrintStream stream )
    {
        this.label = label;
        this.stream = stream;
    }

    @Override
    public void debug( String message )
    {
        log(message, "DEBUG", null);
    }

    @Override
    public void info( String message )
    {
        log(message, "INFO", null);
    }

    @Override
    public void warn( String message )
    {
        log(message, "WARN", null);
    }

    @Override
    public void error( String message )
    {
        log(message, "ERROR", null);
    }

    @Override
    public void debug( String message, Throwable cause )
    {
        log(message, "DEBUG", cause);
    }

    @Override
    public void info( String message, Throwable cause )
    {
        log(message, "INFO", cause);
    }

    @Override
    public void warn( String message, Throwable cause )
    {
        log(message, "WARN", cause);
    }

    @Override
    public void error( String message, Throwable cause )
    {
        log(message, "ERROR", cause);
    }

    private void log( String message, String level, Throwable cause )
    {
        stream.println(String.format("%s [%s] %s: %s", DateFormat.getDateTimeInstance().format( new Date() ), label, level, message));
        if(cause != null)
        {
            cause.printStackTrace(stream);
        }
    }
}
