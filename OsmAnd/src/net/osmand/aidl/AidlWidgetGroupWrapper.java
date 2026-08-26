package net.osmand.aidl;

import net.osmand.aidlapi.mapwidget.AWidgetGroup;

public class AidlWidgetGroupWrapper {

	private final String id;
	private final String name;
	private final String description;
	private final String dayIconName;
	private final String nightIconName;
	private final String dayIconUri;
	private final String nightIconUri;

	public AidlWidgetGroupWrapper(AWidgetGroup group) {
		this.id = group.getId();
		this.name = group.getName();
		this.description = group.getDescription();
		this.dayIconName = group.getDayIconName();
		this.nightIconName = group.getNightIconName();
		this.dayIconUri = group.getDayIconUri();
		this.nightIconUri = group.getNightIconUri();
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
}
