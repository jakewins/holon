package holon.util.collection;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * A single-threaded map that keeps n items, removing items using the LRU-k algorithm when it fills up. It uses
 * the standard bucket-table approach used in j.u.c. Converting it to use open addressing
 * would be an excellent exercise for the future.
 *
 * Because of the LRU nature of the map, it does not do resizing.
 */
public class LRUMap<K, V> implements Map<K, V>
{
    private final LRUEntry<K,V>[] buckets;
    private final BiConsumer<K, V> removeHandler;

    private int size = 0;

    // Head of freelist
    private LRUEntry<K,V> nextFree;

    // LRU-K variables
    private int            clockBucket = 0;
    private LRUEntry<K, V> clockEntry;
    private LRUEntry<K, V> prevClockEntry;

    public LRUMap( int slots, float loadFactor, BiConsumer<K,V> removeHandler )
    {
        if(Integer.bitCount( slots ) != 1)
        {
            throw new IllegalArgumentException( "Map slots must be a power-of-two number." );
        }
        this.removeHandler = removeHandler;
        this.buckets = new LRUEntry[slots];

        int maxItems = Math.round( slots * loadFactor );
        for ( int i = 0; i < maxItems; i++ )
        {
            nextFree = new LRUEntry<>( null, null, nextFree );
        }
    }

    public LRUMap( int slots )
    {
        this(slots, 0.6f, (a,b)->{});
    }

    @Override
    public int size()
    {
        return size;
    }

    @Override
    public boolean isEmpty()
    {
        return size() == 0;
    }

    @Override
    public boolean containsKey( Object key )
    {
        return findEntry( key ) != null;
    }

    @Override
    public boolean containsValue( Object value )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public V get( Object key )
    {
        LRUEntry<K, V> entry = findEntry( key );
        if(entry != null)
        {
            if(entry.usageCount < 5)
            {
                entry.usageCount++;
            }
            return entry.value;
        }
        return null;
    }

    @Override
    public V put( K key, V value )
    {
        V oldValue;
        int index = index( key );
        LRUEntry<K,V> entry = buckets[index];

        while(entry != null && entry.next != null)
        {
            if(entry.key.equals( key ))
            {
                oldValue = entry.value;
                entry.value = value;
                entry.usageCount = 1;
                return oldValue;
            }
            entry = entry.next;
        }

        // If we get here, no such key in the chain
        entry = newEntry( key, value );
        entry.next = buckets[index];
        buckets[index] = entry;
        return null;
    }

    private LRUEntry<K, V> newEntry( K key, V value )
    {
        while(nextFree == null)
        {
            sweep();
        }
        LRUEntry<K, V> entry = nextFree;
        nextFree = entry.next;
        size++;
        return entry.initialize( key, value, null );
    }

    private void sweep()
    {
        for (;;)
        {
            if(clockEntry == null || clockEntry.usageCount == -1)
            {
                clockBucket++;
                if( clockBucket >= buckets.length)
                {
                    clockBucket = 0;
                }
                clockEntry = buckets[clockBucket];
                prevClockEntry = null;
            }
            else
            {
                if(clockEntry.usageCount == 0)
                {
                    if(prevClockEntry != null)
                    {
                        prevClockEntry.next = clockEntry.next;
                    }
                    else
                    {
                        buckets[clockBucket] = clockEntry.next;
                    }
                    removeHandler.accept( clockEntry.key, clockEntry.value );

                    LRUEntry<K, V> next = clockEntry.next;
                    addToFreelist( clockEntry );
                    clockEntry = next;
                    size--;
                    return;
                }
                else
                {
                    clockEntry.usageCount--;
                }

                prevClockEntry = clockEntry;
                clockEntry = clockEntry.next;
            }
        }
    }

    @Override
    public V remove( Object key )
    {
        int index = index( key );
        LRUEntry<K,V> entry = buckets[index],
                   previous = null;

        while(entry != null)
        {
            if(entry.key.equals( key ))
            {
                if(previous != null)
                {
                    previous.next = entry.next;
                }
                else
                {
                    buckets[index] = entry.next;
                }
                size--;
                addToFreelist( entry );

                removeHandler.accept( entry.key, entry.value );

                return entry.value;
            }
            previous = entry;
            entry = entry.next;
        }

        // No such entry
        return null;
    }

    @Override
    public void putAll( Map<? extends K, ? extends V> m )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear()
    {
        for ( int i = 0; i < buckets.length; i++ )
        {
            if(buckets[i] != null)
            {
                LRUEntry<K, V> entry = buckets[i];
                LRUEntry<K, V> next = entry.next;
                while(entry != null)
                {
                    addToFreelist( entry );
                    entry = next;
                    next  = entry != null ? entry.next : null;
                }
                buckets[i] = null;
            }
        }
        size = 0;
    }

    private void addToFreelist( LRUEntry<K, V> entry )
    {
        entry.usageCount = -1;
        entry.next = nextFree;
        nextFree = entry;
    }

    @Override
    public Set<K> keySet()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, V>> entrySet()
    {
        throw new UnsupportedOperationException();
    }

    private LRUEntry<K,V> findEntry( Object key )
    {
        LRUEntry<K,V> entry = buckets[ index( key ) ];
        while(entry != null && !entry.key.equals( key ) && entry.next != null)
        {
            entry = entry.next;
        }
        return entry;
    }

    private int index( Object key )
    {
        int rehash = rehash( key.hashCode() );
        return buckets.length - 1 & rehash;
    }

    private int rehash( long value )
    {
        value ^= (value << 21);
        value ^= (value >>> 35);
        value ^= (value << 4);

        return (int) ((value >>> 32) ^ value);
    }

    private static class LRUEntry<K,V> implements Entry<K, V>
    {
        private byte usageCount = -1;

        private LRUEntry<K,V> next = null;
        private K key;
        private V value;

        public LRUEntry( K key, V value, LRUEntry<K, V> next )
        {
            initialize( key, value, next );
        }

        @Override
        public K getKey()
        {
            return null;
        }

        @Override
        public V getValue()
        {
            return null;
        }

        @Override
        public V setValue( V value )
        {
            return null;
        }

        public LRUEntry<K, V> initialize( K key, V value, LRUEntry<K, V> next )
        {
            assert next != this : "Next cannot point to 'this': " + this;
            this.key = key;
            this.value = value;
            this.next = next;
            this.usageCount = 1;
            return this;
        }

        @Override
        public String toString()
        {
            return "LRUEntry{" +
                    "usageCount=" + usageCount +
                    ", next=" + (next == this ? "self" : next) +
                    ", key=" + key +
                    ", value=" + value +
                    '}';
        }
    }
}
