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

package gnu.trove.list.array;

import gnu.trove.function.TIntFunction;
import gnu.trove.list.TIntList;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.TIntCollection;
import gnu.trove.impl.*;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;


//////////////////////////////////////////////////
// THIS IS A GENERATED CLASS. DO NOT HAND EDIT! //
//////////////////////////////////////////////////


/**
 * A resizable, array-backed list of int primitives.
 */
public class TIntArrayList implements TIntList, Externalizable {
	static final long serialVersionUID = 1L;

    /** the data of the list */
    protected int[] _data;

    /** the index after the last entry in the list */
    protected int _pos;

    /** the default capacity for new lists */
    protected static final int DEFAULT_CAPACITY = Constants.DEFAULT_CAPACITY;

    /** the int value that represents null */
    protected int no_entry_value;


    /**
     * Creates a new <code>TIntArrayList</code> instance with the
     * default capacity.
     */
    @SuppressWarnings({"RedundantCast"})
    public TIntArrayList() {
        this( DEFAULT_CAPACITY, ( int ) 0 );
    }


    /**
     * Creates a new <code>TIntArrayList</code> instance with the
     * specified capacity.
     *
     * @param capacity an <code>int</code> value
     */
    @SuppressWarnings({"RedundantCast"})
    public TIntArrayList( int capacity ) {
        this( capacity, ( int ) 0 );
    }


    /**
     * Creates a new <code>TIntArrayList</code> instance with the
     * specified capacity.
     *
     * @param capacity an <code>int</code> value
     * @param no_entry_value an <code>int</code> value that represents null.
     */
    public TIntArrayList( int capacity, int no_entry_value ) {
        _data = new int[ capacity ];
        _pos = 0;
        this.no_entry_value = no_entry_value;
    }

    /**
     * Creates a new <code>TIntArrayList</code> instance that contains
     * a copy of the collection passed to us.
     *
     * @param collection the collection to copy
     */
    public TIntArrayList ( TIntCollection collection ) {
        this( collection.size() );
        addAll( collection ); 
    }


    /**
     * Creates a new <code>TIntArrayList</code> instance whose
     * capacity is the length of <tt>values</tt> array and whose
     * initial contents are the specified values.
     *
     * @param values an <code>int[]</code> value
     */
    public TIntArrayList( int[] values ) {
        this( values.length );
        add( values );
    }


    /** {@inheritDoc} */
    @Override
	public int getNoEntryValue() {
        return no_entry_value;
    }


    // sizing

    /**
     * Grow the internal array as needed to accommodate the specified number of elements.
     * The size of the array bytes on each resize unless capacity requires more than twice
     * the current capacity.
     */
    public void ensureCapacity( int capacity ) {
        if ( capacity > _data.length ) {
            int newCap = Math.max( _data.length << 1, capacity );
            int[] tmp = new int[ newCap ];
            System.arraycopy( _data, 0, tmp, 0, _data.length );
            _data = tmp;
        }
    }


    /** {@inheritDoc} */
    @Override
	public int size() {
        return _pos;
    }


    /** {@inheritDoc} */
    @Override
	public boolean isEmpty() {
        return _pos == 0;
    }


    /**
     * Sheds any excess capacity above and beyond the current size of the list.
     */
    public void trimToSize() {
        if ( _data.length > size() ) {
            int[] tmp = new int[ size() ];
            toArray( tmp, 0, tmp.length );
            _data = tmp;
        }
    }


    // modifying

    /** {@inheritDoc} */
    @Override
	public boolean add( int val ) {
        ensureCapacity( _pos + 1 );
        _data[ _pos++ ] = val;
        return true;
    }


    /** {@inheritDoc} */
    @Override
	public void add( int[] vals ) {
        add( vals, 0, vals.length );
    }


    /** {@inheritDoc} */
    @Override
	public void add( int[] vals, int offset, int length ) {
        ensureCapacity( _pos + length );
        System.arraycopy( vals, offset, _data, _pos, length );
        _pos += length;
    }


    /** {@inheritDoc} */
    @Override
	public void insert( int offset, int value ) {
        if ( offset == _pos ) {
            add( value );
            return;
        }
        ensureCapacity( _pos + 1 );
        // shift right
        System.arraycopy( _data, offset, _data, offset + 1, _pos - offset );
        // insert
        _data[ offset ] = value;
        _pos++;
    }


