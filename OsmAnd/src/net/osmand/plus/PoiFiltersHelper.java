package net.osmand.plus;


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import net.osmand.data.AmenityType;
import net.osmand.plus.activities.OsmandApplication;
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
	
	private List<PoiFilter> getUserDefinedDefaultFilters(){
		List<PoiFilter> filters = new ArrayList<PoiFilter>();
		Map<AmenityType, LinkedHashSet<String>> types = new LinkedHashMap<AmenityType, LinkedHashSet<String>>();

		LinkedHashSet<String> list = new LinkedHashSet<String>();
		list.add("fuel"); //$NON-NLS-1$
		list.add("car_wash"); //$NON-NLS-1$
		list.add("car_repair"); //$NON-NLS-1$
		types.put(AmenityType.TRANSPORTATION, list);
		list = new LinkedHashSet<String>();
		list.add("car"); //$NON-NLS-1$
		list.add("car_repair"); //$NON-NLS-1$
		types.put(AmenityType.SHOP, list);
		filters.add(new PoiFilter(application.getString(R.string.poi_filter_car_aid), null, types, application)); //$NON-NLS-1$
		types.clear();
		
		
		types.put(AmenityType.HISTORIC, null);
		types.put(AmenityType.TOURISM, null);
		list = new LinkedHashSet<String>();
		list.add("place_of_worship"); //$NON-NLS-1$
		list.add("internet_access"); //$NON-NLS-1$
		list.add("embassy"); //$NON-NLS-1$
		list.add("emergency_phone"); //$NON-NLS-1$
		list.add("marketplace"); //$NON-NLS-1$
		list.add("post_office"); //$NON-NLS-1$
		list.add("telephone"); //$NON-NLS-1$
		list.add("toilets"); //$NON-NLS-1$
		types.put(AmenityType.OTHER, list);
		filters.add(new PoiFilter(application.getString(R.string.poi_filter_for_tourists), null, types, application)); //$NON-NLS-1$
		types.clear();
		
		list = new LinkedHashSet<String>();
		list.add("fuel"); //$NON-NLS-1$
		types.put(AmenityType.TRANSPORTATION, list);
		filters.add(new PoiFilter(application.getString(R.string.poi_filter_fuel), null, types, application)); //$NON-NLS-1$
		types.clear();
		
		list = new LinkedHashSet<String>();
		list.add("alcohol"); //$NON-NLS-1$
		list.add("bakery"); //$NON-NLS-1$
		list.add("beverages"); //$NON-NLS-1$
		list.add("butcher"); //$NON-NLS-1$
		list.add("convenience"); //$NON-NLS-1$
		list.add("department_store"); //$NON-NLS-1$
		list.add("convenience"); //$NON-NLS-1$
		list.add("farm"); //$NON-NLS-1$
		list.add("general"); //$NON-NLS-1$
		list.add("ice_cream"); //$NON-NLS-1$
		list.add("kiosk"); //$NON-NLS-1$
		list.add("supermarket"); //$NON-NLS-1$
		list.add("variety_store"); //$NON-NLS-1$
		types.put(AmenityType.SHOP, list);
		filters.add(new PoiFilter(application.getString(R.string.poi_filter_food_shop), null, types, application)); //$NON-NLS-1$
		types.clear();
		
		return filters;
	}
	
	public List<PoiFilter> getUserDefinedPoiFilters(){
		if(cacheUserDefinedFilters == null){
			////ctx.deleteDatabase(PoiFilterDbHelper.DATABASE_NAME);
			
			cacheUserDefinedFilters = new ArrayList<PoiFilter>();
			PoiFilter filter = new PoiFilter(application.getString(R.string.poi_filter_custom_filter), PoiFilter.CUSTOM_FILTER_ID, new LinkedHashMap<AmenityType, LinkedHashSet<String>>(), application); //$NON-NLS-1$
			cacheUserDefinedFilters.add(filter);
			filter = new PoiFilter(application.getString(R.string.poi_filter_by_name), PoiFilter.BY_NAME_FILTER_ID, new LinkedHashMap<AmenityType, LinkedHashSet<String>>(), application); //$NON-NLS-1$
			cacheUserDefinedFilters.add(filter);
			PoiFilterDbHelper helper = openDbHelper();
			cacheUserDefinedFilters.addAll(helper.getFilters());
			helper.close();
		}
		return Collections.unmodifiableList(cacheUserDefinedFilters);
	}
	
	public static String getOsmDefinedFilterId(AmenityType t){
		return PoiFilter.STD_PREFIX + t;
	}
	
	
	public List<PoiFilter> getOsmDefinedPoiFilters(){
		if(cacheOsmDefinedFilters == null){
			cacheOsmDefinedFilters = new ArrayList<PoiFilter>();
			cacheOsmDefinedFilters.add(new PoiFilter(null, application));
			for(AmenityType t : AmenityType.values()){
				cacheOsmDefinedFilters.add(new PoiFilter(t, application));
			}
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
		boolean res = helper.deleteFilter(filter);
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
			boolean res = helper.editFilter(filter);
			helper.close();
			return res;
		}
		return false;
	}
	
	
	public class PoiFilterDbHelper extends SQLiteOpenHelper {

		public static final String DATABASE_NAME = "poi_filters"; //$NON-NLS-1$
	    private static final int DATABASE_VERSION = 1;
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
	        List<PoiFilter> filters = getUserDefinedDefaultFilters();
	        for(PoiFilter f : filters){
	        	addFilter(f, db,false);
	        }
	    }

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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
	    
	    protected List<PoiFilter> getFilters(){
	    	SQLiteDatabase db = getReadableDatabase();
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
	    
	    protected boolean editFilter(PoiFilter filter) {
			SQLiteDatabase db = getWritableDatabase();
			if (db != null) {
				db.execSQL("DELETE FROM " + CATEGORIES_NAME + " WHERE " + CATEGORIES_FILTER_ID + " = ?",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						new Object[] { filter.getFilterId() });
				addFilter(filter, db, true);
				db.execSQL("UPDATE " + FILTER_NAME + " SET " + FILTER_COL_FILTERBYNAME + " = ?, " + FILTER_COL_NAME + " = ? " + " WHERE " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
						+ FILTER_COL_ID + "= ?", new Object[] { filter.getFilterByName(), filter.getName(), filter.getFilterId() }); //$NON-NLS-1$
				return true;
			}
			return false;
		}
	    
	    protected boolean deleteFilter(PoiFilter p){
	    	SQLiteDatabase db = getWritableDatabase();
	    	if(db != null){
	    		db.execSQL("DELETE FROM " + FILTER_NAME + " WHERE " +FILTER_COL_ID + " = ?",new Object[]{p.getFilterId()}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	    		db.execSQL("DELETE FROM " + CATEGORIES_NAME + " WHERE " +CATEGORIES_FILTER_ID + " = ?", new Object[]{p.getFilterId()}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	    		return true;
	    	}
	    	return false;
	    }
	    


	}

}
