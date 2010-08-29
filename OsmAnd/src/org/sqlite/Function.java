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

import java.sql.*;

/** Provides an interface for creating SQLite user-defined functions.
 *
 * <p>A subclass of <tt>org.sqlite.Function</tt> can be registered with
 * <tt>Function.create()</tt> and called by the name it was given. All
 * functions must implement <tt>xFunc()</tt>, which is called when SQLite
 * runs the custom function.</p>
 *
 * Eg.
 *
 * <pre>
 *      Class.forName("org.sqlite.JDBC");
 *      Connection conn = DriverManager.getConnection("jdbc:sqlite:");
 *
 *      Function.create(conn, "myFunc", new Function() {
 *          protected void xFunc() {
 *              System.out.println("myFunc called!");
 *          }
 *      });
 *
 *      conn.createStatement().execute("select myFunc();");
 *  </pre>
 *
 *  <p>Arguments passed to a custom function can be accessed using the
 *  <tt>protected</tt> functions provided. <tt>args()</tt> returns
 *  the number of arguments passed, while
 *  <tt>value_&lt;type&gt;(int)</tt> returns the value of the specific
 *  argument. Similarly a function can return a value using the
 *  <tt>result(&lt;type&gt;)</tt> function.</p>
 *
 *  <p>Aggregate functions are not yet supported, but coming soon.</p>
 *
 */
public abstract class Function
{
    private Conn conn;
    private DB db;

    long context = 0;     // pointer sqlite3_context*
    long value = 0;       // pointer sqlite3_value**
    int args = 0;

    /** Registers the given function with the Connection using the
     *  provided name. */
    public static final void create(Connection conn, String name, Function f)
            throws SQLException {
        if (conn == null || !(conn instanceof Conn))
            throw new SQLException("connection must be to an SQLite db");
        if (conn.isClosed())
            throw new SQLException("connection closed");

        f.conn = (Conn)conn;
        f.db = f.conn.db();

        if (name == null || name.length() > 255)
            throw new SQLException("invalid function name: '"+name+"'");

        if (f.db.create_function(name, f) != Codes.SQLITE_OK)
            throw new SQLException("error creating function");
    }

    /** Removes the named function form the Connection. */
    public static final void destroy(Connection conn, String name)
            throws SQLException {
        if (conn == null || !(conn instanceof Conn))
            throw new SQLException("connection must be to an SQLite db");
        ((Conn)conn).db().destroy_function(name);
    }


    /** Called by SQLite as a custom function. Should access arguments
     *  through <tt>value_*(int)</tt>, return results with
     *  <tt>result(*)</tt> and throw errors with <tt>error(String)</tt>. */
    protected abstract void xFunc() throws SQLException;


    /** Returns the number of arguments passed to the function.
     *  Can only be called from <tt>xFunc()</tt>. */
    protected synchronized final int args()
        throws SQLException { checkContext(); return args; }

    /** Called by <tt>xFunc</tt> to return a value. */
    protected synchronized final void result(byte[] value)
        throws SQLException { checkContext(); db.result_blob(context, value); }

    /** Called by <tt>xFunc</tt> to return a value. */
    protected synchronized final void result(double value)
        throws SQLException { checkContext(); db.result_double(context,value);}

    /** Called by <tt>xFunc</tt> to return a value. */
    protected synchronized final void result(int value)
        throws SQLException { checkContext(); db.result_int(context, value); }

    /** Called by <tt>xFunc</tt> to return a value. */
    protected synchronized final void result(long value)
        throws SQLException { checkContext(); db.result_long(context, value); }

    /** Called by <tt>xFunc</tt> to return a value. */
    protected synchronized final void result()
        throws SQLException { checkContext(); db.result_null(context); }

    /** Called by <tt>xFunc</tt> to return a value. */
    protected synchronized final void result(String value)
        throws SQLException { checkContext(); db.result_text(context, value); }

    /** Called by <tt>xFunc</tt> to throw an error. */
    protected synchronized final void error(String err)
        throws SQLException { checkContext(); db.result_error(context, err); }

    /** Called by <tt>xFunc</tt> to access the value of an argument. */
    protected synchronized final int value_bytes(int arg)
        throws SQLException {checkValue(arg); return db.value_bytes(this,arg);}

    /** Called by <tt>xFunc</tt> to access the value of an argument. */
    protected synchronized final String value_text(int arg)
        throws SQLException {checkValue(arg); return db.value_text(this,arg);}

    /** Called by <tt>xFunc</tt> to access the value of an argument. */
    protected synchronized final byte[] value_blob(int arg)
        throws SQLException {checkValue(arg); return db.value_blob(this,arg); }

    /** Called by <tt>xFunc</tt> to access the value of an argument. */
    protected synchronized final double value_double(int arg)
        throws SQLException {checkValue(arg); return db.value_double(this,arg);}

    /** Called by <tt>xFunc</tt> to access the value of an argument. */
    protected synchronized final int value_int(int arg)
        throws SQLException {checkValue(arg); return db.value_int(this, arg); }

    /** Called by <tt>xFunc</tt> to access the value of an argument. */
    protected synchronized final long value_long(int arg)
        throws SQLException { checkValue(arg); return db.value_long(this,arg); }

    /** Called by <tt>xFunc</tt> to access the value of an argument. */
    protected synchronized final int value_type(int arg)
        throws SQLException {checkValue(arg); return db.value_type(this,arg); }


    private void checkContext() throws SQLException {
        if (conn == null || conn.db() == null || context == 0)
            throw new SQLException("no context, not allowed to read value");
    }

    private void checkValue(int arg) throws SQLException {
        if (conn == null || conn.db() == null || value == 0)
            throw new SQLException("not in value access state");
        if (arg >= args)
            throw new SQLException("arg "+arg+" out bounds [0,"+args+")");
    }


    public static abstract class Aggregate
            extends Function
            implements Cloneable
    {
        protected final void xFunc() {}
        protected abstract void xStep() throws SQLException;
        protected abstract void xFinal() throws SQLException;

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }
}
