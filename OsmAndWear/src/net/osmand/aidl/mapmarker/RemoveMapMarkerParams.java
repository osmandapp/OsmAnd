package net.osmand.aidl.mapmarker;

import android.os.Parcel;
import android.os.Parcelable;

public class RemoveMapMarkerParams implements Parcelable {

	private AMapMarker marker;
	private boolean ignoreCoordinates;

	public RemoveMapMarkerParams(AMapMarker marker) {
		this.marker = marker;
		this.ignoreCoordinates = false;
	}

	public RemoveMapMarkerParams(AMapMarker marker, boolean ignoreCoordinates) {
		this.marker = marker;
		this.ignoreCoordinates = ignoreCoordinates;
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

	public boolean getIgnoreCoordinates() {
		return ignoreCoordinates;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(marker, flags);
		out.writeInt(ignoreCoordinates ? 1 : 0);
	}

	private void readFromParcel(Parcel in) {
		marker = in.readParcelable(AMapMarker.class.getClassLoader());
		ignoreCoordinates = in.readInt() != 0;
	}

	public int describeContents() {
		return 0;
	}
}
