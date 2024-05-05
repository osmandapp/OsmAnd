package net.osmand.plus.mapmarkers;

import static net.osmand.plus.mapmarkers.ItineraryDataHelper.VISITED_DATE;
import static net.osmand.gpx.GpxParameter.SHOW_AS_MARKERS;

import android.util.Pair;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.mapmarkers.SyncGroupTask.OnGroupSyncedListener;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO rename after 4.0 MapMarkersHelper -> ItineraryHelper
public class MapMarkersHelper {

	public static final int MAP_MARKERS_COLORS_COUNT = 7;

	public static final int BY_NAME = 0;
	public static final int BY_DISTANCE_DESC = 1;
	public static final int BY_DISTANCE_ASC = 2;
	public static final int BY_DATE_ADDED_DESC = 3;
	public static final int BY_DATE_ADDED_ASC = 4;

	private static final Log LOG = PlatformUtil.getLog(MapMarkersHelper.class);

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({BY_NAME, BY_DISTANCE_DESC, BY_DISTANCE_ASC, BY_DATE_ADDED_DESC, BY_DATE_ADDED_ASC})
	public @interface MapMarkersSortByDef {
	}

	private final OsmandApplication ctx;
	private final MapMarkersDbHelper markersDbHelper;
	private final ItineraryDataHelper dataHelper;

	private final ExecutorService executorService = Executors.newSingleThreadExecutor();

	private List<MapMarker> mapMarkers = new ArrayList<>();
	private List<MapMarker> mapMarkersHistory = new ArrayList<>();
	private List<MapMarkersGroup> mapMarkersGroups = new ArrayList<>();

	private final List<MapMarkerChangedListener> listeners = new ArrayList<>();
	private final Set<OnGroupSyncedListener> syncListeners = new HashSet<>();

	private final MarkersPlanRouteContext planRouteContext;

	private long favoriteMarkersModifiedTime;
	private long trackMarkersModifiedTime;

	public List<MapMarker> getMapMarkers() {
		return mapMarkers;
	}

	public List<MapMarker> getMapMarkersHistory() {
		return mapMarkersHistory;
	}

	public List<MapMarkersGroup> getMapMarkersGroups() {
		return mapMarkersGroups;
	}

	public List<MapMarkersGroup> getVisibleMapMarkersGroups() {
		List<MapMarkersGroup> groups = new ArrayList<>();
		for (MapMarkersGroup group : mapMarkersGroups) {
			if (group.isVisible()) {
				groups.add(group);
			}
		}
		return groups;
	}

	public boolean isStartFromMyLocation() {
		return ctx.getSettings().ROUTE_MAP_MARKERS_START_MY_LOC.get();
	}

	public void setStartFromMyLocation(boolean startFromMyLocation) {
		ctx.getSettings().ROUTE_MAP_MARKERS_START_MY_LOC.set(startFromMyLocation);
	}

	public MarkersPlanRouteContext getPlanRouteContext() {
		return planRouteContext;
	}

	public MapMarkersHelper(OsmandApplication ctx) {
		this.ctx = ctx;
		dataHelper = new ItineraryDataHelper(ctx, this);
		markersDbHelper = ctx.getMapMarkersDbHelper();
		planRouteContext = new MarkersPlanRouteContext(ctx);
	}

	public long getMarkersLastModifiedTime() {
		return markersDbHelper.getMarkersLastModifiedTime();
	}

	public long getMarkersHistoryLastModifiedTime() {
		return markersDbHelper.getMarkersHistoryLastModifiedTime();
	}

	public void setMarkersLastModifiedTime(long lastModifiedTime) {
		markersDbHelper.setMarkersLastModifiedTime(lastModifiedTime);
	}

	public void setMarkersHistoryLastModifiedTime(long lastModifiedTime) {
		markersDbHelper.setMarkersHistoryLastModifiedTime(lastModifiedTime);
	}

	public void syncAllGroups() {
		Pair<Map<String, MapMarkersGroup>, Map<String, MapMarker>> pair = dataHelper.loadGroupsAndOrder();
		mapMarkers = new ArrayList<>(pair.second.values());
		mapMarkersGroups = new ArrayList<>(pair.first.values());

		boolean hasFavoriteGroup = false;
		boolean hasTrackGroup = false;
		for (MapMarkersGroup group : mapMarkersGroups) {
			updateGroup(group);
			runGroupSynchronization(group);
			if (group.getType() == ItineraryType.FAVOURITES) {
				hasFavoriteGroup = true;
			} else if (group.getType() == ItineraryType.TRACK) {
				hasTrackGroup = true;
			}
		}
		sortMarkers(mapMarkersHistory, true, BY_DATE_ADDED_DESC);
		sortGroups();
		saveGroups(false);
		lookupAddressAll();
		if (hasFavoriteGroup) {
			updateFavoriteMarkersModifiedTime();
		}
		if (hasTrackGroup) {
			updateTrackMarkersModifiedTime();
		}
	}

	public long getFavoriteMarkersModifiedTime() {
		return favoriteMarkersModifiedTime;
	}

