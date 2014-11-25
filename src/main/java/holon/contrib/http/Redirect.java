package holon.contrib.http;

import holon.api.http.Request;
import holon.api.http.Status;

public class Redirect
{
    public static void redirect(Request req, String location)
    {
        req.addHeader( "Location", location ).respond( Status.Code.SEE_OTHER );
    }
}
