package holon.internal.routing.annotated;

import java.lang.annotation.Annotation;

import holon.api.http.Request;

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

        public void apply( Object[] args, Request ctx )
        {
            args[position] = generateArgument( ctx );
        }

        public abstract Object generateArgument( Request ctx );
    }

    boolean appliesTo( Class<?> type, Annotation[] annotation );

    /**
     * Satisfy an argument, either by setting it directly at the specified position or by returning an ArgumentInjector,
     * which will be invoked when the method is called, and is expected to set the argument at the specified position
     * then.
     */
    ArgumentInjector satisfyArgument( Object[] args, int position, Class<?> type, Annotation[] annotation );
}
