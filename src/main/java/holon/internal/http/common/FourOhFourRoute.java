package holon.internal.http.common;

import holon.spi.RequestContext;
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
    public void call( RequestContext req )
    {
        req.respond( NOT_FOUND );
    }
}
