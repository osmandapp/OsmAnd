package net.osmand.plus.avoidroads;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import java.io.Serializable;

public class AvoidRoadInfo implements Serializable {

	private final long id;
	private final double direction;
	private final double latitude;
	private final double longitude;
	private final String name;
	private final String appModeKey;

	public AvoidRoadInfo(long id, double direction, double latitude, double longitude, String name, String appModeKey) {
		this.id = id;
		this.direction = direction;
		this.latitude = latitude;
		this.longitude = longitude;
		this.name = name;
		this.appModeKey = appModeKey;
	}

	public long getId() {
		return id;
	}

	public double getDirection() {
		return direction;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	@NonNull
	public LatLon getLatLon() {
		return new LatLon(latitude, longitude);
	}

	@NonNull
	public String getName(@NonNull Context context) {
		if (!Algorithms.isEmpty(name)) {
			return name;
		}
		return context.getString(R.string.shared_string_road);
	}

	public String getAppModeKey() {
		return appModeKey;
	}

	@Override
	public int hashCode() {
		return Algorithms.hash(id, latitude, longitude, name, appModeKey);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		AvoidRoadInfo other = (AvoidRoadInfo) obj;
		return Math.abs(latitude - other.latitude) < 0.00001
				&& Math.abs(longitude - other.longitude) < 0.00001
				&& Algorithms.objectEquals(name, other.name)
				&& Algorithms.objectEquals(appModeKey, other.appModeKey)
				&& id == other.id;
	}
}
