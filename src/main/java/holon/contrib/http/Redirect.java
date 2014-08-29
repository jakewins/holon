package holon.contrib.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import holon.api.http.Content;
import holon.api.io.Output;

public class Redirect
{
    public static Content redirect(String location)
    {
        final Map<String, String> headers = new HashMap<>();
        headers.put( "Location", location );

        return new Content()
        {
            @Override
            public void render( Map<String, Object> ctx, Output out ) throws IOException
            {

            }

            @Override
            public Map<String, String> headers()
            {
                return headers;
            }
        };
    }
}
