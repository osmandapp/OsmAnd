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

package gnu.trove.impl.hash;

import gnu.trove.procedure.TIntProcedure;
import gnu.trove.impl.HashFunctions;
import gnu.trove.impl.Constants;

import java.util.Arrays;

//////////////////////////////////////////////////
// THIS IS A GENERATED CLASS. DO NOT HAND EDIT! //
//////////////////////////////////////////////////


/**
 * An open addressed hashing implementation for int primitives.
 *
 * Created: Sun Nov  4 08:56:06 2001
 *
 * @author Eric D. Friedman, Rob Eden, Jeff Randall
 * @version $Id: _E_Hash.template,v 1.1.2.6 2009/11/07 03:36:44 robeden Exp $
 */
abstract public class TIntHash extends TPrimitiveHash {

    /** the set of ints */
    public transient int[] _set;

    /**
     * value that represents null
     *
     * NOTE: should not be modified after the Hash is created, but is
     *       not final because of Externalization
     *
     */
    protected int no_entry_value;


    /**
     * Creates a new <code>TIntHash</code> instance with the default
     * capacity and load factor.
     */
    public TIntHash() {
        super();
        no_entry_value = Constants.DEFAULT_INT_NO_ENTRY_VALUE;
        //noinspection RedundantCast
        if ( no_entry_value != ( int ) 0 ) {
            Arrays.fill( _set, no_entry_value );
        }
    }


    /**
     * Creates a new <code>TIntHash</code> instance whose capacity
     * is the next highest prime above <tt>initialCapacity + 1</tt>
     * unless that value is already prime.
     *
     * @param initialCapacity an <code>int</code> value
     */
    public TIntHash( int initialCapacity ) {
        super( initialCapacity );
        no_entry_value = Constants.DEFAULT_INT_NO_ENTRY_VALUE;
        //noinspection RedundantCast
        if ( no_entry_value != ( int ) 0 ) {
            Arrays.fill( _set, no_entry_value );
        }
    }


    /**
     * Creates a new <code>TIntHash</code> instance with a prime
     * value at or near the specified capacity and load factor.
     *
     * @param initialCapacity used to find a prime capacity for the table.
     * @param loadFactor used to calculate the threshold over which
     * rehashing takes place.
     */
    public TIntHash( int initialCapacity, float loadFactor ) {
        super(initialCapacity, loadFactor);
        no_entry_value = Constants.DEFAULT_INT_NO_ENTRY_VALUE;
        //noinspection RedundantCast
        if ( no_entry_value != ( int ) 0 ) {
            Arrays.fill( _set, no_entry_value );
        }
    }


    /**
     * Creates a new <code>TIntHash</code> instance with a prime
     * value at or near the specified capacity and load factor.
     *
     * @param initialCapacity used to find a prime capacity for the table.
     * @param loadFactor used to calculate the threshold over which
     * rehashing takes place.
     * @param no_entry_value value that represents null
     */
    public TIntHash( int initialCapacity, float loadFactor, int no_entry_value ) {
        super(initialCapacity, loadFactor);
        this.no_entry_value = no_entry_value;
        //noinspection RedundantCast
        if ( no_entry_value != ( int ) 0 ) {
            Arrays.fill( _set, no_entry_value );
        }
    }


    /**
     * Returns the value that is used to represent null. The default
     * value is generally zero, but can be changed during construction
     * of the collection.
     *
     * @return the value that represents null
     */
    public int getNoEntryValue() {
        return no_entry_value;
    }


    /**
     * initializes the hashtable to a prime capacity which is at least
     * <tt>initialCapacity + 1</tt>.
     *
     * @param initialCapacity an <code>int</code> value
     * @return the actual capacity chosen
     */
    protected int setUp( int initialCapacity ) {
        int capacity;

        capacity = super.setUp( initialCapacity );
        _set = new int[capacity];
        return capacity;
    }


