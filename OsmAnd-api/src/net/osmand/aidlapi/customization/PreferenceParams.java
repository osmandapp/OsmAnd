package net.osmand.aidlapi.customization;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import net.osmand.aidlapi.AidlParams;

public class PreferenceParams extends AidlParams {

	private String prefId;
	private String appModeKey;
	private String value;

	public PreferenceParams(@NonNull String prefId) {
		this.prefId = prefId;
	}

	public PreferenceParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<PreferenceParams> CREATOR = new Creator<PreferenceParams>() {
		@Override
		public PreferenceParams createFromParcel(Parcel in) {
			return new PreferenceParams(in);
		}

		@Override
		public PreferenceParams[] newArray(int size) {
			return new PreferenceParams[size];
		}
	};

	public String getPrefId() {
		return prefId;
	}

	public String getAppModeKey() {
		return appModeKey;
	}

	public void setAppModeKey(String appModeKey) {
		this.appModeKey = appModeKey;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("preferenceId", prefId);
		bundle.putString("appModeKey", appModeKey);
		bundle.putString("value", value);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		prefId = bundle.getString("preferenceId");
		appModeKey = bundle.getString("appModeKey");
		value = bundle.getString("value");
	}
}