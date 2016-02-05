package net.osmand.plus;

import android.content.Context;

import net.osmand.StateChangedListener;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.List;

public class MapMarkersHelper {
	private List<MapMarker> mapMarkers = new ArrayList<>();
	private List<MapMarker> mapMarkersHistory = new ArrayList<>();
	private OsmandSettings settings;
	private List<StateChangedListener<Void>> listeners = new ArrayList<StateChangedListener<Void>>();
	private OsmandApplication ctx;

	public static class MapMarker implements LocationPoint {
		public LatLon point;
		private PointDescription pointDescription;
		public int colorIndex;
		public int index;

		public MapMarker(LatLon point, PointDescription name, int colorIndex) {
			this.point = point;
			this.pointDescription = name;
			this.colorIndex = colorIndex;
		}

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

		public static MapMarker create(LatLon point, PointDescription name, int color) {
			if (point != null) {
				return new MapMarker(point, name, color);
			}
			return null;
		}

		public double getLatitude() {
			return point.getLatitude();
		}

		public double getLongitude() {
			return point.getLongitude();
		}

		public int getColorIndex() {
			return colorIndex;
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
			final MapMarker mapMarker = new MapMarker(ips.get(i),
					PointDescription.deserializeFromString(desc.get(i), ips.get(i)), colors.get(i), i);
			mapMarkers.add(mapMarker);
			lookupAddress(mapMarker, false);
		}
		ips = settings.getMapMarkersHistoryPoints();
		desc = settings.getMapMarkersHistoryPointDescriptions(ips.size());
		colors = settings.getMapMarkersHistoryColors(ips.size());
		for (int i = 0; i < ips.size(); i++) {
			final MapMarker mapMarker = new MapMarker(ips.get(i),
					PointDescription.deserializeFromString(desc.get(i), ips.get(i)), colors.get(i), i);
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
					mapMarker.pointDescription.setName(address);
					if (history) {
						settings.updateMapMarkerHistory(mapMarker.point.getLatitude(), mapMarker.point.getLongitude(),
								mapMarker.pointDescription, mapMarker.colorIndex);
					} else {
						settings.updateMapMarker(mapMarker.point.getLatitude(), mapMarker.point.getLongitude(),
								mapMarker.pointDescription, mapMarker.colorIndex);
					}
					refresh();
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
		List<LatLon> list = new ArrayList<LatLon>();
		for (MapMarker m : this.mapMarkers) {
			list.add(m.point);
		}
		return list;
	}

	public List<LatLon> getMarkersHistoryLatLon() {
		List<LatLon> list = new ArrayList<LatLon>();
		for (MapMarker m : this.mapMarkersHistory) {
			list.add(m.point);
		}
		return list;
	}

	public void removeActiveMarkers() {
		cancelAddressRequests();

		settings.clearIntermediatePoints();
		mapMarkers.clear();
		readFromSettings();
		refresh();
	}

	public void removeMarkersHistory() {
		cancelAddressRequests();

		settings.clearIntermediatePoints();
		mapMarkersHistory.clear();
		readFromSettings();
		refresh();
	}

	public void addListener(StateChangedListener<Void> l) {
		listeners.add(l);
	}

	private void updateListeners() {
		for (StateChangedListener<Void> l : listeners) {
			l.stateChanged(null);
		}
	}

	public void refresh() {
		updateListeners();
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
