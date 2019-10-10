package net.osmand.aidl;

import android.content.Intent;

import net.osmand.aidl.mapwidget.AMapWidget;

public class AidlMapWidgetWrapper {

	private String id;
	private String menuIconName;
	private String menuTitle;
	private String lightIconName;
	private String darkIconName;
	private String text;
	private String description;
	private int order;
	private Intent intentOnClick;

	public AidlMapWidgetWrapper(AMapWidget aMapWidget) {
		this.id = aMapWidget.getId();
		this.menuIconName = aMapWidget.getMenuIconName();
		this.menuTitle = aMapWidget.getMenuTitle();
		this.lightIconName = aMapWidget.getLightIconName();
		this.darkIconName = aMapWidget.getDarkIconName();
		this.text = aMapWidget.getText();
		this.description = aMapWidget.getDescription();
		this.order = aMapWidget.getOrder();
		this.intentOnClick = aMapWidget.getIntentOnClick();
	}

	public AidlMapWidgetWrapper(net.osmand.aidlapi.mapwidget.AMapWidget aMapWidget) {
		this.id = aMapWidget.getId();
		this.menuIconName = aMapWidget.getMenuIconName();
		this.menuTitle = aMapWidget.getMenuTitle();
		this.lightIconName = aMapWidget.getLightIconName();
		this.darkIconName = aMapWidget.getDarkIconName();
		this.text = aMapWidget.getText();
		this.description = aMapWidget.getDescription();
		this.order = aMapWidget.getOrder();
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

	public Intent getIntentOnClick() {
		return intentOnClick;
	}
}