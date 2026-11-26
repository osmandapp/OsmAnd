package net.osmand.aidl.mapmarker;

import android.os.Parcel;
import android.os.Parcelable;

public class AddMapMarkerParams implements Parcelable {

	private AMapMarker marker;

	public AddMapMarkerParams(AMapMarker marker) {
		this.marker = marker;
	}

	public AddMapMarkerParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Parcelable.Creator<AddMapMarkerParams> CREATOR = new
			Parcelable.Creator<AddMapMarkerParams>() {
				public AddMapMarkerParams createFromParcel(Parcel in) {
					return new AddMapMarkerParams(in);
				}

				public AddMapMarkerParams[] newArray(int size) {
					return new AddMapMarkerParams[size];
				}
			};

	public AMapMarker getMarker() {
		return marker;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(marker, flags);
	}

	private void readFromParcel(Parcel in) {
		marker = in.readParcelable(AMapMarker.class.getClassLoader());
	}

	public int describeContents() {
		return 0;
	}
}
