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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PathPatternParser
{
    public Iterable<ParsedSegment> parse( String pattern )
    {
        // Dead-simple for now, no support for escape sequences
        List<ParsedSegment> segments = new ArrayList<>();

        pattern = pattern.trim();

        if(pattern.startsWith( "/" ))
        {
            pattern = pattern.substring( 1 );
        }

        if(pattern.endsWith( "/" ))
        {
            pattern = pattern.substring( 0, pattern.length() - 1 );
        }

        if(pattern.length() == 0)
        {
            return Collections.emptyList();
        }

        for ( String s : pattern.split( "/" ) )
        {
            if(s.startsWith( "{" ) && s.endsWith( "}" ))
            {
                segments.add( new NamedWildcardSegment( s.substring( 1, s.length() - 1 ) ) );
            }
            else
            {
                segments.add( new StaticParsedSegment( s ));
            }
        }
        return segments;
    }

    public static interface ParsedSegment
    {
        boolean isWildcard();

        String key();
    }

    public static class StaticParsedSegment implements ParsedSegment
    {
        private final String segmentString;

        public StaticParsedSegment( String segmentString )
        {
            this.segmentString = segmentString;
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }
            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }

            StaticParsedSegment that = (StaticParsedSegment) o;

            if ( segmentString != null ? !segmentString.equals( that.segmentString ) : that.segmentString != null )
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            return segmentString != null ? segmentString.hashCode() : 0;
        }

        @Override
        public boolean isWildcard()
        {
            return false;
        }

        @Override
        public String key()
        {
            return segmentString;
        }

        @Override
        public String toString()
        {
            return String.format("Static[%s]", segmentString);
        }
    }

    public static class NamedWildcardSegment implements ParsedSegment
    {
        private final String key;

        public NamedWildcardSegment( String key )
        {
            this.key = key;
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }
            return !(o == null || getClass() != o.getClass());
        }

        @Override
        public int hashCode()
        {
            return 0;
        }

        @Override
        public boolean isWildcard()
        {
            return true;
        }

        @Override
        public String key()
        {
            return key;
        }

        @Override
        public String toString()
        {
            return String.format("Named[%s]", key);
        }
    }
}
