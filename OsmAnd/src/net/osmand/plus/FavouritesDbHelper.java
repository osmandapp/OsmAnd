package net.osmand.plus;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PersonalFavouritePoint;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.MapMarkersHelper.MapMarkersGroup;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.util.Algorithms;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

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
import java.util.concurrent.ConcurrentHashMap;

import static net.osmand.data.PersonalFavouritePoint.PointType.HOME;
import static net.osmand.data.PersonalFavouritePoint.PointType.PARKING;
import static net.osmand.data.PersonalFavouritePoint.PointType.WORK;

public class FavouritesDbHelper {

	public interface FavoritesListener {
		void onFavoritesLoaded();
		void onFavoriteAddressResolved(@NonNull FavouritePoint favouritePoint);
	}

	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(FavouritesDbHelper.class);

	public static final String FILE_TO_SAVE = "favourites.gpx"; //$NON-NLS-1$
	public static final String BACKUP_FOLDER = "backup"; //$NON-NLS-1$
	public static final int BACKUP_CNT = 20; //$NON-NLS-1$
	public static final String FILE_TO_BACKUP = "favourites_bak.gpx"; //$NON-NLS-1$

	private List<FavouritePoint> cachedFavoritePoints = new ArrayList<>();
	private List<FavouritePoint> cachedPersonalFavoritePoints = new ArrayList<>();
	private List<FavoriteGroup> favoriteGroups = new ArrayList<>();
	private Map<String, FavoriteGroup> flatGroups = new LinkedHashMap<>();
	private final OsmandApplication context;
	private static final String DELIMETER = "__";

	private Set<FavoritesListener> listeners = new HashSet<>();
	private boolean favoritesLoaded;

	private Map<FavouritePoint, AddressLookupRequest> addressRequestMap = new ConcurrentHashMap<>();

	public FavouritesDbHelper(OsmandApplication context) {
		this.context = context;
	}

	public static class FavoriteGroup {
		public String name;
		public boolean visible = true;
		public int color;
		public boolean personal = false;
		public List<FavouritePoint> points = new ArrayList<FavouritePoint>();
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
		if (changed || !getExternalFile().exists()) {
			saveCurrentPointsIntoFile();
		}
		favoritesLoaded = true;
		context.runInUIThread(new Runnable() {
			@Override
			public void run() {
				for (FavoritesListener listener : listeners) {
					listener.onFavoritesLoaded();
				}
			}
		});
	}

	public FavouritePoint getWorkPoint() {
		return getPersonalPoint(WORK);
	}

	public FavouritePoint getHomePoint() {
		return getPersonalPoint(HOME);
	}

	public FavouritePoint getParkingPoint() {
		return getPersonalPoint(PARKING);
	}

	public void deleteParkingPoint() {
		deleteFavourite(getParkingPoint());
	}

	private FavouritePoint getPersonalPoint(PersonalFavouritePoint.PointType pointType) {
		for (FavouritePoint fp : cachedPersonalFavoritePoints) {
				if (((PersonalFavouritePoint) fp).getType() == pointType) {
					return fp;
				}
			}
		return null;
	}

	public boolean isFavoritesLoaded() {
		return favoritesLoaded;
	}

	public void addListener(FavoritesListener listener) {
		listeners.add(listener);
	}

