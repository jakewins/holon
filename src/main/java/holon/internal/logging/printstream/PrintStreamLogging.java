package holon.internal.logging.printstream;

import java.io.PrintStream;

import holon.api.logging.Logging;

public class PrintStreamLogging implements Logging
{
    private final PrintStream stream;

    public PrintStreamLogging(PrintStream stream)
    {
        this.stream = stream;
    }

    @Override
    public Logger logger( String label )
    {
        return new PrintStreamLogger(label, stream);
    }
}
