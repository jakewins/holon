package holon.util.collection;

import java.util.HashMap;
import java.util.Map;

public class Maps
{
    public static <K,V> Map<K, V> map( Object ... alternatingKeyAndValue )
    {
        Map out = new HashMap<>(alternatingKeyAndValue.length / 2);
        int i = 0;
        while ( i < alternatingKeyAndValue.length )
        {
            out.put( alternatingKeyAndValue[i++], alternatingKeyAndValue[i++] );
        }
        return out;
    }
}
