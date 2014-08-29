package holon.internal.routing.annotated.injection;

import java.lang.annotation.Annotation;
import java.util.Map;

import holon.api.http.PostParam;
import holon.api.http.Request;
import holon.internal.routing.annotated.ArgInjectionStrategy;

public class PostParamInjectionStrategy implements ArgInjectionStrategy
{
    @Override
    public boolean appliesTo( Class<?> type, Annotation[] annotations )
    {
        for ( Annotation annotation : annotations )
        {
            if(annotation instanceof PostParam )
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
            if(annotation instanceof PostParam )
            {
                final String attribute = ((PostParam)annotation).value();
                if(!attribute.equals( "" ))
                {
                    if(type != String.class)
                    {
                        throw new IllegalArgumentException( "PostParam attribute needs to be a string, but was " + type + "." );
                    }
                    return new ArgumentInjector(position)
                    {
                        @Override
                        public Object generateArgument( Request ctx )
                        {
                            return ctx.postData().get( attribute );
                        }
                    };
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
            public Object generateArgument( Request ctx )
            {
                return ctx.postData();
            }
        };
    }
}
