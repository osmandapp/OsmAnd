package net.osmand.plus.helpers;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis
 * on 23.01.15.
 */
public class DownloadFrequencyHelper {

    private static final String DB_NAME = "download_history"; //$NON-NLS-1$
    private static final int DB_VERSION = 1;
    private static final String HISTORY_TABLE_NAME = "history"; //$NON-NLS-1$
    private static final String HISTORY_COL_NAME = "name"; //$NON-NLS-1$
    private static final String HISTORY_COL_COUNT = "count"; //$NON-NLS-1$
    private static final String HISTORY_TABLE_CREATE =   "CREATE TABLE " + HISTORY_TABLE_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
            HISTORY_COL_NAME + " TEXT, " + HISTORY_COL_COUNT + " long);"; //$NON-NLS-1$ //$NON-NLS-2$

    private OsmandApplication app;

    public static class HistoryEntry {
        long count;
        String name;

        public HistoryEntry(String name, long count){
            this.count = count;
            this.name = name;

        }

        public String getName() {
            return name;
        }

        public long getCount() {
            return count;
        }
    }

    public DownloadFrequencyHelper(OsmandApplication context) {
        app = context;
    }

    private SQLiteAPI.SQLiteConnection openConnection(boolean readonly) {
        SQLiteAPI.SQLiteConnection conn = app.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
        if (conn.getVersion() == 0 || DB_VERSION != conn.getVersion()) {
            if (readonly) {
                conn.close();
                conn = app.getSQLiteAPI().getOrCreateDatabase(DB_NAME, true);
            }
            if (conn.getVersion() == 0) {
                conn.setVersion(DB_VERSION);
                onCreate(conn);
            } else {
                onUpgrade(conn, conn.getVersion(), DB_VERSION);
            }

        }
        return conn;
    }

    public void onCreate(SQLiteAPI.SQLiteConnection db) {
        db.execSQL(HISTORY_TABLE_CREATE);
    }

    public void onUpgrade(SQLiteAPI.SQLiteConnection db, int oldVersion, int newVersion) {
    }

    public boolean remove(HistoryEntry e){
        SQLiteAPI.SQLiteConnection db = openConnection(false);
        if(db != null){
            try {
                db.execSQL("DELETE FROM " + HISTORY_TABLE_NAME + " WHERE " + HISTORY_COL_NAME + " = ?", new Object[] { e.getName() }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            } finally {
                db.close();
            }
            return true;
        }
        return false;
    }

    public boolean removeAll(){
        SQLiteAPI.SQLiteConnection db = openConnection(false);
        if(db != null){
            try {
                db.execSQL("DELETE FROM " + HISTORY_TABLE_NAME); //$NON-NLS-1$
            } finally {
                db.close();
            }
            return true;
        }
        return false;
    }

    public boolean update(HistoryEntry e){
        SQLiteAPI.SQLiteConnection db = openConnection(false);
        if(db != null){
            try {
                db.execSQL(
                        "UPDATE " + HISTORY_TABLE_NAME + " SET " + HISTORY_COL_COUNT + " = ? WHERE " + HISTORY_COL_NAME + " = ?", new Object[] { e.getCount(), e.getName() }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            } finally {
                db.close();
            }
            return true;
        }
        return false;
    }

    public boolean add(HistoryEntry e){
        SQLiteAPI.SQLiteConnection db = openConnection(false);
        if(db != null){
            try {
                db.execSQL(
                        "INSERT INTO " + HISTORY_TABLE_NAME + " VALUES (?, ?)", new Object[] { e.getName(), e.getCount()}); //$NON-NLS-1$ //$NON-NLS-2$
            } finally {
                db.close();
            }
            return true;
        }
        return false;
    }

    public long getCount(String name) {
        SQLiteAPI.SQLiteConnection db = openConnection(true);
        long count = 0;
        if(db != null){
            try {
                SQLiteAPI.SQLiteCursor query = db.rawQuery(
                        "SELECT " + HISTORY_COL_COUNT + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                HISTORY_TABLE_NAME + " WHERE " + HISTORY_COL_NAME + "='" + name + "'", null); //$NON-NLS-1$//$NON-NLS-2$
                if (query.moveToFirst()) {
                    do {
                        count = query.getInt(0);
                    } while (query.moveToNext());
                }
                query.close();
            } finally {
                db.close();
            }
        }
        return count;
    }

    public List<HistoryEntry> getEntries(){
        List<HistoryEntry> entries = new ArrayList<HistoryEntry>();
        SQLiteAPI.SQLiteConnection db = openConnection(true);
        if(db != null){
            try {
                SQLiteAPI.SQLiteCursor query = db.rawQuery(
                        "SELECT " + HISTORY_COL_NAME + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                HISTORY_TABLE_NAME + " ORDER BY " + HISTORY_COL_COUNT + " DESC", null); //$NON-NLS-1$//$NON-NLS-2$
                if (query.moveToFirst()) {
                    do {
                        HistoryEntry e = new HistoryEntry(query.getString(0), query.getInt(1));
                        entries.add(e);
                    } while (query.moveToNext());
                }
                query.close();
            } finally {
                db.close();
            }
        }
        return entries;
    }

}