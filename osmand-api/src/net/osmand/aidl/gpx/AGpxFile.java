package net.osmand.aidl.gpx;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class AGpxFile implements Parcelable {

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
		readFromParcel(in);
	}

	public static final Creator<AGpxFile> CREATOR = new
			Creator<AGpxFile>() {
				public AGpxFile createFromParcel(Parcel in) {
					return new AGpxFile(in);
				}

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

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(fileName);
		out.writeLong(modifiedTime);
		out.writeLong(fileSize);
		out.writeByte((byte) (active ? 1 : 0));

		out.writeByte((byte) (details != null ? 1 : 0));
		if (details != null) {
			out.writeParcelable(details, flags);
		}
	}

	private void readFromParcel(Parcel in) {
		fileName = in.readString();
		modifiedTime = in.readLong();
		fileSize = in.readLong();
		active = in.readByte() != 0;

		boolean hasDetails = in.readByte() != 0;
		if (hasDetails) {
			details = in.readParcelable(AGpxFileDetails.class.getClassLoader());
		} else {
			details = null;
		}
	}

	public int describeContents() {
		return 0;
	}
}