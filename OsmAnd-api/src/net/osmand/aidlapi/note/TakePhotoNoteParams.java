package net.osmand.aidlapi.note;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class TakePhotoNoteParams extends AidlParams {

	private double latitude;
	private double longitude;

	public TakePhotoNoteParams(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public TakePhotoNoteParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<TakePhotoNoteParams> CREATOR = new Creator<TakePhotoNoteParams>() {
		@Override
		public TakePhotoNoteParams createFromParcel(Parcel in) {
			return new TakePhotoNoteParams(in);
		}

		@Override
		public TakePhotoNoteParams[] newArray(int size) {
			return new TakePhotoNoteParams[size];
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