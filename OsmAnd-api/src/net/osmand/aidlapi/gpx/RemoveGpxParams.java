package net.osmand.aidlapi.gpx;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class RemoveGpxParams extends AidlParams {

	private String relativePath;

	public RemoveGpxParams(String relativePath) {
		this.relativePath = relativePath;
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

	public String getRelativePath() {
		return relativePath;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("relativePath", relativePath);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		relativePath = bundle.getString("relativePath");
	}
}