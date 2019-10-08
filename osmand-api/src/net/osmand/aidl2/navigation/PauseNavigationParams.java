package net.osmand.aidl2.navigation;

import android.os.Parcel;

import net.osmand.aidl2.AidlParams;

public class PauseNavigationParams extends AidlParams {

	public PauseNavigationParams() {

	}

	public PauseNavigationParams(Parcel in) {
		super(in);
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
}