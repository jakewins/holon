package holon.internal.http.common;

import holon.api.exception.HolonException;
import holon.api.http.Status;
import holon.internal.http.common.files.FileContent;
import holon.internal.http.common.files.FileRepository;
import holon.spi.RequestContext;
import holon.spi.Route;
import holon.util.scheduling.Scheduler;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static holon.internal.routing.HttpMethod.Standard.GET;
import static io.netty.handler.codec.http.HttpHeaders.Names;

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
        String etag = req.headers().getFirst( Names.IF_NONE_MATCH );
        String cacheControl = req.headers().getFirst( Names.CACHE_CONTROL );
        FileContent file = files.get( req.path().fullPath() );
        if(file != null)
        {
            if(etag != null
               && (cacheControl == null || !cacheControl.equalsIgnoreCase( "no-cache" ))
               && file.etag().equals(etag))
            {
                req.respond( Status.Code.NOT_MODIFIED );
            }
            else
            {
                req.addHeader( Names.ETAG, file.etag() );
                req.addHeader( Names.CACHE_CONTROL, "public, max-age=86400" );
                req.respond( Status.Code.OK, file );
            }
        }
        else
        {
            req.respond( Status.Code.NOT_FOUND );
        }
    }
}
