package holon.contrib.template.mustache;

import java.io.File;
import java.io.Writer;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.util.Node;

public class UncachedMustache implements Mustache
{
    private final String templatePath;
    private final File basePath;

    public UncachedMustache( String templatePath, File basePath )
    {
        this.templatePath = templatePath;
        this.basePath = basePath;
    }

    @Override
    public void append( String s )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Writer execute( Writer writer, Object params )
    {
        return new DefaultMustacheFactory( basePath ).compile( templatePath ).execute( writer, params );
    }

    @Override
    public Writer execute( Writer writer, Object[] params )
    {
        return new DefaultMustacheFactory( basePath ).compile( templatePath ).execute( writer, params );
    }

    @Override
    public Code[] getCodes()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void identity( Writer writer )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void init()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object clone()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object clone( Set<Code> codes )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node invert( Node node, String s, AtomicInteger atomicInteger )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCodes( Code[] codes )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Writer run( Writer writer, Object[] objects )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node invert( String s )
    {
        throw new UnsupportedOperationException();
    }
}
