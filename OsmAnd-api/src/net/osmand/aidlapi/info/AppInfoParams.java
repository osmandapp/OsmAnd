package net.osmand.aidlapi.info;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;
import net.osmand.aidlapi.map.ALatLon;

public class AppInfoParams extends AidlParams {

	private ALatLon lastKnownLocation;
	private ALatLon mapLocation;
	private int time;
	private long eta;
	private int leftDistance;
	private boolean mapVisible;

	public AppInfoParams(ALatLon lastKnownLocation, ALatLon mapLocation, int time, long eta, int leftDistance, boolean mapVisible) {
		this.lastKnownLocation = lastKnownLocation;
		this.mapLocation = mapLocation;
		this.time = time;
		this.eta = eta;
		this.leftDistance = leftDistance;
		this.mapVisible = mapVisible;
	}

	public AppInfoParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AppInfoParams> CREATOR = new Creator<AppInfoParams>() {
		@Override
		public AppInfoParams createFromParcel(Parcel in) {
			return new AppInfoParams(in);
		}

		@Override
		public AppInfoParams[] newArray(int size) {
			return new AppInfoParams[size];
		}
	};

	public ALatLon getLastKnownLocation() {
		return lastKnownLocation;
	}

	public ALatLon getMapLocation() {
		return mapLocation;
	}

	public int getTime() {
		return time;
	}

	public long getEta() {
		return eta;
	}

	public int getLeftDistance() {
		return leftDistance;
	}

	public boolean isMapVisible() {
		return mapVisible;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("location", lastKnownLocation);
		bundle.putParcelable("mapLocation", mapLocation);
		bundle.putInt("time", time);
		bundle.putLong("eta", eta);
		bundle.putInt("leftDistance", leftDistance);
		bundle.putBoolean("mapVisible", mapVisible);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		lastKnownLocation = bundle.getParcelable("location");
		mapLocation = bundle.getParcelable("mapLocation");
		time = bundle.getInt("time");
		eta = bundle.getLong("eta");
		leftDistance = bundle.getInt("leftDistance");
		mapVisible = bundle.getBoolean("mapVisible");
	}
}