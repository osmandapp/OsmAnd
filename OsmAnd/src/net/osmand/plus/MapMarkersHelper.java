package net.osmand.plus;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;

import net.osmand.IndexConstants;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.mapmarkers.MapMarkersDbHelper;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static net.osmand.data.PointDescription.POINT_TYPE_MAP_MARKER;

public class MapMarkersHelper {
	public static final int MAP_MARKERS_COLORS_COUNT = 7;

	private List<MapMarker> mapMarkers = new LinkedList<>();
	private List<MapMarker> mapMarkersHistory = new LinkedList<>();
	private OsmandSettings settings;
	private List<MapMarkerChangedListener> listeners = new ArrayList<>();
	private OsmandApplication ctx;
	private MapMarkersDbHelper markersDbHelper;
	private boolean startFromMyLocation;

	public interface MapMarkerChangedListener {
		void onMapMarkerChanged(MapMarker mapMarker);

		void onMapMarkersChanged();
	}

	public static class MapMarker implements LocationPoint {
		private static int[] colors;

		public String id;
		public LatLon point;
		private PointDescription pointDescription;
		public int colorIndex;
		public int index;
		public boolean history;
		public boolean selected;
		public int dist;
		public long creationDate;
		public long visitedDate;
		public String nextKey;
		public String groupKey;
		public String groupName;

		public MapMarker(LatLon point, PointDescription name, int colorIndex,
						 boolean selected, int index) {
			this.point = point;
			this.pointDescription = name;
			this.colorIndex = colorIndex;
			this.selected = selected;
			this.index = index;
		}

		public PointDescription getPointDescription(Context ctx) {
			return new PointDescription(POINT_TYPE_MAP_MARKER, ctx.getString(R.string.map_marker),
					getOnlyName());
		}

		public String getName(Context ctx) {
			String name;
			PointDescription pd = getPointDescription(ctx);
			if (Algorithms.isEmpty(pd.getName())) {
				name = pd.getTypeName();
			} else {
				name = pd.getName();
			}
			return name;
		}

		public PointDescription getOriginalPointDescription() {
			return pointDescription;
		}

		public String getOnlyName() {
			return pointDescription == null ? "" : pointDescription.getName();
		}

		public double getLatitude() {
			return point.getLatitude();
		}

		public double getLongitude() {
			return point.getLongitude();
		}

		@Override
		public int getColor() {
			return 0;
		}

		@Override
		public boolean isVisible() {
			return false;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			MapMarker mapMarker = (MapMarker) o;

			if (colorIndex != mapMarker.colorIndex) return false;
			return point.equals(mapMarker.point);

		}

		@Override
		public int hashCode() {
			int result = point.hashCode();
			result = 31 * result + colorIndex;
			return result;
		}

		private static final int[] colorsIds = new int[]{
				R.color.marker_blue,
				R.color.marker_green,
				R.color.marker_orange,
				R.color.marker_red,
				R.color.marker_yellow,
				R.color.marker_teal,
				R.color.marker_purple
		};

		public static int[] getColors(Context context) {
			if (colors != null) {
				return colors;
			}
			colors = new int[colorsIds.length];
			for (int i = 0; i < colorsIds.length; i++) {
				colors[i] = ContextCompat.getColor(context, colorsIds[i]);
			}
			return colors;
		}

		public static int getColorId(int colorIndex) {
			return (colorIndex >= 0 && colorIndex < colorsIds.length) ? colorsIds[colorIndex] : colorsIds[0];
		}

		public static int getColorIndex(Context context, int color) {
			int[] colors = getColors(context);
			for (int i = 0; i < colors.length; i++) {
				if (colors[i] == color) {
					return i;
				}
			}
			return -1;
		}
	}

	@Nullable
	public MarkersSyncGroup getGroup(String id) {
		return markersDbHelper.getGroup(id);
	}

	public static class MarkersSyncGroup {

		public static final int FAVORITES_TYPE = 0;
		public static final int GPX_TYPE = 1;

