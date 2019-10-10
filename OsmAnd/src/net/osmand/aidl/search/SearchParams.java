package net.osmand.aidl.search;

import android.os.Parcel;
import android.os.Parcelable;

public class SearchParams implements Parcelable {

	public static final int SEARCH_TYPE_POI = 1;
	public static final int SEARCH_TYPE_ADDRESS = 2;
	public static final int SEARCH_TYPE_ALL = SEARCH_TYPE_POI | SEARCH_TYPE_ADDRESS;

	private String searchQuery;
	private int searchType;
	private double latitude;
	private double longitude;
	private int radiusLevel = 1;
	private int totalLimit = -1;

	public SearchParams(String searchQuery, int searchType, double latitude, double longitude, int radiusLevel, int totalLimit) {
		this.searchQuery = searchQuery;
		this.searchType = searchType;
		this.latitude = latitude;
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

	public int getSearchType() {
		return searchType;
	}

	public double getLatitude() {
		return latitude;
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
		out.writeInt(searchType);
		out.writeDouble(latitude);
		out.writeDouble(longitude);
		out.writeInt(radiusLevel);
		out.writeInt(totalLimit);
	}

	private void readFromParcel(Parcel in) {
		searchQuery = in.readString();
		searchType = in.readInt();
		latitude = in.readDouble();
		longitude = in.readDouble();
		radiusLevel = in.readInt();
		totalLimit = in.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}
}
