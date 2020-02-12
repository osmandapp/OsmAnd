package net.osmand.aidlapi.plugins;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class PluginParams extends AidlParams {

	private String pluginId;
	private int newState; //0- off, 1 - on

	public PluginParams(String pluginId, int newState) {
		this.pluginId = pluginId;
		this.newState = newState;
	}

	public String getPluginId() {
		return pluginId;
	}

	public int getNewState() {
		return newState;
	}

	protected PluginParams(Parcel in) {
		readFromParcel(in);
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("pluginId", pluginId);
		bundle.putInt("newState", newState);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		pluginId = bundle.getString("pluginId");
		newState = bundle.getInt("newState");
	}

	public static final Creator<PluginParams> CREATOR = new Creator<PluginParams>() {
		@Override
		public PluginParams createFromParcel(Parcel in) {
			return new PluginParams(in);
		}

		@Override
		public PluginParams[] newArray(int size) {
			return new PluginParams[size];
		}
	};
}