package holon.spi;

import holon.api.http.Cookies;
import holon.api.http.Request;
import holon.api.http.RequestHeaders;
import holon.internal.routing.path.Path;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Map;

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
    RequestHeaders headers();

    // Temporary
    @Deprecated
    Cookies cookies();

    // Temporary
    @Deprecated
    Map<String, Iterable<String>> queryParams();

    // Temporary
    @Deprecated
    default SocketAddress remoteAddress() { return null; }
}
