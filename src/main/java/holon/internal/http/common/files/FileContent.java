package holon.internal.http.common.files;

import holon.api.http.Content;
import holon.api.http.Output;
import holon.internal.io.ContentTypes;
import holon.util.Digest;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * Used by the file repository to transfer static files.
 *
 * This class is single-threaded, it's expected that each backend keeps it's own repository of files - no reason to
 * use shared memory for the file handles, since that's just metadata.
 */
public class FileContent implements Content
{
    private final Path path;
    private final FileChannel channel;
    private final String type;
    private final String etag;

    public FileContent( Path path, FileChannel channel ) throws IOException
    {
        this.path = path;
        this.channel = channel;
        this.type = determineType(path);
        this.etag = channel == null ? Digest.md5( "" ) : Digest.md5( channel );
    }

    public FileContent( FileChannel channel ) throws IOException
    {
        this.channel = channel;
        this.type = "text/plain";
        this.path = null;
        this.etag = channel == null ? Digest.md5( "" ) : Digest.md5( channel );
    }

    @Override
    public void render( Output out, Object context ) throws IOException
    {
        channel.position(0);
        out.write( channel );
    }

    @Override
    public String contentType( Object context )
    {
        return type;
    }

    public String etag()
    {
        return etag;
    }

    private String determineType( Path path )
    {
        String[] split = path.toUri().getRawPath().split( "\\." );
        return ContentTypes.contentTypeForSuffix( split[split.length-1] );
    }

    public void close() throws IOException
    {
        channel.close();
    }

    @Override
    public String toString()
    {
        return String.format("FileContent['%s', %s]", path, type);
    }
}
