package net.osmand.aidl;

import android.content.Intent;

import net.osmand.aidl.mapwidget.AMapWidget;

public class AidlMapWidgetWrapper {

	private final String id;
	private final String menuIconName;
	private final String menuTitle;
	private final String lightIconName;
	private final String darkIconName;
	private final String text;
	private final String description;
	private final int order;
	private final boolean rightPanelByDefault;
	private final Intent intentOnClick;

	/**
	 * Old API constructor
	 */
	public AidlMapWidgetWrapper(AMapWidget aMapWidget) {
		this.id = aMapWidget.getId();
		this.menuIconName = aMapWidget.getMenuIconName();
		this.menuTitle = aMapWidget.getMenuTitle();
		this.lightIconName = aMapWidget.getLightIconName();
		this.darkIconName = aMapWidget.getDarkIconName();
		this.text = aMapWidget.getText();
		this.description = aMapWidget.getDescription();
		this.order = aMapWidget.getOrder();
		this.rightPanelByDefault = true;
		this.intentOnClick = aMapWidget.getIntentOnClick();
	}

	/**
	 * Up to date API constructor
	 */
	public AidlMapWidgetWrapper(net.osmand.aidlapi.mapwidget.AMapWidget aMapWidget) {
		this.id = aMapWidget.getId();
		this.menuIconName = aMapWidget.getMenuIconName();
		this.menuTitle = aMapWidget.getMenuTitle();
		this.lightIconName = aMapWidget.getLightIconName();
		this.darkIconName = aMapWidget.getDarkIconName();
		this.text = aMapWidget.getText();
		this.description = aMapWidget.getDescription();
		this.order = aMapWidget.getOrder();
		this.rightPanelByDefault = aMapWidget.isRightPanelByDefault();
		this.intentOnClick = aMapWidget.getIntentOnClick();
	}

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

	public Intent getIntentOnClick() {
		return intentOnClick;
	}
}