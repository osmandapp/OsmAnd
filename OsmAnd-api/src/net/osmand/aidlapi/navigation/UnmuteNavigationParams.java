package net.osmand.aidlapi.navigation;

import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class UnmuteNavigationParams extends AidlParams {

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
}