package net.osmand.aidl.search;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class SearchResult implements Parcelable {

	private double latitude;
	private double longitude;

	private String localName;
	private String localTypeName;

	private String alternateName;
	private List<String> otherNames = new ArrayList<>();


	public SearchResult(double latitude, double longitude, String localName, String localTypeName,
						String alternateName, List<String> otherNames) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.localName = localName;
		this.localTypeName = localTypeName;
		this.alternateName = alternateName;
		if (otherNames != null) {
			this.otherNames = otherNames;
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
	public void writeToParcel(Parcel out, int flags) {
		out.writeDouble(latitude);
		out.writeDouble(longitude);
		out.writeString(localName);
		out.writeString(localTypeName);
		out.writeString(alternateName);
		out.writeStringList(otherNames);
	}

	private void readFromParcel(Parcel in) {
		latitude = in.readDouble();
		longitude = in.readDouble();
		localName = in.readString();
		localTypeName = in.readString();
		alternateName = in.readString();
		in.readStringList(otherNames);
	}

	@Override
	public int describeContents() {
		return 0;
	}
}
