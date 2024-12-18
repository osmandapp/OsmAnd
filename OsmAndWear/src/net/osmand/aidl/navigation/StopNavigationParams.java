package net.osmand.aidl.navigation;

import android.os.Parcel;
import android.os.Parcelable;

public class StopNavigationParams implements Parcelable {

	public StopNavigationParams() {
	}

	public StopNavigationParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<StopNavigationParams> CREATOR = new Creator<StopNavigationParams>() {
		@Override
		public StopNavigationParams createFromParcel(Parcel in) {
			return new StopNavigationParams(in);
		}

		@Override
		public StopNavigationParams[] newArray(int size) {
			return new StopNavigationParams[size];
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
