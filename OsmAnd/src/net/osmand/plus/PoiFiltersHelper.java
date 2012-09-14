package net.osmand.plus;


import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import net.osmand.data.AmenityType;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

public class PoiFiltersHelper {
	private final OsmandApplication application;
	
	private NameFinderPoiFilter nameFinderPOIFilter;
	private List<PoiFilter> cacheUserDefinedFilters;
	private List<PoiFilter> cacheOsmDefinedFilters;
	
	private static final String UDF_CAR_AID = "car_aid";
	private static final String UDF_FOR_TOURISTS = "for_tourists";
	private static final String UDF_FOOD_SHOP = "food_shop";
	private static final String UDF_FUEL = "fuel";
	private static final String UDF_SIGHTSEEING = "sightseeing";
	private static final String UDF_EMERGENCY = "emergency";
	private static final String UDF_PUBLIC_TRANSPORT = "public_transport";
	private static final String UDF_ENTERTAINMENT = "entertainment";
	private static final String UDF_ACCOMODATION = "accomodation";
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
	
	
	public PoiFilter getFilterById(String filterId){
		if(filterId == null){
			return null;
		}
		if(filterId.equals(NameFinderPoiFilter.FILTER_ID)){
			return getNameFinderPOIFilter();
		}
		if(filterId.startsWith(PoiFilter.USER_PREFIX)){
			List<PoiFilter> filters = getUserDefinedPoiFilters();
			for(PoiFilter f : filters){
				if(f.getFilterId().equals(filterId)){
					return f;
				}
			}
		} else if(filterId.startsWith(PoiFilter.STD_PREFIX)){
			List<PoiFilter> filters = getOsmDefinedPoiFilters();
			for(PoiFilter f : filters){
				if(f.getFilterId().equals(filterId)){
					return f;
				}
			}
		}
		return null;
	}
	
	private void putAll(Map<AmenityType, LinkedHashSet<String>> types, AmenityType tp){
		types.put(tp, null);
	}
	
	private void putValues(Map<AmenityType, LinkedHashSet<String>> types, AmenityType tp,String... vls){
		LinkedHashSet<String> list = new LinkedHashSet<String>();
		for(String v: vls){
			list.add(v);
		}
		types.put(tp, list);
	}
	
	private List<PoiFilter> getUserDefinedDefaultFilters() {
		List<PoiFilter> filters = new ArrayList<PoiFilter>();
		filters.add(new PoiFilter(application.getString(R.string.poi_filter_car_aid), PoiFilter.USER_PREFIX + UDF_CAR_AID,
				configureDefaultUserDefinedFilter(null, UDF_CAR_AID), application));
		filters.add(new PoiFilter(application.getString(R.string.poi_filter_for_tourists), PoiFilter.USER_PREFIX + UDF_FOR_TOURISTS,
				configureDefaultUserDefinedFilter(null, UDF_FOR_TOURISTS), application));
		filters.add(new PoiFilter(application.getString(R.string.poi_filter_fuel), PoiFilter.USER_PREFIX + UDF_FUEL,
				configureDefaultUserDefinedFilter(null, UDF_FUEL), application));
		filters.add(new PoiFilter(application.getString(R.string.poi_filter_food_shop), PoiFilter.USER_PREFIX + UDF_FOOD_SHOP,
				configureDefaultUserDefinedFilter(null, UDF_FOOD_SHOP), application));
		filters.add(new PoiFilter(application.getString(R.string.poi_filter_sightseeing), PoiFilter.USER_PREFIX + UDF_SIGHTSEEING,
				configureDefaultUserDefinedFilter(null, UDF_SIGHTSEEING), application));

		// UDF_EMERGENCY = "emergency";
		// UDF_ENTERTAINMENT = "entertainment";
		filters.add(new PoiFilter(application.getString(R.string.poi_filter_accomodation), PoiFilter.USER_PREFIX + UDF_ACCOMODATION,
				configureDefaultUserDefinedFilter(null, UDF_ACCOMODATION), application));
		filters.add(new PoiFilter(application.getString(R.string.poi_filter_restaurants), PoiFilter.USER_PREFIX + UDF_RESTAURANTS,
				configureDefaultUserDefinedFilter(null, UDF_RESTAURANTS), application));
		filters.add(new PoiFilter(application.getString(R.string.poi_filter_public_transport),
				PoiFilter.USER_PREFIX + UDF_PUBLIC_TRANSPORT, configureDefaultUserDefinedFilter(null, UDF_PUBLIC_TRANSPORT), application));
		filters.add(new PoiFilter(application.getString(R.string.poi_filter_parking), PoiFilter.USER_PREFIX + UDF_PARKING,
				configureDefaultUserDefinedFilter(null, UDF_PARKING), application));
		return filters;
	}
	
