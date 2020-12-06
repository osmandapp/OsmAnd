package net.osmand.aidlapi.customization;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;


public class CustomPluginParams extends AidlParams {

	public static final String PLUGIN_ID_KEY = "plugin_id";

	private String pluginId;

	public CustomPluginParams(String pluginId) {
		this.pluginId = pluginId;
	}

	public CustomPluginParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<CustomPluginParams> CREATOR = new Creator<CustomPluginParams>() {
		@Override
		public CustomPluginParams createFromParcel(Parcel in) {
			return new CustomPluginParams(in);
		}

		@Override
		public CustomPluginParams[] newArray(int size) {
			return new CustomPluginParams[size];
		}
	};

	public String getPluginId() {
		return pluginId;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString(PLUGIN_ID_KEY, pluginId);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		pluginId = bundle.getString(PLUGIN_ID_KEY);
	}
}