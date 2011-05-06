package net.osmand.plus;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.FavouritePoint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FavouritesDbHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String FAVOURITE_TABLE_NAME = "favourite"; //$NON-NLS-1$
	private static final String FAVOURITE_COL_NAME = "name"; //$NON-NLS-1$
	private static final String FAVOURITE_COL_LAT = "latitude"; //$NON-NLS-1$
	private static final String FAVOURITE_COL_LON = "longitude"; //$NON-NLS-1$
	private static final String FAVOURITE_TABLE_CREATE = "CREATE TABLE " + FAVOURITE_TABLE_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
			FAVOURITE_COL_NAME + " TEXT, " + FAVOURITE_COL_LAT + " double, " + //$NON-NLS-1$ //$NON-NLS-2$
			FAVOURITE_COL_LON + " double);"; //$NON-NLS-1$

	private List<FavouritePoint> favoritePointsFromGPXFile = null;
	private Map<String, FavouritePoint> favoritePoints = null;

	public FavouritesDbHelper(Context context) {
		super(context, FAVOURITE_TABLE_NAME, null, DATABASE_VERSION);
	}

	public List<FavouritePoint> getFavoritePointsFromGPXFile() {
		return favoritePointsFromGPXFile;
	}

	public void setFavoritePointsFromGPXFile(List<FavouritePoint> favoritePointsFromGPXFile) {
		this.favoritePointsFromGPXFile = favoritePointsFromGPXFile;
	}

	public boolean addFavourite(FavouritePoint p) {
		checkFavoritePoints();
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			// delete with same name before
			deleteFavourite(p);
			db.execSQL("INSERT INTO " + FAVOURITE_TABLE_NAME + " VALUES (?, ?, ?)", new Object[] { p.getName(), p.getLatitude(), p.getLongitude() }); //$NON-NLS-1$ //$NON-NLS-2$
			favoritePoints.put(p.getName(), p);
			p.setStored(true);
			return true;
		}
		return false;
	}
	
	private void checkFavoritePoints(){
		if(favoritePoints == null){
			favoritePoints = new LinkedHashMap<String, FavouritePoint>();
			SQLiteDatabase db = getReadableDatabase();
			if (db != null) {
				Cursor query = db.rawQuery("SELECT " + FAVOURITE_COL_NAME + ", " + FAVOURITE_COL_LAT + "," + FAVOURITE_COL_LON + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
						FAVOURITE_TABLE_NAME, null);
				favoritePoints.clear();
				if (query.moveToFirst()) {
					do {
						FavouritePoint p = new FavouritePoint();
						p.setName(query.getString(0));
						p.setStored(true);
						p.setLatitude(query.getDouble(1));
						p.setLongitude(query.getDouble(2));
						favoritePoints.put(p.getName(), p);
					} while (query.moveToNext());
				}
				query.close();
			}
		}
	}

	public Collection<FavouritePoint> getFavouritePoints() {
		checkFavoritePoints();
		return favoritePoints.values();
	}
	
	public FavouritePoint getFavoritePointByName(String name){
		checkFavoritePoints();
		return favoritePoints.get(name);
	}

	public boolean editFavouriteName(FavouritePoint p, String newName) {
		checkFavoritePoints();
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			db.execSQL("UPDATE " + FAVOURITE_TABLE_NAME + " SET name = ? WHERE name = ?", new Object[] { newName, p.getName() }); //$NON-NLS-1$ //$NON-NLS-2$
			favoritePoints.remove(p.getName());
			p.setName(newName);
			favoritePoints.put(newName, p);
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

	public boolean deleteFavourite(FavouritePoint p) {
		checkFavoritePoints();
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			db.execSQL("DELETE FROM " + FAVOURITE_TABLE_NAME + " WHERE name = ?", new Object[] { p.getName() }); //$NON-NLS-1$ //$NON-NLS-2$
			FavouritePoint fp = favoritePoints.remove(p.getName());
			if(fp != null){
				fp.setStored(false);
			}
			return true;
		}
		return false;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(FAVOURITE_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
}