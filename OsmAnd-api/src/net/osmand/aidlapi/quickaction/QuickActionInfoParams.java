package net.osmand.aidlapi.quickaction;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class QuickActionInfoParams extends AidlParams {

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
	public void writeToBundle(Bundle bundle) {
		bundle.putInt("actionId", actionId);
		bundle.putString("name", name);
		bundle.putString("actionType", actionType);
		bundle.putString("params", params);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		actionId = bundle.getInt("actionNumber");
		name = bundle.getString("name");
		actionType = bundle.getString("actionType");
		params = bundle.getString("params");
	}
}