	private Map<AmenityType, LinkedHashSet<String>> configureDefaultUserDefinedFilter(Map<AmenityType, LinkedHashSet<String>> types, String key) {
		if(types == null) {
			types = new LinkedHashMap<AmenityType, LinkedHashSet<String>>();
		}
		if(UDF_CAR_AID.equals(key)){
			putValues(types, AmenityType.TRANSPORTATION, "fuel", "car_wash", "car_repair","car", "car_sharing");
			putValues(types, AmenityType.SHOP, "fuel", "car_wash", "car_repair","car");
		} else if(UDF_SIGHTSEEING.equals(key)){
			putAll(types, AmenityType.HISTORIC);
			putAll(types, AmenityType.TOURISM);
			putAll(types, AmenityType.OSMWIKI);
			putValues(types, AmenityType.OTHER, "place_of_worship");
		} else if(UDF_FOR_TOURISTS.equals(key)){
			putAll(types, AmenityType.HISTORIC);
			putAll(types, AmenityType.TOURISM);
			putAll(types, AmenityType.FINANCE);
			putAll(types, AmenityType.OSMWIKI);
			putValues(types, AmenityType.OTHER, "place_of_worship", "internet_access", "embassy","emergency_phone","marketplace",
					"post_office","telephone", "toilets");
		} else if(UDF_FUEL.equals(key)){
			putValues(types, AmenityType.TRANSPORTATION, "fuel");
		} else if (UDF_FOOD_SHOP.equals(key)) {
			putValues(types, AmenityType.SHOP, "alcohol", "bakery", "beverages", "butcher", "convenience", "department_store",
					"convenience", "farm", "general", "ice_cream", "kiosk", "supermarket", "variety_store");
		} else if (UDF_SIGHTSEEING.equals(key)) {
			putAll(types, AmenityType.HISTORIC);
			putValues(types, AmenityType.TOURISM, "attraction",
					"artwork","zoo","theme_park", "museum","viewpoint");
			putValues(types, AmenityType.OTHER, "place_of_worship");
		} else if (UDF_ACCOMODATION.equals(key)) {
			putValues(types, AmenityType.TOURISM, "camp_site",
					"caravan_site","picnic_site","alpine_hut", "chalet","guest_house",
					"hostel", "hotel","motel");
		} else if (UDF_PARKING.equals(key)) {
			putValues(types, AmenityType.TRANSPORTATION, "parking",
					"bicycle_parking");
		} else if (UDF_PUBLIC_TRANSPORT.equals(key)) {
			putValues(types, AmenityType.TRANSPORTATION, "public_transport_stop_position", "public_transport_platform",
					"public_transport_station",
					// railway
					"railway_platform", "railway_station", "halt", "tram_stop", "subway_entrance", "railway_buffer_stop",
					// bus, cars, bicycle
					"bus_stop", "platform", "ferry_terminal", "taxi", "bicycle_rental", "bus_station", "car_rental", "car_sharing",
					// aero
					"airport", "aerodrome", "terminal", "gate",
					// aerial ways
					"aerialway_cable_car", "aerialway_gondola", "aerialway_chair_lift", "aerialway_mixed_lift", "aerialway_drag_lift",
					"aerialway_goods", "aerialway_station",
					// ways
					"rail", "tram", "light_rail", "subway", "railway_narrow_gauge", "railway_monorail", "railway_funicular");
		} else if (UDF_RESTAURANTS.equals(key)) {
			putValues(types, AmenityType.SUSTENANCE, "restaurant",
					"cafe", "food_court", "fast_food", "pub", "bar", "biergarten");
		} else if (UDF_EMERGENCY.equals(key)) {
			putAll(types, AmenityType.HEALTHCARE);
			putAll(types, AmenityType.EMERGENCY);
		} else if (UDF_ENTERTAINMENT.equals(key)) {
			putAll(types, AmenityType.ENTERTAINMENT);
		}
		
		return types;
	}
	
	
	public List<PoiFilter> getUserDefinedPoiFilters(){
		if(cacheUserDefinedFilters == null){
			cacheUserDefinedFilters = new ArrayList<PoiFilter>();
			PoiFilter filter = new PoiFilter(application.getString(R.string.poi_filter_custom_filter), PoiFilter.CUSTOM_FILTER_ID, 
					new LinkedHashMap<AmenityType, LinkedHashSet<String>>(), application); //$NON-NLS-1$
			filter.setStandardFilter(true);
			cacheUserDefinedFilters.add(filter);
			filter = new SearchByNameFilter(application);
			cacheUserDefinedFilters.add(filter);
			PoiFilterDbHelper helper = openDbHelper();
			List<PoiFilter> userDefined = helper.getFilters(helper.getReadableDatabase());
			final Collator instance = Collator.getInstance();
			Collections.sort(userDefined, new Comparator<PoiFilter>() {
				@Override
				public int compare(PoiFilter object1, PoiFilter object2) {
					return instance.compare(object1.getName(), object2.getName());
				}
			});
			cacheUserDefinedFilters.addAll(userDefined);
			helper.close();
		}
		return Collections.unmodifiableList(cacheUserDefinedFilters);
	}
	
