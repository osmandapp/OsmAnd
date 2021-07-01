package net.osmand.plus;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.FavouritePoint.SpecialPointType;
import net.osmand.data.LatLon;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
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


public class FavouritesDbHelper {

	public interface FavoritesListener {

		void onFavoritesLoaded();

		void onFavoriteDataUpdated(@NonNull FavouritePoint favouritePoint);
	}

	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(FavouritesDbHelper.class);

	public static final String FILE_TO_SAVE = "favourites.gpx"; //$NON-NLS-1$
	public static final String BACKUP_FOLDER = "backup"; //$NON-NLS-1$
	public static final int BACKUP_CNT = 20; //$NON-NLS-1$
	public static final String FILE_TO_BACKUP = "favourites_bak.gpx"; //$NON-NLS-1$

	private List<FavouritePoint> cachedFavoritePoints = new ArrayList<>();
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
		public static final String PERSONAL_CATEGORY = "personal";
		private String name;
		private boolean visible = true;
		private int color;
		private List<FavouritePoint> points = new ArrayList<>();

		public FavoriteGroup() {
		}

		public FavoriteGroup(String name, boolean visible, int color) {
			this.name = name;
			this.visible = visible;
			this.color = color;
		}

		public FavoriteGroup(String name, List<FavouritePoint> points, int color, boolean visible) {
			this.name = name;
			this.color = color;
			this.points = points;
			this.visible = visible;
		}

		public boolean isPersonal() {
			return isPersonal(name);
		}

		private static boolean isPersonal(String name) {
			return PERSONAL_CATEGORY.equals(name);
		}

		public static boolean isPersonalCategoryDisplayName(Context ctx, String name) {
			return name.equals(ctx.getString(R.string.personal_category_name));
		}

		public static String getDisplayName(Context ctx, String name) {
			if (isPersonal(name)) {
				return ctx.getString(R.string.personal_category_name);
			} else if (name.isEmpty()) {
				return ctx.getString(R.string.shared_string_favorites);
			} else {
				return name;
			}
		}

		public List<FavouritePoint> getPoints() {
			return points;
		}

		public int getColor() {
			return color;
		}

		public boolean isVisible() {
			return visible;
		}

		public String getName() {
			return name;
		}

		public String getDisplayName(Context ctx) {
			return getDisplayName(ctx, name);
		}

