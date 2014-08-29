package holon.util.collection;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

public class LRUMapTest
{
    @Test
    public void shouldSetAndGet() throws Exception
    {
        // Given
        Map<String, Boolean> map = new LRUMap<>( 16 );

        // When
        map.put( "one", true );
        map.put( "two", true );
        map.put( "three", false );

        // Then
        assertTrue(map.containsKey( "one" ));
        assertTrue(map.containsKey( "two" ));
        assertTrue(map.containsKey( "three" ));
        assertFalse(map.containsKey( "twoAndAHalf" ));

        assertTrue(map.get("one"));
        assertTrue(map.get("two"));
        assertFalse(map.get("three"));
    }

    @Test
    public void shouldPutAndRemove() throws Exception
    {
        // Given
        Map<String, Boolean> map = new LRUMap<>( 16 );

        map.put( "one", true );
        map.put( "three", false );

        // When
        map.remove( "one" );

        // Then
        assertFalse( map.containsKey( "one" ) );
        assertTrue(map.containsKey( "three" ));
        assertFalse(map.containsKey( "twoAndAHalf" ));

        assertNull( map.get( "one" ) );
        assertFalse(map.get("three"));
    }

    @Test
    public void shouldEvictOldEntries() throws Exception
    {
        // Given
        Map<String, Boolean> evicted = new HashMap<>();
        Map<String, Boolean> map = new LRUMap<>( 8, 1.0f, evicted::put );

        // When
        for ( int i = 0; i < 128; i++ )
        {
            map.put( "key-" + i, i > 64 );
        }

        // Then
        assertThat(map.size(), equalTo(8));
        assertThat(evicted.size(), equalTo(120));
    }
}
