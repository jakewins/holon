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

import holon.spi.Route;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static holon.internal.routing.path.PathPatternParser.ParsedSegment;
import static holon.internal.routing.path.PathPatternParser.StaticParsedSegment;

/**
 * This builds a tree structure of path patterns, given a set of routes. Each segment of the path becomes an entry
 * in the tree, and the last segment of a path can be called to invoke the route. For instance, two endpoints with
 * the following paths:
 *
 * /my/{id}/path
 * /my/{name}/pooch
 * /my/other/{id}/path
 *
 * Becomes the following tree:
 *
 *               (my)
 *                /\
 *               /  \
 *         (other)   (*)
 *                   /\
 *                  /  \
 *             (path)  (pooch)
 *
 * Where the last segments are callable, and the "*" represents dynamic catch-all segments. For routes that have these
 * catch all segments, invoking the last segment will provide a proper path object to the request context before
 * invoking the route itself.
 *
 * The invokable segments have, if they contain 'dynamic' segments, mappings from the key the route
 * specified to the index of that segment. In this case, both '/my/{id}/path' and '/my/{name}/pooch' will have such
 * mappings. For the 'path' route, it would map 'id' to segment 2 (index 1), and the 'pooch' route will map 'name' to
 * the same segment. Because a path is represented, when routing, as a primitive string array,
 * performing lookups of named
 * path values becomes very fast.
 */
public class PathTreeCompiler
{
    private final PathPatternParser patternParser = new PathPatternParser();

    private static class PatternSegmentBuilder
    {
        private final List<PatternSegmentBuilder> children = new ArrayList<>();
        private final ParsedSegment segment;

        // Route and its associated dynamic segments (eg. the 'key' in a pattern like /path/{key}/hello).
        private Map<String, Integer> segmentKeys;
        private Route route;

        public PatternSegmentBuilder( ParsedSegment segment )
        {
            this.segment = segment;
        }

        public PatternSegment build()
        {
            if(segment.isWildcard())
            {
                return new PatternSegment.CatchAllPatternSegment( route, segmentKeys, buildChildren() );
            }
            else
            {
                return new PatternSegment.StaticPatternSegment( route, segment.key(), segmentKeys, buildChildren() );
            }
        }

        public PatternSegment[] buildChildren()
        {
            PatternSegment[] childSegments = new PatternSegment[children.size()];
            for ( int i = 0; i < children.size(); i++ )
            {
                childSegments[i] = children.get( i ).build();
            }

            // Sort the children such that static segments take precedence over dynamic segments
            Arrays.sort( childSegments, ( seg1, seg2 ) -> {
                if(seg1 instanceof PatternSegment.StaticPatternSegment)
                {
                    if(seg2 instanceof PatternSegment.CatchAllPatternSegment)
                    {
                        return -1;
                    }
                    // both are static, order them equal. May want to be smarter here later on
                    return 0;
                }
                if(seg2 instanceof PatternSegment.StaticPatternSegment)
                {
                    // seg1 is catch all, seg2 is static, seg2 should go higher
                    return 1;
                }
                // both are catchall, order does not matter (also, this should not be possible)
                return 0;
            });

            return childSegments;
        }

        public PatternSegmentBuilder getOrCreateSegment( ParsedSegment segment )
        {
            for ( PatternSegmentBuilder child : children )
            {
                if(child.matches(segment))
                {
                    return child;
                }
            }

            PatternSegmentBuilder seg = new PatternSegmentBuilder( segment );
            children.add( seg );
            return seg;
        }

        private boolean matches( ParsedSegment segment )
        {
            return this.segment.equals( segment );
        }

        public void setRoute( Route route, Map<String, Integer> dynamicSegments )
        {
            this.route = route;
            this.segmentKeys = dynamicSegments;
        }
    }

    public PatternSegment[] compile( Iterable<Route> routes )
    {
        PatternSegmentBuilder root = new PatternSegmentBuilder( null );
        routes.forEach( ( r ) -> {
            if(r.pattern().isDynamic())
            {
                // Ignore these as they are implemented as java code, and we are not allowed to
                // try and be smart about how to route to these types of paths.
                return;
            }

            Iterable<ParsedSegment> pp = patternParser.parse( r.pattern().pattern() );
            Map</*route-specified name */String, /* segment index */Integer> dynamicSegments = new HashMap<>();

            PatternSegmentBuilder current = root.getOrCreateSegment( new StaticParsedSegment( r.method() ) );
            int segmentIndex = 0;
            for ( ParsedSegment parsedSegment : pp )
            {
                current = current.getOrCreateSegment( parsedSegment );
                if ( parsedSegment.isWildcard() )
                {
                    dynamicSegments.put( parsedSegment.key(), segmentIndex );
                }
                segmentIndex++;
            }

            current.setRoute( r, dynamicSegments );
        });

        return root.buildChildren();
    }

}