	public long getTrackMarkersModifiedTime() {
		return trackMarkersModifiedTime;
	}

	private void updateFavoriteMarkersModifiedTime() {
		favoriteMarkersModifiedTime = System.currentTimeMillis();
	}

	private void updateTrackMarkersModifiedTime() {
		trackMarkersModifiedTime = System.currentTimeMillis();
	}

	void updateLastModifiedTime(@NonNull MapMarkersGroup group) {
		if (group.getType() == ItineraryType.FAVOURITES) {
			updateFavoriteMarkersModifiedTime();
		} else if (group.getType() == ItineraryType.TRACK) {
			updateTrackMarkersModifiedTime();
		}
	}

	public void saveGroups(boolean notify) {
		List<MapMarkersGroup> markersGroups = new ArrayList<>();
		for (MapMarkersGroup group : mapMarkersGroups) {
			if (!group.isDisabled()) {
				markersGroups.add(group);
			}
		}
		dataHelper.saveGroups(markersGroups, mapMarkers);
		if (notify) {
			notifyMarkersChanged();
		}
	}

	public ItineraryDataHelper getDataHelper() {
		return dataHelper;
	}

	protected MapMarkersDbHelper getMarkersDbHelper() {
		return markersDbHelper;
	}

	public void lookupAddressAll() {
		for (MapMarker mapMarker : mapMarkers) {
			lookupAddress(mapMarker);
		}
		for (MapMarker mapMarker : mapMarkersHistory) {
			lookupAddress(mapMarker);
		}
	}

	private void lookupAddress(MapMarker mapMarker) {
		if (mapMarker != null && mapMarker.getOriginalPointDescription().isSearchingAddress(ctx)) {
			cancelPointAddressRequests(mapMarker.point);
			AddressLookupRequest lookupRequest = new AddressLookupRequest(mapMarker.point,
					address -> {
						PointDescription pointDescription = mapMarker.getOriginalPointDescription();
						if (Algorithms.isEmpty(address)) {
							pointDescription.setName(PointDescription.getAddressNotFoundStr(ctx));
						} else {
							pointDescription.setName(address);
						}
						markersDbHelper.updateMarker(mapMarker);
						notifyMarkerChanged(mapMarker);
					}, null);
			ctx.getGeocodingLookupService().lookupAddress(lookupRequest);
		}
	}

	private void cancelAddressRequests() {
		List<MapMarker> markers = new ArrayList<>(mapMarkers);
		for (MapMarker m : markers) {
			cancelPointAddressRequests(m.point);
		}
		markers = new ArrayList<>(mapMarkersHistory);
		for (MapMarker m : markers) {
			cancelPointAddressRequests(m.point);
		}
	}

	private void cancelPointAddressRequests(LatLon latLon) {
		if (latLon != null) {
			ctx.getGeocodingLookupService().cancel(latLon);
		}
	}

	public void sortMarkers(@MapMarkersSortByDef int sortByMode, LatLon location) {
		sortMarkers(getMapMarkers(), false, sortByMode, location);
		saveGroups(false);
	}

	private void sortMarkers(List<MapMarker> markers, boolean visited, @MapMarkersSortByDef int sortByMode) {
		sortMarkers(markers, visited, sortByMode, null);
	}

	private void sortMarkers(List<MapMarker> markers,
	                         boolean visited,
	                         @MapMarkersSortByDef int sortByMode,
	                         @Nullable LatLon location) {
		Collections.sort(markers, (mapMarker1, mapMarker2) -> {
			if (sortByMode == BY_DATE_ADDED_DESC || sortByMode == BY_DATE_ADDED_ASC) {
				long t1 = visited ? mapMarker1.visitedDate : mapMarker1.creationDate;
				long t2 = visited ? mapMarker2.visitedDate : mapMarker2.creationDate;
				if (t1 > t2) {
					return sortByMode == BY_DATE_ADDED_DESC ? -1 : 1;
				} else if (t1 == t2) {
					return 0;
				} else {
					return sortByMode == BY_DATE_ADDED_DESC ? 1 : -1;
				}
			} else if (location != null && (sortByMode == BY_DISTANCE_DESC || sortByMode == BY_DISTANCE_ASC)) {
				int d1 = (int) MapUtils.getDistance(location, mapMarker1.getLatitude(), mapMarker1.getLongitude());
				int d2 = (int) MapUtils.getDistance(location, mapMarker2.getLatitude(), mapMarker2.getLongitude());
				if (d1 > d2) {
					return sortByMode == BY_DISTANCE_DESC ? -1 : 1;
				} else if (d1 == d2) {
					return 0;
				} else {
					return sortByMode == BY_DISTANCE_DESC ? 1 : -1;
				}
			} else {
				String n1 = mapMarker1.getName(ctx);
				String n2 = mapMarker2.getName(ctx);
				return n1.compareToIgnoreCase(n2);
			}
		});
	}

	public void runSynchronization(@NonNull MapMarkersGroup group) {
		ctx.runInUIThread(() -> new SyncGroupTask(ctx, group, syncListeners).executeOnExecutor(executorService));
	}

