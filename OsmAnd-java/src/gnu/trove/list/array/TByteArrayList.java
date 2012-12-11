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

import gnu.trove.function.TByteFunction;
import gnu.trove.list.TByteList;
import gnu.trove.procedure.TByteProcedure;
import gnu.trove.iterator.TByteIterator;
import gnu.trove.TByteCollection;
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
 * A resizable, array-backed list of byte primitives.
 */
public class TByteArrayList implements TByteList, Externalizable {
	static final long serialVersionUID = 1L;

    /** the data of the list */
    protected byte[] _data;

    /** the index after the last entry in the list */
    protected int _pos;

    /** the default capacity for new lists */
    protected static final int DEFAULT_CAPACITY = Constants.DEFAULT_CAPACITY;

    /** the byte value that represents null */
    protected byte no_entry_value;


    /**
     * Creates a new <code>TByteArrayList</code> instance with the
     * default capacity.
     */
    @SuppressWarnings({"RedundantCast"})
    public TByteArrayList() {
        this( DEFAULT_CAPACITY, ( byte ) 0 );
    }


    /**
     * Creates a new <code>TByteArrayList</code> instance with the
     * specified capacity.
     *
     * @param capacity an <code>int</code> value
     */
    @SuppressWarnings({"RedundantCast"})
    public TByteArrayList( int capacity ) {
        this( capacity, ( byte ) 0 );
    }


    /**
     * Creates a new <code>TByteArrayList</code> instance with the
     * specified capacity.
     *
     * @param capacity an <code>int</code> value
     * @param no_entry_value an <code>byte</code> value that represents null.
     */
    public TByteArrayList( int capacity, byte no_entry_value ) {
        _data = new byte[ capacity ];
        _pos = 0;
        this.no_entry_value = no_entry_value;
    }

    /**
     * Creates a new <code>TByteArrayList</code> instance that contains
     * a copy of the collection passed to us.
     *
     * @param collection the collection to copy
     */
    public TByteArrayList ( TByteCollection collection ) {
        this( collection.size() );
        addAll( collection ); 
    }


    /**
     * Creates a new <code>TByteArrayList</code> instance whose
     * capacity is the length of <tt>values</tt> array and whose
     * initial contents are the specified values.
     *
     * @param values an <code>byte[]</code> value
     */
    public TByteArrayList( byte[] values ) {
        this( values.length );
        add( values );
    }


