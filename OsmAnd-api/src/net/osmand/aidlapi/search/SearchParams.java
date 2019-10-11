package net.osmand.aidlapi.search;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class SearchParams extends AidlParams {

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
	public void writeToBundle(Bundle bundle) {
		bundle.putString("searchQuery", searchQuery);
		bundle.putInt("searchType", searchType);
		bundle.putDouble("latitude", latitude);
		bundle.putDouble("longitude", longitude);
		bundle.putInt("radiusLevel", radiusLevel);
		bundle.putInt("totalLimit", totalLimit);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		searchQuery = bundle.getString("searchQuery");
		searchType = bundle.getInt("searchType");
		latitude = bundle.getDouble("latitude");
		longitude = bundle.getDouble("longitude");
		radiusLevel = bundle.getInt("radiusLevel");
		totalLimit = bundle.getInt("totalLimit");
	}
}