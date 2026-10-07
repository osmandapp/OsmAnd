package net.osmand.aidlapi.info;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

/**
 * Profile whose map widgets layout to read.
 */
public class MapWidgetsLayoutParams extends AidlParams {

	private String appModeKey;

	public MapWidgetsLayoutParams(String appModeKey) {
		this.appModeKey = appModeKey;
	}

	public MapWidgetsLayoutParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<MapWidgetsLayoutParams> CREATOR = new Creator<MapWidgetsLayoutParams>() {
		@Override
		public MapWidgetsLayoutParams createFromParcel(Parcel in) {
			return new MapWidgetsLayoutParams(in);
		}

		@Override
		public MapWidgetsLayoutParams[] newArray(int size) {
			return new MapWidgetsLayoutParams[size];
		}
	};

	/**
	 * @return profile key; empty for the current profile
	 */
	public String getAppModeKey() {
		return appModeKey;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("appModeKey", appModeKey);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		appModeKey = bundle.getString("appModeKey");
	}
}
