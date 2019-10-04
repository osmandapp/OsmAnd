package net.osmand.aidl.copyfile;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class CopyFileParams implements Parcelable {
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

	protected CopyFileParams(Parcel in) {
		fileName = in.readString();
		filePartData = in.createByteArray();
		startTime = in.readLong();
		done = in.readByte() == 0;
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


	@Override
	public int describeContents() {
		return 0;
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

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(fileName);
		dest.writeByteArray(filePartData);
		dest.writeLong(startTime);
		dest.writeByte((byte) (done ? 0 : 1));
	}
}
