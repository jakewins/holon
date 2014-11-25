/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package holon.internal.routing.path;

import holon.spi.RequestContext;
import holon.spi.Route;

import java.util.Arrays;
import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * Pattern segments are used by the {@link holon.internal.routing.path.PathTreeCompiler tree router}, they constitute
 * the nodes in the tree. Each segment represents a path segment, so in the path "/foo/bar/baz", 'foo', 'bar' and 'baz'
 * are segments, and would be represented as {@link holon.internal.routing.path.PatternSegment.StaticPatternSegment}.
 *
 * Some (but not all) segments will have routes associated with them, making them callable. These are always end
 * segments in user-defined paths. Please refer to {@link holon.internal.routing.path.PathTreeCompiler} for details.
 */
public interface PatternSegment
{
    boolean matches(String segment);

    void invoke( String[] allSegments, String path, RequestContext ctx );

    PatternSegment[] children();

    /** Signal that this segment constitutes the end segment of a full route, and is callable */
    boolean isCompleteRoute();

    /** For introspection, access the internal mapping table from named path segments to indexes. */
    Map<String, Integer> pathParamMapping();

    static class StaticPatternSegment implements PatternSegment
    {
        /**
         * End route at this segment, may be null.
         */
        private final Route route;
        private final String segmentValue;
        private final ParamHandlingPath path;
        private final PatternSegment[] children;

        public StaticPatternSegment( Route route, String segmentValue, Map<String,
                Integer> segmentKeys, PatternSegment[] children )
        {
            this.route = route;
            this.children = children;
            this.path = new ParamHandlingPath( segmentKeys );
            this.segmentValue = segmentValue.toLowerCase();
        }

        @Override
        public final boolean matches( String segment )
        {
            return segment.equals( segmentValue );
        }

        @Override
        public final void invoke( String[] allSegments, String fullPath, RequestContext ctx )
        {
            route.call( ctx.initialize( path.initialize( fullPath, allSegments ) ) );
        }

        @Override
        public final PatternSegment[] children()
        {
            return children;
        }

        @Override
        public final boolean isCompleteRoute()
        {
            return route != null;
        }

        @Override
        public final Map<String,Integer> pathParamMapping()
        {
            return path.segmentKeys;
        }

        @Override
        public String toString()
        {
            if(children.length == 0)
            {
                return "Seg[" + segmentValue + "]";
            }
            return "Seg[" + segmentValue + ", children=" + Arrays.toString( children ) + ']';
        }
    }

    static class CatchAllPatternSegment implements PatternSegment
    {
        private final Route route;
        private final ParamHandlingPath path;
        private final PatternSegment[] children;

        public CatchAllPatternSegment( Route route, Map<String, Integer> segmentKeys, PatternSegment[] children )
        {
            this.route = route;
            this.children = children;
            this.path = new ParamHandlingPath(segmentKeys);
        }

        @Override
        public final boolean matches( String segment )
        {
            return true; // Matches everything
        }

        @Override
        public final void invoke( String[] allSegments, String fullPath, RequestContext ctx )
        {
            route.call(ctx.initialize( path.initialize( fullPath, allSegments ) ));
        }

        @Override
        public final PatternSegment[] children()
        {
            return children;
        }

        @Override
        public final boolean isCompleteRoute()
        {
            return route != null;
        }

        @Override
        public final Map<String,Integer> pathParamMapping()
        {
            return path.segmentKeys;
        }

        @Override
        public String toString()
        {
            if(children.length == 0)
            {
                return "Seg[*]";
            }
            return "Seg[*, children=" + Arrays.toString( children ) + ']';
        }
    }

    /** This is the facade we expose to routes when they are invoked. */
    static class ParamHandlingPath implements Path
    {
        private final Map<String, Integer> segmentKeys;
        private String path;
        private String[] segments;

        public ParamHandlingPath()
        {
            this(null);
        }

        public ParamHandlingPath( Map<String, Integer> segmentKeys )
        {
            this.segmentKeys = segmentKeys == null ? emptyMap() : segmentKeys;
        }

        @Override
        public final String param( String key )
        {
            Integer segmentIndex = segmentKeys.get( key );
            if(segmentIndex != null && segments.length > segmentIndex)
            {
                return segments[segmentIndex];
            }
            return null;
        }

        @Override
        public final String fullPath()
        {
            return path;
        }

        public final ParamHandlingPath initialize( String path, String[] segments )
        {
            this.path = path;
            this.segments = segments;
            return this;
        }
    }
}
