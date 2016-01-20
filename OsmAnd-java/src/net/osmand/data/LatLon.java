package net.osmand.data;

import java.io.Serializable;

public class LatLon implements Serializable {
	private final double longitude;
	private final double latitude;

	public LatLon(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		int temp;
		temp = (int)Math.floor(latitude * 10000);
		result = prime * result + temp;
		temp = (int)Math.floor(longitude * 10000);
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
		return Math.abs(latitude - other.latitude) < 0.00001
				&& Math.abs(longitude - other.longitude) < 0.00001;
	}

	@Override
	public String toString() {
		return "Lat " + ((float)latitude) + " Lon " + ((float)longitude); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

}