	public void removeListener(FavoritesListener listener) {
		listeners.remove(listener);
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

	private void runSyncWithMarkers(FavoriteGroup favGroup) {
		MapMarkersHelper helper = context.getMapMarkersHelper();
		MapMarkersGroup group = helper.getMarkersGroup(favGroup);
		if(group != null) {
			helper.runSynchronization(group);
		}
	}

	private boolean removeFromMarkers(FavoriteGroup favGroup) {
		MapMarkersHelper helper = context.getMapMarkersHelper();
		MapMarkersGroup group = helper.getMarkersGroup(favGroup);
		if(group != null) {
			helper.removeMarkersGroup(group);
			return true;
		}
		return false;
	}

	private void addToMarkers(FavoriteGroup favGroup) {
		MapMarkersHelper helper = context.getMapMarkersHelper();
		helper.addOrEnableGroup(favGroup);
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
				runSyncWithMarkers(gr);
			}
		}
		if (groupsToDelete != null) {
			for (FavoriteGroup g : groupsToDelete) {
				flatGroups.remove(g.name);
				favoriteGroups.remove(g);
				cachedFavoritePoints.removeAll(g.points);
				removeFromMarkers(g);
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
				runSyncWithMarkers(group);
			}
			cachedFavoritePoints.remove(p);
			if (p.isPersonal()) {
				cachedPersonalFavoritePoints.remove(p);
			}
		}
		if (saveImmediately) {
			saveCurrentPointsIntoFile();
		}
		return true;
	}

	public void setHomePoint(@NonNull LatLon latLon, @Nullable String description) {
		FavouritePoint homePoint = getHomePoint();
		if (homePoint != null) {
			editFavourite(homePoint, latLon.getLatitude(), latLon.getLongitude(), description);
		} else {
			homePoint = new PersonalFavouritePoint(context, HOME, latLon.getLatitude(), latLon.getLongitude());
			homePoint.setDescription(description);
			addFavourite(homePoint);
		}
		if (description == null) {
			lookupAddress(homePoint);
		}
	}

	public void setWorkPoint(@NonNull LatLon latLon, @Nullable String description) {
		FavouritePoint workPoint = getWorkPoint();
		if (workPoint != null) {
			editFavourite(workPoint, latLon.getLatitude(), latLon.getLongitude(), description);
		} else {
			workPoint = new PersonalFavouritePoint(context, WORK, latLon.getLatitude(), latLon.getLongitude());
			workPoint.setDescription(description);
			addFavourite(workPoint);
		}
		if (description == null) {
			lookupAddress(workPoint);
		}
	}

	public void setParkingPoint(@NonNull LatLon latLon) {
		FavouritePoint parkingPoint = getParkingPoint();
		if (parkingPoint != null) {
			editFavourite(parkingPoint, latLon.getLatitude(), latLon.getLongitude(), null);
		} else {
			parkingPoint = new PersonalFavouritePoint(context, PARKING, latLon.getLatitude(), latLon.getLongitude());
			addFavourite(parkingPoint);
		}
		lookupAddress(parkingPoint);
	}

	public boolean addFavourite(FavouritePoint p) {
		return addFavourite(p, true);
	}

	public boolean addFavourite(FavouritePoint p, boolean saveImmediately) {
		if (p.getName().equals("") && flatGroups.containsKey(p.getCategory())) {
			return true;
		}
		context.getSettings().SHOW_FAVORITES.set(true);
		FavoriteGroup group = getOrCreateGroup(p, 0);

		if (!p.getName().equals("")) {
			p.setVisible(group.visible);
			p.setColor(group.color);
			group.points.add(p);
			cachedFavoritePoints.add(p);
		}
		if (p.isPersonal()) {
			cachedPersonalFavoritePoints.add(p);
		}
		if (saveImmediately) {
			sortAll();
			saveCurrentPointsIntoFile();
		}
		runSyncWithMarkers(group);

		return true;
	}

	public void lookupAddressAllPersonalPoints() {
		if (!context.isApplicationInitializing()) {
			FavouritePoint workPoint = getWorkPoint();
			if (workPoint != null) {
				lookupAddress(workPoint);
			}
			FavouritePoint homePoint = getHomePoint();
			if (homePoint != null) {
				lookupAddress(homePoint);
			}
			FavouritePoint parkingPoint = getParkingPoint();
			if (parkingPoint != null) {
				lookupAddress(parkingPoint);
			}
		}
	}

	private void lookupAddress(@NonNull final FavouritePoint p) {
		AddressLookupRequest request = addressRequestMap.get(p);
		double latitude = p.getLatitude();
		double longitude = p.getLongitude();
		if (request == null || !request.getLatLon().equals(new LatLon(latitude, longitude))) {
			cancelAddressRequest(p);
			request = new AddressLookupRequest(new LatLon(latitude, longitude),
					new GeocodingLookupService.OnAddressLookupResult() {
				@Override
				public void geocodingDone(String address) {
					addressRequestMap.remove(p);
					editFavouriteDescription(p, address);
					context.runInUIThread(new Runnable() {
						@Override
						public void run() {
							for (FavoritesListener listener : listeners) {
								listener.onFavoriteAddressResolved(p);
							}
						}
					});

				}
			}, null);
			addressRequestMap.put(p, request);
			context.getGeocodingLookupService().lookupAddress(request);
		}
	}

	private void cancelAddressRequest(@NonNull FavouritePoint p) {
		AddressLookupRequest request = addressRequestMap.get(p);
		if (request != null) {
			context.getGeocodingLookupService().cancel(request);
			addressRequestMap.remove(p);
		}
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
		runSyncWithMarkers(getOrCreateGroup(p, 0));
		return true;
	}

	public boolean editFavouriteDescription(FavouritePoint p, String description) {
		p.setDescription(description);
		saveCurrentPointsIntoFile();
		runSyncWithMarkers(getOrCreateGroup(p, 0));
		return true;
	}

	public boolean editFavourite(FavouritePoint p, double lat, double lon) {
		cancelAddressRequest(p);
		p.setLatitude(lat);
		p.setLongitude(lon);
		saveCurrentPointsIntoFile();
		runSyncWithMarkers(getOrCreateGroup(p, 0));
		return true;
	}

	public boolean editFavourite(FavouritePoint p, double lat, double lon, String description) {
		cancelAddressRequest(p);
		p.setLatitude(lat);
		p.setLongitude(lon);
		p.setDescription(description);
		saveCurrentPointsIntoFile();
		runSyncWithMarkers(getOrCreateGroup(p, 0));
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
			BZip2CompressorOutputStream out = new BZip2CompressorOutputStream( new FileOutputStream(f));
			FileInputStream fis = new FileInputStream(externalFile);
			Algorithms.streamCopy(fis, out);
			fis.close();
			out.close();
		} catch (Exception e) {
			log.warn("Backup failed", e);
		}
	}

	public Exception exportFavorites() {
		return saveExternalFile(null);
	}


	private Exception saveExternalFile(Set<String> deleted) {
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
			removeFromMarkers(group);
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

	public Exception saveFile(List<FavouritePoint> favoritePoints, File f) {
		GPXFile gpx = asGpxFile(favoritePoints);
		return GPXUtilities.writeGpxFile(f, gpx);
	}


	public GPXFile asGpxFile() {
		return asGpxFile(cachedFavoritePoints);
	}

	private GPXFile asGpxFile(List<FavouritePoint> favoritePoints) {
		GPXFile gpx = new GPXFile(Version.getFullVersion(context));
		for (FavouritePoint p : favoritePoints) {
			context.getSelectedGpxHelper().addPoint(p.toWpt(), gpx);
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

	public List<FavouritePoint> getNonPersonalVisibleFavouritePoints() {
		List<FavouritePoint> fp = new ArrayList<>();
		for (FavouritePoint p : getNonPersonalFavouritePoints()) {
			if (p.isVisible()) {
				fp.add(p);
			}
		}
		return fp;
	}

	public List<FavouritePoint> getNonPersonalFavouritePoints() {
		List<FavouritePoint> fp = new ArrayList<>();
		for (FavouritePoint p : cachedFavoritePoints) {
			if (!p.isPersonal()) {
				fp.add(p);
			}
		}
		return fp;
	}

	@Nullable
	public FavouritePoint getVisibleFavByLatLon(@NonNull LatLon latLon) {
		for (FavouritePoint fav : cachedFavoritePoints) {
			if (fav.isVisible() && latLon.equals(new LatLon(fav.getLatitude(), fav.getLongitude()))) {
				return fav;
			}
		}
		return null;
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
		List<FavouritePoint> allPoints = new ArrayList<>();
		List<FavouritePoint> personalPoints = new ArrayList<>();
		for (FavoriteGroup f : favoriteGroups) {
			if (f.personal) {
				personalPoints.addAll(f.points);
			}
			allPoints.addAll(f.points);
		}
		cachedFavoritePoints = allPoints;
		cachedPersonalFavoritePoints = personalPoints;
	}

	public void sortAll() {
		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		Collections.sort(favoriteGroups, new Comparator<FavoriteGroup>() {

			@Override
			public int compare(FavoriteGroup lhs, FavoriteGroup rhs) {
				return lhs.personal ? -1 : rhs.personal ? 1 : collator.compare(lhs.name, rhs.name);
			}
		});
		Comparator<FavouritePoint> favoritesComparator = getComparator();
		for (FavoriteGroup g : favoriteGroups) {
			Collections.sort(g.points, favoritesComparator);
		}
		if (cachedFavoritePoints != null) {
			Collections.sort(cachedFavoritePoints, favoritesComparator);
		}
		if (cachedPersonalFavoritePoints != null) {
			Collections.sort(cachedPersonalFavoritePoints, favoritesComparator);
		}
	}

	public static Comparator<FavouritePoint> getComparator() {
		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		return new Comparator<FavouritePoint>() {

			@Override
			public int compare(FavouritePoint o1, FavouritePoint o2) {
				if (o1.isPersonal() && o2.isPersonal()) {
					int x = ((PersonalFavouritePoint) o1).getType().getOrder();
					int y = ((PersonalFavouritePoint) o2).getType().getOrder();
					return Algorithms.compare(x, y);
				} else if (o1.isPersonal()) {
					return -1;
				} else if (o2.isPersonal()) {
					return 1;
				}
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
	}

	private boolean loadGPXFile(File file, Map<String, FavouritePoint> points) {
		if (!file.exists()) {
			return false;
		}
		GPXFile res = GPXUtilities.loadGPXFile(file);
		if (res.error != null) {
			return false;
		}
		for (WptPt p : res.getPoints()) {
			FavouritePoint fp = FavouritePoint.fromWpt(context, p);
			if (fp != null) {
				points.put(getKey(fp), fp);
			}
		}
		return true;
	}

	public void editFavouriteGroup(FavoriteGroup group, String newName, int color, boolean visible) {
		if (color != 0 && group.color != color) {
			FavoriteGroup gr = flatGroups.get(group.name);
			group.color = color;
			for (FavouritePoint p : gr.points) {
				p.setColor(color);
			}
			runSyncWithMarkers(gr);
		}
		if (group.visible != visible) {
			FavoriteGroup gr = flatGroups.get(group.name);
			group.visible = visible;
			for (FavouritePoint p : gr.points) {
				p.setVisible(visible);
			}
			runSyncWithMarkers(gr);
		}
		if (!group.name.equals(newName)) {
			FavoriteGroup gr = flatGroups.remove(group.name);
			boolean isInMarkers = removeFromMarkers(gr);
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
			if (isInMarkers) {
				addToMarkers(renamedGroup);
			}
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
		group.personal = p.isPersonal();
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
		if (conn.getVersion() < DATABASE_VERSION) {
			if (readonly) {
				conn.close();
				conn = context.getSQLiteAPI().getOrCreateDatabase(FAVOURITE_DB_NAME, false);
			}
			int version = conn.getVersion();
			if (version == 0) {
				onCreate(conn);
			} else {
				onUpgrade(conn, version, DATABASE_VERSION);
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
					if (query != null && query.moveToFirst()) {
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
					if (query != null) {
						query.close();
					}
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
