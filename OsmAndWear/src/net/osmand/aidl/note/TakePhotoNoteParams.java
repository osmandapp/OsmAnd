package net.osmand.aidl.note;

import android.os.Parcel;
import android.os.Parcelable;

public class TakePhotoNoteParams implements Parcelable {

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
