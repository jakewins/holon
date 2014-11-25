package holon.internal.http.common.files;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchEvent;

import holon.api.exception.HolonException;
import holon.api.http.Content;
import holon.util.collection.LRUMap;
import holon.util.io.FileSystemWatcher;
import holon.util.scheduling.Scheduler;

/**
 * Keeps a fixed amount of file handles open - files will be removed from the repo according to an LRU strategy, or
 * if the files are change or are removed on disk.
 */
public class FileRepository implements FileSystemWatcher.FSEventHandler
{
    private final Path basePath;

    /** Cached file handles */
    private final LRUMap<String, FileContent> openFiles = new LRUMap<>( 1024, 1.0f, ( p, f ) ->
    {
        try
        {
            f.close();
        }
        catch ( IOException e )
        {
            throw new HolonException( "Failed to close static file.", e );
        }
    });

    /**
     * File events come from a separate thread, and the openFiles map is owned by the thread that created this
     * file repo, meaning the event thread is not allowed to manipulate it. Instead, it signals through this boolean
     * that files have changed on disk.
     */
    private volatile boolean filesHaveChangedOnDisk = false;

    public FileRepository( Path basePath, FileSystem fs, Scheduler scheduler )
    {
        this.basePath = basePath.normalize();
        scheduler.schedule( new FileSystemWatcher(fs, basePath, this) );
    }

    public boolean contains( String path )
    {
        if(openFiles.containsKey( path ))
        {
            return true;
        }
        else
        {
            Path normalized = normalizedPath( path );
            return normalized.startsWith( basePath ) && Files.exists( normalized );
        }
    }

    public Content get( String path )
    {
        if(filesHaveChangedOnDisk)
        {
            openFiles.clear();
        }

        FileContent content = openFiles.get( path );
        if( content == null )
        {
            try
            {
                Path normalized = normalizedPath( path );
                content = new FileContent( normalized, FileChannel.open( normalized, StandardOpenOption.READ ) );
                openFiles.put( path, content );
            }
            catch ( IOException e )
            {
                throw new HolonException( "Failed to open static file: '" + path + "'.", e );
            }
        }
        return content;
    }

    private Path normalizedPath( String path )
    {
        return basePath.resolve( path.substring( 1 ) ).normalize();
    }

    @Override
    public void onFileEvent( WatchEvent.Kind kind, Path path )
    {
        filesHaveChangedOnDisk = true;
    }
}
