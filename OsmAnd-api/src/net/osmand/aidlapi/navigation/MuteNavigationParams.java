package net.osmand.aidlapi.navigation;

import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class MuteNavigationParams extends AidlParams {

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
}