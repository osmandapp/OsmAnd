package net.osmand.aidlapi.info;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;
import net.osmand.aidlapi.map.ALatLon;

import java.util.ArrayList;
import java.util.List;

public class AppInfoParams extends AidlParams {

	private ALatLon lastKnownLocation;
	private ALatLon mapLocation;
	private ALatLon destinationLocation;

	private Bundle turnInfo;
	private Bundle versionsInfo;
	private int leftTime;
	private int leftDistance;
	private long arrivalTime;
	private boolean mapVisible;

	private String osmAndVersion;
	private String releaseDate;
	private ArrayList<String> routingData = new ArrayList<>();

	public AppInfoParams(ALatLon lastKnownLocation, ALatLon mapLocation, Bundle turnInfo,
	                     int leftTime, int leftDistance, long arrivalTime, boolean mapVisible) {
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

	public Bundle getVersionsInfo() {
		return versionsInfo;
	}

	public void setVersionsInfo(Bundle versionsInfo) {
		this.versionsInfo = versionsInfo;
	}

	public String getOsmAndVersion() {
		return osmAndVersion;
	}

	public void setOsmAndVersion(String osmAndVersion) {
		this.osmAndVersion = osmAndVersion;
	}

	public String getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}

	public void setRoutingData(List<String> routingData) {
		if (routingData != null) {
			this.routingData.addAll(routingData);
		}
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
		bundle.putBundle("versionsInfo", versionsInfo);
		bundle.putBoolean("mapVisible", mapVisible);
		bundle.putString("osmAndVersion", osmAndVersion);
		bundle.putString("releaseDate", releaseDate);
		bundle.putStringArrayList("routingData", routingData);
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
		versionsInfo = bundle.getBundle("versionsInfo");
		mapVisible = bundle.getBoolean("mapVisible");
		osmAndVersion = bundle.getString("osmAndVersion");
		releaseDate = bundle.getString("releaseDate");
		routingData = bundle.getStringArrayList("routingData");
	}
}