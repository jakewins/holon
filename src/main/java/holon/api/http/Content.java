package holon.api.http;

import java.io.IOException;
import java.util.Map;

import holon.api.io.Output;

@FunctionalInterface
public interface Content
{
    void render( Map<String, Object> ctx, Output out ) throws IOException;

    default String type() { return "text/html"; }

    /**
     * Set additional headers.
     */
    default Map<String, String> headers() { return null; }
}
