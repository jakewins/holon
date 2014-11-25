package holon.internal.routing.path;

import holon.spi.Route;

public class BasicPathPattern implements Route.PathPattern
{
    public static Route.PathPattern compile(String pattern)
    {
        return new BasicPathPattern(pattern);
    }

    private final String pattern;

    private BasicPathPattern(String pattern)
    {
        this.pattern = pattern;
    }

    @Override
    public boolean isDynamic()
    {
        return false;
    }

    @Override
    public String pattern()
    {
        return pattern;
    }

    @Override
    public boolean matches( String path )
    {
        return path.equals( pattern );
    }
}
