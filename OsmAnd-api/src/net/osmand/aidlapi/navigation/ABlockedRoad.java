package net.osmand.aidlapi.navigation;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class ABlockedRoad extends AidlParams {

	private long roadId;
	private double latitude;
	private double longitude;
	private double direction;
	private String name;
	private String appModeKey;

	public ABlockedRoad(long roadId, double latitude, double longitude, double direction, String name, String appModeKey) {
		this.roadId = roadId;
		this.latitude = latitude;
		this.longitude = longitude;
		this.direction = direction;
		this.name = name;
		this.appModeKey = appModeKey;
	}

	protected ABlockedRoad(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ABlockedRoad> CREATOR = new Creator<ABlockedRoad>() {
		@Override
		public ABlockedRoad createFromParcel(Parcel in) {
			return new ABlockedRoad(in);
		}

		@Override
		public ABlockedRoad[] newArray(int size) {
			return new ABlockedRoad[size];
		}
	};

	public long getRoadId() {
		return roadId;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getDirection() {
		return direction;
	}

	public String getName() {
		return name;
	}

	public String getAppModeKey() {
		return appModeKey;
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		roadId = bundle.getLong("roadId");
		latitude = bundle.getDouble("latitude");
		longitude = bundle.getDouble("longitude");
		direction = bundle.getDouble("direction");
		name = bundle.getString("name");
		appModeKey = bundle.getString("appModeKey");
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putLong("roadId", roadId);
		bundle.putDouble("latitude", latitude);
		bundle.putDouble("longitude", longitude);
		bundle.putDouble("direction", direction);
		bundle.putString("name", name);
		bundle.putString("appModeKey", appModeKey);
	}
}