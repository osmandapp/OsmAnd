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

import java.lang.ref.*;
import java.io.File;
import java.sql.*;
import java.util.*;

/*
 * This class is the interface to SQLite. It provides some helper functions
 * used by other parts of the driver. The goal of the helper functions here
 * are not only to provide functionality, but to handle contractual
 * differences between the JDBC specification and the SQLite C API.
 *
 * The process of moving SQLite weirdness into this class is incomplete.
 * You'll still find lots of code in Stmt and PrepStmt that are doing
 * implicit contract conversions. Sorry.
 *
 * The two subclasses, NativeDB and NestedDB, provide the actual access to
 * SQLite functions.
 */
abstract class DB implements Codes
{
    /** The JDBC Connection that 'owns' this database instance. */
    Conn conn = null;

    /** The "begin;"and  "commit;" statement handles. */
    long begin = 0;
    long commit = 0;

    /** Tracer for statements to avoid unfinalized statements on db close. */
    private Map stmts = new Hashtable();

    // WRAPPER FUNCTIONS ////////////////////////////////////////////

    abstract void interrupt() throws SQLException;
    abstract void busy_timeout(int ms) throws SQLException;
    abstract String errmsg() throws SQLException;
    abstract String libversion() throws SQLException;
    abstract int changes() throws SQLException;
    abstract int shared_cache(boolean enable) throws SQLException;

    final synchronized void exec(String sql) throws SQLException {
        long pointer = 0;
        try {
            pointer = prepare(sql);
            switch (step(pointer)) {
                case SQLITE_DONE:
                    ensureAutoCommit();
                    return;
                case SQLITE_ROW:
                    return;
                default:
                    throwex();
            }
        } finally {
            finalize(pointer);
        }
    }

    final synchronized void open(Conn conn, String file) throws SQLException {
        this.conn = conn;
        _open(file);
    }