		private String id;
		private String name;
		private int type;
		private int color;

		public MarkersSyncGroup(@NonNull String id, @NonNull String name, int type, int color) {
			this.id = id;
			this.name = name;
			this.type = type;
			this.color = color;
		}

		public MarkersSyncGroup(@NonNull String id, @NonNull String name, int type) {
			this.id = id;
			this.name = name;
			this.type = type;
			this.color = -1;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public int getType() {
			return type;
		}

		public int getColor() {
			return color;
		}

		public void setColor(int color) {
			this.color = color;
		}
	}

	public MapMarkersHelper(OsmandApplication ctx) {
		this.ctx = ctx;
		settings = ctx.getSettings();
		markersDbHelper = ctx.getMapMarkersDbHelper();
		startFromMyLocation = settings.ROUTE_MAP_MARKERS_START_MY_LOC.get();
		loadMarkers();
	}

	public boolean isStartFromMyLocation() {
		return startFromMyLocation;
	}

	public void setStartFromMyLocation(boolean startFromMyLocation) {
		this.startFromMyLocation = startFromMyLocation;
		settings.ROUTE_MAP_MARKERS_START_MY_LOC.set(startFromMyLocation);
	}

	public void lookupAddressAll() {
		for (MapMarker mapMarker : mapMarkers) {
			lookupAddress(mapMarker);
		}
		for (MapMarker mapMarker : mapMarkersHistory) {
			lookupAddress(mapMarker);
		}
	}

	private void loadMarkers() {
		mapMarkers.clear();
		mapMarkersHistory.clear();

		List<MapMarker> activeMarkers = markersDbHelper.getActiveMarkers();
		mapMarkers.addAll(activeMarkers);
		checkAndFixActiveMarkersOrderIfNeeded();

		List<MapMarker> markersHistory = markersDbHelper.getMarkersHistory();
		sortMarkers(markersHistory, true, OsmandSettings.MapMarkersOrderByMode.DATE_ADDED_DESC);
		mapMarkersHistory.addAll(markersHistory);

		if (!ctx.isApplicationInitializing()) {
			lookupAddressAll();
		}
	}

