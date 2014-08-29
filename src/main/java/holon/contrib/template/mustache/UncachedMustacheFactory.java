package holon.contrib.template.mustache;

import java.io.File;
import java.io.Reader;
import java.io.Writer;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.MustacheVisitor;
import com.github.mustachejava.ObjectHandler;

public class UncachedMustacheFactory implements MustacheFactory
{
    private final File basePath;

    public UncachedMustacheFactory( File basePath )
    {
        this.basePath = basePath;
    }

    @Override
    public MustacheVisitor createMustacheVisitor()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getReader( String s )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void encode( String s, Writer writer )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectHandler getObjectHandler()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mustache compile( String templatePath )
    {
        return new UncachedMustache(templatePath, basePath);
    }

    @Override
    public Mustache compile( Reader reader, String s )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String translate( String s )
    {
        throw new UnsupportedOperationException();
    }
}
