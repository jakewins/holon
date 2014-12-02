package holon.internal.routing.annotated.injection;

import holon.api.exception.HolonException;
import holon.api.http.Default;
import holon.api.http.FormParam;
import holon.api.http.UploadedFile;
import holon.api.middleware.Pipeline;
import holon.internal.routing.annotated.ArgInjectionStrategy;
import holon.spi.RequestContext;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Map;

import static holon.internal.routing.annotated.AnnotationExtractor.findAnnotation;

public class FormParamInjectionStrategy implements ArgInjectionStrategy
{
    @Override
    public boolean appliesTo( Class<?> type, Annotation[] annotations )
    {
        return findAnnotation( FormParam.class, annotations ) != null;
    }

    @Override
    public ArgInjectionStrategy.ArgumentInjector satisfyArgument( Object[] args, int position, Class<?> type, Annotation[] annotations )
    {
        // First, check if the annotation specifies a specific attribute to use, and if it does, set things up
        // to inject the value of that attribute directly
        final FormParam paramAnnotation = findAnnotation( FormParam.class, annotations );
        final Default defaultAnnotation = findAnnotation( Default.class, annotations );

        final String attribute = paramAnnotation.value();
        final String defaultValue = defaultAnnotation == null ? null : defaultAnnotation.value();

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
                            if(defaultValue != null)
                            {
                                return defaultValue;
                            }
                            throw new HolonException( "Missing required post parameter '" + attribute + "'" );
                        }
                        return o;
                    }
                };
            }

            throw new IllegalArgumentException( "PostParam attribute needs to be a string, but was " + type + "." );
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
