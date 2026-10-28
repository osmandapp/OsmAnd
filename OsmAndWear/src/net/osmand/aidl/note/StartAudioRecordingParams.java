package net.osmand.aidl.note;

import android.os.Parcel;
import android.os.Parcelable;

public class StartAudioRecordingParams implements Parcelable {

	private double latitude;
	private double longitude;

	public StartAudioRecordingParams(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public StartAudioRecordingParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<StartAudioRecordingParams> CREATOR = new Creator<StartAudioRecordingParams>() {
		@Override
		public StartAudioRecordingParams createFromParcel(Parcel in) {
			return new StartAudioRecordingParams(in);
		}

		@Override
		public StartAudioRecordingParams[] newArray(int size) {
			return new StartAudioRecordingParams[size];
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
