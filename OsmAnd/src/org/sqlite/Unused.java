/*
 * Copyright (c) 2007 David Crawshaw <david@zentus.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package org.sqlite;

import java.io.*;
import java.math.*;
import java.net.*;
import java.sql.*;
import java.util.Map;

/** Unused JDBC functions from Statement, PreparedStatement and ResultSet.  */
abstract class Unused
{
    private SQLException unused() {
        return new SQLException("not implemented by SQLite JDBC driver");
    }


    // Statement ////////////////////////////////////////////////////

    public boolean execute(String sql, int[] colinds)
        throws SQLException { throw unused(); }
    public boolean execute(String sql, String[] colnames)
        throws SQLException { throw unused(); }
    public int executeUpdate(String sql, int autoKeys)
        throws SQLException { throw unused(); }
    public int executeUpdate(String sql, int[] colinds)
        throws SQLException { throw unused(); }
    public int executeUpdate(String sql, String[] cols)
        throws SQLException { throw unused(); }
    public boolean execute(String sql, int autokeys)
        throws SQLException { throw unused(); }


    // PreparedStatement ////////////////////////////////////////////

    public void setArray(int i, Array x)
        throws SQLException { throw unused(); }
    public void setAsciiStream(int parameterIndex, InputStream x, int length)
        throws SQLException { throw unused(); }
    public void setBigDecimal(int parameterIndex, BigDecimal x)
        throws SQLException { throw unused(); }
    public void setBinaryStream(int parameterIndex, InputStream x, int length)
        throws SQLException { throw unused(); }
    public void setBlob(int i, Blob x)
        throws SQLException { throw unused(); }
    public void setCharacterStream(int pos, Reader reader, int length)
        throws SQLException { throw unused(); }
    public void setClob(int i, Clob x)
        throws SQLException { throw unused(); }
    public void setRef(int i, Ref x)
        throws SQLException { throw unused(); }
    public void setUnicodeStream(int pos, InputStream x, int length)
        throws SQLException { throw unused(); }
    public void setURL(int pos, URL x)
        throws SQLException { throw unused(); }


    // ResultSet ////////////////////////////////////////////////////

    public Array getArray(int i)
        throws SQLException { throw unused(); }
    public Array getArray(String col)
        throws SQLException { throw unused(); }
    public InputStream getAsciiStream(int col)
        throws SQLException { throw unused(); }
    public InputStream getAsciiStream(String col)
        throws SQLException { throw unused(); }
    public BigDecimal getBigDecimal(int col)
        throws SQLException { throw unused(); }
    public BigDecimal getBigDecimal(int col, int s)
        throws SQLException { throw unused(); }
    public BigDecimal getBigDecimal(String col)
        throws SQLException { throw unused(); }
    public BigDecimal getBigDecimal(String col, int s)
        throws SQLException { throw unused(); }
    public InputStream getBinaryStream(int col)
        throws SQLException { throw unused(); }
    public InputStream getBinaryStream(String col)
        throws SQLException { throw unused(); }
    public Blob getBlob(int col)
        throws SQLException { throw unused(); }
    public Blob getBlob(String col)
        throws SQLException { throw unused(); }
    public Reader getCharacterStream(int col)
        throws SQLException { throw unused(); }
    public Reader getCharacterStream(String col)
        throws SQLException { throw unused(); }
    public Clob getClob(int col)
        throws SQLException { throw unused(); }
    public Clob getClob(String col)
        throws SQLException { throw unused(); }
    public Object getObject(int col, Map map)
        throws SQLException { throw unused(); }
    public Object getObject(String col, Map map)
        throws SQLException { throw unused(); }
    public Ref getRef(int i)
        throws SQLException { throw unused(); }
    public Ref getRef(String col)
        throws SQLException { throw unused(); }

    public InputStream getUnicodeStream(int col)
        throws SQLException { throw unused(); }
    public InputStream getUnicodeStream(String col)
        throws SQLException { throw unused(); }
    public URL getURL(int col)
        throws SQLException { throw unused(); }
    public URL getURL(String col)
        throws SQLException { throw unused(); }

