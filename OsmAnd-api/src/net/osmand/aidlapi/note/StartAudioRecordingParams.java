package net.osmand.aidlapi.note;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class StartAudioRecordingParams extends AidlParams {

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