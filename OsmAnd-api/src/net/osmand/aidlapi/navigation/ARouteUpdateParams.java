package net.osmand.aidlapi.navigation;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class ARouteUpdateParams extends AidlParams {

	private boolean subscribeToUpdates = true;
	private long callbackId = -1L;

	public ARouteUpdateParams() {
	}

	protected ARouteUpdateParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ARouteUpdateParams> CREATOR = new Creator<ARouteUpdateParams>() {
		@Override
		public ARouteUpdateParams createFromParcel(Parcel in) {
			return new ARouteUpdateParams(in);
		}

		@Override
		public ARouteUpdateParams[] newArray(int size) {
			return new ARouteUpdateParams[size];
		}
	};

	public long getCallbackId() {
		return callbackId;
	}

	public void setCallbackId(long callbackId) {
		this.callbackId = callbackId;
	}

	public boolean isSubscribeToUpdates() {
		return subscribeToUpdates;
	}

	public void setSubscribeToUpdates(boolean subscribeToUpdates) {
		this.subscribeToUpdates = subscribeToUpdates;
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		callbackId = bundle.getLong("callbackId", -1L);
		subscribeToUpdates = bundle.getBoolean("subscribeToUpdates", true);
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putLong("callbackId", callbackId);
		bundle.putBoolean("subscribeToUpdates", subscribeToUpdates);
	}
}
