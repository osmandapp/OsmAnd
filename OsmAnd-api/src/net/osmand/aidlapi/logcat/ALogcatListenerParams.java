package net.osmand.aidlapi.logcat;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class ALogcatListenerParams extends AidlParams {

	private static final String ARG_CALLBACK_ID = "callback_id";
	private static final String ARG_SUBSCRIBED = "subscribed";
	private static final String ARG_FILTER_LEVEL = "filter_level";

	private boolean subscribeToUpdates = true;
	private long callbackId = -1L;
	private String filterLevel;

	public static final Creator<ALogcatListenerParams> CREATOR = new Creator<ALogcatListenerParams>() {
		@Override
		public ALogcatListenerParams createFromParcel(Parcel in) {
			return new ALogcatListenerParams(in);
		}

		@Override
		public ALogcatListenerParams[] newArray(int size) {
			return new ALogcatListenerParams[size];
		}
	};

	public ALogcatListenerParams() { }

	protected ALogcatListenerParams(Parcel in) {
		readFromParcel(in);
	}

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

	public String getFilterLevel() {
		return filterLevel;
	}

	public void setFilterLevel(String filterLevel) {
		this.filterLevel = filterLevel;
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		callbackId = bundle.getLong(ARG_CALLBACK_ID);
		subscribeToUpdates = bundle.getBoolean(ARG_SUBSCRIBED);
		filterLevel = bundle.getString(ARG_FILTER_LEVEL);
	}

	@Override
	protected void writeToBundle(Bundle bundle) {
		bundle.putLong(ARG_CALLBACK_ID, callbackId);
		bundle.putBoolean(ARG_SUBSCRIBED, subscribeToUpdates);
		bundle.putString(ARG_FILTER_LEVEL, filterLevel);
	}
}
