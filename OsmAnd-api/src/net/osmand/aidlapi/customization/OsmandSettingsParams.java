package net.osmand.aidlapi.customization;

import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.aidlapi.AidlParams;

public class OsmandSettingsParams extends AidlParams {

	private String sharedPreferencesName;
	private Bundle bundleSettings;

	public OsmandSettingsParams(@NonNull String sharedPreferencesName, @Nullable Bundle bundle) {
		this.sharedPreferencesName = sharedPreferencesName;
		this.bundleSettings = bundle;
	}

	public OsmandSettingsParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<OsmandSettingsParams> CREATOR = new Creator<OsmandSettingsParams>() {
		@Override
		public OsmandSettingsParams createFromParcel(Parcel in) {
			return new OsmandSettingsParams(in);
		}

		@Override
		public OsmandSettingsParams[] newArray(int size) {
			return new OsmandSettingsParams[size];
		}
	};

	public String getSharedPreferencesName() {
		return sharedPreferencesName;
	}

	public Bundle getBundle() {
		return bundleSettings;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("sharedPreferencesName", sharedPreferencesName);
		bundle.putBundle("bundleSettings", bundleSettings);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		sharedPreferencesName = bundle.getString("sharedPreferencesName");
		bundleSettings = bundle.getBundle("bundleSettings");
	}
}