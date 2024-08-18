package net.osmand;

import net.osmand.data.LatLon;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class LocationsHolder {

	private static final int LOCATION_TYPE_UNKNOWN = -1;
	private static final int LOCATION_TYPE_LATLON = 0;
	private static final int LOCATION_TYPE_LOCATION = 1;
	private static final int LOCATION_TYPE_WPTPT = 2;

	private List<LatLon> latLonList;
	private List<Location> locationList;
	private List<WptPt> wptPtList;
	private int locationType;
	private int size;

	@SuppressWarnings("unchecked")
	public LocationsHolder(List<?> locations) {
		this.locationType = resolveLocationType(locations);
		switch (locationType) {
			case LOCATION_TYPE_LATLON:
				latLonList = new ArrayList<>((List<LatLon>) locations);
				size = locations.size();
				break;
			case LOCATION_TYPE_LOCATION:
				locationList = new ArrayList<>((List<Location>) locations);
				size = locations.size();
				break;
			case LOCATION_TYPE_WPTPT:
				wptPtList = new ArrayList<>((List<WptPt>) locations);
				size = locations.size();
				break;
		}
	}

	private int resolveLocationType(List<?> locations) {
		if (!Algorithms.isEmpty(locations)) {
			Object locationObj = locations.get(0);
			if (locationObj instanceof LatLon) {
				return LOCATION_TYPE_LATLON;
			} else if (locationObj instanceof WptPt) {
				return LOCATION_TYPE_WPTPT;
			} else if (locationObj instanceof Location) {
				return LOCATION_TYPE_LOCATION;
			} else {
				throw new IllegalArgumentException("Unsupported location type: " + locationObj.getClass().getSimpleName());
			}
		}
		return LOCATION_TYPE_UNKNOWN;
	}

	public double getLatitude(int index) {
		switch (locationType) {
			case LOCATION_TYPE_LATLON:
				return latLonList.get(index).getLatitude();
			case LOCATION_TYPE_LOCATION:
				return locationList.get(index).getLatitude();
			case LOCATION_TYPE_WPTPT:
				return wptPtList.get(index).getLatitude();
			default:
				return 0;
		}
	}

	public double getLongitude(int index) {
		switch (locationType) {
			case LOCATION_TYPE_LATLON:
				return latLonList.get(index).getLongitude();
			case LOCATION_TYPE_LOCATION:
				return locationList.get(index).getLongitude();
			case LOCATION_TYPE_WPTPT:
				return wptPtList.get(index).getLongitude();
			default:
				return 0;
		}
	}

	public int getSize() {
		return size;
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> getList(int locationType) {
		List<T> res = new ArrayList<>();
		if (size > 0) {
			for (int i = 0; i < size; i++) {
				switch (locationType) {
					case LOCATION_TYPE_LATLON:
						res.add((T) getLatLon(i));
						break;
					case LOCATION_TYPE_LOCATION:
						res.add((T) getLocation(i));
						break;
					case LOCATION_TYPE_WPTPT:
						res.add((T) getWptPt(i));
						break;
				}
			}
		}
		return res;
	}

	public List<LatLon> getLatLonList() {
		if (this.locationType == LOCATION_TYPE_LATLON) {
			return latLonList;
		} else {
			return getList(LOCATION_TYPE_LATLON);
		}
	}

	public List<WptPt> getWptPtList() {
		if (this.locationType == LOCATION_TYPE_WPTPT) {
			return wptPtList;
		} else {
			return getList(LOCATION_TYPE_WPTPT);
		}
	}

	public List<Location> getLocationsList() {
		if (this.locationType == LOCATION_TYPE_LOCATION) {
			return locationList;
		} else {
			return getList(LOCATION_TYPE_LOCATION);
		}
	}

	public long getTime(int index) {
		if (this.locationType == LOCATION_TYPE_WPTPT) {
			return wptPtList.get(index).time;
		} else {
			return 0;
		}
	}

	public LatLon getLatLon(int index) {
		if (this.locationType == LOCATION_TYPE_LATLON) {
			return latLonList.get(index);
		} else {
			return new LatLon(getLatitude(index), getLongitude(index));
		}
	}

	public WptPt getWptPt(int index) {
		if (this.locationType == LOCATION_TYPE_WPTPT) {
			return wptPtList.get(index);
		} else {
			WptPt wptPt = new WptPt();
			wptPt.lat = getLatitude(index);
			wptPt.lon = getLongitude(index);
			return wptPt;
		}
	}

	public Location getLocation(int index) {
		if (this.locationType == LOCATION_TYPE_LOCATION) {
			return locationList.get(index);
		} else {
			return new Location("", getLatitude(index), getLongitude(index));
		}
	}
}