    final synchronized void close() throws SQLException {
        // finalize any remaining statements before closing db
        synchronized (stmts) {
            Iterator i = stmts.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry)i.next();
                Stmt stmt = (Stmt)entry.getValue();
                finalize(((Long)entry.getKey()).longValue());
                if (stmt != null) {
                    stmt.pointer = 0;
                }
                i.remove();
            }
        }

        // remove memory used by user-defined functions
        free_functions();

        // clean up commit object
        if (begin != 0) {
            finalize(begin);
            begin = 0;
        }
        if (commit != 0) {
            finalize(commit);
            commit = 0;
        }

        _close();
    }

    final synchronized void prepare(Stmt stmt) throws SQLException {
        if (stmt.pointer != 0)
            finalize(stmt);
        stmt.pointer = prepare(stmt.sql);
        stmts.put(new Long(stmt.pointer), stmt);
    }

    final synchronized int finalize(Stmt stmt) throws SQLException {
        if (stmt.pointer == 0) return 0;
        int rc = SQLITE_ERROR;
        try {
            rc = finalize(stmt.pointer);
        } finally {
            stmts.remove(new Long(stmt.pointer));
            stmt.pointer = 0;
        }
        return rc;
    }

    protected abstract void _open(String filename) throws SQLException;
    protected abstract void _close() throws SQLException;
    protected abstract long prepare(String sql) throws SQLException;
    protected abstract int finalize(long stmt) throws SQLException;
    protected abstract int step(long stmt) throws SQLException;
    protected abstract int reset(long stmt) throws SQLException;

    abstract int clear_bindings(long stmt) throws SQLException; // TODO remove?
    abstract int bind_parameter_count(long stmt) throws SQLException;

    abstract int    column_count      (long stmt) throws SQLException;
    abstract int    column_type       (long stmt, int col) throws SQLException;
    abstract String column_decltype   (long stmt, int col) throws SQLException;
    abstract String column_table_name (long stmt, int col) throws SQLException;
    abstract String column_name       (long stmt, int col) throws SQLException;
    abstract String column_text       (long stmt, int col) throws SQLException;
    abstract byte[] column_blob       (long stmt, int col) throws SQLException;
    abstract double column_double     (long stmt, int col) throws SQLException;
    abstract long   column_long       (long stmt, int col) throws SQLException;
    abstract int    column_int        (long stmt, int col) throws SQLException;

    abstract int bind_null  (long stmt, int pos) throws SQLException;
    abstract int bind_int   (long stmt, int pos, int    v) throws SQLException;
    abstract int bind_long  (long stmt, int pos, long   v) throws SQLException;
    abstract int bind_double(long stmt, int pos, double v) throws SQLException;
    abstract int bind_text  (long stmt, int pos, String v) throws SQLException;
    abstract int bind_blob  (long stmt, int pos, byte[] v) throws SQLException;

    abstract void result_null  (long context) throws SQLException;
    abstract void result_text  (long context, String val) throws SQLException;
    abstract void result_blob  (long context, byte[] val) throws SQLException;
    abstract void result_double(long context, double val) throws SQLException;
    abstract void result_long  (long context, long   val) throws SQLException;
    abstract void result_int   (long context, int    val) throws SQLException;
    abstract void result_error (long context, String err) throws SQLException;

    abstract int    value_bytes (Function f, int arg) throws SQLException;
    abstract String value_text  (Function f, int arg) throws SQLException;
    abstract byte[] value_blob  (Function f, int arg) throws SQLException;
    abstract double value_double(Function f, int arg) throws SQLException;
    abstract long   value_long  (Function f, int arg) throws SQLException;
    abstract int    value_int   (Function f, int arg) throws SQLException;
    abstract int    value_type  (Function f, int arg) throws SQLException;

    abstract int create_function(String name, Function f) throws SQLException;
    abstract int destroy_function(String name) throws SQLException;
    abstract void free_functions() throws SQLException;

    /** Provides metadata for the columns of a statement. Returns:
     *   res[col][0] = true if column constrained NOT NULL
     *   res[col][1] = true if column is part of the primary key
     *   res[col][2] = true if column is auto-increment
     */
    abstract boolean[][] column_metadata(long stmt) throws SQLException;


    // COMPOUND FUNCTIONS ////////////////////////////////////////////

    final synchronized String[] column_names(long stmt) throws SQLException {
        String[] names = new String[column_count(stmt)];
        for (int i=0; i < names.length; i++)
            names[i] = column_name(stmt, i);
        return names;
    }

    final synchronized int sqlbind(long stmt, int pos, Object v)
            throws SQLException {
        pos++;
        if (v == null) {
            return bind_null(stmt, pos);
        } else if (v instanceof Integer) {
            return bind_int(stmt, pos, ((Integer)v).intValue());
        } else if (v instanceof Long) {
            return bind_long(stmt, pos, ((Long)v).longValue());
        } else if (v instanceof Double) {
            return bind_double(stmt, pos, ((Double)v).doubleValue());
        } else if (v instanceof String) {
            return bind_text(stmt, pos, (String)v);
        } else if (v instanceof byte[]) {
            return bind_blob(stmt, pos, (byte[])v);
        } else {
            throw new SQLException("unexpected param type: "+v.getClass());
        }
    }

    final synchronized int[] executeBatch(long stmt, int count, Object[] vals)
            throws SQLException {
        if (count < 1) throw new SQLException("count (" + count + ") < 1");

        final int params = bind_parameter_count(stmt);

        int rc;
        int[] changes = new int[count];

        try {
        for (int i=0; i < count; i++) {
            reset(stmt);
            for (int j=0; j < params; j++)
                if (sqlbind(stmt, j, vals[(i * params) + j]) != SQLITE_OK)
                    throwex();

            rc = step(stmt);
            if (rc != SQLITE_DONE) {
                reset(stmt);
                if (rc == SQLITE_ROW) throw new BatchUpdateException(
                    "batch entry "+i+": query returns results", changes);
                throwex();
            }

            changes[i] = changes();
        }
        } finally {
            ensureAutoCommit();
        }

        reset(stmt);
        return changes;
    }

    final synchronized boolean execute(Stmt stmt, Object[] vals)
            throws SQLException {
        if (vals != null) {
            final int params = bind_parameter_count(stmt.pointer);
            if (params != vals.length)
                throw new SQLException("assertion failure: param count ("
                        + params + ") != value count (" + vals.length + ")");

            for (int i=0; i < params; i++)
                if (sqlbind(stmt.pointer, i, vals[i]) != SQLITE_OK) throwex();
        }

        switch (step(stmt.pointer)) {
            case SQLITE_DONE:
                reset(stmt.pointer);
                ensureAutoCommit();
                return false;
            case SQLITE_ROW:
                return true;
            case SQLITE_BUSY:
            case SQLITE_LOCKED:
                throw new SQLException("database locked");
            case SQLITE_MISUSE:
                throw new SQLException(errmsg());
            default:
                finalize(stmt);
                throw new SQLException(errmsg());
        }
    }

    final synchronized int executeUpdate(Stmt stmt, Object[] vals)
            throws SQLException {
        if (execute(stmt, vals))
            throw new SQLException("query returns results");
        reset(stmt.pointer);
        return changes();
    }

    final void throwex() throws SQLException {
        throw new SQLException(errmsg());
    }

    /*
     * SQLite and the JDBC API have very different ideas about the meaning
     * of auto-commit. Under JDBC, when executeUpdate() returns in
     * auto-commit mode (the default), the programmer assumes the data has
     * been written to disk. In SQLite however, a call to sqlite3_step()
     * with an INSERT statement can return SQLITE_OK, and yet the data is
     * still in limbo.
     *
     * This limbo appears when another statement on the database is active,
     * e.g. a SELECT. SQLite auto-commit waits until the final read
     * statement finishes, and then writes whatever updates have already
     * been OKed. So if a program crashes before the reads are complete,
     * data is lost. E.g:
     *
     *     select begins
     *     insert
     *     select continues
     *     select finishes
     *
     * Works as expected, however
     *
     *     select beings
     *     insert
     *     select continues
     *     crash
     *
     * Results in the data never being written to disk.
     *
     * As a solution, we call "commit" after every statement in auto-commit
     * mode.
     */
    final void ensureAutoCommit() throws SQLException {
        /*
        if (!conn.getAutoCommit())
            return;

        if (begin == 0)
            begin = prepare("begin;");
        if (commit == 0)
            commit = prepare("commit;");

        try {
            if (step(begin) != SQLITE_DONE)
                return; // assume we are in a transaction
            if (step(commit) != SQLITE_DONE) {
                reset(commit);
                throw new SQLException("unable to auto-commit");
            }
        } finally {
            reset(begin);
            reset(commit);
        }
        */
    }
}
