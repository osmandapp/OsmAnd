package net.osmand.plus.avoidroads;

import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

public class AvoidRoadInfo {

	public long id;
	public double direction = Double.NaN;
	public double latitude;
	public double longitude;
	public String name;
	public String appModeKey;

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
