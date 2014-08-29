package holon.util.collection;

import java.util.HashMap;
import java.util.Map;

public class Maps
{
    public static Map<String, Object> map( Object ... alternatingKeyAndValue )
    {
        Map<String, Object> out = new HashMap<>(alternatingKeyAndValue.length / 2);
        int i = 0;
        while ( i < alternatingKeyAndValue.length )
        {
            out.put( (String) alternatingKeyAndValue[i++], alternatingKeyAndValue[i++] );
        }
        return out;
    }
}