    /** {@inheritDoc} */
    @Override
	public void insert( int offset, int[] values ) {
        insert( offset, values, 0, values.length );
    }


    /** {@inheritDoc} */
    @Override
	public void insert( int offset, int[] values, int valOffset, int len ) {
        if ( offset == _pos ) {
            add( values, valOffset, len );
            return;
        }

        ensureCapacity( _pos + len );
        // shift right
        System.arraycopy( _data, offset, _data, offset + len, _pos - offset );
        // insert
        System.arraycopy( values, valOffset, _data, offset, len );
        _pos += len;
    }


    /** {@inheritDoc} */
    @Override
	public int get( int offset ) {
        if ( offset >= _pos ) {
            throw new ArrayIndexOutOfBoundsException( offset );
        }
        return _data[ offset ];
    }


    /**
     * Returns the value at the specified offset without doing any bounds checking.
     */
    public int getQuick( int offset ) {
        return _data[ offset ];
    }


    /** {@inheritDoc} */
    @Override
	public int set( int offset, int val ) {
        if ( offset >= _pos ) {
            throw new ArrayIndexOutOfBoundsException( offset );
        }

		int prev_val = _data[ offset ];
        _data[ offset ] = val;
		return prev_val;
    }


    /** {@inheritDoc} */
    @Override
	public int replace( int offset, int val ) {
        if ( offset >= _pos ) {
            throw new ArrayIndexOutOfBoundsException( offset );
        }
        int old = _data[ offset ];
        _data[ offset ] = val;
        return old;
    }


    /** {@inheritDoc} */
    @Override
	public void set( int offset, int[] values ) {
        set( offset, values, 0, values.length );
    }


    /** {@inheritDoc} */
    @Override
	public void set( int offset, int[] values, int valOffset, int length ) {
        if ( offset < 0 || offset + length > _pos ) {
            throw new ArrayIndexOutOfBoundsException( offset );
        }
        System.arraycopy( values, valOffset, _data, offset, length );
    }


    /**
     * Sets the value at the specified offset without doing any bounds checking.
     */
    public void setQuick( int offset, int val ) {
        _data[ offset ] = val;
    }


    /** {@inheritDoc} */
    @Override
	public void clear() {
        clear( DEFAULT_CAPACITY );
    }


    /**
     * Flushes the internal state of the list, setting the capacity of the empty list to
     * <tt>capacity</tt>.
     */
    public void clear( int capacity ) {
        _data = new int[ capacity ];
        _pos = 0;
    }


    /**
     * Sets the size of the list to 0, but does not change its capacity. This method can
     * be used as an alternative to the {@link #clear()} method if you want to recycle a
     * list without allocating new backing arrays.
     */
    public void reset() {
        _pos = 0;
        Arrays.fill( _data, no_entry_value );
    }


    /**
     * Sets the size of the list to 0, but does not change its capacity. This method can
     * be used as an alternative to the {@link #clear()} method if you want to recycle a
     * list without allocating new backing arrays. This method differs from
     * {@link #reset()} in that it does not clear the old values in the backing array.
     * Thus, it is possible for getQuick to return stale data if this method is used and
     * the caller is careless about bounds checking.
     */
    public void resetQuick() {
        _pos = 0;
    }


    /** {@inheritDoc} */
    @Override
	public boolean remove( int value ) {
        for ( int index = 0; index < _pos; index++ ) {
            if ( value == _data[index]  ) {
                remove( index, 1 );
                return true;
            }
        }
        return false;
    }


    /** {@inheritDoc} */
    @Override
	public int removeAt( int offset ) {
        int old = get( offset );
        remove( offset, 1 );
        return old;
    }


    /** {@inheritDoc} */
    @Override
	public void remove( int offset, int length ) {
        if ( offset < 0 || offset >= _pos ) {
            throw new ArrayIndexOutOfBoundsException(offset);
        }

        if ( offset == 0 ) {
            // data at the front
            System.arraycopy( _data, length, _data, 0, _pos - length );
        }
        else if ( _pos - length == offset ) {
            // no copy to make, decrementing pos "deletes" values at
            // the end
        }
        else {
            // data in the middle
            System.arraycopy( _data, offset + length, _data, offset,
                _pos - ( offset + length ) );
        }
        _pos -= length;
        // no need to clear old values beyond _pos, because this is a
        // primitive collection and 0 takes as much room as any other
        // value
    }


