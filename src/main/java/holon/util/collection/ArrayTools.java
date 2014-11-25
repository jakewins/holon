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
package holon.util.collection;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ArrayTools
{
    public static <T> T[] concat (T[] first, T[] second)
    {
        @SuppressWarnings("unchecked")
        T[] C = (T[]) Array.newInstance( first.getClass().getComponentType(), first.length + second.length );
        System.arraycopy(first, 0, C, 0, first.length );
        System.arraycopy(second, 0, C, first.length, second.length );

        return C;
    }

    public static <T> T[] reverse(T[] input)
    {
        for(int i = 0; i < input.length / 2; i++)
        {
            T temp = input[i];
            input[i] = input[input.length - i - 1];
            input[input.length - i - 1] = temp;
        }
        return input;
    }

    public static <T> List<T> reverse(List<T> input)
    {
        for(int i = 0; i < input.size() / 2; i++)
        {
            T temp = input.get(i);
            input.set(i, input.get(input.size() - i - 1));
            input.set(input.size() - i - 1, temp);
        }
        return input;
    }

    public static <T> T[] toArray( List<T> list )
    {
        return (T[]) list.toArray();
    }

    public static <IN, OUT> List<OUT> map( List<IN> input, Function<IN, OUT> func )
    {
        List<OUT> out = new ArrayList<>(input.size());
        input.forEach( (i) -> out.add( func.apply(i) ) );
        return out;
    }
}
