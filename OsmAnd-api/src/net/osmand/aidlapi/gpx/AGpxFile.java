package net.osmand.aidlapi.gpx;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.aidlapi.AidlParams;

public class AGpxFile extends AidlParams {

	private String fileName;
	private String relativePath;
	private long modifiedTime;
	private long fileSize;
	private boolean active;
	private String color;
	private AGpxFileDetails details;
	private String activityType;
	private String nearestCityName;
	private double startLatitude = Double.NaN;
	private double startLongitude = Double.NaN;

	public AGpxFile(@NonNull String fileName, long modifiedTime, long fileSize, boolean active, String color, @Nullable AGpxFileDetails details) {
		this.fileName = fileName;
		this.modifiedTime = modifiedTime;
		this.fileSize = fileSize;
		this.active = active;
		this.color = color;
		this.details = details;
	}

	public AGpxFile(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AGpxFile> CREATOR = new Creator<AGpxFile>() {
		@Override
		public AGpxFile createFromParcel(Parcel in) {
			return new AGpxFile(in);
		}

		@Override
		public AGpxFile[] newArray(int size) {
			return new AGpxFile[size];
		}
	};

	public String getFileName() {
		return fileName;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public long getModifiedTime() {
		return modifiedTime;
	}

	public long getFileSize() {
		return fileSize;
	}

	public boolean isActive() {
		return active;
	}

	public String getColor() {
		return color;
	}

	public AGpxFileDetails getDetails() {
		return details;
	}

	/**
	 * @return activity id set for the track (e.g. "skiing", "hiking"), null if not set
	 */
	@Nullable
	public String getActivityType() {
		return activityType;
	}

	public void setActivityType(@Nullable String activityType) {
		this.activityType = activityType;
	}

	/**
	 * @return name of the city nearest to the track start, null if unknown
	 */
	@Nullable
	public String getNearestCityName() {
		return nearestCityName;
	}

	public void setNearestCityName(@Nullable String nearestCityName) {
		this.nearestCityName = nearestCityName;
	}

	/**
	 * @return latitude of the track start, NaN if unknown
	 */
	public double getStartLatitude() {
		return startLatitude;
	}

	/**
	 * @return longitude of the track start, NaN if unknown
	 */
	public double getStartLongitude() {
		return startLongitude;
	}

	public void setStartLocation(double latitude, double longitude) {
		this.startLatitude = latitude;
		this.startLongitude = longitude;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("fileName", fileName);
		bundle.putString("relativePath", relativePath);
		bundle.putLong("modifiedTime", modifiedTime);
		bundle.putLong("fileSize", fileSize);
		bundle.putBoolean("active", active);
		bundle.putParcelable("details", details);
		bundle.putString("color", color);
		// new fields go only when present: a track list is one binder transaction (~1 MB)
		if (activityType != null && !activityType.isEmpty()) {
			bundle.putString("activityType", activityType);
		}
		if (nearestCityName != null && !nearestCityName.isEmpty()) {
			bundle.putString("nearestCityName", nearestCityName);
		}
		if (!Double.isNaN(startLatitude) && !Double.isNaN(startLongitude)) {
			bundle.putDouble("startLatitude", startLatitude);
			bundle.putDouble("startLongitude", startLongitude);
		}
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AGpxFileDetails.class.getClassLoader());
		fileName = bundle.getString("fileName");
		relativePath = bundle.getString("relativePath");
		modifiedTime = bundle.getLong("modifiedTime");
		fileSize = bundle.getLong("fileSize");
		active = bundle.getBoolean("active");
		details = bundle.getParcelable("details");
		color = bundle.getString("color");
		activityType = bundle.getString("activityType");
		nearestCityName = bundle.getString("nearestCityName");
		startLatitude = bundle.getDouble("startLatitude", Double.NaN);
		startLongitude = bundle.getDouble("startLongitude", Double.NaN);
	}
}