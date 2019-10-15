package net.osmand.aidlapi.gpx;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.io.File;

public class CreateGpxBitmapParams extends AidlParams {

	private File gpxFile;
	private Uri gpxUri;
	private float density;
	private int widthPixels;
	private int heightPixels;
	private int color; //ARGB color int

	public CreateGpxBitmapParams(File gpxFile, float density, int widthPixels, int heightPixels, int color) {
		this.gpxFile = gpxFile;
		this.density = density;
		this.widthPixels = widthPixels;
		this.heightPixels = heightPixels;
		this.color = color;
	}

	public CreateGpxBitmapParams(Uri gpxUri, float density, int widthPixels, int heightPixels, int color) {
		this.gpxUri = gpxUri;
		this.density = density;
		this.widthPixels = widthPixels;
		this.heightPixels = heightPixels;
		this.color = color;
	}

	public CreateGpxBitmapParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<CreateGpxBitmapParams> CREATOR = new Creator<CreateGpxBitmapParams>() {
		@Override
		public CreateGpxBitmapParams createFromParcel(Parcel in) {
			return new CreateGpxBitmapParams(in);
		}

		@Override
		public CreateGpxBitmapParams[] newArray(int size) {
			return new CreateGpxBitmapParams[size];
		}
	};

	public File getGpxFile() {
		return gpxFile;
	}

	public Uri getGpxUri() {
		return gpxUri;
	}

	public int getWidthPixels() {
		return widthPixels;
	}

	public int getHeightPixels() {
		return heightPixels;
	}

	public float getDensity() {
		return density;
	}

	public int getColor() {
		return color;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("gpxAbsolutePath", gpxFile != null ? gpxFile.getAbsolutePath() : null);
		bundle.putParcelable("gpxUri", gpxUri);
		bundle.putFloat("density", density);
		bundle.putInt("widthPixels", widthPixels);
		bundle.putInt("heightPixels", heightPixels);
		bundle.putInt("color", color);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(Uri.class.getClassLoader());

		String gpxAbsolutePath = bundle.getString("gpxAbsolutePath");
		if (gpxAbsolutePath != null) {
			gpxFile = new File(gpxAbsolutePath);
		}
		gpxUri = bundle.getParcelable("gpxUri");
		density = bundle.getFloat("density");
		widthPixels = bundle.getInt("widthPixels");
		heightPixels = bundle.getInt("heightPixels");
		color = bundle.getInt("color");
	}
}