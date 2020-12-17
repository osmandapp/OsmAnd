package net.osmand.aidlapi.navigation;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class ABlockedRoadParams extends AidlParams {

	public long roadId;
	public double latitude;
	public double longitude;
	public String name;
	public String appModeKey;

	public ABlockedRoadParams(long roadId, double latitude, double longitude, String name, String appModeKey) {
		this.roadId = roadId;
		this.latitude = latitude;
		this.longitude = longitude;
		this.name = name;
		this.appModeKey = appModeKey;
	}

	protected ABlockedRoadParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ABlockedRoadParams> CREATOR = new Creator<ABlockedRoadParams>() {
		@Override
		public ABlockedRoadParams createFromParcel(Parcel in) {
			return new ABlockedRoadParams(in);
		}

		@Override
		public ABlockedRoadParams[] newArray(int size) {
			return new ABlockedRoadParams[size];
		}
	};

	@Override
	protected void readFromBundle(Bundle bundle) {
		roadId = bundle.getLong("roadId");
		latitude = bundle.getDouble("latitude");
		longitude = bundle.getDouble("longitude");
		name = bundle.getString("name");
		appModeKey = bundle.getString("appModeKey");
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putLong("roadId", roadId);
		bundle.putDouble("latitude", latitude);
		bundle.putDouble("longitude", longitude);
		bundle.putString("name", name);
		bundle.putString("appModeKey", appModeKey);
	}
}