package net.osmand.plus;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;

import net.osmand.IndexConstants;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.mapmarkers.MapMarkersDbHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

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
			return new PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, ctx.getString(R.string.map_marker),
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

		public static int getColorId(int colorIndex) {
			int colorId;
			switch (colorIndex) {
				case 0:
					colorId = R.color.marker_blue;
					break;
				case 1:
					colorId = R.color.marker_green;
					break;
				case 2:
					colorId = R.color.marker_orange;
					break;
				case 3:
					colorId = R.color.marker_red;
					break;
				case 4:
					colorId = R.color.marker_yellow;
					break;
				case 5:
					colorId = R.color.marker_teal;
					break;
				case 6:
					colorId = R.color.marker_purple;
					break;
				default:
					colorId = R.color.marker_blue;
			}
			return colorId;
		}
	}

	public static class MarkersSyncGroup {

		public static final int FAVORITES_TYPE = 0;
		public static final int GPX_TYPE = 1;

		private String id;
		private String name;
		private int type;

		public MarkersSyncGroup(@NonNull String id, @NonNull String name, int type) {
			this.id = id;
			this.name = name;
			this.type = type;
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
		sortMarkers(markersHistory, true);
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

	private void sortMarkers(List<MapMarker> markers, final boolean history) {
		Collections.sort(markers, new Comparator<MapMarker>() {
			@Override
			public int compare(MapMarker mapMarker1, MapMarker mapMarker2) {
				long firstMarkerDate = history ? mapMarker1.visitedDate : mapMarker1.creationDate;
				long secondMarkerDate = history ? mapMarker2.visitedDate : mapMarker2.creationDate;
				if (firstMarkerDate > secondMarkerDate) {
					return -1;
				} else if (firstMarkerDate == secondMarkerDate) {
					return 0;
				} else {
					return 1;
				}
			}
		});
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

	public void syncGroup(MarkersSyncGroup group) {

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
			sortMarkers(mapMarkersHistory, true);
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
			sortMarkers(mapMarkersHistory, true);
			refresh();
		}
	}

	public void removeMarkerFromHistory(MapMarker marker) {
		if (marker != null) {
			markersDbHelper.removeMarkerFromHistory(marker);
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
		markersDbHelper.moveAllActiveMarkersToHistory();
		for (MapMarker marker : mapMarkers) {
			marker.history = true;
			marker.nextKey = MapMarkersDbHelper.HISTORY_NEXT_VALUE;
		}
		mapMarkersHistory.addAll(mapMarkers);
		mapMarkers.clear();
		sortMarkers(mapMarkersHistory, true);
		refresh();
	}

	public void removeMarkersHistory() {
		cancelAddressRequests();
		markersDbHelper.clearAllMarkersHistory();
		mapMarkersHistory.clear();
		refresh();
	}

	public void addMapMarker(LatLon point, PointDescription historyName) {
		addMarkers(Collections.singletonList(point), Collections.singletonList(historyName), null);
	}

	public void addMapMarkers(List<LatLon> points, List<PointDescription> historyNames, @Nullable MarkersSyncGroup group) {
		addMarkers(points, historyNames, group);
	}

	public void addMapMarkers(List<LatLon> points, List<PointDescription> historyNames) {
		addMarkers(points, historyNames, null);
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
					if (markersDbHelper.getGroup(group.getId()) == null) {
						markersDbHelper.addGroup(group.getId(), group.getName(), group.getType());
					}
					marker.id = group.getId() + marker.getName(ctx);
					marker.groupName = group.getName();
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
				loadMarkers();
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
		GPXUtilities.GPXFile file = new GPXUtilities.GPXFile();
		for (MapMarker marker : markersDbHelper.getActiveMarkers()) {
			GPXUtilities.WptPt wpt = new GPXUtilities.WptPt();
			wpt.lat = marker.getLatitude();
			wpt.lon = marker.getLongitude();
			wpt.setColor(ctx.getResources().getColor(MapMarker.getColorId(marker.colorIndex)));
			wpt.name = marker.getOnlyName();
			file.points.add(wpt);
		}
		GPXUtilities.writeGpxFile(fout, file, ctx);
	}
}
