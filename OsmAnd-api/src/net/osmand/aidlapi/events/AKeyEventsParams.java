package net.osmand.aidlapi.events;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;

public class AKeyEventsParams extends AidlParams {

	private long callbackId = -1L;
	private boolean subscribeToUpdates = true;
	private ArrayList<Integer> keyEventList;

	public AKeyEventsParams() {
	}

	protected AKeyEventsParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Parcelable.Creator<AKeyEventsParams> CREATOR = new Parcelable.Creator<AKeyEventsParams>() {
		@Override
		public AKeyEventsParams createFromParcel(Parcel in) {
			return new AKeyEventsParams(in);
		}

		@Override
		public AKeyEventsParams[] newArray(int size) {
			return new AKeyEventsParams[size];
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

	public void setKeyEventList(ArrayList<Integer> keyEventList) {
		this.keyEventList = keyEventList;
	}

	public ArrayList<Integer> getKeyEventList() {
		return keyEventList;
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		callbackId = bundle.getLong("callbackId");
		subscribeToUpdates = bundle.getBoolean("subscribeToUpdates");
		keyEventList = bundle.getIntegerArrayList("keyEventList");
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putLong("callbackId", callbackId);
		bundle.putBoolean("subscribeToUpdates", subscribeToUpdates);
		bundle.putIntegerArrayList("keyEventList", keyEventList);
	}
}