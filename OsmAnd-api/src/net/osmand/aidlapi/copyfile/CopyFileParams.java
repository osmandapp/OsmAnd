package net.osmand.aidlapi.copyfile;

import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;

import net.osmand.aidlapi.AidlParams;

public class CopyFileParams extends AidlParams {

	private String fileName;
	private byte[] filePartData;
	private long startTime;
	private boolean done;

	public CopyFileParams(@NonNull String fileName, @NonNull byte[] filePartData, long startTime, boolean done) {
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
		bundle.putString("fileName", fileName);
		bundle.putByteArray("filePartData", filePartData);
		bundle.putLong("startTime", startTime);
		bundle.putBoolean("done", done);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		fileName = bundle.getString("fileName");
		filePartData = bundle.getByteArray("filePartData");
		startTime = bundle.getLong("startTime");
		done = bundle.getBoolean("done");
	}

	@Override
	public String toString() {
		return "CopyFileParams {" +
				" fileName=" + fileName +
				", filePartData size=" + filePartData.length +
				", startTime=" + startTime +
				", done=" + done +
				" }";
	}
}