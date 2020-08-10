package net.osmand.aidlapi.lock;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class SetLockStateParams extends AidlParams {

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

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putBoolean("lock", this.lock);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		lock = bundle.getBoolean("lock");
	}
}