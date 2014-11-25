package holon.api.http;

import java.io.IOException;

/**
 * This embodies the content of a response to a client. Content is re-usable and single-threaded. Users are encouraged
 * to create content instances up-front and to set them on final fields, and then re-use the same content instance
 * for multiple responses. This is meant to minimize object allocation, so as to not trigger latency-inducing GC pauses.
 */
public interface Content
{
    /** Render the body of this content. */
    void render(Output out, Object context) throws IOException;

    /** Unless the user has specifically set the Content-type header, this content type will be used. */
    default String contentType(Object context) { return "text/html"; }
}
