package holon.api.exception;

public class MissingConfigurationException extends HolonException
{
    public MissingConfigurationException( String setting )
    {
        super(String.format("Setting '%s' is required, you need to configure this.", setting), null);
    }
}
