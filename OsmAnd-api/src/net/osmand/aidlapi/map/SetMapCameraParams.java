package net.osmand.aidlapi.map;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

/**
 * Moves the map camera. NaN keeps the current value.
 */
public class SetMapCameraParams extends AidlParams {

	private double latitude = Double.NaN;
	private double longitude = Double.NaN;
	private float zoom = Float.NaN;
	private float rotation = Float.NaN;
	private float elevationAngle = Float.NaN;
	private boolean animated;

	public SetMapCameraParams(double latitude, double longitude, float zoom, float rotation, float elevationAngle, boolean animated) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.zoom = zoom;
		this.rotation = rotation;
		this.elevationAngle = elevationAngle;
		this.animated = animated;
	}

	public SetMapCameraParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<SetMapCameraParams> CREATOR = new Creator<SetMapCameraParams>() {
		@Override
		public SetMapCameraParams createFromParcel(Parcel in) {
			return new SetMapCameraParams(in);
		}

		@Override
		public SetMapCameraParams[] newArray(int size) {
			return new SetMapCameraParams[size];
		}
	};

	/**
	 * @return latitude of the map center, NaN keeps it
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * @return longitude of the map center, NaN keeps it
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * @return zoom with a fractional part, NaN keeps it
	 */
	public float getZoom() {
		return zoom;
	}

	/**
	 * @return map rotation in degrees, 0 is north up, NaN keeps it
	 */
	public float getRotation() {
		return rotation;
	}

	/**
	 * @return camera elevation angle in degrees, 90 is a flat map, smaller values tilt it (3D), NaN keeps it
	 */
	public float getElevationAngle() {
		return elevationAngle;
	}

	/**
	 * @return true to animate the move
	 */
	public boolean isAnimated() {
		return animated;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putDouble("latitude", latitude);
		bundle.putDouble("longitude", longitude);
		bundle.putFloat("zoom", zoom);
		bundle.putFloat("rotation", rotation);
		bundle.putFloat("elevationAngle", elevationAngle);
		bundle.putBoolean("animated", animated);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		latitude = bundle.getDouble("latitude", Double.NaN);
		longitude = bundle.getDouble("longitude", Double.NaN);
		zoom = bundle.getFloat("zoom", Float.NaN);
		rotation = bundle.getFloat("rotation", Float.NaN);
		elevationAngle = bundle.getFloat("elevationAngle", Float.NaN);
		animated = bundle.getBoolean("animated");
	}
}
