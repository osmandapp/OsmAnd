package net.osmand.aidlapi.info;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

/**
 * Map widget in the screen layout of a profile: where it stands or, if it is not on the screen, that it can be added.
 */
public class AMapWidgetInfo extends AidlParams {

	private String id;
	private String type;
	private String title;
	private String panel;
	private int page;
	private int order;
	private boolean enabled;
	private boolean purchased;

	public AMapWidgetInfo(String id, String type, String title, String panel, int page, int order,
	                      boolean enabled, boolean purchased) {
		this.id = id;
		this.type = type;
		this.title = title;
		this.panel = panel;
		this.page = page;
		this.order = order;
		this.enabled = enabled;
		this.purchased = purchased;
	}

	public AMapWidgetInfo(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AMapWidgetInfo> CREATOR = new Creator<AMapWidgetInfo>() {
		@Override
		public AMapWidgetInfo createFromParcel(Parcel in) {
			return new AMapWidgetInfo(in);
		}

		@Override
		public AMapWidgetInfo[] newArray(int size) {
			return new AMapWidgetInfo[size];
		}
	};

	/**
	 * @return widget id, e.g. speed or speed__1653912678220 for a second speed widget
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return widget type, the id of its first instance, e.g. speed
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return widget name
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return LEFT, RIGHT, TOP or BOTTOM; for a widget that is not on the screen, the panel it is added to by default
	 */
	public String getPanel() {
		return panel;
	}

	/**
	 * @return page of the side panel or row of the top and bottom panel, from 0; -1 if the widget is not on the screen
	 */
	public int getPage() {
		return page;
	}

	/**
	 * @return position on the page, from 0
	 */
	public int getOrder() {
		return order;
	}

	/**
	 * @return whether the widget is on the screen layout
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @return false for a paid widget that is not purchased
	 */
	public boolean isPurchased() {
		return purchased;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("id", id);
		bundle.putString("type", type);
		bundle.putString("title", title);
		bundle.putString("panel", panel);
		bundle.putInt("page", page);
		bundle.putInt("order", order);
		bundle.putBoolean("enabled", enabled);
		bundle.putBoolean("purchased", purchased);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		id = bundle.getString("id");
		type = bundle.getString("type");
		title = bundle.getString("title");
		panel = bundle.getString("panel");
		page = bundle.getInt("page");
		order = bundle.getInt("order");
		enabled = bundle.getBoolean("enabled");
		purchased = bundle.getBoolean("purchased");
	}
}
