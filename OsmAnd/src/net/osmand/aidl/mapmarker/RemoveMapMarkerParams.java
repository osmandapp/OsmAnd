package net.osmand.aidl.mapmarker;

import android.os.Parcel;
import android.os.Parcelable;

public class RemoveMapMarkerParams implements Parcelable {

	private AMapMarker marker;

	public RemoveMapMarkerParams(AMapMarker marker) {
		this.marker = marker;
	}

	public RemoveMapMarkerParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Parcelable.Creator<RemoveMapMarkerParams> CREATOR = new
			Parcelable.Creator<RemoveMapMarkerParams>() {
				public RemoveMapMarkerParams createFromParcel(Parcel in) {
					return new RemoveMapMarkerParams(in);
				}

				public RemoveMapMarkerParams[] newArray(int size) {
					return new RemoveMapMarkerParams[size];
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
