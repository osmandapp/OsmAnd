package net.osmand.plus.poi;


import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.api.SQLiteAPI.SQLiteStatement;

public class PoiFiltersHelper {
	private final OsmandApplication application;
	
	private NameFinderPoiFilter nameFinderPOIFilter;
	private List<PoiLegacyFilter> cacheTopStandardFilters;
	private List<PoiLegacyFilter> cacheUserDefinedFilters;
	private List<PoiLegacyFilter> cacheOsmDefinedFilters;
	
	private static final String UDF_CAR_AID = "car_aid";
	private static final String UDF_FOR_TOURISTS = "for_tourists";
	private static final String UDF_FOOD_SHOP = "food_shop";
	private static final String UDF_FUEL = "fuel";
	private static final String UDF_SIGHTSEEING = "sightseeing";
	private static final String UDF_EMERGENCY = "emergency";
	private static final String UDF_PUBLIC_TRANSPORT = "public_transport";
	private static final String UDF_ENTERTAINMENT = "entertainment";
	private static final String UDF_ACCOMMODATION = "accomodation";
	private static final String UDF_RESTAURANTS = "restaurants";
	private static final String UDF_PARKING = "parking";
	
	private static final String[] DEL = new String[] {};
	
	public PoiFiltersHelper(OsmandApplication application){
		this.application = application;
	}
	public NameFinderPoiFilter getNameFinderPOIFilter() {
		if(nameFinderPOIFilter == null){
			nameFinderPOIFilter = new NameFinderPoiFilter(application);
		}
		return nameFinderPOIFilter;
	}
	
	
	private PoiLegacyFilter findPoiFilter(String filterId, List<PoiLegacyFilter>... collections) {
		for(List<PoiLegacyFilter> c : collections) {
			for(PoiLegacyFilter f : c) {
				if(f.getFilterId().equals(filterId)){
					return f;
				}
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public PoiLegacyFilter getFilterById(String filterId){
		if(filterId == null){
			return null;
		}
		if(filterId.equals(NameFinderPoiFilter.FILTER_ID)){
			return getNameFinderPOIFilter();
		}
		return findPoiFilter(filterId, getUserDefinedPoiFilters(), getTopStandardFilters(), getOsmDefinedPoiFilters());
	}
	
	
	public MapPoiTypes getPoiTypes() {
		return application.getPoiTypes();
	}
	
	private void putAll(Map<PoiCategory, LinkedHashSet<String>> types, String tp) {
		types.put(getPoiTypes().getPoiCategoryByName(tp), null);
	}

	private void putValues(Map<PoiCategory, LinkedHashSet<String>> types, String p, String... vls) {
		LinkedHashSet<String> list = new LinkedHashSet<String>();
		Collections.addAll(list, vls);
		types.put(getPoiTypes().getPoiCategoryByName(p), list);
	}
	
	private List<PoiLegacyFilter> getUserDefinedDefaultFilters() {
		List<PoiLegacyFilter> filters = new ArrayList<PoiLegacyFilter>();
		filters.add(new PoiLegacyFilter(application.getString(R.string.poi_filter_accomodation), PoiLegacyFilter.USER_PREFIX + UDF_ACCOMMODATION,
				configureDefaultUserDefinedFilter(null, UDF_ACCOMMODATION), application));
		filters.add(new PoiLegacyFilter(application.getString(R.string.poi_filter_car_aid), PoiLegacyFilter.USER_PREFIX + UDF_CAR_AID,
				configureDefaultUserDefinedFilter(null, UDF_CAR_AID), application));
		filters.add(new PoiLegacyFilter(application.getString(R.string.poi_filter_food_shop), PoiLegacyFilter.USER_PREFIX + UDF_FOOD_SHOP,
				configureDefaultUserDefinedFilter(null, UDF_FOOD_SHOP), application));
		filters.add(new PoiLegacyFilter(application.getString(R.string.poi_filter_for_tourists), PoiLegacyFilter.USER_PREFIX + UDF_FOR_TOURISTS,
				configureDefaultUserDefinedFilter(null, UDF_FOR_TOURISTS), application));
		filters.add(new PoiLegacyFilter(application.getString(R.string.poi_filter_fuel), PoiLegacyFilter.USER_PREFIX + UDF_FUEL,
				configureDefaultUserDefinedFilter(null, UDF_FUEL), application));
		filters.add(new PoiLegacyFilter(application.getString(R.string.poi_filter_parking), PoiLegacyFilter.USER_PREFIX + UDF_PARKING,
				configureDefaultUserDefinedFilter(null, UDF_PARKING), application));
		filters.add(new PoiLegacyFilter(application.getString(R.string.poi_filter_public_transport),
				PoiLegacyFilter.USER_PREFIX + UDF_PUBLIC_TRANSPORT, configureDefaultUserDefinedFilter(null, UDF_PUBLIC_TRANSPORT), application));
		filters.add(new PoiLegacyFilter(application.getString(R.string.poi_filter_restaurants), PoiLegacyFilter.USER_PREFIX + UDF_RESTAURANTS,
				configureDefaultUserDefinedFilter(null, UDF_RESTAURANTS), application));
		filters.add(new PoiLegacyFilter(application.getString(R.string.poi_filter_sightseeing), PoiLegacyFilter.USER_PREFIX + UDF_SIGHTSEEING,
				configureDefaultUserDefinedFilter(null, UDF_SIGHTSEEING), application));
		// UDF_EMERGENCY = "emergency";
		// UDF_ENTERTAINMENT = "entertainment";
		return filters;
	}
	
	private Map<PoiCategory, LinkedHashSet<String>> configureDefaultUserDefinedFilter(Map<PoiCategory, LinkedHashSet<String>> types, String key) {
		if(types == null) {
			types = new LinkedHashMap<PoiCategory, LinkedHashSet<String>>();
		}
		if(UDF_ACCOMMODATION.equals(key)){
			putValues(types, "tourism", "camp_site",
					"caravan_site","picnic_site","alpine_hut", "chalet","guest_house",
					"hostel", "hotel","motel");
		} else if (UDF_CAR_AID.equals(key)) {
			putValues(types, "transportation", "fuel", "car_wash", "car_repair","car", "car_sharing");
			putValues(types, "shop", "fuel", "car_wash", "car_repair","car", "car_parts");
		} else if (UDF_FOOD_SHOP.equals(key)) {
			putValues(types, "shop", "alcohol", "bakery", "beverages", "butcher", "convenience", "department_store",
					"convenience", "farm", "general", "ice_cream", "kiosk", "seafood", "supermarket", "variety_store");
		} else if(UDF_FOR_TOURISTS.equals(key)){
			putAll(types, "historic");
			putAll(types, "tourism");
			putAll(types, "finance");
			putAll(types, "osmwiki");
			putValues(types, "other", "place_of_worship", "internet_access_wlan", "internet_access_wired",
					"internet_access_terminal", "internet_access_public", "internet_access_service",
					"embassy", "marketplace", "post_office", "telephone", "toilets", "emergency_phone");
		} else if(UDF_FUEL.equals(key)){
			putValues(types, "transportation", "fuel");
		} else if (UDF_PARKING.equals(key)) {
			putValues(types, "transportation", "parking",
					"bicycle_parking");
		} else if (UDF_PUBLIC_TRANSPORT.equals(key)) {
			putValues(types, "transportation", "public_transport_stop_position", "public_transport_platform",
					"public_transport_station",
					// railway
					"railway_platform", "railway_station", "halt", "tram_stop", "subway_entrance", "railway_buffer_stop",
					// bus, cars, bicycle
					"bus_stop", "platform", "ferry_terminal", "taxi", "bicycle_rental", "bus_station", "car_rental", "car_sharing",
					// aero
					"airport", "aerodrome", "terminal", "gate",
					// aerial ways ( hide ways)
					// "aerialway_cable_car", "aerialway_gondola", "aerialway_chair_lift", "aerialway_mixed_lift", "aerialway_drag_lift", "aerialway_goods", 
					"aerialway_station"
					// ways (hide ways)
					// "rail", "tram", "light_rail", "subway", "railway_narrow_gauge", "railway_monorail", "railway_funicular"
					);
		} else if (UDF_RESTAURANTS.equals(key)) {
			putValues(types, "sustenance", "restaurant",
					"cafe", "food_court", "fast_food", "pub", "bar", "biergarten");
		} else if (UDF_SIGHTSEEING.equals(key)) {
			putAll(types, "historic");
			putValues(types, "tourism", "attraction",
					"artwork","zoo","theme_park", "museum","viewpoint");
			putAll(types, "osmwiki");
			putValues(types, "other", "place_of_worship");
		} else if (UDF_EMERGENCY.equals(key)) {
			putAll(types, "healthcare");
			putAll(types, "emergency");
		} else if (UDF_ENTERTAINMENT.equals(key)) {
			putAll(types, "entertainment");
		}
		return types;
	}
	
	public void reloadAllPoiFilters() {
		cacheOsmDefinedFilters = null;
		cacheTopStandardFilters = null;
		cacheUserDefinedFilters = null;
		getUserDefinedPoiFilters();
		getTopStandardFilters();
		getOsmDefinedPoiFilters();
		
	}
	
	
	public List<PoiLegacyFilter> getUserDefinedPoiFilters(){
		if(cacheUserDefinedFilters == null){
			cacheUserDefinedFilters = new ArrayList<PoiLegacyFilter>();
			PoiFilterDbHelper helper = openDbHelper();
			List<PoiLegacyFilter> userDefined = helper.getFilters(helper.getReadableDatabase());
			sortListOfFiltersByName(userDefined);
			cacheUserDefinedFilters.addAll(userDefined);
			helper.close();
		}
		return Collections.unmodifiableList(cacheUserDefinedFilters);
	}
	private void sortListOfFiltersByName(List<PoiLegacyFilter> userDefined) {
		final Collator instance = Collator.getInstance();
		Collections.sort(userDefined, new Comparator<PoiLegacyFilter>() {
			@Override
			public int compare(PoiLegacyFilter object1, PoiLegacyFilter object2) {
				return instance.compare(object1.getName(), object2.getName());
			}
		});
	}
	
	public List<PoiLegacyFilter> getTopStandardFilters() {
		if (cacheTopStandardFilters == null) {
			cacheTopStandardFilters = new ArrayList<PoiLegacyFilter>();

			PoiLegacyFilter filter = new SearchByNameFilter(application);
			filter.setStandardFilter(true);
			cacheTopStandardFilters.add(filter);

			filter = new PoiLegacyFilter(application.getString(R.string.poi_filter_custom_filter),
					PoiLegacyFilter.CUSTOM_FILTER_ID, new LinkedHashMap<PoiCategory, LinkedHashSet<String>>(), application); //$NON-NLS-1$
			filter.setStandardFilter(true);
			cacheTopStandardFilters.add(filter);

			cacheTopStandardFilters.add(new PoiLegacyFilter(null, application));
		}
		return Collections.unmodifiableList(cacheTopStandardFilters);
	}
	
	public static String getOsmDefinedFilterId(PoiCategory t){
		return PoiLegacyFilter.STD_PREFIX + (t == null ? null : t.getKeyName());
	}
	
	public void updateFilters(boolean onlyAddFilters){
		PoiFilterDbHelper helper = openDbHelper();
		helper.upgradeFilters(helper.getWritableDatabase(), onlyAddFilters);
		helper.close();	
	}
	
	
	public List<PoiLegacyFilter> getOsmDefinedPoiFilters(){
		if(cacheOsmDefinedFilters == null){
			cacheOsmDefinedFilters = new ArrayList<PoiLegacyFilter>();
			for(PoiCategory t : getPoiTypes().getCategories()){
				cacheOsmDefinedFilters.add(new PoiLegacyFilter(t, application));
			}
			final Collator instance = Collator.getInstance();
			Collections.sort(cacheOsmDefinedFilters, new Comparator<PoiLegacyFilter>() {
				@Override
				public int compare(PoiLegacyFilter object1, PoiLegacyFilter object2) {
					return instance.compare(object1.getName(), object2.getName());
				}
			});
		}
		return Collections.unmodifiableList(cacheOsmDefinedFilters);
	}
	
	private PoiFilterDbHelper openDbHelper(){
		return new PoiFilterDbHelper(application); 
	}
	
	public boolean removePoiFilter(PoiLegacyFilter filter){
		if(filter.getFilterId().equals(PoiLegacyFilter.CUSTOM_FILTER_ID) || 
				filter.getFilterId().equals(PoiLegacyFilter.BY_NAME_FILTER_ID) ||
				filter.getFilterId().startsWith(PoiLegacyFilter.STD_PREFIX)){
			return false;
		}
		PoiFilterDbHelper helper = openDbHelper();
		if(helper == null){
			return false;
		}
		boolean res = helper.deleteFilter(helper.getWritableDatabase(), filter);
		if(res){
			cacheUserDefinedFilters.remove(filter);
		}
		helper.close();
		return res;
	}
	
	public boolean createPoiFilter(PoiLegacyFilter filter){
		PoiFilterDbHelper helper = openDbHelper();
		if(helper == null){
			return false;
		}
		boolean res = helper.addFilter(filter, helper.getWritableDatabase(), false);
		if(res){
			cacheUserDefinedFilters.add(filter);
			sortListOfFiltersByName(cacheUserDefinedFilters);
		}
		helper.close();
		return res;
	}
	
	
	
	public boolean editPoiFilter(PoiLegacyFilter filter) {
		if (filter.getFilterId().equals(PoiLegacyFilter.CUSTOM_FILTER_ID) || 
				filter.getFilterId().equals(PoiLegacyFilter.BY_NAME_FILTER_ID) || filter.getFilterId().startsWith(PoiLegacyFilter.STD_PREFIX)) {
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
	
	
	public class PoiFilterDbHelper  {

		public static final String DATABASE_NAME = "poi_filters"; //$NON-NLS-1$
	    private static final int DATABASE_VERSION = 2;
	    private static final String FILTER_NAME = "poi_filters"; //$NON-NLS-1$
	    private static final String FILTER_COL_NAME = "name"; //$NON-NLS-1$
	    private static final String FILTER_COL_ID = "id"; //$NON-NLS-1$
	    private static final String FILTER_COL_FILTERBYNAME = "filterbyname"; //$NON-NLS-1$
	    private static final String FILTER_TABLE_CREATE =   "CREATE TABLE " + FILTER_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
	    FILTER_COL_NAME + ", " + FILTER_COL_ID + ", " +  FILTER_COL_FILTERBYNAME + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	    
	    
	    private static final String CATEGORIES_NAME = "categories"; //$NON-NLS-1$
	    private static final String CATEGORIES_FILTER_ID = "filter_id"; //$NON-NLS-1$
	    private static final String CATEGORIES_COL_CATEGORY = "category"; //$NON-NLS-1$
	    private static final String CATEGORIES_COL_SUBCATEGORY = "subcategory"; //$NON-NLS-1$
	    private static final String CATEGORIES_TABLE_CREATE =   "CREATE TABLE " + CATEGORIES_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
	    CATEGORIES_FILTER_ID + ", " + CATEGORIES_COL_CATEGORY + ", " +  CATEGORIES_COL_SUBCATEGORY + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		private OsmandApplication context;
		private SQLiteConnection conn;

	    PoiFilterDbHelper(OsmandApplication context) {
			this.context = context;
	    }
	    
	    public SQLiteConnection getWritableDatabase() {
	    	return openConnection(false);
		}

		public void close() {
			if(conn != null) {
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
					conn = context.getSQLiteAPI().getOrCreateDatabase(DATABASE_NAME, readonly);
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
	        upgradeFilters(conn, true);
	    }

		public void upgradeFilters(SQLiteConnection conn, boolean onlyAdd) {
			List<PoiLegacyFilter> filters = PoiFilterDbHelper.this.getFilters(conn);
			List<PoiLegacyFilter> def = getUserDefinedDefaultFilters();
	        for(PoiLegacyFilter f : filters){
	        	PoiLegacyFilter std = null;
	        	for(PoiLegacyFilter d : def){
	        		if(f.getFilterId().equals(d.getFilterId())){
	        			std = d;
	        			break;
	        		}
	        	}
	        	for(String toDel : DEL) {
	        		if(f.getFilterId().equals(toDel)) {
	        			deleteFilter(conn, f);
	        		}
	        	}
	        	if(std != null){
	        		if(!onlyAdd){
	        			editFilter(conn, std);
	        		} else {
	        			updateName(conn, std);
	        		}
	        		def.remove(std);
	        	}
	        }
	        for(PoiLegacyFilter d : def){
	        	addFilter(d, conn, false);
	        }
		}
		
		public void onUpgrade(SQLiteConnection conn, int oldVersion, int newVersion) {
			if (newVersion == 2 || newVersion == 3) {
				upgradeFilters(conn, false);
			} else {
				upgradeFilters(conn, true);
			}
			conn.setVersion(newVersion);
		}
	    
	    protected boolean addFilter(PoiLegacyFilter p, SQLiteConnection db, boolean addOnlyCategories){
	    	if(db != null){
	    		if(!addOnlyCategories){
	    			db.execSQL("INSERT INTO " + FILTER_NAME + " VALUES (?, ?, ?)",new Object[]{p.getName(), p.getFilterId(), p.getFilterByName()}); //$NON-NLS-1$ //$NON-NLS-2$
	    		}
	    		Map<PoiCategory, LinkedHashSet<String>> types = p.getAcceptedTypes();
	    		SQLiteStatement insertCategories = db.compileStatement("INSERT INTO " +  CATEGORIES_NAME + " VALUES (?, ?, ?)"); //$NON-NLS-1$ //$NON-NLS-2$
	    		for(PoiCategory a : types.keySet()){
	    			if(types.get(a) == null){
		    			insertCategories.bindString(1, p.getFilterId());
						insertCategories.bindString(2, a.getTranslation());
						insertCategories.bindNull(3);
    					insertCategories.execute();
	    			} else {
	    				for(String s : types.get(a)){
	    					insertCategories.bindString(1, p.getFilterId());
	    					insertCategories.bindString(2, a.getTranslation());
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
	    
	    protected List<PoiLegacyFilter> getFilters(SQLiteConnection conn){
	    	ArrayList<PoiLegacyFilter> list = new ArrayList<PoiLegacyFilter>();
	    	if(conn != null){
	    		SQLiteCursor query = conn.rawQuery("SELECT " + CATEGORIES_FILTER_ID +", " + CATEGORIES_COL_CATEGORY +"," + CATEGORIES_COL_SUBCATEGORY +" FROM " +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	    				CATEGORIES_NAME, null);
	    		Map<String, Map<PoiCategory, LinkedHashSet<String>>> map = new LinkedHashMap<String, Map<PoiCategory,LinkedHashSet<String>>>();
	    		if(query.moveToFirst()){
	    			do {
	    				String filterId = query.getString(0);
	    				if(!map.containsKey(filterId)){
	    					map.put(filterId, new LinkedHashMap<PoiCategory, LinkedHashSet<String>>());
	    				}
	    				Map<PoiCategory, LinkedHashSet<String>> m = map.get(filterId);
	    				PoiCategory a = getPoiTypes().getPoiCategoryByName(query.getString(1));
	    				String subCategory = query.getString(2);
	    				if(subCategory == null){
	    					m.put(a, null);
	    				} else {
	    					if(m.get(a) == null){
	    						m.put(a, new LinkedHashSet<String>());
	    					}
	    					m.get(a).add(subCategory);
	    				}
	    			} while(query.moveToNext());
	    		}
	    		query.close();
	    		
	    		query = conn.rawQuery("SELECT " + FILTER_COL_ID +", " + FILTER_COL_NAME +"," + FILTER_COL_FILTERBYNAME +" FROM " +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	    				FILTER_NAME, null);
	    		if(query.moveToFirst()){
	    			do {
	    				String filterId = query.getString(0);
	    				if(map.containsKey(filterId)){
	    					PoiLegacyFilter filter = new PoiLegacyFilter(query.getString(1), filterId,
	    							map.get(filterId), application);
	    					filter.setFilterByName(query.getString(2));
	    					list.add(filter);
	    				}
	    			} while(query.moveToNext());
	    		}
	    		query.close();
	    	}
	    	return list;
	    }
	    
	    protected boolean editFilter(SQLiteConnection conn, PoiLegacyFilter filter) {
			if (conn != null) {
				conn.execSQL("DELETE FROM " + CATEGORIES_NAME + " WHERE " + CATEGORIES_FILTER_ID + " = ?",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						new Object[] { filter.getFilterId() });
				addFilter(filter, conn, true);
				updateName(conn, filter);
				return true;
			}
			return false;
		}

		private void updateName(SQLiteConnection db, PoiLegacyFilter filter) {
			db.execSQL("UPDATE " + FILTER_NAME + " SET " + FILTER_COL_FILTERBYNAME + " = ?, " + FILTER_COL_NAME + " = ? " + " WHERE " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
					+ FILTER_COL_ID + "= ?", new Object[] { filter.getFilterByName(), filter.getName(), filter.getFilterId() }); //$NON-NLS-1$
		}
	    
	    protected boolean deleteFilter(SQLiteConnection db, PoiLegacyFilter p){
	    	if(db != null){
	    		db.execSQL("DELETE FROM " + FILTER_NAME + " WHERE " +FILTER_COL_ID + " = ?",new Object[]{p.getFilterId()}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	    		db.execSQL("DELETE FROM " + CATEGORIES_NAME + " WHERE " +CATEGORIES_FILTER_ID + " = ?", new Object[]{p.getFilterId()}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	    		return true;
	    	}
	    	return false;
	    }
	    


	}

}
