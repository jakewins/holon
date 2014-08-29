package holon.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.readAllBytes;

public class FileTools
{

    public static void write( File file, String contents, Charset encoding ) throws IOException
    {
        Files.write( file.toPath(), contents.getBytes( encoding ) );
    }

    public static String read( File file, Charset encoding ) throws IOException
    {
        return new String( readAllBytes( file.toPath() ), encoding);
    }

    /**
     * Find files recursively in a directory, matching a {@link java.nio.file.PathMatcher glob} path pattern.
     */
    public static List<File> findFiles( File dir, String pattern )
    {
        List<File> out = new ArrayList<>();
        findFiles( dir, FileSystems.getDefault().getPathMatcher( "glob:" + pattern ), out );
        return out;
    }

    private static void findFiles( File dir, PathMatcher predicate, List<File> output )
    {
        for ( File file : dir.listFiles() )
        {
            if(file.isDirectory())
            {
                findFiles( file, predicate, output );
            }
            else
            {
                if(predicate.matches( file.toPath() ))
                {
                    output.add( file );
                }
            }
        }
    }
}
