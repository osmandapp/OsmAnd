package net.osmand.aidl.mapmarker;

import android.os.Parcel;
import android.os.Parcelable;

public class UpdateMapMarkerParams implements Parcelable {

	private AMapMarker markerPrev;
	private AMapMarker markerNew;
	private boolean ignoreCoordinates;

	public UpdateMapMarkerParams(AMapMarker markerPrev, AMapMarker markerNew) {
		this.markerPrev = markerPrev;
		this.markerNew = markerNew;
		this.ignoreCoordinates = false;
	}

	public UpdateMapMarkerParams(AMapMarker markerPrev, AMapMarker markerNew, boolean ignoreCoordinates) {
		this.markerPrev = markerPrev;
		this.markerNew = markerNew;
		this.ignoreCoordinates = ignoreCoordinates;
	}

	public UpdateMapMarkerParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Parcelable.Creator<UpdateMapMarkerParams> CREATOR = new
			Parcelable.Creator<UpdateMapMarkerParams>() {
				public UpdateMapMarkerParams createFromParcel(Parcel in) {
					return new UpdateMapMarkerParams(in);
				}

				public UpdateMapMarkerParams[] newArray(int size) {
					return new UpdateMapMarkerParams[size];
				}
			};

	public AMapMarker getMarkerPrev() {
		return markerPrev;
	}

	public AMapMarker getMarkerNew() {
		return markerNew;
	}

	public boolean getIgnoreCoordinates() {
		return ignoreCoordinates;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(markerPrev, flags);
		out.writeParcelable(markerNew, flags);
		out.writeInt(ignoreCoordinates ? 1 : 0);
	}

	private void readFromParcel(Parcel in) {
		markerPrev = in.readParcelable(AMapMarker.class.getClassLoader());
		markerNew = in.readParcelable(AMapMarker.class.getClassLoader());
		ignoreCoordinates = in.readInt() != 0;
	}

	public int describeContents() {
		return 0;
	}
}
