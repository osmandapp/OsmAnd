package net.osmand.aidlapi.exit;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class ExitAppParams extends AidlParams {

	private boolean shouldRestart;

	public ExitAppParams(boolean shouldRestart) {
		this.shouldRestart = shouldRestart;
	}

	public ExitAppParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ExitAppParams> CREATOR = new Creator<ExitAppParams>() {
		@Override
		public ExitAppParams createFromParcel(Parcel in) {
			return new ExitAppParams(in);
		}

		@Override
		public ExitAppParams[] newArray(int size) {
			return new ExitAppParams[size];
		}
	};

	public boolean shouldRestart() {
		return shouldRestart;
	}

	public void setShouldRestart(boolean shouldRestart) {
		this.shouldRestart = shouldRestart;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putBoolean("shouldRestart", shouldRestart);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		shouldRestart = bundle.getBoolean("shouldRestart");
	}
}