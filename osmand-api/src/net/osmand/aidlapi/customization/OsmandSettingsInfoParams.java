package net.osmand.aidlapi.customization;

import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;

import net.osmand.aidlapi.AidlParams;

public class OsmandSettingsInfoParams extends AidlParams {

	private String sharedPreferencesName;

	public OsmandSettingsInfoParams(@NonNull String sharedPreferencesName) {
		this.sharedPreferencesName = sharedPreferencesName;
	}

	public OsmandSettingsInfoParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<OsmandSettingsInfoParams> CREATOR = new Creator<OsmandSettingsInfoParams>() {
		@Override
		public OsmandSettingsInfoParams createFromParcel(Parcel in) {
			return new OsmandSettingsInfoParams(in);
		}

		@Override
		public OsmandSettingsInfoParams[] newArray(int size) {
			return new OsmandSettingsInfoParams[size];
		}
	};

	public String getSharedPreferencesName() {
		return sharedPreferencesName;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("sharedPreferencesName", sharedPreferencesName);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		sharedPreferencesName = bundle.getString("sharedPreferencesName");
	}
}