package net.osmand.aidl2.gpx;

import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;

import net.osmand.aidl2.AidlParams;

public class GpxColorParams extends AidlParams {

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
		super(in);
	}

	public static final Creator<GpxColorParams> CREATOR = new Creator<GpxColorParams>() {
		@Override
		public GpxColorParams createFromParcel(Parcel in) {
			return new GpxColorParams(in);
		}

		@Override
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

	@Override
	protected void readFromBundle(Bundle bundle) {
		fileName = bundle.getString("fileName");
		gpxColor = bundle.getString("gpxColor");
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("fileName", fileName);
		bundle.putString("gpxColor", gpxColor);
	}
}