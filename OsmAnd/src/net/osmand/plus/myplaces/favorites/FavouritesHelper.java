package net.osmand.plus.myplaces.favorites;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.gpx.GPXUtilities.DEFAULT_ICON_NAME;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.BackgroundType;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.SpecialPointType;
import net.osmand.gpx.GPXUtilities.PointsGroup;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.favorites.SaveFavoritesTask.SaveFavoritesListener;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class FavouritesHelper {

	private static final Log log = PlatformUtil.getLog(FavouritesHelper.class);

	private final OsmandApplication app;
	private final FavouritesFileHelper fileHelper;

	private List<FavoriteGroup> favoriteGroups = new ArrayList<>();
	private Map<String, FavoriteGroup> flatGroups = new LinkedHashMap<>();
	private List<FavouritePoint> cachedFavoritePoints = new ArrayList<>();

	private final Set<FavoritesListener> listeners = new HashSet<>();
	private final Map<FavouritePoint, AddressLookupRequest> addressRequestMap = new ConcurrentHashMap<>();

	private boolean favoritesLoaded;
	private long lastModifiedTime;

	public FavouritesHelper(@NonNull OsmandApplication app) {
		this.app = app;
		fileHelper = new FavouritesFileHelper(app);
	}

	public long getLastUploadedTime() {
		return app.getSettings().FAVORITES_LAST_UPLOADED_TIME.get();
	}

	public void setLastUploadedTime(long time) {
		app.getSettings().FAVORITES_LAST_UPLOADED_TIME.set(time);
	}

	@NonNull
	public FavouritesFileHelper getFileHelper() {
		return fileHelper;
	}

	@NonNull
	public List<FavoriteGroup> getFavoriteGroups() {
		return favoriteGroups;
	}

	@NonNull
	public List<FavouritePoint> getFavouritePoints() {
		return new ArrayList<>(cachedFavoritePoints);
	}

	@Nullable
	public Drawable getColoredIconForGroup(@NonNull String groupName) {
		String groupIdName = FavoriteGroup.convertDisplayNameToGroupIdName(app, groupName);
		FavoriteGroup favoriteGroup = getGroup(groupIdName);
		if (favoriteGroup != null) {
			int color = favoriteGroup.getColor() == 0 ? ContextCompat.getColor(app, R.color.color_favorite) : favoriteGroup.getColor();
			return app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_group_name_16, color);
		}
		return null;
	}

	public int getColorWithCategory(@NonNull FavouritePoint point, int defaultColor) {
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
		Map<String, FavoriteGroup> groups = fileHelper.loadInternalGroups();
		Map<String, FavoriteGroup> extGroups = fileHelper.loadExternalGroups();

		boolean changed = merge(extGroups, groups);

		flatGroups = groups;
		favoriteGroups = new ArrayList<>(groups.values());

		recalculateCachedFavPoints();
		sortAll();

		File legacyExternalFile = fileHelper.getLegacyExternalFile();
		// Force save favorites to file if internals are different from externals
		// or no favorites created yet or legacy favourites.gpx present
		if (changed || !fileHelper.getExternalDir().exists() || legacyExternalFile.exists()) {
			saveCurrentPointsIntoFile(false);
			// Delete legacy favourites.gpx if exists
			if (legacyExternalFile.exists()) {
				legacyExternalFile.delete();
			}
		} else {
			updateLastModifiedTime();
		}
		favoritesLoaded = true;
		notifyListeners();
	}

	public long getLastModifiedTime() {
		MapMarkersHelper mapMarkersHelper = app.getMapMarkersHelper();
		if (mapMarkersHelper != null) {
			return Math.max(lastModifiedTime, mapMarkersHelper.getFavoriteMarkersModifiedTime());
		} else {
			return lastModifiedTime;
		}
	}

	private void updateLastModifiedTime() {
		lastModifiedTime = System.currentTimeMillis();
	}

	public void fixBlackBackground() {
		flatGroups = new LinkedHashMap<>();
		favoriteGroups = new ArrayList<>();
		for (FavouritePoint fp : getFavouritePoints()) {
			if (fp.getColor() == 0xFF000000 || fp.getColor() == ContextCompat.getColor(app, R.color.color_favorite)) {
				fp.setColor(0);
			}
			if (fp.getBackgroundType() == FavouritePoint.DEFAULT_BACKGROUND_TYPE) {
				fp.setBackgroundType(null);
			}
			if (fp.getIconIdOrDefault() == FavouritePoint.DEFAULT_UI_ICON_ID) {
				fp.setIconId(0);
			}
			FavoriteGroup group = getOrCreateGroup(fp);
			group.getPoints().add(fp);
		}
		sortAll();
		saveCurrentPointsIntoFile(false);
		notifyListeners();
	}

	private void notifyListeners() {
		app.runInUIThread(() -> {
			for (FavoritesListener listener : listeners) {
				listener.onFavoritesLoaded();
			}
		});
	}

	@Nullable
	public FavouritePoint getSpecialPoint(SpecialPointType pointType) {
		for (FavouritePoint point : getFavouritePoints()) {
			if (point.getSpecialPointType() == pointType) {
				return point;
			}
		}
		return null;
	}

	public boolean isFavoritesLoaded() {
		return favoritesLoaded;
	}

	public void addListener(@NonNull FavoritesListener listener) {
		listeners.add(listener);
	}

	public void removeListener(@NonNull FavoritesListener listener) {
		listeners.remove(listener);
	}

	private boolean merge(Map<String, FavoriteGroup> source, Map<String, FavoriteGroup> destination) {
		boolean changed = false;
		for (Map.Entry<String, FavoriteGroup> entry : source.entrySet()) {
			String key = entry.getKey();
			FavoriteGroup sourceGroup = entry.getValue();
			FavoriteGroup destinationGroup = destination.get(key);

			if (destinationGroup == null) {
				changed = true;
				destinationGroup = new FavoriteGroup(sourceGroup);
				destination.put(key, destinationGroup);
			} else {
				List<FavouritePoint> points = destinationGroup.getPoints();
				Map<String, FavouritePoint> pointsMap = new HashMap<>();
				for (FavouritePoint point : points) {
					pointsMap.put(point.getKey(), point);
				}
				for (FavouritePoint point : sourceGroup.getPoints()) {
					if (!pointsMap.containsKey(point.getKey())) {
						changed = true;
						points.add(point);
					}
				}
			}
		}
		return changed;
	}

	private void runSyncWithMarkers(FavoriteGroup favGroup) {
		MapMarkersHelper helper = app.getMapMarkersHelper();
		MapMarkersGroup group = helper.getMarkersGroup(favGroup);
		if (group != null) {
			helper.runSynchronization(group);
		}
	}

	private boolean removeFromMarkers(FavoriteGroup favGroup) {
		MapMarkersHelper helper = app.getMapMarkersHelper();
		MapMarkersGroup group = helper.getMarkersGroup(favGroup);
		if (group != null) {
			helper.removeMarkersGroup(group);
			return true;
		}
		return false;
	}

	private void addToMarkers(FavoriteGroup favGroup) {
		MapMarkersHelper helper = app.getMapMarkersHelper();
		helper.addOrEnableGroup(favGroup);
	}

	public void delete(@Nullable Set<FavoriteGroup> groupsToDelete, @Nullable Set<FavouritePoint> favoritesSelected) {
		if (!Algorithms.isEmpty(favoritesSelected)) {
			Set<FavoriteGroup> groupsToSync = new HashSet<>();
			for (FavouritePoint point : favoritesSelected) {
				FavoriteGroup group = flatGroups.get(point.getCategory());
				if (group != null) {
					group.getPoints().remove(point);
					groupsToSync.add(group);
				}
				if (point.isHomeOrWork()) {
					app.getLauncherShortcutsHelper().updateLauncherShortcuts();
				}
				removeFavouritePoint(point);
			}
			for (FavoriteGroup group : groupsToSync) {
				runSyncWithMarkers(group);
			}
		}
		if (!Algorithms.isEmpty(groupsToDelete)) {
			Map<String, FavoriteGroup> tmpFlatGroups = new LinkedHashMap<>(flatGroups);
			ArrayList<FavoriteGroup> tmpFavoriteGroups = new ArrayList<>(favoriteGroups);
			for (FavoriteGroup g : groupsToDelete) {
				tmpFlatGroups.remove(g.getName());
				tmpFavoriteGroups.remove(g);
				removeFavouritePoints(g.getPoints());
				removeFromMarkers(g);
				if (g.isPersonal()) {
					app.getLauncherShortcutsHelper().updateLauncherShortcuts();
				}
			}
			flatGroups = tmpFlatGroups;
			favoriteGroups = tmpFavoriteGroups;
		}
		saveCurrentPointsIntoFile(true);
	}

	public boolean deleteFavourite(FavouritePoint point) {
		return deleteFavourite(point, true);
	}

	public boolean deleteFavourite(FavouritePoint p, boolean saveImmediately) {
		if (p != null) {
			FavoriteGroup group = flatGroups.get(p.getCategory());
			if (group != null) {
				group.getPoints().remove(p);
				runSyncWithMarkers(group);
			}
			removeFavouritePoint(p);
			if (p.isHomeOrWork()) {
				app.getLauncherShortcutsHelper().updateLauncherShortcuts();
			}
		}
		if (saveImmediately) {
			saveCurrentPointsIntoFile(false);
		}
		return true;
	}

	public void setParkingPoint(@NonNull LatLon latLon, @Nullable String address, long pickupTimestamp, boolean addToCalendar) {
		SpecialPointType specialType = SpecialPointType.PARKING;
		FavouritePoint point = getSpecialPoint(specialType);
		if (point != null) {
			point.setIconId(specialType.getIconId(app));
			point.setPickupDate(pickupTimestamp);
			point.setCalendarEvent(addToCalendar);
			point.setTimestamp(System.currentTimeMillis());
			editFavourite(point, latLon.getLatitude(), latLon.getLongitude(), address);
			lookupAddress(point);
		} else {
			point = new FavouritePoint(latLon.getLatitude(), latLon.getLongitude(), specialType.getName(), specialType.getCategory());
			point.setAddress(address);
			point.setPickupDate(pickupTimestamp);
			point.setCalendarEvent(addToCalendar);
			point.setIconId(specialType.getIconId(app));
			addFavourite(point);
		}
	}

	public void setSpecialPoint(@NonNull LatLon latLon, SpecialPointType specialType, @Nullable String address) {
		FavouritePoint point = getSpecialPoint(specialType);
		if (point != null) {
			point.setIconId(specialType.getIconId(app));
			editFavourite(point, latLon.getLatitude(), latLon.getLongitude(), address);
			lookupAddress(point);
		} else {
			point = new FavouritePoint(latLon.getLatitude(), latLon.getLongitude(), specialType.getName(), specialType.getCategory());
			point.setAddress(address);
			point.setIconId(specialType.getIconId(app));
			addFavourite(point);
		}
	}

	public boolean addFavourite(@NonNull FavouritePoint point) {
		return addFavourite(point, true, true, true, null);
	}

	public boolean addFavourite(@NonNull FavouritePoint point, boolean lookupAddress, boolean sortAndSave,
	                            boolean saveAsync, @Nullable PointsGroup pointsGroup) {
		if (Double.isNaN(point.getAltitude()) || point.getAltitude() == 0) {
			initAltitude(point);
		}
		if (point.getName().isEmpty() && flatGroups.containsKey(point.getCategory())) {
			return true;
		}
		if (lookupAddress && !point.isAddressSpecified()) {
			lookupAddress(point);
		}
		app.getSettings().SHOW_FAVORITES.set(true);

		FavoriteGroup group = getOrCreateGroup(point, pointsGroup);
		if (!point.getName().isEmpty()) {
			point.setVisible(group.isVisible());
			if (SpecialPointType.PARKING == point.getSpecialPointType()) {
				point.setColor(ContextCompat.getColor(app, R.color.parking_icon_background));
			} else if (point.getColor() == 0) {
				point.setColor(group.getColor());
			}
			group.getPoints().add(point);
			addFavouritePoint(point);
		}
		if (sortAndSave) {
			sortAll();
			saveCurrentPointsIntoFile(saveAsync);
		}

		runSyncWithMarkers(group);
		if (point.isHomeOrWork()) {
			app.getLauncherShortcutsHelper().updateLauncherShortcuts();
		}

		return true;
	}

	public void lookupAddress(@NonNull FavouritePoint point) {
		AddressLookupRequest request = addressRequestMap.get(point);
		double latitude = point.getLatitude();
		double longitude = point.getLongitude();
		if (request == null || !request.getLatLon().equals(new LatLon(latitude, longitude))) {
			cancelAddressRequest(point);
			request = new AddressLookupRequest(new LatLon(latitude, longitude), address -> {
				addressRequestMap.remove(point);
				editAddressDescription(point, address);
				app.runInUIThread(() -> {
					for (FavoritesListener listener : listeners) {
						listener.onFavoriteDataUpdated(point);
					}
				});
			}, null);
			addressRequestMap.put(point, request);
			app.getGeocodingLookupService().lookupAddress(request);
		}
	}

	private void cancelAddressRequest(@NonNull FavouritePoint point) {
		AddressLookupRequest request = addressRequestMap.get(point);
		if (request != null) {
			app.getGeocodingLookupService().cancel(request);
			addressRequestMap.remove(point);
		}
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
				old.getPoints().remove(p);
			}
			FavoriteGroup pg = getOrCreateGroup(p);
			p.setVisible(pg.isVisible());
			if (SpecialPointType.PARKING == p.getSpecialPointType()) {
				p.setColor(ContextCompat.getColor(app, R.color.parking_icon_background));
			} else {
				if (p.getColor() == 0) {
					p.setColor(pg.getColor());
				}
			}
			pg.getPoints().add(p);
		}
		sortAll();
		saveCurrentPointsIntoFile(true);
		runSyncWithMarkers(getOrCreateGroup(p));
		return true;
	}

	private void editAddressDescription(@NonNull FavouritePoint p, @Nullable String address) {
		p.setAddress(address);
		saveCurrentPointsIntoFile(true);
		runSyncWithMarkers(getOrCreateGroup(p));
	}

	public boolean editFavourite(@NonNull FavouritePoint p, double lat, double lon) {
		return editFavourite(p, lat, lon, null);
	}

	public boolean favouritePassed(@NonNull FavouritePoint point, boolean passed, boolean saveImmediately) {
		point.setVisitedDate(passed ? System.currentTimeMillis() : 0);
		if (saveImmediately) {
			saveCurrentPointsIntoFile(false);
		}
		FavoriteGroup group = getOrCreateGroup(point);
		runSyncWithMarkers(group);
		return true;
	}

	private boolean editFavourite(@NonNull FavouritePoint point, double lat, double lon, @Nullable String description) {
		cancelAddressRequest(point);
		point.setLatitude(lat);
		point.setLongitude(lon);
		initAltitude(point);
		if (description != null) {
			point.setDescription(description);
		}
		saveCurrentPointsIntoFile(true);
		runSyncWithMarkers(getOrCreateGroup(point));
		return true;
	}

	public void saveCurrentPointsIntoFile(boolean async) {
		updateLastModifiedTime();
		SaveFavoritesListener listener = this::onFavouritePropertiesUpdated;

		List<FavoriteGroup> groups = new ArrayList<>(favoriteGroups);
		if (async) {
			fileHelper.saveFavoritesIntoFile(groups, listener);
		} else {
			fileHelper.saveFavoritesIntoFileSync(groups, listener);
		}
	}

	public boolean deleteGroup(@NonNull FavoriteGroup group, boolean saveImmediately) {
		List<FavoriteGroup> tmpFavoriteGroups = new ArrayList<>(favoriteGroups);
		boolean remove = tmpFavoriteGroups.remove(group);
		if (remove) {
			favoriteGroups = tmpFavoriteGroups;
			Map<String, FavoriteGroup> tmpFlatGroups = new LinkedHashMap<>(flatGroups);
			tmpFlatGroups.remove(group.getName());
			flatGroups = tmpFlatGroups;
			if (saveImmediately) {
				saveCurrentPointsIntoFile(false);
			}
			removeFromMarkers(group);
			return true;
		}
		return false;
	}

	public FavoriteGroup addFavoriteGroup(@NonNull String name, int color) {
		return addFavoriteGroup(name, color, DEFAULT_ICON_NAME, DEFAULT_BACKGROUND_TYPE);
	}

	public FavoriteGroup addFavoriteGroup(@NonNull String name, int color, @NonNull String iconName, @NonNull BackgroundType backgroundType) {
		FavoriteGroup group = new FavoriteGroup();
		group.setName(FavoriteGroup.convertDisplayNameToGroupIdName(app, name));
		group.setColor(color);
		group.setIconName(iconName);
		group.setBackgroundType(backgroundType);
		favoriteGroups = CollectionUtils.addToList(favoriteGroups, group);
		Map<String, FavoriteGroup> tmpFlatGroups = new LinkedHashMap<>(flatGroups);
		tmpFlatGroups.put(group.getName(), group);
		flatGroups = tmpFlatGroups;
		return group;
	}

	public void initAltitude(@NonNull FavouritePoint point) {
		initAltitude(point, null);
	}

	public void initAltitude(@NonNull FavouritePoint point, @Nullable Runnable callback) {
		Location location = new Location("", point.getLatitude(), point.getLongitude());
		app.getLocationProvider().getRouteSegment(location, null, false,
				new ResultMatcher<RouteDataObject>() {

					@Override
					public boolean publish(RouteDataObject routeDataObject) {
						if (routeDataObject != null) {
							LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
							routeDataObject.calculateHeightArray(latLon);
							point.setAltitude(routeDataObject.heightByCurrentLocation);
						}
						if (callback != null) {
							callback.run();
						}
						return true;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				});
	}

	@NonNull
	public List<FavouritePoint> getVisibleFavouritePoints() {
		List<FavouritePoint> points = new ArrayList<>();
		for (FavouritePoint point : getFavouritePoints()) {
			if (point.isVisible()) {
				points.add(point);
			}
		}
		return points;
	}

	@Nullable
	public FavouritePoint getVisibleFavByLatLon(@NonNull LatLon latLon) {
		for (FavouritePoint point : getFavouritePoints()) {
			if (point.isVisible() && latLon.equals(new LatLon(point.getLatitude(), point.getLongitude()))) {
				return point;
			}
		}
		return null;
	}

	public boolean isGroupVisible(@NonNull String name) {
		String nameLowercase = name.toLowerCase();
		for (Map.Entry<String, FavoriteGroup> entry : flatGroups.entrySet()) {
			String groupName = entry.getKey();
			if (groupName.toLowerCase().equals(nameLowercase) || FavoriteGroup.getDisplayName(app, groupName).equals(name)) {
				return entry.getValue().isVisible();
			}
		}
		return false;
	}

	public boolean groupExists(String name) {
		String nameLowercase = name.toLowerCase();
		for (String groupName : flatGroups.keySet()) {
			if (groupName.toLowerCase().equals(nameLowercase) || FavoriteGroup.getDisplayName(app, groupName).equals(name)) {
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

	private void addFavouritePoint(@NonNull FavouritePoint point) {
		cachedFavoritePoints = CollectionUtils.addToList(cachedFavoritePoints, point);
	}

	private void removeFavouritePoint(@NonNull FavouritePoint point) {
		cachedFavoritePoints = CollectionUtils.removeFromList(cachedFavoritePoints, point);
	}

	private void removeFavouritePoints(@NonNull List<FavouritePoint> points) {
		cachedFavoritePoints = CollectionUtils.removeAllFromList(cachedFavoritePoints, points);
	}

	public void recalculateCachedFavPoints() {
		List<FavouritePoint> allPoints = new ArrayList<>();
		for (FavoriteGroup f : favoriteGroups) {
			allPoints.addAll(f.getPoints());
		}
		cachedFavoritePoints = new ArrayList<>(allPoints);
	}

	public void sortAll() {
		Collator collator = getCollator();
		ArrayList<FavoriteGroup> tmpFavoriteGroups = new ArrayList<>(favoriteGroups);
		Collections.sort(tmpFavoriteGroups, (lhs, rhs) -> lhs.isPersonal() ? -1 : rhs.isPersonal() ? 1 : collator.compare(lhs.getName(), rhs.getName()));
		Comparator<FavouritePoint> favoritesComparator = getComparator();
		for (FavoriteGroup group : tmpFavoriteGroups) {
			ArrayList<FavouritePoint> points = new ArrayList<>(group.getPoints());
			Collections.sort(points, favoritesComparator);
			group.setPoints(points);
		}
		favoriteGroups = tmpFavoriteGroups;
		ArrayList<FavouritePoint> tmpCechPoints = new ArrayList<>(cachedFavoritePoints);
		Collections.sort(tmpCechPoints, favoritesComparator);
		cachedFavoritePoints = tmpCechPoints;
	}

	@NonNull
	private static Collator getCollator() {
		Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		return collator;
	}

	@NonNull
	private static Comparator<FavouritePoint> getComparator() {
		Collator collator = getCollator();
		return (o1, o2) -> {
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
		};
	}

	public void updateGroupColor(@NonNull FavoriteGroup group, int color, boolean updatePoints, boolean saveImmediately) {
		if (updatePoints) {
			for (FavouritePoint point : group.getPoints()) {
				point.setColor(color);
			}
		}
		group.setColor(color);
		runSyncWithMarkers(group);
		if (saveImmediately) {
			saveCurrentPointsIntoFile(false);
		}
	}

	public void updateGroupIconName(@NonNull FavoriteGroup group, @NonNull String iconName,
	                                boolean updatePoints, boolean saveImmediately) {
		if (updatePoints) {
			for (FavouritePoint point : group.getPoints()) {
				point.setIconIdFromName(iconName);
			}
		}
		group.setIconName(iconName);
		runSyncWithMarkers(group);
		if (saveImmediately) {
			saveCurrentPointsIntoFile(false);
		}
	}

	public void updateGroupBackgroundType(@NonNull FavoriteGroup group, @NonNull BackgroundType backgroundType,
	                                      boolean updatePoints, boolean saveImmediately) {
		if (updatePoints) {
			for (FavouritePoint point : group.getPoints()) {
				point.setBackgroundType(backgroundType);
			}
		}
		group.setBackgroundType(backgroundType);
		runSyncWithMarkers(group);
		if (saveImmediately) {
			saveCurrentPointsIntoFile(false);
		}
	}

	public void updateGroupVisibility(@NonNull FavoriteGroup group, boolean visible, boolean saveImmediately) {
		if (group.isVisible() != visible) {
			for (FavouritePoint point : group.getPoints()) {
				point.setVisible(visible);
			}
			group.setVisible(visible);
			runSyncWithMarkers(group);
		}
		if (saveImmediately) {
			saveCurrentPointsIntoFile(false);
		}
	}

	public void updateGroupName(@NonNull FavoriteGroup group, @NonNull String newName, boolean saveImmediately) {
		if (!Algorithms.stringsEqual(group.getName(), newName)) {
			flatGroups.remove(group.getName());
			boolean isInMarkers = removeFromMarkers(group);

			group.setName(newName);
			FavoriteGroup renamedGroup = flatGroups.get(group.getName());
			boolean existing = renamedGroup != null;
			if (renamedGroup == null) {
				renamedGroup = group;
				Map<String, FavoriteGroup> tmpFlatGroups = new LinkedHashMap<>(flatGroups);
				tmpFlatGroups.put(group.getName(), group);
				flatGroups = tmpFlatGroups;
			} else {
				favoriteGroups = CollectionUtils.removeFromList(favoriteGroups, group);
			}
			for (FavouritePoint point : group.getPoints()) {
				point.setCategory(newName);
			}
			if (existing) {
				renamedGroup.getPoints().addAll(group.getPoints());
			}
			if (isInMarkers) {
				addToMarkers(renamedGroup);
			}
		}
		if (saveImmediately) {
			saveCurrentPointsIntoFile(false);
		}
	}

	@NonNull
	private FavoriteGroup getOrCreateGroup(@NonNull FavouritePoint point) {
		return getOrCreateGroup(point, null);
	}

	@NonNull
	private FavoriteGroup getOrCreateGroup(@NonNull FavouritePoint point, @Nullable PointsGroup pointsGroup) {
		FavoriteGroup favoriteGroup = flatGroups.get(point.getCategory());
		if (favoriteGroup == null) {
			favoriteGroup = new FavoriteGroup(point);
			Map<String, FavoriteGroup> tmpFlatGroups = new LinkedHashMap<>(flatGroups);
			tmpFlatGroups.put(favoriteGroup.getName(), favoriteGroup);
			flatGroups = tmpFlatGroups;
			favoriteGroups = CollectionUtils.addToList(favoriteGroups, favoriteGroup);
		}
		updateGroupAppearance(favoriteGroup, pointsGroup);

		return favoriteGroup;
	}

	private void updateGroupAppearance(@Nullable FavoriteGroup favoriteGroup, @Nullable PointsGroup pointsGroup) {
		if (favoriteGroup != null && pointsGroup != null) {
			favoriteGroup.setColor(pointsGroup.color);
			favoriteGroup.setIconName(pointsGroup.iconName);
			favoriteGroup.setBackgroundType(BackgroundType.getByTypeName(pointsGroup.backgroundType, DEFAULT_BACKGROUND_TYPE));
		}
	}

	private void onFavouritePropertiesUpdated() {
		for (FavoritesListener listener : listeners) {
			listener.onFavoritePropertiesUpdated();
		}
	}

	@NonNull
	public static List<FavouritePoint> getPointsFromGroups(@NonNull List<FavoriteGroup> groups) {
		List<FavouritePoint> favouritePoints = new ArrayList<>();
		for (FavoriteGroup group : groups) {
			favouritePoints.addAll(group.getPoints());
		}
		return favouritePoints;
	}
}