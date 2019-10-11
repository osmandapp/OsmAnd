package net.osmand.aidlapi.navigation;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class ANavigationVoiceRouterMessageParams extends AidlParams {

	private long callbackId = -1L;
	private boolean subscribeToUpdates = true;

	public ANavigationVoiceRouterMessageParams() {

	}

	protected ANavigationVoiceRouterMessageParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ANavigationVoiceRouterMessageParams> CREATOR = new Creator<ANavigationVoiceRouterMessageParams>() {
		@Override
		public ANavigationVoiceRouterMessageParams createFromParcel(Parcel in) {
			return new ANavigationVoiceRouterMessageParams(in);
		}

		@Override
		public ANavigationVoiceRouterMessageParams[] newArray(int size) {
			return new ANavigationVoiceRouterMessageParams[size];
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