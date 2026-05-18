package net.osmand.plus.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.backup.BackupUtils;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchHistoryHelper {

	private static final int HISTORY_LIMIT = 1500;
	private static final int[] DEF_INTERVALS_MIN = {
			5, 60, 60 * 24, 5 * 60 * 24, 10 * 60 * 24, 30 * 60 * 24
	};

	private static SearchHistoryHelper instance;

	private final OsmandApplication app;
	private List<HistoryEntry> loadedEntries;
	private final Map<PointDescription, HistoryEntry> mp = new HashMap<>();

	public SearchHistoryHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public static SearchHistoryHelper getInstance(@NonNull OsmandApplication app) {
		if (instance == null) {
			instance = new SearchHistoryHelper(app);
		}
		return instance;
	}

	public long getLastModifiedTime() {
		return new HistoryItemDBHelper().getLastModifiedTime();
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		new HistoryItemDBHelper().setLastModifiedTime(lastModifiedTime);
	}

	public void addNewItemToHistory(double latitude, double longitude, PointDescription name, HistorySource source) {
		addNewItemToHistory(new HistoryEntry(latitude, longitude, name, source));
	}

	public void addNewItemToHistory(AbstractPoiType poiType, HistorySource source) {
		addNewItemToHistory(new HistoryEntry(0, 0, createPointDescription(poiType), source));
	}

	public void addNewItemToHistory(PoiUIFilter filter, HistorySource source) {
		addNewItemToHistory(new HistoryEntry(0, 0, createPointDescription(filter), source));
		if (app.getSettings().SEARCH_HISTORY.get()) {
			app.getPoiFilters().markHistory(filter.getFilterId(), true);
		}
	}

	public void addNewItemToHistory(GPXInfo gpxInfo, HistorySource source) {
		if (gpxInfo != null) {
			addNewItemToHistory(new HistoryEntry(0, 0, createPointDescription(gpxInfo), source));
		}
	}

	@NonNull
	public List<HistoryEntry> getHistoryEntries(boolean onlyPoints) {
		return getHistoryEntries(null, onlyPoints, false);
	}

	@NonNull
	public List<HistoryEntry> getHistoryEntries(@Nullable HistorySource source, boolean onlyPoints) {
		return getHistoryEntries(source, onlyPoints, false);
	}

	@NonNull
	public List<HistoryEntry> getHistoryEntries(@Nullable HistorySource source, boolean onlyPoints, boolean includeDeleted) {
		if (loadedEntries == null) {
			checkLoadedEntries();
		}
		List<HistoryEntry> entries = new ArrayList<>();
		for (HistoryEntry entry : loadedEntries) {
			PointDescription description = entry.getName();

			boolean exists = isPointDescriptionExists(description);
			if ((includeDeleted || exists) && (source == null || entry.source == source)) {
				if (!onlyPoints || (!description.isPoiType() && !description.isCustomPoiFilter())) {
					entries.add(entry);
				}
			}
		}
		return entries;
	}

	private boolean isPointDescriptionExists(@NonNull PointDescription description) {
		String name = description.getName();
		if (description.isPoiType()) {
			MapPoiTypes poiTypes = app.getPoiTypes();
			return poiTypes.getAnyPoiTypeByKey(name) != null || poiTypes.getAnyPoiAdditionalTypeByKey(name) != null;
		} else if (description.isCustomPoiFilter()) {
			return app.getPoiFilters().getFilterById(name, true) != null;
		} else if (description.isGpxFile()) {
			return GpxUiHelper.getGpxInfoByFileName(app, name) != null;
		}
		return true;
	}

	@NonNull
	public List<SearchResult> getHistoryResults(@Nullable HistorySource source, boolean onlyPoints, boolean includeDeleted) {
		List<SearchResult> searchResults = new ArrayList<>();

		SearchPhrase phrase = SearchPhrase.emptyPhrase();
		for (HistoryEntry entry : getHistoryEntries(source, onlyPoints, includeDeleted)) {
			SearchResult result = SearchHistoryAPI.createSearchResult(app, entry, phrase);
			searchResults.add(result);
		}
		return searchResults;
	}

	private PointDescription createPointDescription(AbstractPoiType pt) {
		return new PointDescription(PointDescription.POINT_TYPE_POI_TYPE, pt.getKeyName());
	}

	private PointDescription createPointDescription(PoiUIFilter filter) {
		return new PointDescription(PointDescription.POINT_TYPE_CUSTOM_POI_FILTER, filter.getFilterId());
	}

	private PointDescription createPointDescription(GPXInfo gpxInfo) {
		return new PointDescription(PointDescription.POINT_TYPE_GPX_FILE, gpxInfo.getFileName());
	}

	public void remove(Object item) {
		PointDescription pd = null;
		if (item instanceof HistoryEntry) {
			pd = ((HistoryEntry) item).getName();
		} else if (item instanceof AbstractPoiType) {
			pd = createPointDescription((AbstractPoiType) item);
		} else if (item instanceof PoiUIFilter) {
			pd = createPointDescription((PoiUIFilter) item);
		} else if (item instanceof GPXInfo) {
			pd = createPointDescription((GPXInfo) item);
		}
		if (pd != null) {
			remove(pd);
		}
	}

	private void remove(PointDescription pd) {
		HistoryEntry model = mp.get(pd);
		if (model != null && checkLoadedEntries().remove(model)) {
			if (pd.isCustomPoiFilter()) {
				app.getPoiFilters().markHistory(pd.getName(), false);
			}
			loadedEntries = CollectionUtils.removeFromList(loadedEntries, model);
			mp.remove(pd);
		}
	}

	public void removeAll() {
		HistoryItemDBHelper helper = checkLoadedEntries();
		if (helper.removeAll()) {
			app.getPoiFilters().clearHistory();
			loadedEntries = new ArrayList<>();
			mp.clear();
		}
	}

	private HistoryItemDBHelper checkLoadedEntries() {
		HistoryItemDBHelper helper = new HistoryItemDBHelper();
		if (loadedEntries == null) {
			loadedEntries = sortHistoryEntries(helper.getEntries());
			for (HistoryEntry he : loadedEntries) {
				mp.put(he.getName(), he);
			}
		}
		return helper;
	}

	private void addNewItemToHistory(HistoryEntry model) {
		if (app.getSettings().SEARCH_HISTORY.get()) {
			HistoryItemDBHelper helper = checkLoadedEntries();
			if (mp.containsKey(model.getName())) {
				model = mp.get(model.getName());
				model.markAsAccessed(System.currentTimeMillis());
				helper.update(model);
			} else {
				loadedEntries = CollectionUtils.addToList(loadedEntries, model);
				mp.put(model.getName(), model);
				model.markAsAccessed(System.currentTimeMillis());
				helper.add(model);
			}
			updateEntriesList();
		}
	}

	public void addItemsToHistory(List<HistoryEntry> entries) {
		for (HistoryEntry model : entries) {
			addItemToHistoryWithReplacement(model);
		}
		updateEntriesList();
	}

	public void updateEntriesList() {
		HistoryItemDBHelper helper = checkLoadedEntries();
		List<HistoryEntry> historyEntries = sortHistoryEntries(loadedEntries);

		while (historyEntries.size() > HISTORY_LIMIT) {
			int lastIndex = historyEntries.size() - 1;
			if (helper.remove(historyEntries.get(lastIndex))) {
				historyEntries.remove(lastIndex);
			}
		}
		loadedEntries = historyEntries;
	}

	private void addItemToHistoryWithReplacement(@NonNull HistoryEntry model) {
		HistoryItemDBHelper helper = checkLoadedEntries();
		List<HistoryEntry> historyEntries = new ArrayList<>(loadedEntries);

		PointDescription name = model.getName();
		if (mp.containsKey(name)) {
			HistoryEntry oldModel = mp.remove(name);
			historyEntries.remove(oldModel);
			helper.remove(model);
		}
		historyEntries.add(model);
		loadedEntries = historyEntries;

		mp.put(name, model);
		helper.add(model);
	}

	@Nullable
	public HistoryEntry getEntryByName(@Nullable PointDescription pd) {
		return pd != null ? mp.get(pd) : null;
	}

	@NonNull
	private List<HistoryEntry> sortHistoryEntries(@NonNull List<HistoryEntry> historyEntries) {
		List<HistoryEntry> entries = new ArrayList<>(historyEntries);
		Collections.sort(entries, new HistoryEntryComparator());
		return entries;
	}

	public static class HistoryEntry {
		private final double lat;
		private final double lon;
		private final PointDescription name;
		private final HistorySource source;

		private long lastAccessedTime;
		private int[] intervals = new int[0];
		private double[] intervalValues = new double[0];


		public HistoryEntry(double lat, double lon, @NonNull PointDescription name, @NonNull HistorySource source) {
			this.lat = lat;
			this.lon = lon;
			this.name = name;
			this.source = source;
		}

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

		@NonNull
		public HistorySource getSource() {
			return source;
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
			lastAccessedTime = time;
		}

		double getUsageLastTime(long time, int days, int hours, int minutes) {
			long mins = (minutes + (hours + 24 * days) * 60);
			long timeInPast = time - mins * 60 * 1000;
			if (lastAccessedTime <= timeInPast) {
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
				markAsAccessed(lastAccessedTime);
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

		public long getLastAccessTime() {
			return lastAccessedTime;
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

		public void setLastAccessTime(long time) {
			lastAccessedTime = time;
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
		private static final int DB_VERSION = 3;
		private static final String HISTORY_TABLE_NAME = "history_recents";
		private static final String HISTORY_COL_NAME = "name";
		private static final String HISTORY_COL_TIME = "time";
		private static final String HISTORY_COL_FREQ_INTERVALS = "freq_intervals";
		private static final String HISTORY_COL_FREQ_VALUES = "freq_values";
		private static final String HISTORY_COL_LAT = "latitude";
		private static final String HISTORY_COL_LON = "longitude";
		private static final String HISTORY_COL_SOURCE = "source";
		private static final String HISTORY_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + HISTORY_TABLE_NAME + " (" +
				HISTORY_COL_NAME + " TEXT, " +
				HISTORY_COL_TIME + " long, " +
				HISTORY_COL_FREQ_INTERVALS + " TEXT, " +
				HISTORY_COL_FREQ_VALUES + " TEXT, " +
				HISTORY_COL_LAT + " double, " + HISTORY_COL_LON + " double, " + HISTORY_COL_SOURCE + " TEXT);";

		private static final String HISTORY_LAST_MODIFIED_NAME = "history_recents";

		HistoryItemDBHelper() {
		}

		private SQLiteConnection openConnection(boolean readonly) {
			SQLiteConnection conn = app.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
			if (conn != null && conn.getVersion() < DB_VERSION) {
				if (readonly) {
					conn.close();
					conn = app.getSQLiteAPI().getOrCreateDatabase(DB_NAME, false);
				}
				if (conn != null) {
					int version = conn.getVersion();
					if (version == 0) {
						onCreate(conn);
					} else {
						onUpgrade(conn, version, DB_VERSION);
					}
					conn.setVersion(DB_VERSION);
				}
			}
			return conn;
		}

		public void onCreate(SQLiteConnection db) {
			db.execSQL(HISTORY_TABLE_CREATE);
		}

		public void onUpgrade(SQLiteConnection db, int oldVersion, int newVersion) {
			boolean upgraded = false;
			if (oldVersion < 2) {
				db.execSQL("DROP TABLE IF EXISTS " + HISTORY_TABLE_NAME);
				onCreate(db);
				upgraded = true;
			}
			if (oldVersion < 3) {
				db.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_SOURCE + " TEXT");
			}
			if (upgraded) {
				updateLastModifiedTime();
			}
		}

		public long getLastModifiedTime() {
			long lastModifiedTime = BackupUtils.getLastModifiedTime(app, HISTORY_LAST_MODIFIED_NAME);
			if (lastModifiedTime == 0) {
				File dbFile = app.getDatabasePath(DB_NAME);
				lastModifiedTime = dbFile.exists() ? dbFile.lastModified() : 0;
				BackupUtils.setLastModifiedTime(app, HISTORY_LAST_MODIFIED_NAME, lastModifiedTime);
			}
			return lastModifiedTime;
		}

		public void setLastModifiedTime(long lastModifiedTime) {
			BackupUtils.setLastModifiedTime(app, HISTORY_LAST_MODIFIED_NAME, lastModifiedTime);
		}

		private void updateLastModifiedTime() {
			BackupUtils.setLastModifiedTime(app, HISTORY_LAST_MODIFIED_NAME);
		}

		public boolean remove(HistoryEntry e) {
			SQLiteConnection db = openConnection(false);
			if (db != null) {
				try {
					db.execSQL("DELETE FROM " + HISTORY_TABLE_NAME + " WHERE " +
									HISTORY_COL_NAME + " = ? AND " +
									HISTORY_COL_LAT + " = ? AND " + HISTORY_COL_LON + " = ?",
							new Object[] {e.getSerializedName(), e.getLat(), e.getLon()});
					updateLastModifiedTime();
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
					updateLastModifiedTime();
				} finally {
					db.close();
				}
				return true;
			}
			return false;
		}

		public boolean update(HistoryEntry e) {
			SQLiteConnection db = openConnection(false);
			if (db != null) {
				try {
					db.execSQL(
							"UPDATE " + HISTORY_TABLE_NAME + " SET " + HISTORY_COL_TIME + "= ? " +
									", " + HISTORY_COL_FREQ_INTERVALS + " = ? " +
									", " + HISTORY_COL_FREQ_VALUES + "= ? WHERE " +
									HISTORY_COL_NAME + " = ? AND " +
									HISTORY_COL_LAT + " = ? AND " + HISTORY_COL_LON + " = ? AND " + HISTORY_COL_SOURCE + " = ?",
							new Object[] {e.getLastAccessTime(), e.getIntervals(), e.getIntervalsValues(),
									e.getSerializedName(), e.getLat(), e.getLon(), e.getSource().name()});
					updateLastModifiedTime();
				} finally {
					db.close();
				}
				return true;
			}
			return false;
		}

		public boolean add(HistoryEntry e) {
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

		private void insert(HistoryEntry e, SQLiteConnection db) {
			db.execSQL(
					"INSERT INTO " + HISTORY_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?, ?)",
					new Object[] {e.getSerializedName(), e.getLastAccessTime(),
							e.getIntervals(), e.getIntervalsValues(), e.getLat(), e.getLon(), e.getSource().name()});
			updateLastModifiedTime();
		}

		public List<HistoryEntry> getEntries() {
			List<HistoryEntry> entries = new ArrayList<>();
			SQLiteConnection db = openConnection(true);
			if (db != null) {
				try {
					SQLiteCursor query = db.rawQuery(
							"SELECT " + HISTORY_COL_NAME + ", " + HISTORY_COL_LAT + "," + HISTORY_COL_LON + ", " +
									HISTORY_COL_TIME + ", " + HISTORY_COL_FREQ_INTERVALS + ", " + HISTORY_COL_FREQ_VALUES + ", " + HISTORY_COL_SOURCE +
									" FROM " + HISTORY_TABLE_NAME, null);
					Map<PointDescription, HistoryEntry> st = new HashMap<>();
					if (query != null && query.moveToFirst()) {
						boolean reinsert = false;
						do {
							String name = query.getString(0);
							double lat = query.getDouble(1);
							double lon = query.getDouble(2);
							long lastAccessedTime = query.getLong(3);
							String frequencyIntervals = query.getString(4);
							String frequencyValues = query.getString(5);
							HistorySource source = HistorySource.getHistorySourceByName(query.getString(6));

							PointDescription pd = PointDescription.deserializeFromString(name, new LatLon(lat, lon));
							if (app.getPoiTypes().isTypeForbidden(pd.getName())) {
								query.moveToNext();
							}
							HistoryEntry entry = new HistoryEntry(lat, lon, pd, source);
							entry.setLastAccessTime(lastAccessedTime);
							entry.setFrequency(frequencyIntervals, frequencyValues);
							if (st.containsKey(pd)) {
								reinsert = true;
							}
							entries.add(entry);
							st.put(pd, entry);
						} while (query.moveToNext());
						if (reinsert) {
							System.err.println("Reinsert all values for search history");
							db.execSQL("DELETE FROM " + HISTORY_TABLE_NAME);
							entries.clear();
							entries.addAll(st.values());
							for (HistoryEntry he : entries) {
								insert(he, db);
							}
							updateLastModifiedTime();
						}
					}
					if (query != null) {
						query.close();
					}
				} finally {
					db.close();
				}
			}
			return entries;
		}
	}
}
