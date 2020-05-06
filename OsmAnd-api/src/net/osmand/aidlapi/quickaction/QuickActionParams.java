package net.osmand.aidlapi.quickaction;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class QuickActionParams extends AidlParams {

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
	public void writeToBundle(Bundle bundle) {
		bundle.putInt("actionNumber", actionNumber);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		actionNumber = bundle.getInt("actionNumber");
	}
}