package net.osmand.aidlapi.map;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class SetMapLocationParams extends AidlParams {

	private double latitude;
	private double longitude;
	private int zoom;
	private float rotation;
	private boolean animated;

	public SetMapLocationParams(double latitude, double longitude, int zoom, float rotation, boolean animated) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.zoom = zoom;
		this.rotation = rotation;
		this.animated = animated;
	}

	public SetMapLocationParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<SetMapLocationParams> CREATOR = new Creator<SetMapLocationParams>() {
		@Override
		public SetMapLocationParams createFromParcel(Parcel in) {
			return new SetMapLocationParams(in);
		}

		@Override
		public SetMapLocationParams[] newArray(int size) {
			return new SetMapLocationParams[size];
		}
	};

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public int getZoom() {
		return zoom;
	}

	public float getRotation() {
		return rotation;
	}
	
	public boolean isAnimated() {
		return animated;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putDouble("latitude", latitude);
		bundle.putDouble("longitude", longitude);
		bundle.putInt("zoom", zoom);
		bundle.putFloat("rotation", rotation);
		bundle.putBoolean("animated", animated);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		latitude = bundle.getDouble("latitude");
		longitude = bundle.getDouble("longitude");
		zoom = bundle.getInt("zoom");
		rotation = bundle.getFloat("rotation");
		animated = bundle.getBoolean("animated");
	}
}