package holon.internal.http.common;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import holon.api.exception.HolonException;
import holon.api.http.Content;
import holon.api.http.Status;
import holon.internal.http.common.files.FileRepository;
import holon.spi.RequestContext;
import holon.spi.Route;
import holon.util.scheduling.Scheduler;

import static holon.internal.routing.HttpMethod.Standard.GET;

public class StaticContentRoute implements Route
{
    private final FileRepository files;

    public StaticContentRoute( Path staticContentFolder, Scheduler scheduler )
    {
        FileSystem fs = staticContentFolder.getFileSystem();
        ensurePublicDirExists( staticContentFolder );
        files = new FileRepository( staticContentFolder, fs, scheduler );
    }

    private void ensurePublicDirExists( Path staticContentFolder )
    {
        if(!Files.exists( staticContentFolder ))
        {
            try
            {
                Files.createDirectories( staticContentFolder );
            }
            catch ( IOException e )
            {
                throw new HolonException( "Failed to create public directory at " + staticContentFolder.toString() );
            }
        }
    }

    @Override
    public String method()
    {
        return GET.name();
    }

    @Override
    public PathPattern pattern()
    {
        return new PathPattern()
        {
            @Override
            public boolean isDynamic()
            {
                return true;
            }

            @Override
            public String pattern()
            {
                return "/*";
            }

            @Override
            public boolean matches( String path )
            {
                return files.contains( path );
            }
        };
    }

    @Override
    public void call( RequestContext req )
    {
        Content file = files.get( req.path().fullPath() );
        if(file != null)
        {
            req.respond( Status.Code.OK, file );
        }
        else
        {
            req.respond( Status.Code.NOT_FOUND );
        }
    }
}
