package net.osmand.aidlapi.customization;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class MapMarginsParams extends AidlParams {

	private String appModeKey;
	private int leftMargin;
	private int topMargin;
	private int rightMargin;
	private int bottomMargin;

	public MapMarginsParams(String appModeKey, int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
		this.appModeKey = appModeKey;
		this.leftMargin = leftMargin;
		this.topMargin = topMargin;
		this.rightMargin = rightMargin;
		this.bottomMargin = bottomMargin;
	}

	public MapMarginsParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<MapMarginsParams> CREATOR = new Creator<MapMarginsParams>() {
		@Override
		public MapMarginsParams createFromParcel(Parcel in) {
			return new MapMarginsParams(in);
		}

		@Override
		public MapMarginsParams[] newArray(int size) {
			return new MapMarginsParams[size];
		}
	};

	public String getAppModeKey() {
		return appModeKey;
	}

	public int getLeftMargin() {
		return leftMargin;
	}

	public int getTopMargin() {
		return topMargin;
	}

	public int getRightMargin() {
		return rightMargin;
	}

	public int getBottomMargin() {
		return bottomMargin;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("appModeKey", appModeKey);
		bundle.putInt("leftMargin", leftMargin);
		bundle.putInt("topMargin", topMargin);
		bundle.putInt("rightMargin", rightMargin);
		bundle.putInt("bottomMargin", bottomMargin);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		appModeKey = bundle.getString("appModeKey");
		leftMargin = bundle.getInt("leftMargin");
		topMargin = bundle.getInt("topMargin");
		rightMargin = bundle.getInt("rightMargin");
		bottomMargin = bundle.getInt("bottomMargin");
	}
}