	public static String getOsmDefinedFilterId(AmenityType t){
		return PoiFilter.STD_PREFIX + t;
	}
	
	public void updateFilters(boolean onlyAddFilters){
		PoiFilterDbHelper helper = openDbHelper();
		helper.upgradeFilters(helper.getWritableDatabase(), onlyAddFilters);
		helper.close();	
	}
	
	
	public List<PoiFilter> getOsmDefinedPoiFilters(){
		if(cacheOsmDefinedFilters == null){
			cacheOsmDefinedFilters = new ArrayList<PoiFilter>();
			for(AmenityType t : AmenityType.values()){
				cacheOsmDefinedFilters.add(new PoiFilter(t, application));
			}
			final Collator instance = Collator.getInstance();
			Collections.sort(cacheOsmDefinedFilters, new Comparator<PoiFilter>() {
				@Override
				public int compare(PoiFilter object1, PoiFilter object2) {
					return instance.compare(object1.getName(), object2.getName());
				}
			});
			cacheOsmDefinedFilters.add(0, new PoiFilter(null, application));
		}
		return Collections.unmodifiableList(cacheOsmDefinedFilters);
	}
	
	private PoiFilterDbHelper openDbHelper(){
		return new PoiFilterDbHelper(application.getApplicationContext()); 
	}
	
