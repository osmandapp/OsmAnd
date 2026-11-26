package net.osmand.aidlapi.info;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

import java.util.ArrayList;

/**
 * New content of one widget panel, as Configure screen saves it: pages (rows for the top and bottom panels)
 * of widget ids. Widgets of the panel that are not listed are removed from the screen.
 */
public class SetMapWidgetsPanelParams extends AidlParams {

	private String appModeKey;
	private String panel;
	private ArrayList<String> pages;

	/**
	 * @param pages each page is a comma separated list of widget ids. An id of a widget on this panel keeps it;
	 *              a widget type (e.g. speed) adds a new widget of that type, unless the type's first widget
	 *              is already on this page list or another panel - then a second widget of the type is created.
	 */
	public SetMapWidgetsPanelParams(String appModeKey, String panel, ArrayList<String> pages) {
		this.appModeKey = appModeKey;
		this.panel = panel;
		this.pages = pages;
	}

	public SetMapWidgetsPanelParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<SetMapWidgetsPanelParams> CREATOR = new Creator<SetMapWidgetsPanelParams>() {
		@Override
		public SetMapWidgetsPanelParams createFromParcel(Parcel in) {
			return new SetMapWidgetsPanelParams(in);
		}

		@Override
		public SetMapWidgetsPanelParams[] newArray(int size) {
			return new SetMapWidgetsPanelParams[size];
		}
	};

	/**
	 * @return profile key; empty for the current profile
	 */
	public String getAppModeKey() {
		return appModeKey;
	}

	/**
	 * @return LEFT, RIGHT, TOP or BOTTOM
	 */
	public String getPanel() {
		return panel;
	}

	/**
	 * @return pages, each a comma separated list of widget ids or types
	 */
	public ArrayList<String> getPages() {
		return pages;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("appModeKey", appModeKey);
		bundle.putString("panel", panel);
		bundle.putStringArrayList("pages", pages);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		appModeKey = bundle.getString("appModeKey");
		panel = bundle.getString("panel");
		pages = bundle.getStringArrayList("pages");
	}
}
