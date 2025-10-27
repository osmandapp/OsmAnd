package net.osmand.aidl.map;

import android.os.Parcel;
import android.os.Parcelable;

public class SetMapLocationParams implements Parcelable {

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

	public static final Creator<SetMapLocationParams> CREATOR = new
			Creator<SetMapLocationParams>() {
				public SetMapLocationParams createFromParcel(Parcel in) {
					return new SetMapLocationParams(in);
				}

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

	public void writeToParcel(Parcel out, int flags) {
		out.writeDouble(latitude);
		out.writeDouble(longitude);
		out.writeInt(zoom);
		out.writeByte((byte) (animated ? 1 : 0));
		out.writeFloat(rotation);
	}

	private void readFromParcel(Parcel in) {
		latitude = in.readDouble();
		longitude = in.readDouble();
		zoom = in.readInt();
		animated = in.readByte() != 0;
		rotation = in.readFloat();
	}

	public int describeContents() {
		return 0;
	}
}
