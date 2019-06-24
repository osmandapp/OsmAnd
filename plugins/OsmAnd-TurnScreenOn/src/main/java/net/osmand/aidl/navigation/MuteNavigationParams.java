package net.osmand.aidl.navigation;

import android.os.Parcel;
import android.os.Parcelable;

public class MuteNavigationParams implements Parcelable {

	public MuteNavigationParams() {
	}

	public MuteNavigationParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<MuteNavigationParams> CREATOR = new Creator<MuteNavigationParams>() {
		@Override
		public MuteNavigationParams createFromParcel(Parcel in) {
			return new MuteNavigationParams(in);
		}

		@Override
		public MuteNavigationParams[] newArray(int size) {
			return new MuteNavigationParams[size];
		}
	};

	@Override
	public void writeToParcel(Parcel out, int flags) {
	}

	private void readFromParcel(Parcel in) {
	}

	@Override
	public int describeContents() {
		return 0;
	}
}
