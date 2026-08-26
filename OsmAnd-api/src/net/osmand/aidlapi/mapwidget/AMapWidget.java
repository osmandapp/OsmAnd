package net.osmand.aidlapi.mapwidget;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class AMapWidget extends AidlParams {
	private String id;
	private String menuIconName;
	private String menuTitle;
	private String lightIconName;
	private String darkIconName;
	private String text;
	private String description;
	private int order;
	private boolean rightPanelByDefault = true;
	private Intent intentOnClick;

	private String groupId;

	private String menuIconUri;
	private String lightIconUri;
	private String darkIconUri;

	public AMapWidget(String id, String menuIconName, String menuTitle,
	                  String lightIconName, String darkIconName, String text, String description,
	                  int order, Intent intentOnClick) {
		this.id = id;
		this.menuIconName = menuIconName;
		this.menuTitle = menuTitle;
		this.lightIconName = lightIconName;
		this.darkIconName = darkIconName;
		this.text = text;
		this.description = description;
		this.order = order;
		this.intentOnClick = intentOnClick;
	}

	public AMapWidget(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AMapWidget> CREATOR = new Creator<AMapWidget>() {
		@Override
		public AMapWidget createFromParcel(Parcel in) {
			return new AMapWidget(in);
		}

		@Override
		public AMapWidget[] newArray(int size) {
			return new AMapWidget[size];
		}
	};

	public String getId() {
		return id;
	}

	public String getMenuIconName() {
		return menuIconName;
	}

	public String getMenuTitle() {
		return menuTitle;
	}

	public String getLightIconName() {
		return lightIconName;
	}

	public String getDarkIconName() {
		return darkIconName;
	}

	public String getText() {
		return text;
	}

	public String getDescription() {
		return description;
	}

	public int getOrder() {
		return order;
	}

	public boolean isRightPanelByDefault() {
		return rightPanelByDefault;
	}

	public void setRightPanelByDefault(boolean rightPanelByDefault) {
		this.rightPanelByDefault = rightPanelByDefault;
	}

	public Intent getIntentOnClick() {
		return intentOnClick;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getGroupId() {
		return groupId;
	}

	/**
	 * Sets custom widget icons as content URIs. When set, they take
	 * precedence over the resource-name icons. The external app must grant OsmAnd
	 * read access to the URIs (e.g. FileProvider URIs with read permission).
	 *
	 * @param menuIconUri  icon shown in the configure menu (optional).
	 * @param lightIconUri widget icon for the light theme.
	 * @param darkIconUri  widget icon for the dark theme.
	 */
	public void setIconUris(String menuIconUri, String lightIconUri, String darkIconUri) {
		this.menuIconUri = menuIconUri;
		this.lightIconUri = lightIconUri;
		this.darkIconUri = darkIconUri;
	}

	public String getMenuIconUri() {
		return menuIconUri;
	}

	public String getLightIconUri() {
		return lightIconUri;
	}

	public String getDarkIconUri() {
		return darkIconUri;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("id", id);
		bundle.putString("menuIconName", menuIconName);
		bundle.putString("menuTitle", menuTitle);
		bundle.putString("lightIconName", lightIconName);
		bundle.putString("darkIconName", darkIconName);
		bundle.putString("text", text);
		bundle.putString("description", description);
		bundle.putInt("order", order);
		bundle.putBoolean("rightPanelByDefault", rightPanelByDefault);
		bundle.putParcelable("intentOnClick", intentOnClick);
		bundle.putString("groupId", groupId);
		bundle.putString("menuIconUri", menuIconUri);
		bundle.putString("lightIconUri", lightIconUri);
		bundle.putString("darkIconUri", darkIconUri);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(Intent.class.getClassLoader());
		id = bundle.getString("id");
		menuIconName = bundle.getString("menuIconName");
		menuTitle = bundle.getString("menuTitle");
		lightIconName = bundle.getString("lightIconName");
		darkIconName = bundle.getString("darkIconName");
		text = bundle.getString("text");
		description = bundle.getString("description");
		order = bundle.getInt("order");
		rightPanelByDefault = bundle.getBoolean("rightPanelByDefault");
		intentOnClick = bundle.getParcelable("intentOnClick");
		groupId = bundle.getString("groupId");
		menuIconUri = bundle.getString("menuIconUri");
		lightIconUri = bundle.getString("lightIconUri");
		darkIconUri = bundle.getString("darkIconUri");
	}
}