	public MapMarkersGroup getMarkersGroup(GPXFile gpx) {
		if (gpx == null || gpx.path == null) {
			return null;
		}
		return getMapMarkerGroupById(getMarkerGroupId(new File(gpx.path)), ItineraryType.TRACK);
	}

	public MapMarkersGroup getMarkersGroup(FavoriteGroup favGroup) {
		return getMapMarkerGroupById(getMarkerGroupId(favGroup), ItineraryType.FAVOURITES);
	}

	public MapMarkersGroup addOrEnableGpxGroup(@NonNull File file) {
		updateGpxShowAsMarkers(file);
		MapMarkersGroup gr = getMapMarkerGroupById(getMarkerGroupId(file), ItineraryType.TRACK);
		if (gr == null) {
			gr = createGPXMarkerGroup(file);
			addGroupInternally(gr);
		}
		enableGroup(gr);
		return gr;
	}

	public MapMarkersGroup addOrEnableGroup(@NonNull GPXFile file) {
		updateGpxShowAsMarkers(new File(file.path));
		MapMarkersGroup gr = getMarkersGroup(file);
		if (gr == null) {
			gr = createGPXMarkerGroup(new File(file.path));
			addGroupInternally(gr);
		}
		enableGroup(gr);
		return gr;
	}

	public MapMarkersGroup addOrEnableGroup(@NonNull FavoriteGroup group) {
		MapMarkersGroup gr = getMarkersGroup(group);
		if (gr == null) {
			gr = createFavMarkerGroup(group);
			addGroupInternally(gr);
		}
		enableGroup(gr);
		return gr;
	}

	public void enableGroup(@NonNull MapMarkersGroup gr) {
		// check if group doesn't exist internally
		if (!mapMarkersGroups.contains(gr)) {
			addGroupInternally(gr);
		}
		if (gr.isDisabled()) {
			updateGroupDisabled(gr, false);
		}
		runSynchronization(gr);
	}

	private void addGroupInternally(MapMarkersGroup group) {
		addHistoryMarkersToGroup(group);
		addToGroupsList(group);
		saveGroups(false);
	}

	private void updateGpxShowAsMarkers(@NonNull File file) {
		GpxDbHelper gpxDbHelper = ctx.getGpxDbHelper();
		GpxDataItem dataItem = gpxDbHelper.getItem(file);
		if (dataItem != null) {
			dataItem.setParameter(SHOW_AS_MARKERS, true);
			gpxDbHelper.updateDataItem(dataItem);
		}
	}

	private void addHistoryMarkersToGroup(@NonNull MapMarkersGroup group) {
		List<MapMarker> historyMarkers = new ArrayList<>(mapMarkersHistory);
		for (MapMarker m : historyMarkers) {
			if (m.groupKey != null && group.getId() != null && m.groupKey.equals(group.getId())) {
				group.getMarkers().add(m);
			}
		}
	}

	public void removeMarkersGroup(MapMarkersGroup group) {
		if (group != null) {
			removeGroupActiveMarkers(group, false);
			removeFromGroupsList(group);
			saveGroups(false);
			updateLastModifiedTime(group);
		}
	}

	public void updateGroupDisabled(@NonNull MapMarkersGroup group, boolean disabled) {
		String id = group.getId();
		if (id != null) {
			group.setDisabled(disabled);
			saveGroups(false);
		}
	}

	public void updateGroupWptCategories(@NonNull MapMarkersGroup group, Set<String> wptCategories) {
		if (group.getId() != null && !Algorithms.objectEquals(group.getWptCategories(), wptCategories)) {
			group.setWptCategories(wptCategories);
			saveGroups(false);
		}
	}

	private void removeGroupActiveMarkers(MapMarkersGroup group, boolean updateGroup) {
		if (group != null) {
			markersDbHelper.removeActiveMarkersFromGroup(group.getId());
			removeFromMapMarkersList(group.getActiveMarkers());
			if (updateGroup) {
				group.setMarkers(group.getHistoryMarkers());
				updateGroup(group);
			}
			saveGroups(true);
		}
	}

	public void updateGroups() {
		for (MapMarkersGroup group : mapMarkersGroups) {
			updateGroup(group);
		}
	}

	public void updateGroup(MapMarkersGroup mapMarkersGroup) {
		if (mapMarkersGroup.getId() == null || mapMarkersGroup.getName() == null) {
			return;
		}
		int historyMarkersCount = mapMarkersGroup.getHistoryMarkers().size();
		ShowHideHistoryButton showHideHistoryButton = mapMarkersGroup.getShowHideHistoryButton();
		if (showHideHistoryButton != null) {
			if (historyMarkersCount == 0) {
				mapMarkersGroup.setShowHideHistoryButton(null);
			}
		} else if (historyMarkersCount > 0) {
			showHideHistoryButton = new ShowHideHistoryButton();
			showHideHistoryButton.showHistory = false;
			mapMarkersGroup.setShowHideHistoryButton(showHideHistoryButton);
		}
	}

	private void addMarkersToGroups(@NonNull List<MapMarker> markers) {
		for (MapMarker marker : markers) {
			addMarkerToGroup(marker);
		}
		saveGroups(false);
	}

