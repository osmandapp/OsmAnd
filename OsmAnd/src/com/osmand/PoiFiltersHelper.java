package com.osmand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.osmand.data.AmenityType;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PoiFiltersHelper {

	public static PoiFilter getFilterById(Context ctx, String filterId){
		if(filterId == null){
			return null;
		}
		if(filterId.startsWith(PoiFilter.USER_PREFIX)){
			List<PoiFilter> filters = getUserDefinedPoiFilters(ctx);
			for(PoiFilter f : filters){
				if(f.getFilterId().equals(filterId)){
					return f;
				}
			}
		} else if(filterId.startsWith(PoiFilter.STD_PREFIX)){
			List<PoiFilter> filters = getOsmDefinedPoiFilters(ctx);
			for(PoiFilter f : filters){
				if(f.getFilterId().equals(filterId)){
					return f;
				}
			}
		}
		return null;
	}
	
	private static List<PoiFilter> getUserDefinedDefaultFilters(){
		List<PoiFilter> filters = new ArrayList<PoiFilter>();
		Map<AmenityType, List<String>> types = new LinkedHashMap<AmenityType, List<String>>();

		List<String> list = new ArrayList<String>();
		list.add("fuel");
		list.add("car_wash");
		list.add("car_repair");
		types.put(AmenityType.TRANSPORTATION, list);
		list = new ArrayList<String>();
		list.add("car");
		list.add("car_repair");
		types.put(AmenityType.SHOP, list);
		filters.add(new PoiFilter(Messages.getMessage("poi_filter_car_aid"), null, types));
		types.clear();
		
		
		types.put(AmenityType.HISTORIC, null);
		types.put(AmenityType.TOURISM, null);
		list = new ArrayList<String>();
		list.add("place_of_worship");
		list.add("internet_access");
		list.add("bench");
		list.add("embassy");
		list.add("emergency_phone");
		list.add("marketplace");
		list.add("post_office");
		list.add("recycling");
		list.add("telephone");
		list.add("toilets");
		list.add("waste_basket");
		list.add("waste_disposal");
		types.put(AmenityType.OTHER, list);
		filters.add(new PoiFilter(Messages.getMessage("poi_filter_for_tourists"), null, types));
		types.clear();
		
		list = new ArrayList<String>();
		list.add("fuel");
		types.put(AmenityType.TRANSPORTATION, list);
		filters.add(new PoiFilter(Messages.getMessage("poi_filter_fuel"), null, types));
		types.clear();
		
		list = new ArrayList<String>();
		list.add("alcohol");
		list.add("bakery");
		list.add("beverages");
		list.add("butcher");
		list.add("convenience");
		list.add("department_store");
		list.add("convenience");
		list.add("farm");
		list.add("general");
		list.add("ice_cream");
		list.add("kiosk");
		list.add("supermarket");
		list.add("variety_store");
		types.put(AmenityType.SHOP, list);
		filters.add(new PoiFilter(Messages.getMessage("poi_filter_food_shop"), null, types));
		types.clear();
		
		return filters;
	}
	
	private static List<PoiFilter> cacheUserDefinedFilters;
	public static List<PoiFilter> getUserDefinedPoiFilters(Context ctx){
		if(cacheUserDefinedFilters == null){
			////ctx.deleteDatabase(PoiFilterDbHelper.DATABASE_NAME);
			
			cacheUserDefinedFilters = new ArrayList<PoiFilter>();
			PoiFilter filter = new PoiFilter(Messages.getMessage("poi_filter_custom_filter"), PoiFilter.CUSTOM_FILTER_ID, null);
			cacheUserDefinedFilters.add(filter);
			PoiFilterDbHelper helper = new PoiFilterDbHelper(ctx);
			cacheUserDefinedFilters.addAll(helper.getFilters());
			helper.close();
		}
		return Collections.unmodifiableList(cacheUserDefinedFilters);
	}
	
	private static List<PoiFilter> cacheOsmDefinedFilters;
	public static List<PoiFilter> getOsmDefinedPoiFilters(Context ctx){
		if(cacheOsmDefinedFilters == null){
			cacheOsmDefinedFilters = new ArrayList<PoiFilter>();
			cacheOsmDefinedFilters.add(new PoiFilter(null));
			for(AmenityType t : AmenityType.values()){
				cacheOsmDefinedFilters.add(new PoiFilter(t));
			}
		}
		return Collections.unmodifiableList(cacheOsmDefinedFilters);
	}
	
	public static boolean removePoiFilter(Context ctx, PoiFilter filter){
		PoiFilterDbHelper helper = new PoiFilterDbHelper(ctx);
		boolean res = helper.deleteFilter(filter);
		if(res){
			getUserDefinedPoiFilters(ctx).remove(filter);
		}
		helper.close();
		return res;
	}
	
	public static boolean createPoiFilter(Context ctx, PoiFilter filter){
		PoiFilterDbHelper helper = new PoiFilterDbHelper(ctx);
		boolean res = helper.addFilter(filter, helper.getWritableDatabase(), false);
		if(res){
			getUserDefinedPoiFilters(ctx).add(filter);
		}
		helper.close();
		return res;
	}
	
	public static boolean editPoiFilter(Context ctx, PoiFilter filter){
		PoiFilterDbHelper helper = new PoiFilterDbHelper(ctx);
		boolean res = helper.editFilter(filter);
		helper.close();
		return res;
	}
	
	
	protected static class PoiFilterDbHelper extends SQLiteOpenHelper {

		public static final String DATABASE_NAME = "poi_filters";
	    private static final int DATABASE_VERSION = 1;
	    private static final String FILTER_NAME = "poi_filters";
	    private static final String FILTER_COL_NAME = "name";
	    private static final String FILTER_COL_ID = "id";
	    private static final String FILTER_COL_FILTERBYNAME = "filterbyname";
	    private static final String FILTER_TABLE_CREATE =   "CREATE TABLE " + FILTER_NAME + " (" +
	    FILTER_COL_NAME + ", " + FILTER_COL_ID + ", " +  FILTER_COL_FILTERBYNAME + ");";
	    
	    
	    private static final String CATEGORIES_NAME = "categories";
	    private static final String CATEGORIES_FILTER_ID = "filter_id";
	    private static final String CATEGORIES_COL_CATEGORY = "category";
	    private static final String CATEGORIES_COL_SUBCATEGORY = "subcategory";
	    private static final String CATEGORIES_TABLE_CREATE =   "CREATE TABLE " + CATEGORIES_NAME + " (" +
	    CATEGORIES_FILTER_ID + ", " + CATEGORIES_COL_CATEGORY + ", " +  CATEGORIES_COL_SUBCATEGORY + ");";

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
	    
	    public boolean addFilter(PoiFilter p, SQLiteDatabase db, boolean addOnlyCategories){
	    	if(db != null){
	    		if(!addOnlyCategories){
	    			db.execSQL("INSERT INTO " + FILTER_NAME + " VALUES (?, ?, ?)",new Object[]{p.getName(), p.getFilterId(), p.getFilterByName()});
	    		}
	    		Map<AmenityType, List<String>> types = p.getAcceptedTypes();
	    		for(AmenityType a : types.keySet()){
	    			if(types.get(a) == null){
	    				db.execSQL("INSERT INTO " +  CATEGORIES_NAME + " VALUES (?, ?, ?)",
	    						new Object[]{p.getFilterId(), AmenityType.valueToString(a), null});
	    			} else {
	    				for(String s : types.get(a)){
	    					db.execSQL("INSERT INTO " +  CATEGORIES_NAME + " VALUES (?, ?, ?)",
		    						new Object[]{p.getFilterId(), AmenityType.valueToString(a), s});
	    				}
	    			}
	    		}
	    		return true;
	    	}
	    	return false;
	    }
	    
	    public List<PoiFilter> getFilters(){
	    	SQLiteDatabase db = getReadableDatabase();
	    	ArrayList<PoiFilter> list = new ArrayList<PoiFilter>();
	    	if(db != null){
	    		Cursor query = db.rawQuery("SELECT " + CATEGORIES_FILTER_ID +", " + CATEGORIES_COL_CATEGORY +"," + CATEGORIES_COL_SUBCATEGORY +" FROM " + 
	    				CATEGORIES_NAME, null);
	    		Map<String, Map<AmenityType, List<String>>> map = new LinkedHashMap<String, Map<AmenityType,List<String>>>();
	    		if(query.moveToFirst()){
	    			do {
	    				String filterId = query.getString(0);
	    				if(!map.containsKey(filterId)){
	    					map.put(filterId, new LinkedHashMap<AmenityType, List<String>>());
	    				}
	    				Map<AmenityType, List<String>> m = map.get(filterId);
	    				AmenityType a = AmenityType.fromString(query.getString(1));
	    				String subCategory = query.getString(2);
	    				if(subCategory == null){
	    					m.put(a, null);
	    				} else {
	    					if(m.get(a) == null){
	    						m.put(a, new ArrayList<String>());
	    					}
	    					m.get(a).add(subCategory);
	    				}
	    			} while(query.moveToNext());
	    		}
	    		query.close();
	    		
	    		query = db.rawQuery("SELECT " + FILTER_COL_ID +", " + FILTER_COL_NAME +"," + FILTER_COL_FILTERBYNAME +" FROM " + 
	    				FILTER_NAME, null);
	    		if(query.moveToFirst()){
	    			do {
	    				String filterId = query.getString(0);
	    				if(map.containsKey(filterId)){
	    					PoiFilter filter = new PoiFilter(query.getString(1), filterId, map.get(filterId));
	    					filter.setFilterByName(query.getString(2));
	    					list.add(filter);
	    				}
	    			} while(query.moveToNext());
	    		}
	    		query.close();
	    	}
	    	return list;
	    }
	    
	    public boolean editFilter(PoiFilter filter) {
			SQLiteDatabase db = getWritableDatabase();
			if (db != null) {
				db.execSQL("DELETE FROM " + CATEGORIES_NAME + " WHERE " + CATEGORIES_FILTER_ID + " = ?", 
						new Object[] { filter.getFilterId() });
				addFilter(filter, db, true);
				db.execSQL("UPDATE " + FILTER_NAME + " SET " + FILTER_COL_FILTERBYNAME + " = ?, " + FILTER_COL_NAME + " = ? " + " WHERE "
						+ FILTER_COL_ID + "= ?", new Object[] { filter.getFilterByName(), filter.getName(), filter.getFilterId() });
				return true;
			}
			return false;
		}
	    
	    public boolean deleteFilter(PoiFilter p){
	    	SQLiteDatabase db = getWritableDatabase();
	    	if(db != null){
	    		db.execSQL("DELETE FROM " + FILTER_NAME + " WHERE " +FILTER_COL_ID + " = ?",new Object[]{p.getFilterId()});
	    		db.execSQL("DELETE FROM " + CATEGORIES_NAME + " WHERE " +CATEGORIES_FILTER_ID + " = ?", new Object[]{p.getFilterId()});
	    		return true;
	    	}
	    	return false;
	    }
	    


	}

}
