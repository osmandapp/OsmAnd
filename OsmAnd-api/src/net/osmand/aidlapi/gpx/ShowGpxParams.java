package net.osmand.aidlapi.gpx;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class ShowGpxParams extends AidlParams {

	private String fileName;

	public ShowGpxParams(String fileName) {
		this.fileName = fileName;
	}

	public ShowGpxParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ShowGpxParams> CREATOR = new Creator<ShowGpxParams>() {
		@Override
		public ShowGpxParams createFromParcel(Parcel in) {
			return new ShowGpxParams(in);
		}

		@Override
		public ShowGpxParams[] newArray(int size) {
			return new ShowGpxParams[size];
		}
	};

	public String getFileName() {
		return fileName;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("fileName", fileName);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		fileName = bundle.getString("fileName");
	}
}