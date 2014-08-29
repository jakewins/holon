package holon.internal;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import holon.Holon;
import holon.api.config.Config;
import holon.api.exception.HolonException;
import holon.util.collection.Maps;

public class HolonFiles
{
    public static final String DEFAULT_CONFIG_NAME = "holon.json";

    public static String defaultHome(Config config)
    {
        return config.get( Holon.Configuration.config_path).getParent().toAbsolutePath().toString();
    }

    public static String defaultTemplatePath( Config config )
    {
        return config.get( Holon.Configuration.home_dir ).resolve( "template" ).toAbsolutePath().toString();
    }

    public static Map<String, Object> defaultEndpointPackages( Config config )
    {
        return Maps.map( "", String.format( "%s.web.endpoint", config.get( Holon.Configuration.app_name ) ) );
    }

    public static String defaultStaticPath( Config config )
    {
        return config.get( Holon.Configuration.home_dir ).resolve( "public" ).toAbsolutePath().toString();
    }


    /** If no working dir was specified, traverse the filesystem to try and find it. */
    public static File findHomeDirectory()
    {
        File dir = new File( "." );
        while(true)
        {
            for ( File file : dir.listFiles() )
            {
                if ( file.getName().equals( DEFAULT_CONFIG_NAME ) )
                {
                    try
                    {
                        return dir.getCanonicalFile();
                    }
                    catch ( IOException e )
                    {
                        throw new HolonException( "Unable to determine home directory due to IO error.", e);
                    }
                }
            }

            dir = dir.getParentFile();
            if(dir == null)
            {
                break;
            }
        }

        String canonicalPath;
        try
        {
            canonicalPath = new File( "." ).getCanonicalPath();
        }
        catch ( IOException e )
        {
            canonicalPath = new File( "." ).getAbsolutePath();
        }
        throw new HolonException( "Unable to determine working dir. Either specify one, or create a holon.json file " +
                "in '"+ canonicalPath +"'.", null );
    }
}
