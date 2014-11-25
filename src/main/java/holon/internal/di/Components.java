package holon.internal.di;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;

public class Components
{
    private final Map<Class<?>, Object> registry = new HashMap<>();

    public <T> T resolve(Class<T> cls)
    {
        return (T) registry.get( cls );
    }

    public <T> Components register( T component )
    {
        return register( component.getClass(), component );
    }

    private <T> Components register( Class<? extends T> cls, T component )
    {
        asList( cls.getInterfaces() ).forEach( ( i ) -> register( i, component ) );
        if(cls.getSuperclass() != null) register( cls.getSuperclass(), component );

        if(!cls.equals(Object.class))
        {
            registry.put( cls, component );
        }
        return this;
    }

    public boolean contains( Class<?> cls )
    {
        return registry.containsKey( cls );
    }
}