	private void addMarkerToGroup(@NonNull MapMarker marker) {
		MapMarkersGroup mapMarkersGroup = getMapMarkerGroupById(marker.groupKey, marker.getType());
		if (mapMarkersGroup != null) {
			mapMarkersGroup.getMarkers().add(marker);
			updateGroup(mapMarkersGroup);
			if (mapMarkersGroup.getName() == null) {
				sortMarkers(mapMarkersGroup.getMarkers(), false, BY_DATE_ADDED_DESC);
			}
		} else {
			mapMarkersGroup = new MapMarkersGroup();
			mapMarkersGroup.setCreationDate(Long.MAX_VALUE);
			mapMarkersGroup.getMarkers().add(marker);
			addToGroupsList(mapMarkersGroup);
			sortGroups();
			updateGroup(mapMarkersGroup);
		}
	}

	private void removeMarkerFromGroup(MapMarker marker) {
		if (marker != null) {
			MapMarkersGroup mapMarkersGroup = getMapMarkerGroupById(marker.groupKey, marker.getType());
			if (mapMarkersGroup != null) {
				mapMarkersGroup.getMarkers().remove(marker);
				updateGroup(mapMarkersGroup);
			}
		}
	}

	private void sortGroups() {
		if (mapMarkersGroups.size() > 0) {
			Collections.sort(mapMarkersGroups, (group1, group2) -> {
				long t1 = group1.getCreationDate();
				long t2 = group2.getCreationDate();
				return (t1 > t2) ? -1 : ((t1 == t2) ? 0 : 1);
			});
		}
	}

	@Nullable
	public MapMarkersGroup getMapMarkerGroupById(String id, ItineraryType type) {
		for (MapMarkersGroup group : mapMarkersGroups) {
			if ((id == null && group.getId() == null)
					|| (group.getId() != null && group.getId().equals(id))) {
				if (type == ItineraryType.MARKERS || type == group.getType()) {
					return group;
				}
			}
		}
		return null;
	}

	private MapMarkersGroup createGPXMarkerGroup(File fl) {
		return new MapMarkersGroup(getMarkerGroupId(fl),
				Algorithms.getFileNameWithoutExtension(fl.getName()),
				ItineraryType.TRACK);
	}

	private MapMarkersGroup createFavMarkerGroup(FavoriteGroup favGroup) {
		return new MapMarkersGroup(favGroup.getName(), favGroup.getName(), ItineraryType.FAVOURITES);
	}

	private String getMarkerGroupId(File gpx) {
		String path = gpx.getAbsolutePath();
		String gpxDir = ctx.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath();
		int index = path.indexOf(gpxDir);
		if (index != -1) {
			path = path.substring(gpxDir.length() + 1);
		}
		return path;
	}

	private String getMarkerGroupId(FavoriteGroup group) {
		return group.getName();
	}

	@NonNull
	public List<MapMarkersGroup> getGroupsForDisplayedGpx() {
		List<MapMarkersGroup> res = new ArrayList<>();
		List<SelectedGpxFile> selectedGpxFiles = ctx.getSelectedGpxHelper().getSelectedGPXFiles();
		for (SelectedGpxFile selected : selectedGpxFiles) {
			MapMarkersGroup search = getMarkersGroup(selected.getGpxFile());
			if (search == null && selected.getGpxFile() != null && !Algorithms.isEmpty(selected.getGpxFile().path)) {
				MapMarkersGroup group = createGPXMarkerGroup(new File(selected.getGpxFile().path));
				group.setDisabled(true);
				res.add(group);
			}
		}
		return res;
	}

	@NonNull
	public List<MapMarkersGroup> getGroupsForSavedArticlesTravelBook() {
		List<MapMarkersGroup> res = new ArrayList<>();
		TravelHelper travelHelper = ctx.getTravelHelper();
		if (travelHelper.isAnyTravelBookPresent()) {
			List<TravelArticle> savedArticles = travelHelper.getBookmarksHelper().getSavedArticles();
			for (TravelArticle art : savedArticles) {
				String gpxName = travelHelper.getGPXName(art);
				File path = ctx.getAppPath(IndexConstants.GPX_TRAVEL_DIR + gpxName);
				MapMarkersGroup search = getMapMarkerGroupById(getMarkerGroupId(path), ItineraryType.TRACK);
				if (search == null) {
					MapMarkersGroup group = createGPXMarkerGroup(path);
					group.setDisabled(true);
					res.add(group);
				}
			}
		}
		return res;
	}

	@Nullable
	public MapMarker getMapMarker(WptPt wptPt) {
		for (MapMarker marker : getAllMarkers()) {
			if (marker.wptPt == wptPt) {
				return marker;
			}
		}
		return null;
	}

	@Nullable
	public MapMarker getMapMarker(FavouritePoint favouritePoint) {
		for (MapMarker marker : getAllMarkers()) {
			if (marker.favouritePoint == favouritePoint) {
				return marker;
			}
		}
		return null;
	}

