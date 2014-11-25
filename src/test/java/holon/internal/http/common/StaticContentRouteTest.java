package holon.internal.http.common;

import holon.api.http.Content;
import holon.api.http.Cookies;
import holon.api.http.Request;
import holon.api.http.Status;
import holon.internal.io.ByteArrayOutput;
import holon.internal.routing.path.PatternSegment;
import holon.spi.RequestContext;
import holon.util.io.FileTools;
import holon.util.scheduling.TestScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StaticContentRouteTest
{
    private Path rootDir;
    private Path subDirectory;

    @Before
    public void createFiles() throws IOException
    {
        File file = File.createTempFile( "holon", "static-test" );
        file.delete();
        file.mkdir();
        subDirectory = new File( file, "folder" ).toPath();
        subDirectory.toFile().mkdir();

        File testFile = new File( file, "test.txt" );
        testFile.createNewFile();

        new File( subDirectory.toFile(), "folderTest.txt").createNewFile();

        FileTools.write( testFile, "Hello, world!", Charset.forName( "UTF-8" ) );

        rootDir = file.toPath();
    }

    @After
    public void cleanup() throws IOException
    {
        delete(rootDir.toFile());
    }

    void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            throw new FileNotFoundException("Failed to delete file: " + f);
    }

    @Test
    public void shouldMatchFilesThatExist() throws Exception
    {
        // Given
        StaticContentRoute route = new StaticContentRoute(rootDir, new TestScheduler());

        // When & then
        assertTrue(route.pattern().matches( "/test.txt" ));
        assertTrue( route.pattern().matches( "/folder/folderTest.txt" ) );

        assertFalse( route.pattern().matches( "/nosuch.txt" ) );
        assertFalse(route.pattern().matches( "/folder/nosuch.txt" ));
    }

    @Test
    public void shouldStopTraversalAttacks() throws Exception
    {
        // Given
        StaticContentRoute route = new StaticContentRoute(subDirectory, new TestScheduler());

        // When & then
        assertTrue( route.pattern().matches( "/folderTest.txt" ));
        assertTrue( route.pattern().matches( "/somefolder/../folderTest.txt" ) );
        assertTrue( route.pattern().matches( "/../folder/folderTest.txt" ) );

        assertFalse( route.pattern().matches( "/../test.txt" ) );
    }

    @Test
    public void shouldTransferFile() throws Exception
    {
        // Given
        StaticContentRoute route = new StaticContentRoute(rootDir, new TestScheduler());
        CollectingRequest req = new CollectingRequest("/test.txt");

        // When
        route.call( req );

        // Then
        assertThat(req.status, equalTo(Status.Code.OK));
        assertThat( req.out.toByteArray(), equalTo( "Hello, world!".getBytes( "UTF-8" ) ) );
    }

    private static class CollectingRequest implements RequestContext
    {
        private final holon.internal.routing.path.Path path;
        public Status status;
        public Content content;
        public ByteArrayOutput out = new ByteArrayOutput();

        public CollectingRequest(String path)
        {
            this.path = new PatternSegment.ParamHandlingPath().initialize( path, null );
        }

        @Override
        public void respond( Status status )
        {
            respond(status, null);
        }

        @Override
        public void respond( Status status, Content content )
        {
            respond(status, content, null);
        }

        @Override
        public void respond( Status status, Content content, Object context )
        {
            this.status = status;
            this.content = content;
            try
            {
                content.render( out, context );
            }
            catch ( IOException e )
            {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Request addCookie( String name, String value )
        {
            return null;
        }

        @Override
        public Request addCookie( String name, String value, String path, String domain, int maxAge, boolean secure,
                                  boolean httpOnly )
        {
            return null;
        }

        @Override
        public Request discardCookie( String name )
        {
            return null;
        }

        @Override
        public Request addHeader( String header, String value )
        {
            return null;
        }

        @Override
        public RequestContext initialize( holon.internal.routing.path.Path path )
        {
            return null;
        }

        @Override
        public holon.internal.routing.path.Path path()
        {
            return path;
        }

        @Override
        public Map<String, Object> formData()
        {
            return null;
        }

        @Override
        public Cookies cookies()
        {
            return null;
        }

        @Override
        public Map<String, Iterable<String>> queryParams()
        {
            return null;
        }
    }

}