    /** {@inheritDoc} */
    @Override
	public TIntIterator iterator() {
        return new TIntArrayIterator( 0 );
    }


    /** {@inheritDoc} */
    @Override
	public boolean containsAll( Collection<?> collection ) {
        for ( Object element : collection ) {
            if ( element instanceof Integer ) {
                int c = ( ( Integer ) element ).intValue();
                if ( ! contains( c ) ) {
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
        if ( this == collection ) {
            return true;
        }
        TIntIterator iter = collection.iterator();
        while ( iter.hasNext() ) {
            int element = iter.next();
            if ( ! contains( element ) ) {
                return false;
            }
        }
        return true;
    }


    /** {@inheritDoc} */
    @Override
	public boolean containsAll( int[] array ) {
        for ( int i = array.length; i-- > 0; ) {
            if ( ! contains( array[i] ) ) {
                return false;
            }
        }
        return true;
    }


    /** {@inheritDoc} */
    @Override
	public boolean addAll( Collection<? extends Integer> collection ) {
        boolean changed = false;
        for ( Integer element : collection ) {
            int e = element.intValue();
            if ( add( e ) ) {
                changed = true;
            }
        }
        return changed;
    }


    /** {@inheritDoc} */
    @Override
	public boolean addAll( TIntCollection collection ) {
        boolean changed = false;
        TIntIterator iter = collection.iterator();
        while ( iter.hasNext() ) {
            int element = iter.next();
            if ( add( element ) ) {
                changed = true;
            }
        }
        return changed;
    }


    /** {@inheritDoc} */
    @Override
	public boolean addAll( int[] array ) {
        boolean changed = false;
        for ( int element : array ) {
            if ( add( element ) ) {
                changed = true;
            }
        }
        return changed;
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
        int[] data = _data;

        for ( int i = data.length; i-- > 0; ) {
            if ( Arrays.binarySearch( array, data[i] ) < 0 ) {
                remove( i, 1 );
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
    @Override
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
    @Override
	public void transformValues( TIntFunction function ) {
        for ( int i = _pos; i-- > 0; ) {
            _data[ i ] = function.execute( _data[ i ] );
        }
    }


    /** {@inheritDoc} */
    @Override
	public void reverse() {
        reverse( 0, _pos );
    }


    /** {@inheritDoc} */
    @Override
	public void reverse( int from, int to ) {
        if ( from == to ) {
            return;             // nothing to do
        }
        if ( from > to ) {
            throw new IllegalArgumentException( "from cannot be greater than to" );
        }
        for ( int i = from, j = to - 1; i < j; i++, j-- ) {
            swap( i, j );
        }
    }


    /** {@inheritDoc} */
    @Override
	public void shuffle( Random rand ) {
        for ( int i = _pos; i-- > 1; ) {
            swap( i, rand.nextInt( i ) );
        }
    }


    /**
     * Swap the values at offsets <tt>i</tt> and <tt>j</tt>.
     *
     * @param i an offset into the data array
     * @param j an offset into the data array
     */
    private void swap( int i, int j ) {
        int tmp = _data[ i ];
        _data[ i ] = _data[ j ];
        _data[ j ] = tmp;
    }


    // copying

    /** {@inheritDoc} */
    @Override
	public TIntList subList( int begin, int end ) {
    	if ( end < begin ) {
			throw new IllegalArgumentException( "end index " + end +
				" greater than begin index " + begin );
		}
		if ( begin < 0 ) {
			throw new IndexOutOfBoundsException( "begin index can not be < 0" );
		}
		if ( end > _data.length ) {
			throw new IndexOutOfBoundsException( "end index < " + _data.length );
		}
        TIntArrayList list = new TIntArrayList( end - begin );
        for ( int i = begin; i < end; i++ ) {
        	list.add( _data[ i ] );
        }
        return list;
    }


    /** {@inheritDoc} */
    @Override
	public int[] toArray() {
        return toArray( 0, _pos );
    }


    /** {@inheritDoc} */
    @Override
	public int[] toArray( int offset, int len ) {
        int[] rv = new int[ len ];
        toArray( rv, offset, len );
        return rv;
    }


    /** {@inheritDoc} */
    @Override
	public int[] toArray( int[] dest ) {
        int len = dest.length;
        if ( dest.length > _pos ) {
            len = _pos;
            dest[len] = no_entry_value;
        }
        toArray( dest, 0, len );
        return dest;
    }


    /** {@inheritDoc} */
    @Override
	public int[] toArray( int[] dest, int offset, int len ) {
        if ( len == 0 ) {
            return dest;             // nothing to copy
        }
        if ( offset < 0 || offset >= _pos ) {
            throw new ArrayIndexOutOfBoundsException( offset );
        }
        System.arraycopy( _data, offset, dest, 0, len );
        return dest;
    }


    /** {@inheritDoc} */
    @Override
	public int[] toArray( int[] dest, int source_pos, int dest_pos, int len ) {
        if ( len == 0 ) {
            return dest;             // nothing to copy
        }
        if ( source_pos < 0 || source_pos >= _pos ) {
            throw new ArrayIndexOutOfBoundsException( source_pos );
        }
        System.arraycopy( _data, source_pos, dest, dest_pos, len );
        return dest;
    }


    // comparing

    /** {@inheritDoc} */
    @Override
    public boolean equals( Object other ) {
        if ( other == this ) {
            return true;
        }
        else if ( other instanceof TIntArrayList ) {
            TIntArrayList that = ( TIntArrayList )other;
            if ( that.size() != this.size() ) return false;
            else {
                for ( int i = _pos; i-- > 0; ) {
                    if ( this._data[ i ] != that._data[ i ] ) {
                        return false;
                    }
                }
                return true;
            }
        }
        else return false;
    }


    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int h = 0;
        for ( int i = _pos; i-- > 0; ) {
            h += HashFunctions.hash( _data[ i ] );
        }
        return h;
    }


    // procedures

    /** {@inheritDoc} */
    @Override
	public boolean forEach( TIntProcedure procedure ) {
        for ( int i = 0; i < _pos; i++ ) {
            if ( !procedure.execute( _data[ i ] ) ) {
                return false;
            }
        }
        return true;
    }


    /** {@inheritDoc} */
    @Override
	public boolean forEachDescending( TIntProcedure procedure ) {
        for ( int i = _pos; i-- > 0; ) {
            if ( !procedure.execute( _data[ i ] ) ) {
                return false;
            }
        }
        return true;
    }


    // sorting

    /** {@inheritDoc} */
    @Override
	public void sort() {
        Arrays.sort( _data, 0, _pos );
    }


    /** {@inheritDoc} */
    @Override
	public void sort( int fromIndex, int toIndex ) {
        Arrays.sort( _data, fromIndex, toIndex );
    }


    // filling

    /** {@inheritDoc} */
    @Override
	public void fill( int val ) {
        Arrays.fill( _data, 0, _pos, val );
    }


    /** {@inheritDoc} */
    @Override
	public void fill( int fromIndex, int toIndex, int val ) {
        if ( toIndex > _pos ) {
          ensureCapacity( toIndex );
          _pos = toIndex;
        }
        Arrays.fill( _data, fromIndex, toIndex, val );
    }


    // searching

    /** {@inheritDoc} */
    @Override
	public int binarySearch( int value ) {
        return binarySearch( value, 0, _pos );
    }


    /** {@inheritDoc} */
    @Override
	public int binarySearch(int value, int fromIndex, int toIndex) {
        if ( fromIndex < 0 ) {
            throw new ArrayIndexOutOfBoundsException( fromIndex );
        }
        if ( toIndex > _pos ) {
            throw new ArrayIndexOutOfBoundsException( toIndex );
        }

        int low = fromIndex;
        int high = toIndex - 1;

        while ( low <= high ) {
            int mid = ( low + high ) >>> 1;
            int midVal = _data[ mid ];

            if ( midVal < value ) {
                low = mid + 1;
            }
            else if ( midVal > value ) {
                high = mid - 1;
            }
            else {
                return mid; // value found
            }
        }
        return -( low + 1 );  // value not found.
    }


    /** {@inheritDoc} */
    @Override
	public int indexOf( int value ) {
        return indexOf( 0, value );
    }


    /** {@inheritDoc} */
    @Override
	public int indexOf( int offset, int value ) {
        for ( int i = offset; i < _pos; i++ ) {
            if ( _data[ i ] == value ) {
                return i;
            }
        }
        return -1;
    }


    /** {@inheritDoc} */
    @Override
	public int lastIndexOf( int value ) {
        return lastIndexOf( _pos, value );
    }


    /** {@inheritDoc} */
    @Override
	public int lastIndexOf( int offset, int value ) {
        for ( int i = offset; i-- > 0; ) {
            if ( _data[ i ] == value ) {
                return i;
            }
        }
        return -1;
    }


    /** {@inheritDoc} */
    @Override
	public boolean contains( int value ) {
        return lastIndexOf( value ) >= 0;
    }


    /** {@inheritDoc} */
    @Override
	public TIntList grep( TIntProcedure condition ) {
        TIntArrayList list = new TIntArrayList();
        for ( int i = 0; i < _pos; i++ ) {
            if ( condition.execute( _data[ i ] ) ) {
                list.add( _data[ i ] );
            }
        }
        return list;
    }


    /** {@inheritDoc} */
    @Override
	public TIntList inverseGrep( TIntProcedure condition ) {
        TIntArrayList list = new TIntArrayList();
        for ( int i = 0; i < _pos; i++ ) {
            if ( !condition.execute( _data[ i ] ) ) {
                list.add( _data[ i ] );
            }
        }
        return list;
    }


    /** {@inheritDoc} */
    @Override
	public int max() {
        if ( size() == 0 ) {
            throw new IllegalStateException("cannot find maximum of an empty list");
        }
        int max = Integer.MIN_VALUE;
        for ( int i = 0; i < _pos; i++ ) {
        	if ( _data[ i ] > max ) {
        		max = _data[ i ];
        	}
        }
        return max;
    }


    /** {@inheritDoc} */
    @Override
	public int min() {
        if ( size() == 0 ) {
            throw new IllegalStateException( "cannot find minimum of an empty list" );
        }
        int min = Integer.MAX_VALUE;
        for ( int i = 0; i < _pos; i++ ) {
        	if ( _data[i] < min ) {
        		min = _data[i];
        	}
        }
        return min;
    }


    // stringification

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder( "{" );
        for ( int i = 0, end = _pos - 1; i < end; i++ ) {
            buf.append( _data[ i ] );
            buf.append( ", " );
        }
        if ( size() > 0 ) {
            buf.append( _data[ _pos - 1 ] );
        }
        buf.append( "}" );
        return buf.toString();
    }


    /** TIntArrayList iterator */
    class TIntArrayIterator implements TIntIterator {

        /** Index of element to be returned by subsequent call to next. */
        private int cursor = 0;

        /**
         * Index of element returned by most recent call to next or
         * previous.  Reset to -1 if this element is deleted by a call
         * to remove.
         */
        int lastRet = -1;


        TIntArrayIterator( int index ) {
            cursor = index;
        }


        /** {@inheritDoc} */
        @Override
		public boolean hasNext() {
            return cursor < size();
	    }


        /** {@inheritDoc} */
        @Override
		public int next() {
            try {
                int next = get( cursor );
                lastRet = cursor++;
                return next;
            } catch ( IndexOutOfBoundsException e ) {
                throw new NoSuchElementException();
            }
        }


        /** {@inheritDoc} */
        @Override
		public void remove() {
            if ( lastRet == -1 )
		        throw new IllegalStateException();

            try {
                TIntArrayList.this.remove( lastRet, 1);
                if ( lastRet < cursor )
                    cursor--;
                lastRet = -1;
            } catch ( IndexOutOfBoundsException e ) {
                throw new ConcurrentModificationException();
            }
        }
    }


    @Override
	public void writeExternal( ObjectOutput out ) throws IOException {
    	// VERSION
    	out.writeByte( 0 );

    	// POSITION
    	out.writeInt( _pos );

    	// NO_ENTRY_VALUE
    	out.writeInt( no_entry_value );

    	// ENTRIES
    	int len = _data.length;
    	out.writeInt( len );
    	for( int i = 0; i < len; i++ ) {
    		out.writeInt( _data[ i ] );
    	}
    }


    @Override
	public void readExternal( ObjectInput in )
    	throws IOException, ClassNotFoundException {

    	// VERSION
    	in.readByte();

    	// POSITION
    	_pos = in.readInt();

    	// NO_ENTRY_VALUE
    	no_entry_value = in.readInt();

    	// ENTRIES
    	int len = in.readInt();
    	_data = new int[ len ];
    	for( int i = 0; i < len; i++ ) {
    		_data[ i ] = in.readInt();
    	}
    }
} // TIntArrayList
