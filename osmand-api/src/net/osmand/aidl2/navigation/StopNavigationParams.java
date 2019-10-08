package net.osmand.aidl2.navigation;

import android.os.Parcel;

import net.osmand.aidl2.AidlParams;

public class StopNavigationParams extends AidlParams {

	public StopNavigationParams() {

	}

	public StopNavigationParams(Parcel in) {
		super(in);
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
}