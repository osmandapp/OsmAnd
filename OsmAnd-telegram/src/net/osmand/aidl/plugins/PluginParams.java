package net.osmand.aidl.plugins;

import android.os.Parcel;
import android.os.Parcelable;

public class PluginParams implements Parcelable {

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
		pluginId = in.readString();
		newState = in.readInt();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(pluginId);
		dest.writeInt(newState);
	}

	@Override
	public int describeContents() {
		return 0;
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
