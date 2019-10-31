package net.osmand.aidlapi.search;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;
import java.util.List;

public class SearchResult extends AidlParams {

	private double latitude;
	private double longitude;

	private String localName;
	private String localTypeName;

	private String alternateName;
	private ArrayList<String> otherNames = new ArrayList<>();


	public SearchResult(double latitude, double longitude, String localName, String localTypeName,
	                    String alternateName, List<String> otherNames) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.localName = localName;
		this.localTypeName = localTypeName;
		this.alternateName = alternateName;
		if (otherNames != null) {
			this.otherNames.addAll(otherNames);
		}
	}

	public SearchResult(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<SearchResult> CREATOR = new Creator<SearchResult>() {
		@Override
		public SearchResult createFromParcel(Parcel in) {
			return new SearchResult(in);
		}

		@Override
		public SearchResult[] newArray(int size) {
			return new SearchResult[size];
		}
	};

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getLocalName() {
		return localName;
	}

	public String getLocalTypeName() {
		return localTypeName;
	}

	public String getAlternateName() {
		return alternateName;
	}

	public List<String> getOtherNames() {
		return otherNames;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putDouble("latitude", latitude);
		bundle.putDouble("longitude", longitude);
		bundle.putString("localName", localName);
		bundle.putString("localTypeName", localTypeName);
		bundle.putString("alternateName", alternateName);
		bundle.putStringArrayList("otherNames", otherNames);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		latitude = bundle.getDouble("latitude");
		longitude = bundle.getDouble("longitude");
		localName = bundle.getString("localName");
		localTypeName = bundle.getString("localTypeName");
		alternateName = bundle.getString("alternateName");
		otherNames = bundle.getStringArrayList("otherName");
	}
}