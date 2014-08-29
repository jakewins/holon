package holon.internal.config;

import java.util.HashMap;
import java.util.Map;

import holon.api.config.Config;
import holon.api.config.Setting;

public class MapConfig implements Config
{
    private final Map<String, Object> params;

    public MapConfig(Map<String, Object> params)
    {
        this.params = params;
    }

    @Override
    public synchronized <T> T get( Setting<T> setting )
    {
        Object level = params;
        for ( String part : setting.key().split( "\\." ) )
        {
            if(level instanceof Map)
            {
                level = ((Map<String, Object>) level).get( part );
            }
            else
            {
                level = null;
                break;
            }
        }

        if(level == null)
        {
            return setting.apply( setting.defaultValue(this) );
        }

        return setting.apply( level );
    }

    @Override
    public synchronized <T> void set( Setting<T> setting, Object value )
    {
        Map<String, Object> level = params;
        String[] segments = setting.key().split( "\\." );
        for ( int i = 0; i < segments.length - 1; i++ )
        {
            String part = segments[i];
            if(!level.containsKey( part ))
            {
                level.put( part, new HashMap<String, Object>() );
            }

            level = (Map<String, Object>) level.get( part );
        }

        level.put( segments[segments.length - 1], value );
    }

    @Override
    public String toString()
    {
        return "MapConfig{" +
                "params=" + params +
                '}';
    }
}