	public boolean removePoiFilter(PoiFilter filter){
		if(filter.getFilterId().equals(PoiFilter.CUSTOM_FILTER_ID) || 
				filter.getFilterId().equals(PoiFilter.BY_NAME_FILTER_ID)){
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
	
	public boolean createPoiFilter(PoiFilter filter){
		PoiFilterDbHelper helper = openDbHelper();
		if(helper == null){
			return false;
		}
		boolean res = helper.addFilter(filter, helper.getWritableDatabase(), false);
		if(res){
			cacheUserDefinedFilters.add(filter);
		}
		helper.close();
		return res;
	}
	
	
	
	public boolean editPoiFilter(PoiFilter filter) {
		if (filter.getFilterId().equals(PoiFilter.CUSTOM_FILTER_ID) || 
				filter.getFilterId().equals(PoiFilter.BY_NAME_FILTER_ID)) {
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
	
	
	public class PoiFilterDbHelper extends SQLiteOpenHelper {

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

	    PoiFilterDbHelper(Context context) {
	        super(context, DATABASE_NAME, null, DATABASE_VERSION);
	    }
	    
	    @Override
	    public void onCreate(SQLiteDatabase db) {
	        db.execSQL(FILTER_TABLE_CREATE);
	        db.execSQL(CATEGORIES_TABLE_CREATE);
	        upgradeFilters(db, true);
	    }

		public void upgradeFilters(SQLiteDatabase db, boolean onlyAdd) {
			List<PoiFilter> filters = PoiFilterDbHelper.this.getFilters(db);
			List<PoiFilter> def = getUserDefinedDefaultFilters();
	        for(PoiFilter f : filters){
	        	PoiFilter std = null;
	        	for(PoiFilter d : def){
	        		if(f.getFilterId().equals(d.getFilterId())){
	        			std = d;
	        			break;
	        		}
	        	}
	        	for(String toDel : DEL) {
	        		if(f.getFilterId().equals(toDel)) {
	        			deleteFilter(db, f);
	        		}
	        	}
	        	if(std != null){
	        		if(!onlyAdd){
	        			editFilter(db, std);
	        		} else {
	        			updateName(db, std);
	        		}
	        		def.remove(std);
	        	}
	        }
	        for(PoiFilter d : def){
	        	addFilter(d, db, false);
	        }
		}
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (newVersion == 2 || newVersion == 3) {
				upgradeFilters(db, false);
			} else {
				upgradeFilters(db, true);
			}
		}
	    
	    protected boolean addFilter(PoiFilter p, SQLiteDatabase db, boolean addOnlyCategories){
	    	if(db != null){
	    		if(!addOnlyCategories){
	    			db.execSQL("INSERT INTO " + FILTER_NAME + " VALUES (?, ?, ?)",new Object[]{p.getName(), p.getFilterId(), p.getFilterByName()}); //$NON-NLS-1$ //$NON-NLS-2$
	    		}
	    		Map<AmenityType, LinkedHashSet<String>> types = p.getAcceptedTypes();
	    		SQLiteStatement insertCategories = db.compileStatement("INSERT INTO " +  CATEGORIES_NAME + " VALUES (?, ?, ?)"); //$NON-NLS-1$ //$NON-NLS-2$
	    		for(AmenityType a : types.keySet()){
	    			if(types.get(a) == null){
		    			insertCategories.bindString(1, p.getFilterId());
						insertCategories.bindString(2, AmenityType.valueToString(a));
						insertCategories.bindNull(3);
    					insertCategories.execute();
	    			} else {
	    				for(String s : types.get(a)){
	    					insertCategories.bindString(1, p.getFilterId());
	    					insertCategories.bindString(2, AmenityType.valueToString(a));
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
	    
	    protected List<PoiFilter> getFilters(SQLiteDatabase db){
	    	ArrayList<PoiFilter> list = new ArrayList<PoiFilter>();
	    	if(db != null){
	    		Cursor query = db.rawQuery("SELECT " + CATEGORIES_FILTER_ID +", " + CATEGORIES_COL_CATEGORY +"," + CATEGORIES_COL_SUBCATEGORY +" FROM " +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	    				CATEGORIES_NAME, null);
	    		Map<String, Map<AmenityType, LinkedHashSet<String>>> map = new LinkedHashMap<String, Map<AmenityType,LinkedHashSet<String>>>();
	    		if(query.moveToFirst()){
	    			do {
	    				String filterId = query.getString(0);
	    				if(!map.containsKey(filterId)){
	    					map.put(filterId, new LinkedHashMap<AmenityType, LinkedHashSet<String>>());
	    				}
	    				Map<AmenityType, LinkedHashSet<String>> m = map.get(filterId);
	    				AmenityType a = AmenityType.fromString(query.getString(1));
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
	    		
	    		query = db.rawQuery("SELECT " + FILTER_COL_ID +", " + FILTER_COL_NAME +"," + FILTER_COL_FILTERBYNAME +" FROM " +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	    				FILTER_NAME, null);
	    		if(query.moveToFirst()){
	    			do {
	    				String filterId = query.getString(0);
	    				if(map.containsKey(filterId)){
	    					PoiFilter filter = new PoiFilter(query.getString(1), filterId, map.get(filterId), application);
	    					filter.setFilterByName(query.getString(2));
	    					list.add(filter);
	    				}
	    			} while(query.moveToNext());
	    		}
	    		query.close();
	    	}
	    	return list;
	    }
	    
	    protected boolean editFilter(SQLiteDatabase db, PoiFilter filter) {
			if (db != null) {
				db.execSQL("DELETE FROM " + CATEGORIES_NAME + " WHERE " + CATEGORIES_FILTER_ID + " = ?",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						new Object[] { filter.getFilterId() });
				addFilter(filter, db, true);
				updateName(db, filter);
				return true;
			}
			return false;
		}

		private void updateName(SQLiteDatabase db, PoiFilter filter) {
			db.execSQL("UPDATE " + FILTER_NAME + " SET " + FILTER_COL_FILTERBYNAME + " = ?, " + FILTER_COL_NAME + " = ? " + " WHERE " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
					+ FILTER_COL_ID + "= ?", new Object[] { filter.getFilterByName(), filter.getName(), filter.getFilterId() }); //$NON-NLS-1$
		}
	    
	    protected boolean deleteFilter(SQLiteDatabase db, PoiFilter p){
	    	if(db != null){
	    		db.execSQL("DELETE FROM " + FILTER_NAME + " WHERE " +FILTER_COL_ID + " = ?",new Object[]{p.getFilterId()}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	    		db.execSQL("DELETE FROM " + CATEGORIES_NAME + " WHERE " +CATEGORIES_FILTER_ID + " = ?", new Object[]{p.getFilterId()}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	    		return true;
	    	}
	    	return false;
	    }
	    


	}

}
