package net.osmand.aidlapi.customization;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.Nullable;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;
import java.util.List;

public class MapMarginsParams extends AidlParams {

	public static final String LEFT_MARGIN_KEY = "leftMargin";
	public static final String TOP_MARGIN_KEY = "topMargin";
	public static final String RIGHT_MARGIN_KEY = "rightMargin";
	public static final String BOTTOM_MARGIN_KEY = "bottomMargin";
	public static final String APP_MODES_KEYS_KEY = "appModesKeys";
	private ArrayList<String> appModesKeys = new ArrayList<>();
	private int leftMargin;
	private int topMargin;
	private int rightMargin;
	private int bottomMargin;

	public MapMarginsParams(int leftMargin, int topMargin, int rightMargin, int bottomMargin,
	                        @Nullable List<String> appModesKeys) {
		if (appModesKeys != null) {
			this.appModesKeys.addAll(appModesKeys);
		}
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

	public List<String> getAppModesKeys() {
		return appModesKeys;
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
		bundle.putInt(LEFT_MARGIN_KEY, leftMargin);
		bundle.putInt(TOP_MARGIN_KEY, topMargin);
		bundle.putInt(RIGHT_MARGIN_KEY, rightMargin);
		bundle.putInt(BOTTOM_MARGIN_KEY, bottomMargin);
		bundle.putStringArrayList(APP_MODES_KEYS_KEY, appModesKeys);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		leftMargin = bundle.getInt(LEFT_MARGIN_KEY);
		topMargin = bundle.getInt(TOP_MARGIN_KEY);
		rightMargin = bundle.getInt(RIGHT_MARGIN_KEY);
		bottomMargin = bundle.getInt(BOTTOM_MARGIN_KEY);
		appModesKeys = bundle.getStringArrayList(APP_MODES_KEYS_KEY);
	}
}