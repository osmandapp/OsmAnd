package net.osmand.aidl.gpx;

import android.os.Parcel;
import android.os.Parcelable;

public class ShowGpxParams implements Parcelable {

	private String fileName;

	public ShowGpxParams(String fileName) {
		this.fileName = fileName;
	}

	public ShowGpxParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ShowGpxParams> CREATOR = new
			Creator<ShowGpxParams>() {
				public ShowGpxParams createFromParcel(Parcel in) {
					return new ShowGpxParams(in);
				}

				public ShowGpxParams[] newArray(int size) {
					return new ShowGpxParams[size];
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
