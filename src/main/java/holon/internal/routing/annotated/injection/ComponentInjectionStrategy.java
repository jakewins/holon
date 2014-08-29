package holon.internal.routing.annotated.injection;

import java.lang.annotation.Annotation;

import holon.internal.di.Components;
import holon.internal.routing.annotated.ArgInjectionStrategy;

public class ComponentInjectionStrategy implements ArgInjectionStrategy
{
    private final Components components;

    public ComponentInjectionStrategy( Components components )
    {
        this.components = components;
    }

    @Override
    public boolean appliesTo( Class<?> type, Annotation[] annotation )
    {
        return components.contains( type );
    }

    @Override
    public ArgumentInjector satisfyArgument( Object[] args, int position, Class<?> type, Annotation[] annotation )
    {
        args[position] = components.resolve( type );
        return null;
    }
}
