package net.osmand.aidlapi;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public abstract class AidlParams implements Parcelable {

	@Override
	public final void writeToParcel(Parcel dest, int flags) {
		Bundle bundle = new Bundle();
		writeToBundle(bundle);
		dest.writeBundle(bundle);
	}

	public final void readFromParcel(Parcel in) {
		Bundle bundle = in.readBundle(getClass().getClassLoader());
		if (bundle != null) {
			readFromBundle(bundle);
		}
	}

	protected void writeToBundle(Bundle bundle) {

	}

	protected void readFromBundle(Bundle bundle) {

	}

	@Override
	public int describeContents() {
		return 0;
	}
}