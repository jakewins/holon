package holon.internal.routing.annotated.injection;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Map;

import holon.api.exception.HolonException;
import holon.api.http.FormParam;
import holon.api.http.UploadedFile;
import holon.api.middleware.Pipeline;
import holon.internal.routing.annotated.ArgInjectionStrategy;
import holon.spi.RequestContext;

public class FormParamInjectionStrategy implements ArgInjectionStrategy
{
    @Override
    public boolean appliesTo( Class<?> type, Annotation[] annotations )
    {
        for ( Annotation annotation : annotations )
        {
            if(annotation instanceof FormParam )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public ArgumentInjector satisfyArgument( Object[] args, int position, Class<?> type, Annotation[] annotations )
    {
        // First, check if the annotation specifies a specific attribute to use, and if it does, set things up
        // to inject the value of that attribute directly
        for ( Annotation annotation : annotations )
        {
            if(annotation instanceof FormParam )
            {
                FormParam formParam = (FormParam) annotation;
                final String attribute = formParam.value();
                final String defaultVal = formParam.defaultVal().length() == 0 ? null : formParam.defaultVal();
                if(!attribute.equals( "" ))
                {
                    if(type == String.class || type == UploadedFile.class)
                    {
                        return new ArgumentInjector(position)
                        {
                            @Override
                            public Object generateArgument( RequestContext ctx, Pipeline pipeline ) throws IOException
                            {
                                Object o = ctx.formData().get( attribute );
                                if(o == null)
                                {
                                    if(defaultVal != null)
                                    {
                                        return defaultVal;
                                    }
                                    throw new HolonException( "Missing required post parameter '" + attribute + "'" );
                                }
                                return o;
                            }
                        };
                    }

                    throw new IllegalArgumentException( "PostParam attribute needs to be a string, but was " + type + "." );
                }
            }
        }

        // If a specific attribute was not asked for, the user should get the full attribute map
        if(type != Map.class)
        {
            throw new IllegalArgumentException( "PostParam attribute needs to be a map, but was " + type + "." );
        }

        return new ArgumentInjector(position)
        {
            @Override
            public Object generateArgument( RequestContext ctx, Pipeline pipeline ) throws IOException
            {
                return ctx.formData();
            }
        };
    }
}