	@Nullable
	public MapMarker getMapMarker(@NonNull LatLon latLon) {
		for (MapMarker marker : getAllMarkers()) {
			if (latLon.equals(marker.point)) {
				return marker;
			}
		}
		return null;
	}

	@Nullable
	public MapMarker getMapMarker(@NonNull String id) {
		for (MapMarker marker : getAllMarkers()) {
			if (Algorithms.stringsEqual(marker.id, id)) {
				return marker;
			}
		}
		return null;
	}

	private List<MapMarker> getAllMarkers() {
		List<MapMarker> res = new ArrayList<>(mapMarkers);
		res.addAll(mapMarkersHistory);
		return res;
	}

	@Nullable
	public MapMarker getMapMarker(@NonNull String mapObjectName, @NonNull LatLon latLon) {
		for (MapMarker marker : mapMarkers) {
			if (marker.mapObjectName != null
					&& marker.point != null
					&& marker.mapObjectName.equals(mapObjectName)
					&& MapUtils.getDistance(latLon, marker.point) < 15) {
				return marker;
			}
		}
		return null;
	}

	public void moveMapMarkerToHistory(MapMarker marker) {
		if (marker != null) {
			cancelPointAddressRequests(marker.point);
			marker.history = true;
			marker.visitedDate = System.currentTimeMillis();
			markersDbHelper.moveMarkerToHistory(marker);
			removeFromMapMarkersList(marker);
			addToMapMarkersHistoryList(marker);
			sortMarkers(mapMarkersHistory, true, BY_DATE_ADDED_DESC);
			syncPassedPoints();
			saveGroups(true);
		}
	}

	public void addMarker(MapMarker marker) {
		if (marker != null) {
			markersDbHelper.addMarker(marker);
			if (marker.history) {
				addToMapMarkersHistoryList(marker);
				sortMarkers(mapMarkersHistory, true, BY_DATE_ADDED_DESC);
			} else {
				addToMapMarkersList(marker);
			}
			addMarkerToGroup(marker);
			saveGroups(true);
		}
	}

	public void restoreMarkerFromHistory(MapMarker marker, int position) {
		if (marker != null) {
			markersDbHelper.restoreMapMarkerFromHistory(marker);
			removeFromMapMarkersHistoryList(marker);
			marker.history = false;
			addToMapMarkersList(position, marker);
			sortMarkers(mapMarkersHistory, true, BY_DATE_ADDED_DESC);
			syncPassedPoints();
			saveGroups(true);
		}
	}

	public void restoreMarkersFromHistory(List<MapMarker> markers) {
		if (markers != null) {
			for (MapMarker marker : markers) {
				markersDbHelper.restoreMapMarkerFromHistory(marker);
				marker.history = false;
			}
			removeFromMapMarkersHistoryList(markers);
			addToMapMarkersList(markers);
			sortMarkers(mapMarkersHistory, true, BY_DATE_ADDED_DESC);
			updateGroups();
			syncPassedPoints();
			saveGroups(true);
		}
	}

	public void removeMarker(MapMarker marker) {
		removeMarker(marker, true);
	}

	public void removeMarkers(List<MapMarker> mapMarkers) {
		for (MapMarker marker : mapMarkers) {
			removeMarker(marker, false);
		}
		saveGroups(true);
	}

	private void removeMarker(MapMarker marker, boolean save) {
		if (marker != null) {
			markersDbHelper.removeMarker(marker);
			if (marker.history) {
				removeFromMapMarkersHistoryList(marker);
			} else {
				removeFromMapMarkersList(marker);
			}
			removeMarkerFromGroup(marker);
			if (save) {
				saveGroups(true);
			}
		}
	}

	@Nullable
	public MapMarker getFirstMapMarker() {
		return mapMarkers.size() > 0 ? mapMarkers.get(0) : null;
	}

	public void deselectAllActiveMarkers() {
		for (MapMarker m : mapMarkers) {
			if (m.selected) {
				m.selected = false;
				markersDbHelper.updateMarker(m);
			}
		}
	}

	public void selectAllActiveMarkers() {
		for (MapMarker m : mapMarkers) {
			if (!m.selected) {
				m.selected = true;
				markersDbHelper.updateMarker(m);
			}
		}
	}

	public List<MapMarker> getSelectedMarkers() {
		List<MapMarker> list = new ArrayList<>();
		for (MapMarker m : mapMarkers) {
			if (m.selected) {
				list.add(m);
			}
		}
		return list;
	}

	public int getSelectedMarkersCount() {
		int res = 0;
		for (MapMarker m : mapMarkers) {
			if (m.selected) {
				res++;
			}
		}
		return res;
	}

	public void addSelectedMarkersToTop(@NonNull List<MapMarker> markers) {
		List<MapMarker> markersToRemove = new ArrayList<>();
		for (MapMarker m : mapMarkers) {
			if (m.selected) {
				if (!markers.contains(m)) {
					return;
				}
				markersToRemove.add(m);
			}
		}
		if (markersToRemove.size() != markers.size()) {
			return;
		}
		removeFromMapMarkersList(markersToRemove);
		addToMapMarkersList(0, markers);
		saveGroups(false);
	}

