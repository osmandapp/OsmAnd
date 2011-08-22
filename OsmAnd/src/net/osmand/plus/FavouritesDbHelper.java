package net.osmand.plus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.FavouritePoint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FavouritesDbHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 2;
	public static final String FAVOURITE_DB_NAME = "favourite"; //$NON-NLS-1$
	private static final String FAVOURITE_TABLE_NAME = "favourite"; //$NON-NLS-1$
	private static final String FAVOURITE_COL_NAME = "name"; //$NON-NLS-1$
	private static final String FAVOURITE_COL_CATEGORY = "category"; //$NON-NLS-1$
	private static final String FAVOURITE_COL_LAT = "latitude"; //$NON-NLS-1$
	private static final String FAVOURITE_COL_LON = "longitude"; //$NON-NLS-1$
	private static final String FAVOURITE_TABLE_CREATE = "CREATE TABLE " + FAVOURITE_TABLE_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
			FAVOURITE_COL_NAME + " TEXT, " + FAVOURITE_COL_CATEGORY + " TEXT, " + //$NON-NLS-1$ //$NON-NLS-2$ 
			FAVOURITE_COL_LAT + " double, " + FAVOURITE_COL_LON + " double);"; //$NON-NLS-1$ //$NON-NLS-2$
	
	// externalize ?
	private static final String GPX_GROUP = "Gpx"; 

	private List<FavouritePoint> favoritePointsFromGPXFile = null;
	private List<FavouritePoint> cachedFavoritePoints = new ArrayList<FavouritePoint>();
	private Map<String, List<FavouritePoint>> favoriteGroups = null;
	private final Context context;

	public FavouritesDbHelper(Context context) {
		super(context, FAVOURITE_DB_NAME, null, DATABASE_VERSION);
		this.context = context;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(FAVOURITE_TABLE_CREATE);
		createCategories(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion == 1){
			db.execSQL("ALTER TABLE " + FAVOURITE_TABLE_NAME +  " ADD " + FAVOURITE_COL_CATEGORY + " text");
			createCategories(db);
			db.execSQL("UPDATE " + FAVOURITE_TABLE_NAME + " SET category = ?", new Object[] { context.getString(R.string.favorite_default_category)}); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	private void createCategories(SQLiteDatabase db){
		addCategoryQuery(context.getString(R.string.favorite_home_category), db);
		addCategoryQuery(context.getString(R.string.favorite_friends_category), db);
		addCategoryQuery(context.getString(R.string.favorite_places_category), db);
		addCategoryQuery(context.getString(R.string.favorite_default_category), db);
	}

	public List<FavouritePoint> getFavoritePointsFromGPXFile() {
		return favoritePointsFromGPXFile;
	}

	public void setFavoritePointsFromGPXFile(List<FavouritePoint> favoritePointsFromGPXFile) {
		this.favoritePointsFromGPXFile = favoritePointsFromGPXFile;
		if(favoritePointsFromGPXFile == null){
			favoriteGroups.remove(GPX_GROUP);
		} else {
			checkFavoritePoints();
			for(FavouritePoint t : favoritePointsFromGPXFile){
				t.setCategory(GPX_GROUP);
				t.setStored(false);	
			}
			favoriteGroups.put(GPX_GROUP, favoritePointsFromGPXFile);
		}
		recalculateCachedFavPoints();
	}
	
	public List<FavouritePoint> getFavouritePoints() {
		checkFavoritePoints();
		return cachedFavoritePoints;
	}
	
	public Map<String, List<FavouritePoint>> getFavoriteGroups() {
		checkFavoritePoints();
		return favoriteGroups;
	}
	

	public boolean editFavouriteName(FavouritePoint p, String newName, String category) {
		checkFavoritePoints();
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			String oldCategory = p.getCategory();
			db.execSQL("UPDATE " + FAVOURITE_TABLE_NAME + " SET name = ?, category = ? WHERE name = ?", new Object[] { newName, category, p.getName() }); //$NON-NLS-1$ //$NON-NLS-2$
			p.setName(newName);
			p.setCategory(category);
			if(!oldCategory.equals(category)){
				favoriteGroups.get(oldCategory).remove(p);
				if(!favoriteGroups.containsKey(category)){
					addCategoryQuery(category, db);
					favoriteGroups.put(category, new ArrayList<FavouritePoint>());
				}
				favoriteGroups.get(category).add(p);
			}
			return true;
		}
		return false;
	}

	public boolean editFavourite(FavouritePoint p, double lat, double lon) {
		checkFavoritePoints();
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			db.execSQL("UPDATE " + FAVOURITE_TABLE_NAME + " SET latitude = ?, longitude = ? WHERE name = ?", new Object[] { lat, lon, p.getName() }); //$NON-NLS-1$ //$NON-NLS-2$ 
			p.setLatitude(lat);
			p.setLongitude(lon);
			return true;
		}
		return false;
	}
	
	private FavouritePoint findFavoriteByName(String name, String category){
		if (favoriteGroups.containsKey(category)) {
			for (FavouritePoint fv : favoriteGroups.get(category)) {
				if (name.equals(fv.getName())) {
					return fv;
				}
			}
		}
		return null;
	}

	public boolean deleteFavourite(FavouritePoint p) {
		checkFavoritePoints();
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			db.execSQL("DELETE FROM " + FAVOURITE_TABLE_NAME + " WHERE name = ? AND category = ? ", new Object[] { p.getName(), p.getCategory()}); //$NON-NLS-1$ //$NON-NLS-2$
			FavouritePoint fp = findFavoriteByName(p.getName(), p.getCategory());
			if(fp != null){
				favoriteGroups.get(p.getCategory()).remove(fp);
				cachedFavoritePoints.remove(fp);
				fp.setStored(false);
			}
			return true;
		}
		return false;
	}
	
	public boolean deleteGroup(String group){
		checkFavoritePoints();
		FavouritePoint fp = new FavouritePoint(0, 0, "", group);
		if(deleteFavourite(fp)){
			favoriteGroups.remove(group);
		}
		return false;
	}

	public boolean addFavourite(FavouritePoint p) {
		checkFavoritePoints();
		if(p.getName().equals("") && favoriteGroups.containsKey(p.getCategory())){
			return true;
		}
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			// delete with same name before
			deleteFavourite(p);
			db.execSQL("INSERT INTO " + FAVOURITE_TABLE_NAME +
					" (" +FAVOURITE_COL_NAME +", " +FAVOURITE_COL_CATEGORY +", " +FAVOURITE_COL_LAT +", " +FAVOURITE_COL_LON + ")" +
					" VALUES (?, ?, ?, ?)", new Object[] { p.getName(), p.getCategory(), p.getLatitude(), p.getLongitude() }); //$NON-NLS-1$ //$NON-NLS-2$
			if(!favoriteGroups.containsKey(p.getCategory())){
				if (!p.getName().equals("")) {
					addFavourite(new FavouritePoint(0, 0, "", p.getCategory()));
				}
				favoriteGroups.put(p.getCategory(), new ArrayList<FavouritePoint>());
			}
			if(!p.getName().equals("")){
				favoriteGroups.get(p.getCategory()).add(p);
				cachedFavoritePoints.add(p);
			}
			p.setStored(true);
			return true;
		}
		return false;
	}
	
	private void addCategoryQuery(String category, SQLiteDatabase db) {
		db.execSQL("INSERT INTO " + FAVOURITE_TABLE_NAME +
				" (" +FAVOURITE_COL_NAME +", " +FAVOURITE_COL_CATEGORY +", " +FAVOURITE_COL_LAT +", " +FAVOURITE_COL_LON + ")" +
				" VALUES (?, ?, ?, ?)", new Object[] { "", category, 0f, 0f }); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	
	private void recalculateCachedFavPoints(){
		ArrayList<FavouritePoint> temp = new ArrayList<FavouritePoint>();
		for(List<FavouritePoint> f : favoriteGroups.values()){
			temp.addAll(f);
		}
		cachedFavoritePoints = temp;
	}
	
	private void checkFavoritePoints(){
		if(favoriteGroups == null){
			favoriteGroups = new LinkedHashMap<String, List<FavouritePoint>>();
			SQLiteDatabase db = getWritableDatabase();
			if (db != null) {
				Cursor query = db.rawQuery("SELECT " + FAVOURITE_COL_NAME + ", " + FAVOURITE_COL_CATEGORY + ", " + FAVOURITE_COL_LAT + "," + FAVOURITE_COL_LON + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
						FAVOURITE_TABLE_NAME, null);
				cachedFavoritePoints.clear();
				if (query.moveToFirst()) {
					do {
						String name = query.getString(0);
						String cat = query.getString(1);
						if(!favoriteGroups.containsKey(cat)){
							favoriteGroups.put(cat, new ArrayList<FavouritePoint>());
						}
						if (!name.equals("")) {
							FavouritePoint p = new FavouritePoint();
							p.setName(name);
							p.setCategory(cat);
							p.setStored(true);
							p.setLatitude(query.getDouble(2));
							p.setLongitude(query.getDouble(3));
							favoriteGroups.get(cat).add(p);
						}
					} while (query.moveToNext());
				}
				query.close();
			}
			recalculateCachedFavPoints();
		}
	}

	


}