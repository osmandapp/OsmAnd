package net.osmand.aidl.gpx;

import android.os.Parcel;
import android.os.Parcelable;

public class ASelectedGpxFile implements Parcelable {

	private String fileName;

	public ASelectedGpxFile(String fileName) {
		this.fileName = fileName;
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

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(fileName);
	}

	private void readFromParcel(Parcel in) {
		fileName = in.readString();
	}

	public int describeContents() {
		return 0;
	}
}

