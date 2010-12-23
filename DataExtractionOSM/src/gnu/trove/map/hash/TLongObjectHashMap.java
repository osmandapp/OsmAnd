///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2001, Eric D. Friedman All Rights Reserved.
// Copyright (c) 2009, Rob Eden All Rights Reserved.
// Copyright (c) 2009, Jeff Randall All Rights Reserved.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
///////////////////////////////////////////////////////////////////////////////

package gnu.trove.map.hash;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.impl.Constants;
import gnu.trove.impl.HashFunctions;
import gnu.trove.impl.hash.*;
import gnu.trove.procedure.TLongObjectProcedure;
import gnu.trove.procedure.TLongProcedure;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.iterator.TPrimitiveIterator;
import gnu.trove.function.TObjectFunction;
import gnu.trove.set.TLongSet;
import gnu.trove.TLongCollection;

import java.io.*;
import java.util.*;


//////////////////////////////////////////////////
// THIS IS A GENERATED CLASS. DO NOT HAND EDIT! //
//////////////////////////////////////////////////


/**
 * An open addressed Map implementation for long keys and Object values.
 *
 * Created: Sun Nov  4 08:52:45 2001
 *
 * @author Eric D. Friedman
 * @author Rob Eden
 * @author Jeff Randall
 */
public class TLongObjectHashMap<V> extends TLongHash implements
    TLongObjectMap<V>, Externalizable {

    static final long serialVersionUID = 1L;

    private final TLongObjectProcedure<V> PUT_ALL_PROC = new TLongObjectProcedure<V>() {
        public boolean execute( long key, V value) {
            put( key, value );
            return true;
        }
    };

    /** the values of the map */
    protected transient V[] _values;

    /** the value that represents null in the key set. */
    protected long no_entry_key;


    /**
     * Creates a new <code>TLongObjectHashMap</code> instance with the default
     * capacity and load factor.
     */
    public TLongObjectHashMap() {
        super();
    }


    /**
     * Creates a new <code>TLongObjectHashMap</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the default load factor.
     *
     * @param initialCapacity an <code>int</code> value
     */
    public TLongObjectHashMap( int initialCapacity ) {
        super( initialCapacity );
        no_entry_key = Constants.DEFAULT_LONG_NO_ENTRY_VALUE;
    }


    /**
     * Creates a new <code>TLongObjectHashMap</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the specified load factor.
     *
     * @param initialCapacity an <code>int</code> value
     * @param loadFactor a <code>float</code> value
     */
    public TLongObjectHashMap( int initialCapacity, float loadFactor ) {
        super( initialCapacity, loadFactor );
        no_entry_key = Constants.DEFAULT_LONG_NO_ENTRY_VALUE;
    }


    /**
     * Creates a new <code>TLongObjectHashMap</code> instance with a prime
     * value at or near the specified capacity and load factor.
     *
     * @param initialCapacity used to find a prime capacity for the table.
     * @param loadFactor used to calculate the threshold over which
     * rehashing takes place.
     * @param noEntryKey the value used to represent null in the key set.
     */
    public TLongObjectHashMap( int initialCapacity, float loadFactor, long noEntryKey ) {
        super( initialCapacity, loadFactor );
        this.no_entry_value = noEntryKey;
    }


    /**
     * Creates a new <code>TLongObjectHashMap</code> that contains the entries
     * in the map passed to it.
     *
     * @param map the <tt>TLongObjectMap</tt> to be copied.
     */
    public TLongObjectHashMap( TLongObjectMap<V> map ) {
        this( map.size(), 0.5f, map.getNoEntryKey() );
        putAll( map );
    }


    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    protected int setUp( int initialCapacity ) {
        int capacity;

        capacity = super.setUp( initialCapacity );
        _values = ( V[] ) new Object[capacity];
        return capacity;
    }


    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    protected void rehash( int newCapacity ) {
        int oldCapacity = _set.length;
        
        long oldKeys[] = _set;
        V oldVals[] = _values;
        byte oldStates[] = _states;

        _set = new long[newCapacity];
        _values = (V[]) new Object[newCapacity];
        _states = new byte[newCapacity];

        for ( int i = oldCapacity; i-- > 0; ) {
            if( oldStates[i] == FULL ) {
                long o = oldKeys[i];
                int index = insertionIndex(o);
                _set[index] = o;
                _values[index] = oldVals[i];
                _states[index] = FULL;
            }
        }
    }


    // Query Operations

    /** {@inheritDoc} */
    public long getNoEntryKey() {
        return no_entry_key;
    }


    /** {@inheritDoc} */
    public boolean containsKey( long key ) {
        return contains( key );
    }


    /** {@inheritDoc} */
    public boolean containsValue( Object val ) {
        byte[] states = _states;
        V[] vals = _values;

        // special case null values so that we don't have to
        // perform null checks before every call to equals()
        if ( null == val ) {
            for ( int i = vals.length; i-- > 0; ) {
                if ( states[i] == FULL && val == vals[i] ) {
                    return true;
                }
            }
        } else {
            for ( int i = vals.length; i-- > 0; ) {
                if ( states[i] == FULL &&
                    ( val == vals[i] || val.equals( vals[i] ) ) ) {
                    return true;
                }
            }
        } // end of else
        return false;
    }


    /** {@inheritDoc} */
    public V get( long key ) {
        int index = index( key );
        return index < 0 ? null : _values[index];
    }


    // Modification Operations

    /** {@inheritDoc} */
    public V put( long key, V value ) {
        int index = insertionIndex( key );
        return doPut( key, value, index );
    }


    /** {@inheritDoc} */
    public V putIfAbsent( long key, V value ) {
        int index = insertionIndex( key );
        if ( index < 0 )
            return _values[-index - 1];
        return doPut( key, value, index );
    }


    @SuppressWarnings({"unchecked"})
    private V doPut( long key, V value, int index ) {
        byte previousState;
        V previous = null;
        boolean isNewMapping = true;
        if ( index < 0 ) {
            index = -index -1;
            previous = _values[index];
            isNewMapping = false;
        }
        previousState = _states[index];
        _set[index] = key;
        _states[index] = FULL;
        _values[index] = value;
        if (isNewMapping) {
            postInsertHook( previousState == FREE );
        }

        return previous;
    }


    /** {@inheritDoc} */
    public V remove( long key ) {
        V prev = null;
        int index = index( key );
        if ( index >= 0 ) {
            prev = _values[index];
            removeAt( index );    // clear key,state; adjust size
        }
        return prev;
    }


    /** {@inheritDoc} */
    protected void removeAt( int index ) {
        _values[index] = null;
        super.removeAt( index );  // clear key, state; adjust size
    }


    // Bulk Operations

    /** {@inheritDoc} */
    public void putAll( Map<? extends Long, ? extends V> map ) {
        Set<? extends Map.Entry<? extends Long,? extends V>> set = map.entrySet();
        for ( Map.Entry<? extends Long,? extends V> entry : set ) {
            put( entry.getKey(), entry.getValue() );
        }
    }


    /** {@inheritDoc} */
    public void putAll( TLongObjectMap<V> map ){
        map.forEachEntry( PUT_ALL_PROC );
    }


    /** {@inheritDoc} */
    public void clear() {
        super.clear();
        Arrays.fill( _set, 0, _set.length, no_entry_key );
        Arrays.fill( _states, 0, _states.length, FREE );
        Arrays.fill( _values, 0, _values.length, null );
    }


    // Views

    /** {@inheritDoc} */
    public TLongSet keySet() {
        return new KeyView();
    }


    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    public long[] keys() {
        long[] keys = new long[size()];
        long[] k = _set;
        byte[] states = _states;

        for ( int i = k.length, j = 0; i-- > 0; ) {
          if ( states[i] == FULL ) {
            keys[j++] = k[i];
          }
        }
        return keys;
    }


    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
   public long[] keys( long[] dest ) {
        if ( dest.length < _size ) {
			dest = new long[_size];
        }

        long[] k = _set;
        byte[] states = _states;

        for ( int i = k.length, j = 0; i-- > 0; ) {
          if ( states[i] == FULL ) {
            dest[j++] = k[i];
          }
        }
        return dest;
    }


    /** {@inheritDoc} */
    public Collection<V> valueCollection() {
        return new ValueView();
    }


    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    public V[] values() {
        V[] vals = ( V[] ) new Object[size()];
        V[] v = _values;
        byte[] states = _states;

        for ( int i = v.length, j = 0; i-- > 0; ) {
          if ( states[i] == FULL ) {
            vals[j++] = v[i];
          }
        }
        return vals;
    }


    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    public <T> T[] values( T[] dest ) {
        if ( dest.length < _size ) {
			dest = ( T[] ) java.lang.reflect.Array.newInstance(
			                dest.getClass().getComponentType(), _size);
        }

        V[] v = _values;
        byte[] states = _states;

        for ( int i = v.length, j = 0; i-- > 0; ) {
            if ( states[i] == FULL ) {
                dest[j++] = ( T ) v[i];
            }
        }
		return dest;
    }


    /** {@inheritDoc} */
    public TLongObjectIterator<V> iterator() {
        return new TLongObjectHashIterator<V>( this );
    }


    /** {@inheritDoc} */
    public boolean forEachKey( TLongProcedure procedure ) {
        return forEach( procedure );
    }


    /** {@inheritDoc} */
    public boolean forEachValue( TObjectProcedure<V> procedure ) {
        byte[] states = _states;
        V[] values = _values;
        for ( int i = values.length; i-- > 0; ) {
            if ( states[i] == FULL && ! procedure.execute( values[i] ) ) {
                return false;
            }
        }
        return true;
    }


    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    public boolean forEachEntry( TLongObjectProcedure<V> procedure ) {
        byte[] states = _states;
        long[] keys = _set;
        V[] values = _values;
        for (int i = keys.length; i-- > 0;) {
            if (states[i] == FULL && ! procedure.execute(keys[i],values[i])) {
                return false;
            }
        }
        return true;
    }


    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    public boolean retainEntries( TLongObjectProcedure<V> procedure ) {
        boolean modified = false;
        byte[] states = _states;
        long[] keys = _set;
        V[] values = _values;

        // Temporarily disable compaction. This is a fix for bug #1738760
        tempDisableAutoCompaction();
        try {
            for ( int i = keys.length; i-- > 0; ) {
                if ( states[i] == FULL && ! procedure.execute( keys[i], values[i] ) ) {
                    removeAt( i );
                    modified = true;
                }
            }
        }
        finally {
            reenableAutoCompaction( true );
        }

        return modified;
    }


    /** {@inheritDoc} */
    public void transformValues( TObjectFunction<V,V> function ) {
        byte[] states = _states;
        V[] values = _values;
        for ( int i = values.length; i-- > 0; ) {
            if ( states[i] == FULL ) {
                values[i] = function.execute( values[i] );
            }
        }
    }


    // Comparison and hashing

    /** {@inheritDoc} */
    public boolean equals( Object other ) {
        if ( ! ( other instanceof TLongObjectMap ) ) {
            return false;
        }
        TLongObjectMap that = ( TLongObjectMap ) other;
        if ( that.size() != this.size() ) {
            return false;
        }
        try {
            TLongObjectIterator iter = this.iterator();
            while ( iter.hasNext() ) {
                iter.advance();
                long key = iter.key();
                Object value = iter.value();
                if ( value == null ) {
                    if ( !( that.get( key ) == null && that.containsKey( key ) ) ) {
                        return false;
                    }
                } else {
                    if ( !value.equals( that.get( key ) ) ) {
                        return false;
                    }
                }
            }
        } catch ( ClassCastException ex ) {
            // unused.
        }
        return true;
    }


    /** {@inheritDoc} */
    public int hashCode() {
        int hashcode = 0;
        V[] values = _values;
        byte[] states = _states;
        for ( int i = values.length; i-- > 0; ) {
            if ( states[i] == FULL ) {
                hashcode += HashFunctions.hash( _set[i] ) ^
                            ( values[i] == null ? 0 : values[i].hashCode() );
            }
        }
        return hashcode;
    }


    class KeyView implements TLongSet {

        /** {@inheritDoc} */
        public long getNoEntryValue() {
            return no_entry_key;
        }

        /** {@inheritDoc} */
        public int size() {
            return _size;
        }

        /** {@inheritDoc} */
        public boolean isEmpty() {
            return _size == 0;
        }

        /** {@inheritDoc} */
        public boolean contains( long entry ) {
            return TLongObjectHashMap.this.containsKey( entry );
        }

        /** {@inheritDoc} */
        public TLongIterator iterator() {
            return new TLongHashIterator( TLongObjectHashMap.this );
        }

        /** {@inheritDoc} */
        public long[] toArray() {
            return keys();
        }

        /** {@inheritDoc} */
        public long[] toArray( long[] dest ) {
            return keys( dest );
        }

        /** {@inheritDoc} */
        public boolean add( long entry ) {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        public boolean remove( long entry ) {
            return null != TLongObjectHashMap.this.remove( entry );
        }

        /** {@inheritDoc} */
        public boolean containsAll( Collection<?> collection ) {
           for ( Object element : collection ) {
                if ( ! TLongObjectHashMap.this.containsKey( ( ( Long ) element ).longValue() ) ) {
                    return false;
                }
            }
            return true;
        }

        /** {@inheritDoc} */
        public boolean containsAll( TLongCollection collection ) {
            if ( collection == this ) {
                return true;
            }
            TLongIterator iter = collection.iterator();
            while ( iter.hasNext() ) {
                if ( ! TLongObjectHashMap.this.containsKey( iter.next() ) ) {
                    return false;
                }
            }
            return true;
        }

        /** {@inheritDoc} */
        public boolean containsAll( long[] array ) {
            for ( long element : array  ) {
                if ( ! TLongObjectHashMap.this.containsKey( element ) ) {
                    return false;
                }
            }
            return true;
        }

        /** {@inheritDoc} */
        public boolean addAll( Collection<? extends Long> collection ) {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        public boolean addAll( TLongCollection collection ) {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        public boolean addAll( long[] array ) {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        public boolean retainAll( Collection<?> collection ) {
            boolean modified = false;
            TLongIterator iter = iterator();
            while ( iter.hasNext() ) {
                //noinspection SuspiciousMethodCalls
                if ( ! collection.contains( Long.valueOf ( iter.next() ) ) ) {
                    iter.remove();
                    modified = true;
                }
            }
            return modified;
        }

        /** {@inheritDoc} */
        public boolean retainAll( TLongCollection collection ) {
            if ( this == collection ) {
                return false;
            }
            boolean modified = false;
            TLongIterator iter = iterator();
            while ( iter.hasNext() ) {
                if ( ! collection.contains( iter.next() ) ) {
                    iter.remove();
                    modified = true;
                }
            }
            return modified;
        }

        /** {@inheritDoc} */
        public boolean retainAll( long[] array ) {
            boolean changed = false;
            Arrays.sort( array );
            long[] set = _set;
            byte[] states = _states;
    
            for ( int i = set.length; i-- > 0; ) {
                if ( states[i] == FULL && ( Arrays.binarySearch( array, set[i] ) < 0) ) {
                    removeAt( i );
                    changed = true;
                }
            }
            return changed;
        }

        /** {@inheritDoc} */
        public boolean removeAll( Collection<?> collection ) {
            boolean changed = false;
            for ( Object element : collection ) {
                if ( element instanceof Long ) {
                    long c = ( ( Long ) element ).longValue();
                    if ( remove( c ) ) {
                        changed = true;
                    }
                }
            }
            return changed;
        }

        /** {@inheritDoc} */
        public boolean removeAll( TLongCollection collection ) {
            if ( collection == this ) {
                clear();
                return true;
            }
            boolean changed = false;
            TLongIterator iter = collection.iterator();
            while ( iter.hasNext() ) {
                long element = iter.next();
                if ( remove( element ) ) {
                    changed = true;
                }
            }
            return changed;
        }

        /** {@inheritDoc} */
        public boolean removeAll( long[] array ) {
            boolean changed = false;
            for ( int i = array.length; i-- > 0; ) {
                if ( remove(array[i]) ) {
                    changed = true;
                }
            }
            return changed;
        }

        /** {@inheritDoc} */
        public void clear() {
            TLongObjectHashMap.this.clear();
        }

        /** {@inheritDoc} */
        public boolean forEach( TLongProcedure procedure ) {
            return TLongObjectHashMap.this.forEachKey( procedure );
        }

        /** {@inheritDoc) */
        public boolean equals( Object other ) {
            if (! ( other instanceof TLongSet ) ) {
                return false;
            }
            final TLongSet that = ( TLongSet ) other;
            if ( that.size() != this.size() ) {
                return false;
            }
            for ( int i = _states.length; i-- > 0; ) {
                if ( _states[i] == FULL ) {
                    if ( ! that.contains( _set[i] ) ) {
                        return false;
                    }
                }
            }
            return true;
        }

        /** {@inheritDoc} */
        public int hashCode() {
            int hashcode = 0;
            for ( int i = _states.length; i-- > 0; ) {
                if ( _states[i] == FULL ) {
                    hashcode += HashFunctions.hash( _set[i] );
                }
            }
            return hashcode;
        }

        /** {@inheritDoc} */
        public String toString() {
            final StringBuilder buf = new StringBuilder("{");
            boolean first = true;
            for ( int i = _states.length; i-- > 0; ) {
                if ( _states[i] == FULL ) {
                    if ( first ) first = false;
                    else buf.append( "," );
                    buf.append( _set[i] );
                }
            }
            return buf.toString();
        }


       class TLongHashIterator extends THashPrimitiveIterator implements TLongIterator {

            /** the collection on which the iterator operates */
            private final TLongHash _hash;

            /** {@inheritDoc} */
            public TLongHashIterator( TLongHash hash ) {
                super( hash );
                this._hash = hash;
            }

            /** {@inheritDoc} */
            public long next() {
                moveToNextIndex();
                return _hash._set[_index];
            }
        }
    }


    /** a view onto the values of the map. */
    protected class ValueView extends MapBackedView<V> {

        @SuppressWarnings({"unchecked"})
        public Iterator<V> iterator() {
            return new TLongObjectValueHashIterator( TLongObjectHashMap.this ) {
                protected V objectAtIndex( int index ) {
                    return _values[index];
                }
            };
        }

        public boolean containsElement( V value ) {
            return containsValue( value );
        }

        public boolean removeElement( V value ) {
            V[] values = _values;
            byte[] states = _states;

            for ( int i = values.length; i-- > 0; ) {
                if ( states[i] == FULL ) {
                     if ( value == values[i] ||
                        ( null != values[i] && values[i].equals( value ) ) ) {
                        removeAt( i );
                        return true;
                    }
                }
            }
            return false;
        }

        class TLongObjectValueHashIterator extends THashPrimitiveIterator implements Iterator<V> {

            protected final TLongObjectHashMap _map;

            public TLongObjectValueHashIterator( TLongObjectHashMap map ) {
                super( map );
                _map = map;
            }

            @SuppressWarnings("unchecked")
            protected V objectAtIndex( int index ) {
                byte[] states = _states;
                Object value = _map._values[index];
                if ( states[index] != FULL ) {
                    return null;
                }
                return ( V ) value;
            }

            /** {@inheritDoc} */
            @SuppressWarnings("unchecked")
            public V next() {
                moveToNextIndex();
	            return ( V ) _map._values[_index];
            }
        }
    }


    private abstract class MapBackedView<E> extends AbstractSet<E>
            implements Set<E>, Iterable<E> {

        public abstract Iterator<E> iterator();

        public abstract boolean removeElement( E key );

        public abstract boolean containsElement( E key );

        @SuppressWarnings({"unchecked"})
        public boolean contains( Object key ) {
            return containsElement( (E) key );
        }

        @SuppressWarnings({"unchecked"})
        public boolean remove( Object o ) {
            return removeElement( (E) o );
        }

        public void clear() {
            TLongObjectHashMap.this.clear();
        }

        public boolean add( E obj ) {
            throw new UnsupportedOperationException();
        }

        public int size() {
            return TLongObjectHashMap.this.size();
        }

        public Object[] toArray() {
            Object[] result = new Object[size()];
            Iterator<E> e = iterator();
            for ( int i = 0; e.hasNext(); i++ ) {
                result[i] = e.next();
            }
            return result;
        }

        @SuppressWarnings({"unchecked"})
        public <T> T[] toArray( T[] a ) {
            int size = size();
            if ( a.length < size ) {
                a = (T[]) java.lang.reflect.Array.newInstance( a.getClass().getComponentType(), size );
            }

            Iterator<E> it = iterator();
            Object[] result = a;
            for ( int i = 0; i < size; i++ ) {
                result[i] = it.next();
            }

            if ( a.length > size ) {
                a[size] = null;
            }

            return a;
        }

        public boolean isEmpty() {
            return TLongObjectHashMap.this.isEmpty();
        }

        public boolean addAll( Collection<? extends E> collection ) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings({"SuspiciousMethodCalls"})
        public boolean retainAll( Collection<?> collection ) {
            boolean changed = false;
            Iterator<E> i = iterator();
            while ( i.hasNext() ) {
                if ( !collection.contains( i.next() ) ) {
                    i.remove();
                    changed = true;
                }
            }
            return changed;
        }
    }


    class TLongObjectHashIterator<V> extends THashPrimitiveIterator
        implements TLongObjectIterator<V> {

        /** the collection being iterated over */
        private final TLongObjectHashMap<V> _map;

        /**
         * Creates an iterator over the specified map
         *
         * @param map map to iterate over.
         */
        public TLongObjectHashIterator( TLongObjectHashMap<V> map ) {
            super( map );
            this._map = map;
        }

        /** {@inheritDoc} */
        public void advance() {
            moveToNextIndex();
        }

        /** {@inheritDoc} */
        public long key() {
            return _map._set[_index];
        }

        /** {@inheritDoc} */
        public V value() {
            return _map._values[_index];
        }

        /** {@inheritDoc} */
        public V setValue( V val ) {
            V old = value();
            _map._values[_index] = val;
            return old;
        }
    }


    public void writeExternal( ObjectOutput out ) throws IOException {
    	// VERSION
    	out.writeByte( 0 );

    	// SUPER
    	super.writeExternal( out );

    	// NO_ENTRY_KEY
    	out.writeLong( no_entry_key );

    	// NUMBER OF ENTRIES
    	out.writeInt( _size );

    	// ENTRIES
    	for ( int i = _states.length; i-- > 0; ) {
            if ( _states[i] == FULL ) {
                out.writeLong( _set[i] );
                out.writeObject( _values[i] );
            }
        }
    }


    @SuppressWarnings({"unchecked"})
    public void readExternal( ObjectInput in )
    	throws IOException, ClassNotFoundException {

    	// VERSION
    	in.readByte();

    	// SUPER
    	super.readExternal( in );

    	// NO_ENTRY_KEY
    	no_entry_key = in.readLong();

    	// NUMBER OF ENTRIES
    	int size = in.readInt();
    	setUp( size );

    	// ENTRIES
        while (size-- > 0) {
            long key = in.readLong();
            V val = (V) in.readObject();
            put(key, val);
        }
    }


    public String toString() {
        final StringBuilder buf = new StringBuilder("{");
        forEachEntry(new TLongObjectProcedure<V>() {
            private boolean first = true;
            public boolean execute(long key, Object value) {
                if ( first ) first = false;
                else buf.append( "," );

                buf.append(key);
                buf.append("=");
                buf.append(value);
                return true;
            }
        });
        buf.append("}");
        return buf.toString();
    }
} // TLongObjectHashMap
