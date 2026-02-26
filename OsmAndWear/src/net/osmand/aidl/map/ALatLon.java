package net.osmand.aidl.map;

import android.os.Parcel;
import android.os.Parcelable;

public class ALatLon implements Parcelable {

	private double longitude;
	private double latitude;

	public ALatLon(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public ALatLon(Parcel in) {
		readFromParcel(in);
	}

	public static final Parcelable.Creator<ALatLon> CREATOR = new
			Parcelable.Creator<ALatLon>() {
				public ALatLon createFromParcel(Parcel in) {
					return new ALatLon(in);
				}

				public ALatLon[] newArray(int size) {
					return new ALatLon[size];
				}
			};

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		int temp;
		temp = (int)Math.floor(latitude * 10000);
		result = prime * result + temp;
		temp = (int)Math.floor(longitude * 10000);
		result = prime * result + temp;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		ALatLon other = (ALatLon) obj;
		return Math.abs(latitude - other.latitude) < 0.00001
				&& Math.abs(longitude - other.longitude) < 0.00001;
	}

	@Override
	public String toString() {
		return "Lat " + ((float)latitude) + " Lon " + ((float)longitude);
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeDouble(latitude);
		out.writeDouble(longitude);
	}

	public void readFromParcel(Parcel in) {
		latitude = in.readDouble();
		longitude = in.readDouble();
	}

	public int describeContents() {
		return 0;
	}
}
