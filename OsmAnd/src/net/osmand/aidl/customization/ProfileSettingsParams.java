package net.osmand.aidl.customization;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import net.osmand.aidlapi.profile.ExportSettingsType;

import java.util.ArrayList;

public class ProfileSettingsParams implements Parcelable {

	private Uri profileSettingsUri;
	private String latestChanges;
	private int version;
	ArrayList<String> settingsTypeKeyList = new ArrayList<>();
	boolean replace;

	public ProfileSettingsParams(Uri profileSettingsUri, ArrayList<ExportSettingsType> settingsTypeList,
	                             boolean replace, String latestChanges, int version) {
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

	public ArrayList<String> getSettingsTypeKeys() {
		return settingsTypeKeyList;
	}

	public boolean isReplace() {
		return replace;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(version);
		out.writeString(latestChanges);
		out.writeParcelable(profileSettingsUri, flags);
		out.writeStringList(settingsTypeKeyList);
		out.writeInt(replace ? 1 : 0);
	}

	private void readFromParcel(Parcel in) {
		version = in.readInt();
		latestChanges = in.readString();
		profileSettingsUri = in.readParcelable(Uri.class.getClassLoader());
		in.readStringList(settingsTypeKeyList);
		replace = in.readInt() == 1;
	}

	@Override
	public int describeContents() {
		return 0;
	}
}