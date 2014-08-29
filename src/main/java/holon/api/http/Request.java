package holon.api.http;

import java.util.Map;

/**
 * The context of a regular HTTP request.
 */
public interface Request
{
    public void respond( Status status );
    public void respond( Status status, Content content );
    public void respond( Status status, Content content, Map<String, Object> context );

    String path();


    // Temporary
    Map<String, String> postData();
}
