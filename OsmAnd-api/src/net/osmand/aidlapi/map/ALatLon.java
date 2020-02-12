package net.osmand.aidlapi.map;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class ALatLon extends AidlParams {

	private double longitude;
	private double latitude;

	public ALatLon(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public ALatLon(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ALatLon> CREATOR = new Creator<ALatLon>() {
		@Override
		public ALatLon createFromParcel(Parcel in) {
			return new ALatLon(in);
		}

		@Override
		public ALatLon[] newArray(int size) {
			return new ALatLon[size];
		}
	};

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		int temp;
		temp = (int) Math.floor(latitude * 10000);
		result = prime * result + temp;
		temp = (int) Math.floor(longitude * 10000);
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
		return "Lat " + ((float) latitude) + " Lon " + ((float) longitude);
	}

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