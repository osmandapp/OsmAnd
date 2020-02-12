package net.osmand.aidl.gpx;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ASelectedGpxFile implements Parcelable {

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

	public static final Creator<ASelectedGpxFile> CREATOR = new
			Creator<ASelectedGpxFile>() {
				public ASelectedGpxFile createFromParcel(Parcel in) {
					return new ASelectedGpxFile(in);
				}

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

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(fileName);
		out.writeLong(modifiedTime);
		out.writeLong(fileSize);

		out.writeByte((byte) (details != null ? 1 : 0));
		if (details != null) {
			out.writeParcelable(details, flags);
		}
	}

	private void readFromParcel(Parcel in) {
		fileName = in.readString();
		modifiedTime = in.readLong();
		fileSize = in.readLong();

		boolean hasDetails= in.readByte() != 0;
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

