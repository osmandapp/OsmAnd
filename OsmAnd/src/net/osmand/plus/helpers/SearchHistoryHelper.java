package net.osmand.plus.helpers;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
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
	private static final int[] DEF_INTERVALS_MIN = new int[]{
			5, 60, 60 * 24, 5 * 60 * 24, 10 * 60 * 24, 30 * 60 * 24
	};

	private static SearchHistoryHelper instance = null;

	private OsmandApplication context;
	private List<PointHistoryEntry> loadedPoints = null;
	private Map<PointDescription, PointHistoryEntry> mp = new HashMap<>();

	private SearchHistoryHelper(OsmandApplication context) {
		this.context = context;
	}

	public static SearchHistoryHelper getInstance(OsmandApplication context) {
		if (instance == null) {
			instance = new SearchHistoryHelper(context);
		}
		return instance;
	}

	public void addPointToHistory(double latitude, double longitude, PointDescription pointDescription) {
		addPointToHistory(new PointHistoryEntry(latitude, longitude, pointDescription));
	}

	public List<PointHistoryEntry> getHistoryPoints() {
		if (loadedPoints == null) {
			checkLoadedEntries();
		}
		return new ArrayList<>(loadedPoints);
	}

	public void remove(PointHistoryEntry point) {
		HistoryItemDBHelper helper = checkLoadedEntries();
		if (helper.remove(point)) {
			loadedPoints.remove(point);
			mp.remove(point.getName());
		}
	}

	public void removeAll() {
		HistoryItemDBHelper helper = checkLoadedEntries();
		if (helper.removeAll()) {
			loadedPoints.clear();
			mp.clear();
		}
	}

	private HistoryItemDBHelper checkLoadedEntries() {
		HistoryItemDBHelper helper = new HistoryItemDBHelper();
		if (loadedPoints == null) {
			loadedPoints = helper.getPoints();
			Collections.sort(loadedPoints, new HistoryEntryComparator());
			for (PointHistoryEntry he : loadedPoints) {
				mp.put(he.getName(), he);
			}
		}
		return helper;
	}

	private void addPointToHistory(PointHistoryEntry point) {
		HistoryItemDBHelper helper = checkLoadedEntries();
		if (mp.containsKey(point.getName())) {
			point = mp.get(point.getName());
			point.markAsAccessed(System.currentTimeMillis());
			helper.update(point);
		} else {
			loadedPoints.add(point);
			mp.put(point.getName(), point);
			point.markAsAccessed(System.currentTimeMillis());
			helper.add(point);
		}
		Collections.sort(loadedPoints, new HistoryEntryComparator());
		if (loadedPoints.size() > HISTORY_LIMIT) {
			if (helper.remove(loadedPoints.get(loadedPoints.size() - 1))) {
				loadedPoints.remove(loadedPoints.size() - 1);
			}
		}
	}

	public static class PointHistoryEntry extends HistoryEntry {

		private double lat;
		private double lon;
		private PointDescription name;

		PointHistoryEntry(double lat, double lon, PointDescription name) {
			this.lat = lat;
			this.lon = lon;
			this.name = name;
		}

		public double getLat() {
			return lat;
		}

		public double getLon() {
			return lon;
		}

		public PointDescription getName() {
			return name;
		}

		public String getSerializedName() {
			return PointDescription.serializeToString(name);
		}
	}

	private static class HistoryEntry {

		protected long lastAccessedTime;
		private int[] intervals = new int[0];
		private double[] intervalValues = new double[0];

		private double rankFunction(double cf, double timeDiff) {
			if (timeDiff <= 0) {
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
				if (ntimeDiff < timeDiff || nvl <= vl) {
					continue;
				}
				rnk += rankFunction(nvl - vl, baseTimeDiff + (ntimeDiff - timeDiff) / 2 + timeDiff);
				vl = nvl - vl;
				timeDiff = ntimeDiff;
			}
			return rnk;
		}

		public void markAsAccessed(long time) {
			int[] nintervals = new int[DEF_INTERVALS_MIN.length];
			double[] nintervalValues = new double[DEF_INTERVALS_MIN.length];
			for (int k = 0; k < nintervals.length; k++) {
				nintervals[k] = DEF_INTERVALS_MIN[k];
				nintervalValues[k] = getUsageLastTime(time, 0, 0, nintervals[k]) + 1;
			}
			intervals = nintervals;
			intervalValues = nintervalValues;
			this.lastAccessedTime = time;
		}

		private double getUsageLastTime(long time, int days, int hours, int minutes) {
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
			if (Algorithms.isEmpty(intervalsString) || Algorithms.isEmpty(values)) {
				markAsAccessed(this.lastAccessedTime);
				return;
			}
			String[] ints = intervalsString.split(",");
			String[] vsl = values.split(",");
			intervals = new int[ints.length];
			intervalValues = new double[ints.length];
			try {
				for (int i = 0; i < ints.length && i < vsl.length; i++) {
					intervals[i] = Integer.parseInt(ints[i]);
					intervalValues[i] = Double.parseDouble(vsl[i]);
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}

		public String getIntervalsValues() {
			StringBuilder s = new StringBuilder();
			for (int i = 0; i < intervalValues.length; i++) {
				if (i > 0) {
					s.append(",");
				}
				s.append(intervalValues[i]);
			}
			return s.toString();
		}

		public String getIntervals() {
			StringBuilder s = new StringBuilder();
			for (int i = 0; i < intervals.length; i++) {
				if (i > 0) {
					s.append(",");
				}
				s.append(intervals[i]);
			}
			return s.toString();
		}
	}

	private static class HistoryEntryComparator implements Comparator<HistoryEntry> {
		long time = System.currentTimeMillis();

		@Override
		public int compare(HistoryEntry lhs, HistoryEntry rhs) {
			double l = lhs.getRank(time);
			double r = rhs.getRank(time);
			return -Double.compare(l, r);
		}
	}

	private class HistoryItemDBHelper {

		private static final String DB_NAME = "search_history";
		private static final int DB_VERSION = 2;

		private static final String HISTORY_TABLE_NAME = "history_recents";
		private static final String HISTORY_COL_NAME = "name";
		private static final String HISTORY_COL_TIME = "time";
		private static final String HISTORY_COL_FREQ_INTERVALS = "freq_intervals";
		private static final String HISTORY_COL_FREQ_VALUES = "freq_values";
		private static final String HISTORY_COL_LAT = "latitude";
		private static final String HISTORY_COL_LON = "longitude";

		private static final String HISTORY_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " +
				HISTORY_TABLE_NAME + " (" +
				HISTORY_COL_NAME + " TEXT, " +
				HISTORY_COL_TIME + " long, " +
				HISTORY_COL_FREQ_INTERVALS + " TEXT, " +
				HISTORY_COL_FREQ_VALUES + " TEXT, " +
				HISTORY_COL_LAT + " double, " +
				HISTORY_COL_LON + " double);";

		private static final String HISTORY_TABLE_SELECT = "SELECT " +
				HISTORY_COL_NAME + ", " +
				HISTORY_COL_TIME + ", " +
				HISTORY_COL_FREQ_INTERVALS + ", " +
				HISTORY_COL_FREQ_VALUES + ", " +
				HISTORY_COL_LAT + ", " +
				HISTORY_COL_LON +
				" FROM " + HISTORY_TABLE_NAME;

		private SQLiteConnection openConnection(boolean readonly) {
			SQLiteConnection conn = context.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
			int version = conn.getVersion();
			if (version == 0 || DB_VERSION != version) {
				if (readonly) {
					conn.close();
					conn = context.getSQLiteAPI().getOrCreateDatabase(DB_NAME, false);
				}
				if (version == 0) {
					onCreate(conn);
				} else {
					onUpgrade(conn, version, DB_VERSION);
				}
				conn.setVersion(DB_VERSION);
			}
			return conn;
		}

		private void onCreate(SQLiteConnection db) {
			db.execSQL(HISTORY_TABLE_CREATE);
		}

		private void onUpgrade(SQLiteConnection db, int oldVersion, int newVersion) {
			if (oldVersion < 2) {
				db.execSQL("DROP TABLE IF EXISTS " + HISTORY_TABLE_NAME);
				onCreate(db);
			}
		}

		public boolean remove(PointHistoryEntry e) {
			SQLiteConnection db = openConnection(false);
			if (db != null) {
				try {
					db.execSQL("DELETE FROM " + HISTORY_TABLE_NAME + " WHERE " + HISTORY_COL_NAME + " = ?",
							new Object[]{e.getSerializedName()});
				} finally {
					db.close();
				}
				return true;
			}
			return false;
		}

		public boolean removeAll() {
			SQLiteConnection db = openConnection(false);
			if (db != null) {
				try {
					db.execSQL("DELETE FROM " + HISTORY_TABLE_NAME);
				} finally {
					db.close();
				}
				return true;
			}
			return false;
		}

		public boolean update(PointHistoryEntry e) {
			SQLiteConnection db = openConnection(false);
			if (db != null) {
				try {
					db.execSQL("UPDATE " + HISTORY_TABLE_NAME +
									" SET " + HISTORY_COL_TIME + " = ?, " +
									HISTORY_COL_FREQ_INTERVALS + " = ?, " +
									HISTORY_COL_FREQ_VALUES + " = ? WHERE " +
									HISTORY_COL_NAME + " = ?",
							new Object[]{e.lastAccessedTime, e.getIntervals(), e.getIntervalsValues(),
									e.getSerializedName()});
				} finally {
					db.close();
				}
				return true;
			}
			return false;
		}

		public boolean add(PointHistoryEntry e) {
			SQLiteConnection db = openConnection(false);
			if (db != null) {
				try {
					insert(e, db);
				} finally {
					db.close();
				}
				return true;
			}
			return false;
		}

		private void insert(PointHistoryEntry e, SQLiteConnection db) {
			db.execSQL("INSERT INTO " + HISTORY_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?)",
					new Object[]{e.getSerializedName(), e.lastAccessedTime,
							e.getIntervals(), e.getIntervalsValues(), e.getLat(), e.getLon()});
		}

		public List<PointHistoryEntry> getPoints() {
			List<PointHistoryEntry> res = new ArrayList<>();
			SQLiteConnection db = openConnection(true);
			if (db != null) {
				try {
					SQLiteCursor query = db.rawQuery(HISTORY_TABLE_SELECT, null);
					Map<PointDescription, PointHistoryEntry> st = new HashMap<>();
					if (query != null && query.moveToFirst()) {
						boolean reinsert = false;
						do {
							String name = query.getString(0);
							long lastAccessed = query.getLong(1);
							String intervals = query.getString(2);
							String values = query.getString(3);
							double lat = query.getDouble(4);
							double lon = query.getDouble(5);

							PointDescription pd = PointDescription.deserializeFromString(name, new LatLon(lat, lon));
							PointHistoryEntry e = new PointHistoryEntry(lat, lon, pd);
							e.lastAccessedTime = lastAccessed;
							e.setFrequency(intervals, values);
							if (st.containsKey(pd)) {
								reinsert = true;
							}
							res.add(e);
							st.put(pd, e);
						} while (query.moveToNext());

						if (reinsert) {
							System.err.println("Reinsert all values for search history");
							db.execSQL("DELETE FROM " + HISTORY_TABLE_NAME);
							res.clear();
							res.addAll(st.values());
							for (PointHistoryEntry he : res) {
								insert(he, db);
							}
						}
					}
					if (query != null) {
						query.close();
					}
				} finally {
					db.close();
				}
			}
			return res;
		}
	}
}
