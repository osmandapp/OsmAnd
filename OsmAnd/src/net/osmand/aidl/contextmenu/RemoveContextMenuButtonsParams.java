package net.osmand.aidl.contextmenu;

import android.os.Parcel;
import android.os.Parcelable;

public class RemoveContextMenuButtonsParams implements Parcelable {

	private String paramsId;

	private long callbackId = -1L;

	public RemoveContextMenuButtonsParams(String paramsId, long callbackId) {
		this.paramsId = paramsId;
		this.callbackId = callbackId;
	}

	public RemoveContextMenuButtonsParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<RemoveContextMenuButtonsParams> CREATOR = new
			Creator<RemoveContextMenuButtonsParams>() {
				public RemoveContextMenuButtonsParams createFromParcel(Parcel in) {
					return new RemoveContextMenuButtonsParams(in);
				}

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

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(paramsId);
		dest.writeLong(callbackId);
	}

	private void readFromParcel(Parcel in) {
		paramsId = in.readString();
		callbackId = in.readLong();
	}

	public int describeContents() {
		return 0;
	}
}
