package net.osmand.aidlapi.info;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

/**
 * Map widget as the user sees it.
 */
public class AMapWidgetValue extends AidlParams {

	private String id;
	private String title;
	private String value;
	private String panel;
	private boolean visible;

	public AMapWidgetValue(String id, String title, String value, String panel, boolean visible) {
		this.id = id;
		this.title = title;
		this.value = value;
		this.panel = panel;
		this.visible = visible;
	}

	public AMapWidgetValue(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AMapWidgetValue> CREATOR = new Creator<AMapWidgetValue>() {
		@Override
		public AMapWidgetValue createFromParcel(Parcel in) {
			return new AMapWidgetValue(in);
		}

		@Override
		public AMapWidgetValue[] newArray(int size) {
			return new AMapWidgetValue[size];
		}
	};

	/**
	 * @return widget id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return widget name
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return shown text, e.g. 12.5 km/h
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @return LEFT, RIGHT, TOP or BOTTOM
	 */
	public String getPanel() {
		return panel;
	}

	/**
	 * @return whether the widget is on the screen now
	 */
	public boolean isVisible() {
		return visible;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("id", id);
		bundle.putString("title", title);
		bundle.putString("value", value);
		bundle.putString("panel", panel);
		bundle.putBoolean("visible", visible);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		id = bundle.getString("id");
		title = bundle.getString("title");
		value = bundle.getString("value");
		panel = bundle.getString("panel");
		visible = bundle.getBoolean("visible");
	}
}
