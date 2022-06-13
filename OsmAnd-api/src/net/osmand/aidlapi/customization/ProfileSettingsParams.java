package net.osmand.aidlapi.customization;

import static net.osmand.aidlapi.profile.ExportProfileParams.SETTINGS_TYPE_KEY;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;
import net.osmand.aidlapi.profile.AExportSettingsType;

import java.util.ArrayList;
import java.util.List;

public class ProfileSettingsParams extends AidlParams {

	public static final String VERSION_KEY = "version";
	public static final String REPLACE_KEY = "replace";
	public static final String SILENT_IMPORT_KEY = "silentImport";
	public static final String LATEST_CHANGES_KEY = "latestChanges";
	public static final String PROFILE_SETTINGS_URI_KEY = "profileSettingsUri";
	private Uri profileSettingsUri;
	private String latestChanges;
	private int version;
	private List<String> settingsTypeKeyList = new ArrayList<>();
	private boolean silent;
	private boolean replace;

	public ProfileSettingsParams(Uri profileSettingsUri, List<AExportSettingsType> settingsTypeList, boolean replace,
	                             boolean silent, String latestChanges, int version) {
		this.profileSettingsUri = profileSettingsUri;
		for (AExportSettingsType settingsType : settingsTypeList) {
			settingsTypeKeyList.add(settingsType.name());
		}
		this.replace = replace;
		this.latestChanges = latestChanges;
		this.version = version;
		this.silent = silent;
	}

	public ProfileSettingsParams(List<String> settingsTypeList, Uri profileSettingsUri, boolean replace,
	                             boolean silent, String latestChanges, int version) {
		this.profileSettingsUri = profileSettingsUri;
		if (settingsTypeList != null) {
			settingsTypeKeyList.addAll(settingsTypeList);
		}
		this.replace = replace;
		this.latestChanges = latestChanges;
		this.version = version;
		this.silent = silent;
	}

	public ProfileSettingsParams(Uri profileSettingsUri, List<AExportSettingsType> settingsTypeList,
	                             boolean replace, String latestChanges, int version) {
		this(profileSettingsUri, settingsTypeList, replace, false, latestChanges, version);
	}

	public ProfileSettingsParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ProfileSettingsParams> CREATOR = new Creator<ProfileSettingsParams>() {
		@Override
		public ProfileSettingsParams createFromParcel(Parcel in) {
			return new ProfileSettingsParams(in);
		}

		@Override
		public ProfileSettingsParams[] newArray(int size) {
			return new ProfileSettingsParams[size];
		}
	};

	public int getVersion() {
		return version;
	}

	public String getLatestChanges() {
		return latestChanges;
	}

	public Uri getProfileSettingsUri() {
		return profileSettingsUri;
	}

	public List<String> getSettingsTypeKeys() {
		return settingsTypeKeyList;
	}

	public void setSettingsTypeKeyList(List<String> settingsTypeKeyList) {
		this.settingsTypeKeyList = settingsTypeKeyList;
	}

	public boolean isReplace() {
		return replace;
	}

	public boolean isSilent() {
		return silent;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putInt(VERSION_KEY, version);
		bundle.putString(LATEST_CHANGES_KEY, latestChanges);
		bundle.putParcelable(PROFILE_SETTINGS_URI_KEY, profileSettingsUri);
		bundle.putStringArrayList(SETTINGS_TYPE_KEY, new ArrayList<>(settingsTypeKeyList));
		bundle.putBoolean(REPLACE_KEY, replace);
		bundle.putBoolean(SILENT_IMPORT_KEY, silent);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		version = bundle.getInt(VERSION_KEY);
		latestChanges = bundle.getString(LATEST_CHANGES_KEY);
		profileSettingsUri = bundle.getParcelable(PROFILE_SETTINGS_URI_KEY);
		settingsTypeKeyList = bundle.getStringArrayList(SETTINGS_TYPE_KEY);
		replace = bundle.getBoolean(REPLACE_KEY);
		silent = bundle.getBoolean(SILENT_IMPORT_KEY);
	}
}