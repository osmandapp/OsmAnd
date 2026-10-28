package net.osmand.aidl.navigation;

import android.os.Parcel;
import android.os.Parcelable;

public class PauseNavigationParams implements Parcelable {

	public PauseNavigationParams() {
	}

	public PauseNavigationParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<PauseNavigationParams> CREATOR = new Creator<PauseNavigationParams>() {
		@Override
		public PauseNavigationParams createFromParcel(Parcel in) {
			return new PauseNavigationParams(in);
		}

		@Override
		public PauseNavigationParams[] newArray(int size) {
			return new PauseNavigationParams[size];
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
