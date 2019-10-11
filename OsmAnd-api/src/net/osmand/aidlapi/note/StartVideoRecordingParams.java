package net.osmand.aidlapi.note;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class StartVideoRecordingParams extends AidlParams {

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
	public void writeToBundle(Bundle bundle) {
		bundle.putDouble("latitude", latitude);
		bundle.putDouble("longitude", longitude);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		latitude = bundle.getDouble("latitude");
		longitude = bundle.getDouble("longitude");
	}
}