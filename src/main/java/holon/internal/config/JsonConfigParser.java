package holon.internal.config;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import holon.api.config.Config;
import holon.api.exception.InvalidConfigurationException;

public class JsonConfigParser
{
    public Config parse( URL jsonURL )
    {
        ObjectMapper mapper = new ObjectMapper();
        try
        {
            JsonNode json = mapper.getJsonFactory().createParser( jsonURL ).readValueAsTree();
            Map<String, Object> map = (Map<String, Object>) treeToMap( json );
            return new MapConfig( map );
        }
        catch ( IOException e )
        {
            throw new InvalidConfigurationException("Unable to parse configuration file '" + jsonURL.toString() + "'.", e);
        }
    }

    private Object treeToMap( JsonNode node )
    {
        if(node.isObject())
        {
            Map<String, Object> out = new HashMap<>();
            Iterator<String> names = node.fieldNames();
            while(names.hasNext())
            {
                String next = names.next();
                out.put( next, treeToMap( node.get( next ) ) );
            }
            return out;
        }
        else if(node.isValueNode())
        {
            return node.asText();
        }
        else if(node.isArray())
        {
            ArrayList<Object> out = new ArrayList<>();
            Iterator<JsonNode> elements = node.elements();
            while(elements.hasNext())
            {
                out.add( treeToMap( elements.next() ) );
            }
            return out;
        }
        else
        {
            throw new IllegalArgumentException( "Unknown json value: " + node );
        }
    }
}
