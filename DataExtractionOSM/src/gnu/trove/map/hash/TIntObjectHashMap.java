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

import gnu.trove.map.TIntObjectMap;
import gnu.trove.impl.Constants;
import gnu.trove.impl.HashFunctions;
import gnu.trove.impl.hash.*;
import gnu.trove.procedure.TIntObjectProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.iterator.TPrimitiveIterator;
import gnu.trove.function.TObjectFunction;
import gnu.trove.set.TIntSet;
import gnu.trove.TIntCollection;

import java.io.*;
import java.util.*;


//////////////////////////////////////////////////
// THIS IS A GENERATED CLASS. DO NOT HAND EDIT! //
//////////////////////////////////////////////////


/**
 * An open addressed Map implementation for int keys and Object values.
 *
 * Created: Sun Nov  4 08:52:45 2001
 *
 * @author Eric D. Friedman
 * @author Rob Eden
 * @author Jeff Randall
 */
public class TIntObjectHashMap<V> extends TIntHash implements
    TIntObjectMap<V>, Externalizable {

    static final long serialVersionUID = 1L;

    private final TIntObjectProcedure<V> PUT_ALL_PROC = new TIntObjectProcedure<V>() {
        public boolean execute( int key, V value) {
            put( key, value );
            return true;
        }
    };

    /** the values of the map */
    protected transient V[] _values;

    /** the value that represents null in the key set. */
    protected int no_entry_key;


    /**
     * Creates a new <code>TIntObjectHashMap</code> instance with the default
     * capacity and load factor.
     */
    public TIntObjectHashMap() {
        super();
    }


    /**
     * Creates a new <code>TIntObjectHashMap</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the default load factor.
     *
     * @param initialCapacity an <code>int</code> value
     */
    public TIntObjectHashMap( int initialCapacity ) {
        super( initialCapacity );
        no_entry_key = Constants.DEFAULT_INT_NO_ENTRY_VALUE;
    }


    /**
     * Creates a new <code>TIntObjectHashMap</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the specified load factor.
     *
     * @param initialCapacity an <code>int</code> value
     * @param loadFactor a <code>float</code> value
     */
    public TIntObjectHashMap( int initialCapacity, float loadFactor ) {
        super( initialCapacity, loadFactor );
        no_entry_key = Constants.DEFAULT_INT_NO_ENTRY_VALUE;
    }


    /**
     * Creates a new <code>TIntObjectHashMap</code> instance with a prime
     * value at or near the specified capacity and load factor.
     *
     * @param initialCapacity used to find a prime capacity for the table.
     * @param loadFactor used to calculate the threshold over which
     * rehashing takes place.
     * @param noEntryKey the value used to represent null in the key set.
     */
    public TIntObjectHashMap( int initialCapacity, float loadFactor, int noEntryKey ) {
        super( initialCapacity, loadFactor );
        this.no_entry_value = noEntryKey;
    }


    /**
     * Creates a new <code>TIntObjectHashMap</code> that contains the entries
     * in the map passed to it.
     *
     * @param map the <tt>TIntObjectMap</tt> to be copied.
     */
    public TIntObjectHashMap( TIntObjectMap<V> map ) {
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
        
        int oldKeys[] = _set;
        V oldVals[] = _values;
        byte oldStates[] = _states;

        _set = new int[newCapacity];
        _values = (V[]) new Object[newCapacity];
        _states = new byte[newCapacity];

        for ( int i = oldCapacity; i-- > 0; ) {
            if( oldStates[i] == FULL ) {
                int o = oldKeys[i];
                int index = insertionIndex(o);
                _set[index] = o;
                _values[index] = oldVals[i];
                _states[index] = FULL;
            }
        }
    }


    // Query Operations

    /** {@inheritDoc} */
    public int getNoEntryKey() {
        return no_entry_key;
    }


    /** {@inheritDoc} */
    public boolean containsKey( int key ) {
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
    public V get( int key ) {
        int index = index( key );
        return index < 0 ? null : _values[index];
    }


    // Modification Operations

    /** {@inheritDoc} */
    public V put( int key, V value ) {
        int index = insertionIndex( key );
        return doPut( key, value, index );
    }


    /** {@inheritDoc} */
    public V putIfAbsent( int key, V value ) {
        int index = insertionIndex( key );
        if ( index < 0 )
            return _values[-index - 1];
        return doPut( key, value, index );
    }


    @SuppressWarnings({"unchecked"})
    private V doPut( int key, V value, int index ) {
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
    public V remove( int key ) {
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
    public void putAll( Map<? extends Integer, ? extends V> map ) {
        Set<? extends Map.Entry<? extends Integer,? extends V>> set = map.entrySet();
        for ( Map.Entry<? extends Integer,? extends V> entry : set ) {
            put( entry.getKey(), entry.getValue() );
        }
    }


    /** {@inheritDoc} */
    public void putAll( TIntObjectMap<V> map ){
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
    public TIntSet keySet() {
        return new KeyView();
    }


    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    public int[] keys() {
        int[] keys = new int[size()];
        int[] k = _set;
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
   public int[] keys( int[] dest ) {
        if ( dest.length < _size ) {
			dest = new int[_size];
        }

        int[] k = _set;
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
    public TIntObjectIterator<V> iterator() {
        return new TIntObjectHashIterator<V>( this );
    }


    /** {@inheritDoc} */
    public boolean forEachKey( TIntProcedure procedure ) {
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
    public boolean forEachEntry( TIntObjectProcedure<V> procedure ) {
        byte[] states = _states;
        int[] keys = _set;
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
    public boolean retainEntries( TIntObjectProcedure<V> procedure ) {
        boolean modified = false;
        byte[] states = _states;
        int[] keys = _set;
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
        if ( ! ( other instanceof TIntObjectMap ) ) {
            return false;
        }
        TIntObjectMap that = ( TIntObjectMap ) other;
        if ( that.size() != this.size() ) {
            return false;
        }
        try {
            TIntObjectIterator iter = this.iterator();
            while ( iter.hasNext() ) {
                iter.advance();
                int key = iter.key();
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


    class KeyView implements TIntSet {

        /** {@inheritDoc} */
        public int getNoEntryValue() {
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
        public boolean contains( int entry ) {
            return TIntObjectHashMap.this.containsKey( entry );
        }

        /** {@inheritDoc} */
        public TIntIterator iterator() {
            return new TIntHashIterator( TIntObjectHashMap.this );
        }

        /** {@inheritDoc} */
        public int[] toArray() {
            return keys();
        }

        /** {@inheritDoc} */
        public int[] toArray( int[] dest ) {
            return keys( dest );
        }

        /** {@inheritDoc} */
        public boolean add( int entry ) {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        public boolean remove( int entry ) {
            return null != TIntObjectHashMap.this.remove( entry );
        }

        /** {@inheritDoc} */
        public boolean containsAll( Collection<?> collection ) {
           for ( Object element : collection ) {
                if ( ! TIntObjectHashMap.this.containsKey( ( ( Integer ) element ).intValue() ) ) {
                    return false;
                }
            }
            return true;
        }

        /** {@inheritDoc} */
        public boolean containsAll( TIntCollection collection ) {
            if ( collection == this ) {
                return true;
            }
            TIntIterator iter = collection.iterator();
            while ( iter.hasNext() ) {
                if ( ! TIntObjectHashMap.this.containsKey( iter.next() ) ) {
                    return false;
                }
            }
            return true;
        }

        /** {@inheritDoc} */
        public boolean containsAll( int[] array ) {
            for ( int element : array  ) {
                if ( ! TIntObjectHashMap.this.containsKey( element ) ) {
                    return false;
                }
            }
            return true;
        }

        /** {@inheritDoc} */
        public boolean addAll( Collection<? extends Integer> collection ) {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        public boolean addAll( TIntCollection collection ) {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        public boolean addAll( int[] array ) {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        public boolean retainAll( Collection<?> collection ) {
            boolean modified = false;
            TIntIterator iter = iterator();
            while ( iter.hasNext() ) {
                //noinspection SuspiciousMethodCalls
                if ( ! collection.contains( Integer.valueOf ( iter.next() ) ) ) {
                    iter.remove();
                    modified = true;
                }
            }
            return modified;
        }

        /** {@inheritDoc} */
        public boolean retainAll( TIntCollection collection ) {
            if ( this == collection ) {
                return false;
            }
            boolean modified = false;
            TIntIterator iter = iterator();
            while ( iter.hasNext() ) {
                if ( ! collection.contains( iter.next() ) ) {
                    iter.remove();
                    modified = true;
                }
            }
            return modified;
        }

        /** {@inheritDoc} */
        public boolean retainAll( int[] array ) {
            boolean changed = false;
            Arrays.sort( array );
            int[] set = _set;
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
                if ( element instanceof Integer ) {
                    int c = ( ( Integer ) element ).intValue();
                    if ( remove( c ) ) {
                        changed = true;
                    }
                }
            }
            return changed;
        }

        /** {@inheritDoc} */
        public boolean removeAll( TIntCollection collection ) {
            if ( collection == this ) {
                clear();
                return true;
            }
            boolean changed = false;
            TIntIterator iter = collection.iterator();
            while ( iter.hasNext() ) {
                int element = iter.next();
                if ( remove( element ) ) {
                    changed = true;
                }
            }
            return changed;
        }

        /** {@inheritDoc} */
        public boolean removeAll( int[] array ) {
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
            TIntObjectHashMap.this.clear();
        }

        /** {@inheritDoc} */
        public boolean forEach( TIntProcedure procedure ) {
            return TIntObjectHashMap.this.forEachKey( procedure );
        }

        /** {@inheritDoc) */
        public boolean equals( Object other ) {
            if (! ( other instanceof TIntSet ) ) {
                return false;
            }
            final TIntSet that = ( TIntSet ) other;
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


       class TIntHashIterator extends THashPrimitiveIterator implements TIntIterator {

            /** the collection on which the iterator operates */
            private final TIntHash _hash;

            /** {@inheritDoc} */
            public TIntHashIterator( TIntHash hash ) {
                super( hash );
                this._hash = hash;
            }

            /** {@inheritDoc} */
            public int next() {
                moveToNextIndex();
                return _hash._set[_index];
            }
        }
    }


    /** a view onto the values of the map. */
    protected class ValueView extends MapBackedView<V> {

        @SuppressWarnings({"unchecked"})
        public Iterator<V> iterator() {
            return new TIntObjectValueHashIterator( TIntObjectHashMap.this ) {
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

        class TIntObjectValueHashIterator extends THashPrimitiveIterator implements Iterator<V> {

            protected final TIntObjectHashMap _map;

            public TIntObjectValueHashIterator( TIntObjectHashMap map ) {
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
            TIntObjectHashMap.this.clear();
        }

        public boolean add( E obj ) {
            throw new UnsupportedOperationException();
        }

        public int size() {
            return TIntObjectHashMap.this.size();
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
            return TIntObjectHashMap.this.isEmpty();
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


    class TIntObjectHashIterator<V> extends THashPrimitiveIterator
        implements TIntObjectIterator<V> {

        /** the collection being iterated over */
        private final TIntObjectHashMap<V> _map;

        /**
         * Creates an iterator over the specified map
         *
         * @param map map to iterate over.
         */
        public TIntObjectHashIterator( TIntObjectHashMap<V> map ) {
            super( map );
            this._map = map;
        }

        /** {@inheritDoc} */
        public void advance() {
            moveToNextIndex();
        }

        /** {@inheritDoc} */
        public int key() {
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
    	out.writeInt( no_entry_key );

    	// NUMBER OF ENTRIES
    	out.writeInt( _size );

    	// ENTRIES
    	for ( int i = _states.length; i-- > 0; ) {
            if ( _states[i] == FULL ) {
                out.writeInt( _set[i] );
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
    	no_entry_key = in.readInt();

    	// NUMBER OF ENTRIES
    	int size = in.readInt();
    	setUp( size );

    	// ENTRIES
        while (size-- > 0) {
            int key = in.readInt();
            V val = (V) in.readObject();
            put(key, val);
        }
    }


    public String toString() {
        final StringBuilder buf = new StringBuilder("{");
        forEachEntry(new TIntObjectProcedure<V>() {
            private boolean first = true;
            public boolean execute(int key, Object value) {
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
} // TIntObjectHashMap
