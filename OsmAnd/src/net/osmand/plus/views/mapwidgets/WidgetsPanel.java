package net.osmand.plus.views.mapwidgets;

import android.text.TextUtils;
import android.util.Pair;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

public enum WidgetsPanel {

	LEFT(R.drawable.ic_action_screen_side_left, R.string.map_widget_left, R.id.left_side),
	RIGHT(R.drawable.ic_action_screen_side_right, R.string.map_widget_right, R.id.right_side),
	TOP(R.drawable.ic_action_screen_side_top, R.string.top_widgets_panel, R.id.top_side),
	BOTTOM(R.drawable.ic_action_screen_side_bottom, R.string.bottom_widgets_panel, R.id.bottom_side);

	public static final String PAGE_SEPARATOR = ";";
	public static final String WIDGET_SEPARATOR = ",";
	public static final Integer DEFAULT_ORDER = 1000;

	private static final List<String> originalLeftOrder = new ArrayList<>();
	private static final List<String> originalRightOrder = new ArrayList<>();
	private static final List<String> originalTopOrder = new ArrayList<>();
	private static final List<String> originalBottomOrder = new ArrayList<>();

	static {
		for (WidgetParams widget : WidgetParams.values()) {
			String id = widget.id;
			WidgetsPanel defaultPanel = widget.defaultPanel;
			if (defaultPanel == LEFT) {
				originalLeftOrder.add(id);
			} else if (defaultPanel == TOP) {
				originalTopOrder.add(id);
			} else if (defaultPanel == RIGHT) {
				originalRightOrder.add(id);
			} else if (defaultPanel == BOTTOM) {
				originalBottomOrder.add(id);
			} else {
				throw new IllegalStateException("Unsupported panel");
			}
		}
	}

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

	@NonNull
	public List<String> getOriginalOrder() {
		if (this == LEFT) {
			return new ArrayList<>(originalLeftOrder);
		} else if (this == RIGHT) {
			return new ArrayList<>(originalRightOrder);
		} else if (this == TOP) {
			return new ArrayList<>(originalTopOrder);
		} else {
			return new ArrayList<>(originalBottomOrder);
		}
	}

	public int getWidgetPage(@NonNull String widgetId, @NonNull OsmandSettings settings) {
		return getWidgetPage(settings.getApplicationMode(), widgetId, settings);
	}

	public int getWidgetPage(@NonNull ApplicationMode appMode, @NonNull String widgetId, @NonNull OsmandSettings settings) {
		return getPagedOrder(appMode, widgetId, settings).first;
	}

	public int getWidgetOrder(@NonNull String widgetId, @NonNull OsmandSettings settings) {
		return getWidgetOrder(settings.getApplicationMode(), widgetId, settings);
	}

	public int getWidgetOrder(@NonNull ApplicationMode appMode, @NonNull String widgetId, @NonNull OsmandSettings settings) {
		return getPagedOrder(appMode, widgetId, settings).second;
	}

	@NonNull
	private Pair<Integer, Integer> getPagedOrder(@NonNull ApplicationMode appMode,
	                                             @NonNull String widgetId,
	                                             @NonNull OsmandSettings settings) {
		ListStringPreference orderPreference = getOrderPreference(settings);
		List<String> pages = orderPreference.getStringsListForProfile(appMode);
		if (Algorithms.isEmpty(pages)) {
			return Pair.create(0, DEFAULT_ORDER);
		}

		for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
			String page = pages.get(pageIndex);
			List<String> orders = Arrays.asList(page.split(","));
			int order = orders.indexOf(widgetId);
			if (order != -1) {
				return Pair.create(pageIndex, order);
			}
		}

		return Pair.create(0, DEFAULT_ORDER);
	}

	public boolean setWidgetsOrder(@NonNull ApplicationMode appMode,
	                               @NonNull List<List<String>> pagedOrder,
	                               @NonNull OsmandSettings settings) {
		ListStringPreference orderPreference = getOrderPreference(settings);
		StringBuilder stringBuilder = new StringBuilder();
		for (List<String> widgets : pagedOrder) {
			String widgetsOrder = TextUtils.join(WIDGET_SEPARATOR, widgets);
			if (!Algorithms.isEmpty(widgetsOrder)) {
				stringBuilder.append(widgetsOrder)
						.append(PAGE_SEPARATOR);
			}
		}
		return orderPreference.setModeValue(appMode, stringBuilder.toString());
	}

	public boolean isPagingAllowed() {
		return this == RIGHT;
	}

	@NonNull
	public ListStringPreference getOrderPreference(@NonNull OsmandSettings settings) {
		if (this == LEFT) {
			return settings.LEFT_WIDGET_PANEL_ORDER;
		} else if (this == RIGHT) {
			return settings.RIGHT_WIDGET_PANEL_ORDER;
		} else if (this == TOP) {
			return settings.TOP_WIDGET_PANEL_ORDER;
		} else if (this == BOTTOM) {
			return settings.BOTTOM_WIDGET_PANEL_ORDER;
		}
		throw new IllegalStateException("Unsupported panel");
	}
}