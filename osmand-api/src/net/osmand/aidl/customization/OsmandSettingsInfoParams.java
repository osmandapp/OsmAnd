package net.osmand.aidl.customization;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class OsmandSettingsInfoParams implements Parcelable {

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
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(sharedPreferencesName);
	}

	@SuppressLint("ParcelClassLoader")
	private void readFromParcel(Parcel in) {
		sharedPreferencesName = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}
}