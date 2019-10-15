package net.osmand.aidlapi.gpx;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class HideGpxParams extends AidlParams {

	private String fileName;

	public HideGpxParams(String fileName) {
		this.fileName = fileName;
	}

	public HideGpxParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<HideGpxParams> CREATOR = new Creator<HideGpxParams>() {
		@Override
		public HideGpxParams createFromParcel(Parcel in) {
			return new HideGpxParams(in);
		}

		@Override
		public HideGpxParams[] newArray(int size) {
			return new HideGpxParams[size];
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