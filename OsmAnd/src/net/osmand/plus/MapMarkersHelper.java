package net.osmand.plus;

import android.content.Context;

import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MapMarkersHelper {
	public static final int MAP_MARKERS_COLORS_COUNT = 7;

	private List<MapMarker> mapMarkers = new ArrayList<>();
	private List<MapMarker> sortedMapMarkers = new ArrayList<>();
	private List<MapMarker> mapMarkersHistory = new ArrayList<>();
	private OsmandSettings settings;
	private List<MapMarkerChangedListener> listeners = new ArrayList<>();
	private OsmandApplication ctx;
	private boolean startFromMyLocation;

	public interface MapMarkerChangedListener {
		void onMapMarkerChanged(MapMarker mapMarker);

		void onMapMarkersChanged();
	}

	public static class MapMarker implements LocationPoint {
		public LatLon point;
		private PointDescription pointDescription;
		public int colorIndex;
		public int pos;
		public int index;
		public boolean history;
		public boolean selected;
		public int dist;

		public MapMarker(LatLon point, PointDescription name, int colorIndex, int pos,
						 boolean selected, int index) {
			this.point = point;
			this.pointDescription = name;
			this.colorIndex = colorIndex;
			this.pos = pos;
			this.selected = selected;
			this.index = index;
		}

		public PointDescription getPointDescription(Context ctx) {
			return new PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, ctx.getString(R.string.map_marker, ""),
					getOnlyName());
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

	}

	public MapMarkersHelper(OsmandApplication ctx) {
		this.ctx = ctx;
		settings = ctx.getSettings();
		startFromMyLocation = settings.ROUTE_MAP_MARKERS_START_MY_LOC.get();
		readFromSettings();
	}

	public boolean isStartFromMyLocation() {
		return startFromMyLocation;
	}

	public void setStartFromMyLocation(boolean startFromMyLocation) {
		this.startFromMyLocation = startFromMyLocation;
		settings.ROUTE_MAP_MARKERS_START_MY_LOC.set(startFromMyLocation);
	}

	private void readFromSettings() {
		mapMarkers.clear();
		mapMarkersHistory.clear();
		List<LatLon> ips = settings.getMapMarkersPoints();
		List<String> desc = settings.getMapMarkersPointDescriptions(ips.size());
		List<Integer> colors = settings.getMapMarkersColors(ips.size());
		List<Integer> positions = settings.getMapMarkersPositions(ips.size());
		List<Boolean> selections = settings.getMapMarkersSelections(ips.size());
		int colorIndex = 0;
		int pos = 0;
		for (int i = 0; i < ips.size(); i++) {
			if (colors.size() > i) {
				colorIndex = colors.get(i);
			}
			if (positions.size() > i) {
				pos = positions.get(i);
			} else {
				pos++;
			}
			MapMarker mapMarker = new MapMarker(ips.get(i),
					PointDescription.deserializeFromString(desc.get(i), ips.get(i)), colorIndex,
					pos, selections.get(i), i);
			mapMarkers.add(mapMarker);
			lookupAddress(mapMarker, false);
		}

		updateSortedArray();

		ips = settings.getMapMarkersHistoryPoints();
		desc = settings.getMapMarkersHistoryPointDescriptions(ips.size());
		for (int i = 0; i < ips.size(); i++) {
			MapMarker mapMarker = new MapMarker(ips.get(i),
					PointDescription.deserializeFromString(desc.get(i), ips.get(i)), 0, 0, false, i);
			mapMarker.history = true;
			mapMarkersHistory.add(mapMarker);
			lookupAddress(mapMarker, true);
		}
	}

	private void updateSortedArray() {
		sortedMapMarkers.clear();
		sortedMapMarkers.addAll(mapMarkers);
		Collections.sort(sortedMapMarkers, new Comparator<MapMarker>() {
			@Override
			public int compare(MapMarker lhs, MapMarker rhs) {
				return lhs.pos < rhs.pos ? -1 : (lhs.pos == rhs.pos ? 0 : 1);
			}
		});
	}

	public void normalizePositions() {
		for (int i = 0; i < sortedMapMarkers.size(); i++) {
			MapMarker marker = sortedMapMarkers.get(i);
			marker.pos = i;
		}
		saveMapMarkers(mapMarkers, null);
	}

	private void lookupAddress(final MapMarker mapMarker, final boolean history) {
		if (mapMarker != null && mapMarker.pointDescription.isSearchingAddress(ctx)) {
			cancelPointAddressRequests(mapMarker.point);
			GeocodingLookupService.AddressLookupRequest lookupRequest = new GeocodingLookupService.AddressLookupRequest(mapMarker.point, new GeocodingLookupService.OnAddressLookupResult() {
				@Override
				public void geocodingDone(String address) {
					if (Algorithms.isEmpty(address)) {
						mapMarker.pointDescription.setName(PointDescription.getAddressNotFoundStr(ctx));
					} else {
						mapMarker.pointDescription.setName(address);
					}
					if (history) {
						settings.updateMapMarkerHistory(mapMarker.point.getLatitude(), mapMarker.point.getLongitude(),
								mapMarker.pointDescription, mapMarker.colorIndex);
					} else {
						settings.updateMapMarker(mapMarker.point.getLatitude(), mapMarker.point.getLongitude(),
								mapMarker.pointDescription, mapMarker.colorIndex, mapMarker.pos, mapMarker.selected);
					}
					updateMarker(mapMarker);
				}
			}, null);
			ctx.getGeocodingLookupService().lookupAddress(lookupRequest);
		}
	}

	public void removeMapMarker(int index) {
		settings.deleteMapMarker(index);
		MapMarker mapMarker = mapMarkers.remove(index);
		cancelPointAddressRequests(mapMarker.point);
		int ind = 0;
		for (MapMarker marker : mapMarkers) {
			marker.index = ind++;
		}
		updateSortedArray();
		refresh();
	}

	public List<MapMarker> getActiveMapMarkers() {
		return mapMarkers;
	}

	public MapMarker getFirstMapMarker() {
		if (mapMarkers.size() > 0) {
			return mapMarkers.get(0);
		} else {
			return null;
		}
	}

	public List<MapMarker> getSortedMapMarkers() {
		return sortedMapMarkers;
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

		List<MapMarker> markers = new ArrayList<>(mapMarkers.size());
		for (int i = mapMarkers.size() - 1; i >= 0; i--) {
			MapMarker marker = mapMarkers.get(i);
			markers.add(marker);
		}
		mapMarkers = markers;
		saveMapMarkers(mapMarkers, null);
	}

	public void removeActiveMarkers() {
		cancelAddressRequests();
		for (int i = mapMarkers.size() - 1; i>= 0; i--) {
			MapMarker marker = mapMarkers.get(i);
			addMapMarkerHistory(marker);
		}
		settings.clearActiveMapMarkers();
		readFromSettings();
		refresh();
	}

	public void removeMarkersHistory() {
		cancelAddressRequests();
		settings.clearMapMarkersHistory();
		readFromSettings();
		refresh();
	}

	public void addMapMarker(LatLon point, PointDescription historyName) {
		List<LatLon> points = new ArrayList<>(1);
		List<PointDescription> historyNames = new ArrayList<>(1);
		points.add(point);
		historyNames.add(historyName);
		addMapMarkers(points, historyNames);
	}

	public void addMapMarkers(List<LatLon> points, List<PointDescription> historyNames) {
		if (points.size() > 0) {
			int colorIndex = -1;
			double[] latitudes = new double[points.size()];
			double[] longitudes = new double[points.size()];
			List<PointDescription> pointDescriptions = new ArrayList<>();
			int[] colorIndexes = new int[points.size()];
			int[] positions = new int[points.size()];
			boolean[] selections = new boolean[points.size()];
			int[] indexes = new int[points.size()];
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
					if (sortedMapMarkers.size() > 0) {
						colorIndex = (sortedMapMarkers.get(0).colorIndex + 1) % MAP_MARKERS_COLORS_COUNT;
					} else {
						colorIndex = 0;
					}
				} else {
					colorIndex = (colorIndex + 1) % MAP_MARKERS_COLORS_COUNT;
				}

				latitudes[i] = point.getLatitude();
				longitudes[i] = point.getLongitude();
				pointDescriptions.add(pointDescription);
				colorIndexes[i] = colorIndex;
				positions[i] = -1 - i;
				selections[i] = false;
				indexes[i] = 0;
			}
			/* adding map marker to second topbar's row
			if (sortedMapMarkers.size() > 0) {
				MapMarker firstMarker = sortedMapMarkers.get(0);
				settings.updateMapMarker(firstMarker.getLatitude(), firstMarker.getLongitude(),
						firstMarker.pointDescription, firstMarker.colorIndex, -points.size(), firstMarker.selected);
			}
			*/
			settings.insertMapMarkers(latitudes, longitudes, pointDescriptions, colorIndexes, positions,
					selections, indexes);
			readFromSettings();
			normalizePositions();
		}
	}

	public void updateMapMarker(MapMarker marker, boolean refresh) {
		if (marker != null) {
			settings.updateMapMarker(marker.getLatitude(), marker.getLongitude(),
					marker.pointDescription, marker.colorIndex, marker.pos, marker.selected);
			if (refresh) {
				readFromSettings();
				refresh();
			}
		}
	}

	public void removeMapMarker(MapMarker marker) {
		if (marker != null) {
			settings.deleteMapMarker(marker.index);
			readFromSettings();
			refresh();
		}
	}

	public void addMapMarkerHistory(MapMarker marker) {
		if (marker != null) {
			settings.insertMapMarkerHistory(marker.getLatitude(), marker.getLongitude(), marker.pointDescription, marker.colorIndex, 0);
			readFromSettings();
			refresh();
		}
	}

	public void removeMapMarkerHistory(MapMarker marker) {
		if (marker != null) {
			settings.deleteMapMarkerHistory(marker.index);
			readFromSettings();
			refresh();
		}
	}

	public void saveMapMarkers(List<MapMarker> markers, List<MapMarker> markersHistory) {
		if (markers != null) {
			List<LatLon> ls = new ArrayList<>(markers.size());
			List<String> names = new ArrayList<>(markers.size());
			List<Integer> colors = new ArrayList<>(markers.size());
			List<Integer> positions = new ArrayList<>(markers.size());
			List<Boolean> selections = new ArrayList<>(markers.size());
			for (MapMarker marker : markers) {
				ls.add(marker.point);
				names.add(PointDescription.serializeToString(marker.pointDescription));
				colors.add(marker.colorIndex);
				positions.add(marker.pos);
				selections.add(marker.selected);
			}
			settings.saveMapMarkers(ls, names, colors, positions, selections);
		}

		if (markersHistory != null) {
			List<LatLon> ls = new ArrayList<>(markersHistory.size());
			List<String> names = new ArrayList<>(markersHistory.size());
			List<Integer> colors = new ArrayList<>(markersHistory.size());
			for (MapMarker marker : markersHistory) {
				ls.add(marker.point);
				names.add(PointDescription.serializeToString(marker.pointDescription));
				colors.add(marker.colorIndex);
			}
			settings.saveMapMarkersHistory(ls, names, colors);
		}

		if (markers != null || markersHistory != null) {
			readFromSettings();
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
}
