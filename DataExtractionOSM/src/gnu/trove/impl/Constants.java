// ////////////////////////////////////////////////////////////////////////////
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
// ////////////////////////////////////////////////////////////////////////////
package gnu.trove.impl;

/**
 * Central location for constants needed by various implementations.
 */
public class Constants {

    private static final boolean VERBOSE =
            System.getProperty( "gnu.trove.verbose", null ) != null;

    /** the default capacity for new collections */
    public static final int DEFAULT_CAPACITY = 10;

    /** the load above which rehashing occurs. */
    public static final float DEFAULT_LOAD_FACTOR = 0.5f;


    /** the default value that represents for <tt>byte</tt> types. */
    public static final byte DEFAULT_BYTE_NO_ENTRY_VALUE;
    static {
        byte value;
        String property = System.getProperty( "gnu.trove.no_entry.byte", "0" );
        if ( "MAX_VALUE".equalsIgnoreCase( property ) ) value = Byte.MAX_VALUE;
        else if ( "MIN_VALUE".equalsIgnoreCase( property ) ) value = Byte.MIN_VALUE;
        else value = Byte.valueOf( property );

        if ( value > Byte.MAX_VALUE ) value = Byte.MAX_VALUE;
        else if ( value < Byte.MIN_VALUE ) value = Byte.MIN_VALUE;
        DEFAULT_BYTE_NO_ENTRY_VALUE = value;
        if ( VERBOSE ) {
            System.out.println( "DEFAULT_BYTE_NO_ENTRY_VALUE: " +
                                DEFAULT_BYTE_NO_ENTRY_VALUE );
        }
    }


    /** the default value that represents for <tt>short</tt> types. */
    public static final short DEFAULT_SHORT_NO_ENTRY_VALUE;
    static {
        short value;
        String property = System.getProperty( "gnu.trove.no_entry.short", "0" );
        if ( "MAX_VALUE".equalsIgnoreCase( property ) ) value = Short.MAX_VALUE;
        else if ( "MIN_VALUE".equalsIgnoreCase( property ) ) value = Short.MIN_VALUE;
        else value = Short.valueOf( property );

        if ( value > Short.MAX_VALUE ) value = Short.MAX_VALUE;
        else if ( value < Short.MIN_VALUE ) value = Short.MIN_VALUE;
        DEFAULT_SHORT_NO_ENTRY_VALUE = value;
        if ( VERBOSE ) {
            System.out.println( "DEFAULT_SHORT_NO_ENTRY_VALUE: " +
                                DEFAULT_SHORT_NO_ENTRY_VALUE );
        }
    }


    /** the default value that represents for <tt>char</tt> types. */
    public static final char DEFAULT_CHAR_NO_ENTRY_VALUE;
    static {
        char value;
        String property = System.getProperty( "gnu.trove.no_entry.char", "\0" );
        if ( "MAX_VALUE".equalsIgnoreCase( property ) ) value = Character.MAX_VALUE;
        else if ( "MIN_VALUE".equalsIgnoreCase( property ) ) value = Character.MIN_VALUE;
        else value = property.toCharArray()[0];

        if ( value > Character.MAX_VALUE ) value = Character.MAX_VALUE;
        else if ( value < Character.MIN_VALUE ) value = Character.MIN_VALUE;
        DEFAULT_CHAR_NO_ENTRY_VALUE = value;
        if ( VERBOSE ) {
            System.out.println( "DEFAULT_CHAR_NO_ENTRY_VALUE: " +
                                Integer.valueOf( value ) );
        }
    }


