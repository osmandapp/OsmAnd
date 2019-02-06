package net.osmand.aidl.tiles;

import android.os.Parcel;
import android.os.Parcelable;

public class FilePartParams implements Parcelable {
	private String filename;
	private long size;
	private long sentSize;
	private byte[] filePartData;

	public FilePartParams(String filename, String filePartId, long size, long sentSize, byte[] filePartData) {
		this.filename = filename;
		this.size = size;
		this.sentSize = sentSize;
		this.filePartData = filePartData;
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

	protected FilePartParams(Parcel in) {
		filename = in.readString();
		size = in.readLong();
		sentSize = in.readLong();
		filePartData = in.createByteArray();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(filename);
		dest.writeLong(size);
		dest.writeLong(sentSize);
		dest.writeByteArray(filePartData);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<FilePartParams> CREATOR = new Creator<FilePartParams>() {
		@Override
		public FilePartParams createFromParcel(Parcel in) {
			return new FilePartParams(in);
		}

		@Override
		public FilePartParams[] newArray(int size) {
			return new FilePartParams[size];
		}
	};
}
