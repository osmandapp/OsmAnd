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


//////////////////////////////////////////////////
// THIS IS A GENERATED CLASS. DO NOT HAND EDIT! //
//////////////////////////////////////////////////

import gnu.trove.map.TIntLongMap;
import gnu.trove.function.TLongFunction;
import gnu.trove.procedure.*;
import gnu.trove.set.*;
import gnu.trove.iterator.*;
import gnu.trove.impl.hash.*;
import gnu.trove.impl.HashFunctions;
import gnu.trove.*;

import java.io.*;
import java.util.*;

/**
 * An open addressed Map implementation for int keys and long values.
 *
 * @author Eric D. Friedman
 * @author Rob Eden
 * @author Jeff Randall
 * @version $Id: _K__V_HashMap.template,v 1.1.2.16 2010/03/02 04:09:50 robeden Exp $
 */
public class TIntLongHashMap extends TIntLongHash implements TIntLongMap, Externalizable {
    static final long serialVersionUID = 1L;

    /** the values of the map */
    protected transient long[] _values;


    /**
     * Creates a new <code>TIntLongHashMap</code> instance with the default
     * capacity and load factor.
     */
    public TIntLongHashMap() {
        super();
    }


    /**
     * Creates a new <code>TIntLongHashMap</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the default load factor.
     *
     * @param initialCapacity an <code>int</code> value
     */
    public TIntLongHashMap( int initialCapacity ) {
        super( initialCapacity );
    }


    /**
     * Creates a new <code>TIntLongHashMap</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the specified load factor.
     *
     * @param initialCapacity an <code>int</code> value
     * @param loadFactor a <code>float</code> value
     */
    public TIntLongHashMap( int initialCapacity, float loadFactor ) {
        super( initialCapacity, loadFactor );
    }


    /**
     * Creates a new <code>TIntLongHashMap</code> instance with a prime
     * capacity equal to or greater than <tt>initialCapacity</tt> and
     * with the specified load factor.
     *
     * @param initialCapacity an <code>int</code> value
     * @param loadFactor a <code>float</code> value
     * @param noEntryKey a <code>int</code> value that represents
     *                   <tt>null</tt> for the Key set.
     * @param noEntryValue a <code>long</code> value that represents
     *                   <tt>null</tt> for the Value set.
     */
    public TIntLongHashMap( int initialCapacity, float loadFactor,
        int noEntryKey, long noEntryValue ) {
        super( initialCapacity, loadFactor, noEntryKey, noEntryValue );
    }


    /**
     * Creates a new <code>TIntLongHashMap</code> instance containing
     * all of the entries in the map passed in.
     *
     * @param keys a <tt>int</tt> array containing the keys for the matching values.
     * @param values a <tt>long</tt> array containing the values.
     */
    public TIntLongHashMap( int[] keys, long[] values ) {
        super( Math.max( keys.length, values.length ) );

        int size = Math.min( keys.length, values.length );
        for ( int i = 0; i < size; i++ ) {
            this.put( keys[i], values[i] );
        }
    }


    /**
     * Creates a new <code>TIntLongHashMap</code> instance containing
     * all of the entries in the map passed in.
     *
     * @param map a <tt>TIntLongMap</tt> that will be duplicated.
     */
    public TIntLongHashMap( TIntLongMap map ) {
        super( map.size() );
        if ( map instanceof TIntLongHashMap ) {
            TIntLongHashMap hashmap = ( TIntLongHashMap ) map;
            this._loadFactor = hashmap._loadFactor;
            this.no_entry_key = hashmap.no_entry_key;
            this.no_entry_value = hashmap.no_entry_value;
            //noinspection RedundantCast
            if ( this.no_entry_key != ( int ) 0 ) {
                Arrays.fill( _set, this.no_entry_key );
            }
            //noinspection RedundantCast
            if ( this.no_entry_value != ( long ) 0 ) {
                Arrays.fill( _values, this.no_entry_value );
            }
            setUp( (int) Math.ceil( DEFAULT_CAPACITY / _loadFactor ) );
        }
        putAll( map );
    }


    /**
     * initializes the hashtable to a prime capacity which is at least
     * <tt>initialCapacity + 1</tt>.
     *
     * @param initialCapacity an <code>int</code> value
     * @return the actual capacity chosen
     */
    @Override
	protected int setUp( int initialCapacity ) {
        int capacity;

        capacity = super.setUp( initialCapacity );
        _values = new long[capacity];
        return capacity;
    }


