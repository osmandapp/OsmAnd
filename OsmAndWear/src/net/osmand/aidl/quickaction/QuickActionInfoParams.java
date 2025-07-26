package net.osmand.aidl.quickaction;

import android.os.Parcel;
import android.os.Parcelable;

public class QuickActionInfoParams implements Parcelable {

	private int actionId;
	private String name;
	private String actionType;
	private String params;

	public QuickActionInfoParams(int actionId, String name, String actionType, String params) {
		this.actionId = actionId;
		this.name = name;
		this.actionType = actionType;
		this.params = params;
	}

	public QuickActionInfoParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<QuickActionInfoParams> CREATOR = new Creator<QuickActionInfoParams>() {
		@Override
		public QuickActionInfoParams createFromParcel(Parcel in) {
			return new QuickActionInfoParams(in);
		}

		@Override
		public QuickActionInfoParams[] newArray(int size) {
			return new QuickActionInfoParams[size];
		}
	};

	public int getActionId() {
		return actionId;
	}

	public String getName() {
		return name;
	}

	public String getActionType() {
		return actionType;
	}

	public String getParams() {
		return params;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(actionId);
		out.writeString(name);
		out.writeString(actionType);
		out.writeString(params);
	}

	private void readFromParcel(Parcel in) {
		actionId = in.readInt();
		name = in.readString();
		actionType = in.readString();
		params = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}
}