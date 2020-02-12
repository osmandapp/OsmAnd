package net.osmand.aidl.mapmarker;

import android.os.Parcel;
import android.os.Parcelable;

public class RemoveMapMarkersParams implements Parcelable {


	public RemoveMapMarkersParams() {
	}

	public RemoveMapMarkersParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<RemoveMapMarkersParams> CREATOR = new
			Creator<RemoveMapMarkersParams>() {
				public RemoveMapMarkersParams createFromParcel(Parcel in) {
					return new RemoveMapMarkersParams(in);
				}

				public RemoveMapMarkersParams[] newArray(int size) {
					return new RemoveMapMarkersParams[size];
				}
			};


	public void writeToParcel(Parcel out, int flags) {
	}

	private void readFromParcel(Parcel in) {
	}

	public int describeContents() {
		return 0;
	}
}
