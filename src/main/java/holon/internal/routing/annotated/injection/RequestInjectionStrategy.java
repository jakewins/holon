package holon.internal.routing.annotated.injection;

import java.lang.annotation.Annotation;

import holon.api.http.Request;
import holon.internal.routing.annotated.ArgInjectionStrategy;

public class RequestInjectionStrategy implements ArgInjectionStrategy
{
    @Override
    public boolean appliesTo( Class<?> type, Annotation[] annotation )
    {
        return type == Request.class;
    }

    @Override
    public ArgumentInjector satisfyArgument( Object[] args, int position, Class<?> type, Annotation[] annotation )
    {
        return new ArgumentInjector(position)
        {
            @Override
            public Object generateArgument( Request ctx )
            {
                return ctx;
            }
        };
    }
}
