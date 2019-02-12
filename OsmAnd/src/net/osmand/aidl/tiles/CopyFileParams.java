package net.osmand.aidl.tiles;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public class CopyFileParams implements Parcelable {
	private String filename;
	private byte[] filePartData;
	private long startTime;
	private int actionId;

	public CopyFileParams(String filename, byte[] filePartData, long startTime, int actionId) {
		this.filename = filename;
		this.filePartData = filePartData;
		this.startTime = startTime;
		this.actionId = actionId;
	}

	public String getFilename() {
		return filename;
	}


	public byte[] getFilePartData() {
		return filePartData;
	}

	public int getActionId() {
		return actionId;
	}

	public long getStartTime() {
		return startTime;
	}

	protected CopyFileParams(Parcel in) {
		filename = in.readString();
		filePartData = in.createByteArray();
		startTime = in.readLong();
		actionId = in.readInt();
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
		return "CopyFileParams{" +
			"filename='" + filename + '\'' +
			", filePartData=" + filePartData +
			", startTime=" + startTime +
			", actionId=" + actionId +
			'}';
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(filename);
		dest.writeByteArray(filePartData);
		dest.writeLong(startTime);
		dest.writeInt(actionId);

	}


}
