package net.osmand.aidlapi.map;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

/**
 * A recorded flyover video to open.
 */
public class FlyoverFileParams extends AidlParams {

	private String fileName;

	public FlyoverFileParams(String fileName) {
		this.fileName = fileName;
	}

	public FlyoverFileParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<FlyoverFileParams> CREATOR = new Creator<FlyoverFileParams>() {
		@Override
		public FlyoverFileParams createFromParcel(Parcel in) {
			return new FlyoverFileParams(in);
		}

		@Override
		public FlyoverFileParams[] newArray(int size) {
			return new FlyoverFileParams[size];
		}
	};

	/**
	 * @return file name in OsmAnd's flyover folder, or the path recordMapFlyover returned
	 */
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
