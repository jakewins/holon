package holon.internal.routing;

import java.util.ArrayList;
import java.util.List;

import holon.internal.di.Components;
import holon.internal.routing.annotated.RouteCompiler;
import holon.internal.routing.annotated.RouteScanner;
import holon.spi.Route;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class RouteScannerTest
{
    @Test
    public void shouldFindRoutes() throws Exception
    {
        // Given
        RouteScanner discoverer = new RouteScanner( new RouteCompiler( new Components() ) );

        List<Route> routes = new ArrayList<>();

        // When
        discoverer.scan( "/myroot", "holon.internal.routing.testroutes", routes::add );

        // Then
        Assert.assertThat( routes.size(), Matchers.equalTo( 1 ) );
        Assert.assertThat( routes.get( 0 ).pattern(), Matchers.equalTo( "/myroot/hello/world" ) );
        Assert.assertThat( routes.get( 0 ).method(), Matchers.equalTo( HttpMethod.Standard.GET ) );
    }
}
