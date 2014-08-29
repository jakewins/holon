package holon.api.exception;

public class InvalidConfigurationException extends HolonException
{
    public InvalidConfigurationException( String message ) { this(message, null) ;}
    public InvalidConfigurationException( String message, Throwable cause )
    {
        super(message, cause);
    }
}
