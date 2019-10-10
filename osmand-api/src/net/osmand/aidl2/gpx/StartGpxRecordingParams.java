package net.osmand.aidl2.gpx;

import android.os.Parcel;

import net.osmand.aidl2.AidlParams;

public class StartGpxRecordingParams extends AidlParams {

	public StartGpxRecordingParams() {

	}

	public StartGpxRecordingParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<StartGpxRecordingParams> CREATOR = new Creator<StartGpxRecordingParams>() {
		@Override
		public StartGpxRecordingParams createFromParcel(Parcel in) {
			return new StartGpxRecordingParams(in);
		}

		@Override
		public StartGpxRecordingParams[] newArray(int size) {
			return new StartGpxRecordingParams[size];
		}
	};
}