	public void checkAndFixActiveMarkersOrderIfNeeded() {
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

	public void sortMarkers(List<MapMarker> markers, final boolean visited, final OsmandSettings.MapMarkersOrderByMode orderByMode) {
		final LatLon location = ctx.getSettings().getLastKnownMapLocation();
		Collections.sort(markers, new Comparator<MapMarker>() {
			@Override
			public int compare(MapMarker mapMarker1, MapMarker mapMarker2) {
				if (orderByMode.isDateAddedDescending() || orderByMode.isDateAddedAscending()) {
					long t1 = visited ? mapMarker1.visitedDate : mapMarker1.creationDate;
					long t2 = visited ? mapMarker2.visitedDate : mapMarker2.creationDate;
					if (t1 > t2) {
						return orderByMode.isDateAddedDescending() ? -1 : 1;
					} else if (t1 == t2) {
						return 0;
					} else {
						return orderByMode.isDateAddedDescending() ? 1 : -1;
					}
				} else if (orderByMode.isDistanceDescending() || orderByMode.isDistanceAscending()) {
					int d1 = (int) MapUtils.getDistance(location, mapMarker1.getLatitude(), mapMarker1.getLongitude());
					int d2 = (int) MapUtils.getDistance(location, mapMarker2.getLatitude(), mapMarker2.getLongitude());
					if (d1 > d2) {
						return orderByMode.isDistanceDescending() ? -1 : 1;
					} else if (d1 == d2) {
						return 0;
					} else {
						return orderByMode.isDistanceDescending() ? 1 : -1;
					}
				} else {
					String n1 = mapMarker1.getName(ctx);
					String n2 = mapMarker2.getName(ctx);
					return n1.compareToIgnoreCase(n2);
				}
			}
		});
	}

	public void orderMarkers(OsmandSettings.MapMarkersOrderByMode orderByMode) {
		sortMarkers(getMapMarkers(), false, orderByMode);
		checkAndFixActiveMarkersOrderIfNeeded();
	}

	private void lookupAddress(final MapMarker mapMarker) {
		if (mapMarker != null && mapMarker.pointDescription.isSearchingAddress(ctx)) {
			cancelPointAddressRequests(mapMarker.point);
			GeocodingLookupService.AddressLookupRequest lookupRequest =
					new GeocodingLookupService.AddressLookupRequest(mapMarker.point, new GeocodingLookupService.OnAddressLookupResult() {
						@Override
						public void geocodingDone(String address) {
							if (Algorithms.isEmpty(address)) {
								mapMarker.pointDescription.setName(PointDescription.getAddressNotFoundStr(ctx));
							} else {
								mapMarker.pointDescription.setName(address);
							}
							markersDbHelper.updateMarker(mapMarker);
							updateMarker(mapMarker);
						}
					}, null);
			ctx.getGeocodingLookupService().lookupAddress(lookupRequest);
		}
	}

	public boolean isGroupSynced(String id) {
		return markersDbHelper.getGroup(id) != null;
	}

	public void syncAllGroups() {
		List<MarkersSyncGroup> groups = markersDbHelper.getAllGroups();
		for (MarkersSyncGroup gr : groups) {
			syncGroup(gr);
		}
	}

	public void syncGroup(MarkersSyncGroup group) {
		if (!isGroupSynced(group.getId())) {
			return;
		}
		List<MapMarker> dbMarkers = markersDbHelper.getMarkersFromGroup(group);

		if (group.getType() == MarkersSyncGroup.FAVORITES_TYPE) {
			FavoriteGroup favGroup = ctx.getFavorites().getGroup(group.getName());
			if (favGroup == null) {
				return;
			}
			if (!favGroup.visible) {
				removeActiveMarkersFromSyncGroup(group.getId());
				return;
			}
			if (group.getColor() == -1) {
				group.setColor(favGroup.color);
			}

			for (FavouritePoint fp : favGroup.points) {
				addNewMarkerIfNeeded(group, dbMarkers, new LatLon(fp.getLatitude(), fp.getLongitude()), fp.getName());
			}

			removeOldMarkersIfNeeded(dbMarkers);
		} else if (group.getType() == MarkersSyncGroup.GPX_TYPE) {
			GpxSelectionHelper gpxHelper = ctx.getSelectedGpxHelper();
			File file = new File(group.getId());
			if (!file.exists()) {
				return;
			}

			SelectedGpxFile selectedGpxFile = gpxHelper.getSelectedFileByPath(group.getId());
			GPXFile gpx = selectedGpxFile == null ? null : selectedGpxFile.getGpxFile();
			if (gpx == null) {
				removeActiveMarkersFromSyncGroup(group.getId());
				return;
			}

			List<WptPt> gpxPoints = new LinkedList<>(gpx.getPoints());
			int defColor = ContextCompat.getColor(ctx, R.color.marker_red);
			for (WptPt pt : gpxPoints) {
				group.setColor(pt.getColor(defColor));
				addNewMarkerIfNeeded(group, dbMarkers, new LatLon(pt.lat, pt.lon), pt.name);
			}

			removeOldMarkersIfNeeded(dbMarkers);
		}
	}

	private void addNewMarkerIfNeeded(MarkersSyncGroup group, List<MapMarker> markers, LatLon latLon, String name) {
		boolean exists = false;

		for (MapMarker marker : markers) {
			if (marker.id.equals(group.getId() + name)) {
				exists = true;
				if (!marker.history && (!marker.point.equals(latLon))) {
					for (MapMarker m : mapMarkers) {
						if (m.id.equals(marker.id)) {
							m.point = latLon;
							updateMapMarker(m, true);
							break;
						}
					}
				}
				markers.remove(marker);
				break;
			}
		}

		if (!exists) {
			addMarkers(Collections.singletonList(latLon),
					Collections.singletonList(new PointDescription(POINT_TYPE_MAP_MARKER, name)), group);
		}
	}

	private void removeOldMarkersIfNeeded(List<MapMarker> markers) {
		if (!markers.isEmpty()) {
			boolean needRefresh = false;
			for (MapMarker marker : markers) {
				if (!marker.history) {
					markersDbHelper.removeMarker(marker, false);
					mapMarkers.remove(marker);
					needRefresh = true;
				}
			}
			if (needRefresh) {
				checkAndFixActiveMarkersOrderIfNeeded();
				refresh();
			}
		}
	}

	public void moveMapMarkerToHistory(MapMarker marker) {
		if (marker != null) {
			cancelPointAddressRequests(marker.point);
			markersDbHelper.moveMarkerToHistory(marker);
			mapMarkers.remove(marker);
			marker.history = true;
			marker.nextKey = MapMarkersDbHelper.HISTORY_NEXT_VALUE;
			mapMarkersHistory.add(marker);
			checkAndFixActiveMarkersOrderIfNeeded();
			sortMarkers(mapMarkersHistory, true, OsmandSettings.MapMarkersOrderByMode.DATE_ADDED_DESC);
			refresh();
		}
	}

	public void addMarker(MapMarker marker) {
		if (marker != null) {
			markersDbHelper.addMarker(marker);
			if (marker.history) {
				mapMarkersHistory.add(marker);
				sortMarkers(mapMarkersHistory, true, OsmandSettings.MapMarkersOrderByMode.DATE_ADDED_DESC);
			} else {
				mapMarkers.add(marker);
				checkAndFixActiveMarkersOrderIfNeeded();
			}
			refresh();
		}
	}

	public void restoreMarkerFromHistory(MapMarker marker, int position) {
		if (marker != null) {
			markersDbHelper.restoreMapMarkerFromHistory(marker);
			mapMarkersHistory.remove(marker);
			marker.history = false;
			mapMarkers.add(position, marker);
			checkAndFixActiveMarkersOrderIfNeeded();
			sortMarkers(mapMarkersHistory, true, OsmandSettings.MapMarkersOrderByMode.DATE_ADDED_DESC);
			refresh();
		}
	}

	public void restoreMarkersFromHistory(List<MapMarker> markers) {
		if (markers != null) {
			for (MapMarker marker : markers) {
				markersDbHelper.restoreMapMarkerFromHistory(marker);
				mapMarkersHistory.remove(marker);
				marker.history = false;
				mapMarkers.add(marker);
			}
			checkAndFixActiveMarkersOrderIfNeeded();
			sortMarkers(mapMarkersHistory, true, OsmandSettings.MapMarkersOrderByMode.DATE_ADDED_DESC);
			refresh();
		}
	}

	public void removeMarkerFromHistory(MapMarker marker) {
		if (marker != null) {
			markersDbHelper.removeMarker(marker, true);
			mapMarkersHistory.remove(marker);
			refresh();
		}
	}

	public List<MapMarker> getMapMarkers() {
		return mapMarkers;
	}

	public MapMarker getFirstMapMarker() {
		if (mapMarkers.size() > 0) {
			return mapMarkers.get(0);
		} else {
			return null;
		}
	}

	public List<MapMarker> getMapMarkersHistory() {
		return mapMarkersHistory;
	}

	public List<MapMarker> getSelectedMarkers() {
		List<MapMarker> list = new ArrayList<>();
		for (MapMarker m : this.mapMarkers) {
			if (m.selected) {
				list.add(m);
			}
		}
		return list;
	}

	public List<LatLon> getActiveMarkersLatLon() {
		List<LatLon> list = new ArrayList<>();
		for (MapMarker m : this.mapMarkers) {
			list.add(m.point);
		}
		return list;
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

	public List<LatLon> getMarkersHistoryLatLon() {
		List<LatLon> list = new ArrayList<>();
		for (MapMarker m : this.mapMarkersHistory) {
			list.add(m.point);
		}
		return list;
	}

	public void reverseActiveMarkersOrder() {
		cancelAddressRequests();
		Collections.reverse(mapMarkers);
		checkAndFixActiveMarkersOrderIfNeeded();
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
		mapMarkersHistory.addAll(mapMarkers);
		mapMarkers.clear();
		sortMarkers(mapMarkersHistory, true, OsmandSettings.MapMarkersOrderByMode.DATE_ADDED_DESC);
		refresh();
	}

	public void removeMarkersHistory() {
		cancelAddressRequests();
		markersDbHelper.clearAllMarkersHistory();
		mapMarkersHistory.clear();
		refresh();
	}

	public void addMarkersSyncGroup(MarkersSyncGroup group) {
		if (group != null) {
			if (markersDbHelper.getGroup(group.getId()) == null) {
				markersDbHelper.addGroup(group.getId(), group.getName(), group.getType());
			}
		}
	}

	public void removeMarkersSyncGroup(String id, boolean removeActiveMarkers) {
		if (id != null) {
			markersDbHelper.removeMarkersSyncGroup(id);
			if (removeActiveMarkers) {
				removeActiveMarkersFromSyncGroup(id);
			}
		}
	}

	public void removeActiveMarkersFromSyncGroup(String syncGroupId) {
		if (syncGroupId != null) {
			markersDbHelper.removeActiveMarkersFromSyncGroup(syncGroupId);
			for (Iterator<MapMarker> iterator = mapMarkers.iterator(); iterator.hasNext(); ) {
				String groupKey = iterator.next().groupKey;
				if (groupKey != null && groupKey.equals(syncGroupId)) {
					iterator.remove();
				}
			}
			checkAndFixActiveMarkersOrderIfNeeded();
			refresh();
		}
	}

	public void addMapMarker(LatLon point, PointDescription historyName) {
		addMarkers(Collections.singletonList(point), Collections.singletonList(historyName), null);
	}

	public void addMapMarkers(List<LatLon> points, List<PointDescription> historyNames, @Nullable MarkersSyncGroup group) {
		addMarkers(points, historyNames, group);
	}

	private void addMarkers(List<LatLon> points, List<PointDescription> historyNames, @Nullable MarkersSyncGroup group) {
		if (points.size() > 0) {
			int colorIndex = -1;
			for (int i = 0; i < points.size(); i++) {
				LatLon point = points.get(i);
				PointDescription historyName = historyNames.get(i);
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
						colorIndex = (mapMarkers.get(mapMarkers.size() - 1).colorIndex + 1) % MAP_MARKERS_COLORS_COUNT;
					} else {
						colorIndex = 0;
					}
				} else {
					colorIndex = (colorIndex + 1) % MAP_MARKERS_COLORS_COUNT;
				}

				MapMarker marker = new MapMarker(point, pointDescription, colorIndex, false, 0);
				if (group != null) {
					marker.id = group.getId() + marker.getName(ctx);
					if (markersDbHelper.getMarker(marker.id) != null) {
						continue;
					}
					marker.groupName = group.getName();
					marker.groupKey = group.getId();
				}
				marker.history = false;
				marker.nextKey = MapMarkersDbHelper.TAIL_NEXT_VALUE;
				markersDbHelper.addMarker(marker);
				mapMarkers.add(marker);
				checkAndFixActiveMarkersOrderIfNeeded();
			}
		}
	}

