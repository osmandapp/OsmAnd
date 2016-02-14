package net.osmand.plus;

import android.content.Context;

import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class MapMarkersHelper {
	public static final int MAP_MARKERS_COLORS_COUNT = 7;

	private List<MapMarker> mapMarkers = new ArrayList<>();
	private List<MapMarker> mapMarkersHistory = new ArrayList<>();
	private OsmandSettings settings;
	private List<MapMarkerChangedListener> listeners = new ArrayList<>();
	private OsmandApplication ctx;

	public interface MapMarkerChangedListener {
		void onMapMarkerChanged(MapMarker mapMarker);
		void onMapMarkersChanged();
	}

	public static class MapMarker implements LocationPoint {
		public LatLon point;
		private PointDescription pointDescription;
		public int colorIndex;
		public int index;
		public boolean history;
		public int dist;

		public MapMarker(LatLon point, PointDescription name, int colorIndex, int index) {
			this.point = point;
			this.pointDescription = name;
			this.colorIndex = colorIndex;
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

		public boolean isSearchingAddress(Context ctx) {
			return pointDescription != null && pointDescription.isSearchingAddress(ctx);
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
		this.settings = ctx.getSettings();
		readFromSettings();
	}

	private void readFromSettings() {
		mapMarkers.clear();
		mapMarkersHistory.clear();
		List<LatLon> ips = settings.getMapMarkersPoints();
		List<String> desc = settings.getMapMarkersPointDescriptions(ips.size());
		List<Integer> colors = settings.getMapMarkersColors(ips.size());
		for (int i = 0; i < ips.size(); i++) {
			MapMarker mapMarker = new MapMarker(ips.get(i),
					PointDescription.deserializeFromString(desc.get(i), ips.get(i)), colors.get(i), i);
			mapMarkers.add(mapMarker);
			lookupAddress(mapMarker, false);
		}
		ips = settings.getMapMarkersHistoryPoints();
		desc = settings.getMapMarkersHistoryPointDescriptions(ips.size());
		colors = settings.getMapMarkersHistoryColors(ips.size());
		for (int i = 0; i < ips.size(); i++) {
			MapMarker mapMarker = new MapMarker(ips.get(i),
					PointDescription.deserializeFromString(desc.get(i), ips.get(i)), colors.get(i), i);
			mapMarker.history = true;
			mapMarkersHistory.add(mapMarker);
			lookupAddress(mapMarker, true);
		}
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
								mapMarker.pointDescription, mapMarker.colorIndex);
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
		refresh();
	}

	public List<MapMarker> getActiveMapMarkers() {
		return mapMarkers;
	}

	public List<MapMarker> getMapMarkersHistory() {
		return mapMarkersHistory;
	}

	public List<LatLon> getActiveMarkersLatLon() {
		List<LatLon> list = new ArrayList<>();
		for (MapMarker m : this.mapMarkers) {
			list.add(m.point);
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

	public void removeActiveMarkers() {
		cancelAddressRequests();

		settings.clearActiveMapMarkers();
		mapMarkers.clear();
		readFromSettings();
		refresh();
	}

	public void removeMarkersHistory() {
		cancelAddressRequests();

		settings.clearMapMarkersHistory();
		mapMarkersHistory.clear();
		readFromSettings();
		refresh();
	}

	public void addMapMarker(LatLon point, PointDescription historyName) {
		if (point != null) {
			final PointDescription pointDescription;
			if (historyName == null) {
				pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
			} else {
				pointDescription = historyName;
			}
			if (pointDescription.isLocation() && Algorithms.isEmpty(pointDescription.getName())) {
				pointDescription.setName(PointDescription.getSearchAddressStr(ctx));
			}
			int colorIndex;
			if (mapMarkers.size() > 0) {
				colorIndex = (mapMarkers.get(0).colorIndex + 1) % MAP_MARKERS_COLORS_COUNT;
			} else {
				colorIndex = 0;
			}
			settings.insertMapMarker(point.getLatitude(), point.getLongitude(),
					pointDescription, colorIndex, 0);

			readFromSettings();
			refresh();
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
			for (MapMarker marker : markers) {
				ls.add(marker.point);
				names.add(PointDescription.serializeToString(marker.pointDescription));
				colors.add(marker.colorIndex);
			}
			settings.saveMapMarkers(ls, names, colors);
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
