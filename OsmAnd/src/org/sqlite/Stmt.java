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
import java.util.ArrayList;

class Stmt extends Unused implements Statement, Codes
{
    final Conn conn;
    final DB db;
    final RS rs;

    long pointer;
    String sql = null;

    int batchPos;
    Object[] batch = null;
    boolean resultsWaiting = false;

    Stmt(Conn c) {
        conn = c;
        db = conn.db();
        rs = new RS(this);
    }

    protected final void checkOpen() throws SQLException {
        if (pointer == 0) throw new SQLException("statement is not executing");
    }
    boolean isOpen() throws SQLException {
        return (pointer != 0);
    }

    /** Calls sqlite3_step() and sets up results. Expects a clean stmt. */
    protected boolean exec() throws SQLException {
        if (sql == null) throw new SQLException(
            "SQLiteJDBC internal error: sql==null");
        if (rs.isOpen()) throw new SQLException(
            "SQLite JDBC internal error: rs.isOpen() on exec.");

        boolean rc = false;
        try {
            rc = db.execute(this, null);
        } finally {
            resultsWaiting = rc;
        }

        return db.column_count(pointer) != 0;
    }


    // PUBLIC INTERFACE /////////////////////////////////////////////

    public void close() throws SQLException {
        if (pointer == 0) return;
        rs.close();
        batch = null;
        batchPos = 0;
        int resp = db.finalize(this);
        if (resp != SQLITE_OK && resp != SQLITE_MISUSE)
            db.throwex();
    }

    protected void finalize() throws SQLException { close(); }

    public boolean execute(String sql) throws SQLException {
        close();
        this.sql = sql;
        db.prepare(this);
        return exec();
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        close();
        this.sql = sql;
        db.prepare(this);
        if (!exec()) {
            close();
            throw new SQLException("query does not return ResultSet");
        }
        return getResultSet();
    }

    public int executeUpdate(String sql) throws SQLException {
        close();
        this.sql = sql;
        int changes = 0;
        try {
            db.prepare(this);
            changes = db.executeUpdate(this, null);
        } finally { close(); }
        return changes;
    }

    public ResultSet getResultSet() throws SQLException {
        checkOpen();
        if (rs.isOpen()) throw new SQLException("ResultSet already requested");
        if (db.column_count(pointer) == 0) throw new SQLException(
            "no ResultSet available");
        if (rs.colsMeta == null)
            rs.colsMeta = db.column_names(pointer);
        rs.cols = rs.colsMeta;

        rs.open = resultsWaiting;
        resultsWaiting = false;
        return rs;
    }

    /*
     * This function has a complex behaviour best understood by carefully
     * reading the JavaDoc for getMoreResults() and considering the test
     * StatementTest.execute().
     */
    public int getUpdateCount() throws SQLException {
        if (pointer != 0
                && !rs.isOpen()
                && !resultsWaiting
                && db.column_count(pointer) == 0)
            return db.changes();
        return -1;
    }

    public void addBatch(String sql) throws SQLException {
        close();
        if (batch == null || batchPos + 1 >= batch.length) {
            Object[] nb = new Object[Math.max(10, batchPos * 2)];
            if (batch != null)
                System.arraycopy(batch, 0, nb, 0, batch.length);
            batch = nb;
        }
        batch[batchPos++] = sql;
    }

    public void clearBatch() throws SQLException {
        batchPos = 0;
        if (batch != null)
            for (int i=0; i < batch.length; i++)
                batch[i] = null;
    }

    public int[] executeBatch() throws SQLException {
        // TODO: optimise
        close();
        if (batch == null || batchPos == 0) return new int[] {};

        int[] changes = new int[batchPos];

        synchronized (db) { try {
            for (int i=0; i < changes.length; i++) {
                try {
                    this.sql = (String)batch[i];
                    db.prepare(this);
                    changes[i] = db.executeUpdate(this, null);
                } catch (SQLException e) {
                    throw new BatchUpdateException(
                        "batch entry " + i + ": " + e.getMessage(), changes);
                } finally {
                    db.finalize(this);
                }
            }
        } finally {
            clearBatch();
        } }

        return changes;
    }

    public void setCursorName(String name) {}

    public SQLWarning getWarnings() throws SQLException { return null; }
    public void clearWarnings() throws SQLException {}

    public Connection getConnection() throws SQLException {
        return conn; }

    public void cancel() throws SQLException { rs.checkOpen(); db.interrupt(); }
    public int getQueryTimeout() throws SQLException {
        return conn.getTimeout(); }
    public void setQueryTimeout(int seconds) throws SQLException {
        if (seconds < 0) throw new SQLException("query timeout must be >= 0");
        conn.setTimeout(1000 * seconds);
    }

    public int getMaxRows() throws SQLException {
        return rs.maxRows;
    }
    public void setMaxRows(int max) throws SQLException {
        if (max < 0) throw new SQLException("max row count must be >= 0");
        rs.maxRows = max;
    }

    public int getMaxFieldSize() throws SQLException { return 0; }
    public void setMaxFieldSize(int max) throws SQLException {
        if (max < 0) throw new SQLException(
            "max field size "+max+" cannot be negative");
    }

    public int getFetchSize() throws SQLException { return rs.getFetchSize(); }
    public void setFetchSize(int r) throws SQLException { rs.setFetchSize(r); }
    public int getFetchDirection() throws SQLException {
        return rs.getFetchDirection();
    }
    public void setFetchDirection(int d) throws SQLException {
        rs.setFetchDirection(d);
    }

    /** As SQLite's last_insert_rowid() function is DB-specific not
     *  statement specific, this function introduces a race condition
     *  if the same connection is used by two threads and both insert. */
    public ResultSet getGeneratedKeys() throws SQLException {
        return ((MetaData)conn.getMetaData()).getGeneratedKeys();
    }

    /** SQLite does not support multiple results from execute(). */
    public boolean getMoreResults() throws SQLException {
        return getMoreResults(0);
    }
    public boolean getMoreResults(int c) throws SQLException {
        checkOpen();
        close(); // as we never have another result, clean up pointer
        return false;
    }

    public int getResultSetConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY; }
    public int getResultSetHoldability() throws SQLException {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
    public int getResultSetType() throws SQLException {
        return ResultSet.TYPE_FORWARD_ONLY; }

    public void setEscapeProcessing(boolean enable) {}
}