	public List<LatLon> getSelectedMarkersLatLon() {
		List<LatLon> list = new ArrayList<>();
		for (MapMarker m : this.mapMarkers) {
			if (m.selected) {
				list.add(m.point);
			}
		}
		return list;
	}

	public void reverseActiveMarkersOrder() {
		cancelAddressRequests();
		Collections.reverse(mapMarkers);
		saveGroups(false);
	}

	public void moveAllActiveMarkersToHistory() {
		cancelAddressRequests();
		long timestamp = System.currentTimeMillis();
		markersDbHelper.moveAllActiveMarkersToHistory(timestamp);
		for (MapMarker marker : mapMarkers) {
			marker.visitedDate = timestamp;
			marker.history = true;
		}
		addToMapMarkersHistoryList(mapMarkers);
		mapMarkers = new ArrayList<>();
		sortMarkers(mapMarkersHistory, true, BY_DATE_ADDED_DESC);
		updateGroups();
		syncPassedPoints();
		saveGroups(true);
	}

	public void addMapMarker(@NonNull LatLon point,
	                         @Nullable PointDescription historyName,
	                         @Nullable String mapObjectName) {
		addMapMarkers(Collections.singletonList(point),
				Collections.singletonList(historyName),
				Collections.singletonList(mapObjectName));
	}

	public void addMapMarkers(@NonNull List<LatLon> points,
	                          @NonNull List<PointDescription> historyNames,
	                          @Nullable List<String> mapObjNames) {
		if (points.size() > 0) {
			ctx.getSettings().SHOW_MAP_MARKERS.set(true);
			int colorIndex = -1;
			List<MapMarker> addedMarkers = new ArrayList<>();
			for (int i = 0; i < points.size(); i++) {
				LatLon point = points.get(i);
				PointDescription historyName = historyNames.get(i);
				String mapObjName = mapObjNames == null ? null : mapObjNames.get(i);
				PointDescription pointDescription = historyName != null
						? historyName : new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
				if (pointDescription.isLocation() && Algorithms.isEmpty(pointDescription.getName())) {
					pointDescription.setName(PointDescription.getSearchAddressStr(ctx));
				}
				if (colorIndex == -1) {
					if (mapMarkers.size() > 0) {
						colorIndex = (mapMarkers.get(0).colorIndex + 1) % MAP_MARKERS_COLORS_COUNT;
					} else {
						colorIndex = 0;
					}
				} else {
					colorIndex = (colorIndex + 1) % MAP_MARKERS_COLORS_COUNT;
				}

				MapMarker marker = new MapMarker(point, pointDescription, colorIndex);
				marker.mapObjectName = mapObjName;

				addedMarkers.add(marker);
				markersDbHelper.addMarker(marker);
				if (marker.history) {
					addToMapMarkersHistoryList(marker);
					sortMarkers(mapMarkersHistory, true, BY_DATE_ADDED_DESC);
				} else {
					addToMapMarkersList(0, marker);
				}
				lookupAddress(marker);
			}
			addMarkersToGroups(addedMarkers);
		}
	}

	public void updateMapMarker(MapMarker marker, boolean save) {
		if (marker != null) {
			markersDbHelper.updateMarker(marker);
			if (save) {
				saveGroups(true);
			}
		}
	}

	public void moveMarkerToTop(MapMarker marker) {
		int i = mapMarkers.indexOf(marker);
		if (i != -1 && mapMarkers.size() > 1) {
			removeFromMapMarkersList(marker);
			addToMapMarkersList(0, marker);
			saveGroups(true);
		}
	}

	public void moveMapMarker(MapMarker marker, LatLon latLon) {
		if (marker != null) {
			LatLon point = new LatLon(latLon.getLatitude(), latLon.getLongitude());
			int index = mapMarkers.indexOf(marker);
			if (index != -1) {
				mapMarkers.get(index).point = point;
			}
			marker.point = point;
			markersDbHelper.updateMarker(marker);
			saveGroups(true);
			lookupAddress(marker);
		}
	}

	public void addSyncListener(OnGroupSyncedListener listener) {
		syncListeners.add(listener);
	}

	public void removeSyncListener(OnGroupSyncedListener listener) {
		syncListeners.remove(listener);
	}

	public void addListener(MapMarkerChangedListener l) {
		if (!listeners.contains(l)) {
			listeners.add(l);
		}
	}

	public void removeListener(MapMarkerChangedListener l) {
		listeners.remove(l);
	}

	private void notifyMarkerChanged(MapMarker marker) {
		ctx.runInUIThread(() -> {
			for (MapMarkerChangedListener l : listeners) {
				l.onMapMarkerChanged(marker);
			}
		});
	}

	private void notifyMarkersChanged() {
		ctx.runInUIThread(() -> {
			for (MapMarkerChangedListener l : listeners) {
				l.onMapMarkersChanged();
			}
		});
	}

