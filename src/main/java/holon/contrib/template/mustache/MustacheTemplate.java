package holon.contrib.template.mustache;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import com.github.mustachejava.Mustache;
import holon.api.io.Output;
import holon.api.template.Template;

public class MustacheTemplate implements Template
{
    private final Mustache template;

    public MustacheTemplate( Mustache template )
    {
        this.template = template;
    }

    @Override
    public void render( Map<String, Object> ctx, Output out ) throws IOException
    {
        Writer writer = out.asWriter();
        template.execute( writer, ctx );
        writer.flush();
    }

    @Override
    public String type()
    {
        return "text/html";
    }
}
