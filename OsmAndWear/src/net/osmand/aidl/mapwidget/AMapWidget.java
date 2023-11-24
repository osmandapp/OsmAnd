package net.osmand.aidl.mapwidget;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

public class AMapWidget implements Parcelable {
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

	public static final Parcelable.Creator<AMapWidget> CREATOR = new
			Parcelable.Creator<AMapWidget>() {
				public AMapWidget createFromParcel(Parcel in) {
					return new AMapWidget(in);
				}

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

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(id);
		out.writeString(menuIconName);
		out.writeString(menuTitle);
		out.writeString(lightIconName);
		out.writeString(darkIconName);
		out.writeString(text);
		out.writeString(description);
		out.writeInt(order);
		out.writeParcelable(intentOnClick, flags);
	}

	private void readFromParcel(Parcel in) {
		id = in.readString();
		menuIconName = in.readString();
		menuTitle = in.readString();
		lightIconName = in.readString();
		darkIconName = in.readString();
		text = in.readString();
		description = in.readString();
		order = in.readInt();
		intentOnClick = in.readParcelable(Intent.class.getClassLoader());
	}

	public int describeContents() {
		return 0;
	}
}