    /**
     * Searches the set for <tt>val</tt>
     *
     * @param val an <code>int</code> value
     * @return a <code>boolean</code> value
     */
    public boolean contains( int val ) {
        return index(val) >= 0;
    }


    /**
     * Executes <tt>procedure</tt> for each element in the set.
     *
     * @param procedure a <code>TObjectProcedure</code> value
     * @return false if the loop over the set terminated because
     * the procedure returned false for some value.
     */
    public boolean forEach( TIntProcedure procedure ) {
        byte[] states = _states;
        int[] set = _set;
        for ( int i = set.length; i-- > 0; ) {
            if ( states[i] == FULL && ! procedure.execute( set[i] ) ) {
                return false;
            }
        }
        return true;
    }


    /**
     * Releases the element currently stored at <tt>index</tt>.
     *
     * @param index an <code>int</code> value
     */
    protected void removeAt( int index ) {
        _set[index] = no_entry_value;
        super.removeAt( index );
    }


    /**
     * Locates the index of <tt>val</tt>.
     *
     * @param val an <code>int</code> value
     * @return the index of <tt>val</tt> or -1 if it isn't in the set.
     */
    protected int index( int val ) {
        int hash, probe, index, length;

        final byte[] states = _states;
        final int[] set = _set;
        length = states.length;
        hash = HashFunctions.hash( val ) & 0x7fffffff;
        index = hash % length;

        if ( states[index] != FREE &&
            ( states[index] == REMOVED || set[index] != val ) ) {
            // see Knuth, p. 529
            probe = 1 + ( hash % ( length - 2 ) );

            do {
                index -= probe;
                if ( index < 0 ) {
                    index += length;
                }
            } while ( states[index] != FREE &&
                     ( states[index] == REMOVED || set[index] != val ) );
        }

        return states[index] == FREE ? -1 : index;
    }


    /**
     * Locates the index at which <tt>val</tt> can be inserted.  if
     * there is already a value equal()ing <tt>val</tt> in the set,
     * returns that value as a negative integer.
     *
     * @param val an <code>int</code> value
     * @return an <code>int</code> value
     */
    protected int insertionIndex( int val ) {
        int hash, probe, index, length;

        final byte[] states = _states;
        final int[] set = _set;
        length = states.length;
        hash = HashFunctions.hash( val ) & 0x7fffffff;
        index = hash % length;

        if ( states[index] == FREE ) {
            return index;       // empty, all done
        } else if ( states[index] == FULL && set[index] == val ) {
            return -index -1;   // already stored
        } else {                // already FULL or REMOVED, must probe
            // compute the double hash
            probe = 1 + ( hash % ( length - 2 ) );

            // if the slot we landed on is FULL (but not removed), probe
            // until we find an empty slot, a REMOVED slot, or an element
            // equal to the one we are trying to insert.
            // finding an empty slot means that the value is not present
            // and that we should use that slot as the insertion point;
            // finding a REMOVED slot means that we need to keep searching,
            // however we want to remember the offset of that REMOVED slot
            // so we can reuse it in case a "new" insertion (i.e. not an update)
            // is possible.
            // finding a matching value means that we've found that our desired
            // key is already in the table

            if ( states[index] != REMOVED ) {
				// starting at the natural offset, probe until we find an
				// offset that isn't full.
				do {
					index -= probe;
					if (index < 0) {
						index += length;
					}
				} while ( states[index] == FULL && set[index] != val );
            }

            // if the index we found was removed: continue probing until we
            // locate a free location or an element which equal()s the
            // one we have.
            if ( states[index] == REMOVED) {
                int firstRemoved = index;
                while ( states[index] != FREE &&
                       ( states[index] == REMOVED || set[index] != val ) ) {
                    index -= probe;
                    if (index < 0) {
                        index += length;
                    }
                }
                return states[index] == FULL ? -index -1 : firstRemoved;
            }
            // if it's full, the key is already stored
            return states[index] == FULL ? -index -1 : index;
        }
    }
} // TIntHash
