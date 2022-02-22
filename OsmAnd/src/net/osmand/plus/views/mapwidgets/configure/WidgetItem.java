package net.osmand.plus.views.mapwidgets.configure;

public class WidgetItem {
	private final String title;
	private final int iconId;
	private boolean isActive;
	private int priority;

	public WidgetItem(int iconId, String title) {
		this.iconId = iconId;
		this.title = title;
	}

	public int getIconId() {
		return iconId;
	}

	public String getTitle() {
		return title;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean active) {
		this.isActive = active;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}
}
