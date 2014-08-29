package holon.internal.routing;

import java.util.HashMap;
import java.util.Map;

import holon.api.http.Content;
import holon.api.http.Request;
import holon.api.http.Status;

public class TestRequest implements Request
{
    private final String path;
    private final HashMap<String, String> postData;

    public TestRequest( String path, HashMap<String, String> postData )
    {
        this.path = path;
        this.postData = postData;
    }

    @Override
    public void respond( Status status, Content content, Map<String, Object> context )
    {

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
    public String path()
    {
        return path;
    }

    @Override
    public Map<String, String> postData()
    {
        return postData;
    }
}
