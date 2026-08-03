package net.osmand.aidl.gpx;

import android.os.Parcel;
import android.os.Parcelable;

public class StopGpxRecordingParams implements Parcelable {

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
