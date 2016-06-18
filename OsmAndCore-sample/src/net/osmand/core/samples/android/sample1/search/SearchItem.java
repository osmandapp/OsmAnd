package net.osmand.core.samples.android.sample1.search;

import net.osmand.core.jni.LatLon;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.Utilities;

public abstract class SearchItem {

	protected double latitude;
	protected double longitude;

	public SearchItem(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public SearchItem(PointI location31) {
		LatLon latLon = Utilities.convert31ToLatLon(location31);
		latitude = latLon.getLatitude();
		longitude = latLon.getLongitude();
	}

	public abstract String getName();

	public abstract String getType();

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	@Override
	public String toString() {
		return getName() + " {lat:" + getLatitude() + " lon: " + getLongitude() + "}";
	}
}
