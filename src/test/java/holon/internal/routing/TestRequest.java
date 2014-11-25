package holon.internal.routing;

import holon.api.http.Content;
import holon.api.http.Cookies;
import holon.api.http.Request;
import holon.api.http.Status;
import holon.internal.routing.path.Path;
import holon.internal.routing.path.PatternSegment;
import holon.spi.RequestContext;

import java.util.HashMap;
import java.util.Map;

public class TestRequest implements RequestContext
{
    private final Path path;
    private final HashMap<String, Object> postData;

    public TestRequest( String path, HashMap<String, Object> postData )
    {
        this.path = new PatternSegment.ParamHandlingPath().initialize( path, null );
        this.postData = postData;
    }

    @Override
    public void respond( Status status )
    {

    }

    @Override
    public void respond( Status status, Content content )
    {

    }

    @Override
    public void respond( Status status, Content content, Object context )
    {

    }

    @Override
    public Request addCookie( String name, String value )
    {
        return null;
    }

    @Override
    public Request addCookie( String name, String value, String path, String domain, int maxAge, boolean secure,
                              boolean httpOnly )
    {
        return null;
    }

    @Override
    public Request discardCookie( String name )
    {
        return null;
    }

    @Override
    public Request addHeader( String header, String value )
    {
        return null;
    }

    @Override
    public RequestContext initialize( Path path )
    {
        return null;
    }

    @Override
    public Path path()
    {
        return path;
    }

    @Override
    public Map<String, Object> formData()
    {
        return postData;
    }

    @Override
    public Cookies cookies()
    {
        return null;
    }

    @Override
    public Map<String, Iterable<String>> queryParams()
    {
        return null;
    }
}
