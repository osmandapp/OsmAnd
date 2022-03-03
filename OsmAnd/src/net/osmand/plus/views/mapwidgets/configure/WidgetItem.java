package net.osmand.plus.views.mapwidgets.configure;

public class WidgetItem {

	public final String title;
	public final int iconId;
	public boolean isActive;
	public int priority;

	public WidgetItem(int iconId, String title) {
		this.iconId = iconId;
		this.title = title;
	}

}