	private void syncPassedPoints() {
		Set<GPXFile> gpxFiles = new HashSet<>();
		boolean shouldSaveFavourites = syncPassedPoints(mapMarkers, gpxFiles);
		shouldSaveFavourites |= syncPassedPoints(mapMarkersHistory, gpxFiles);

		if (shouldSaveFavourites) {
			ctx.getFavoritesHelper().saveCurrentPointsIntoFile(true);
		}
		for (GPXFile gpxFile : gpxFiles) {
			SaveGpxHelper.saveGpx(gpxFile);
		}
		if (!gpxFiles.isEmpty()) {
			updateTrackMarkersModifiedTime();
		}
	}

	private boolean syncPassedPoints(List<MapMarker> markers, Set<GPXFile> gpxFiles) {
		boolean shouldSaveFavourites = false;
		for (MapMarker marker : markers) {
			if (marker.favouritePoint != null) {
				shouldSaveFavourites |= syncFavouritesPassedPoints(marker);
			} else if (marker.wptPt != null) {
				syncGpxPassedPoints(marker, gpxFiles);
			}
		}
		return shouldSaveFavourites;
	}

	private boolean syncFavouritesPassedPoints(MapMarker marker) {
		boolean passedPoint = marker.favouritePoint.getVisitedDate() != 0;
		if (marker.history && !passedPoint) {
			return ctx.getFavoritesHelper().favouritePassed(marker.favouritePoint, true, false);
		} else if (!marker.history && passedPoint) {
			return ctx.getFavoritesHelper().favouritePassed(marker.favouritePoint, false, false);
		}
		return false;
	}

	private void syncGpxPassedPoints(MapMarker marker, Set<GPXFile> gpxFiles) {
		GpxSelectionHelper gpxHelper = ctx.getSelectedGpxHelper();
		File file = ctx.getAppPath(IndexConstants.GPX_INDEX_DIR + marker.groupKey);
		if (file.exists()) {
			SelectedGpxFile selectedGpxFile = gpxHelper.getSelectedFileByPath(file.getAbsolutePath());
			if (selectedGpxFile != null) {
				boolean passedPoint = marker.wptPt.getExtensionsToWrite().containsKey(VISITED_DATE);
				if (marker.history && !passedPoint) {
					marker.wptPt.getExtensionsToWrite().put(VISITED_DATE, GPXUtilities.formatTime(System.currentTimeMillis()));
					gpxFiles.add(selectedGpxFile.getGpxFile());
				} else if (!marker.history && passedPoint) {
					marker.wptPt.getExtensionsToWrite().remove(VISITED_DATE);
					gpxFiles.add(selectedGpxFile.getGpxFile());
				}
			}
		}
	}

	// ---------------------------------------------------------------------------------------------

	// accessors to active markers:

	private void addToMapMarkersList(MapMarker marker) {
		addToMapMarkersList(mapMarkers.size(), marker);
	}

	private void addToMapMarkersList(List<MapMarker> markers) {
		addToMapMarkersList(mapMarkers.size(), markers);
	}

	private void addToMapMarkersList(int position, MapMarker marker) {
		List<MapMarker> copy = new ArrayList<>(mapMarkers);
		copy.add(position, marker);
		mapMarkers = copy;
	}

	private void addToMapMarkersList(int position, List<MapMarker> markers) {
		List<MapMarker> copy = new ArrayList<>(mapMarkers);
		copy.addAll(position, markers);
		mapMarkers = copy;
	}

	private void removeFromMapMarkersList(MapMarker marker) {
		mapMarkers = CollectionUtils.removeFromList(mapMarkers, marker);
	}

	private void removeFromMapMarkersList(List<MapMarker> markers) {
		mapMarkers = CollectionUtils.removeAllFromList(mapMarkers, markers);
	}

	// accessors to history markers:

	private void addToMapMarkersHistoryList(MapMarker marker) {
		mapMarkersHistory = CollectionUtils.addToList(mapMarkersHistory, marker);
	}

	private void addToMapMarkersHistoryList(List<MapMarker> markers) {
		mapMarkersHistory = CollectionUtils.addAllToList(mapMarkersHistory, markers);
	}

	private void removeFromMapMarkersHistoryList(MapMarker marker) {
		mapMarkersHistory = CollectionUtils.removeFromList(mapMarkersHistory, marker);
	}

	private void removeFromMapMarkersHistoryList(List<MapMarker> markers) {
		mapMarkersHistory = CollectionUtils.removeAllFromList(mapMarkersHistory, markers);
	}

	// accessors to markers groups:

	private void addToGroupsList(MapMarkersGroup group) {
		mapMarkersGroups = CollectionUtils.addToList(mapMarkersGroups, group);
	}

	private void removeFromGroupsList(MapMarkersGroup group) {
		mapMarkersGroups = CollectionUtils.removeFromList(mapMarkersGroups, group);
	}

	// ---------------------------------------------------------------------------------------------

	public interface MapMarkerChangedListener {
		void onMapMarkerChanged(MapMarker mapMarker);

		void onMapMarkersChanged();
	}

