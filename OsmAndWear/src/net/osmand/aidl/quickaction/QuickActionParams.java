package net.osmand.aidl.quickaction;

import android.os.Parcel;
import android.os.Parcelable;

public class QuickActionParams implements Parcelable {

	private int actionNumber;

	public QuickActionParams(int actionNumber) {
		this.actionNumber = actionNumber;
	}

	public QuickActionParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<QuickActionParams> CREATOR = new Creator<QuickActionParams>() {
		@Override
		public QuickActionParams createFromParcel(Parcel in) {
			return new QuickActionParams(in);
		}

		@Override
		public QuickActionParams[] newArray(int size) {
			return new QuickActionParams[size];
		}
	};

	public int getActionNumber() {
		return actionNumber;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(actionNumber);
	}

	private void readFromParcel(Parcel in) {
		actionNumber = in.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}
}