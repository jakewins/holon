package holon.spi;

import java.io.IOException;
import java.util.Map;

import holon.api.http.Cookies;
import holon.api.http.Request;
import holon.internal.routing.path.Path;

public interface RequestContext extends Request
{
    RequestContext initialize( Path path );

    // Temporary
    @Deprecated
    Path path();

    // Temporary
    @Deprecated
    Map<String, Object> formData() throws IOException;

    // Temporary
    @Deprecated
    Cookies cookies();

    // Temporary
    @Deprecated
    Map<String, Iterable<String>> queryParams();
}
