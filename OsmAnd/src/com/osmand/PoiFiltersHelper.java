package com.osmand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.osmand.data.AmenityType;

import android.content.Context;

public class PoiFiltersHelper {

	public static PoiFilter getFilterById(Context ctx, String filterId){
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
	
	public static List<PoiFilter> getUserDefinedDefaultFilters(Context ctx){
		List<PoiFilter> filters = new ArrayList<PoiFilter>();
		Map<AmenityType, List<String>> types = new LinkedHashMap<AmenityType, List<String>>();

		List<String> list = new ArrayList<String>();
		list.add("fuel");
		list.add("car_wash");
		list.add("car_repair");
		types.put(AmenityType.TRANSPORTATION, list);
		
		list = new ArrayList<String>();
		list.add("car");
		types.put(AmenityType.SHOP, list);
		
		filters.add(new PoiFilter("Car aid", null, types));
		types.clear();
		
		
		
		return filters;
	}
	
	private static List<PoiFilter> cacheUserDefinedFilters;
	public static List<PoiFilter> getUserDefinedPoiFilters(Context ctx){
		if(cacheUserDefinedFilters == null){
			cacheUserDefinedFilters = new ArrayList<PoiFilter>();
			
			// TODO
			cacheUserDefinedFilters.addAll(getUserDefinedDefaultFilters(ctx));
		}
		return cacheUserDefinedFilters;
	}
	
	private static List<PoiFilter> cacheOsmDefinedFilters;
	public static List<PoiFilter> getOsmDefinedPoiFilters(Context ctx){
		if(cacheOsmDefinedFilters == null){
			cacheOsmDefinedFilters = new ArrayList<PoiFilter>();
			// for test purposes
			cacheOsmDefinedFilters.addAll(getUserDefinedPoiFilters(ctx));
			cacheOsmDefinedFilters.add(new PoiFilter(null));
			for(AmenityType t : AmenityType.values()){
				cacheOsmDefinedFilters.add(new PoiFilter(t));
			}
		}
		return cacheOsmDefinedFilters;
	}
	
	public static boolean removePoiFilter(Context ctx, PoiFilter filter){
		return false;
	}
	
	public static boolean createPoiFilter(Context ctx, PoiFilter filter){
		return false;
	}
	
	public static boolean commitPoiFilter(Context ctx, PoiFilter filter){
		return false;
	}
	
	
//	protected static class PoiFilterDbHelper extends SQLiteOpenHelper {
//
//	    private static final int DATABASE_VERSION = 1;
//	    private static final String POI_FILTERS_NAME = "poi_filters";
//	    private static final String FAVOURITE_COL_NAME = "name";
//	    private static final String FAVOURITE_COL_LAT = "latitude";
//	    private static final String FAVOURITE_COL_LON = "longitude";
//	    private static final String FAVOURITE_TABLE_CREATE =   "CREATE TABLE " + FAVOURITE_TABLE_NAME + " (" +
//	                FAVOURITE_COL_NAME + " TEXT, " + FAVOURITE_COL_LAT + " double, " +
//	                FAVOURITE_COL_LON + " double);";
//
//	    PoiFilterDbHelper(Context context) {
//	        super(context, POI_FILTERS_NAME, null, DATABASE_VERSION);
//	    }
//	    
//	    public boolean addFavourite(FavouritePoint p){
//	    	SQLiteDatabase db = getWritableDatabase();
//	    	if(db != null){
//	    		db.execSQL("INSERT INTO " + FAVOURITE_TABLE_NAME + " VALUES (?, ?, ?)",new Object[]{p.getName(), p.getLatitude(), p.getLongitude()});
//	    		return true;
//	    	}
//	    	return false;
//	    }
//	    
//	    public List<FavouritePoint> getFavouritePoints(){
//	    	SQLiteDatabase db = getReadableDatabase();
//	    	ArrayList<FavouritePoint> list = new ArrayList<FavouritePoint>();
//	    	if(db != null){
//	    		Cursor query = db.rawQuery("SELECT " + FAVOURITE_COL_NAME +", " + FAVOURITE_COL_LAT +"," + FAVOURITE_COL_LON +" FROM " + 
//	    				FAVOURITE_TABLE_NAME, null);
//	    		if(query.moveToFirst()){
//	    			do {
//	    				FavouritePoint p = new FavouritePoint();
//	    				p.setName(query.getString(0));
//	    				p.setLatitude(query.getDouble(1));
//	    				p.setLongitude(query.getDouble(2));
//	    				list.add(p);
//	    			} while(query.moveToNext());
//	    		}
//	    		query.close();
//	    	}
//	    	return list;
//	    }
//	    
//	    public boolean editFavouriteName(FavouritePoint p, String newName){
//	    	SQLiteDatabase db = getWritableDatabase();
//	    	if(db != null){
//	    		db.execSQL("UPDATE " + FAVOURITE_TABLE_NAME + " SET name = ? WHERE name = ?",new Object[]{newName, p.getName()});
//	    		p.setName(newName);
//	    		return true;
//	    	}
//	    	return false;
//	    }
//	    
//	    public boolean deleteFavourite(FavouritePoint p){
//	    	SQLiteDatabase db = getWritableDatabase();
//	    	if(db != null){
//	    		db.execSQL("DELETE FROM " + FAVOURITE_TABLE_NAME + " WHERE name = ?",new Object[]{p.getName()});
//	    		return true;
//	    	}
//	    	return false;
//	    }
//	    
//
//	    @Override
//	    public void onCreate(SQLiteDatabase db) {
//	        db.execSQL(FAVOURITE_TABLE_CREATE);
//	    }
//
//		@Override
//		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		}
//	}

}
