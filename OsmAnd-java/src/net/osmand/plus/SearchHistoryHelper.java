package net.osmand.plus;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;

public class SearchHistoryHelper {
	
	private static final int HISTORY_LIMIT = 50;
	private ClientContext context;
	private List<HistoryEntry> loadedEntries = null;
	
	public SearchHistoryHelper(ClientContext context) {
		this.context = context;
	}

	public static SearchHistoryHelper getInstance(ClientContext context){
		return new SearchHistoryHelper(context);
	}
	
	public static class HistoryEntry {
		double lat;
		double lon;
		String name;
		
		public HistoryEntry(double lat, double lon, String name){
			this.lat = lat;
			this.lon = lon;
			this.name = name;
			
		}
		
		public String getName() {
			return name;
		}
		
		public double getLat() {
			return lat;
		}
		public double getLon() {
			return lon;
		}
	}
	
	public List<HistoryEntry> getHistoryEntries() {
		if(loadedEntries == null){
			checkLoadedEntries();
		}
		return loadedEntries;
	}
	
	private HistoryItemDBHelper checkLoadedEntries(){
		HistoryItemDBHelper helper = new HistoryItemDBHelper();
		if(loadedEntries == null){
			loadedEntries = helper.getEntries();
		}
		return helper;
	}

	public void remove(HistoryEntry model) {
		HistoryItemDBHelper helper = checkLoadedEntries();
		if(helper.remove(model)){
			loadedEntries.remove(model);
		}
	}

	public void removeAll() {
		HistoryItemDBHelper helper = checkLoadedEntries();
		if(helper.removeAll()){
			loadedEntries.clear();
		}
	}

	public void selectEntry(HistoryEntry model) {
		HistoryItemDBHelper helper = checkLoadedEntries();
		int i = loadedEntries.indexOf(model);
		updateModelAt(model, helper, i);
	}

	private void updateModelAt(HistoryEntry model, HistoryItemDBHelper helper, int i) {
		if(i == -1){
			if(helper.add(model)){
				loadedEntries.add(0, model);
				if(loadedEntries.size() > HISTORY_LIMIT){
					if(helper.remove(loadedEntries.get(loadedEntries.size() - 1))){
						loadedEntries.remove(loadedEntries.size() - 1);
					}
				}
			}
		} else {
			if(helper.update(model)){
				loadedEntries.remove(i);
				loadedEntries.add(0, model);
			}
		}
	}
	
	public HistoryEntry addNewItemToHistory(double lat, double lon, String description){
		HistoryItemDBHelper helper = checkLoadedEntries();
		int i = 0;
		HistoryEntry model = new HistoryEntry(lat, lon, description);
		for(HistoryEntry e : loadedEntries){
			if(description.equals(e.getName())){
				break;
			}
			i++;
		}
		if(i == loadedEntries.size()){
			i = -1;
		}
		if (i != 0) {
			updateModelAt(model, helper, i);
		}
		return model;
	}
	
	private class HistoryItemDBHelper {

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

		
		public HistoryItemDBHelper() {
		}
		
		private SQLiteConnection openConnection(boolean readonly) {
			SQLiteConnection conn = context.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
			if (conn.getVersion() == 0 || DB_VERSION != conn.getVersion()) {
				if (readonly) {
					conn.close();
					conn = context.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
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

		public void onCreate(SQLiteConnection db) {
			db.execSQL(HISTORY_TABLE_CREATE);
		}

		public void onUpgrade(SQLiteConnection db, int oldVersion, int newVersion) {
		}
		
		public boolean remove(HistoryEntry e){
			SQLiteConnection db = openConnection(false);
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
			SQLiteConnection db = openConnection(false);
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
			SQLiteConnection db = openConnection(false);
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
			SQLiteConnection db = openConnection(false);
			if(db != null){
				try {
					db.execSQL(
							"INSERT INTO " + HISTORY_TABLE_NAME + " VALUES (?, ?, ?, ?, ?)", new Object[] { e.getName(), System.currentTimeMillis(), null, e.getLat(), e.getLon() }); //$NON-NLS-1$ //$NON-NLS-2$
				} finally {
					db.close();
				}
				return true;
			}
			return false;
		} 
		
		public List<HistoryEntry> getEntries(){
			List<HistoryEntry> entries = new ArrayList<HistoryEntry>();
			SQLiteConnection db = openConnection(true);
			if(db != null){
				try {
					SQLiteCursor query = db.rawQuery(
							"SELECT " + HISTORY_COL_NAME + ", " + HISTORY_COL_LAT + "," + HISTORY_COL_LON + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
									HISTORY_TABLE_NAME + " ORDER BY " + HISTORY_COL_TIME + " DESC", null); //$NON-NLS-1$//$NON-NLS-2$
					if (query.moveToFirst()) {
						do {
							HistoryEntry e = new HistoryEntry(query.getDouble(1), query.getDouble(2), query.getString(0));
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
	
	
	

}
