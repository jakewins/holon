package holon.api.http;

/**
 * The context of a regular HTTP request.
 */
public interface Request
{
    public void respond( Status status );
    public void respond( Status status, Content content );
    public void respond( Status status, Content content, Object context );

    /** Set a cookie on the client browser. */
    Request addCookie( String name, String value );

    /** Set a cookie on the client browser. */
    Request addCookie( String name, String value, String path, String domain, int maxAge, boolean secure,
                       boolean httpOnly );

    /** Discard a cookie in the client browser. */
    Request discardCookie(String name);

    /** Set a header for the request. */
    Request addHeader( String header, String value );
}
