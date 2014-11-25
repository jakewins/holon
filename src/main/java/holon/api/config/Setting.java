package holon.api.config;

import java.util.function.Function;

import holon.api.exception.InvalidConfigurationException;
import holon.api.exception.MissingConfigurationException;

/**
 * A key'd function that converts strings, maps or lists into a designated type.
 */
public interface Setting<T> extends Function<Object, T>
{
    String key();
    Object defaultValue(Config context);

    //
    // Companion interfaces and helper methods
    //

    public static Function<Config, Object> NO_DEFAULT = request -> {
        throw new MissingConfigurationException();
    };

    static Function<Config, Object> defaultValue( Object raw ) {
        return request -> raw;
    }

    public static <T> Setting<T> setting(String key, Function<? extends Object, T> converter, Function<Config, Object> defaultValue)
    {
        return new Setting<T>()
        {
            @Override
            public String key()
            {
                return key;
            }

            @Override
            public Object defaultValue(Config config)
            {
                return defaultValue.apply( config );
            }

            @Override
            public T apply( Object o )
            {
                try
                {
                    return (T) ((Function) converter).apply( o );
                }
                catch(ClassCastException e)
                {
                    // Terrible hack, but as far as I can tell, we can't get ParameterizedType out of the converter,
                    // so we're screwed trying to get the generic type at runtime, since it's defined on this method.
                    // Could be that we can pull it out of the method call somehow, but I'll leave that for later.
                    String[] parts = e.getMessage().split( " " );
                    String expectedTYpe = parts[parts.length - 1];

                    if( expectedTYpe.contains("Map"))
                    {
                        throw new InvalidConfigurationException( String.format("'%s' expects a key/value map, '%s' is not allowed.", key, o.toString()) );
                    }
                    else if( expectedTYpe.contains( "Iterable" ) || expectedTYpe.contains( "Collection" ))
                    {
                        throw new InvalidConfigurationException( String.format("'%s' expects a list, '%s' is not allowed.", key, o.toString()) );
                    }
                    else
                    {
                        throw new InvalidConfigurationException( String.format("'%s' expects a string, '%s' is not allowed.", key, o.toString()) );
                    }
                }
            }
        };
    }

}
