package net.osmand.plus;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import android.app.AlertDialog;
import android.content.Context;

public class FavouritesDbHelper {

	public interface FavoritesUpdatedListener {
		void updateFavourites();
	}


	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(FavouritesDbHelper.class);
	
	public static final String FILE_TO_SAVE = "favourites.gpx"; //$NON-NLS-1$
	public static final String FILE_TO_BACKUP = "favourites_bak.gpx"; //$NON-NLS-1$

	private List<FavouritePoint> cachedFavoritePoints = new ArrayList<FavouritePoint>();
	private List<FavoriteGroup> favoriteGroups = new ArrayList<FavouritesDbHelper.FavoriteGroup>();
	private Map<String, FavoriteGroup> flatGroups = new LinkedHashMap<String, FavouritesDbHelper.FavoriteGroup>();
	private final OsmandApplication context;
	protected static final String HIDDEN = "HIDDEN";
	private static final String DELIMETER = "__";
	

	public FavouritesDbHelper(OsmandApplication context) {
		this.context = context;
	}
	
	public static class FavoriteGroup {
		public String name;
		public boolean visible = true;
		public int color;
		public List<FavouritePoint> points = new ArrayList<FavouritePoint>();
	}
	
	public void loadFavorites() {
		flatGroups.clear();
		favoriteGroups.clear();
		
		File internalFile = getInternalFile();
		if(!internalFile.exists()) {
			File dbPath = context.getDatabasePath(FAVOURITE_DB_NAME);
			if(dbPath.exists()) {
				loadAndCheckDatabasePoints();
				saveCurrentPointsIntoFile();
			}
			//createDefaultCategories();
		}
		Map<String, FavouritePoint> points = new LinkedHashMap<String, FavouritePoint>();
		Map<String, FavouritePoint> extPoints = new LinkedHashMap<String, FavouritePoint>();
		loadGPXFile(internalFile, points);
		loadGPXFile(getExternalFile(), extPoints);
		boolean changed = merge(extPoints, points);
		
		for(FavouritePoint pns : points.values()) {
			FavoriteGroup group = getOrCreateGroup(pns, 0);
			group.points.add(pns);
		}
		sortAll();
		recalculateCachedFavPoints();
		if(changed) {
			saveCurrentPointsIntoFile();
		}
		favouritesUpdated();
		
	}

	private void favouritesUpdated(){
	}


	private boolean merge(Map<String, FavouritePoint> source, Map<String, FavouritePoint> destination) {
		boolean changed = false;
		for(String ks : source.keySet()) {
			if(!destination.containsKey(ks)) {
				changed = true;
				destination.put(ks, source.get(ks));
			}
		}
		return changed;
	}



	private File getInternalFile() {
		return context.getFileStreamPath(FILE_TO_BACKUP);
	}
	
	public void delete(Set<FavoriteGroup> groupsToDelete, Set<FavouritePoint> favoritesSelected) {
		if (favoritesSelected != null) {
			for (FavouritePoint p : favoritesSelected) {
				FavoriteGroup group = flatGroups.get(p.getCategory());
				if (group != null) {
					group.points.remove(p);
				}
				cachedFavoritePoints.remove(p);
			}
		}
		if (groupsToDelete != null) {
			for (FavoriteGroup g : groupsToDelete) {
				flatGroups.remove(g.name);
				favoriteGroups.remove(g);
				cachedFavoritePoints.removeAll(g.points);
			}
		}
		saveCurrentPointsIntoFile();
	}
	
	public boolean deleteFavourite(FavouritePoint p) {
		return deleteFavourite(p, true);
	}

	public boolean deleteFavourite(FavouritePoint p, boolean saveImmediately) {
		if (p != null) {
			FavoriteGroup group = flatGroups.get(p.getCategory());
			if (group != null) {
				group.points.remove(p);
			}
			cachedFavoritePoints.remove(p);
		}
		if (saveImmediately) {
			saveCurrentPointsIntoFile();
		}
		return true;
	}
	
	public boolean addFavourite(FavouritePoint p) {
		return addFavourite(p, true);
	}

