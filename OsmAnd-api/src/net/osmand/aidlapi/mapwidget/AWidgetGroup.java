package net.osmand.aidlapi.mapwidget;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

/**
 * Describes a group of external (AIDL) widgets. Widgets are assigned to a group
 * by sharing the same id via {@link AMapWidget#setGroupId(String)}; this object
 * provides the group title, description and icons shown in OsmAnd "Configure screen"
 * <p>
 * Icons are referenced by name from OsmAnd app resources (the same way widget
 * icons work)
 */
public class AWidgetGroup extends AidlParams {

	private String id;
	private String name;
	private String description;
	private String dayIconName;
	private String nightIconName;
	private String dayIconUri;
	private String nightIconUri;

	public AWidgetGroup(String id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	public AWidgetGroup(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AWidgetGroup> CREATOR = new Creator<AWidgetGroup>() {
		@Override
		public AWidgetGroup createFromParcel(Parcel in) {
			return new AWidgetGroup(in);
		}

		@Override
		public AWidgetGroup[] newArray(int size) {
			return new AWidgetGroup[size];
		}
	};

	/**
	 * Sets the group icons (by resource name from OsmAnd app resources).
	 *
	 * @param dayIconName   icon name for the light theme
	 * @param nightIconName icon name for the dark theme
	 */
	public void setIconNames(String dayIconName, String nightIconName) {
		this.dayIconName = dayIconName;
		this.nightIconName = nightIconName;
	}

	/**
	 * Sets custom group icons as content URIs. When set, they take
	 * precedence over the resource-name icons. The external app must grant OsmAnd
	 * read access to the URIs.
	 *
	 * @param dayIconUri   group icon for the light theme.
	 * @param nightIconUri group icon for the dark theme.
	 */
	public void setIconUris(String dayIconUri, String nightIconUri) {
		this.dayIconUri = dayIconUri;
		this.nightIconUri = nightIconUri;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getDayIconName() {
		return dayIconName;
	}

	public String getNightIconName() {
		return nightIconName;
	}

	public String getDayIconUri() {
		return dayIconUri;
	}

	public String getNightIconUri() {
		return nightIconUri;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("id", id);
		bundle.putString("name", name);
		bundle.putString("description", description);
		bundle.putString("dayIconName", dayIconName);
		bundle.putString("nightIconName", nightIconName);
		bundle.putString("dayIconUri", dayIconUri);
		bundle.putString("nightIconUri", nightIconUri);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		id = bundle.getString("id");
		name = bundle.getString("name");
		description = bundle.getString("description");
		dayIconName = bundle.getString("dayIconName");
		nightIconName = bundle.getString("nightIconName");
		dayIconUri = bundle.getString("dayIconUri");
		nightIconUri = bundle.getString("nightIconUri");
	}
}
