package holon.internal.http.common;

import holon.api.http.Request;
import holon.spi.Route;

import static holon.api.http.Status.Code.NOT_FOUND;

public class FourOhFourRoute implements Route
{
    @Override
    public String method()
    {
        return null;
    }

    @Override
    public PathPattern pattern()
    {
        return null;
    }

    @Override
    public void call( Request req )
    {
        req.respond( NOT_FOUND );
    }
}
