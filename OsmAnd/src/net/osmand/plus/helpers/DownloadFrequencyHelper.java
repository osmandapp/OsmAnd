package net.osmand.plus.helpers;

import android.content.Context;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis
 * on 23.01.15.
 */
public class DownloadFrequencyHelper {

    private static final String DB_NAME = "search_history"; //$NON-NLS-1$
    private static final int DB_VERSION = 1;
    private static final String HISTORY_TABLE_NAME = "history"; //$NON-NLS-1$
    private static final String HISTORY_COL_NAME = "name"; //$NON-NLS-1$
    private static final String HISTORY_COL_TIME = "time"; //$NON-NLS-1$
    private static final String HISTORY_COL_TYPE = "type"; //$NON-NLS-1$
    private static final String HISTORY_COL_LAT = "latitude"; //$NON-NLS-1$
    private static final String HISTORY_COL_LON = "longitude"; //$NON-NLS-1$
    private static final String HISTORY_TABLE_CREATE =   "CREATE TABLE " + HISTORY_TABLE_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
            HISTORY_COL_NAME + " TEXT, " + 	HISTORY_COL_TIME + " long, " + HISTORY_COL_TYPE + " TEXT, " + 	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            HISTORY_COL_LAT + " double, " +HISTORY_COL_LON + " double);"; //$NON-NLS-1$ //$NON-NLS-2$

    private OsmandApplication app;

    public static class HistoryEntry {
        int count;
        String name;

        public HistoryEntry(int count, String name){
            this.count = count;
            this.name = name;

        }

        public String getName() {
            return name;
        }

        public int getCount() {
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
                conn = app.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
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
                        "UPDATE " + HISTORY_TABLE_NAME + " SET time = ? WHERE " + HISTORY_COL_NAME + " = ?", new Object[] { System.currentTimeMillis(), e.getName() }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
                        "INSERT INTO " + HISTORY_TABLE_NAME + " VALUES (?, ?, ?, ?, ?)", new Object[] { e.getName(), System.currentTimeMillis(), null, e.getCount()}); //$NON-NLS-1$ //$NON-NLS-2$
            } finally {
                db.close();
            }
            return true;
        }
        return false;
    }

    public List<HistoryEntry> getEntries(){
        List<HistoryEntry> entries = new ArrayList<HistoryEntry>();
        SQLiteAPI.SQLiteConnection db = openConnection(true);
        if(db != null){
            try {
                SQLiteAPI.SQLiteCursor query = db.rawQuery(
                        "SELECT " + HISTORY_COL_NAME + ", " + HISTORY_COL_LAT + "," + HISTORY_COL_LON + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                HISTORY_TABLE_NAME + " ORDER BY " + HISTORY_COL_TIME + " DESC", null); //$NON-NLS-1$//$NON-NLS-2$
                if (query.moveToFirst()) {
                    do {
                        HistoryEntry e = new HistoryEntry((int)query.getInt(1), query.getString(0));
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