	public void updateMapMarker(MapMarker marker, boolean refresh) {
		if (marker != null) {
			markersDbHelper.updateMarker(marker);
			if (refresh) {
				refresh();
			}
		}
	}

	public void moveMapMarker(@Nullable MapMarker marker, LatLon latLon) {
		if (marker != null) {
			LatLon point = new LatLon(latLon.getLatitude(), latLon.getLongitude());
			int index = mapMarkers.indexOf(marker);
			if (index != -1) {
				mapMarkers.get(index).point = point;
			}
			marker.point = point;
			markersDbHelper.updateMarker(marker);
			checkAndFixActiveMarkersOrderIfNeeded();
			refresh();
		}
	}

	public void moveMarkerToTop(MapMarker marker) {
		int i = mapMarkers.indexOf(marker);
		if (i != -1 && mapMarkers.size() > 1) {
			mapMarkers.remove(i);
			mapMarkers.add(0, marker);
			checkAndFixActiveMarkersOrderIfNeeded();
			refresh();
		}
	}

	public void saveMapMarkers(List<MapMarker> markers, List<MapMarker> markersHistory) {
		if (markers != null) {
			List<LatLon> ls = new ArrayList<>(markers.size());
			List<String> names = new ArrayList<>(markers.size());
			List<Integer> colors = new ArrayList<>(markers.size());
			List<Boolean> selections = new ArrayList<>(markers.size());
			List<Long> creationDates = new ArrayList<>(markers.size());
			for (MapMarker marker : markers) {
				ls.add(marker.point);
				names.add(PointDescription.serializeToString(marker.pointDescription));
				colors.add(marker.colorIndex);
				selections.add(marker.selected);
				creationDates.add(marker.creationDate);
			}
			settings.saveMapMarkers(ls, names, colors, selections, creationDates);
		}

		if (markersHistory != null) {
			List<LatLon> ls = new ArrayList<>(markersHistory.size());
			List<String> names = new ArrayList<>(markersHistory.size());
			List<Integer> colors = new ArrayList<>(markersHistory.size());
			List<Long> creationDates = new ArrayList<>(markersHistory.size());
			for (MapMarker marker : markersHistory) {
				ls.add(marker.point);
				names.add(PointDescription.serializeToString(marker.pointDescription));
				colors.add(marker.colorIndex);
				creationDates.add(marker.creationDate);
			}
			settings.saveMapMarkersHistory(ls, names, colors, creationDates);
		}

		if (markers != null || markersHistory != null) {
			loadMarkers();
			refresh();
		}
	}

