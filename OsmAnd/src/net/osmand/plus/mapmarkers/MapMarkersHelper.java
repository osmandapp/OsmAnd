package net.osmand.plus.mapmarkers;

import android.os.AsyncTask;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.osmand.data.PointDescription.POINT_TYPE_MAP_MARKER;
import static net.osmand.util.MapUtils.createShortLinkString;

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
		loadMarkers();
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

	private void loadMarkers() {
		mapMarkers = new ArrayList<>();
		mapMarkersHistory = new ArrayList<>();

		addToMapMarkersList(markersDbHelper.getActiveMarkers());
		reorderActiveMarkersIfNeeded();

		List<MapMarker> markersHistory = markersDbHelper.getMarkersHistory();
		sortMarkers(markersHistory, true, BY_DATE_ADDED_DESC);
		addToMapMarkersHistoryList(markersHistory);

		if (!ctx.isApplicationInitializing()) {
			lookupAddressAll();
		}
	}

	public void syncAllGroups() {
		mapMarkersGroups = dataHelper.loadGroups();
		for (MapMarkersGroup group : mapMarkersGroups) {
			updateGroup(group);
			runGroupSynchronization(group, true);
		}
		sortGroups();
		saveGroups();
	}

	private void saveGroups() {
		List<MapMarkersGroup> markersGroups = new ArrayList<>();
		for (MapMarkersGroup group : mapMarkersGroups) {
			if (!group.isDisabled()) {
				markersGroups.add(group);
			}
		}
		dataHelper.saveGroups(markersGroups);
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

	private void lookupAddress(final MapMarker mapMarker) {
		if (mapMarker != null && mapMarker.getOriginalPointDescription().isSearchingAddress(ctx)) {
			cancelPointAddressRequests(mapMarker.point);
			AddressLookupRequest lookupRequest = new AddressLookupRequest(mapMarker.point,
					new GeocodingLookupService.OnAddressLookupResult() {
						@Override
						public void geocodingDone(String address) {
							PointDescription pointDescription = mapMarker.getOriginalPointDescription();
							if (Algorithms.isEmpty(address)) {
								pointDescription.setName(PointDescription.getAddressNotFoundStr(ctx));
							} else {
								pointDescription.setName(address);
							}
							markersDbHelper.updateMarker(mapMarker);
							refreshMarker(mapMarker);
						}
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

	public void reorderActiveMarkersIfNeeded() {
		if (!mapMarkers.isEmpty()) {
			if (mapMarkers.size() > 1) {
				for (int i = 0; i < mapMarkers.size() - 1; i++) {
					MapMarker first = mapMarkers.get(i);
					MapMarker second = mapMarkers.get(i + 1);
					if (!first.nextKey.equals(second.id)) {
						markersDbHelper.changeActiveMarkerPosition(first, second);
						first.nextKey = second.id;
					}
				}
			}

			MapMarker tail = mapMarkers.get(mapMarkers.size() - 1);
			if (!tail.nextKey.equals(MapMarkersDbHelper.TAIL_NEXT_VALUE)) {
				markersDbHelper.changeActiveMarkerPosition(tail, null);
			}
		}
	}

	public void sortMarkers(final @MapMarkersSortByDef int sortByMode, LatLon location) {
		sortMarkers(getMapMarkers(), false, sortByMode, location);
		reorderActiveMarkersIfNeeded();
	}

	private void sortMarkers(List<MapMarker> markers, final boolean visited, final @MapMarkersSortByDef int sortByMode) {
		sortMarkers(markers, visited, sortByMode, null);
	}

	private void sortMarkers(List<MapMarker> markers,
							 final boolean visited,
							 final @MapMarkersSortByDef int sortByMode,
							 @Nullable final LatLon location) {
		Collections.sort(markers, new Comparator<MapMarker>() {
			@Override
			public int compare(MapMarker mapMarker1, MapMarker mapMarker2) {
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
			}
		});
	}

	public void runSynchronization(final @NonNull MapMarkersGroup group) {
		ctx.runInUIThread(new Runnable() {
			@Override
			public void run() {
				new SyncGroupTask(group).executeOnExecutor(executorService);
			}
		});
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
		saveGroups();
	}

	private void updateGpxShowAsMarkers(File file) {
		GpxDataItem dataItem = ctx.getGpxDbHelper().getItem(file);
		if (dataItem != null) {
			ctx.getGpxDbHelper().updateShowAsMarkers(dataItem, true);
			dataItem.setShowAsMarkers(true);
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
			saveGroups();
		}
	}

	public void updateGroupDisabled(@NonNull MapMarkersGroup group, boolean disabled) {
		String id = group.getId();
		if (id != null) {
			group.setDisabled(disabled);
			saveGroups();
		}
	}

	public void updateGroupWptCategories(@NonNull MapMarkersGroup group, Set<String> wptCategories) {
		if (group.getId() != null) {
			group.setWptCategories(wptCategories);
			saveGroups();
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
			reorderActiveMarkersIfNeeded();
			refresh();
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
			Collections.sort(mapMarkersGroups, new Comparator<MapMarkersGroup>() {
				@Override
				public int compare(MapMarkersGroup group1, MapMarkersGroup group2) {
					long t1 = group1.getCreationDate();
					long t2 = group2.getCreationDate();
					return (t1 > t2) ? -1 : ((t1 == t2) ? 0 : 1);
				}
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
		for (MapMarker marker : getMarkers()) {
			if (marker.wptPt == wptPt) {
				return marker;
			}
		}
		return null;
	}

	@Nullable
	public MapMarker getMapMarker(FavouritePoint favouritePoint) {
		for (MapMarker marker : getMarkers()) {
			if (marker.favouritePoint == favouritePoint) {
				return marker;
			}
		}
		return null;
	}

	@Nullable
	public MapMarker getMapMarker(@NonNull LatLon latLon) {
		for (MapMarker marker : getMarkers()) {
			if (marker.point != null && marker.point.equals(latLon)) {
				return marker;
			}
		}
		return null;
	}

	private List<MapMarker> getMarkers() {
		List<MapMarker> res = new ArrayList<>(mapMarkers);
		if (ctx.getSettings().KEEP_PASSED_MARKERS_ON_MAP.get()) {
			res.addAll(mapMarkersHistory);
		}
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
			marker.nextKey = MapMarkersDbHelper.HISTORY_NEXT_VALUE;
			markersDbHelper.moveMarkerToHistory(marker);
			removeFromMapMarkersList(marker);
			addToMapMarkersHistoryList(marker);
			reorderActiveMarkersIfNeeded();
			sortMarkers(mapMarkersHistory, true, BY_DATE_ADDED_DESC);
			syncPassedPoints();
			refresh();
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
				reorderActiveMarkersIfNeeded();
			}
			addMarkerToGroup(marker);
			refresh();
		}
	}

	public void restoreMarkerFromHistory(MapMarker marker, int position) {
		if (marker != null) {
			markersDbHelper.restoreMapMarkerFromHistory(marker);
			removeFromMapMarkersHistoryList(marker);
			marker.history = false;
			addToMapMarkersList(position, marker);
			reorderActiveMarkersIfNeeded();
			sortMarkers(mapMarkersHistory, true, BY_DATE_ADDED_DESC);
			syncPassedPoints();
			refresh();
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
			reorderActiveMarkersIfNeeded();
			sortMarkers(mapMarkersHistory, true, BY_DATE_ADDED_DESC);
			updateGroups();
			syncPassedPoints();
			refresh();
		}
	}

	public void removeMarker(MapMarker marker) {
		removeMarker(marker, true);
	}

	private void removeMarker(MapMarker marker, boolean refresh) {
		if (marker != null) {
			markersDbHelper.removeMarker(marker);
			if (marker.history) {
				removeFromMapMarkersHistoryList(marker);
			} else {
				removeFromMapMarkersList(marker);
			}
			removeMarkerFromGroup(marker);
			if (refresh) {
				refresh();
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
		reorderActiveMarkersIfNeeded();
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
		reorderActiveMarkersIfNeeded();
	}

	public void moveAllActiveMarkersToHistory() {
		cancelAddressRequests();
		long timestamp = System.currentTimeMillis();
		markersDbHelper.moveAllActiveMarkersToHistory(timestamp);
		for (MapMarker marker : mapMarkers) {
			marker.visitedDate = timestamp;
			marker.history = true;
			marker.nextKey = MapMarkersDbHelper.HISTORY_NEXT_VALUE;
		}
		addToMapMarkersHistoryList(mapMarkers);
		mapMarkers = new ArrayList<>();
		sortMarkers(mapMarkersHistory, true, BY_DATE_ADDED_DESC);
		updateGroups();
		syncPassedPoints();
		refresh();
	}

	public void addMapMarker(@NonNull LatLon point, @Nullable PointDescription historyName) {
		addMapMarkers(Collections.singletonList(point), Collections.singletonList(historyName), null);
	}

	public void addMapMarker(@NonNull LatLon point,
							 @Nullable PointDescription historyName,
							 @Nullable String mapObjectName) {
		addMarkers(Collections.singletonList(point),
				Collections.singletonList(historyName),
				null,
				null,
				null,
				Collections.singletonList(mapObjectName));
	}

	public void addMapMarkers(@NonNull List<LatLon> points,
							  @NonNull List<PointDescription> historyNames,
							  @Nullable MapMarkersGroup group) {
		addMarkers(points, historyNames, group, null, null, null);
	}

	private void addMarkers(@NonNull List<LatLon> points,
							@NonNull List<PointDescription> historyNames,
							@Nullable MapMarkersGroup group,
							@Nullable List<FavouritePoint> favouritePoints,
							@Nullable List<WptPt> wptPts,
							@Nullable List<String> mapObjNames) {
		if (points.size() > 0) {
			ctx.getSettings().SHOW_MAP_MARKERS.set(true);
			int colorIndex = -1;
			List<MapMarker> addedMarkers = new ArrayList<>();
			for (int i = 0; i < points.size(); i++) {
				LatLon point = points.get(i);
				PointDescription historyName = historyNames.get(i);
				FavouritePoint favouritePoint = favouritePoints == null ? null : favouritePoints.get(i);
				WptPt wptPt = wptPts == null ? null : wptPts.get(i);
				String mapObjName = mapObjNames == null ? null : mapObjNames.get(i);
				final PointDescription pointDescription;
				if (historyName == null) {
					pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
				} else {
					pointDescription = historyName;
				}
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

				MapMarker marker = new MapMarker(point, pointDescription, colorIndex, false, 0);
				if (group != null) {
					marker.id = getMarkerId(ctx, group, marker);
					if (markersDbHelper.getMarker(marker.id) != null) {
						continue;
					}
					marker.groupName = group.getName();
					marker.groupKey = group.getId();
				}
				if (wptPt != null) {
					String timestamp = wptPt.getExtensionsToWrite().get(FavouritePoint.PASSED_TIMESTAMP);
					long passedTime = Algorithms.parseLongSilently(timestamp, 0);
					marker.wptPt = wptPt;
					marker.history = passedTime != 0;
					marker.visitedDate = passedTime;
				} else if (favouritePoint != null) {
					marker.favouritePoint = favouritePoint;
					marker.history = favouritePoint.getPassedTimestamp() != 0;
					marker.visitedDate = favouritePoint.getPassedTimestamp();
				} else {
					marker.history = false;
				}
				marker.mapObjectName = mapObjName;

				addedMarkers.add(marker);
				markersDbHelper.addMarker(marker);
				if (marker.history) {
					marker.nextKey = MapMarkersDbHelper.HISTORY_NEXT_VALUE;
					addToMapMarkersHistoryList(marker);
					sortMarkers(mapMarkersHistory, true, BY_DATE_ADDED_DESC);
				} else {
					marker.nextKey = MapMarkersDbHelper.TAIL_NEXT_VALUE;
					addToMapMarkersList(0, marker);
					reorderActiveMarkersIfNeeded();
				}
				lookupAddress(marker);
			}
			addMarkersToGroups(addedMarkers);
		}
	}

	public static String getMarkerId(OsmandApplication app, MapMarkersGroup group, MapMarker marker) {
		return group.getId() + marker.getName(app) + createShortLinkString(marker.point.getLatitude(), marker.point.getLongitude(), 15);
	}

	public void updateMapMarker(MapMarker marker, boolean refresh) {
		if (marker != null) {
			markersDbHelper.updateMarker(marker);
			if (refresh) {
				refresh();
			}
		}
	}

	public void moveMarkerToTop(MapMarker marker) {
		int i = mapMarkers.indexOf(marker);
		if (i != -1 && mapMarkers.size() > 1) {
			removeFromMapMarkersList(marker);
			addToMapMarkersList(0, marker);
			reorderActiveMarkersIfNeeded();
			refresh();
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
			reorderActiveMarkersIfNeeded();
			refresh();
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

	private void refreshMarker(final MapMarker marker) {
		ctx.runInUIThread(new Runnable() {
			@Override
			public void run() {
				for (MapMarkerChangedListener l : listeners) {
					l.onMapMarkerChanged(marker);
				}
			}
		});
	}

	private void refresh() {
		ctx.runInUIThread(new Runnable() {
			@Override
			public void run() {
				for (MapMarkerChangedListener l : listeners) {
					l.onMapMarkersChanged();
				}
			}
		});
		saveGroups();
	}

	private void syncPassedPoints() {
		Set<GPXFile> gpxFiles = new HashSet<>();
		boolean shouldSaveFavourites = syncPassedPoints(mapMarkers, gpxFiles);
		shouldSaveFavourites |= syncPassedPoints(mapMarkersHistory, gpxFiles);

		if (shouldSaveFavourites) {
			ctx.getFavorites().saveCurrentPointsIntoFile();
		}
		for (GPXFile gpxFile : gpxFiles) {
			GpxUiHelper.saveGpx(gpxFile, null);
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
		boolean passedPoint = marker.favouritePoint.getPassedTimestamp() != 0;
		if (marker.history && !passedPoint) {
			return ctx.getFavorites().favouritePassed(marker.favouritePoint, true, false);
		} else if (!marker.history && passedPoint) {
			return ctx.getFavorites().favouritePassed(marker.favouritePoint, false, false);
		}
		return false;
	}

	private void syncGpxPassedPoints(MapMarker marker, Set<GPXFile> gpxFiles) {
		GpxSelectionHelper gpxHelper = ctx.getSelectedGpxHelper();
		File file = ctx.getAppPath(IndexConstants.GPX_INDEX_DIR + marker.groupKey);
		if (file.exists()) {
			SelectedGpxFile selectedGpxFile = gpxHelper.getSelectedFileByPath(file.getAbsolutePath());
			if (selectedGpxFile != null) {
				boolean passedPoint = marker.wptPt.getExtensionsToWrite().containsKey(FavouritePoint.PASSED_TIMESTAMP);
				if (marker.history && !passedPoint) {
					marker.wptPt.getExtensionsToWrite().put(FavouritePoint.PASSED_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
					gpxFiles.add(selectedGpxFile.getGpxFile());
				} else if (!marker.history && passedPoint) {
					marker.wptPt.getExtensionsToWrite().remove(FavouritePoint.PASSED_TIMESTAMP);
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

	private void addToMapMarkersList(int position, MapMarker marker) {
		List<MapMarker> copyList = new ArrayList<>(mapMarkers);
		copyList.add(position, marker);
		mapMarkers = copyList;
	}

	private void addToMapMarkersList(List<MapMarker> markers) {
		addToMapMarkersList(mapMarkers.size(), markers);
	}

	private void addToMapMarkersList(int position, List<MapMarker> markers) {
		List<MapMarker> copyList = new ArrayList<>(mapMarkers);
		copyList.addAll(position, markers);
		mapMarkers = copyList;
	}

	private void removeFromMapMarkersList(MapMarker marker) {
		List<MapMarker> copyList = new ArrayList<>(mapMarkers);
		copyList.remove(marker);
		mapMarkers = copyList;
	}

	private void removeFromMapMarkersList(List<MapMarker> markers) {
		List<MapMarker> copyList = new ArrayList<>(mapMarkers);
		copyList.removeAll(markers);
		mapMarkers = copyList;
	}

	// accessors to history markers:

	private void addToMapMarkersHistoryList(MapMarker marker) {
		List<MapMarker> copyList = new ArrayList<>(mapMarkersHistory);
		copyList.add(marker);
		mapMarkersHistory = copyList;
	}

	private void addToMapMarkersHistoryList(List<MapMarker> markers) {
		List<MapMarker> copyList = new ArrayList<>(mapMarkersHistory);
		copyList.addAll(markers);
		mapMarkersHistory = copyList;
	}

	private void removeFromMapMarkersHistoryList(MapMarker marker) {
		List<MapMarker> copyList = new ArrayList<>(mapMarkersHistory);
		copyList.remove(marker);
		mapMarkersHistory = copyList;
	}

	private void removeFromMapMarkersHistoryList(List<MapMarker> markers) {
		List<MapMarker> copyList = new ArrayList<>(mapMarkersHistory);
		copyList.removeAll(markers);
		mapMarkersHistory = copyList;
	}

	// accessors to markers groups:

	private void addToGroupsList(MapMarkersGroup group) {
		List<MapMarkersGroup> copyList = new ArrayList<>(mapMarkersGroups);
		copyList.add(group);
		mapMarkersGroups = copyList;
	}

	private void removeFromGroupsList(MapMarkersGroup group) {
		List<MapMarkersGroup> copyList = new ArrayList<>(mapMarkersGroups);
		copyList.remove(group);
		mapMarkersGroups = copyList;
	}

	// ---------------------------------------------------------------------------------------------

	// classes and interfaces:

	public interface MapMarkerChangedListener {
		void onMapMarkerChanged(MapMarker mapMarker);

		void onMapMarkersChanged();
	}

	public interface OnGroupSyncedListener {

		void onSyncStarted();

		void onSyncDone();
	}

	private void runGroupSynchronization(MapMarkersGroup group, boolean initialSync) {
		List<MapMarker> groupMarkers = new ArrayList<>(group.getMarkers());
		if (group.getType() == ItineraryType.FAVOURITES) {
			FavoriteGroup favGroup = ctx.getFavorites().getGroup(group.getId());
			if (favGroup == null) {
				removeFromGroupsList(group);
				return;
			}
			group.setVisible(favGroup.isVisible());
			if (!group.isVisible() || group.isDisabled()) {
				removeGroupActiveMarkers(group, true);
				return;
			}
			if (initialSync) {
				groupMarkers.clear();
				group.getMarkers().clear();
			}
			List<FavouritePoint> points = new ArrayList<>(favGroup.getPoints());
			for (FavouritePoint point : points) {
				LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
				addNewMarkerIfNeeded(group, groupMarkers, latLon, point.getName(), point, null);
			}
		} else if (group.getType() == ItineraryType.TRACK) {
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
			if (initialSync) {
				groupMarkers.clear();
				group.getMarkers().clear();
			}
			boolean addAll = group.getWptCategories() == null || group.getWptCategories().isEmpty();
			List<WptPt> gpxPoints = new ArrayList<>(gpx.getPoints());
			for (WptPt pt : gpxPoints) {
				if (addAll || group.getWptCategories().contains(pt.category)
						|| (pt.category == null && group.getWptCategories().contains(""))) {
					addNewMarkerIfNeeded(group, groupMarkers, new LatLon(pt.lat, pt.lon), pt.name, null, pt);
				}
			}
		} else if (group.getType() == ItineraryType.MARKERS) {
			groupMarkers.clear();
			List<MapMarker> markers = new ArrayList<>();
			List<MapMarker> allMarkers = new ArrayList<>(mapMarkers);
			allMarkers.addAll(mapMarkersHistory);
			for (MapMarker marker : allMarkers) {
				if (marker.groupKey == null) {
					markers.add(marker);
				}
			}
			group.setMarkers(markers);
		} else {
			throw new IllegalArgumentException("Unsupported ItineraryType: " + group.getType());
		}
		removeOldMarkersIfPresent(groupMarkers);
	}

	private void addNewMarkerIfNeeded(@NonNull MapMarkersGroup group,
									  @NonNull List<MapMarker> groupMarkers,
									  @NonNull LatLon latLon,
									  @NonNull String name,
									  @Nullable FavouritePoint favouritePoint,
									  @Nullable WptPt wptPt) {
		boolean exists = false;

		Iterator<MapMarker> iterator = groupMarkers.iterator();
		while (iterator.hasNext()) {
			MapMarker marker = iterator.next();
			if (marker.id.equals(getMarkerId(ctx, group, marker))) {
				exists = true;
				marker.favouritePoint = favouritePoint;
				marker.wptPt = wptPt;
				if (!marker.history && !marker.point.equals(latLon)) {
					marker.point = latLon;
					updateMapMarker(marker, true);
				}
				iterator.remove();
				break;
			}
		}

		if (!exists) {
			addMarkers(Collections.singletonList(latLon),
					Collections.singletonList(new PointDescription(POINT_TYPE_MAP_MARKER, name)),
					group,
					Collections.singletonList(favouritePoint),
					Collections.singletonList(wptPt),
					null);
		}
	}

	private void removeOldMarkersIfPresent(List<MapMarker> markers) {
		if (!markers.isEmpty()) {
			boolean needRefresh = false;
			for (MapMarker marker : markers) {
				if (!marker.history) {
					removeMarker(marker, false);
					needRefresh = true;
				}
			}
			if (needRefresh) {
				reorderActiveMarkersIfNeeded();
				refresh();
			}
		}
	}

	private class SyncGroupTask extends AsyncTask<Void, Void, Void> {

		private final MapMarkersGroup group;

		SyncGroupTask(MapMarkersGroup group) {
			this.group = group;
		}

		@Override
		protected void onPreExecute() {
			if (!syncListeners.isEmpty()) {
				ctx.runInUIThread(new Runnable() {
					@Override
					public void run() {
						for (OnGroupSyncedListener listener : syncListeners) {
							listener.onSyncStarted();
						}
					}
				});
			}
		}

		@Override
		protected Void doInBackground(Void... voids) {
			runGroupSynchronization(group, false);
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			if (!syncListeners.isEmpty()) {
				ctx.runInUIThread(new Runnable() {
					@Override
					public void run() {
						for (OnGroupSyncedListener listener : syncListeners) {
							listener.onSyncDone();
						}
					}
				});
			}
		}
	}
}