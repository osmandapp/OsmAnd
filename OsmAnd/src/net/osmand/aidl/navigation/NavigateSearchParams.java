package net.osmand.aidl.navigation;

import android.os.Parcel;
import android.os.Parcelable;

public class NavigateSearchParams implements Parcelable {

	private String startName;
	private double startLat;
	private double startLon;
	private String searchQuery;
	private String profile;
	private boolean force;

	public NavigateSearchParams(String startName, double startLat, double startLon, String searchQuery, String profile, boolean force) {
		this.startName = startName;
		this.startLat = startLat;
		this.startLon = startLon;
		this.searchQuery = searchQuery;
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

	public String getProfile() {
		return profile;
	}

	public boolean isForce() {
		return force;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(startName);
		out.writeDouble(startLat);
		out.writeDouble(startLon);
		out.writeString(searchQuery);
		out.writeString(profile);
		out.writeByte((byte) (force ? 1 : 0));
	}

	private void readFromParcel(Parcel in) {
		startName = in.readString();
		startLat = in.readDouble();
		startLon = in.readDouble();
		searchQuery = in.readString();
		profile = in.readString();
		force = in.readByte() != 0;
	}

	@Override
	public int describeContents() {
		return 0;
	}

}
