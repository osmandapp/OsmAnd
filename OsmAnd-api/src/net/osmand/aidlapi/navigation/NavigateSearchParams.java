package net.osmand.aidlapi.navigation;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class NavigateSearchParams extends AidlParams {

	private String profile;
	private String startName;
	private String searchQuery;

	private double startLat;
	private double startLon;
	private double searchLat;
	private double searchLon;

	private boolean force;

	public NavigateSearchParams(String startName, double startLat, double startLon,
	                            String searchQuery, double searchLat, double searchLon,
	                            String profile, boolean force) {
		this.startName = startName;
		this.startLat = startLat;
		this.startLon = startLon;
		this.searchQuery = searchQuery;
		this.searchLat = searchLat;
		this.searchLon = searchLon;
		this.profile = profile;
		this.force = force;
	}

	public NavigateSearchParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<NavigateSearchParams> CREATOR = new Creator<NavigateSearchParams>() {
		@Override
		public NavigateSearchParams createFromParcel(Parcel in) {
			return new NavigateSearchParams(in);
		}

		@Override
		public NavigateSearchParams[] newArray(int size) {
			return new NavigateSearchParams[size];
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

	public String getSearchQuery() {
		return searchQuery;
	}

	public double getSearchLat() {
		return searchLat;
	}

	public double getSearchLon() {
		return searchLon;
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
		bundle.putString("searchQuery", searchQuery);
		bundle.putString("profile", profile);
		bundle.putBoolean("force", force);
		bundle.putDouble("searchLat", searchLat);
		bundle.putDouble("searchLon", searchLon);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		startName = bundle.getString("startName");
		startLat = bundle.getDouble("startLat");
		startLon = bundle.getDouble("startLon");
		searchQuery = bundle.getString("searchQuery");
		profile = bundle.getString("profile");
		force = bundle.getBoolean("force");
		searchLat = bundle.getDouble("searchLat");
		searchLon = bundle.getDouble("searchLon");
	}
}