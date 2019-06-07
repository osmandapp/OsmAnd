package net.osmand.aidl.note;

import android.os.Parcel;
import android.os.Parcelable;

public class StartVideoRecordingParams implements Parcelable {

	private double latitude;
	private double longitude;

	public StartVideoRecordingParams(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public StartVideoRecordingParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<StartVideoRecordingParams> CREATOR = new Creator<StartVideoRecordingParams>() {
		@Override
		public StartVideoRecordingParams createFromParcel(Parcel in) {
			return new StartVideoRecordingParams(in);
		}

		@Override
		public StartVideoRecordingParams[] newArray(int size) {
			return new StartVideoRecordingParams[size];
		}
	};

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeDouble(latitude);
		out.writeDouble(longitude);
	}

	private void readFromParcel(Parcel in) {
		latitude = in.readDouble();
		longitude = in.readDouble();
	}

	@Override
	public int describeContents() {
		return 0;
	}

}
