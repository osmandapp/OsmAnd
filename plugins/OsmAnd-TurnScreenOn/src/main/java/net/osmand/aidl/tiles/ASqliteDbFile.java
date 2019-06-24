package net.osmand.aidl.tiles;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class ASqliteDbFile implements Parcelable {

	private String fileName;
	private long modifiedTime;
	private long fileSize;
	private boolean active;

	public ASqliteDbFile(@NonNull String fileName, long modifiedTime, long fileSize, boolean active) {
		this.fileName = fileName;
		this.modifiedTime = modifiedTime;
		this.fileSize = fileSize;
		this.active = active;
	}

	public ASqliteDbFile(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ASqliteDbFile> CREATOR = new
			Creator<ASqliteDbFile>() {
				public ASqliteDbFile createFromParcel(Parcel in) {
					return new ASqliteDbFile(in);
				}

				public ASqliteDbFile[] newArray(int size) {
					return new ASqliteDbFile[size];
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

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(fileName);
		out.writeLong(modifiedTime);
		out.writeLong(fileSize);
		out.writeByte((byte) (active ? 1 : 0));
	}

	private void readFromParcel(Parcel in) {
		fileName = in.readString();
		modifiedTime = in.readLong();
		fileSize = in.readLong();
		active = in.readByte() != 0;
	}

	public int describeContents() {
		return 0;
	}
}
