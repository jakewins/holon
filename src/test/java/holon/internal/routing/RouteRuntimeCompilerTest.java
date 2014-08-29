package holon.internal.routing;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import holon.api.http.POST;
import holon.api.http.PostParam;
import holon.api.http.Request;
import holon.internal.di.Components;
import holon.internal.routing.annotated.RouteCompiler;
import holon.spi.Route;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class RouteRuntimeCompilerTest
{
    private static final AtomicReference<Object> actualData = new AtomicReference<>( null );

    public static class Endpoint
    {
        @POST("/")
        public void post( Request req, @PostParam Map<String, String> data )
        {
            actualData.set( data );
        }

        @POST("/attr")
        public void postAttribute( Request req, @PostParam("attr") String data )
        {
            actualData.set( data );
        }
    }

    @Test
    public void shouldProvidePostData() throws Exception
    {
        // Given
        Method method = Endpoint.class.getMethod( "post", Request.class, Map.class );
        Route route = new RouteCompiler( new Components() ).compile( "",
                Endpoint.class, method, method.getAnnotation( POST.class ) );
        HashMap<String, String> data = new HashMap<>();

        // When
        route.call( new TestRequest("/", data ) );

        // Then
        assertThat( actualData.get(), equalTo( data ) );
    }

    @Test
    public void shouldProvidePostDataAttribute() throws Exception
    {
        // Given
        Method method = Endpoint.class.getMethod( "postAttribute", Request.class, String.class );
        Route route = new RouteCompiler( new Components() ).compile( "",
                Endpoint.class, method, method.getAnnotation( POST.class ) );
        HashMap<String, String> data = new HashMap<>();
        data.put( "attr", "success" );

        // When
        route.call( new TestRequest("/attr", data ) );

        // Then
        assertThat( actualData.get(), equalTo( "success" ) );
    }
}
