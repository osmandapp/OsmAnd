package net.osmand.plus.poi;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.api.SQLiteAPI.SQLiteStatement;
import net.osmand.util.Algorithms;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static net.osmand.plus.poi.PoiFiltersHelper.PoiFilterType.SEARCH;
import static net.osmand.plus.poi.PoiFiltersHelper.PoiFilterType.TOP_DEFINED;

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

	private static final String[] DEL = new String[]{
			UDF_CAR_AID, UDF_FOR_TOURISTS, UDF_FOOD_SHOP, UDF_FUEL, UDF_SIGHTSEEING, UDF_EMERGENCY,
			UDF_PUBLIC_TRANSPORT, UDF_ACCOMMODATION, UDF_RESTAURANTS, UDF_PARKING
	};
	
	public static final Comparator<PoiUIFilter> ORDER_COMPARATOR = new Comparator<PoiUIFilter>() {
		@Override
		public int compare(PoiUIFilter o1, PoiUIFilter o2) {
			return (o1.getOrder() < o2.getOrder()) ? -1 : ((o1.getOrder() == o2.getOrder()) ? 0 : 1);
		}
	};

	public static final Comparator<PoiUIFilter> ALPHABET_COMPARATOR = new Comparator<PoiUIFilter>() {
		@Override
		public int compare(PoiUIFilter o1, PoiUIFilter o2) {
			return o1.compareTo(o2);
		}
	};
	
	public enum PoiFilterType {
		TOP_DEFINED,
		SEARCH
	}

	public PoiFiltersHelper(OsmandApplication application) {
		this.application = application;
		PoiFilterDbHelper helper = openDbHelperNoPois();
		helper.doDeletion();
		helper.close();
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

	public void resetNominatimFilters() {
		nominatimPOIFilter = null;
		nominatimAddressFilter = null;
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
					PoiUIFilter.CUSTOM_FILTER_ID, new LinkedHashMap<PoiCategory, LinkedHashSet<String>>(), application);
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
						application.getLangTranslation(application.getLanguage()));
				filter.setSavedFilterByName("wiki:lang:" + application.getLanguage());
				filter.setStandardFilter(true);
				localWikiPoiFilter = filter;
			}
		}
		return localWikiPoiFilter;
	}

	public PoiUIFilter getShowAllPOIFilter() {
		if (showAllPOIFilter == null) {
			PoiUIFilter filter = new PoiUIFilter(null, application, "");
			filter.setStandardFilter(true);
			showAllPOIFilter = filter;
		}
		return showAllPOIFilter;
	}

	public void markHistory(String filterId, boolean history) {
		PoiFilterDbHelper helper = openDbHelperNoPois();
		helper.markHistory(filterId, history);
		helper.close();
	}

	public void clearHistory() {
		PoiFilterDbHelper helper = openDbHelperNoPois();
		helper.clearHistory();
		helper.close();
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
		return getFilterById(filterId, false);
	}

	public PoiUIFilter getFilterById(String filterId, boolean includeDeleted) {
		if (filterId == null) {
			return null;
		}
		for (PoiUIFilter f : getTopDefinedPoiFilters(includeDeleted)) {
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
				ArrayList<PoiUIFilter> copy = new ArrayList<>(cacheTopStandardFilters);
				copy.add(lf);
				Collections.sort(copy);
				cacheTopStandardFilters = copy;
				return lf;
			}
			AbstractPoiType lt = application.getPoiTypes().getAnyPoiAdditionalTypeByKey(typeId);
			if (lt != null) {
				PoiUIFilter lf = new PoiUIFilter(lt, application, "");
				ArrayList<PoiUIFilter> copy = new ArrayList<>(cacheTopStandardFilters);
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

	public List<PoiUIFilter> getUserDefinedPoiFilters(boolean includeDeleted) {
		ArrayList<PoiUIFilter> userDefinedFilters = new ArrayList<>();
		PoiFilterDbHelper helper = openDbHelper();
		if (helper != null) {
			List<PoiUIFilter> userDefined = helper.getFilters(helper.getReadableDatabase(), includeDeleted);
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
		return getTopDefinedPoiFilters(false);
	}

	public List<PoiUIFilter> getTopDefinedPoiFilters(boolean includeDeleted) {
		if (cacheTopStandardFilters == null) {
			List<PoiUIFilter> top = new ArrayList<>();
			// user defined
			top.addAll(getUserDefinedPoiFilters(true));
			if (getLocalWikiPOIFilter() != null) {
				top.add(getLocalWikiPOIFilter());
			}
			// default
			MapPoiTypes poiTypes = application.getPoiTypes();
			for (AbstractPoiType t : poiTypes.getTopVisibleFilters()) {
				PoiUIFilter f = new PoiUIFilter(t, application, "");
				top.add(f);
			}
			Collections.sort(top);
			cacheTopStandardFilters = top;
		}
		List<PoiUIFilter> result = new ArrayList<>();
		for (PoiUIFilter filter : cacheTopStandardFilters) {
			if (includeDeleted || !filter.isDeleted()) {
				result.add(filter);
			}
		}
		result.add(getShowAllPOIFilter());
		return result;
	}

	public List<PoiUIFilter> getProfileDependentPoiUIFilters(boolean onlyActive) {
		return getProfileDependentPoiUIFilters(onlyActive, new PoiFilterType[]{TOP_DEFINED, SEARCH});
	}

	public List<PoiUIFilter> getProfileDependentPoiUIFilters(boolean onlyActive, PoiFilterType[] filterTypes) {
		initPoiUIFiltersArrangementAndActivation();
		List<PoiUIFilter> result = new ArrayList<>();
		for (PoiFilterType type : filterTypes) {
			List<PoiUIFilter> filterList = new ArrayList<>();
			switch (type) {
				case TOP_DEFINED:
					filterList = getTopDefinedPoiFilters();
					break;
				case SEARCH:
					filterList = getSearchPoiFilters();
					break;
			}
			if (onlyActive) {
				for (PoiUIFilter f : filterList) {
					if (f.isActive) {
						result.add(f);
					}
				}
			} else {
				result.addAll(filterList);
			}
		}
		Collections.sort(result, ORDER_COMPARATOR);
		return result;
	}
	
	private void initPoiUIFiltersArrangementAndActivation() {
		List<PoiUIFilter> allFilters = new ArrayList<>();
		allFilters.addAll(getTopDefinedPoiFilters());
		allFilters.addAll(getSearchPoiFilters());

		List<PoiFilterBean> poiBeans = getPoiFiltersBeans();
		Map<String, Boolean> activation = null;
		Map<String, Integer> arrangement = null;
		if (poiBeans != null) {
			activation = new HashMap<>();
			arrangement = new HashMap<>();
			for (PoiFilterBean filterBean : poiBeans) {
				activation.put(filterBean.getFilterId(), filterBean.isActive);
				arrangement.put(filterBean.getFilterId(), filterBean.getOrder());
			}
		}
		reorderPoiFilters(allFilters, arrangement);
		refreshPoiFiltersActivation(allFilters, activation);
		
		//set up the biggest order to custom filter
		PoiUIFilter customFilter = getCustomPOIFilter();
		customFilter.setActive(true);
		customFilter.setOrder(allFilters.size());
	}
	
	private void reorderPoiFilters(List<PoiUIFilter> filters, Map<String, Integer> arrangement) {
		boolean defaultArrangement = application.getSettings().ARRANGE_POI_FILTERS_BY_DEFAULT.get();
		if (arrangement == null || defaultArrangement) {
			//arrangement by default
			Collections.sort(filters, ALPHABET_COMPARATOR);
			for (int i = 0; i < filters.size(); i++) {
				PoiUIFilter poiFilter = filters.get(i);
				poiFilter.setOrder(i);
			}
		} else {
			for (PoiUIFilter filter : filters) {
				String filterId = filter.getFilterId();
				if (arrangement.containsKey(filterId)) {
					filter.setOrder(arrangement.get(filterId));
				}
			}
		}
	}
	
	private void refreshPoiFiltersActivation(List<PoiUIFilter> allFilters, Map<String, Boolean> activation) {
		if (activation == null) {
			//activation by default
			for (PoiUIFilter filter : allFilters) {
				filter.setActive(true);
			}
		} else {
			for (PoiUIFilter filter : allFilters) {
				String filterId = filter.getFilterId();
				if (activation.containsKey(filterId)) {
					filter.setActive(activation.get(filterId));
				}
			}
		}
	}

	public static class PoiFilterBean {
		@Expose
		String filterId;
		@Expose
		int order;
		@Expose
		boolean isActive;

		public PoiFilterBean(String filterId, int order, boolean isActive) {
			this.filterId = filterId;
			this.order = order;
			this.isActive = isActive;
		}

		public String getFilterId() {
			return filterId;
		}

		public int getOrder() {
			return order;
		}

		public boolean isActive() {
			return isActive;
		}

		public void setFilterId(String filterId) {
			this.filterId = filterId;
		}

		public void setOrder(int order) {
			this.order = order;
		}

		public void setActive(boolean active) {
			isActive = active;
		}
	}

	public void savePoiFiltersBeans(List<PoiFilterBean> list) {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		String defaultProfiles = gson.toJson(list);
		application.getSettings().POI_FILTERS_ARRANGEMENT.set(defaultProfiles);
	}

	public List<PoiFilterBean> getPoiFiltersBeans() {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		Type t = new TypeToken<ArrayList<PoiFilterBean>>() {
		}.getType();
		return gson.fromJson(application.getSettings().POI_FILTERS_ARRANGEMENT.get(), t);
	}

	private PoiFilterDbHelper openDbHelperNoPois() {
		return new PoiFilterDbHelper(null, application);
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
		boolean res = helper.deleteFilter(helper.getWritableDatabase(), filter, false);
		helper.close();
		return res;
	}

	public boolean createPoiFilter(PoiUIFilter filter, boolean forHistory) {
		PoiFilterDbHelper helper = openDbHelper();
		if (helper == null) {
			return false;
		}
		helper.deleteFilter(helper.getWritableDatabase(), filter, true);
		Iterator<PoiUIFilter> it = cacheTopStandardFilters.iterator();
		while (it.hasNext()) {
			if (it.next().getFilterId().equals(filter.getFilterId())) {
				it.remove();
			}
		}
		boolean res = helper.addFilter(filter, helper.getWritableDatabase(), false, forHistory);
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
		selectedPoiFilters.clear();
		Set<String> filters = application.getSettings().getSelectedPoiFilters();
		for (String f : filters) {
			PoiUIFilter filter = getFilterById(f);
			if (filter != null) {
				selectedPoiFilters.add(filter);
			}
		}
	}

	private void saveSelectedPoiFilters() {
		Set<String> filters = new HashSet<>();
		for (PoiUIFilter f : selectedPoiFilters) {
			filters.add(f.filterId);
		}
		application.getSettings().setSelectedPoiFilters(filters);
	}

	public class PoiFilterDbHelper {

		private static final int TRUE_INT = 1;
		private static final int FALSE_INT = 0;

		public static final String DATABASE_NAME = "poi_filters";
		private static final int DATABASE_VERSION = 6;

		private static final String FILTER_NAME = "poi_filters";
		private static final String FILTER_COL_NAME = "name";
		private static final String FILTER_COL_ID = "id";
		private static final String FILTER_COL_FILTERBYNAME = "filterbyname";
		private static final String FILTER_COL_HISTORY = "history";
		private static final String FILTER_COL_DELETED = "deleted";

		private static final String FILTER_TABLE_CREATE = "CREATE TABLE " +
				FILTER_NAME + " (" +
				FILTER_COL_NAME + ", " +
				FILTER_COL_ID + ", " +
				FILTER_COL_FILTERBYNAME + ", " +
				FILTER_COL_HISTORY + ", " +
				FILTER_COL_DELETED + ");";

		private static final String CATEGORIES_NAME = "categories";
		private static final String CATEGORIES_FILTER_ID = "filter_id";
		private static final String CATEGORIES_COL_CATEGORY = "category";
		private static final String CATEGORIES_COL_SUBCATEGORY = "subcategory";

		private static final String CATEGORIES_TABLE_CREATE = "CREATE TABLE " +
				CATEGORIES_NAME + " (" +
				CATEGORIES_FILTER_ID + ", " +
				CATEGORIES_COL_CATEGORY + ", " +
				CATEGORIES_COL_SUBCATEGORY + ");";

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
			if (conn.getVersion() < DATABASE_VERSION) {
				if (readonly) {
					conn.close();
					conn = context.getSQLiteAPI().getOrCreateDatabase(DATABASE_NAME, false);
				}
				int version = conn.getVersion();
				conn.setVersion(DATABASE_VERSION);
				if (version == 0) {
					onCreate(conn);
				} else {
					onUpgrade(conn, version, DATABASE_VERSION);
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
			if (oldVersion < 6) {
				conn.execSQL("ALTER TABLE " + FILTER_NAME + " ADD " + FILTER_COL_HISTORY + " int DEFAULT " + FALSE_INT);
				conn.execSQL("ALTER TABLE " + FILTER_NAME + " ADD " + FILTER_COL_DELETED + " int DEFAULT " + FALSE_INT);
			}
		}

		private void deleteOldFilters(SQLiteConnection conn) {
			if (conn != null) {
				for (String toDel : DEL) {
					deleteFilter(conn, "user_" + toDel);
				}
			}
		}

		void doDeletion() {
			SQLiteConnection conn = getWritableDatabase();
			if (conn != null) {
				String query = "SELECT " + FILTER_COL_ID + ", " + FILTER_COL_HISTORY + ", " + FILTER_COL_DELETED + " FROM " + FILTER_NAME;
				SQLiteCursor cursor = conn.rawQuery(query, null);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						do {
							if (cursor.getInt(1) == FALSE_INT && cursor.getInt(2) == TRUE_INT) {
								deleteFilter(conn, cursor.getString(0));
							}
						} while (cursor.moveToNext());
					}
					cursor.close();
				}
			}
		}

		void markHistory(String filterId, boolean history) {
			SQLiteConnection conn = getWritableDatabase();
			if (conn != null) {
				conn.execSQL("UPDATE " + FILTER_NAME + " SET " + FILTER_COL_HISTORY + " = ? WHERE " + FILTER_COL_ID + " = ?",
						new Object[]{history ? TRUE_INT : FALSE_INT, filterId});
			}
		}

		void clearHistory() {
			SQLiteConnection conn = getWritableDatabase();
			if (conn != null) {
				conn.execSQL("UPDATE " + FILTER_NAME + " SET " + FILTER_COL_HISTORY + " = ?", new Object[]{FALSE_INT});
			}
		}

		protected boolean addFilter(PoiUIFilter p, SQLiteConnection db, boolean addOnlyCategories, boolean forHistory) {
			if (db != null) {
				if (!addOnlyCategories) {
					p.setDeleted(forHistory);
					int value = forHistory ? TRUE_INT : FALSE_INT;
					db.execSQL("INSERT INTO " + FILTER_NAME + " VALUES (?, ?, ?, ?, ?)",
							new Object[]{p.getName(), p.getFilterId(), p.getFilterByName(), value, value});
				}
				Map<PoiCategory, LinkedHashSet<String>> types = p.getAcceptedTypes();
				SQLiteStatement insertCategories = db.compileStatement("INSERT INTO " + CATEGORIES_NAME + " VALUES (?, ?, ?)");
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

		protected List<PoiUIFilter> getFilters(SQLiteConnection conn, boolean includeDeleted) {
			ArrayList<PoiUIFilter> list = new ArrayList<>();
			if (conn != null) {
				SQLiteCursor query = conn.rawQuery("SELECT " + CATEGORIES_FILTER_ID + ", " + CATEGORIES_COL_CATEGORY + "," + CATEGORIES_COL_SUBCATEGORY + " FROM " +
						CATEGORIES_NAME, null);
				Map<String, Map<PoiCategory, LinkedHashSet<String>>> map = new LinkedHashMap<>();
				if (query != null && query.moveToFirst()) {
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
				if (query != null) {
					query.close();
				}

				query = conn.rawQuery("SELECT " +
						FILTER_COL_ID + ", " +
						FILTER_COL_NAME + ", " +
						FILTER_COL_FILTERBYNAME + ", " +
						FILTER_COL_DELETED +
						" FROM " + FILTER_NAME, null);
				if (query != null && query.moveToFirst()) {
					do {
						String filterId = query.getString(0);
						boolean deleted = query.getInt(3) == TRUE_INT;
						if (map.containsKey(filterId) && (includeDeleted || !deleted)) {
							PoiUIFilter filter = new PoiUIFilter(query.getString(1), filterId,
									map.get(filterId), application);
							filter.setSavedFilterByName(query.getString(2));
							filter.setDeleted(deleted);
							list.add(filter);
						}
					} while (query.moveToNext());
				}
				if (query != null) {
					query.close();
				}
			}
			return list;
		}

		protected boolean editFilter(SQLiteConnection conn, PoiUIFilter filter) {
			if (conn != null) {
				conn.execSQL("DELETE FROM " + CATEGORIES_NAME + " WHERE " + CATEGORIES_FILTER_ID + " = ?",
						new Object[]{filter.getFilterId()});
				addFilter(filter, conn, true, false);
				updateName(conn, filter);
				return true;
			}
			return false;
		}

		private void updateName(SQLiteConnection db, PoiUIFilter filter) {
			db.execSQL("UPDATE " + FILTER_NAME + " SET " + FILTER_COL_FILTERBYNAME + " = ?, " + FILTER_COL_NAME + " = ? " + " WHERE "
					+ FILTER_COL_ID + "= ?", new Object[]{filter.getFilterByName(), filter.getName(), filter.getFilterId()});
		}

		protected boolean deleteFilter(SQLiteConnection db, PoiUIFilter p, boolean force) {
			if (db != null) {
				if (force) {
					deleteFilter(db, p.getFilterId());
				} else {
					db.execSQL("UPDATE " + FILTER_NAME + " SET " + FILTER_COL_DELETED + " = ? WHERE " + FILTER_COL_ID + " = ?",
							new Object[]{TRUE_INT, p.getFilterId()});
				}
				return true;
			}
			return false;
		}

		private void deleteFilter(@NonNull SQLiteConnection db, String key) {
			db.execSQL("DELETE FROM " + FILTER_NAME + " WHERE " + FILTER_COL_ID + " = ?", new Object[]{key});
			db.execSQL("DELETE FROM " + CATEGORIES_NAME + " WHERE " + CATEGORIES_FILTER_ID + " = ?", new Object[]{key});
		}
	}
}