	public boolean addFavourite(FavouritePoint p, boolean saveImmediately) {
		if (p.getName().equals("") && flatGroups.containsKey(p.getCategory())) {
			return true;
		}
		FavoriteGroup group = getOrCreateGroup(p, 0);

		if (!p.getName().equals("")) {
			p.setVisible(group.visible);
			p.setColor(group.color);
			group.points.add(p);
			cachedFavoritePoints.add(p);
		}
		if (saveImmediately) {
			saveCurrentPointsIntoFile();
			sortAll();
		}

		return true;
	}
	
	public static AlertDialog.Builder checkDublicates(FavouritePoint p, FavouritesDbHelper fdb, Context uiContext) {
		boolean emoticons = false;
		String index = "";
		int number = 0;
		String name = checkEmoticons(p.getName());
		String category = checkEmoticons(p.getCategory());
		p.setCategory(category);
		String description = checkEmoticons(p.getDescription());
		p.setDescription(description);
		if (name.length() != p.getName().length()) {
			emoticons = true;
		}
		boolean fl = true;
		while (fl) {
			fl = false;
			for (FavouritePoint fp : fdb.getFavouritePoints()) {
				if (fp.getName().equals(name)) {
					number++;
					index = " (" + number + ")";
					name = p.getName() + index;
					fl = true;
					break;
				}
			}
		}
		if ((index.length() > 0 || emoticons) ) {
			AlertDialog.Builder builder = new AlertDialog.Builder(uiContext);
			builder.setTitle(R.string.fav_point_dublicate);
			if (emoticons) {
				builder.setMessage(uiContext.getString(R.string.fav_point_emoticons_message, name));
			} else {
				builder.setMessage(uiContext.getString(R.string.fav_point_dublicate_message, name));
			}
			p.setName(name);
			return builder;
		}
		return null;
	}

	public static String checkEmoticons(String name){
		char[] chars = name.toCharArray();
		int index;
		char ch1;
		char ch2;

		index = 0;
		StringBuilder builder = new StringBuilder();
		while (index < chars.length) {
			ch1 = chars[index];
			if ((int)ch1 == 0xD83C) {
				ch2 = chars[index+1];
				if ((int)ch2 >= 0xDF00 && (int)ch2 <= 0xDFFF) {
					index += 2;
					continue;
				}
			}
			else if ((int)ch1 == 0xD83D) {
				ch2 = chars[index+1];
				if ((int)ch2 >= 0xDC00 && (int)ch2 <= 0xDDFF) {
					index += 2;
					continue;
				}
			}
			builder.append(ch1);
			++index;
		}

		builder.trimToSize(); // remove trailing null characters
		return builder.toString();
	}

	public boolean editFavouriteName(FavouritePoint p, String newName, String category, String descr) {
		String oldCategory = p.getCategory();
		p.setName(newName);
		p.setCategory(category);
		p.setDescription(descr);
		if (!oldCategory.equals(category)) {
			FavoriteGroup old = flatGroups.get(oldCategory);
			if (old != null) {
				old.points.remove(p);
			}
			FavoriteGroup pg = getOrCreateGroup(p, 0);
			p.setVisible(pg.visible);
			p.setColor(pg.color);
			pg.points.add(p);
		}
		sortAll();
		saveCurrentPointsIntoFile();
		return true;
	}
	

	public boolean editFavourite(FavouritePoint p, double lat, double lon) {
		p.setLatitude(lat);
		p.setLongitude(lon);
		saveCurrentPointsIntoFile();
		return true;
	}
	
