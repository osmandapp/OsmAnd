package net.osmand.aidl.navigation;

import android.os.Parcel;
import android.os.Parcelable;

public class ResumeNavigationParams implements Parcelable {

	public ResumeNavigationParams() {
	}

	public ResumeNavigationParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ResumeNavigationParams> CREATOR = new Creator<ResumeNavigationParams>() {
		@Override
		public ResumeNavigationParams createFromParcel(Parcel in) {
			return new ResumeNavigationParams(in);
		}

		@Override
		public ResumeNavigationParams[] newArray(int size) {
			return new ResumeNavigationParams[size];
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
