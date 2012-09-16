package net.osmand.plus.activities.search;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SearchHistoryHelper {
	
	private static SearchHistoryHelper helper = new SearchHistoryHelper();
	private static final int HISTORY_LIMIT = 50;
	
	public static SearchHistoryHelper getInstance(){
		return helper;
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
	
	private List<HistoryEntry> loadedEntries = null;
	 

	public List<HistoryEntry> getHistoryEntries(Context ctx) {
		if(loadedEntries == null){
			HistoryItemDBHelper helper = checkLoadedEntries(ctx);
			helper.close();
		}
		return loadedEntries;
	}
	
	private HistoryItemDBHelper checkLoadedEntries(Context ctx){
		HistoryItemDBHelper helper = new HistoryItemDBHelper(ctx);
		if(loadedEntries == null){
			loadedEntries = helper.getEntries();
		}
		return helper;
	}

	public void remove(HistoryEntry model, Context ctx) {
		HistoryItemDBHelper helper = checkLoadedEntries(ctx);
		if(helper.remove(model)){
			loadedEntries.remove(model);
		}
		helper.close();
	}

	public void removeAll(Context ctx) {
		HistoryItemDBHelper helper = checkLoadedEntries(ctx);
		if(helper.removeAll()){
			loadedEntries.clear();
		}
		helper.close();
	}

	public void selectEntry(HistoryEntry model, Context ctx) {
		HistoryItemDBHelper helper = checkLoadedEntries(ctx);
		int i = loadedEntries.indexOf(model);
		updateModelAt(model, helper, i);
		helper.close();
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
	
	public HistoryEntry addNewItemToHistory(double lat, double lon, String description, Context ctx){
		HistoryItemDBHelper helper = checkLoadedEntries(ctx);
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
		helper.close();
		return model;
	}
	
	private static class HistoryItemDBHelper extends SQLiteOpenHelper {

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

		
		public HistoryItemDBHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION); 
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(HISTORY_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
		
		public boolean remove(HistoryEntry e){
			SQLiteDatabase db = getWritableDatabase();
			if(db != null){
				db.execSQL("DELETE FROM "+ HISTORY_TABLE_NAME+ " WHERE " + HISTORY_COL_NAME+ " = ?", new Object[]{e.getName()}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return true;
			}
			return false;
		}
		
		public boolean removeAll(){
			SQLiteDatabase db = getWritableDatabase();
			if(db != null){
				db.execSQL("DELETE FROM "+ HISTORY_TABLE_NAME); //$NON-NLS-1$
				return true;
			}
			return false;
		}
		
		public boolean update(HistoryEntry e){
			SQLiteDatabase db = getWritableDatabase();
			if(db != null){
				db.execSQL("UPDATE "+ HISTORY_TABLE_NAME+ " SET time = ? WHERE " + HISTORY_COL_NAME+ " = ?", new Object[]{System.currentTimeMillis(), e.getName()}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return true;
			}
			return false;
		}
		
		public boolean add(HistoryEntry e){
			SQLiteDatabase db = getWritableDatabase();
			if(db != null){
				db.execSQL("INSERT INTO "+ HISTORY_TABLE_NAME+ " VALUES (?, ?, ?, ?, ?)", new Object[]{e.getName(), System.currentTimeMillis(), null, e.getLat(), e.getLon()}); //$NON-NLS-1$ //$NON-NLS-2$ 
				return true;
			}
			return false;
		} 
		
		public List<HistoryEntry> getEntries(){
			List<HistoryEntry> entries = new ArrayList<HistoryEntry>();
			SQLiteDatabase db = getReadableDatabase();
			if(db != null){
				Cursor query = db.rawQuery("SELECT " + HISTORY_COL_NAME +", " + HISTORY_COL_LAT +"," + HISTORY_COL_LON +" FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
						HISTORY_TABLE_NAME + " ORDER BY " + HISTORY_COL_TIME + " DESC", null);  //$NON-NLS-1$//$NON-NLS-2$
	    		if(query.moveToFirst()){
	    			do {
	    				HistoryEntry e = new HistoryEntry(query.getDouble(1), query.getDouble(2), query.getString(0));
	    				entries.add(e);
	    			} while(query.moveToNext());
	    		}
	    		query.close();
			}
			return entries;
		}
		
	}
	
	
	

}
