package net.osmand.plus;

import android.content.Context;
import android.support.v7.app.AlertDialog;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.MapMarkersHelper.MarkersSyncGroup;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.util.Algorithms;

import org.apache.tools.bzip2.CBZip2OutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FavouritesDbHelper {

	public interface FavoritesUpdatedListener {
		void updateFavourites();
	}


	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(FavouritesDbHelper.class);

	public static final String FILE_TO_SAVE = "favourites.gpx"; //$NON-NLS-1$
	public static final String BACKUP_FOLDER = "backup"; //$NON-NLS-1$
	public static final int BACKUP_CNT = 20; //$NON-NLS-1$
	public static final String FILE_TO_BACKUP = "favourites_bak.gpx"; //$NON-NLS-1$

	private List<FavouritePoint> cachedFavoritePoints = new ArrayList<FavouritePoint>();
	private List<FavoriteGroup> favoriteGroups = new ArrayList<FavouritesDbHelper.FavoriteGroup>();
	private Map<String, FavoriteGroup> flatGroups = new LinkedHashMap<String, FavouritesDbHelper.FavoriteGroup>();
	private List<FavouritePoint> syncedFavouritePoints = new ArrayList<>();
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

	public void addSyncedGroup(FavoriteGroup group) {
		List<FavouritePoint> copyList = new ArrayList<>(syncedFavouritePoints);
		copyList.addAll(group.points);
		syncedFavouritePoints = copyList;
	}

	public void removeSyncedGroup(FavoriteGroup group) {
		List<FavouritePoint> copyList = new ArrayList<>(syncedFavouritePoints);
		List<FavouritePoint> pointsToRemove = new ArrayList<>();
		for (int i = 0; i < copyList.size(); i++) {
			FavouritePoint point = copyList.get(i);
			if (point.getCategory().equals(group.name)) {
				pointsToRemove.add(point);
			}
		}
		copyList.removeAll(pointsToRemove);
		syncedFavouritePoints = copyList;
	}

	public List<FavouritePoint> getSyncedFavouritePoints() {
		return syncedFavouritePoints;
	}

	public void loadFavorites() {
		flatGroups.clear();
		favoriteGroups.clear();

		File internalFile = getInternalFile();
		if (!internalFile.exists()) {
			File dbPath = context.getDatabasePath(FAVOURITE_DB_NAME);
			if (dbPath.exists()) {
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

		for (FavouritePoint pns : points.values()) {
			FavoriteGroup group = getOrCreateGroup(pns, 0);
			group.points.add(pns);
		}
		sortAll();
		recalculateCachedFavPoints();
		if (changed) {
			saveCurrentPointsIntoFile();
		}
		favouritesUpdated();

	}

	private void favouritesUpdated() {
	}


	private boolean merge(Map<String, FavouritePoint> source, Map<String, FavouritePoint> destination) {
		boolean changed = false;
		for (String ks : source.keySet()) {
			if (!destination.containsKey(ks)) {
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
			Set<FavoriteGroup> groupsToSync = new HashSet<>();
			for (FavouritePoint p : favoritesSelected) {
				FavoriteGroup group = flatGroups.get(p.getCategory());
				if (group != null) {
					group.points.remove(p);
					groupsToSync.add(group);
				}
				cachedFavoritePoints.remove(p);
			}
			for (FavoriteGroup gr : groupsToSync) {
				context.getMapMarkersHelper().syncGroupAsync(new MarkersSyncGroup(gr.name, gr.name, MarkersSyncGroup.FAVORITES_TYPE));
			}
		}
		if (groupsToDelete != null) {
			for (FavoriteGroup g : groupsToDelete) {
				flatGroups.remove(g.name);
				favoriteGroups.remove(g);
				cachedFavoritePoints.removeAll(g.points);
				context.getMapMarkersHelper().removeMarkersSyncGroup(g.name, true);
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
				context.getMapMarkersHelper().syncGroupAsync(new MarkersSyncGroup(group.name, group.name, MarkersSyncGroup.FAVORITES_TYPE));
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
			sortAll();
			saveCurrentPointsIntoFile();
		}
		context.getMapMarkersHelper().syncGroupAsync(new MarkersSyncGroup(group.name, group.name, MarkersSyncGroup.FAVORITES_TYPE, group.color));

		return true;
	}

	public static AlertDialog.Builder checkDuplicates(FavouritePoint p, FavouritesDbHelper fdb, Context uiContext) {
		boolean emoticons = false;
		String index = "";
		int number = 0;
		String name = checkEmoticons(p.getName());
		String category = checkEmoticons(p.getCategory());
		p.setCategory(category);
		String description = null;
		if (p.getDescription() != null) {
			description = checkEmoticons(p.getDescription());
		}
		p.setDescription(description);
		if (name.length() != p.getName().length()) {
			emoticons = true;
		}
		boolean fl = true;
		while (fl) {
			fl = false;
			for (FavouritePoint fp : fdb.getFavouritePoints()) {
				if (fp.getName().equals(name) && p.getLatitude() != fp.getLatitude() && p.getLongitude() != fp.getLongitude()) {
					number++;
					index = " (" + number + ")";
					name = p.getName() + index;
					fl = true;
					break;
				}
			}
		}
		if ((index.length() > 0 || emoticons)) {
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

	public static String checkEmoticons(String name) {
		char[] chars = name.toCharArray();
		int index;
		char ch1;
		char ch2;

		index = 0;
		StringBuilder builder = new StringBuilder();
		while (index < chars.length) {
			ch1 = chars[index];
			if ((int) ch1 == 0xD83C) {
				ch2 = chars[index + 1];
				if ((int) ch2 >= 0xDF00 && (int) ch2 <= 0xDFFF) {
					index += 2;
					continue;
				}
			} else if ((int) ch1 == 0xD83D) {
				ch2 = chars[index + 1];
				if ((int) ch2 >= 0xDC00 && (int) ch2 <= 0xDDFF) {
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
		context.getMapMarkersHelper().syncGroupAsync(new MarkersSyncGroup(category, category, MarkersSyncGroup.FAVORITES_TYPE, p.getColor()));
		return true;
	}

	public boolean editFavourite(FavouritePoint p, double lat, double lon) {
		p.setLatitude(lat);
		p.setLongitude(lon);
		saveCurrentPointsIntoFile();
		context.getMapMarkersHelper().syncGroupAsync(new MarkersSyncGroup(p.getCategory(), p.getCategory(), MarkersSyncGroup.FAVORITES_TYPE, p.getColor()));
		return true;
	}

	public void saveCurrentPointsIntoFile() {
		try {
			Map<String, FavouritePoint> deletedInMemory = new LinkedHashMap<String, FavouritePoint>();
			loadGPXFile(getInternalFile(), deletedInMemory);
			for (FavouritePoint fp : cachedFavoritePoints) {
				deletedInMemory.remove(getKey(fp));
			}
			saveFile(cachedFavoritePoints, getInternalFile());
			saveExternalFile(deletedInMemory.keySet());
			backup(getBackupFile(), getExternalFile());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void backup(File backupFile, File externalFile) {
		try {
			File f = new File(backupFile.getParentFile(), backupFile.getName());
			FileOutputStream fout = new FileOutputStream(f);
			fout.write('B');
			fout.write('Z');
			CBZip2OutputStream out = new CBZip2OutputStream(fout);
			FileInputStream fis = new FileInputStream(externalFile);
			Algorithms.streamCopy(fis, out);
			fis.close();
			out.close();
			fout.close();
		} catch (Exception e) {
			log.warn("Backup failed", e);
		}
	}

	public String exportFavorites() {
		return saveExternalFile(null);
	}


	private String saveExternalFile(Set<String> deleted) {
		Map<String, FavouritePoint> all = new LinkedHashMap<String, FavouritePoint>();
		loadGPXFile(getExternalFile(), all);
		List<FavouritePoint> favoritePoints = new ArrayList<FavouritePoint>(cachedFavoritePoints);
		if (deleted != null) {
			for (String key : deleted) {
				all.remove(key);
			}
		}
		// remove already existing in memory
		for (FavouritePoint p : favoritePoints) {
			all.remove(getKey(p));
		}
		// save favoritePoints from memory in order to update existing
		favoritePoints.addAll(all.values());
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
			context.getMapMarkersHelper().removeMarkersSyncGroup(group.name, true);
			return true;
		}
		return false;
	}

	public File getExternalFile() {
		return new File(context.getAppPath(null), FILE_TO_SAVE);
	}

	public File getBackupFile() {
		File fld = new File(context.getAppPath(null), BACKUP_FOLDER);
		if (!fld.exists()) {
			fld.mkdirs();
		}
		int back = 1;
		String backPrefix = "" + back;
		File firstModified = null;
		long firstModifiedMin = System.currentTimeMillis();
		while (back <= BACKUP_CNT) {
			backPrefix = "" + back;
			if (back < 10) {
				backPrefix = "0" + backPrefix;
			}
			File bak = new File(fld, "favourites_bak_" + backPrefix + ".gpx.bz2");
			if (!bak.exists()) {
				return bak;
			} else if (bak.lastModified() < firstModifiedMin) {
				firstModified = bak;
				firstModifiedMin = bak.lastModified();
			}
			back++;
		}
		return firstModified;
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
			if (!p.isVisible()) {
				pt.getExtensionsToWrite().put(HIDDEN, "true");
			}
			if (p.getColor() != 0) {
				pt.setColor(p.getColor());
			}
			pt.name = p.getName();
			pt.desc = p.getDescription();
			if (p.getCategory().length() > 0)
				pt.category = p.getCategory();
			if (p.getOriginObjectName().length() > 0) {
				pt.comment = p.getOriginObjectName();
			}
			context.getSelectedGpxHelper().addPoint(pt, gpx);
		}
		return gpx;
	}


	public void addEmptyCategory(String name) {
		addEmptyCategory(name, 0, true);
	}

	public void addEmptyCategory(String name, int color) {
		addEmptyCategory(name, color, true);
	}

	public void addEmptyCategory(String name, int color, boolean visible) {
		FavoriteGroup group = new FavoriteGroup();
		group.name = name;
		group.color = color;
		group.visible = visible;
		favoriteGroups.add(group);
		flatGroups.put(name, group);
	}

	public List<FavouritePoint> getFavouritePoints() {
		return cachedFavoritePoints;
	}

	public List<FavouritePoint> getVisibleFavouritePoints() {
		List<FavouritePoint> fp = new ArrayList<>();
		for (FavouritePoint p : cachedFavoritePoints) {
			if (p.isVisible()) {
				fp.add(p);
			}
		}
		return fp;
	}


	public List<FavoriteGroup> getFavoriteGroups() {
		return favoriteGroups;
	}

	public boolean groupExists(String name) {
		String nameLowercase = name.toLowerCase();
		for (String groupName : flatGroups.keySet()) {
			if (groupName.toLowerCase().equals(nameLowercase)) {
				return true;
			}
		}
		return false;
	}

	public FavoriteGroup getGroup(FavouritePoint p) {
		if (flatGroups.containsKey(p.getCategory())) {
			return flatGroups.get(p.getCategory());
		} else {
			return null;
		}
	}

	public FavoriteGroup getGroup(String name) {
		if (flatGroups.containsKey(name)) {
			return flatGroups.get(name);
		} else {
			return null;
		}
	}

	private FavouritePoint findFavoriteByAllProperties(String category, String name, double lat, double lon) {
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


	public void recalculateCachedFavPoints() {
		ArrayList<FavouritePoint> temp = new ArrayList<FavouritePoint>();
		for (FavoriteGroup f : favoriteGroups) {
			temp.addAll(f.points);
		}
		cachedFavoritePoints = temp;
	}

	public void sortAll() {
		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		Collections.sort(favoriteGroups, new Comparator<FavoriteGroup>() {

			@Override
			public int compare(FavoriteGroup lhs, FavoriteGroup rhs) {
				return collator.compare(lhs.name, rhs.name);
			}
		});
		Comparator<FavouritePoint> favoritesComparator = getComparator();
		for (FavoriteGroup g : favoriteGroups) {
			Collections.sort(g.points, favoritesComparator);
		}
		if (cachedFavoritePoints != null) {
			Collections.sort(cachedFavoritePoints, favoritesComparator);
		}
	}

	public static Comparator<FavouritePoint> getComparator() {
		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		Comparator<FavouritePoint> favoritesComparator = new Comparator<FavouritePoint>() {

			@Override
			public int compare(FavouritePoint o1, FavouritePoint o2) {
				String s1 = o1.getName();
				String s2 = o2.getName();
				int i1 = Algorithms.extractIntegerNumber(s1);
				int i2 = Algorithms.extractIntegerNumber(s2);
				String ot1 = Algorithms.extractIntegerPrefix(s1);
				String ot2 = Algorithms.extractIntegerPrefix(s2);
				// Next 6 lines needed for correct comparison of names with and without digits
				if (ot1.length() == 0) {
					ot1 = s1;
				}
				if (ot2.length() == 0) {
					ot2 = s2;
				}
				int res = collator.compare(ot1, ot2);
				if (res == 0) {
					res = i1 - i2;
				}
				if (res == 0) {
					res = collator.compare(s1, s2);
				}
				return res;

			}
		};
		return favoritesComparator;
	}


	private boolean loadGPXFile(File file, Map<String, FavouritePoint> points) {
		if (!file.exists()) {
			return false;
		}
		GPXFile res = GPXUtilities.loadGPXFile(context, file);
		if (res.warning != null) {
			return false;
		}
		for (WptPt p : res.getPoints()) {
			int c;
			String name = p.name;
			String categoryName = p.category != null ? p.category : "";
			if (name == null) {
				name = "";
			}
			// old way to store the category, in name.
			if ("".equals(categoryName.trim()) && (c = name.lastIndexOf('_')) != -1) {
				categoryName = name.substring(c + 1);
				name = name.substring(0, c);
			}
			FavouritePoint fp = new FavouritePoint(p.lat, p.lon, name, categoryName);
			fp.setDescription(p.desc);
			if (p.comment != null) {
				fp.setOriginObjectName(p.comment);
			}
			fp.setColor(p.getColor(0));
			fp.setVisible(!p.getExtensionsToRead().containsKey(HIDDEN));
			points.put(getKey(fp), fp);
		}
		return true;
	}

	public void editFavouriteGroup(FavoriteGroup group, String newName, int color, boolean visible) {
		MapMarkersHelper markersHelper = context.getMapMarkersHelper();
		if (color != 0 && group.color != color) {
			FavoriteGroup gr = flatGroups.get(group.name);
			group.color = color;
			for (FavouritePoint p : gr.points) {
				p.setColor(color);
			}
			markersHelper.syncGroupAsync(new MarkersSyncGroup(gr.name, gr.name, MarkersSyncGroup.FAVORITES_TYPE, color));
		}
		if (group.visible != visible) {
			FavoriteGroup gr = flatGroups.get(group.name);
			group.visible = visible;
			for (FavouritePoint p : gr.points) {
				p.setVisible(visible);
			}
			markersHelper.syncGroupAsync(new MarkersSyncGroup(gr.name, gr.name, MarkersSyncGroup.FAVORITES_TYPE, group.color));
		}
		if (!group.name.equals(newName)) {
			FavoriteGroup gr = flatGroups.remove(group.name);
			markersHelper.removeMarkersSyncGroup(group.name, true);
			gr.name = newName;
			FavoriteGroup renamedGroup = flatGroups.get(gr.name);
			boolean existing = renamedGroup != null;
			if (renamedGroup == null) {
				renamedGroup = gr;
				flatGroups.put(gr.name, gr);
			} else {
				favoriteGroups.remove(gr);
			}
			for (FavouritePoint p : gr.points) {
				p.setCategory(newName);
				if (existing) {
					renamedGroup.points.add(p);
				}
			}
			MarkersSyncGroup syncGroup = new MarkersSyncGroup(renamedGroup.name, renamedGroup.name, MarkersSyncGroup.FAVORITES_TYPE, group.color);
			markersHelper.addMarkersSyncGroup(syncGroup);
			markersHelper.syncGroupAsync(syncGroup);
		}
		saveCurrentPointsIntoFile();
	}

	protected void createDefaultCategories() {
		addEmptyCategory(context.getString(R.string.favorite_home_category));
		addEmptyCategory(context.getString(R.string.favorite_friends_category));
		addEmptyCategory(context.getString(R.string.favorite_places_category));
		addEmptyCategory(context.getString(R.string.shared_string_others));
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
				conn = context.getSQLiteAPI().getOrCreateDatabase(FAVOURITE_DB_NAME, false);
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
		if (oldVersion == 1) {
			db.execSQL("ALTER TABLE " + FAVOURITE_TABLE_NAME + " ADD " + FAVOURITE_COL_CATEGORY + " text");
			db.execSQL("UPDATE " + FAVOURITE_TABLE_NAME + " SET category = ?", new Object[]{""}); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void loadAndCheckDatabasePoints() {
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
						"DELETE FROM " + FAVOURITE_TABLE_NAME + " WHERE category = ? AND " + whereNameLatLon(), new Object[]{p.getCategory(), p.getName(), p.getLatitude(), p.getLongitude()}); //$NON-NLS-1$ //$NON-NLS-2$
				FavouritePoint fp = findFavoriteByAllProperties(p.getCategory(), p.getName(), p.getLatitude(), p.getLongitude());
				if (fp != null) {
					FavoriteGroup group = flatGroups.get(p.getCategory());
					if (group != null) {
						group.points.remove(fp);
					}
					cachedFavoritePoints.remove(fp);
				}
				saveCurrentPointsIntoFile();
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}


	public boolean addFavouriteDB(FavouritePoint p) {
		if (p.getName().equals("") && flatGroups.containsKey(p.getCategory())) {
			return true;
		}
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL(
						"INSERT INTO " + FAVOURITE_TABLE_NAME + " (" + FAVOURITE_COL_NAME + ", " + FAVOURITE_COL_CATEGORY + ", "
								+ FAVOURITE_COL_LAT + ", " + FAVOURITE_COL_LON + ")" + " VALUES (?, ?, ?, ?)", new Object[]{p.getName(), p.getCategory(), p.getLatitude(), p.getLongitude()}); //$NON-NLS-1$ //$NON-NLS-2$
				FavoriteGroup group = getOrCreateGroup(p, 0);
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
						"UPDATE " + FAVOURITE_TABLE_NAME + " SET " + FAVOURITE_COL_NAME + " = ?, " + FAVOURITE_COL_CATEGORY + "= ? WHERE " + whereNameLatLon(), new Object[]{newName, category, p.getName(), p.getLatitude(), p.getLongitude()}); //$NON-NLS-1$ //$NON-NLS-2$
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
						"UPDATE " + FAVOURITE_TABLE_NAME + " SET latitude = ?, longitude = ? WHERE " + whereNameLatLon(), new Object[]{lat, lon, p.getName(), p.getLatitude(), p.getLongitude()}); //$NON-NLS-1$ //$NON-NLS-2$
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
