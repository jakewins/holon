package holon.internal.routing.annotated.injection;

import java.lang.annotation.Annotation;

import holon.api.http.Request;
import holon.api.middleware.Pipeline;
import holon.internal.routing.annotated.ArgInjectionStrategy;
import holon.spi.RequestContext;

public class RequestInjectionStrategy implements ArgInjectionStrategy
{
    @Override
    public boolean appliesTo( Class<?> type, Annotation[] annotation )
    {
        // Temporary, this whole subcomponent needs to be redesigned
        return type == Request.class || type == Pipeline.class || type == RequestContext.class;
    }

    @Override
    public ArgumentInjector satisfyArgument( Object[] args, int position, Class<?> type, Annotation[] annotation )
    {
        if(type == Request.class || type == RequestContext.class)
        {
            return new ArgumentInjector( position )
            {
                @Override
                public Object generateArgument( RequestContext ctx, Pipeline pipeline )
                {
                    return ctx;
                }
            };
        }
        else
        {
            return new ArgumentInjector( position )
            {
                @Override
                public Object generateArgument( RequestContext ctx, Pipeline pipeline )
                {
                    return pipeline;
                }
            };
        }
    }
}
