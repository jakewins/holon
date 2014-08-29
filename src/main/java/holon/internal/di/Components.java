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
        Class<?> cls = component.getClass();
        asList( cls.getInterfaces() ).forEach( (i) -> register(i, component) );
        return register( cls, component );
    }

    public <T> Components register( Class<? extends T> cls, T template )
    {
        registry.put( cls, template );
        return this;
    }

    public boolean contains( Class<?> cls )
    {
        return registry.containsKey( cls );
    }
}
