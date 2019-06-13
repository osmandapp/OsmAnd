package net.osmand.aidl.gpx;

import android.os.Parcel;
import android.os.Parcelable;

public class HideGpxParams implements Parcelable {

	private String fileName;

	public HideGpxParams(String fileName) {
		this.fileName = fileName;
	}

	public HideGpxParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<HideGpxParams> CREATOR = new
			Creator<HideGpxParams>() {
				public HideGpxParams createFromParcel(Parcel in) {
					return new HideGpxParams(in);
				}

				public HideGpxParams[] newArray(int size) {
					return new HideGpxParams[size];
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

