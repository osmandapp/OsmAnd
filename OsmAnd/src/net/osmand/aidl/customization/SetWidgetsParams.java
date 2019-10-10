package net.osmand.aidl.customization;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SetWidgetsParams implements Parcelable {

	private String widgetKey;
	private List<String> appModesKeys;

	public SetWidgetsParams(String widgetKey, @Nullable List<String> appModesKeys) {
		this.widgetKey = widgetKey;
		this.appModesKeys = appModesKeys;
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
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(widgetKey);
		writeStringList(out, appModesKeys);
	}

	private void readFromParcel(Parcel in) {
		widgetKey = in.readString();
		appModesKeys = readStringList(in);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	private void writeStringList(Parcel out, List<String> val) {
		if (val == null) {
			out.writeInt(-1);
			return;
		}
		int N = val.size();
		int i = 0;
		out.writeInt(N);
		while (i < N) {
			out.writeString(val.get(i));
			i++;
		}
	}

	private List<String> readStringList(Parcel in) {
		List<String> list = new ArrayList<>();
		int M = list.size();
		int N = in.readInt();
		if (N == -1) {
			return null;
		}
		int i = 0;
		for (; i < M && i < N; i++) {
			list.set(i, in.readString());
		}
		for (; i < N; i++) {
			list.add(in.readString());
		}
		for (; i < M; i++) {
			list.remove(N);
		}
		return list;
	}
}
