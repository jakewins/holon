package holon.contrib.template.mustache;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import holon.Holon;
import holon.api.config.Config;
import holon.api.http.Content;
import holon.contrib.template.Templates;

public class MustacheTemplateEngine implements Templates
{
    private final MustacheFactory mustacheFactory;

    public MustacheTemplateEngine( Config config )
    {
        if(config.get( Holon.Configuration.auto_redeploy ))
        {
            mustacheFactory = new UncachedMustacheFactory( config.get( Holon.Configuration.template_path ).toFile() );
        }
        else
        {
            mustacheFactory = new DefaultMustacheFactory( config.get( Holon.Configuration.template_path ).toFile() );
        }
    }

    @Override
    public Content load( String path )
    {
        Mustache compile = mustacheFactory.compile( path );
        return new MustacheTemplate(compile);
    }
}
