package net.osmand.aidl.mapmarker;

import android.os.Parcel;
import android.os.Parcelable;

import net.osmand.aidl.map.ALatLon;

public class AMapMarker implements Parcelable {

	private ALatLon latLon;
	private String name;

	public AMapMarker(ALatLon latLon, String name) {

		if (latLon == null) {
			throw new IllegalArgumentException("latLon cannot be null");
		}

		if (name == null) {
			name = "";
		}

		this.latLon = latLon;
		this.name = name;
	}

	public AMapMarker(Parcel in) {
		readFromParcel(in);
	}

	public static final Parcelable.Creator<AMapMarker> CREATOR = new
			Parcelable.Creator<AMapMarker>() {
				public AMapMarker createFromParcel(Parcel in) {
					return new AMapMarker(in);
				}

				public AMapMarker[] newArray(int size) {
					return new AMapMarker[size];
				}
			};

	public ALatLon getLatLon() {
		return latLon;
	}

	public String getName() {
		return name;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(latLon, flags);
		out.writeString(name);
	}

	private void readFromParcel(Parcel in) {
		latLon = in.readParcelable(ALatLon.class.getClassLoader());
		name = in.readString();
	}

	public int describeContents() {
		return 0;
	}
}
