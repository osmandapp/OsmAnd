package net.osmand.aidl.search;

import android.os.Parcel;
import android.os.Parcelable;

public class SearchParams implements Parcelable {

	private String searchQuery;
	private double latutude;
	private double longitude;
	private int radiusLevel = 1;
	private int totalLimit = -1;

	public SearchParams(String searchQuery, double latutude, double longitude, int radiusLevel, int totalLimit) {
		this.searchQuery = searchQuery;
		this.latutude = latutude;
		this.longitude = longitude;
		this.radiusLevel = radiusLevel;
		this.totalLimit = totalLimit;
	}

	public SearchParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<SearchParams> CREATOR = new Creator<SearchParams>() {
		@Override
		public SearchParams createFromParcel(Parcel in) {
			return new SearchParams(in);
		}

		@Override
		public SearchParams[] newArray(int size) {
			return new SearchParams[size];
		}
	};

	public String getSearchQuery() {
		return searchQuery;
	}

	public double getLatutude() {
		return latutude;
	}

	public double getLongitude() {
		return longitude;
	}

	public int getRadiusLevel() {
		return radiusLevel;
	}

	public int getTotalLimit() {
		return totalLimit;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(searchQuery);
		out.writeDouble(latutude);
		out.writeDouble(longitude);
		out.writeInt(radiusLevel);
		out.writeInt(totalLimit);
	}

	private void readFromParcel(Parcel in) {
		searchQuery = in.readString();
		latutude = in.readDouble();
		longitude = in.readDouble();
		radiusLevel = in.readInt();
		totalLimit = in.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}
}
