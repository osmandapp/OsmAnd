package net.osmand.aidlapi.customization;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class SelectProfileParams extends AidlParams {

	public static final String PROFILE_ID_KEY = "profile_id";

	private String appModeKey;

	public SelectProfileParams(String appModeKey) {
		this.appModeKey = appModeKey;
	}

	public SelectProfileParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<SelectProfileParams> CREATOR = new Creator<SelectProfileParams>() {
		@Override
		public SelectProfileParams createFromParcel(Parcel in) {
			return new SelectProfileParams(in);
		}

		@Override
		public SelectProfileParams[] newArray(int size) {
			return new SelectProfileParams[size];
		}
	};

	public String getAppModeKey() {
		return appModeKey;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString(PROFILE_ID_KEY, appModeKey);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		appModeKey = bundle.getString(PROFILE_ID_KEY);
	}
}