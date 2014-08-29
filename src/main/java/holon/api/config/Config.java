package holon.api.config;

public interface Config
{
    <T> T get( Setting<T> setting );

    <T> void set( Setting<T> setting, Object value );
}
