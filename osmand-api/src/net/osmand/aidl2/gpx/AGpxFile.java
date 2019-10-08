package net.osmand.aidl2.gpx;

import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.aidl2.AidlParams;

public class AGpxFile extends AidlParams {

	private String fileName;
	private long modifiedTime;
	private long fileSize;
	private boolean active;
	private AGpxFileDetails details;

	public AGpxFile(@NonNull String fileName, long modifiedTime, long fileSize, boolean active, @Nullable AGpxFileDetails details) {
		this.fileName = fileName;
		this.modifiedTime = modifiedTime;
		this.fileSize = fileSize;
		this.active = active;
		this.details = details;
	}

	public AGpxFile(Parcel in) {
		super(in);
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

	public long getModifiedTime() {
		return modifiedTime;
	}

	public long getFileSize() {
		return fileSize;
	}

	public boolean isActive() {
		return active;
	}

	public AGpxFileDetails getDetails() {
		return details;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("fileName", fileName);
		bundle.putLong("modifiedTime", modifiedTime);
		bundle.putLong("fileSize", fileSize);
		bundle.putBoolean("active", active);
		bundle.putParcelable("details", details);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AGpxFileDetails.class.getClassLoader());
		fileName = bundle.getString("fileName");
		modifiedTime = bundle.getLong("modifiedTime");
		fileSize = bundle.getLong("fileSize");
		active = bundle.getBoolean("active");
		details = bundle.getParcelable("details");
	}
}