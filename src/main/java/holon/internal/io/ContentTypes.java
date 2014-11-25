package holon.internal.io;

import java.util.HashMap;
import java.util.Map;

public class ContentTypes
{
    private static final Map<String, String> suffixToContentType = new HashMap<>();

    static
    {
        suffixToContentType.put( "css",  "text/css" );
        suffixToContentType.put( "html", "text/html" );
        suffixToContentType.put( "js",   "application/javascript" );
        suffixToContentType.put( "json", "application/json" );
    }

    public static String contentTypeForSuffix( String suffix )
    {
        String type = suffixToContentType.get( suffix );
        return type == null ? "text/plain" : type;
    }

}
