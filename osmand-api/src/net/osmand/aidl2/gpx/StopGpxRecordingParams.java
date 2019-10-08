package net.osmand.aidl2.gpx;

import android.os.Parcel;

import net.osmand.aidl2.AidlParams;

public class StopGpxRecordingParams extends AidlParams {

	public StopGpxRecordingParams() {

	}

	public StopGpxRecordingParams(Parcel in) {
		super(in);
	}

	public static final Creator<StopGpxRecordingParams> CREATOR = new Creator<StopGpxRecordingParams>() {
		@Override
		public StopGpxRecordingParams createFromParcel(Parcel in) {
			return new StopGpxRecordingParams(in);
		}

		@Override
		public StopGpxRecordingParams[] newArray(int size) {
			return new StopGpxRecordingParams[size];
		}
	};
}