package net.osmand.plus.myplaces.favorites;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.plus.myplaces.favorites.SaveOption.APPLY_TO_ALL;
import static net.osmand.plus.myplaces.favorites.SaveOption.APPLY_TO_NEW;
import static net.osmand.shared.gpx.GpxUtilities.DEFAULT_ICON_NAME;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
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
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.favorites.SaveFavoritesTask.SaveFavoritesListener;
import net.osmand.plus.myplaces.favorites.add.AddFavoriteOptions;
import net.osmand.plus.myplaces.favorites.add.AddFavoriteResult;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.parking.ParkingPositionPlugin;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.logging.Log;

import java.io.File;
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
		FavoriteGroup favoriteGroup = getGroup(point);
		int groupColor = favoriteGroup != null ? favoriteGroup.getColor() : 0;
		return getColorWithCategory(point.getColor(), groupColor, defaultColor);
	}

	public int getColorWithCategory(@ColorInt int pointColor, @ColorInt int groupColor, @ColorInt int defaultColor) {
		if (pointColor != 0) {
			return pointColor;
		}
		if (groupColor != 0) {
			return groupColor;
		}
		return defaultColor;
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

	private boolean merge(@NonNull Map<String, FavoriteGroup> source, @NonNull Map<String, FavoriteGroup> destination) {
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
				boolean groupChanged = false;
				if (!destinationGroup.appearanceEquals(sourceGroup)) {
					groupChanged = true;
					destinationGroup.copyAppearance(sourceGroup);
				}
				Map<String, FavouritePoint> destPointsMap = new LinkedHashMap<>();
				for (FavouritePoint point : destinationGroup.getPoints()) {
					destPointsMap.put(point.getKey(), point);
				}
				for (FavouritePoint sourcePoint : sourceGroup.getPoints()) {
					String pointKey = sourcePoint.getKey();
					FavouritePoint destPoint = destPointsMap.get(pointKey);
					if (destPoint == null) {
						groupChanged = true;
						destPointsMap.put(pointKey, sourcePoint);
					} else if (!destPoint.appearanceEquals(sourcePoint)) {
						groupChanged = true;
						destPoint.copyAppearance(sourcePoint);
					}
				}
				if (groupChanged) {
					changed = true;
					destinationGroup.setPoints(new ArrayList<>(destPointsMap.values()));
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
			saveCurrentPointsIntoFile(true);
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

	public void copyToFavorites(@NonNull GpxDisplayGroup displayGroup, @NonNull String groupName) {
		ParkingPositionPlugin plugin = PluginsHelper.getPlugin(ParkingPositionPlugin.class);
		FavouritesHelper favouritesHelper = app.getFavoritesHelper();

		List<FavouritePoint> addedPoints = new ArrayList<>();
		List<FavouritePoint> duplicatePoints = new ArrayList<>();
		AddFavoriteOptions options = new AddFavoriteOptions().setLookupAddress(true);

		for (GpxDisplayItem item : displayGroup.getDisplayItems()) {
			if (item.locationStart != null) {
				FavouritePoint point = FavouritePoint.fromWpt(item.locationStart, groupName);
				if (!Algorithms.isEmpty(item.description)) {
					point.setDescription(item.description);
				}
				if (plugin != null && point.getSpecialPointType() == SpecialPointType.PARKING) {
					plugin.updateParkingPoint(point);
				}
				switch (favouritesHelper.addFavourite(point, options)) {
					case ADDED -> addedPoints.add(point);
					case DUPLICATE -> duplicatePoints.add(point);
				}
			}
		}
		favouritesHelper.saveCurrentPointsIntoFile(true);

		if (!addedPoints.isEmpty()) {
			app.showShortToastMessage(R.string.msg_gpx_waypoints_copied_to_favorites, addedPoints.size());
		}
		if (!duplicatePoints.isEmpty()) {
			app.showShortToastMessage(R.string.msg_favorites_skipped_as_existing, duplicatePoints.size());
		}
	}

	public boolean addFavourite(@NonNull FavouritePoint point) {
		return addFavourite(point, new AddFavoriteOptions().enableAll()) == AddFavoriteResult.ADDED;
	}

	@NonNull
	public AddFavoriteResult addFavourite(@NonNull FavouritePoint point, @NonNull AddFavoriteOptions options) {
		return addFavourite(point, null, options);
	}

	@NonNull
	public AddFavoriteResult addFavourite(@NonNull FavouritePoint point,
	                                      @Nullable PointsGroup pointsGroup,
	                                      @NonNull AddFavoriteOptions options) {
		if (Double.isNaN(point.getAltitude()) || point.getAltitude() == 0) {
			initAltitude(point);
		}

		String pointName = point.getName();
		FavoriteGroup favoriteGroup = flatGroups.get(point.getCategory());
		if (favoriteGroup != null && pointName.isEmpty()) {
			return AddFavoriteResult.IGNORED;
		}
		if (favoriteGroup != null && favoriteGroup.containsPointByName(pointName)) {
			return AddFavoriteResult.DUPLICATE;
		}

		if (options.lookupAddress && !point.isAddressSpecified()) {
			lookupAddress(point);
		}
		app.getSettings().SHOW_FAVORITES.set(true);

		FavoriteGroup group = getOrCreateGroup(point, pointsGroup);
		if (!pointName.isEmpty()) {
			point.setVisible(group.isVisible());
			if (SpecialPointType.PARKING == point.getSpecialPointType()) {
				point.setColor(getParkingIconColor());
			} else if (point.getColor() == 0) {
				point.setColor(group.getColor());
			}
			group.getPoints().add(point);
			addFavouritePoint(point);
		}
		if (options.sortAndSave) {
			sortAll();
			saveCurrentPointsIntoFile(options.saveAsync);
		}

		runSyncWithMarkers(group);
		if (point.isHomeOrWork()) {
			app.getLauncherShortcutsHelper().updateLauncherShortcuts();
		}

		return AddFavoriteResult.ADDED;
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

	@ColorInt
	public int getParkingIconColor() {
		return ColorUtilities.getColor(app, R.color.parking_icon_background);
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
			saveCurrentPointsIntoFile(true);
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
		saveGroupsInternal(new ArrayList<>(favoriteGroups), true, async);
	}

	public void saveSelectedGroupsIntoFile(@NonNull List<FavoriteGroup> groups, boolean async) {
		saveGroupsInternal(groups, false, async);
	}

	private void saveGroupsInternal(@NonNull List<FavoriteGroup> groups, boolean saveAllGroups, boolean async) {
		updateLastModifiedTime();
		SaveFavoritesListener listener = this::onSavingFavoritesFinished;

		if (async) {
			fileHelper.saveFavoritesIntoFile(groups, saveAllGroups, listener);
		} else {
			fileHelper.saveFavoritesIntoFileSync(groups, saveAllGroups, listener);
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
				saveCurrentPointsIntoFile(true);
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
		List<FavoriteGroup> tmpFavoriteGroups = new ArrayList<>(favoriteGroups);
		Collections.sort(tmpFavoriteGroups, (lhs, rhs) -> lhs.isPersonal() ? -1
				: rhs.isPersonal() ? 1 : collator.compare(lhs.getName(), rhs.getName()));

		Comparator<FavouritePoint> comparator = new FavouritePointComparator(collator);
		for (FavoriteGroup group : tmpFavoriteGroups) {
			List<FavouritePoint> points = new ArrayList<>(group.getPoints());
			Collections.sort(points, comparator);
			group.setPoints(points);
		}
		favoriteGroups = tmpFavoriteGroups;
		List<FavouritePoint> tmpCechPoints = new ArrayList<>(cachedFavoritePoints);
		Collections.sort(tmpCechPoints, comparator);
		cachedFavoritePoints = tmpCechPoints;
	}

	@NonNull
	private static Collator getCollator() {
		Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		return collator;
	}

	public void updateGroupColor(@NonNull FavoriteGroup group, int color, @NonNull SaveOption saveOption, boolean saveImmediately) {
		if (saveOption.shouldUpdatePoints()) {
			for (FavouritePoint point : group.getPoints()) {
				point.setColor(color);
			}
		}
		if (saveOption.shouldUpdateGroup()) {
			group.setColor(color);
		}
		runSyncWithMarkers(group);
		if (saveImmediately) {
			saveCurrentPointsIntoFile(true);
		}
	}

	public void updateGroupColor(@NonNull FavoriteGroup group, int color, boolean updatePoints, boolean saveImmediately) {
		SaveOption saveOption = updatePoints ? APPLY_TO_ALL : APPLY_TO_NEW;
		updateGroupColor(group, color, saveOption, saveImmediately);
	}

	public void updateGroupIconName(@NonNull FavoriteGroup group, @NonNull String iconName,
	                                @NonNull SaveOption saveOption, boolean saveImmediately) {
		if (saveOption.shouldUpdatePoints()) {
			for (FavouritePoint point : group.getPoints()) {
				point.setIconIdFromName(iconName);
			}
		}
		if (saveOption.shouldUpdateGroup()) {
			group.setIconName(iconName);
		}
		runSyncWithMarkers(group);
		if (saveImmediately) {
			saveCurrentPointsIntoFile(true);
		}
	}

	public void updateGroupIconName(@NonNull FavoriteGroup group, @NonNull String iconName,
	                                boolean updatePoints, boolean saveImmediately) {
		SaveOption saveOption = updatePoints ? APPLY_TO_ALL : APPLY_TO_NEW;
		updateGroupIconName(group, iconName, saveOption, saveImmediately);
	}

	public void updateGroupBackgroundType(@NonNull FavoriteGroup group, @NonNull BackgroundType backgroundType,
	                                      @NonNull SaveOption saveOption, boolean saveImmediately) {
		if (saveOption.shouldUpdatePoints()) {
			for (FavouritePoint point : group.getPoints()) {
				point.setBackgroundType(backgroundType);
			}
		}
		if (saveOption.shouldUpdateGroup()) {
			group.setBackgroundType(backgroundType);
		}
		runSyncWithMarkers(group);
		if (saveImmediately) {
			saveCurrentPointsIntoFile(true);
		}
	}

	public void updateGroupBackgroundType(@NonNull FavoriteGroup group, @NonNull BackgroundType backgroundType,
	                                      boolean updatePoints, boolean saveImmediately) {
		SaveOption saveOption = updatePoints ? APPLY_TO_ALL : APPLY_TO_NEW;
		updateGroupBackgroundType(group, backgroundType, saveOption, saveImmediately);
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
			saveCurrentPointsIntoFile(true);
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
			saveCurrentPointsIntoFile(true);
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
			favoriteGroup.setColor(pointsGroup.getColor());
			favoriteGroup.setIconName(pointsGroup.getIconName());
			favoriteGroup.setBackgroundType(BackgroundType.getByTypeName(pointsGroup.getBackgroundType(), DEFAULT_BACKGROUND_TYPE));
			favoriteGroup.setVisible(!pointsGroup.isHidden());
		}
	}

	private void onSavingFavoritesFinished() {
		for (FavoritesListener listener : listeners) {
			listener.onSavingFavoritesFinished();
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

	public void doAddFavorite(String name, String category, String description, String address, @ColorInt int color,
	                          BackgroundType backgroundType, @DrawableRes int iconId, @NonNull FavouritePoint favorite) {
		favorite.setName(name);
		favorite.setCategory(category);
		favorite.setDescription(description);
		favorite.setAddress(address);
		favorite.setColor(color);
		favorite.setBackgroundType(backgroundType);
		favorite.setIconId(iconId);
		app.getSettings().LAST_FAV_CATEGORY_ENTERED.set(category);
		addFavourite(favorite);
	}
}