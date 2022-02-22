package net.osmand.plus.views.mapwidgets;

import net.osmand.plus.R;

public enum WidgetsPanel {

	LEFT(R.drawable.ic_action_screen_side_left, R.string.map_widget_left, R.id.left_side),
	RIGHT(R.drawable.ic_action_screen_side_right, R.string.map_widget_right, R.id.right_side),
	TOP(R.drawable.ic_action_screen_side_top, R.string.top_widgets_panel, R.id.top_side),
	BOTTOM(R.drawable.ic_action_screen_side_bottom, R.string.bottom_widgets_panel, R.id.bottom_side);

	private final int iconId;
	private final int titleId;
	private final int tabId;

	WidgetsPanel(int iconId, int titleId, int tabId) {
		this.iconId = iconId;
		this.titleId = titleId;
		this.tabId = tabId;
	}

	public int getIconId() {
		return iconId;
	}

	public int getTitleId() {
		return titleId;
	}

	public int getTabId() {
		return tabId;
	}

}
