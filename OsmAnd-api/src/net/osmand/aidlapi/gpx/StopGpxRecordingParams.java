package net.osmand.aidlapi.gpx;

import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class StopGpxRecordingParams extends AidlParams {

	public StopGpxRecordingParams() {

	}

	public StopGpxRecordingParams(Parcel in) {
		readFromParcel(in);
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