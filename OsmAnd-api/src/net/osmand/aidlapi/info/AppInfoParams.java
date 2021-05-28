package net.osmand.aidlapi.info;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;
import net.osmand.aidlapi.map.ALatLon;

public class AppInfoParams extends AidlParams {

	private ALatLon lastKnownLocation;
	private ALatLon mapLocation;
	private ALatLon destinationLocation;

	private Bundle turnInfo;
	private int leftTime;
	private int leftDistance;
	private long arrivalTime;
	private boolean mapVisible;

	public AppInfoParams(ALatLon lastKnownLocation, ALatLon mapLocation, Bundle turnInfo, int leftTime, int leftDistance, long arrivalTime, boolean mapVisible) {
		this.lastKnownLocation = lastKnownLocation;
		this.mapLocation = mapLocation;
		this.leftTime = leftTime;
		this.leftDistance = leftDistance;
		this.arrivalTime = arrivalTime;
		this.turnInfo = turnInfo;
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

	public ALatLon getDestinationLocation() {
		return destinationLocation;
	}

	public void setDestinationLocation(ALatLon destinationLocation) {
		this.destinationLocation = destinationLocation;
	}

	public int getLeftTime() {
		return leftTime;
	}

	public long getArrivalTime() {
		return arrivalTime;
	}

	public int getLeftDistance() {
		return leftDistance;
	}

	public boolean isMapVisible() {
		return mapVisible;
	}

	public Bundle getTurnInfo() {
		return turnInfo;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("lastKnownLocation", lastKnownLocation);
		bundle.putParcelable("mapLocation", mapLocation);
		bundle.putParcelable("destinationLocation", destinationLocation);
		bundle.putInt("leftTime", leftTime);
		bundle.putLong("arrivalTime", arrivalTime);
		bundle.putInt("leftDistance", leftDistance);
		bundle.putBundle("turnInfo", turnInfo);
		bundle.putBoolean("mapVisible", mapVisible);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		lastKnownLocation = bundle.getParcelable("lastKnownLocation");
		mapLocation = bundle.getParcelable("mapLocation");
		destinationLocation = bundle.getParcelable("destinationLocation");
		leftTime = bundle.getInt("leftTime");
		arrivalTime = bundle.getLong("arrivalTime");
		leftDistance = bundle.getInt("leftDistance");
		turnInfo = bundle.getBundle("turnInfo");
		mapVisible = bundle.getBoolean("mapVisible");
	}
}