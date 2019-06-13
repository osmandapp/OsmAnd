package net.osmand.aidl.gpx;

import android.os.Parcel;
import android.os.Parcelable;

public class StartGpxRecordingParams implements Parcelable {

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

	@Override
	public void writeToParcel(Parcel out, int flags) {
	}

	private void readFromParcel(Parcel in) {

	}

	@Override
	public int describeContents() {
		return 0;
	}
}
