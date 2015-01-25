package net.osmand.plus.helpers;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis
 * on 23.01.15.
 */
public class DatabaseHelper {

    public static final int DOWNLOAD_ENTRY = 0;
    public static final int FAVORITES_ENTRY = 1;

    private static final String DB_NAME = "usage_history"; //$NON-NLS-1$
    private static final int DB_VERSION = 1;

    private static final String DOWNLOADS_TABLE_NAME = "downloads"; //$NON-NLS-1$
    private static final String FAVORITES_TABLE_NAME = "favorites"; //$NON-NLS-1$

    private static final String HISTORY_COL_NAME = "name"; //$NON-NLS-1$
    private static final String HISTORY_COL_COUNT = "count"; //$NON-NLS-1$
    private static final String DOWNLOAD_TABLE_CREATE =   "CREATE TABLE " + DOWNLOADS_TABLE_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
            HISTORY_COL_NAME + " TEXT, " + HISTORY_COL_COUNT + " long);"; //$NON-NLS-1$ //$NON-NLS-2$

    private static final String FAVORITES_TABLE_CREATE =   "CREATE TABLE " + FAVORITES_TABLE_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
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

    public DatabaseHelper(OsmandApplication context) {
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
        db.execSQL(DOWNLOAD_TABLE_CREATE);
        db.execSQL(FAVORITES_TABLE_CREATE);
    }

    public void onUpgrade(SQLiteAPI.SQLiteConnection db, int oldVersion, int newVersion) {
    }

    public boolean remove(HistoryEntry e, int type){
        SQLiteAPI.SQLiteConnection db = openConnection(false);
        if(db != null){
            try {
                switch (type){
                    case DOWNLOAD_ENTRY:
                        db.execSQL("DELETE FROM " + DOWNLOADS_TABLE_NAME + " WHERE " + HISTORY_COL_NAME + " = ?", new Object[] { e.getName() }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    case FAVORITES_ENTRY:
                        db.execSQL("DELETE FROM " + FAVORITES_TABLE_NAME + " WHERE " + HISTORY_COL_NAME + " = ?", new Object[] { e.getName() }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }
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
                db.execSQL("DELETE FROM " + DOWNLOADS_TABLE_NAME); //$NON-NLS-1$
            } finally {
                db.close();
            }
            return true;
        }
        return false;
    }

    public boolean update(HistoryEntry e, int type){
        SQLiteAPI.SQLiteConnection db = openConnection(false);
        if(db != null){
            try {
                switch (type) {
                    case DOWNLOAD_ENTRY:
                        db.execSQL("UPDATE " + DOWNLOADS_TABLE_NAME + " SET " + HISTORY_COL_COUNT + " = ? WHERE " + HISTORY_COL_NAME + " = ?", new Object[] { e.getCount(), e.getName() }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    case FAVORITES_ENTRY:
                        db.execSQL("UPDATE " + FAVORITES_TABLE_NAME + " SET " + HISTORY_COL_COUNT + " = ? WHERE " + HISTORY_COL_NAME + " = ?", new Object[] { e.getCount(), e.getName() }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }
            } finally {
                db.close();
            }
            return true;
        }
        return false;
    }

    public boolean add(HistoryEntry e, int type){
        SQLiteAPI.SQLiteConnection db = openConnection(false);
        if(db != null){
            try {
                switch (type) {
                    case DOWNLOAD_ENTRY:
                        db.execSQL("INSERT INTO " + DOWNLOADS_TABLE_NAME + " VALUES (?, ?)", new Object[] { e.getName(), e.getCount()}); //$NON-NLS-1$ //$NON-NLS-2$
                    case FAVORITES_ENTRY:
                        db.execSQL("INSERT INTO " + FAVORITES_TABLE_NAME + " VALUES (?, ?)", new Object[] { e.getName(), e.getCount()}); //$NON-NLS-1$ //$NON-NLS-2$
                }
            } finally {
                db.close();
            }
            return true;
        }
        return false;
    }

    public long getCount(String name, int type) {
        SQLiteAPI.SQLiteConnection db = openConnection(true);
        long count = 0;
        if(db != null){
            try {
                SQLiteAPI.SQLiteCursor query;
                switch (type) {
                    case DOWNLOAD_ENTRY:
                        query =  db.rawQuery(
                                "SELECT " + HISTORY_COL_COUNT + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                        DOWNLOADS_TABLE_NAME + " WHERE " + HISTORY_COL_NAME + "='" + name + "'", null); //$NON-NLS-1$//$NON-NLS-2$
                    case FAVORITES_ENTRY:
                        query =  db.rawQuery(
                                "SELECT " + HISTORY_COL_COUNT + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                        FAVORITES_TABLE_NAME + " WHERE " + HISTORY_COL_NAME + "='" + name + "'", null); //$NON-NLS-1$//$NON-NLS-2$
                    default:
                        query =  db.rawQuery("not supported", null); //$NON-NLS-1$//$NON-NLS-2$
                }

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

    public List<HistoryEntry> getEntries(int type){
        List<HistoryEntry> entries = new ArrayList<HistoryEntry>();
        SQLiteAPI.SQLiteConnection db = openConnection(true);
        if(db != null){
            try {
                SQLiteAPI.SQLiteCursor query;
                switch (type) {
                    case DOWNLOAD_ENTRY:
                        query = db.rawQuery(
                                "SELECT " + HISTORY_COL_NAME + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                        DOWNLOADS_TABLE_NAME + " ORDER BY " + HISTORY_COL_COUNT + " DESC", null); //$NON-NLS-1$//$NON-NLS-2$
                    case FAVORITES_ENTRY:
                        query = db.rawQuery(
                                "SELECT " + HISTORY_COL_NAME + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                        FAVORITES_TABLE_NAME + " ORDER BY " + HISTORY_COL_COUNT + " DESC", null); //$NON-NLS-1$//$NON-NLS-2$
                    default:
                        query =  db.rawQuery("not supported", null); //$NON-NLS-1$//$NON-NLS-2$
                }
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