package net.osmand.aidlapi.info;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;

/**
 * Map widgets of a profile as in Configure screen: the ones on the screen with their panel, page and order,
 * then the ones that can be added.
 */
public class AMapWidgetsLayout extends AidlParams {

	private String appModeKey;
	private boolean separateLayouts;
	private ArrayList<AMapWidgetInfo> widgets;

	public AMapWidgetsLayout(String appModeKey, boolean separateLayouts, ArrayList<AMapWidgetInfo> widgets) {
		this.appModeKey = appModeKey;
		this.separateLayouts = separateLayouts;
		this.widgets = widgets;
	}

	public AMapWidgetsLayout(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AMapWidgetsLayout> CREATOR = new Creator<AMapWidgetsLayout>() {
		@Override
		public AMapWidgetsLayout createFromParcel(Parcel in) {
			return new AMapWidgetsLayout(in);
		}

		@Override
		public AMapWidgetsLayout[] newArray(int size) {
			return new AMapWidgetsLayout[size];
		}
	};

	/**
	 * @return profile key
	 */
	public String getAppModeKey() {
		return appModeKey;
	}

	/**
	 * @return whether the profile keeps separate layouts for portrait and landscape; the current orientation is used
	 */
	public boolean isSeparateLayouts() {
		return separateLayouts;
	}

	/**
	 * @return widgets on the screen sorted by panel, page and order, then the widgets that can be added
	 */
	public ArrayList<AMapWidgetInfo> getWidgets() {
		return widgets;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("appModeKey", appModeKey);
		bundle.putBoolean("separateLayouts", separateLayouts);
		bundle.putParcelableArrayList("widgets", widgets);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		appModeKey = bundle.getString("appModeKey");
		separateLayouts = bundle.getBoolean("separateLayouts");
		bundle.setClassLoader(AMapWidgetInfo.class.getClassLoader());
		widgets = bundle.getParcelableArrayList("widgets");
	}
}
