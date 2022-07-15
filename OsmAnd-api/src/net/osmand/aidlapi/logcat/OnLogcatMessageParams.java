package net.osmand.aidlapi.logcat;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;
import java.util.List;

public class OnLogcatMessageParams extends AidlParams {

	private static final String ARG_FILTER_LEVEL = "filter_level";
	private static final String ARG_LOGS = "logs";

	private String filterLevel = "";
	private ArrayList<String> logs = new ArrayList<>();

	public static final Creator<OnLogcatMessageParams> CREATOR = new Creator<OnLogcatMessageParams>() {
		@Override
		public OnLogcatMessageParams createFromParcel(Parcel in) {
			return new OnLogcatMessageParams(in);
		}

		@Override
		public OnLogcatMessageParams[] newArray(int size) {
			return new OnLogcatMessageParams[size];
		}
	};

	public OnLogcatMessageParams() { }

	public OnLogcatMessageParams(String filterLevel, List<String> logs) {
		this.filterLevel = filterLevel;
		if (logs != null) {
			this.logs.addAll(logs);
		}
	}

	protected OnLogcatMessageParams(Parcel in) {
		readFromParcel(in);
	}

	@Override
	protected void writeToBundle(Bundle bundle) {
		bundle.putString(ARG_FILTER_LEVEL, filterLevel);
		bundle.putStringArrayList(ARG_LOGS, logs);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		filterLevel = bundle.getString(ARG_FILTER_LEVEL);
		logs = bundle.getStringArrayList(ARG_LOGS);
		if (logs == null) {
			logs = new ArrayList<>();
		}
	}

	public String getFilterLevel() {
		return filterLevel;
	}

	public List<String> getLogs() {
		return logs;
	}
}