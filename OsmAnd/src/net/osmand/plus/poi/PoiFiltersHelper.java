package net.osmand.plus.poi;

import android.support.annotation.NonNull;

import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.api.SQLiteAPI.SQLiteStatement;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class PoiFiltersHelper {
	private final OsmandApplication application;

	private NominatimPoiFilter nominatimPOIFilter;
	private NominatimPoiFilter nominatimAddressFilter;

	private PoiUIFilter searchByNamePOIFilter;
	private PoiUIFilter customPOIFilter;
	private PoiUIFilter showAllPOIFilter;
	private PoiUIFilter localWikiPoiFilter;
	private List<PoiUIFilter> cacheTopStandardFilters;
	private Set<PoiUIFilter> selectedPoiFilters = new TreeSet<>();

	private static final String UDF_CAR_AID = "car_aid";
	private static final String UDF_FOR_TOURISTS = "for_tourists";
	private static final String UDF_FOOD_SHOP = "food_shop";
	private static final String UDF_FUEL = "fuel";
	private static final String UDF_SIGHTSEEING = "sightseeing";
	private static final String UDF_EMERGENCY = "emergency";
	private static final String UDF_PUBLIC_TRANSPORT = "public_transport";
	private static final String UDF_ACCOMMODATION = "accommodation";
	private static final String UDF_RESTAURANTS = "restaurants";
	private static final String UDF_PARKING = "parking";
	
	private static final String[] DEL = new String[] {
		UDF_CAR_AID, UDF_FOR_TOURISTS, UDF_FOOD_SHOP, UDF_FUEL, UDF_SIGHTSEEING, UDF_EMERGENCY,
		UDF_PUBLIC_TRANSPORT, UDF_ACCOMMODATION, UDF_RESTAURANTS, UDF_PARKING
	};

	public PoiFiltersHelper(OsmandApplication application) {
		this.application = application;
	}

	public NominatimPoiFilter getNominatimPOIFilter() {
		if (nominatimPOIFilter == null) {
			nominatimPOIFilter = new NominatimPoiFilter(application, false);
		}
		return nominatimPOIFilter;
	}

	public NominatimPoiFilter getNominatimAddressFilter() {
		if (nominatimAddressFilter == null) {
			nominatimAddressFilter = new NominatimPoiFilter(application, true);
		}
		return nominatimAddressFilter;
	}

	public PoiUIFilter getSearchByNamePOIFilter() {
		if (searchByNamePOIFilter == null) {
			PoiUIFilter filter = new SearchByNameFilter(application);
			filter.setStandardFilter(true);
			searchByNamePOIFilter = filter;
		}
		return searchByNamePOIFilter;
	}

	public PoiUIFilter getCustomPOIFilter() {
		if (customPOIFilter == null) {
			PoiUIFilter filter = new PoiUIFilter(application.getString(R.string.poi_filter_custom_filter),
					PoiUIFilter.CUSTOM_FILTER_ID, new LinkedHashMap<PoiCategory, LinkedHashSet<String>>(), application); //$NON-NLS-1$
			filter.setStandardFilter(true);
			customPOIFilter = filter;
		}
		return customPOIFilter;
	}

	public PoiUIFilter getLocalWikiPOIFilter() {
		if (localWikiPoiFilter == null) {
			PoiType place = application.getPoiTypes().getPoiTypeByKey("wiki_place");
			if (place != null && !Algorithms.isEmpty(application.getLanguage())) {
				PoiUIFilter filter = new PoiUIFilter(place, application, " " +
						application.getLangTranslation(application.getLanguage())); //$NON-NLS-1$
				filter.setSavedFilterByName("wiki:lang:" + application.getLanguage());
				filter.setStandardFilter(true);
				localWikiPoiFilter = filter;
			}
		}
		return localWikiPoiFilter;
	}

	public PoiUIFilter getShowAllPOIFilter() {
		if (showAllPOIFilter == null) {
			PoiUIFilter filter = new PoiUIFilter(null, application, ""); //$NON-NLS-1$
			filter.setStandardFilter(true);
			showAllPOIFilter = filter;
		}
		return showAllPOIFilter;
	}


	private PoiUIFilter getFilterById(String filterId, PoiUIFilter... filters) {
		for (PoiUIFilter pf : filters) {
			if (pf != null && pf.getFilterId() != null && filterId != null && pf.getFilterId().equals(filterId)) {
				return pf;
			}
		}
		return null;
	}

	public PoiUIFilter getFilterById(String filterId) {
		if (filterId == null) {
			return null;
		}
		for (PoiUIFilter f : getTopDefinedPoiFilters()) {
			if (f.getFilterId().equals(filterId)) {
				return f;
			}
		}
		PoiUIFilter ff = getFilterById(filterId, getCustomPOIFilter(), getSearchByNamePOIFilter(),
				getLocalWikiPOIFilter(), getShowAllPOIFilter(), getNominatimPOIFilter(), getNominatimAddressFilter());
		if (ff != null) {
			return ff;
		}
		if (filterId.startsWith(PoiUIFilter.STD_PREFIX)) {
			String typeId = filterId.substring(PoiUIFilter.STD_PREFIX.length());
			AbstractPoiType tp = application.getPoiTypes().getAnyPoiTypeByKey(typeId);
			if (tp != null) {
				PoiUIFilter lf = new PoiUIFilter(tp, application, "");
				ArrayList<PoiUIFilter> copy = new ArrayList<PoiUIFilter>(cacheTopStandardFilters);
				copy.add(lf);
				Collections.sort(copy);
				cacheTopStandardFilters = copy;
				return lf;
			}
			AbstractPoiType lt = application.getPoiTypes().getAnyPoiAdditionalTypeByKey(typeId);
			if (lt != null) {
				PoiUIFilter lf = new PoiUIFilter(lt, application, "");
				ArrayList<PoiUIFilter> copy = new ArrayList<PoiUIFilter>(cacheTopStandardFilters);
				copy.add(lf);
				Collections.sort(copy);
				cacheTopStandardFilters = copy;
				return lf;
			}
		}
		return null;
	}

	public void reloadAllPoiFilters() {
		showAllPOIFilter = null;
		getShowAllPOIFilter();
		cacheTopStandardFilters = null;
		getTopDefinedPoiFilters();
	}

	public List<PoiUIFilter> getUserDefinedPoiFilters() {
		ArrayList<PoiUIFilter> userDefinedFilters = new ArrayList<PoiUIFilter>();
		PoiFilterDbHelper helper = openDbHelper();
		if (helper != null) {
			List<PoiUIFilter> userDefined = helper.getFilters(helper.getReadableDatabase());
			userDefinedFilters.addAll(userDefined);
			helper.close();
		}
		return userDefinedFilters;
	}

	public List<PoiUIFilter> getSearchPoiFilters() {
		List<PoiUIFilter> result = new ArrayList<>();
		List<PoiUIFilter> filters = Arrays.asList(getCustomPOIFilter(),  // getShowAllPOIFilter(),
				getSearchByNamePOIFilter(), getNominatimPOIFilter(), getNominatimAddressFilter());
		for (PoiUIFilter f : filters) {
			if (f != null && !f.isEmpty()) {
				result.add(f);
			}
		}
		return result;
	}

	public List<PoiUIFilter> getTopDefinedPoiFilters() {
//		if (cacheTopStandardFilters == null) {
			List<PoiUIFilter> top = new ArrayList<PoiUIFilter>();
			// user defined
			top.addAll(getUserDefinedPoiFilters());
			if (getLocalWikiPOIFilter() != null) {
				top.add(getLocalWikiPOIFilter());
			}
			// default
			MapPoiTypes poiTypes = application.getPoiTypes();
			for (PoiFilter t : poiTypes.getTopVisibleFilters()) {
				PoiUIFilter f = new PoiUIFilter(t, application, "");
				top.add(f);
			}
			Collections.sort(top);
			cacheTopStandardFilters = top;
//		}
		List<PoiUIFilter> result = new ArrayList<PoiUIFilter>();
		result.addAll(cacheTopStandardFilters);
		result.add(getShowAllPOIFilter());
		return result;
	}

	private PoiFilterDbHelper openDbHelper() {
		if (!application.getPoiTypes().isInit()) {
			return null;
		}
		return new PoiFilterDbHelper(application.getPoiTypes(), application);
	}

	public boolean removePoiFilter(PoiUIFilter filter) {
		if (filter.getFilterId().equals(PoiUIFilter.CUSTOM_FILTER_ID) ||
				filter.getFilterId().equals(PoiUIFilter.BY_NAME_FILTER_ID) ||
				filter.getFilterId().startsWith(PoiUIFilter.STD_PREFIX)) {
			return false;
		}
		PoiFilterDbHelper helper = openDbHelper();
		if (helper == null) {
			return false;
		}
		boolean res = helper.deleteFilter(helper.getWritableDatabase(), filter);
		if (res) {
			ArrayList<PoiUIFilter> copy = new ArrayList<>(cacheTopStandardFilters);
			copy.remove(filter);
			cacheTopStandardFilters = copy;
		}
		helper.close();
		return res;
	}

	public boolean createPoiFilter(PoiUIFilter filter) {
		PoiFilterDbHelper helper = openDbHelper();
		if (helper == null) {
			return false;
		}
		boolean res = helper.deleteFilter(helper.getWritableDatabase(), filter);
		Iterator<PoiUIFilter> it = cacheTopStandardFilters.iterator();
		while (it.hasNext()) {
			if (it.next().getFilterId().equals(filter.getFilterId())) {
				it.remove();
			}
		}
		res = helper.addFilter(filter, helper.getWritableDatabase(), false);
		if (res) {
			ArrayList<PoiUIFilter> copy = new ArrayList<>(cacheTopStandardFilters);
			copy.add(filter);
			Collections.sort(copy);
			cacheTopStandardFilters = copy;
		}
		helper.close();
		return res;
	}

	public boolean editPoiFilter(PoiUIFilter filter) {
		if (filter.getFilterId().equals(PoiUIFilter.CUSTOM_FILTER_ID) ||
				filter.getFilterId().equals(PoiUIFilter.BY_NAME_FILTER_ID) || filter.getFilterId().startsWith(PoiUIFilter.STD_PREFIX)) {
			return false;
		}
		PoiFilterDbHelper helper = openDbHelper();
		if (helper != null) {
			boolean res = helper.editFilter(helper.getWritableDatabase(), filter);
			helper.close();
			return res;
		}
		return false;
	}

	@NonNull
	public Set<PoiUIFilter> getSelectedPoiFilters() {
		return selectedPoiFilters;
	}

	public void addSelectedPoiFilter(PoiUIFilter filter) {
		selectedPoiFilters.add(filter);
		saveSelectedPoiFilters();
	}

	public void removeSelectedPoiFilter(PoiUIFilter filter) {
		selectedPoiFilters.remove(filter);
		saveSelectedPoiFilters();
	}

	public boolean isShowingAnyPoi() {
		return !selectedPoiFilters.isEmpty();
	}

	public void clearSelectedPoiFilters() {
		selectedPoiFilters.clear();
		saveSelectedPoiFilters();
	}

	public void hidePoiFilters() {
		selectedPoiFilters.clear();
	}

	public String getFiltersName(Set<PoiUIFilter> filters) {
		if (filters.isEmpty()) {
			return application.getResources().getString(R.string.shared_string_none);
		} else {
			List<String> names = new ArrayList<>();
			for (PoiUIFilter filter : filters) {
				names.add(filter.getName());
			}
			return android.text.TextUtils.join(", ", names);
		}
	}

	public String getSelectedPoiFiltersName() {
		return getFiltersName(selectedPoiFilters);
	}

	public boolean isPoiFilterSelected(PoiUIFilter filter) {
		return selectedPoiFilters.contains(filter);
	}

	public boolean isPoiFilterSelected(String filterId) {
		for (PoiUIFilter filter : selectedPoiFilters) {
			if (filter.filterId.equals(filterId)) {
				return true;
			}
		}
		return false;
	}

	public void loadSelectedPoiFilters() {
		Set<String> filters = application.getSettings().getSelectedPoiFilters();
		for (String f : filters) {
			PoiUIFilter filter = getFilterById(f);
			if (filter != null) {
				selectedPoiFilters.add(filter);
			}
		}
	}

	public void saveSelectedPoiFilters() {
		Set<String> filters = new HashSet<>();
		for (PoiUIFilter f : selectedPoiFilters) {
			filters.add(f.filterId);
		}
		application.getSettings().setSelectedPoiFilters(filters);
	}

	public class PoiFilterDbHelper {

		public static final String DATABASE_NAME = "poi_filters"; //$NON-NLS-1$
		private static final int DATABASE_VERSION = 5;
		private static final String FILTER_NAME = "poi_filters"; //$NON-NLS-1$
		private static final String FILTER_COL_NAME = "name"; //$NON-NLS-1$
		private static final String FILTER_COL_ID = "id"; //$NON-NLS-1$
		private static final String FILTER_COL_FILTERBYNAME = "filterbyname"; //$NON-NLS-1$
		private static final String FILTER_TABLE_CREATE = "CREATE TABLE " + FILTER_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
				FILTER_COL_NAME + ", " + FILTER_COL_ID + ", " + FILTER_COL_FILTERBYNAME + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$


		private static final String CATEGORIES_NAME = "categories"; //$NON-NLS-1$
		private static final String CATEGORIES_FILTER_ID = "filter_id"; //$NON-NLS-1$
		private static final String CATEGORIES_COL_CATEGORY = "category"; //$NON-NLS-1$
		private static final String CATEGORIES_COL_SUBCATEGORY = "subcategory"; //$NON-NLS-1$
		private static final String CATEGORIES_TABLE_CREATE = "CREATE TABLE " + CATEGORIES_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
				CATEGORIES_FILTER_ID + ", " + CATEGORIES_COL_CATEGORY + ", " + CATEGORIES_COL_SUBCATEGORY + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		private OsmandApplication context;
		private SQLiteConnection conn;
		private MapPoiTypes mapPoiTypes;

		PoiFilterDbHelper(MapPoiTypes mapPoiTypes, OsmandApplication context) {
			this.mapPoiTypes = mapPoiTypes;
			this.context = context;
		}

		public SQLiteConnection getWritableDatabase() {
			return openConnection(false);
		}

		public void close() {
			if (conn != null) {
				conn.close();
				conn = null;
			}
		}

		public SQLiteConnection getReadableDatabase() {
			return openConnection(true);
		}

		private SQLiteConnection openConnection(boolean readonly) {
			conn = context.getSQLiteAPI().getOrCreateDatabase(DATABASE_NAME, readonly);
			if (conn.getVersion() == 0 || DATABASE_VERSION != conn.getVersion()) {
				if (readonly) {
					conn.close();
					conn = context.getSQLiteAPI().getOrCreateDatabase(DATABASE_NAME, false);
				}
				if (conn.getVersion() == 0) {
					conn.setVersion(DATABASE_VERSION);
					onCreate(conn);
				} else {
					onUpgrade(conn, conn.getVersion(), DATABASE_VERSION);
				}

			}
			return conn;
		}

		public void onCreate(SQLiteConnection conn) {
			conn.execSQL(FILTER_TABLE_CREATE);
			conn.execSQL(CATEGORIES_TABLE_CREATE);
		}


		public void onUpgrade(SQLiteConnection conn, int oldVersion, int newVersion) {
			if (newVersion <= 5) {
				deleteOldFilters(conn);
			}
			conn.setVersion(newVersion);
		}

		private void deleteOldFilters(SQLiteConnection conn) {
			for (String toDel : DEL) {
				deleteFilter(conn, "user_" + toDel);
			}
		}

		protected boolean addFilter(PoiUIFilter p, SQLiteConnection db, boolean addOnlyCategories) {
			if (db != null) {
				if (!addOnlyCategories) {
					db.execSQL("INSERT INTO " + FILTER_NAME + " VALUES (?, ?, ?)", new Object[]{p.getName(), p.getFilterId(), p.getFilterByName()}); //$NON-NLS-1$ //$NON-NLS-2$
				}
				Map<PoiCategory, LinkedHashSet<String>> types = p.getAcceptedTypes();
				SQLiteStatement insertCategories = db.compileStatement("INSERT INTO " + CATEGORIES_NAME + " VALUES (?, ?, ?)"); //$NON-NLS-1$ //$NON-NLS-2$
				for (PoiCategory a : types.keySet()) {
					if (types.get(a) == null) {
						insertCategories.bindString(1, p.getFilterId());
						insertCategories.bindString(2, a.getKeyName());
						insertCategories.bindNull(3);
						insertCategories.execute();
					} else {
						for (String s : types.get(a)) {
							insertCategories.bindString(1, p.getFilterId());
							insertCategories.bindString(2, a.getKeyName());
							insertCategories.bindString(3, s);
							insertCategories.execute();
						}
					}
				}
				insertCategories.close();
				return true;
			}
			return false;
		}

		protected List<PoiUIFilter> getFilters(SQLiteConnection conn) {
			ArrayList<PoiUIFilter> list = new ArrayList<PoiUIFilter>();
			if (conn != null) {
				SQLiteCursor query = conn.rawQuery("SELECT " + CATEGORIES_FILTER_ID + ", " + CATEGORIES_COL_CATEGORY + "," + CATEGORIES_COL_SUBCATEGORY + " FROM " +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						CATEGORIES_NAME, null);
				Map<String, Map<PoiCategory, LinkedHashSet<String>>> map = new LinkedHashMap<String, Map<PoiCategory, LinkedHashSet<String>>>();
				if (query.moveToFirst()) {
					do {
						String filterId = query.getString(0);
						if (!map.containsKey(filterId)) {
							map.put(filterId, new LinkedHashMap<PoiCategory, LinkedHashSet<String>>());
						}
						Map<PoiCategory, LinkedHashSet<String>> m = map.get(filterId);
						PoiCategory a = mapPoiTypes.getPoiCategoryByName(query.getString(1).toLowerCase(), false);
						String subCategory = query.getString(2);
						if (subCategory == null) {
							m.put(a, null);
						} else {
							if (m.get(a) == null) {
								m.put(a, new LinkedHashSet<String>());
							}
							m.get(a).add(subCategory);
						}
					} while (query.moveToNext());
				}
				query.close();

				query = conn.rawQuery("SELECT " + FILTER_COL_ID + ", " + FILTER_COL_NAME + "," + FILTER_COL_FILTERBYNAME + " FROM " +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						FILTER_NAME, null);
				if (query.moveToFirst()) {
					do {
						String filterId = query.getString(0);
						if (map.containsKey(filterId)) {
							PoiUIFilter filter = new PoiUIFilter(query.getString(1), filterId,
									map.get(filterId), application);
							filter.setSavedFilterByName(query.getString(2));
							list.add(filter);
						}
					} while (query.moveToNext());
				}
				query.close();
			}
			return list;
		}

		protected boolean editFilter(SQLiteConnection conn, PoiUIFilter filter) {
			if (conn != null) {
				conn.execSQL("DELETE FROM " + CATEGORIES_NAME + " WHERE " + CATEGORIES_FILTER_ID + " = ?",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						new Object[]{filter.getFilterId()});
				addFilter(filter, conn, true);
				updateName(conn, filter);
				return true;
			}
			return false;
		}

		private void updateName(SQLiteConnection db, PoiUIFilter filter) {
			db.execSQL("UPDATE " + FILTER_NAME + " SET " + FILTER_COL_FILTERBYNAME + " = ?, " + FILTER_COL_NAME + " = ? " + " WHERE " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
					+ FILTER_COL_ID + "= ?", new Object[]{filter.getFilterByName(), filter.getName(), filter.getFilterId()}); //$NON-NLS-1$
		}

		protected boolean deleteFilter(SQLiteConnection db, PoiUIFilter p) {
			String key = p.getFilterId();
			return deleteFilter(db, key);
		}

		private boolean deleteFilter(SQLiteConnection db, String key) {
			if (db != null) {
				db.execSQL("DELETE FROM " + FILTER_NAME + " WHERE " + FILTER_COL_ID + " = ?", new Object[]{key}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				db.execSQL(
						"DELETE FROM " + CATEGORIES_NAME + " WHERE " + CATEGORIES_FILTER_ID + " = ?", new Object[]{key}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return true;
			}
			return false;
		}


	}


}
