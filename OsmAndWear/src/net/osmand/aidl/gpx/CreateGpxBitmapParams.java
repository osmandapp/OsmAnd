package net.osmand.aidl.gpx;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

public class CreateGpxBitmapParams implements Parcelable {

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

	public static final Creator<CreateGpxBitmapParams> CREATOR = new
			Creator<CreateGpxBitmapParams>() {
				public CreateGpxBitmapParams createFromParcel(Parcel in) {
					return new CreateGpxBitmapParams(in);
				}

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

	public void writeToParcel(Parcel out, int flags) {
		if (gpxFile != null) {
			out.writeString(gpxFile.getAbsolutePath());
		} else {
			out.writeString(null);
		}
		out.writeParcelable(gpxUri, flags);
		out.writeFloat(density);
		out.writeInt(widthPixels);
		out.writeInt(heightPixels);
		out.writeInt(color);
	}

	private void readFromParcel(Parcel in) {
		String gpxAbsolutePath = in.readString();
		if (gpxAbsolutePath != null) {
			gpxFile = new File(gpxAbsolutePath);
		}
		gpxUri = in.readParcelable(Uri.class.getClassLoader());
		density = in.readFloat();
		widthPixels = in.readInt();
		heightPixels = in.readInt();
		color = in.readInt();
	}

	public int describeContents() {
		return 0;
	}
}