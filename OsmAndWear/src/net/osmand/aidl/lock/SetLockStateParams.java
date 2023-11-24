package net.osmand.aidl.lock;

import android.os.Parcel;
import android.os.Parcelable;

public class SetLockStateParams implements Parcelable {

	private boolean lock;

	public SetLockStateParams(boolean lock) {
		this.lock = lock;
	}

	public SetLockStateParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<SetLockStateParams> CREATOR = new Creator<SetLockStateParams>() {
		@Override
		public SetLockStateParams createFromParcel(Parcel in) {
			return new SetLockStateParams(in);
		}

		@Override
		public SetLockStateParams[] newArray(int size) {
			return new SetLockStateParams[size];
		}
	};

	public boolean getLockState() {
		return lock;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeByte((byte) (lock ? 1 : 0));
	}

	private void readFromParcel(Parcel in) {
		lock = in.readByte() == 1;
	}

	public int describeContents() {
		return 0;
	}
}