	public void saveCurrentPointsIntoFile() {
		try {
			Map<String, FavouritePoint> ex = new LinkedHashMap<String, FavouritePoint>();
			loadGPXFile(getInternalFile(), ex);
			for(FavouritePoint fp : cachedFavoritePoints) {
				ex.remove(getKey(fp));
			}
			saveFile(cachedFavoritePoints, getInternalFile());
			saveExternalFile(ex.keySet());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public String exportFavorites() {
		return saveExternalFile(null);
	}



	private String saveExternalFile(Set<String> deleted) {
		Map<String, FavouritePoint> ex = new LinkedHashMap<String, FavouritePoint>();
		loadGPXFile(getExternalFile(), ex);
		List<FavouritePoint> favoritePoints = new ArrayList<FavouritePoint>(cachedFavoritePoints);
		if(deleted != null) {
			for(String key : deleted) {
				ex.remove(key);
			}
		}
		for(FavouritePoint p : favoritePoints) {
			ex.remove(getKey(p));
		}
		favoritePoints.addAll(ex.values());
		return saveFile(favoritePoints, getExternalFile());
	}



	private String getKey(FavouritePoint p) {
		return p.getName() + DELIMETER + p.getCategory();
	}


	
	public boolean deleteGroup(FavoriteGroup group) {
		boolean remove = favoriteGroups.remove(group);
		if (remove) {
			flatGroups.remove(group.name);
			saveCurrentPointsIntoFile();
			return true;
		}
		return false;
	}

	public File getExternalFile() {
		return new File(context.getAppPath(null), FILE_TO_SAVE);
	}
	
	public String saveFile(List<FavouritePoint> favoritePoints, File f) {
		GPXFile gpx = asGpxFile(favoritePoints);
		return GPXUtilities.writeGpxFile(f, gpx, context);
	}

	
	public GPXFile asGpxFile() {
		return asGpxFile(cachedFavoritePoints);
	}
	
	private GPXFile asGpxFile(List<FavouritePoint> favoritePoints) {
		GPXFile gpx = new GPXFile();
		for (FavouritePoint p : favoritePoints) {
			WptPt pt = new WptPt();
			pt.lat = p.getLatitude();
			pt.lon = p.getLongitude();
			if(!p.isVisible()) {
				pt.getExtensionsToWrite().put(HIDDEN, "true");
			}
			if(p.getColor() != 0) {
				pt.setColor(p.getColor());
			}
			pt.name = p.getName();
			pt.desc = p.getDescription();
			if (p.getCategory().length() > 0)
				pt.category = p.getCategory();
			gpx.points.add(pt);
		}
		return gpx;
	}

	
	private void addEmptyCategory(String name) {
		FavoriteGroup group = new FavoriteGroup();
		group.name = name;
		favoriteGroups.add(group);
		flatGroups.put(name, group);
	}

	public List<FavouritePoint> getFavouritePoints() {
		return cachedFavoritePoints;
	}
	

	public List<FavoriteGroup> getFavoriteGroups() {
		return favoriteGroups;
	}
	

	private FavouritePoint findFavoriteByAllProperties(String category, String name, double lat, double lon){
		if (flatGroups.containsKey(category)) {
			FavoriteGroup fg = flatGroups.get(category);
			for (FavouritePoint fv : fg.points) {
				if (name.equals(fv.getName()) && (lat == fv.getLatitude()) && (lon == fv.getLongitude())) {
					return fv;
				}
			}
		}
		return null;
	}

	
	
	private void recalculateCachedFavPoints(){
		ArrayList<FavouritePoint> temp = new ArrayList<FavouritePoint>();
		for(FavoriteGroup f : favoriteGroups){
			temp.addAll(f.points);
		}
		cachedFavoritePoints = temp;
	}
	
	private void sortAll() {
		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		Collections.sort(favoriteGroups, new Comparator<FavoriteGroup>() {

			@Override
			public int compare(FavoriteGroup lhs, FavoriteGroup rhs) {
				return collator.compare(lhs.name, rhs.name);
			}
		});
		Comparator<FavouritePoint> favoritesComparator = new Comparator<FavouritePoint>() {

			@Override
			public int compare(FavouritePoint object1, FavouritePoint object2) {
				return collator.compare(object1.getName(), object2.getName());
			}
		};
		for(FavoriteGroup g : favoriteGroups) {
			Collections.sort(g.points, favoritesComparator);
		}
		if(cachedFavoritePoints != null) {
			Collections.sort(cachedFavoritePoints, favoritesComparator);
		}
	}
	

	private String loadGPXFile(File file, Map<String, FavouritePoint> points) {
		GPXFile res = GPXUtilities.loadGPXFile(context, file);
		if (res.warning != null) {
			return res.warning;
		}
		for (WptPt p : res.points) {
			int c;
			String name = p.name;
			String categoryName = p.category != null ? p.category : "";
			if (name == null) {
				name = "";
			}
			// old way to store the category, in name.
			if ("".equals(categoryName.trim()) && (c = p.name.lastIndexOf('_')) != -1) {
				categoryName = p.name.substring(c + 1);
				name = p.name.substring(0, c);
			}
			FavouritePoint fp = new FavouritePoint(p.lat, p.lon, name, categoryName);
			fp.setDescription(p.desc);
			fp.setColor(p.getColor(0));
			fp.setVisible(!p.getExtensionsToRead().containsKey(HIDDEN));
			points.put(getKey(fp), fp);
		}
		return null;
	}
	
	public void editFavouriteGroup(FavoriteGroup group, int color, boolean visible) {
		if(color != 0 && group.color != color) {
			FavoriteGroup gr = flatGroups.get(group.name);
			group.color = color;
			for(FavouritePoint p : gr.points) {
				p.setColor(color);
			}	
		}
		if(group.visible != visible) {
			FavoriteGroup gr = flatGroups.get(group.name);
			group.visible = visible;
			for(FavouritePoint p : gr.points) {
				p.setVisible(visible);
			}	
		}
		saveCurrentPointsIntoFile();
	}

	protected void createDefaultCategories() {
		addEmptyCategory(context.getString(R.string.favorite_home_category));
		addEmptyCategory(context.getString(R.string.favorite_friends_category));
		addEmptyCategory(context.getString(R.string.favorite_places_category));
		addEmptyCategory(context.getString(R.string.favorite_default_category));
	}

	private FavoriteGroup getOrCreateGroup(FavouritePoint p, int defColor) {
		if (flatGroups.containsKey(p.getCategory())) {
			return flatGroups.get(p.getCategory());
		}
		FavoriteGroup group = new FavoriteGroup();
		group.name = p.getCategory();
		group.visible = p.isVisible();
		group.color = p.getColor();
		flatGroups.put(group.name, group);
		favoriteGroups.add(group);
		if (group.color == 0) {
			group.color = defColor;
		}
		return group;
	}

	
	/// Deprecated sqlite db
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
	private SQLiteConnection conn;
	
	
	private SQLiteConnection openConnection(boolean readonly) {
		conn = context.getSQLiteAPI().getOrCreateDatabase(FAVOURITE_DB_NAME, readonly);
		if (conn.getVersion() == 0 || DATABASE_VERSION != conn.getVersion()) {
			if (readonly) {
				conn.close();
				conn = context.getSQLiteAPI().getOrCreateDatabase(FAVOURITE_DB_NAME, readonly);
			}
			if (conn.getVersion() == 0) {
				onCreate(conn);
			} else {
				onUpgrade(conn, conn.getVersion(), DATABASE_VERSION);
			}
			conn.setVersion(DATABASE_VERSION);
		}
		return conn;
	}
	
	public void onCreate(SQLiteConnection db) {
		db.execSQL(FAVOURITE_TABLE_CREATE);
	}

	public void onUpgrade(SQLiteConnection db, int oldVersion, int newVersion) {
		if(oldVersion == 1){
			db.execSQL("ALTER TABLE " + FAVOURITE_TABLE_NAME +  " ADD " + FAVOURITE_COL_CATEGORY + " text");
			db.execSQL("UPDATE " + FAVOURITE_TABLE_NAME + " SET category = ?", new Object[] { "" }); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	private void loadAndCheckDatabasePoints(){
		if (favoriteGroups == null) {
			SQLiteConnection db = openConnection(true);
			if (db != null) {
				try {
					SQLiteCursor query = db
							.rawQuery(
									"SELECT " + FAVOURITE_COL_NAME + ", " + FAVOURITE_COL_CATEGORY + ", " + FAVOURITE_COL_LAT + "," + FAVOURITE_COL_LON + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
											FAVOURITE_TABLE_NAME, null);
					cachedFavoritePoints.clear();
					if (query.moveToFirst()) {
						do {
							String name = query.getString(0);
							String cat = query.getString(1);

							FavouritePoint p = new FavouritePoint();
							p.setName(name);
							p.setCategory(cat);
							FavoriteGroup group = getOrCreateGroup(p, 0);
							if (!name.equals("")) {
								p.setLatitude(query.getDouble(2));
								p.setLongitude(query.getDouble(3));
								group.points.add(p);
							}
						} while (query.moveToNext());
					}
					query.close();
				} finally {
					db.close();
				}			
				sortAll();
			}
			recalculateCachedFavPoints();
		}
	}
	
	public boolean deleteFavouriteDB(FavouritePoint p) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL(
						"DELETE FROM " + FAVOURITE_TABLE_NAME + " WHERE category = ? AND " + whereNameLatLon(), new Object[] { p.getCategory(), p.getName(), p.getLatitude(), p.getLongitude() }); //$NON-NLS-1$ //$NON-NLS-2$
				FavouritePoint fp = findFavoriteByAllProperties(p.getCategory(), p.getName(), p.getLatitude(), p.getLongitude());
				if (fp != null) {
					FavoriteGroup group = flatGroups.get(p.getCategory());
					if(group != null) {
						group.points.remove(fp);
					}
					cachedFavoritePoints.remove(fp);
				}
				saveCurrentPointsIntoFile();
			} finally{
				db.close();
			}
			return true;
		}
		return false;
	}


	public boolean addFavouriteDB(FavouritePoint p) {
		if(p.getName().equals("") && flatGroups.containsKey(p.getCategory())){
			return true;
		}
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL(
						"INSERT INTO " + FAVOURITE_TABLE_NAME + " (" + FAVOURITE_COL_NAME + ", " + FAVOURITE_COL_CATEGORY + ", "
								+ FAVOURITE_COL_LAT + ", " + FAVOURITE_COL_LON + ")" + " VALUES (?, ?, ?, ?)", new Object[] { p.getName(), p.getCategory(), p.getLatitude(), p.getLongitude() }); //$NON-NLS-1$ //$NON-NLS-2$
				FavoriteGroup group = getOrCreateGroup(p,  0);
				if (!p.getName().equals("")) {
					p.setVisible(group.visible);
					p.setColor(group.color);
					group.points.add(p);
					cachedFavoritePoints.add(p);
				}
				saveCurrentPointsIntoFile();
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}
	
	

	public boolean editFavouriteNameDB(FavouritePoint p, String newName, String category) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				String oldCategory = p.getCategory();
				db.execSQL(
						"UPDATE " + FAVOURITE_TABLE_NAME + " SET " + FAVOURITE_COL_NAME + " = ?, " + FAVOURITE_COL_CATEGORY + "= ? WHERE " + whereNameLatLon(), new Object[] { newName, category, p.getName(), p.getLatitude(), p.getLongitude() }); //$NON-NLS-1$ //$NON-NLS-2$
				p.setName(newName);
				p.setCategory(category);
				if (!oldCategory.equals(category)) {
					FavoriteGroup old = flatGroups.get(oldCategory);
					if (old != null) {
						old.points.remove(p);
					}
					FavoriteGroup pg = getOrCreateGroup(p, 0);
					p.setVisible(pg.visible);
					p.setColor(pg.color);
					pg.points.add(p);
				}
				sortAll();
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}
	

	public boolean editFavouriteDB(FavouritePoint p, double lat, double lon) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL(
						"UPDATE " + FAVOURITE_TABLE_NAME + " SET latitude = ?, longitude = ? WHERE " + whereNameLatLon(), new Object[] { lat, lon, p.getName(), p.getLatitude(), p.getLongitude() }); //$NON-NLS-1$ //$NON-NLS-2$ 
				p.setLatitude(lat);
				p.setLongitude(lon);
				saveCurrentPointsIntoFile();
			} finally {
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




}
