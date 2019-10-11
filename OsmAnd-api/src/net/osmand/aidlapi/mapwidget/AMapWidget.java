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
	private Intent intentOnClick;

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

	public Intent getIntentOnClick() {
		return intentOnClick;
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
		bundle.putParcelable("intentOnClick", intentOnClick);
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
		intentOnClick = bundle.getParcelable("intentOnClick");
	}
}