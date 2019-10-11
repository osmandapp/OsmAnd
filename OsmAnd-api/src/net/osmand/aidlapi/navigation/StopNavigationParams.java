package net.osmand.aidlapi.navigation;

import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class StopNavigationParams extends AidlParams {

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
}