package holon.contrib.template.mustache;

import java.io.IOException;
import java.io.Writer;

import com.github.mustachejava.Mustache;
import holon.api.http.Content;
import holon.api.http.Output;
import holon.internal.http.common.ErrorContent;

public class MustacheTemplate implements Content
{
    private final Mustache template;

    public MustacheTemplate( Mustache template )
    {
        this.template = template;
    }

    @Override
    public void render( Output out, Object context ) throws IOException
    {
        Writer writer = out.asWriter();

        try
        {
            template.execute( writer, context );
        }
        catch(Throwable e)
        {
            new ErrorContent().render( out, e );
        }
        finally
        {
            writer.flush();
        }
    }
}
