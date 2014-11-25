package holon.internal.routing.annotated;

import java.io.IOException;
import java.lang.annotation.Annotation;

import holon.api.exception.HolonException;
import holon.api.middleware.Pipeline;
import holon.spi.RequestContext;

public interface ArgInjectionStrategy
{
    /**
     * Injects a single argument into an array of arguments, at some specific position. This is used for arguments
     * that change per-request.
     */
    static abstract class ArgumentInjector
    {
        private final int position;

        public ArgumentInjector(int position)
        {
            this.position = position;
        }

        public void apply( Object[] args, RequestContext ctx, Pipeline pipeline )
        {
            try
            {
                args[position] = generateArgument( ctx, pipeline );
            }
            catch ( IOException e )
            {
                throw new HolonException( "IO exception while gathering values for dependency injection.", e );
            }
        }

        public abstract Object generateArgument( RequestContext ctx, Pipeline pipeline ) throws IOException;
    }

    boolean appliesTo( Class<?> type, Annotation[] annotation );

    /**
     * Satisfy an argument, either by setting it directly at the specified position or by returning an ArgumentInjector,
     * which will be invoked when the method is called, and is expected to set the argument at the specified position
     * then.
     */
    ArgumentInjector satisfyArgument( Object[] args, int position, Class<?> type, Annotation[] annotation );
}