	protected void runGroupSynchronization(MapMarkersGroup group) {
		List<MapMarker> existingMarkers = new ArrayList<>();
		List<MapMarker> groupMarkers = new ArrayList<>(group.getMarkers());
		if (group.getType() == ItineraryType.FAVOURITES) {
			syncFavouriteGroup(group, existingMarkers);
		} else if (group.getType() == ItineraryType.TRACK) {
			syncTrackGroup(group, existingMarkers);
		} else if (group.getType() == ItineraryType.MARKERS) {
			existingMarkers.addAll(markersDbHelper.getActiveMarkers());
			existingMarkers.addAll(markersDbHelper.getMarkersHistory());
		} else {
			throw new IllegalArgumentException("Unsupported ItineraryType: " + group.getType());
		}
		updateGroupMarkers(group, groupMarkers, existingMarkers);
		removeOldMarkersIfPresent(groupMarkers);
	}

	private void syncFavouriteGroup(@NonNull MapMarkersGroup group, @NonNull List<MapMarker> existingMarkers) {
		FavoriteGroup favGroup = ctx.getFavoritesHelper().getGroup(group.getId());
		if (favGroup == null) {
			removeFromGroupsList(group);
			return;
		}
		group.setVisible(favGroup.isVisible());
		if (!group.isVisible() || group.isDisabled()) {
			removeGroupActiveMarkers(group, true);
			return;
		}
		int colorIndex = -1;
		List<FavouritePoint> points = new ArrayList<>(favGroup.getPoints());
		for (FavouritePoint point : points) {
			if (colorIndex == -1) {
				colorIndex = mapMarkers.isEmpty() ? 0 : (mapMarkers.get(0).colorIndex + 1) % MAP_MARKERS_COLORS_COUNT;
			} else {
				colorIndex = (colorIndex + 1) % MAP_MARKERS_COLORS_COUNT;
			}
			MapMarker mapMarker = ItineraryDataHelper.fromFavourite(ctx, point, group);
			mapMarker.colorIndex = colorIndex;
			existingMarkers.add(mapMarker);
		}
	}

	private void syncTrackGroup(@NonNull MapMarkersGroup group, @NonNull List<MapMarker> existingMarkers) {
		GpxSelectionHelper gpxHelper = ctx.getSelectedGpxHelper();
		File file = ctx.getAppPath(IndexConstants.GPX_INDEX_DIR + group.getId());
		if (!file.exists() || !file.isFile()) {
			removeFromGroupsList(group);
			return;
		}

		SelectedGpxFile selectedGpxFile = gpxHelper.getSelectedFileByPath(file.getAbsolutePath());
		GPXFile gpx = selectedGpxFile == null ? null : selectedGpxFile.getGpxFile();
		group.setVisible(gpx != null || group.isVisibleUntilRestart());
		if (gpx == null || group.isDisabled()) {
			removeGroupActiveMarkers(group, true);
			return;
		}
		int colorIndex = -1;
		boolean addAll = group.getWptCategories() == null || group.getWptCategories().isEmpty();
		List<WptPt> gpxPoints = new ArrayList<>(gpx.getPoints());
		for (WptPt wptPt : gpxPoints) {
			if (addAll || group.getWptCategories().contains(wptPt.category)
					|| (wptPt.category == null && group.getWptCategories().contains(""))) {
				if (colorIndex == -1) {
					colorIndex = mapMarkers.isEmpty() ? 0 : (mapMarkers.get(0).colorIndex + 1) % MAP_MARKERS_COLORS_COUNT;
				} else {
					colorIndex = (colorIndex + 1) % MAP_MARKERS_COLORS_COUNT;
				}
				MapMarker mapMarker = ItineraryDataHelper.fromWpt(ctx, wptPt, group);
				mapMarker.colorIndex = colorIndex;
				existingMarkers.add(mapMarker);
			}
		}
	}

	private void updateGroupMarkers(@NonNull MapMarkersGroup group, @NonNull List<MapMarker> groupMarkers, @NonNull List<MapMarker> markers) {
		for (MapMarker marker : markers) {
			MapMarker savedMarker = getMapMarker(marker.id);
			if (savedMarker != null) {
				boolean historyChanged = savedMarker.history != marker.history;
				if (historyChanged) {
					if (marker.history) {
						removeFromMapMarkersList(savedMarker);
						addToMapMarkersHistoryList(savedMarker);
					} else {
						removeFromMapMarkersHistoryList(savedMarker);
						addToMapMarkersList(savedMarker);
					}
				}
				savedMarker.copyParams(marker);
			} else {
				if (marker.history) {
					addToMapMarkersHistoryList(marker);
				} else {
					addToMapMarkersList(marker);
				}
				group.getMarkers().add(marker);
			}
			Iterator<MapMarker> iterator = groupMarkers.iterator();
			while (iterator.hasNext()) {
				if (Algorithms.stringsEqual(iterator.next().id, marker.id)) {
					iterator.remove();
				}
			}
		}
	}

	private void removeOldMarkersIfPresent(List<MapMarker> markers) {
		if (!markers.isEmpty()) {
			boolean save = false;
			for (MapMarker marker : markers) {
				if (!marker.history) {
					removeMarker(marker, false);
					save = true;
				}
			}
			if (save) {
				saveGroups(true);
			}
		}
	}
}