    /** {@inheritDoc} */
    @Override
	public byte getNoEntryValue() {
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
            byte[] tmp = new byte[ newCap ];
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
            byte[] tmp = new byte[ size() ];
            toArray( tmp, 0, tmp.length );
            _data = tmp;
        }
    }


    // modifying

    /** {@inheritDoc} */
    @Override
	public boolean add( byte val ) {
        ensureCapacity( _pos + 1 );
        _data[ _pos++ ] = val;
        return true;
    }


    /** {@inheritDoc} */
    @Override
	public void add( byte[] vals ) {
        add( vals, 0, vals.length );
    }


    /** {@inheritDoc} */
    @Override
	public void add( byte[] vals, int offset, int length ) {
        ensureCapacity( _pos + length );
        System.arraycopy( vals, offset, _data, _pos, length );
        _pos += length;
    }


    /** {@inheritDoc} */
    @Override
	public void insert( int offset, byte value ) {
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
	public void insert( int offset, byte[] values ) {
        insert( offset, values, 0, values.length );
    }


    /** {@inheritDoc} */
    @Override
	public void insert( int offset, byte[] values, int valOffset, int len ) {
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
	public byte get( int offset ) {
        if ( offset >= _pos ) {
            throw new ArrayIndexOutOfBoundsException( offset );
        }
        return _data[ offset ];
    }


    /**
     * Returns the value at the specified offset without doing any bounds checking.
     */
    public byte getQuick( int offset ) {
        return _data[ offset ];
    }


    /** {@inheritDoc} */
    @Override
	public byte set( int offset, byte val ) {
        if ( offset >= _pos ) {
            throw new ArrayIndexOutOfBoundsException( offset );
        }

		byte prev_val = _data[ offset ];
        _data[ offset ] = val;
		return prev_val;
    }


    /** {@inheritDoc} */
    @Override
	public byte replace( int offset, byte val ) {
        if ( offset >= _pos ) {
            throw new ArrayIndexOutOfBoundsException( offset );
        }
        byte old = _data[ offset ];
        _data[ offset ] = val;
        return old;
    }


    /** {@inheritDoc} */
    @Override
	public void set( int offset, byte[] values ) {
        set( offset, values, 0, values.length );
    }


    /** {@inheritDoc} */
    @Override
	public void set( int offset, byte[] values, int valOffset, int length ) {
        if ( offset < 0 || offset + length > _pos ) {
            throw new ArrayIndexOutOfBoundsException( offset );
        }
        System.arraycopy( values, valOffset, _data, offset, length );
    }


    /**
     * Sets the value at the specified offset without doing any bounds checking.
     */
    public void setQuick( int offset, byte val ) {
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
        _data = new byte[ capacity ];
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
	public boolean remove( byte value ) {
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
	public byte removeAt( int offset ) {
        byte old = get( offset );
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
	public TByteIterator iterator() {
        return new TByteArrayIterator( 0 );
    }


    /** {@inheritDoc} */
    @Override
	public boolean containsAll( Collection<?> collection ) {
        for ( Object element : collection ) {
            if ( element instanceof Byte ) {
                byte c = ( ( Byte ) element ).byteValue();
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
	public boolean containsAll( TByteCollection collection ) {
        if ( this == collection ) {
            return true;
        }
        TByteIterator iter = collection.iterator();
        while ( iter.hasNext() ) {
            byte element = iter.next();
            if ( ! contains( element ) ) {
                return false;
            }
        }
        return true;
    }


    /** {@inheritDoc} */
    @Override
	public boolean containsAll( byte[] array ) {
        for ( int i = array.length; i-- > 0; ) {
            if ( ! contains( array[i] ) ) {
                return false;
            }
        }
        return true;
    }


    /** {@inheritDoc} */
    @Override
	public boolean addAll( Collection<? extends Byte> collection ) {
        boolean changed = false;
        for ( Byte element : collection ) {
            byte e = element.byteValue();
            if ( add( e ) ) {
                changed = true;
            }
        }
        return changed;
    }


    /** {@inheritDoc} */
    @Override
	public boolean addAll( TByteCollection collection ) {
        boolean changed = false;
        TByteIterator iter = collection.iterator();
        while ( iter.hasNext() ) {
            byte element = iter.next();
            if ( add( element ) ) {
                changed = true;
            }
        }
        return changed;
    }


    /** {@inheritDoc} */
    @Override
	public boolean addAll( byte[] array ) {
        boolean changed = false;
        for ( byte element : array ) {
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
	    TByteIterator iter = iterator();
	    while ( iter.hasNext() ) {
	        if ( ! collection.contains( Byte.valueOf ( iter.next() ) ) ) {
		        iter.remove();
		        modified = true;
	        }
	    }
	    return modified;
    }


    /** {@inheritDoc} */
    @Override
	public boolean retainAll( TByteCollection collection ) {
        if ( this == collection ) {
            return false;
        }
        boolean modified = false;
	    TByteIterator iter = iterator();
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
	public boolean retainAll( byte[] array ) {
        boolean changed = false;
        Arrays.sort( array );
        byte[] data = _data;

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
            if ( element instanceof Byte ) {
                byte c = ( ( Byte ) element ).byteValue();
                if ( remove( c ) ) {
                    changed = true;
                }
            }
        }
        return changed;
    }


    /** {@inheritDoc} */
    @Override
	public boolean removeAll( TByteCollection collection ) {
        if ( collection == this ) {
            clear();
            return true;
        }
        boolean changed = false;
        TByteIterator iter = collection.iterator();
        while ( iter.hasNext() ) {
            byte element = iter.next();
            if ( remove( element ) ) {
                changed = true;
            }
        }
        return changed;
    }


    /** {@inheritDoc} */
    @Override
	public boolean removeAll( byte[] array ) {
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
	public void transformValues( TByteFunction function ) {
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
        byte tmp = _data[ i ];
        _data[ i ] = _data[ j ];
        _data[ j ] = tmp;
    }


    // copying

    /** {@inheritDoc} */
    @Override
	public TByteList subList( int begin, int end ) {
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
        TByteArrayList list = new TByteArrayList( end - begin );
        for ( int i = begin; i < end; i++ ) {
        	list.add( _data[ i ] );
        }
        return list;
    }


    /** {@inheritDoc} */
    @Override
	public byte[] toArray() {
        return toArray( 0, _pos );
    }


    /** {@inheritDoc} */
    @Override
	public byte[] toArray( int offset, int len ) {
        byte[] rv = new byte[ len ];
        toArray( rv, offset, len );
        return rv;
    }


    /** {@inheritDoc} */
    @Override
	public byte[] toArray( byte[] dest ) {
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
	public byte[] toArray( byte[] dest, int offset, int len ) {
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
	public byte[] toArray( byte[] dest, int source_pos, int dest_pos, int len ) {
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
        else if ( other instanceof TByteArrayList ) {
            TByteArrayList that = ( TByteArrayList )other;
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
	public boolean forEach( TByteProcedure procedure ) {
        for ( int i = 0; i < _pos; i++ ) {
            if ( !procedure.execute( _data[ i ] ) ) {
                return false;
            }
        }
        return true;
    }


    /** {@inheritDoc} */
    @Override
	public boolean forEachDescending( TByteProcedure procedure ) {
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
	public void fill( byte val ) {
        Arrays.fill( _data, 0, _pos, val );
    }


    /** {@inheritDoc} */
    @Override
	public void fill( int fromIndex, int toIndex, byte val ) {
        if ( toIndex > _pos ) {
          ensureCapacity( toIndex );
          _pos = toIndex;
        }
        Arrays.fill( _data, fromIndex, toIndex, val );
    }


    // searching

    /** {@inheritDoc} */
    @Override
	public int binarySearch( byte value ) {
        return binarySearch( value, 0, _pos );
    }


    /** {@inheritDoc} */
    @Override
	public int binarySearch(byte value, int fromIndex, int toIndex) {
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
            byte midVal = _data[ mid ];

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
	public int indexOf( byte value ) {
        return indexOf( 0, value );
    }


    /** {@inheritDoc} */
    @Override
	public int indexOf( int offset, byte value ) {
        for ( int i = offset; i < _pos; i++ ) {
            if ( _data[ i ] == value ) {
                return i;
            }
        }
        return -1;
    }


    /** {@inheritDoc} */
    @Override
	public int lastIndexOf( byte value ) {
        return lastIndexOf( _pos, value );
    }


    /** {@inheritDoc} */
    @Override
	public int lastIndexOf( int offset, byte value ) {
        for ( int i = offset; i-- > 0; ) {
            if ( _data[ i ] == value ) {
                return i;
            }
        }
        return -1;
    }


    /** {@inheritDoc} */
    @Override
	public boolean contains( byte value ) {
        return lastIndexOf( value ) >= 0;
    }


    /** {@inheritDoc} */
    @Override
	public TByteList grep( TByteProcedure condition ) {
        TByteArrayList list = new TByteArrayList();
        for ( int i = 0; i < _pos; i++ ) {
            if ( condition.execute( _data[ i ] ) ) {
                list.add( _data[ i ] );
            }
        }
        return list;
    }


    /** {@inheritDoc} */
    @Override
	public TByteList inverseGrep( TByteProcedure condition ) {
        TByteArrayList list = new TByteArrayList();
        for ( int i = 0; i < _pos; i++ ) {
            if ( !condition.execute( _data[ i ] ) ) {
                list.add( _data[ i ] );
            }
        }
        return list;
    }


    /** {@inheritDoc} */
    @Override
	public byte max() {
        if ( size() == 0 ) {
            throw new IllegalStateException("cannot find maximum of an empty list");
        }
        byte max = Byte.MIN_VALUE;
        for ( int i = 0; i < _pos; i++ ) {
        	if ( _data[ i ] > max ) {
        		max = _data[ i ];
        	}
        }
        return max;
    }


    /** {@inheritDoc} */
    @Override
	public byte min() {
        if ( size() == 0 ) {
            throw new IllegalStateException( "cannot find minimum of an empty list" );
        }
        byte min = Byte.MAX_VALUE;
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


    /** TByteArrayList iterator */
    class TByteArrayIterator implements TByteIterator {

        /** Index of element to be returned by subsequent call to next. */
        private int cursor = 0;

        /**
         * Index of element returned by most recent call to next or
         * previous.  Reset to -1 if this element is deleted by a call
         * to remove.
         */
        int lastRet = -1;


        TByteArrayIterator( int index ) {
            cursor = index;
        }


        /** {@inheritDoc} */
        @Override
		public boolean hasNext() {
            return cursor < size();
	    }


        /** {@inheritDoc} */
        @Override
		public byte next() {
            try {
                byte next = get( cursor );
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
                TByteArrayList.this.remove( lastRet, 1);
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
    	out.writeByte( no_entry_value );

    	// ENTRIES
    	int len = _data.length;
    	out.writeInt( len );
    	for( int i = 0; i < len; i++ ) {
    		out.writeByte( _data[ i ] );
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
    	no_entry_value = in.readByte();

    	// ENTRIES
    	int len = in.readInt();
    	_data = new byte[ len ];
    	for( int i = 0; i < len; i++ ) {
    		_data[ i ] = in.readByte();
    	}
    }
} // TByteArrayList
