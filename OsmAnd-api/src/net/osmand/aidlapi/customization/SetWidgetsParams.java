package net.osmand.aidlapi.customization;

import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.Nullable;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;
import java.util.List;

public class SetWidgetsParams extends AidlParams {

	private String widgetKey;
	private ArrayList<String> appModesKeys = new ArrayList<>();

	public SetWidgetsParams(String widgetKey, @Nullable List<String> appModesKeys) {
		this.widgetKey = widgetKey;
		if (appModesKeys != null) {
			this.appModesKeys.addAll(appModesKeys);
		}
	}

	public SetWidgetsParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<SetWidgetsParams> CREATOR = new Creator<SetWidgetsParams>() {
		@Override
		public SetWidgetsParams createFromParcel(Parcel in) {
			return new SetWidgetsParams(in);
		}

		@Override
		public SetWidgetsParams[] newArray(int size) {
			return new SetWidgetsParams[size];
		}
	};

	public String getWidgetKey() {
		return widgetKey;
	}

	public List<String> getAppModesKeys() {
		return appModesKeys;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("widgetKey", widgetKey);
		bundle.putStringArrayList("appModesKeys", appModesKeys);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		widgetKey = bundle.getString("widgetKey");
		appModesKeys = bundle.getStringArrayList("appModesKeys");
	}
}