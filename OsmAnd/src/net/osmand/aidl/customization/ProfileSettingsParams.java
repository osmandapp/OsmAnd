package net.osmand.aidl.customization;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class ProfileSettingsParams implements Parcelable {

	private Uri profileSettingsUri;

	public ProfileSettingsParams(Uri profileSettingsUri) {
		this.profileSettingsUri = profileSettingsUri;
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

	public Uri getProfileSettingsUri() {
		return profileSettingsUri;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeParcelable(profileSettingsUri, flags);
	}

	private void readFromParcel(Parcel in) {
		profileSettingsUri = in.readParcelable(Uri.class.getClassLoader());
	}

	@Override
	public int describeContents() {
		return 0;
	}
}