package net.osmand.aidl.navigation;

import android.os.Parcel;
import android.os.Parcelable;

public class UnmuteNavigationParams implements Parcelable {

	public UnmuteNavigationParams() {
	}

	public UnmuteNavigationParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<UnmuteNavigationParams> CREATOR = new Creator<UnmuteNavigationParams>() {
		@Override
		public UnmuteNavigationParams createFromParcel(Parcel in) {
			return new UnmuteNavigationParams(in);
		}

		@Override
		public UnmuteNavigationParams[] newArray(int size) {
			return new UnmuteNavigationParams[size];
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
