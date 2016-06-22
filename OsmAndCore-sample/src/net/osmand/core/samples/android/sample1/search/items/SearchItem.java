package net.osmand.core.samples.android.sample1.search.items;

import net.osmand.core.jni.LatLon;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QStringList;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.core.jni.Utilities;

import java.util.HashMap;
import java.util.Map;

public abstract class SearchItem {

	protected double latitude;
	protected double longitude;
	protected String nativeName;
	protected Map<String, String> localizedNames = new HashMap<>();

	private double distance;
	private float priority;

	protected SearchItem() {
	}

	protected SearchItem(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	protected SearchItem(PointI location31) {
		LatLon latLon = Utilities.convert31ToLatLon(location31);
		latitude = latLon.getLatitude();
		longitude = latLon.getLongitude();
	}

	public String getName() {
		return nativeName;
	}

	public abstract String getType();

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getNativeName() {
		return nativeName;
	}

	public Map<String, String> getLocalizedNames() {
		return localizedNames;
	}

	protected void setLocation(PointI location31) {
		LatLon latLon = Utilities.convert31ToLatLon(location31);
		latitude = latLon.getLatitude();
		longitude = latLon.getLongitude();
	}

	protected void setNativeName(String nativeName) {
		this.nativeName = nativeName;
	}

	protected void addLocalizedNames(QStringStringHash localizedNames) {
		QStringList locNamesKeys = localizedNames.keys();
		for (int i = 0; i < locNamesKeys.size(); i++) {
			String key = locNamesKeys.get(i);
			String val = localizedNames.get(key);
			this.localizedNames.put(key, val);
		}
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public float getPriority() {
		return priority;
	}

	public void setPriority(float priority) {
		this.priority = priority;
	}

	@Override
	public String toString() {
		return getName() + " (" + getType() + ") {lat:" + getLatitude() + " lon: " + getLongitude() + "}";
	}
}