    public void insertRow() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public void moveToCurrentRow() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public void moveToInsertRow() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public boolean last() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public boolean previous() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public boolean relative(int rows) throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public boolean absolute(int row) throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public void afterLast() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public void beforeFirst() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }
    public boolean first() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); }

    public void cancelRowUpdates()
        throws SQLException { throw unused(); }
    public void deleteRow()
        throws SQLException { throw unused(); }

    public void updateArray(int col, Array x)
        throws SQLException { throw unused(); }
    public void updateArray(String col, Array x)
        throws SQLException { throw unused(); }
    public void updateAsciiStream(int col, InputStream x, int l)
        throws SQLException { throw unused(); }
    public void updateAsciiStream(String col, InputStream x, int l)
        throws SQLException { throw unused(); }
    public void updateBigDecimal(int col, BigDecimal x)
        throws SQLException { throw unused(); }
    public void updateBigDecimal(String col, BigDecimal x)
        throws SQLException { throw unused(); }
    public void updateBinaryStream(int c, InputStream x, int l)
        throws SQLException { throw unused(); }
    public void updateBinaryStream(String c, InputStream x, int l)
        throws SQLException { throw unused(); }
    public void updateBlob(int col, Blob x)
        throws SQLException { throw unused(); }
    public void updateBlob(String col, Blob x)
        throws SQLException { throw unused(); }
    public void updateBoolean(int col, boolean x)
        throws SQLException { throw unused(); }
    public void updateBoolean(String col, boolean x)
        throws SQLException { throw unused(); }
    public void updateByte(int col, byte x)
        throws SQLException { throw unused(); }
    public void updateByte(String col, byte x)
        throws SQLException { throw unused(); }
    public void updateBytes(int col, byte[] x)
        throws SQLException { throw unused(); }
    public void updateBytes(String col, byte[] x)
        throws SQLException { throw unused(); }
    public void updateCharacterStream(int c, Reader x, int l)
        throws SQLException { throw unused(); }
    public void updateCharacterStream(String c, Reader r, int l)
        throws SQLException { throw unused(); }
    public void updateClob(int col, Clob x)
        throws SQLException { throw unused(); }
    public void updateClob(String col, Clob x)
        throws SQLException { throw unused(); }
    public void updateDate(int col, Date x)
        throws SQLException { throw unused(); }
    public void updateDate(String col, Date x)
        throws SQLException { throw unused(); }
    public void updateDouble(int col, double x)
        throws SQLException { throw unused(); }
    public void updateDouble(String col, double x)
        throws SQLException { throw unused(); }
    public void updateFloat(int col, float x)
        throws SQLException { throw unused(); }
    public void updateFloat(String col, float x)
        throws SQLException { throw unused(); }
    public void updateInt(int col, int x)
        throws SQLException { throw unused(); }
    public void updateInt(String col, int x)
        throws SQLException { throw unused(); }
    public void updateLong(int col, long x)
        throws SQLException { throw unused(); }
    public void updateLong(String col, long x)
        throws SQLException { throw unused(); }
    public void updateNull(int col)
        throws SQLException { throw unused(); }
    public void updateNull(String col)
        throws SQLException { throw unused(); }
    public void updateObject(int c, Object x)
        throws SQLException { throw unused(); }
    public void updateObject(int c, Object x, int s)
        throws SQLException { throw unused(); }
    public void updateObject(String col, Object x)
        throws SQLException { throw unused(); }
    public void updateObject(String c, Object x, int s)
        throws SQLException { throw unused(); }
    public void updateRef(int col, Ref x)
        throws SQLException { throw unused(); }
    public void updateRef(String c, Ref x)
        throws SQLException { throw unused(); }
    public void updateRow()
        throws SQLException { throw unused(); }
    public void updateShort(int c, short x)
        throws SQLException { throw unused(); }
    public void updateShort(String c, short x)
        throws SQLException { throw unused(); }
    public void updateString(int c, String x)
        throws SQLException { throw unused(); }
    public void updateString(String c, String x)
        throws SQLException { throw unused(); }
    public void updateTime(int c, Time x)
        throws SQLException { throw unused(); }
    public void updateTime(String c, Time x)
        throws SQLException { throw unused(); }
    public void updateTimestamp(int c, Timestamp x)
        throws SQLException { throw unused(); }
    public void updateTimestamp(String c, Timestamp x)
        throws SQLException { throw unused(); }

    public void refreshRow()
        throws SQLException { throw unused(); }
}
