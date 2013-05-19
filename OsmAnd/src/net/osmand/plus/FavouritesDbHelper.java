package net.osmand.plus;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;

public class FavouritesDbHelper {

	private static final int DATABASE_VERSION = 2;
	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(FavouritesDbHelper.class);
	public static final String FAVOURITE_DB_NAME = "favourite"; //$NON-NLS-1$
	private static final String FAVOURITE_TABLE_NAME = "favourite"; //$NON-NLS-1$
	private static final String FAVOURITE_COL_NAME = "name"; //$NON-NLS-1$
	private static final String FAVOURITE_COL_CATEGORY = "category"; //$NON-NLS-1$
	private static final String FAVOURITE_COL_LAT = "latitude"; //$NON-NLS-1$
	private static final String FAVOURITE_COL_LON = "longitude"; //$NON-NLS-1$
	private static final String FAVOURITE_TABLE_CREATE = "CREATE TABLE " + FAVOURITE_TABLE_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
			FAVOURITE_COL_NAME + " TEXT, " + FAVOURITE_COL_CATEGORY + " TEXT, " + //$NON-NLS-1$ //$NON-NLS-2$ 
			FAVOURITE_COL_LAT + " double, " + FAVOURITE_COL_LON + " double);"; //$NON-NLS-1$ //$NON-NLS-2$
	
	public static final String FILE_TO_SAVE = "favourites.gpx"; //$NON-NLS-1$
	public static final String FILE_TO_BACKUP = "favourites_bak.gpx"; //$NON-NLS-1$
	
	// externalize ?
	private static final String GPX_GROUP = "Gpx"; 

	private List<FavouritePoint> favoritePointsFromGPXFile = null;
	private List<FavouritePoint> cachedFavoritePoints = new ArrayList<FavouritePoint>();
	private Map<String, List<FavouritePoint>> favoriteGroups = null;
	private final ClientContext context;
	private SQLiteConnection conn;

	public FavouritesDbHelper(ClientContext context) {
		this.context = context;
	}
	
