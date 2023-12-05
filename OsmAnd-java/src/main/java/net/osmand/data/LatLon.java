package net.osmand.data;

import net.osmand.util.MapUtils;

import java.io.Serializable;

public class LatLon implements Serializable {

	private static final long serialVersionUID = 1811582709897737392L;
	private final double latitude;
	private final double longitude;

	public LatLon(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		int temp;
		temp = (int) Math.floor(latitude * 10000);
		result = prime * result + temp;
		temp = (int) Math.floor(longitude * 10000);
		result = prime * result + temp;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		LatLon other = (LatLon) obj;
		return MapUtils.areLatLonEqual(this, other);
	}

	@Override
	public String toString() {
		return "Lat " + ((float) latitude) + " Lon " + ((float) longitude);
	}
}