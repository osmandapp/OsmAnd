package net.osmand.aidl.tiles;

import android.os.Parcel;
import android.os.Parcelable;

public class CopyFileParams implements Parcelable {
	private String filename;
	private long sentSize;
	private byte[] filePartData;
	private long copyStartTime;
	private boolean isTransmitComplete;

	public CopyFileParams(String filename, long sentSize, byte[] filePartData, long copyStartTime, boolean isTransmitComplete) {
		this.filename = filename;
		this.sentSize = sentSize;
		this.filePartData = filePartData;
		this.copyStartTime = copyStartTime;
		this.isTransmitComplete = isTransmitComplete;
	}

	public String getFilename() {
		return filename;
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

	public boolean isTransmitComplete() {return  isTransmitComplete; }


	protected CopyFileParams(Parcel in) {
		filename = in.readString();
		sentSize = in.readLong();
		filePartData = in.createByteArray();
		copyStartTime = in.readLong();
		isTransmitComplete = in.readByte() != 0;
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
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(filename);
		dest.writeLong(sentSize);
		dest.writeByteArray(filePartData);
		dest.writeLong(copyStartTime);
		dest.writeByte((byte) (isTransmitComplete ? 1 : 0));
	}

	@Override
	public String toString() {
		return "Filename: " + filename + ", sentSize = " + sentSize + ", filePartData size = " +
			filePartData.length + ", startTime: " + copyStartTime + ", isTransmitComplete: "+ isTransmitComplete;
	}
}
