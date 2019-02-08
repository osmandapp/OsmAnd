package net.osmand.aidl.tiles;

import android.os.Parcel;
import android.os.Parcelable;

public class CopyFileParams implements Parcelable {
	private String filename;
	private long size;
	private long sentSize;
	private byte[] filePartData;
	private long copyStartTime;

	public CopyFileParams(String filename, long size, long sentSize, byte[] filePartData, long copyStartTime) {
		this.filename = filename;
		this.size = size;
		this.sentSize = sentSize;
		this.filePartData = filePartData;
		this.copyStartTime = copyStartTime;
	}

	public String getFilename() {
		return filename;
	}


	public long getSize() {
		return size;
	}

	public long getSentSize() {
		return sentSize;
	}

	public byte[] getFilePartData() {
		return filePartData;
	}

	public long getCopyStartTime() {
		return copyStartTime;
	}

	protected CopyFileParams(Parcel in) {
		filename = in.readString();
		size = in.readLong();
		sentSize = in.readLong();
		filePartData = in.createByteArray();
		copyStartTime = in.readLong();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(filename);
		dest.writeLong(size);
		dest.writeLong(sentSize);
		dest.writeByteArray(filePartData);
		dest.writeLong(copyStartTime);
	}

	@Override
	public int describeContents() {
		return 0;
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
}
