package net.osmand.aidlapi.copyfile;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import net.osmand.aidlapi.AidlParams;

public class CopyFileParams extends AidlParams {

	public static final String DESTINATION_DIR_KEY = "destinationDir";
	public static final String FILE_NAME_KEY = "fileName";
	public static final String FILE_PART_DATA_KEY = "filePartData";
	public static final String START_TIME_KEY = "startTime";
	public static final String DONE_KEY = "done";
	private String destinationDir;
	private String fileName;
	private byte[] filePartData;
	private long startTime;
	private boolean done;

	public CopyFileParams(@NonNull String destinationDir, @NonNull String fileName, @NonNull byte[] filePartData,
	                      long startTime, boolean done) {

		this.destinationDir = destinationDir;
		this.fileName = fileName;
		this.filePartData = filePartData;
		this.startTime = startTime;
		this.done = done;
	}

	protected CopyFileParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<CopyFileParams> CREATOR = new Creator<CopyFileParams>() {
		@Override
		public CopyFileParams createFromParcel(Parcel in) {
			return new CopyFileParams(in);
		}

		@Override
		public CopyFileParams[] newArray(int size) {
			return new CopyFileParams[size];
		}
	};

	public String getDestinationDir() {
		return destinationDir;
	}

	public String getFileName() {
		return fileName;
	}

	public byte[] getFilePartData() {
		return filePartData;
	}

	public boolean isDone() {
		return done;
	}

	public long getStartTime() {
		return startTime;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString(DESTINATION_DIR_KEY, destinationDir);
		bundle.putString(FILE_NAME_KEY, fileName);
		bundle.putByteArray(FILE_PART_DATA_KEY, filePartData);
		bundle.putLong(START_TIME_KEY, startTime);
		bundle.putBoolean(DONE_KEY, done);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		destinationDir = bundle.getString(DESTINATION_DIR_KEY);
		fileName = bundle.getString(FILE_NAME_KEY);
		filePartData = bundle.getByteArray(FILE_PART_DATA_KEY);
		startTime = bundle.getLong(START_TIME_KEY);
		done = bundle.getBoolean(DONE_KEY);
	}

	@Override
	public String toString() {
		return "CopyFileParams {" +
				" destinationDir=" + destinationDir +
				" fileName=" + fileName +
				", filePartData size=" + filePartData.length +
				", startTime=" + startTime +
				", done=" + done +
				" }";
	}
}