package holon.api.exception;

public class HolonException extends RuntimeException
{
    public HolonException( String message, Throwable cause )
    {
        super(message, cause);
    }
}