	private SQLiteConnection openConnection(boolean readonly) {
		conn = context.getSQLiteAPI().getOrCreateDatabase(FAVOURITE_DB_NAME, readonly);
		if (conn.getVersion() == 0 || DATABASE_VERSION != conn.getVersion()) {
			if (readonly) {
				conn.close();
				conn = context.getSQLiteAPI().getOrCreateDatabase(FAVOURITE_DB_NAME, readonly);
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
	
	public void onCreate(SQLiteConnection db) {
		db.execSQL(FAVOURITE_TABLE_CREATE);
		createCategories(db);
	}

	public void onUpgrade(SQLiteConnection db, int oldVersion, int newVersion) {
		if(oldVersion == 1){
			db.execSQL("ALTER TABLE " + FAVOURITE_TABLE_NAME +  " ADD " + FAVOURITE_COL_CATEGORY + " text");
			createCategories(db);
			db.execSQL("UPDATE " + FAVOURITE_TABLE_NAME + " SET category = ?", new Object[] { context.getString(R.string.favorite_default_category)}); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public void backupSilently() {
		try {
			exportFavorites(FILE_TO_BACKUP);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public String exportFavorites() {
		return exportFavorites(FILE_TO_SAVE);
	}
	
	public String exportFavorites(String fileName) {
		File f = new File(context.getAppPath(null), fileName);
		GPXFile gpx = new GPXFile();
		for (FavouritePoint p : getFavouritePoints()) {
			if (p.isStored()) {
				WptPt pt = new WptPt();
				pt.lat = p.getLatitude();
				pt.lon = p.getLongitude();
				pt.name = p.getName();
				if (p.getCategory().length() > 0)
					pt.category = p.getCategory();
				gpx.points.add(pt);
			}
		}
		return GPXUtilities.writeGpxFile(f, gpx, context);
	}
	
	private void createCategories(SQLiteConnection db){
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
	
	public void addFavoritePointToGPXFile(FavouritePoint fp) {
		fp.setCategory(GPX_GROUP);
		fp.setStored(false);
		if (!favoriteGroups.containsKey(GPX_GROUP)) {
			favoriteGroups.put(GPX_GROUP, new ArrayList<FavouritePoint>());
		}
		favoriteGroups.get(GPX_GROUP).add(fp);
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
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				String oldCategory = p.getCategory();
				db.execSQL(
						"UPDATE " + FAVOURITE_TABLE_NAME + " SET " + FAVOURITE_COL_NAME + " = ?, " + FAVOURITE_COL_CATEGORY + "= ? WHERE " + whereNameLatLon(), new Object[] { newName, category, p.getName(), p.getLatitude(), p.getLongitude() }); //$NON-NLS-1$ //$NON-NLS-2$
				p.setName(newName);
				p.setCategory(category);
				if (!oldCategory.equals(category)) {
					favoriteGroups.get(oldCategory).remove(p);
					if (!favoriteGroups.containsKey(category)) {
						addCategoryQuery(category, db);
						favoriteGroups.put(category, new ArrayList<FavouritePoint>());
					}
					favoriteGroups.get(category).add(p);
				}
			}finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	private String whereNameLatLon() {
		String singleFavourite = " " + FAVOURITE_COL_NAME + "= ? AND " + FAVOURITE_COL_LAT + " = ? AND " + FAVOURITE_COL_LON + " = ?";
		return singleFavourite;
	}

	public boolean editFavourite(FavouritePoint p, double lat, double lon) {
		checkFavoritePoints();
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL(
						"UPDATE " + FAVOURITE_TABLE_NAME + " SET latitude = ?, longitude = ? WHERE " + whereNameLatLon(), new Object[] { lat, lon, p.getName(), p.getLatitude(), p.getLongitude() }); //$NON-NLS-1$ //$NON-NLS-2$ 
				p.setLatitude(lat);
				p.setLongitude(lon);
				backupSilently();
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}
	
	private FavouritePoint findFavoriteByAllProperties(String category, String name, double lat, double lon){
		if (favoriteGroups.containsKey(category)) {
			for (FavouritePoint fv : favoriteGroups.get(category)) {
				if (name.equals(fv.getName()) && (lat == fv.getLatitude()) && (lon == fv.getLongitude())) {
					return fv;
				}
			}
		}
		return null;
	}

	public boolean deleteFavourite(FavouritePoint p) {
		checkFavoritePoints();
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL(
						"DELETE FROM " + FAVOURITE_TABLE_NAME + " WHERE category = ? AND " + whereNameLatLon(), new Object[] { p.getCategory(), p.getName(), p.getLatitude(), p.getLongitude() }); //$NON-NLS-1$ //$NON-NLS-2$
				FavouritePoint fp = findFavoriteByAllProperties(p.getCategory(), p.getName(), p.getLatitude(), p.getLongitude());
				if (fp != null) {
					favoriteGroups.get(p.getCategory()).remove(fp);
					cachedFavoritePoints.remove(fp);
					fp.setStored(false);
				}
				backupSilently();
			} finally{
				db.close();
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
			backupSilently();
		}
		return false;
	}

	public boolean addFavourite(FavouritePoint p) {
		checkFavoritePoints();
		if(p.getName().equals("") && favoriteGroups.containsKey(p.getCategory())){
			return true;
		}
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL(
						"INSERT INTO " + FAVOURITE_TABLE_NAME + " (" + FAVOURITE_COL_NAME + ", " + FAVOURITE_COL_CATEGORY + ", "
								+ FAVOURITE_COL_LAT + ", " + FAVOURITE_COL_LON + ")" + " VALUES (?, ?, ?, ?)", new Object[] { p.getName(), p.getCategory(), p.getLatitude(), p.getLongitude() }); //$NON-NLS-1$ //$NON-NLS-2$
				if (!favoriteGroups.containsKey(p.getCategory())) {
					favoriteGroups.put(p.getCategory(), new ArrayList<FavouritePoint>());
					if (!p.getName().equals("")) {
						addFavourite(new FavouritePoint(0, 0, "", p.getCategory()));
					}
				}
				if (!p.getName().equals("")) {
					favoriteGroups.get(p.getCategory()).add(p);
					cachedFavoritePoints.add(p);
				}
				p.setStored(true);
				backupSilently();
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}
	
	private void addCategoryQuery(String category, SQLiteConnection db) {
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
			favoriteGroups = new TreeMap<String, List<FavouritePoint>>(Collator.getInstance());
			SQLiteConnection db = openConnection(true);
			if (db != null) {
				try {
				SQLiteCursor query = db.rawQuery("SELECT " + FAVOURITE_COL_NAME + ", " + FAVOURITE_COL_CATEGORY + ", " + FAVOURITE_COL_LAT + "," + FAVOURITE_COL_LON + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
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
				} finally {
					db.close();
				}
			}
			recalculateCachedFavPoints();
		}
	}

	


}
