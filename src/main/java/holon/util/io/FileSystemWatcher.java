package holon.util.io;

import holon.util.scheduling.Scheduler;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

/**
 * Triggers a callback whenever a change occurs in a specified folder.
 */
public class FileSystemWatcher implements Scheduler.Job
{
    /*
     * Internal notes: The WatchService API is great and all, but it is exceptionally slow in reporting changes.
     * Because of this, we use both the WatchService API (to get notified when files are created), as well as our own
     * polling (to get faster notifications when files change).
     */
    @FunctionalInterface
    public interface FSEventHandler
    {
        void onFileEvent( WatchEvent.Kind kind, Path path );
    }

    private final FSEventHandler handler;
    private final Path basePath;
    private final WatchService watchService;

    /** This tracks the highest found modification time in the file tree we're monitoring, to check for changes.  */
    private Map<Path, Long> modificationTimes = new HashMap<>();

    private volatile boolean stopped = true;

    public FileSystemWatcher( FileSystem fs, Path basePath, FSEventHandler handler )
    {
        this.handler = handler;
        try
        {
            this.basePath = basePath;
            this.watchService = fs.newWatchService();
        }
        catch ( IOException e)
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void run()
    {
        stopped = false;
        try
        {
            registerAll( basePath );

            boolean valid = true;
            while(valid && !stopped)
            {
                WatchKey key = watchService.poll();
                if(key != null)
                {
                    key.pollEvents().forEach( ( e ) -> {
                        if(e.kind() == StandardWatchEventKinds.ENTRY_DELETE)
                        {
                            modificationTimes.remove( e.context() );
                        }
                        handler.onFileEvent( e.kind(), (Path) e.context() );
                    } );
                }
                else
                {
                    pollModifications();
                }

                Thread.sleep( 500 );
                valid = key == null || key.reset();
            }
        }
        catch(Throwable e)
        {
            e.printStackTrace();
            stopped = true;
        }
    }

    private void registerAll(final Path start) throws IOException {
        Files.walkFileTree( start, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs )
                    throws IOException
            {
                dir.register( watchService, ENTRY_CREATE, ENTRY_DELETE );
                return FileVisitResult.CONTINUE;
            }
        } );
    }

    private void pollModifications() throws IOException
    {
        try
        {
            Files.walkFileTree( basePath, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException
                {
                    long time = attrs.lastModifiedTime().toMillis();
                    Long lastTime = modificationTimes.get( file );
                    if ( lastTime == null )
                    {
                        // Don't trigger event, WatchService will have picked this up if it is a new file created
                        modificationTimes.put( file, time );
                    }
                    else
                    {
                        if ( lastTime < time )
                        {
                            modificationTimes.put( file, time );
                            handler.onFileEvent( StandardWatchEventKinds.ENTRY_MODIFY, basePath.relativize( file ) );
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            } );
        }
        catch( NoSuchFileException e)
        {
            // ok
        }
    }

    @Override
    public void stop()
    {
        stopped = true;
    }
}
