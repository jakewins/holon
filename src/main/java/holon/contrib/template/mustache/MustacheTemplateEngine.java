package holon.contrib.template.mustache;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import holon.Holon;
import holon.api.config.Config;
import holon.api.exception.HolonException;
import holon.api.http.Content;
import holon.contrib.template.Templates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MustacheTemplateEngine implements Templates
{
    private final MustacheFactory mustacheFactory;

    public MustacheTemplateEngine( Config config )
    {
        Path templatePath = config.get( Holon.Configuration.template_path );
        if( !Files.exists(templatePath))
        {
            try
            {
                Files.createDirectories( templatePath );
            }
            catch ( IOException e )
            {
                throw new HolonException( "Failed to create template directory at '"+templatePath+"'.", e );
            }
        }

        if(config.get( Holon.Configuration.auto_redeploy ))
        {
            mustacheFactory = new UncachedMustacheFactory( templatePath.toFile() );
        }
        else
        {
            mustacheFactory = new DefaultMustacheFactory( templatePath.toFile() );
        }
    }

    @Override
    public Content load( String path )
    {
        Mustache compile = mustacheFactory.compile( path );
        return new MustacheTemplate(compile);
    }
}
