package holon.internal.http.common.files;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Map;

import holon.api.http.Content;
import holon.api.io.Output;
import holon.internal.io.ContentTypes;

/**
 * Used by the file repository to transfer static files.
 *
 * This class is single-threaded, it's expected that each backend keeps it's own repository of files - no reason to
 * use shared memory for the file handles, since that's just metadata.
 */
class FileContent implements Content
{
    private final FileChannel channel;
    private final String type;

    public FileContent( Path path, FileChannel channel )
    {
        this.channel = channel;
        this.type = determineType(path);
    }

    private String determineType( Path path )
    {
        String[] split = path.toUri().getRawPath().split( "\\." );
        return ContentTypes.contentTypeForSuffix( split[split.length-1] );
    }

    @Override
    public void render( Map<String, Object> ctx, Output out ) throws IOException
    {
        channel.position(0);
        out.write( channel );
    }

    @Override
    public String type()
    {
        return type;
    }

    public void close() throws IOException
    {
        channel.close();
    }
}
