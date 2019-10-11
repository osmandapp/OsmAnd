package net.osmand.aidlapi.gpx;

import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.aidlapi.AidlParams;

public class ASelectedGpxFile extends AidlParams {

	private String fileName;
	private long modifiedTime;
	private long fileSize;
	private AGpxFileDetails details;

	public ASelectedGpxFile(@NonNull String fileName) {
		this.fileName = fileName;
	}

	public ASelectedGpxFile(@NonNull String fileName, long modifiedTime, long fileSize, @Nullable AGpxFileDetails details) {
		this.fileName = fileName;
		this.modifiedTime = modifiedTime;
		this.fileSize = fileSize;
		this.details = details;
	}

	public ASelectedGpxFile(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ASelectedGpxFile> CREATOR = new Creator<ASelectedGpxFile>() {
		@Override
		public ASelectedGpxFile createFromParcel(Parcel in) {
			return new ASelectedGpxFile(in);
		}

		@Override
		public ASelectedGpxFile[] newArray(int size) {
			return new ASelectedGpxFile[size];
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

	public AGpxFileDetails getDetails() {
		return details;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("fileName", fileName);
		bundle.putLong("modifiedTime", modifiedTime);
		bundle.putLong("fileSize", fileSize);
		bundle.putParcelable("details", details);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AGpxFileDetails.class.getClassLoader());
		fileName = bundle.getString("fileName");
		modifiedTime = bundle.getLong("modifiedTime");
		fileSize = bundle.getLong("fileSize");
		details = bundle.getParcelable("details");
	}
}