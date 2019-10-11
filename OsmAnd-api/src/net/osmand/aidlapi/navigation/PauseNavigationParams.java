package net.osmand.aidlapi.navigation;

import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class PauseNavigationParams extends AidlParams {

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
}