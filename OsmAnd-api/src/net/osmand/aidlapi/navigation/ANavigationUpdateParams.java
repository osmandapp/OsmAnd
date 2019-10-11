package net.osmand.aidlapi.navigation;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class ANavigationUpdateParams extends AidlParams {

	private boolean subscribeToUpdates = true;
	private long callbackId = -1L;

	public ANavigationUpdateParams() {

	}

	protected ANavigationUpdateParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ANavigationUpdateParams> CREATOR = new Creator<ANavigationUpdateParams>() {
		@Override
		public ANavigationUpdateParams createFromParcel(Parcel in) {
			return new ANavigationUpdateParams(in);
		}

		@Override
		public ANavigationUpdateParams[] newArray(int size) {
			return new ANavigationUpdateParams[size];
		}
	};

	public long getCallbackId() {
		return callbackId;
	}

	public void setCallbackId(long callbackId) {
		this.callbackId = callbackId;
	}

	public void setSubscribeToUpdates(boolean subscribeToUpdates) {
		this.subscribeToUpdates = subscribeToUpdates;
	}

	public boolean isSubscribeToUpdates() {
		return subscribeToUpdates;
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		callbackId = bundle.getLong("callbackId");
		subscribeToUpdates = bundle.getBoolean("subscribeToUpdates");
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putLong("callbackId", callbackId);
		bundle.putBoolean("subscribeToUpdates", subscribeToUpdates);
	}
}