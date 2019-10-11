package net.osmand.aidlapi.contextmenu;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class RemoveContextMenuButtonsParams extends AidlParams {

	private String paramsId;
	private long callbackId = -1L;

	public RemoveContextMenuButtonsParams(String paramsId, long callbackId) {
		this.paramsId = paramsId;
		this.callbackId = callbackId;
	}

	public RemoveContextMenuButtonsParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<RemoveContextMenuButtonsParams> CREATOR = new Creator<RemoveContextMenuButtonsParams>() {
		@Override
		public RemoveContextMenuButtonsParams createFromParcel(Parcel in) {
			return new RemoveContextMenuButtonsParams(in);
		}

		@Override
		public RemoveContextMenuButtonsParams[] newArray(int size) {
			return new RemoveContextMenuButtonsParams[size];
		}
	};

	public String getParamsId() {
		return paramsId;
	}

	public long getCallbackId() {
		return callbackId;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("paramsId", paramsId);
		bundle.putLong("callbackId", callbackId);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		paramsId = bundle.getString("paramsId");
		callbackId = bundle.getLong("callbackId");
	}
}