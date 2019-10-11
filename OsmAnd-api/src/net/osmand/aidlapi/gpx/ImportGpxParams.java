package net.osmand.aidlapi.gpx;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.io.File;

public class ImportGpxParams extends AidlParams {

	private File gpxFile;
	private Uri gpxUri;
	private String sourceRawData;
	private String destinationPath;
	private String color;
	private boolean show;

	public ImportGpxParams(File gpxFile, String destinationPath, String color, boolean show) {
		this.gpxFile = gpxFile;
		this.destinationPath = destinationPath;
		this.color = color;
		this.show = show;
	}

	public ImportGpxParams(Uri gpxUri, String destinationPath, String color, boolean show) {
		this.gpxUri = gpxUri;
		this.destinationPath = destinationPath;
		this.color = color;
		this.show = show;
	}

	public ImportGpxParams(String sourceRawData, String destinationPath, String color, boolean show) {
		this.sourceRawData = sourceRawData;
		this.destinationPath = destinationPath;
		this.color = color;
		this.show = show;
	}

	public ImportGpxParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ImportGpxParams> CREATOR = new Creator<ImportGpxParams>() {
		@Override
		public ImportGpxParams createFromParcel(Parcel in) {
			return new ImportGpxParams(in);
		}

		@Override
		public ImportGpxParams[] newArray(int size) {
			return new ImportGpxParams[size];
		}
	};

	public File getGpxFile() {
		return gpxFile;
	}

	public Uri getGpxUri() {
		return gpxUri;
	}

	public String getSourceRawData() {
		return sourceRawData;
	}

	public String getDestinationPath() {
		return destinationPath;
	}

	public String getColor() {
		return color;
	}

	public boolean isShow() {
		return show;
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(Uri.class.getClassLoader());

		String gpxAbsolutePath = bundle.getString("gpxAbsolutePath");
		if (gpxAbsolutePath != null) {
			gpxFile = new File(gpxAbsolutePath);
		}
		gpxUri = bundle.getParcelable("gpxUri");
		sourceRawData = bundle.getString("sourceRawData");
		destinationPath = bundle.getString("destinationPath");
		color = bundle.getString("color");
		show = bundle.getBoolean("show");
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("gpxAbsolutePath", gpxFile != null ? gpxFile.getAbsolutePath() : null);
		bundle.putParcelable("gpxUri", gpxUri);
		bundle.putString("sourceRawData", sourceRawData);
		bundle.putString("destinationPath", destinationPath);
		bundle.putString("color", color);
		bundle.putBoolean("show", show);
	}
}