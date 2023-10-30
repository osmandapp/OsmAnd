package net.osmand.aidlapi.gpx;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.Nullable;

import net.osmand.aidlapi.AidlParams;

public class HideGpxParams extends AidlParams {

	@Nullable
	private String filePath;
	@Nullable
	private String fileName;

	public HideGpxParams(@Nullable String fileName) {
		this(null, fileName);
	}

	public HideGpxParams(@Nullable String filePath, @Nullable String fileName) {
		this.filePath = filePath;
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

	@Nullable
	public String getFileName() {
		return fileName;
	}

	@Nullable
	public String getFilePath() {
		return filePath;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("fileName", fileName);
		bundle.putString("filePath", filePath);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		fileName = bundle.getString("fileName");
		filePath = bundle.getString("filePath");
	}
}