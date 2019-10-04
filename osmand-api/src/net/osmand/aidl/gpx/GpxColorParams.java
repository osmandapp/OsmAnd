package net.osmand.aidl.gpx;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class GpxColorParams implements Parcelable {

	private String fileName;
	private String gpxColor;

	public GpxColorParams() {

	}

	public GpxColorParams(@NonNull String fileName) {
		this.fileName = fileName;
	}

	public GpxColorParams(@NonNull String fileName, String gpxColor) {
		this.fileName = fileName;
		this.gpxColor = gpxColor;
	}

	public GpxColorParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<GpxColorParams> CREATOR = new
			Creator<GpxColorParams>() {
				public GpxColorParams createFromParcel(Parcel in) {
					return new GpxColorParams(in);
				}

				public GpxColorParams[] newArray(int size) {
					return new GpxColorParams[size];
				}
			};

	public String getFileName() {
		return fileName;
	}

	public String getGpxColor() {
		return gpxColor;
	}

	public void setGpxColor(String gpxColor) {
		this.gpxColor = gpxColor;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(fileName);
		out.writeString(gpxColor);
	}

	public void readFromParcel(Parcel in) {
		fileName = in.readString();
		gpxColor = in.readString();
	}

	public int describeContents() {
		return 0;
	}
}

