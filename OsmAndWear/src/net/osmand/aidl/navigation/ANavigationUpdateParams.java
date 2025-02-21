package net.osmand.aidl.navigation;

import android.os.Parcel;
import android.os.Parcelable;

public class ANavigationUpdateParams implements Parcelable {

	private boolean subscribeToUpdates = true;
	private long callbackId = -1L;

	public ANavigationUpdateParams() {
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

	protected ANavigationUpdateParams(Parcel in) {
		callbackId = in.readLong();
		subscribeToUpdates = in.readByte() != 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(callbackId);
		dest.writeByte((byte) (subscribeToUpdates ? 1 : 0));
	}

	@Override
	public int describeContents() {
		return 0;
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
}
