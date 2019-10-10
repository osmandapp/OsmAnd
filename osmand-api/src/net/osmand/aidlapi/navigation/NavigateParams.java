package net.osmand.aidlapi.navigation;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class NavigateParams extends AidlParams {

	private String profile;
	private String destName;
	private String startName;

	private double startLat;
	private double startLon;
	private double destLat;
	private double destLon;

	private boolean force;

	public NavigateParams(String startName, double startLat, double startLon, String destName, double destLat, double destLon, String profile, boolean force) {
		this.startName = startName;
		this.startLat = startLat;
		this.startLon = startLon;
		this.destName = destName;
		this.destLat = destLat;
		this.destLon = destLon;
		this.profile = profile;
		this.force = force;
	}

	public NavigateParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<NavigateParams> CREATOR = new Creator<NavigateParams>() {
		@Override
		public NavigateParams createFromParcel(Parcel in) {
			return new NavigateParams(in);
		}

		@Override
		public NavigateParams[] newArray(int size) {
			return new NavigateParams[size];
		}
	};

	public String getStartName() {
		return startName;
	}

	public double getStartLat() {
		return startLat;
	}

	public double getStartLon() {
		return startLon;
	}

	public String getDestName() {
		return destName;
	}

	public double getDestLat() {
		return destLat;
	}

	public double getDestLon() {
		return destLon;
	}

	public String getProfile() {
		return profile;
	}

	public boolean isForce() {
		return force;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("startName", startName);
		bundle.putDouble("startLat", startLat);
		bundle.putDouble("startLon", startLon);
		bundle.putString("destName", destName);
		bundle.putDouble("destLat", destLat);
		bundle.putDouble("destLon", destLon);
		bundle.putString("profile", profile);
		bundle.putBoolean("force", force);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		startName = bundle.getString("startName");
		startLat = bundle.getDouble("startLat");
		startLon = bundle.getDouble("startLon");
		destName = bundle.getString("destName");
		destLat = bundle.getDouble("destLat");
		destLon = bundle.getDouble("destLon");
		profile = bundle.getString("profile");
		force = bundle.getBoolean("force");
	}
}