package net.osmand.aidlapi.gpx;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

/**
 * Track point.
 */
public class AGpxPoint extends AidlParams {

	private double latitude;
	private double longitude;
	private double elevation;
	private long time;
	private float speed;

	public AGpxPoint(double latitude, double longitude, double elevation, long time, float speed) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.elevation = elevation;
		this.time = time;
		this.speed = speed;
	}

	public AGpxPoint(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AGpxPoint> CREATOR = new Creator<AGpxPoint>() {
		@Override
		public AGpxPoint createFromParcel(Parcel in) {
			return new AGpxPoint(in);
		}

		@Override
		public AGpxPoint[] newArray(int size) {
			return new AGpxPoint[size];
		}
	};

	/**
	 * @return latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * @return longitude
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * @return elevation in meters, NaN if unknown
	 */
	public double getElevation() {
		return elevation;
	}

	/**
	 * @return time in milliseconds, 0 if unknown
	 */
	public long getTime() {
		return time;
	}

	/**
	 * @return speed in m/s, 0 if unknown
	 */
	public float getSpeed() {
		return speed;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putDouble("latitude", latitude);
		bundle.putDouble("longitude", longitude);
		bundle.putDouble("elevation", elevation);
		bundle.putLong("time", time);
		bundle.putFloat("speed", speed);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		latitude = bundle.getDouble("latitude");
		longitude = bundle.getDouble("longitude");
		elevation = bundle.getDouble("elevation");
		time = bundle.getLong("time");
		speed = bundle.getFloat("speed");
	}
}
