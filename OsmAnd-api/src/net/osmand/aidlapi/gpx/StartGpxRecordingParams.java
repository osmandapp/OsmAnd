package net.osmand.aidlapi.gpx;

import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

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