package holon.api.exception;

import holon.api.config.Setting;

public class MissingConfigurationException extends HolonException
{
    public MissingConfigurationException()
    {
        super("Missing required setting.", null);
    }

    public MissingConfigurationException( Setting<?> setting )
    {
        super(String.format( "Missing required setting '%s', please configure this.", setting.key() ) );
    }
}
