package net.osmand.aidl2.mapmarker;

import android.os.Parcel;

import net.osmand.aidl2.AidlParams;

public class RemoveMapMarkersParams extends AidlParams {

	public RemoveMapMarkersParams() {

	}

	public RemoveMapMarkersParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<RemoveMapMarkersParams> CREATOR = new Creator<RemoveMapMarkersParams>() {
		@Override
		public RemoveMapMarkersParams createFromParcel(Parcel in) {
			return new RemoveMapMarkersParams(in);
		}

		@Override
		public RemoveMapMarkersParams[] newArray(int size) {
			return new RemoveMapMarkersParams[size];
		}
	};
}