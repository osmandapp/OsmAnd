package net.osmand.aidl2.note;

import android.os.Parcel;

import net.osmand.aidl2.AidlParams;

public class StopRecordingParams extends AidlParams {

	public StopRecordingParams() {

	}

	public StopRecordingParams(Parcel in) {
		super(in);
	}

	public static final Creator<StopRecordingParams> CREATOR = new Creator<StopRecordingParams>() {
		@Override
		public StopRecordingParams createFromParcel(Parcel in) {
			return new StopRecordingParams(in);
		}

		@Override
		public StopRecordingParams[] newArray(int size) {
			return new StopRecordingParams[size];
		}
	};
}