    /**
     * rehashes the map to the new capacity.
     *
     * @param newCapacity an <code>int</code> value
     */
     /** {@inheritDoc} */
    @Override
	protected void rehash( int newCapacity ) {
        int oldCapacity = _set.length;
        
        int oldKeys[] = _set;
        long oldVals[] = _values;
        byte oldStates[] = _states;

        _set = new int[newCapacity];
        _values = new long[newCapacity];
        _states = new byte[newCapacity];

        for ( int i = oldCapacity; i-- > 0; ) {
            if( oldStates[i] == FULL ) {
                int o = oldKeys[i];
                int index = insertionIndex( o );
                _set[index] = o;
                _values[index] = oldVals[i];
                _states[index] = FULL;
            }
        }
    }


    /** {@inheritDoc} */
    @Override
	public long put( int key, long value ) {
        int index = insertionIndex( key );
        return doPut( key, value, index );
    }


    /** {@inheritDoc} */
    @Override
	public long putIfAbsent( int key, long value ) {
        int index = insertionIndex( key );
        if (index < 0)
            return _values[-index - 1];
        return doPut( key, value, index );
    }


    private long doPut( int key, long value, int index ) {
        byte previousState;
        long previous = no_entry_value;
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
    @Override
	public void putAll( Map<? extends Integer, ? extends Long> map ) {
        ensureCapacity( map.size() );
        // could optimize this for cases when map instanceof THashMap
        for ( Map.Entry<? extends Integer, ? extends Long> entry : map.entrySet() ) {
            this.put( entry.getKey().intValue(), entry.getValue().longValue() );
        }
    }
    

    /** {@inheritDoc} */
    @Override
	public void putAll( TIntLongMap map ) {
        ensureCapacity( map.size() );
        TIntLongIterator iter = map.iterator();
        while ( iter.hasNext() ) {
            iter.advance();
            this.put( iter.key(), iter.value() );
        }
    }


    /** {@inheritDoc} */
    @Override
	public long get( int key ) {
        int index = index( key );
        return index < 0 ? no_entry_value : _values[index];
    }


    /** {@inheritDoc} */
    @Override
	public void clear() {
        super.clear();
        Arrays.fill( _set, 0, _set.length, no_entry_key );
        Arrays.fill( _values, 0, _values.length, no_entry_value );
        Arrays.fill( _states, 0, _states.length, FREE );
    }


    /** {@inheritDoc} */
    @Override
	public boolean isEmpty() {
        return 0 == _size;
    }


    /** {@inheritDoc} */
    @Override
	public long remove( int key ) {
        long prev = no_entry_value;
        int index = index( key );
        if ( index >= 0 ) {
            prev = _values[index];
            removeAt( index );    // clear key,state; adjust size
        }
        return prev;
    }


    /** {@inheritDoc} */
    @Override
	protected void removeAt( int index ) {
        _values[index] = no_entry_value;
        super.removeAt( index );  // clear key, state; adjust size
    }


    /** {@inheritDoc} */
    @Override
	public TIntSet keySet() {
        return new TKeyView();
    }


    /** {@inheritDoc} */
    @Override
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
    @Override
	public int[] keys( int[] array ) {
        int size = size();
        if ( array.length < size ) {
            array = new int[size];
        }

        int[] keys = _set;
        byte[] states = _states;

        for ( int i = keys.length, j = 0; i-- > 0; ) {
          if ( states[i] == FULL ) {
            array[j++] = keys[i];
          }
        }
        return array;
    }


    /** {@inheritDoc} */
    @Override
	public TLongCollection valueCollection() {
        return new TValueView();
    }


    /** {@inheritDoc} */
    @Override
	public long[] values() {
        long[] vals = new long[size()];
        long[] v = _values;
        byte[] states = _states;

        for ( int i = v.length, j = 0; i-- > 0; ) {
          if ( states[i] == FULL ) {
            vals[j++] = v[i];
          }
        }
        return vals;
    }


    /** {@inheritDoc} */
    @Override
	public long[] values( long[] array ) {
        int size = size();
        if ( array.length < size ) {
            array = new long[size];
        }

        long[] v = _values;
        byte[] states = _states;

        for ( int i = v.length, j = 0; i-- > 0; ) {
          if ( states[i] == FULL ) {
            array[j++] = v[i];
          }
        }
        return array;
    }


    /** {@inheritDoc} */
    @Override
	public boolean containsValue( long val ) {
        byte[] states = _states;
        long[] vals = _values;

        for ( int i = vals.length; i-- > 0; ) {
            if ( states[i] == FULL && val == vals[i] ) {
                return true;
            }
        }
        return false;
    }


    /** {@inheritDoc} */
    @Override
	public boolean containsKey( int key ) {
        return contains( key );
    }


    /** {@inheritDoc} */
    @Override
	public TIntLongIterator iterator() {
        return new TIntLongHashIterator( this );
    }


    /** {@inheritDoc} */
    @Override
	public boolean forEachKey( TIntProcedure procedure ) {
        return forEach( procedure );
    }


    /** {@inheritDoc} */
    @Override
	public boolean forEachValue( TLongProcedure procedure ) {
        byte[] states = _states;
        long[] values = _values;
        for ( int i = values.length; i-- > 0; ) {
            if ( states[i] == FULL && ! procedure.execute( values[i] ) ) {
                return false;
            }
        }
        return true;
    }


    /** {@inheritDoc} */
    @Override
	public boolean forEachEntry( TIntLongProcedure procedure ) {
        byte[] states = _states;
        int[] keys = _set;
        long[] values = _values;
        for ( int i = keys.length; i-- > 0; ) {
            if ( states[i] == FULL && ! procedure.execute( keys[i], values[i] ) ) {
                return false;
            }
        }
        return true;
    }


    /** {@inheritDoc} */
    @Override
	public void transformValues( TLongFunction function ) {
        byte[] states = _states;
        long[] values = _values;
        for ( int i = values.length; i-- > 0; ) {
            if ( states[i] == FULL ) {
                values[i] = function.execute( values[i] );
            }
        }
    }


    /** {@inheritDoc} */
    @Override
	public boolean retainEntries( TIntLongProcedure procedure ) {
        boolean modified = false;
        byte[] states = _states;
        int[] keys = _set;
        long[] values = _values;


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
    @Override
	public boolean increment( int key ) {
        return adjustValue( key, ( long ) 1 );
    }


    /** {@inheritDoc} */
    @Override
	public boolean adjustValue( int key, long amount ) {
        int index = index( key );
        if (index < 0) {
            return false;
        } else {
            _values[index] += amount;
            return true;
        }
    }


    /** {@inheritDoc} */
    @Override
	public long adjustOrPutValue( int key, long adjust_amount, long put_amount ) {
        int index = insertionIndex( key );
        final boolean isNewMapping;
        final long newValue;
        if ( index < 0 ) {
            index = -index -1;
            newValue = ( _values[index] += adjust_amount );
            isNewMapping = false;
        } else {
            newValue = ( _values[index] = put_amount );
            isNewMapping = true;
        }

        byte previousState = _states[index];
        _set[index] = key;
        _states[index] = FULL;

        if ( isNewMapping ) {
            postInsertHook(previousState == FREE);
        }

        return newValue;
    }


    /** a view onto the keys of the map. */
    protected class TKeyView implements TIntSet {

        /** {@inheritDoc} */
        @Override
		public TIntIterator iterator() {
            return new TIntLongKeyHashIterator( TIntLongHashMap.this );
        }


        /** {@inheritDoc} */
        @Override
		public int getNoEntryValue() {
            return no_entry_key;
        }


        /** {@inheritDoc} */
        @Override
		public int size() {
            return _size;
        }


        /** {@inheritDoc} */
        @Override
		public boolean isEmpty() {
            return 0 == _size;
        }


        /** {@inheritDoc} */
        @Override
		public boolean contains( int entry ) {
            return TIntLongHashMap.this.contains( entry );
        }


        /** {@inheritDoc} */
        @Override
		public int[] toArray() {
            return TIntLongHashMap.this.keys();
        }


        /** {@inheritDoc} */
        @Override
		public int[] toArray( int[] dest ) {
            return TIntLongHashMap.this.keys( dest );
        }


        /**
         * Unsupported when operating upon a Key Set view of a TIntLongMap
         * <p/>
         * {@inheritDoc}
         */
        @Override
		public boolean add( int entry ) {
            throw new UnsupportedOperationException();
        }


        /** {@inheritDoc} */
        @Override
		public boolean remove( int entry ) {
            return no_entry_value != TIntLongHashMap.this.remove( entry );
        }


        /** {@inheritDoc} */
        @Override
		public boolean containsAll( Collection<?> collection ) {
            for ( Object element : collection ) {
                if ( element instanceof Integer ) {
                    int ele = ( ( Integer ) element ).intValue();
                    if ( ! TIntLongHashMap.this.containsKey( ele ) ) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return true;
        }


        /** {@inheritDoc} */
        @Override
		public boolean containsAll( TIntCollection collection ) {
            TIntIterator iter = collection.iterator();
            while ( iter.hasNext() ) {
                if ( ! TIntLongHashMap.this.containsKey( iter.next() ) ) {
                    return false;
                }
            }
            return true;
        }


        /** {@inheritDoc} */
        @Override
		public boolean containsAll( int[] array ) {
            for ( int element : array ) {
                if ( ! TIntLongHashMap.this.contains( element ) ) {
                    return false;
                }
            }
            return true;
        }


        /**
         * Unsupported when operating upon a Key Set view of a TIntLongMap
         * <p/>
         * {@inheritDoc}
         */
        @Override
		public boolean addAll( Collection<? extends Integer> collection ) {
            throw new UnsupportedOperationException();
        }


        /**
         * Unsupported when operating upon a Key Set view of a TIntLongMap
         * <p/>
         * {@inheritDoc}
         */
        @Override
		public boolean addAll( TIntCollection collection ) {
            throw new UnsupportedOperationException();
        }


        /**
         * Unsupported when operating upon a Key Set view of a TIntLongMap
         * <p/>
         * {@inheritDoc}
         */
        @Override
		public boolean addAll( int[] array ) {
            throw new UnsupportedOperationException();
        }


        /** {@inheritDoc} */
        @Override
		@SuppressWarnings({"SuspiciousMethodCalls"})
        public boolean retainAll( Collection<?> collection ) {
            boolean modified = false;
            TIntIterator iter = iterator();
            while ( iter.hasNext() ) {
                if ( ! collection.contains( Integer.valueOf ( iter.next() ) ) ) {
                    iter.remove();
                    modified = true;
                }
            }
            return modified;
        }


        /** {@inheritDoc} */
        @Override
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
        @Override
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
        @Override
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
        @Override
		public boolean removeAll( TIntCollection collection ) {
            if ( this == collection ) {
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
        @Override
		public boolean removeAll( int[] array ) {
            boolean changed = false;
            for ( int i = array.length; i-- > 0; ) {
                if ( remove( array[i] ) ) {
                    changed = true;
                }
            }
            return changed;
        }


        /** {@inheritDoc} */
        @Override
		public void clear() {
            TIntLongHashMap.this.clear();
        }


        /** {@inheritDoc} */
        @Override
		public boolean forEach( TIntProcedure procedure ) {
            return TIntLongHashMap.this.forEachKey( procedure );
        }


        @Override
        public boolean equals( Object other ) {
            if (! (other instanceof TIntSet)) {
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


        @Override
        public int hashCode() {
            int hashcode = 0;
            for ( int i = _states.length; i-- > 0; ) {
                if ( _states[i] == FULL ) {
                    hashcode += HashFunctions.hash( _set[i] );
                }
            }
            return hashcode;
        }


        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder( "{" );
            forEachKey( new TIntProcedure() {
                private boolean first = true;


                @Override
				public boolean execute( int key ) {
                    if ( first ) {
                        first = false;
                    } else {
                        buf.append( ", " );
                    }

                    buf.append( key );
                    return true;
                }
            } );
            buf.append( "}" );
            return buf.toString();
        }
    }


    /** a view onto the values of the map. */
    protected class TValueView implements TLongCollection {

        /** {@inheritDoc} */
        @Override
		public TLongIterator iterator() {
            return new TIntLongValueHashIterator( TIntLongHashMap.this );
        }


        /** {@inheritDoc} */
        @Override
		public long getNoEntryValue() {
            return no_entry_value;
        }


        /** {@inheritDoc} */
        @Override
		public int size() {
            return _size;
        }


        /** {@inheritDoc} */
        @Override
		public boolean isEmpty() {
            return 0 == _size;
        }


        /** {@inheritDoc} */
        @Override
		public boolean contains( long entry ) {
            return TIntLongHashMap.this.containsValue( entry );
        }


        /** {@inheritDoc} */
        @Override
		public long[] toArray() {
            return TIntLongHashMap.this.values();
        }


        /** {@inheritDoc} */
        @Override
		public long[] toArray( long[] dest ) {
            return TIntLongHashMap.this.values( dest );
        }



        @Override
		public boolean add( long entry ) {
            throw new UnsupportedOperationException();
        }


        /** {@inheritDoc} */
        @Override
		public boolean remove( long entry ) {
            long[] values = _values;
            int[] set = _set;

            for ( int i = values.length; i-- > 0; ) {
                if ( ( set[i] != FREE && set[i] != REMOVED ) && entry == values[i] ) {
                    removeAt( i );
                    return true;
                }
            }
            return false;
        }


        /** {@inheritDoc} */
        @Override
		public boolean containsAll( Collection<?> collection ) {
            for ( Object element : collection ) {
                if ( element instanceof Long ) {
                    long ele = ( ( Long ) element ).longValue();
                    if ( ! TIntLongHashMap.this.containsValue( ele ) ) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return true;
        }


        /** {@inheritDoc} */
        @Override
		public boolean containsAll( TLongCollection collection ) {
            TLongIterator iter = collection.iterator();
            while ( iter.hasNext() ) {
                if ( ! TIntLongHashMap.this.containsValue( iter.next() ) ) {
                    return false;
                }
            }
            return true;
        }


        /** {@inheritDoc} */
        @Override
		public boolean containsAll( long[] array ) {
            for ( long element : array ) {
                if ( ! TIntLongHashMap.this.containsValue( element ) ) {
                    return false;
                }
            }
            return true;
        }


        /** {@inheritDoc} */
        @Override
		public boolean addAll( Collection<? extends Long> collection ) {
            throw new UnsupportedOperationException();
        }


        /** {@inheritDoc} */
        @Override
		public boolean addAll( TLongCollection collection ) {
            throw new UnsupportedOperationException();
        }


        /** {@inheritDoc} */
        @Override
		public boolean addAll( long[] array ) {
            throw new UnsupportedOperationException();
        }


        /** {@inheritDoc} */
        @Override
		@SuppressWarnings({"SuspiciousMethodCalls"})
        public boolean retainAll( Collection<?> collection ) {
            boolean modified = false;
            TLongIterator iter = iterator();
            while ( iter.hasNext() ) {
                if ( ! collection.contains( Long.valueOf ( iter.next() ) ) ) {
                    iter.remove();
                    modified = true;
                }
            }
            return modified;
        }


        /** {@inheritDoc} */
        @Override
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
        @Override
		public boolean retainAll( long[] array ) {
            boolean changed = false;
            Arrays.sort( array );
            long[] values = _values;
            byte[] states = _states;

            for ( int i = values.length; i-- > 0; ) {
                if ( states[i] == FULL && ( Arrays.binarySearch( array, values[i] ) < 0) ) {
                    removeAt( i );
                    changed = true;
                }
            }
            return changed;
        }


        /** {@inheritDoc} */
        @Override
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
        @Override
		public boolean removeAll( TLongCollection collection ) {
            if ( this == collection ) {
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
        @Override
		public boolean removeAll( long[] array ) {
            boolean changed = false;
            for ( int i = array.length; i-- > 0; ) {
                if ( remove( array[i] ) ) {
                    changed = true;
                }
            }
            return changed;
        }


        /** {@inheritDoc} */
        @Override
		public void clear() {
            TIntLongHashMap.this.clear();
        }


        /** {@inheritDoc} */
        @Override
		public boolean forEach( TLongProcedure procedure ) {
            return TIntLongHashMap.this.forEachValue( procedure );
        }


        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder( "{" );
            forEachValue( new TLongProcedure() {
                private boolean first = true;

                @Override
				public boolean execute( long value ) {
                    if ( first ) {
                        first = false;
                    } else {
                        buf.append( ", " );
                    }

                    buf.append( value );
                    return true;
                }
            } );
            buf.append( "}" );
            return buf.toString();
        }
    }


    class TIntLongKeyHashIterator extends THashPrimitiveIterator implements TIntIterator {

        /**
         * Creates an iterator over the specified map
         *
         * @param hash the <tt>TPrimitiveHash</tt> we will be iterating over.
         */
        TIntLongKeyHashIterator( TPrimitiveHash hash ) {
            super( hash );
        }

        /** {@inheritDoc} */
        @Override
		public int next() {
            moveToNextIndex();
            return _set[_index];
        }

        /** @{inheritDoc} */
        @Override
		public void remove() {
            if ( _expectedSize != _hash.size() ) {
                throw new ConcurrentModificationException();
            }

            // Disable auto compaction during the remove. This is a workaround for bug 1642768.
            try {
                _hash.tempDisableAutoCompaction();
                TIntLongHashMap.this.removeAt( _index );
            }
            finally {
                _hash.reenableAutoCompaction( false );
            }

            _expectedSize--;
        }
    }


   
    class TIntLongValueHashIterator extends THashPrimitiveIterator implements TLongIterator {

        /**
         * Creates an iterator over the specified map
         *
         * @param hash the <tt>TPrimitiveHash</tt> we will be iterating over.
         */
        TIntLongValueHashIterator( TPrimitiveHash hash ) {
            super( hash );
        }

        /** {@inheritDoc} */
        @Override
		public long next() {
            moveToNextIndex();
            return _values[_index];
        }

        /** @{inheritDoc} */
        @Override
		public void remove() {
            if ( _expectedSize != _hash.size() ) {
                throw new ConcurrentModificationException();
            }

            // Disable auto compaction during the remove. This is a workaround for bug 1642768.
            try {
                _hash.tempDisableAutoCompaction();
                TIntLongHashMap.this.removeAt( _index );
            }
            finally {
                _hash.reenableAutoCompaction( false );
            }

            _expectedSize--;
        }
    }


    class TIntLongHashIterator extends THashPrimitiveIterator implements TIntLongIterator {

        /**
         * Creates an iterator over the specified map
         *
         * @param map the <tt>TIntLongHashMap</tt> we will be iterating over.
         */
        TIntLongHashIterator( TIntLongHashMap map ) {
            super( map );
        }

        /** {@inheritDoc} */
        @Override
		public void advance() {
            moveToNextIndex();
        }

        /** {@inheritDoc} */
        @Override
		public int key() {
            return _set[_index];
        }

        /** {@inheritDoc} */
        @Override
		public long value() {
            return _values[_index];
        }

        /** {@inheritDoc} */
        @Override
		public long setValue( long val ) {
            long old = value();
            _values[_index] = val;
            return old;
        }

        /** @{inheritDoc} */
        @Override
		public void remove() {
            if ( _expectedSize != _hash.size() ) {
                throw new ConcurrentModificationException();
            }
            // Disable auto compaction during the remove. This is a workaround for bug 1642768.
            try {
                _hash.tempDisableAutoCompaction();
                TIntLongHashMap.this.removeAt( _index );
            }
            finally {
                _hash.reenableAutoCompaction( false );
            }
            _expectedSize--;
        }
    }


    /** {@inheritDoc} */
    @Override
    public boolean equals( Object other ) {
        if ( ! ( other instanceof TIntLongMap ) ) {
            return false;
        }
        TIntLongMap that = ( TIntLongMap ) other;
        if ( that.size() != this.size() ) {
            return false;
        }
        long[] values = _values;
        byte[] states = _states;
        long this_no_entry_value = getNoEntryValue();
        long that_no_entry_value = that.getNoEntryValue();
        for ( int i = values.length; i-- > 0; ) {
            if ( states[i] == FULL ) {
                int key = _set[i];
                long that_value = that.get( key );
                long this_value = values[i];
                if ( ( this_value != that_value ) &&
                     ( this_value != this_no_entry_value ) &&
                     ( that_value != that_no_entry_value ) ) {
                    return false;
                }
            }
        }
        return true;
    }


    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int hashcode = 0;
        byte[] states = _states;
        for ( int i = _values.length; i-- > 0; ) {
            if ( states[i] == FULL ) {
                hashcode += HashFunctions.hash( _set[i] ) ^
                            HashFunctions.hash( _values[i] );
            }
        }
        return hashcode;
    }


    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder( "{" );
        forEachEntry( new TIntLongProcedure() {
            private boolean first = true;
            @Override
			public boolean execute( int key, long value ) {
                if ( first ) first = false;
                else buf.append( ", " );

                buf.append(key);
                buf.append("=");
                buf.append(value);
                return true;
            }
        });
        buf.append( "}" );
        return buf.toString();
    }


    /** {@inheritDoc} */
    @Override
	public void writeExternal(ObjectOutput out) throws IOException {
        // VERSION
    	out.writeByte( 0 );

        // SUPER
    	super.writeExternal( out );

    	// NUMBER OF ENTRIES
    	out.writeInt( _size );

    	// ENTRIES
    	for ( int i = _states.length; i-- > 0; ) {
            if ( _states[i] == FULL ) {
                out.writeInt( _set[i] );
                out.writeLong( _values[i] );
            }
        }
    }


    /** {@inheritDoc} */
    @Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        // VERSION
    	in.readByte();

        // SUPER
    	super.readExternal( in );

    	// NUMBER OF ENTRIES
    	int size = in.readInt();
    	setUp( size );

    	// ENTRIES
        while (size-- > 0) {
            int key = in.readInt();
            long val = in.readLong();
            put(key, val);
        }
    }
} // TIntLongHashMap