	public void addListener(MapMarkerChangedListener l) {
		if (!listeners.contains(l)) {
			listeners.add(l);
		}
	}

	public void removeListener(MapMarkerChangedListener l) {
		listeners.remove(l);
	}

	private void updateMarker(MapMarker marker) {
		for (MapMarkerChangedListener l : listeners) {
			l.onMapMarkerChanged(marker);
		}
	}

	private void updateMarkers() {
		for (MapMarkerChangedListener l : listeners) {
			l.onMapMarkersChanged();
		}
	}

	public void refresh() {
		updateMarkers();
	}

	private void cancelAddressRequests() {
		List<LatLon> list = getActiveMarkersLatLon();
		for (LatLon latLon : list) {
			cancelPointAddressRequests(latLon);
		}
		list = getMarkersHistoryLatLon();
		for (LatLon latLon : list) {
			cancelPointAddressRequests(latLon);
		}
	}

	private void cancelPointAddressRequests(LatLon latLon) {
		if (latLon != null) {
			ctx.getGeocodingLookupService().cancel(latLon);
		}
	}

	public void generateGpx() {
		final File dir = ctx.getAppPath(IndexConstants.GPX_INDEX_DIR + "/map markers");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		Date date = new Date();
		String fileName = DateFormat.format("yyyy-MM-dd", date).toString() + "_" + new SimpleDateFormat("HH-mm_EEE", Locale.US).format(date);
		File fout = new File(dir, fileName + ".gpx");
		int ind = 1;
		while (fout.exists()) {
			fout = new File(dir, fileName + "_" + (++ind) + ".gpx");
		}
		GPXFile file = new GPXFile();
		for (MapMarker marker : mapMarkers) {
			WptPt wpt = new WptPt();
			wpt.lat = marker.getLatitude();
			wpt.lon = marker.getLongitude();
			wpt.setColor(ctx.getResources().getColor(MapMarker.getColorId(marker.colorIndex)));
			wpt.name = marker.getOnlyName();
			file.addPoint(wpt);
		}
		GPXUtilities.writeGpxFile(fout, file, ctx);
	}
}
