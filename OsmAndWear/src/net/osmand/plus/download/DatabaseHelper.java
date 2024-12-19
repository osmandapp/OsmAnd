package net.osmand.plus.download;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class DatabaseHelper {

    public static final int DOWNLOAD_ENTRY = 0;
    private static final String DB_NAME = "usage_history"; //$NON-NLS-1$
    private static final int DB_VERSION = 1;
    private static final String DOWNLOADS_TABLE_NAME = "downloads"; //$NON-NLS-1$

    private static final String HISTORY_COL_NAME = "name"; //$NON-NLS-1$
    private static final String HISTORY_COL_COUNT = "count"; //$NON-NLS-1$
    private static final String DOWNLOAD_TABLE_CREATE =   "CREATE TABLE " + DOWNLOADS_TABLE_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
            HISTORY_COL_NAME + " TEXT, " + HISTORY_COL_COUNT + " long);"; //$NON-NLS-1$ //$NON-NLS-2$

    private final OsmandApplication app;

    public static class HistoryDownloadEntry {
    	int count;
        String name;

        public HistoryDownloadEntry(String name, int count){
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

    public DatabaseHelper(OsmandApplication context) {
        app = context;
    }

    private SQLiteAPI.SQLiteConnection openConnection(boolean readonly) {
        SQLiteAPI.SQLiteConnection conn = app.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
        if (conn == null) {
            return null;
        }
        if (conn.getVersion() < DB_VERSION) {
            if (readonly) {
                conn.close();
                conn = app.getSQLiteAPI().getOrCreateDatabase(DB_NAME, false);
            }
            int version = conn.getVersion();
            conn.setVersion(DB_VERSION);
            if (version == 0) {
                onCreate(conn);
            } else {
                onUpgrade(conn, version, DB_VERSION);
            }
        }
        return conn;
    }

    public void onCreate(SQLiteAPI.SQLiteConnection db) {
        db.execSQL(DOWNLOAD_TABLE_CREATE);
    }

    public void onUpgrade(SQLiteAPI.SQLiteConnection db, int oldVersion, int newVersion) {
    }

    public boolean remove(HistoryDownloadEntry e, int type){
        SQLiteAPI.SQLiteConnection db = openConnection(false);
        if(db != null){
            try {
                switch (type){
                    case DOWNLOAD_ENTRY:
                        db.execSQL("DELETE FROM " + DOWNLOADS_TABLE_NAME + " WHERE " + HISTORY_COL_NAME + " = ?", new Object[] { e.getName() }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

    public boolean update(HistoryDownloadEntry e, int type){
        SQLiteAPI.SQLiteConnection db = openConnection(false);
        if(db != null){
            try {
                switch (type) {
                    case DOWNLOAD_ENTRY:
                        db.execSQL("UPDATE " + DOWNLOADS_TABLE_NAME + " SET " + HISTORY_COL_COUNT + " = ? WHERE " + HISTORY_COL_NAME + " = ?", new Object[] { e.getCount(), e.getName() }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }
            } finally {
                db.close();
            }
            return true;
        }
        return false;
    }

    public boolean add(HistoryDownloadEntry e, int type){
        SQLiteAPI.SQLiteConnection db = openConnection(false);
        if(db != null){
            try {
                switch (type) {
                    case DOWNLOAD_ENTRY:
                        db.execSQL("INSERT INTO " + DOWNLOADS_TABLE_NAME + " VALUES (?, ?)", new Object[] { e.getName(), e.getCount()}); //$NON-NLS-1$ //$NON-NLS-2$
                }
            } finally {
                db.close();
            }
            return true;
        }
        return false;
    }

    public int getCount(String name, int type) {
        SQLiteAPI.SQLiteConnection db = openConnection(true);
        int count = 0;
        if(db != null){
            try {
                SQLiteAPI.SQLiteCursor query;
                switch (type) {
                    case DOWNLOAD_ENTRY:
                        query =  db.rawQuery(
                                "SELECT " + HISTORY_COL_COUNT + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                        DOWNLOADS_TABLE_NAME + " WHERE " + HISTORY_COL_NAME + "='" + name + "'", null); //$NON-NLS-1$//$NON-NLS-2$
                        break;
                    default:
                    	query = null;
                    	break;
                }

				if (query != null) {
					if (query.moveToFirst()) {
						count = query.getInt(0);
					}
					query.close();
				}
            } finally {
                db.close();
            }
        }
        return count;
    }

    public List<HistoryDownloadEntry> getEntries(int type){
        List<HistoryDownloadEntry> entries = new ArrayList<HistoryDownloadEntry>();
        SQLiteAPI.SQLiteConnection db = openConnection(true);
        if(db != null){
            try {
                SQLiteAPI.SQLiteCursor query;
                switch (type) {
                    case DOWNLOAD_ENTRY:
                        query = db.rawQuery(
                                "SELECT " + HISTORY_COL_NAME + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                        DOWNLOADS_TABLE_NAME + " ORDER BY " + HISTORY_COL_COUNT + " DESC", null); //$NON-NLS-1$//$NON-NLS-2$
                        break;
                    default:
                        query = null; //$NON-NLS-1$//$NON-NLS-2$
                        break;
                }
				if (query != null) {
					if (query.moveToFirst()) {
						do {
							HistoryDownloadEntry e = new HistoryDownloadEntry(
									query.getString(0), query.getInt(1));
							entries.add(e);
						} while (query.moveToNext());
					}
					query.close();
				}
            } finally {
                db.close();
            }
        }
        return entries;
    }

}