    /** the default value that represents for <tt>int</tt> types. */
    public static final int DEFAULT_INT_NO_ENTRY_VALUE;
     static {
        int value;
        String property = System.getProperty( "gnu.trove.no_entry.int", "0" );
        if ( "MAX_VALUE".equalsIgnoreCase( property ) ) value = Integer.MAX_VALUE;
        else if ( "MIN_VALUE".equalsIgnoreCase( property ) ) value = Integer.MIN_VALUE;
        else value = Integer.valueOf( property );
        DEFAULT_INT_NO_ENTRY_VALUE = value;
        if ( VERBOSE ) {
            System.out.println( "DEFAULT_INT_NO_ENTRY_VALUE: " +
                                DEFAULT_INT_NO_ENTRY_VALUE );
        }
    }


    /** the default value that represents for <tt>long</tt> types. */
    public static final long DEFAULT_LONG_NO_ENTRY_VALUE;
    static {
        long value;
        String property = System.getProperty( "gnu.trove.no_entry.long", "0" );
        if ( "MAX_VALUE".equalsIgnoreCase( property ) ) value = Long.MAX_VALUE;
        else if ( "MIN_VALUE".equalsIgnoreCase( property ) ) value = Long.MIN_VALUE;
        else value = Long.valueOf( property );
        DEFAULT_LONG_NO_ENTRY_VALUE = value;
        if ( VERBOSE ) {
            System.out.println( "DEFAULT_LONG_NO_ENTRY_VALUE: " +
                                DEFAULT_LONG_NO_ENTRY_VALUE );
        }
    }


    /** the default value that represents for <tt>float</tt> types. */
    public static final float DEFAULT_FLOAT_NO_ENTRY_VALUE;
    static {
        float value;
        String property = System.getProperty( "gnu.trove.no_entry.float", "0" );
        if ( "MAX_VALUE".equalsIgnoreCase( property ) ) value = Float.MAX_VALUE;
        else if ( "MIN_VALUE".equalsIgnoreCase( property ) ) value = Float.MIN_VALUE;
        // Value from Float.MIN_NORMAL (introduced in 1.6)
        else if ( "MIN_NORMAL".equalsIgnoreCase( property ) ) value = 0x1.0p-126f;
        else if ( "NEGATIVE_INFINITY".equalsIgnoreCase( property ) ) value = Float.NEGATIVE_INFINITY;
        else if ( "POSITIVE_INFINITY".equalsIgnoreCase( property ) ) value = Float.POSITIVE_INFINITY;
//        else if ( "NaN".equalsIgnoreCase( property ) ) value = Float.NaN;
        else value = Float.valueOf( property );
        DEFAULT_FLOAT_NO_ENTRY_VALUE = value;
        if ( VERBOSE ) {
            System.out.println( "DEFAULT_FLOAT_NO_ENTRY_VALUE: " +
                                DEFAULT_FLOAT_NO_ENTRY_VALUE );
        }
    }


    /** the default value that represents for <tt>double</tt> types. */
    public static final double DEFAULT_DOUBLE_NO_ENTRY_VALUE;
    static {
        double value;
        String property = System.getProperty( "gnu.trove.no_entry.double", "0" );
        if ( "MAX_VALUE".equalsIgnoreCase( property ) ) value = Double.MAX_VALUE;
        else if ( "MIN_VALUE".equalsIgnoreCase( property ) ) value = Double.MIN_VALUE;
        // Value from Double.MIN_NORMAL (introduced in 1.6)
        else if ( "MIN_NORMAL".equalsIgnoreCase( property ) ) value = 0x1.0p-1022;
        else if ( "NEGATIVE_INFINITY".equalsIgnoreCase( property ) ) value = Double.NEGATIVE_INFINITY;
        else if ( "POSITIVE_INFINITY".equalsIgnoreCase( property ) ) value = Double.POSITIVE_INFINITY;
//        else if ( "NaN".equalsIgnoreCase( property ) ) value = Double.NaN;
        else value = Double.valueOf( property );
        DEFAULT_DOUBLE_NO_ENTRY_VALUE = value;
        if ( VERBOSE ) {
            System.out.println( "DEFAULT_DOUBLE_NO_ENTRY_VALUE: " +
                                DEFAULT_DOUBLE_NO_ENTRY_VALUE );
        }
    }
}
