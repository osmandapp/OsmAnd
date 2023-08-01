package net.osmand.aidlapi.gpx;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class RemoveGpxParams extends AidlParams {

	private String fileName;
	private String relativePath;

	public RemoveGpxParams(String fileName) {
		this.fileName = fileName;
	}

	public RemoveGpxParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<RemoveGpxParams> CREATOR = new Creator<RemoveGpxParams>() {
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

	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("fileName", fileName);
		bundle.putString("relativePath", relativePath);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		fileName = bundle.getString("fileName");
		relativePath = bundle.getString("relativePath");
	}
}