package net.osmand.plus.helpers;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchHistoryHelper {
	
	private static final int HISTORY_LIMIT = 1500;
	private OsmandApplication context;
	private List<HistoryEntry> loadedEntries = null;
	private Map<PointDescription, HistoryEntry> mp = new HashMap<PointDescription, SearchHistoryHelper.HistoryEntry>();
	
	public SearchHistoryHelper(OsmandApplication context) {
		this.context = context;
	}

	private static SearchHistoryHelper instance = null; 
	public static SearchHistoryHelper getInstance(OsmandApplication context){
		if(instance == null) {
			instance = new SearchHistoryHelper(context);
		}
		return instance;
	}
	
	
	private static final int[] DEF_INTERVALS_MIN = new int[] {
		5, 60, 60 * 24, 5 * 60 * 24, 10 * 60 * 24, 30 * 60 * 24 
	};
	
	private static Comparator<HistoryEntry> historyEntryComparator = new Comparator<HistoryEntry>() {

		@Override
		public int compare(HistoryEntry lhs, HistoryEntry rhs) {
			long time = System.currentTimeMillis();
			double l = lhs.getRank(time);
			double r = rhs.getRank(time);
			return -Double.compare(l, r);
		}
	};
	
	public static class HistoryEntry {
		double lat;
		double lon;
		PointDescription name;
		private long lastAccessedTime;
		private int[] intervals = new int[0];
		private double[] intervalValues = new double[0];
		
		public HistoryEntry(double lat, double lon, PointDescription name){
			this.lat = lat;
			this.lon = lon;
			this.name = name;
		}
		
		private double rankFunction(double cf, double timeDiff) {
			if(timeDiff <= 0) {
				return 0;
			}
			return cf / timeDiff;
		}

		public double getRank(long time) {
			double baseTimeDiff = ((time - lastAccessedTime) / 1000) + 1;
			double timeDiff = 0;
			double vl = 1;
			double rnk = rankFunction(vl, baseTimeDiff + timeDiff);
			for (int k = 0; k < intervals.length; k++) {
				double ntimeDiff = intervals[k] * 60 * 1000;
				double nvl = intervalValues[k];
				if(ntimeDiff < timeDiff || nvl <= vl){
					continue;
				}
				rnk += rankFunction(nvl - vl, baseTimeDiff + (ntimeDiff - timeDiff) / 2 + timeDiff);
				vl = nvl - vl;
				timeDiff = ntimeDiff;
			}
			return rnk;
		}

		public PointDescription getName() {
			return name;
		}
		
		public String getSerializedName() {
			return PointDescription.serializeToString(name);
		}
		
		public double getLat() {
			return lat;
		}
		public double getLon() {
			return lon;
		}
		
		public void markAsAccessed(long time) {
			int[] nintervals = new int[DEF_INTERVALS_MIN.length];
			double[] nintervalValues = new double[DEF_INTERVALS_MIN.length];
			for(int k = 0; k < nintervals.length; k++) {
				nintervals[k] = DEF_INTERVALS_MIN[k];
				nintervalValues[k] = getUsageLastTime(time, 0, 0, nintervals[k]) + 1;
			}
			intervals = nintervals;
			intervalValues = nintervalValues;
			this.lastAccessedTime = time;
		}
		
		public double getUsageLastTime(long time, int days, int hours, int minutes) {
			long mins = (minutes + (hours + 24 * days) * 60);
			long timeInPast = time - mins * 60 * 1000;
			if (this.lastAccessedTime <= timeInPast) {
				return 0;
			}
			double res = 0;
			for (int k = 0; k < intervals.length; k++) {
				long intPast = intervals[k] * 60 * 1000;
				if (intPast > 0) {
					double r;
					if (lastAccessedTime - timeInPast >= intPast) {
						r = intervalValues[k];
					} else {
						r = intervalValues[k] * ((double) lastAccessedTime - timeInPast) / ((double) intPast);
					}
					res = Math.max(res, r);
				}
			}
			return res;
		}

		public void setFrequency(String intervalsString, String values) {
			if(Algorithms.isEmpty(intervalsString) || Algorithms.isEmpty(values)) {
				markAsAccessed(this.lastAccessedTime);
				return;
			}
			String[] ints = intervalsString.split(",");
			String[] vsl = values.split(",");
			intervals = new int[ints.length];
			intervalValues = new double[ints.length];
			try {
				for(int i = 0; i < ints.length && i < vsl.length; i++) {
					intervals[i] = Integer.parseInt(ints[i]);
					intervalValues[i] = Double.parseDouble(vsl[i]);
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		
		public long getLastAccessTime() {
			return lastAccessedTime;
		}
		
		public String getIntervalsValues() {
			StringBuilder s = new StringBuilder();
			for(int i = 0; i < intervalValues.length; i++) {
				if(i > 0) {
					s.append(",");
				}
				s.append(intervalValues[i]);
			}
			return s.toString();
		}
		
		public String getIntervals() {
			StringBuilder s = new StringBuilder();
			for(int i = 0; i < intervals.length; i++) {
				if(i > 0) {
					s.append(",");
				}
				s.append(intervals[i]);
			}
			return s.toString();
		}

		public void setLastAccessTime(long time) {
			this.lastAccessedTime = time;
		}

	}
	
	public List<HistoryEntry> getHistoryEntries() {
		if(loadedEntries == null){
			checkLoadedEntries();
		}
		return new ArrayList<SearchHistoryHelper.HistoryEntry>(loadedEntries);
	}
	
	private HistoryItemDBHelper checkLoadedEntries() {
		HistoryItemDBHelper helper = new HistoryItemDBHelper();
		if (loadedEntries == null) {
			loadedEntries = helper.getEntries();
			Collections.sort(loadedEntries, historyEntryComparator);
			for(HistoryEntry he : loadedEntries) {
				mp.put(he.getName(), he);
			}
		}
		return helper;
	}

	public void remove(HistoryEntry model) {
		HistoryItemDBHelper helper = checkLoadedEntries();
		if (helper.remove(model)) {
			loadedEntries.remove(model);
			mp.remove(model.getName());
		}
	}

	public void removeAll() {
		HistoryItemDBHelper helper = checkLoadedEntries();
		if (helper.removeAll()) {
			loadedEntries.clear();
			mp.clear();
		}
	}

	public void addNewItemToHistory(HistoryEntry model) {
		HistoryItemDBHelper helper = checkLoadedEntries();
		if(mp.containsKey(model.getName())) {
			model = mp.get(model.getName());
			model.markAsAccessed(System.currentTimeMillis());
			helper.update(model);
		} else {
			loadedEntries.add(model);
			mp.put(model.getName(), model);
			model.markAsAccessed(System.currentTimeMillis());
			helper.add(model);
		}
		Collections.sort(loadedEntries, historyEntryComparator);
		if(loadedEntries.size() > HISTORY_LIMIT){
			if(helper.remove(loadedEntries.get(loadedEntries.size() - 1))){
				loadedEntries.remove(loadedEntries.size() - 1);
			}
		}
	}


	private class HistoryItemDBHelper {

		private static final String DB_NAME = "search_history"; //$NON-NLS-1$
		private static final int DB_VERSION = 2;
		private static final String HISTORY_TABLE_NAME = "history_recents"; //$NON-NLS-1$
	    private static final String HISTORY_COL_NAME = "name"; //$NON-NLS-1$
	    private static final String HISTORY_COL_TIME = "time"; //$NON-NLS-1$
	    private static final String HISTORY_COL_FREQ_INTERVALS = "freq_intervals"; //$NON-NLS-1$
	    private static final String HISTORY_COL_FREQ_VALUES = "freq_values"; //$NON-NLS-1$
	    private static final String HISTORY_COL_LAT = "latitude"; //$NON-NLS-1$
	    private static final String HISTORY_COL_LON = "longitude"; //$NON-NLS-1$
	    private static final String HISTORY_TABLE_CREATE =   "CREATE TABLE IF NOT EXISTS " + HISTORY_TABLE_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
	    			HISTORY_COL_NAME + " TEXT, " +
	    			HISTORY_COL_TIME + " long, " +
	    			HISTORY_COL_FREQ_INTERVALS + " TEXT, " +
	    			HISTORY_COL_FREQ_VALUES + " TEXT, " +
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
					onCreate(conn);
				} else {
					onUpgrade(conn, conn.getVersion(), DB_VERSION);
				}
				conn.setVersion(DB_VERSION);

			}
			return conn;
		}

		public void onCreate(SQLiteConnection db) {
			db.execSQL(HISTORY_TABLE_CREATE);
		}

		public void onUpgrade(SQLiteConnection db, int oldVersion, int newVersion) {
			if(newVersion == 2) {
				db.execSQL(HISTORY_TABLE_CREATE);
				for(HistoryEntry he : getLegacyEntries(db)) {
					insert(he, db);
				}
			}
		}
		
		public boolean remove(HistoryEntry e){
			SQLiteConnection db = openConnection(false);
			if(db != null){
				try {
					removeQuery(e.getSerializedName(), db);
				} finally {
					db.close();
				}
				return true;
			}
			return false;
		}

		private void removeQuery(String name, SQLiteConnection db) {
			db.execSQL("DELETE FROM " + HISTORY_TABLE_NAME + " WHERE " + HISTORY_COL_NAME + " = ?",
					new Object[] { name }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
							"UPDATE " + HISTORY_TABLE_NAME + " SET " + HISTORY_COL_TIME + "= ? "+
									", " + HISTORY_COL_FREQ_INTERVALS + " = ? " +
									", " +HISTORY_COL_FREQ_VALUES + "= ? WHERE " +
									HISTORY_COL_NAME + " = ?", 
							new Object[] { e.getLastAccessTime(), e.getIntervals(), e.getIntervalsValues(),
									e.getSerializedName() }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
					insert(e, db);
				} finally {
					db.close();
				}
				return true;
			}
			return false;
		}

		private void insert(HistoryEntry e, SQLiteConnection db) {
			db.execSQL(
					"INSERT INTO " + HISTORY_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?)", 
					new Object[] { e.getSerializedName(), e.getLastAccessTime(), 
							e.getIntervals(), e.getIntervalsValues(), e.getLat(), e.getLon() }); //$NON-NLS-1$ //$NON-NLS-2$
		} 
		
		public List<HistoryEntry> getLegacyEntries(SQLiteConnection db){
			List<HistoryEntry> entries = new ArrayList<HistoryEntry>();
			if (db != null) {
				// LEGACY QUERY !!
				SQLiteCursor query = db.rawQuery(
						"SELECT name, latitude, longitude, time FROM history ORDER BY time DESC", null); //$NON-NLS-1$//$NON-NLS-2$
				if (query.moveToFirst()) {
					do {
						String name = query.getString(0);
						String type = PointDescription.POINT_TYPE_MARKER;
						// make it proper name with type
						if (name.contains(context.getString(R.string.favorite))) {
							type = PointDescription.POINT_TYPE_FAVORITE;
						} else if (name.contains(context.getString(R.string.search_address_building))) {
							type = PointDescription.POINT_TYPE_ADDRESS;
						} else if (name.contains(context.getString(R.string.search_address_city))) {
							type = PointDescription.POINT_TYPE_ADDRESS;
						} else if (name.contains(context.getString(R.string.search_address_street))) {
							type = PointDescription.POINT_TYPE_ADDRESS;
						} else if (name.contains(context.getString(R.string.search_address_street_option))) {
							type = PointDescription.POINT_TYPE_ADDRESS;
						} else if (name.contains(context.getString(R.string.poi))) {
							type = PointDescription.POINT_TYPE_POI;
						}
						if (name.contains(":")) {
							name = name.substring(name.indexOf(':') + 1);
						}
						HistoryEntry e = new HistoryEntry(query.getDouble(1), query.getDouble(2), new PointDescription(
								type, name));
						e.markAsAccessed(query.getLong(3));
						entries.add(e);
					} while (query.moveToNext());
				}
				query.close();
			}
			return entries;
		}
		
		public List<HistoryEntry> getEntries(){
			List<HistoryEntry> entries = new ArrayList<HistoryEntry>();
			SQLiteConnection db = openConnection(true);
			if(db != null){
				try {
					SQLiteCursor query = db.rawQuery(
							"SELECT " + HISTORY_COL_NAME + ", " + HISTORY_COL_LAT + "," + HISTORY_COL_LON +", " + 
									HISTORY_COL_TIME + ", " + HISTORY_COL_FREQ_INTERVALS + ", " + HISTORY_COL_FREQ_VALUES +
									" FROM " +	HISTORY_TABLE_NAME , null); //$NON-NLS-1$//$NON-NLS-2$
					Map<PointDescription, HistoryEntry> st = new HashMap<PointDescription, HistoryEntry>(); 
					if (query.moveToFirst()) {
						boolean reinsert = false;
						do {
							String name = query.getString(0);
							PointDescription p = PointDescription.deserializeFromString(name, new LatLon(query.getDouble(1), query.getDouble(2)));
							HistoryEntry e = new HistoryEntry(query.getDouble(1), query.getDouble(2), 
									p);
							long time = query.getLong(3);
							e.setLastAccessTime(time);
							e.setFrequency(query.getString(4), query.getString(5));
							if(st.containsKey(p)) {
								reinsert = true;
							}
							entries.add(e);
							st.put(p, e);
						} while (query.moveToNext());
						if(reinsert) {
							System.err.println("Reinsert all values for search history");
							db.execSQL("DELETE FROM " + HISTORY_TABLE_NAME); //$NON-NLS-1$
							entries.clear();
							entries.addAll(st.values());
							for(HistoryEntry he : entries) {
								insert(he, db);
							}
							
						}
					}
					query.close();
				} finally {
					db.close();
				}
			}
			return entries;
		}
		
	}

	public void addNewItemToHistory(double latitude, double longitude, PointDescription pointDescription) {
		addNewItemToHistory(new HistoryEntry(latitude, longitude, pointDescription));
		
	}
	
	
	

}
