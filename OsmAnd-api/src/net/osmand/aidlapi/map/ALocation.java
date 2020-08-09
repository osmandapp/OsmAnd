package net.osmand.aidlapi.map;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class ALocation extends AidlParams {

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getAltitude() {
		return altitude;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public double getBearing() {
		return bearing;
	}

	public void setBearing(double bearing) {
		this.bearing = bearing;
	}

	private double latitude;
	private double longitude;
	private double altitude;
	private double speed;
	private double bearing;

	public ALocation(double latitude, double longitude, double altitude, double speed, double bearing) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.speed = speed;
		this.bearing = bearing;
	}

	public ALocation(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ALocation> CREATOR = new
			Creator<ALocation>() {
				public ALocation createFromParcel(Parcel in) {
					return new ALocation(in);
				}

				public ALocation[] newArray(int size) {
					return new ALocation[size];
				}
			};

	public ALocation() {
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		int temp;
		temp = (int)Math.floor(latitude * 10000);
		result = prime * result + temp;
		temp = (int)Math.floor(longitude * 10000);
		result = prime * result + temp;
		temp = (int)Math.floor(altitude * 10000);
		result = prime * result + temp;
		temp = (int)Math.floor(speed * 10000);
		result = prime * result + temp;
		temp = (int)Math.floor(bearing * 10000);
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

		ALocation other = (ALocation) obj;
		return Math.abs(latitude - other.latitude) < 0.00001
				&& Math.abs(longitude - other.longitude) < 0.00001
				&& Math.abs(altitude - other.altitude) < 1
				&& Math.abs(speed - other.speed) < 1;
	}

	@Override
	public String toString() {
		return "Lat " + ((float)latitude) + " Lon " + ((float)longitude) + " Alt " + ((float)altitude);
	}


	@Override
	protected void readFromBundle(Bundle bundle) {
		latitude = bundle.getDouble("latitude", latitude);
		longitude = bundle.getDouble("longitude", longitude);
		altitude = bundle.getDouble("altitude", altitude);
		speed = bundle.getDouble("speed", speed);
		bearing = bundle.getDouble("bearing", bearing);
	}

	@Override
	protected void writeToBundle(Bundle bundle) {
		bundle.putDouble("latitude", latitude);
		bundle.putDouble("longitude", longitude);
		bundle.putDouble("altitude", altitude);
		bundle.putDouble("speed", speed);
		bundle.putDouble("bearing", bearing);
	}
}
