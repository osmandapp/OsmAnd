package net.osmand.aidl.gpx;

import android.os.Parcel;
import android.os.Parcelable;

public class RemoveGpxParams implements Parcelable {

	private String fileName;

	public RemoveGpxParams(String fileName) {
		this.fileName = fileName;
	}

	public RemoveGpxParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<RemoveGpxParams> CREATOR = new
			Creator<RemoveGpxParams>() {
				@Override
				public RemoveGpxParams createFromParcel(Parcel in) {
					return new RemoveGpxParams(in);
				}

				@Override
				public RemoveGpxParams[] newArray(int size) {
					return new RemoveGpxParams[size];
				}
			};

	public String getFileName() {
		return fileName;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(fileName);
	}

	private void readFromParcel(Parcel in) {
		fileName = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}
}
