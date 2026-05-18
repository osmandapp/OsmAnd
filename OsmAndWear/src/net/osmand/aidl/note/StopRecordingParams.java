package net.osmand.aidl.note;

import android.os.Parcel;
import android.os.Parcelable;

public class StopRecordingParams implements Parcelable {

	public StopRecordingParams() {

	}

	public StopRecordingParams(Parcel in) {
		readFromParcel(in);
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