package net.osmand.aidlapi.customization;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class ProfileSettingsParams extends AidlParams {

	private Uri profileSettingsUri;
	private String latestChanges;
	private int version;

	public ProfileSettingsParams(Uri profileSettingsUri, String latestChanges, int version) {
		this.profileSettingsUri = profileSettingsUri;
		this.latestChanges = latestChanges;
		this.version = version;
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

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putInt("version", version);
		bundle.putString("latestChanges", latestChanges);
		bundle.putParcelable("profileSettingsUri", profileSettingsUri);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		version = bundle.getInt("version");
		latestChanges = bundle.getString("latestChanges");
		profileSettingsUri = bundle.getParcelable("profileSettingsUri");
	}
}