		public static String convertDisplayNameToGroupIdName(Context context, String name) {
			if (isPersonalCategoryDisplayName(context, name)) {
				return PERSONAL_CATEGORY;
			}
			if (name.equals(context.getString(R.string.shared_string_favorites))) {
				return "";
			}
			return name;
		}
	}

	public long getLastUploadedTime() {
		return context.getSettings().FAVORITES_LAST_UPLOADED_TIME.get();
	}

	public void setLastUploadedTime(long time) {
		context.getSettings().FAVORITES_LAST_UPLOADED_TIME.set(time);
	}

	@Nullable
	public Drawable getColoredIconForGroup(String groupName) {
		String groupIdName = FavoriteGroup.convertDisplayNameToGroupIdName(context, groupName);
		FavoriteGroup favoriteGroup = getGroup(groupIdName);
		if (favoriteGroup != null) {
			int color = favoriteGroup.getColor() == 0 ?
					context.getResources().getColor(R.color.color_favorite) : favoriteGroup.getColor();
			return context.getUIUtilities().getPaintedIcon(R.drawable.ic_action_group_name_16, color);
		}
		return null;
	}

	public int getColorWithCategory(FavouritePoint point, int defaultColor) {
		int color = 0;
		if (point.getColor() != 0) {
			color = point.getColor();
		} else {
			FavoriteGroup favoriteGroup = getGroup(point);
			if (favoriteGroup != null) {
				color = favoriteGroup.getColor();
			}
			if (color == 0) {
				color = defaultColor;
			}
		}
		return color;
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
		notifyListeners();
	}

	void fixBlackBackground() {
		flatGroups.clear();
		favoriteGroups.clear();
		for (FavouritePoint fp : cachedFavoritePoints) {
			if (fp.getColor() == 0xFF000000 || fp.getColor() == ContextCompat.getColor(context, R.color.color_favorite)) {
				fp.setColor(0);
			}
			if (fp.getBackgroundType() == FavouritePoint.DEFAULT_BACKGROUND_TYPE) {
				fp.setBackgroundType(null);
			}
			if (fp.getIconId() == FavouritePoint.DEFAULT_UI_ICON_ID) {
				fp.setIconId(0);
			}
			FavoriteGroup group = getOrCreateGroup(fp, 0);
			group.points.add(fp);
		}
		sortAll();
		saveCurrentPointsIntoFile();
		notifyListeners();
	}

	private void notifyListeners() {
		context.runInUIThread(new Runnable() {
			@Override
			public void run() {
				for (FavoritesListener listener : listeners) {
					listener.onFavoritesLoaded();
				}
			}
		});
	}

	public FavouritePoint getSpecialPoint(SpecialPointType pointType) {
		for (FavouritePoint fp : cachedFavoritePoints) {
			if (fp.getSpecialPointType() == pointType) {
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
		for (Map.Entry<String, FavouritePoint> entry : source.entrySet()) {
			String ks = entry.getKey();
			if (!destination.containsKey(ks)) {
				changed = true;
				destination.put(ks, entry.getValue());
			}
		}
		return changed;
	}

	private void runSyncWithMarkers(FavoriteGroup favGroup) {
		MapMarkersHelper helper = context.getMapMarkersHelper();
		MapMarkersGroup group = helper.getMarkersGroup(favGroup);
		if (group != null) {
			helper.runSynchronization(group);
		}
	}

	private boolean removeFromMarkers(FavoriteGroup favGroup) {
		MapMarkersHelper helper = context.getMapMarkersHelper();
		MapMarkersGroup group = helper.getMarkersGroup(favGroup);
		if (group != null) {
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
		}
		if (saveImmediately) {
			saveCurrentPointsIntoFile();
		}
		return true;
	}

	public void setParkingPoint(@NonNull LatLon latLon, @Nullable String address, long pickupTimestamp, boolean addToCalendar) {
		SpecialPointType specialType = SpecialPointType.PARKING;
		FavouritePoint point = getSpecialPoint(specialType);
		if (point != null) {
			point.setIconId(specialType.getIconId(context));
			point.setTimestamp(pickupTimestamp);
			point.setCalendarEvent(addToCalendar);
			editFavourite(point, latLon.getLatitude(), latLon.getLongitude(), address);
			lookupAddress(point);
		} else {
			point = new FavouritePoint(latLon.getLatitude(), latLon.getLongitude(), specialType.getName(), specialType.getCategory());
			point.setAddress(address);
			point.setTimestamp(pickupTimestamp);
			point.setCalendarEvent(addToCalendar);
			point.setIconId(specialType.getIconId(context));
			addFavourite(point);
		}
	}

	public void setSpecialPoint(@NonNull LatLon latLon, SpecialPointType specialType, @Nullable String address) {
		FavouritePoint point = getSpecialPoint(specialType);
		if (point != null) {
			point.setIconId(specialType.getIconId(context));
			editFavourite(point, latLon.getLatitude(), latLon.getLongitude(), address);
			lookupAddress(point);
		} else {
			point = new FavouritePoint(latLon.getLatitude(), latLon.getLongitude(), specialType.getName(), specialType.getCategory());
			point.setAddress(address);
			point.setIconId(specialType.getIconId(context));
			addFavourite(point);
		}
	}

	public boolean addFavourite(FavouritePoint p) {
		return addFavourite(p, true);
	}

	public boolean addFavourite(FavouritePoint p, boolean saveImmediately) {
		if (Double.isNaN(p.getAltitude()) || p.getAltitude() == 0) {
			p.initAltitude(context);
		}
		if (p.getName().isEmpty() && flatGroups.containsKey(p.getCategory())) {
			return true;
		}
		if (!p.isAddressSpecified()) {
			lookupAddress(p);
		}
		context.getSettings().SHOW_FAVORITES.set(true);
		FavoriteGroup group = getOrCreateGroup(p, 0);

		if (!p.getName().isEmpty()) {
			p.setVisible(group.visible);
			if (SpecialPointType.PARKING == p.getSpecialPointType()) {
				p.setColor(ContextCompat.getColor(context, R.color.parking_icon_background));
			} else {
				if (p.getColor() == 0) {
					p.setColor(group.color);
				}
			}
			group.points.add(p);
			cachedFavoritePoints.add(p);
		}
		if (saveImmediately) {
			sortAll();
			saveCurrentPointsIntoFile();
		}
		runSyncWithMarkers(group);

		return true;
	}

	public void lookupAddress(@NonNull final FavouritePoint p) {
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
							editAddressDescription(p, address);
							context.runInUIThread(new Runnable() {
								@Override
								public void run() {
									for (FavoritesListener listener : listeners) {
										listener.onFavoriteDataUpdated(p);
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

	public static AlertDialog.Builder checkDuplicates(FavouritePoint p, FavouritesDbHelper fdb, Activity activity) {
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
				if (fp.getName().equals(name) && p.getLatitude() != fp.getLatitude() && p.getLongitude() != fp.getLongitude() && fp.getCategory().equals(p.getCategory())) {
					number++;
					index = " (" + number + ")";
					name = p.getName() + index;
					fl = true;
					break;
				}
			}
		}
		if ((index.length() > 0 || emoticons)) {
			OsmandApplication app = (OsmandApplication) activity.getApplication();
			boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
			Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
			AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
			builder.setTitle(R.string.fav_point_dublicate);
			if (emoticons) {
				builder.setMessage(activity.getString(R.string.fav_point_emoticons_message, name));
			} else {
				builder.setMessage(activity.getString(R.string.fav_point_dublicate_message, name));
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

	public boolean editFavouriteName(FavouritePoint p, String newName, String category, String descr, String address) {
		String oldCategory = p.getCategory();
		p.setName(newName);
		p.setCategory(category);
		p.setDescription(descr);
		p.setAddress(address);
		if (!oldCategory.equals(category)) {
			FavoriteGroup old = flatGroups.get(oldCategory);
			if (old != null) {
				old.points.remove(p);
			}
			FavoriteGroup pg = getOrCreateGroup(p, 0);
			p.setVisible(pg.visible);
			if (SpecialPointType.PARKING == p.getSpecialPointType()) {
				p.setColor(ContextCompat.getColor(context, R.color.parking_icon_background));
			} else {
				if (p.getColor() == 0) {
					p.setColor(pg.color);
				}
			}
			pg.points.add(p);
		}
		sortAll();
		saveCurrentPointsIntoFile();
		runSyncWithMarkers(getOrCreateGroup(p, 0));
		return true;
	}

	private void editAddressDescription(@NonNull FavouritePoint p, @Nullable String address) {
		p.setAddress(address);
		saveCurrentPointsIntoFile();
		runSyncWithMarkers(getOrCreateGroup(p, 0));
	}

	public boolean editFavourite(@NonNull FavouritePoint p, double lat, double lon) {
		return editFavourite(p, lat, lon, null);
	}

	public boolean favouritePassed(@NonNull FavouritePoint point, boolean passed, boolean saveImmediately) {
		point.setPassedTimestamp(passed ? System.currentTimeMillis() : 0);
		if (saveImmediately) {
			saveCurrentPointsIntoFile();
		}
		FavoriteGroup group = getOrCreateGroup(point, 0);
		runSyncWithMarkers(group);
		return true;
	}

	private boolean editFavourite(@NonNull FavouritePoint p, double lat, double lon, @Nullable String description) {
		cancelAddressRequest(p);
		p.setLatitude(lat);
		p.setLongitude(lon);
		p.initAltitude(context);
		if (description != null) {
			p.setDescription(description);
		}
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

	public static void backup(File backupFile, File externalFile) {
		try {
			File f = new File(backupFile.getParentFile(), backupFile.getName());
			BZip2CompressorOutputStream out = new BZip2CompressorOutputStream(new FileOutputStream(f));
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
		return getBackupFile(context, "favourites_bak_");
	}

	public static File getBackupFile(OsmandApplication app, String fileName) {
		File fld = new File(app.getAppPath(null), BACKUP_FOLDER);
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
			File bak = new File(fld, fileName + backPrefix + ".gpx.bz2");
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

	public GPXFile asGpxFile(List<FavouritePoint> favoritePoints) {
		GPXFile gpx = new GPXFile(Version.getFullVersion(context));
		for (FavouritePoint p : favoritePoints) {
			context.getSelectedGpxHelper().addPoint(p.toWpt(context), gpx);
		}
		return gpx;
	}

	private void addEmptyCategory(String name) {
		addEmptyCategory(name, 0, true);
	}

	public void addEmptyCategory(String name, int color) {
		addEmptyCategory(name, color, true);
	}

	public void addEmptyCategory(String name, int color, boolean visible) {
		FavoriteGroup group = new FavoriteGroup();
		group.name = FavoriteGroup.convertDisplayNameToGroupIdName(context, name);
		group.color = color;
		group.visible = visible;
		favoriteGroups.add(group);
		flatGroups.put(group.name, group);
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

	public boolean isGroupVisible(String name) {
		String nameLowercase = name.toLowerCase();
		for (Map.Entry<String, FavoriteGroup> entry : flatGroups.entrySet()) {
			String groupName = entry.getKey();
			if (groupName.toLowerCase().equals(nameLowercase) || FavoriteGroup.getDisplayName(context, groupName).equals(name)) {
				return entry.getValue().isVisible();
			}
		}
		return false;
	}

	public boolean groupExists(String name) {
		String nameLowercase = name.toLowerCase();
		for (String groupName : flatGroups.keySet()) {
			if (groupName.toLowerCase().equals(nameLowercase) || FavoriteGroup.getDisplayName(context, groupName).equals(name)) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	public FavoriteGroup getGroup(FavouritePoint p) {
		if (p != null && flatGroups.containsKey(p.getCategory())) {
			return flatGroups.get(p.getCategory());
		} else {
			return null;
		}
	}

	@Nullable
	public FavoriteGroup getGroup(String nameId) {
		if (flatGroups.containsKey(nameId)) {
			return flatGroups.get(nameId);
		} else {
			return null;
		}
	}

	@Nullable
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
		for (FavoriteGroup f : favoriteGroups) {
			allPoints.addAll(f.points);
		}
		cachedFavoritePoints = allPoints;
	}

	public void sortAll() {
		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		Collections.sort(favoriteGroups, new Comparator<FavoriteGroup>() {

			@Override
			public int compare(FavoriteGroup lhs, FavoriteGroup rhs) {
				return lhs.isPersonal() ? -1 : rhs.isPersonal() ? 1 : collator.compare(lhs.name, rhs.name);
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
		return new Comparator<FavouritePoint>() {

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
	}

	private boolean loadGPXFile(File file, Map<String, FavouritePoint> points) {
		if (!file.exists()) {
			return false;
		}
		GPXFile res = GPXUtilities.loadGPXFile(file);
		if (res.error != null) {
			return false;
		}
		for (WptPt wptPt : res.getPoints()) {
			FavouritePoint favouritePoint = FavouritePoint.fromWpt(wptPt, context);
			points.put(getKey(favouritePoint), favouritePoint);
		}
		return true;
	}

	public void editFavouriteGroup(FavoriteGroup group, String newName, int color, boolean visible) {
		if (color != 0 && group.color != color) {
			FavoriteGroup gr = flatGroups.get(group.name);
			for (FavouritePoint p : gr.points) {
				if (p.getColor() == group.color) {
					p.setColor(color);
				}
			}
			group.color = color;
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
			db.execSQL("UPDATE " + FAVOURITE_TABLE_NAME + " SET category = ?", new Object[] {""}); //$NON-NLS-1$ //$NON-NLS-2$
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
							if (!name.isEmpty()) {
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
		if (p.getName().isEmpty() && flatGroups.containsKey(p.getCategory())) {
			return true;
		}
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL(
						"INSERT INTO " + FAVOURITE_TABLE_NAME + " (" + FAVOURITE_COL_NAME + ", " + FAVOURITE_COL_CATEGORY + ", "
								+ FAVOURITE_COL_LAT + ", " + FAVOURITE_COL_LON + ")" + " VALUES (?, ?, ?, ?)", new Object[]{p.getName(), p.getCategory(), p.getLatitude(), p.getLongitude()}); //$NON-NLS-1$ //$NON-NLS-2$
				FavoriteGroup group = getOrCreateGroup(p, 0);
				if (!p.getName().isEmpty()) {
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
