package net.osmand.aidl.customization;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class OsmandSettingsParams implements Parcelable {

	private String sharedPreferencesName;
	private Bundle bundle;

	public OsmandSettingsParams(@NonNull String sharedPreferencesName, @Nullable Bundle bundle) {
		this.sharedPreferencesName = sharedPreferencesName;
		this.bundle = bundle;
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
		return bundle;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(sharedPreferencesName);
		out.writeBundle(bundle);
	}

	@SuppressLint("ParcelClassLoader")
	private void readFromParcel(Parcel in) {
		sharedPreferencesName = in.readString();
		bundle = in.readBundle();
	}

	@Override
	public int describeContents() {
		return 0;
	}
}
