package holon.internal.routing.path;

import holon.spi.RequestContext;
import holon.spi.Route;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PathTreeCompilerTest
{

    @Test
    public void shouldCompileOverlappingNamedSegments() throws Exception
    {
        // When & Then
        assertThat( compiledTree(
                        route( "/{id}/name" ),
                        route( "/{id}/age" ) ),
                equalsTree(
                        staticSegment( "get",
                            wildcardSegment(
                                    staticSegment( "name", params( "id", 0 ) ),
                                    staticSegment( "age", params( "id", 0 ) ) ))
                ));
    }

    @Test
    public void shouldCompileOverlappingDifferentlyNamedSegments() throws Exception
    {
        // When & Then
        assertThat( compiledTree(
                        route( "/{id}/name" ),
                        route( "/{banana}/age" ) ),
                equalsTree(
                        staticSegment( "get",
                                wildcardSegment(
                                        staticSegment( "name", params( "id", 0 ) ),
                                        staticSegment( "age", params( "banana", 0 ) ) ))
                ));
    }

    @Test
    public void staticRouteTrumpsWildcard() throws Exception
    {
        // When & Then
        assertThat( compiledTree(
                        route( "/same/{id}" ),
                        route( "/same/specific" ) ),
                equalsTree(
                        staticSegment( "get",
                                staticSegment( "same",
                                        staticSegment( "specific" ),
                                        wildcardSegment( params( "id", 1 ) ) ) )
                ));
    }

    private Route route( String pattern )
    {
        return new Route()
        {
            private PathPattern compiled = BasicPathPattern.compile( pattern );

            @Override
            public String method()
            {
                return "GET";
            }

            @Override
            public PathPattern pattern()
            {
                return compiled;
            }

            @Override
            public void call( RequestContext req )
            {

            }
        };
    }

    private PatternSegment[] compiledTree( Route... routes )
    {
        return new PathTreeCompiler().compile( asList(routes) );
    }

    private Matcher<PatternSegment> wildcardSegment( Matcher<PatternSegment>... children )
    {
        return wildcardSegment( emptyMap(), children );
    }

    private Matcher<PatternSegment> wildcardSegment( Map<String,Integer> pathParams,
            Matcher<PatternSegment>... children )
    {
        return new TypeSafeMatcher<PatternSegment>()
        {
            @Override
            protected boolean matchesSafely( PatternSegment segment )
            {
                return segment instanceof PatternSegment.CatchAllPatternSegment
                       && paramsMatches( segment, pathParams )
                       && segmentsMatches( segment.children(), children );
            }

            private boolean paramsMatches( PatternSegment segment, Map<String,Integer> pathParams )
            {
                assertThat("Expected named path segment mapping to match.", segment.pathParamMapping(),
                        equalTo( pathParams ) );
                return true;
            }

            @Override
            public void describeTo( Description description )
            {
                if(children.length > 0)
                {
                    description.appendList( "Seg[*, children=[", ",", "]]", asList(children) );
                }
                else
                {
                    description.appendText( "Seg[*]" );
                }
            }
        };
    }


    private Matcher<PatternSegment> staticSegment(String segmentStr, Matcher<PatternSegment> ... children )
    {
        return staticSegment( segmentStr, emptyMap(), children );
    }

    private Matcher<PatternSegment> staticSegment(String segmentStr, Map<String, Integer> pathParams,
            Matcher<PatternSegment> ... children )
    {
        return new TypeSafeMatcher<PatternSegment>()
        {
            @Override
            protected boolean matchesSafely( PatternSegment segment )
            {
                return segment instanceof PatternSegment.StaticPatternSegment
                       && segment.matches( segmentStr ) && paramsMatches( segment, pathParams )
                       && segmentsMatches( segment.children(), children );
            }

            private boolean paramsMatches( PatternSegment segment, Map<String,Integer> pathParams )
            {
                assertThat("Expected named path segment mapping to match.", segment.pathParamMapping(),
                        equalTo( pathParams ) );
                return true;
            }

            @Override
            public void describeTo( Description description )
            {
                if(children.length > 0)
                {
                    description.appendList( "Seg[" + segmentStr + ", children=[", ",", "]]", asList( children ) );
                }
                else
                {
                    description.appendText( "Seg[" + segmentStr + "]" );
                }
            }
        };
    }

    private boolean segmentsMatches( PatternSegment[] segments, Matcher<PatternSegment>[] children )
    {
        if(segments.length != children.length)
        {
            return false;
        }
        for ( int i = 0; i < children.length; i++ )
        {
            if(!children[i].matches( segments[i] ))
            {
                return false;
            }
        }
        return true;
    }

    private Matcher<PatternSegment[]> equalsTree( Matcher<PatternSegment> ... tree )
    {
        return new TypeSafeMatcher<PatternSegment[]>()
        {
            @Override
            protected boolean matchesSafely( PatternSegment[] segments )
            {
                return segmentsMatches(segments, tree);
            }

            @Override
            public void describeTo( Description description )
            {
                description.appendList( "[", ",", "]", asList( tree ) );
            }
        };
    }


    private Map<String,Integer> params( Object ... alternatingKeyAndIndex )
    {
        Map<String, Integer> map = new HashMap<>();
        for ( int i = 0; i < alternatingKeyAndIndex.length; i+=2 )
        {
            map.put( (String)alternatingKeyAndIndex[i], (Integer)alternatingKeyAndIndex[i+1] );
        }
        return map;
    }

}