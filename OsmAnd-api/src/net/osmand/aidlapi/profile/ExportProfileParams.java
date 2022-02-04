package net.osmand.aidlapi.profile;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;
import java.util.List;

public class ExportProfileParams extends AidlParams {

	public static final String PROFILE_KEY = "profile";
	public static final String SETTINGS_TYPE_KEY = "settings_type";
	private String profile;
	private ArrayList<String> settingsTypeKeyList = new ArrayList<>();

	public ExportProfileParams(String profile, ArrayList<AExportSettingsType> settingsTypeList) {
		this.profile = profile;
		for (AExportSettingsType settingsType : settingsTypeList) {
			settingsTypeKeyList.add(settingsType.name());
		}
	}

	public ExportProfileParams(String profile, List<String> settingsTypeList) {
		this.profile = profile;
		if (settingsTypeList != null) {
			settingsTypeKeyList.addAll(settingsTypeList);
		}
	}

	public ExportProfileParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ExportProfileParams> CREATOR = new Creator<ExportProfileParams>() {
		@Override
		public ExportProfileParams createFromParcel(Parcel in) {
			return new ExportProfileParams(in);
		}

		@Override
		public ExportProfileParams[] newArray(int size) {
			return new ExportProfileParams[size];
		}
	};

	public String getProfile() {
		return profile;
	}

	public List<String> getSettingsTypeKeys() {
		return settingsTypeKeyList;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString(PROFILE_KEY, profile);
		bundle.putStringArrayList(SETTINGS_TYPE_KEY, settingsTypeKeyList);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		profile = bundle.getString(PROFILE_KEY);
		settingsTypeKeyList = bundle.getStringArrayList(SETTINGS_TYPE_KEY);
	}
}