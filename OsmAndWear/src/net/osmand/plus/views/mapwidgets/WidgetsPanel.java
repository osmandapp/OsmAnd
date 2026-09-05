package net.osmand.plus.views.mapwidgets;

import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum WidgetsPanel {

	LEFT(R.drawable.ic_action_screen_side_left, R.string.map_widget_left),
	RIGHT(R.drawable.ic_action_screen_side_right, R.string.map_widget_right),
	TOP(R.drawable.ic_action_screen_side_top, R.string.top_widgets_panel),
	BOTTOM(R.drawable.ic_action_screen_side_bottom, R.string.bottom_widgets_panel);

	public static final String PAGE_SEPARATOR = ";";
	public static final String WIDGET_SEPARATOR = ",";
	public static final Integer DEFAULT_ORDER = 1000;

	private static final List<String> ORIGINAL_LEFT_ORDER = new ArrayList<>();
	private static final List<String> ORIGINAL_RIGHT_ORDER = new ArrayList<>();
	private static final List<String> ORIGINAL_TOP_ORDER = new ArrayList<>();
	private static final List<String> ORIGINAL_BOTTOM_ORDER = new ArrayList<>();

	static {
		for (WidgetType widget : WidgetType.values()) {
			String id = widget.id;
			WidgetsPanel defaultPanel = widget.defaultPanel;
			if (defaultPanel == LEFT) {
				ORIGINAL_LEFT_ORDER.add(id);
			} else if (defaultPanel == TOP) {
				ORIGINAL_TOP_ORDER.add(id);
			} else if (defaultPanel == RIGHT) {
				ORIGINAL_RIGHT_ORDER.add(id);
			} else if (defaultPanel == BOTTOM) {
				ORIGINAL_BOTTOM_ORDER.add(id);
			} else {
				throw new IllegalStateException("Unsupported panel");
			}
		}
	}

	@DrawableRes
	private final int iconId;
	@StringRes
	private final int titleId;

	WidgetsPanel(@DrawableRes int iconId, @StringRes int titleId) {
		this.iconId = iconId;
		this.titleId = titleId;
	}

	@DrawableRes
	public int getIconId(boolean rtl) {
		return getRtlPanel(rtl).iconId;
	}

	@StringRes
	public int getTitleId(boolean rtl) {
		return getRtlPanel(rtl).titleId;
	}

	@NonNull
	private WidgetsPanel getRtlPanel(boolean rtl) {
		if (!rtl || this == TOP || this == BOTTOM) {
			return this;
		} else if (this == LEFT) {
			return RIGHT;
		} else if (this == RIGHT) {
			return LEFT;
		}
		throw new IllegalStateException("Unsupported panel");
	}

	@NonNull
	public List<String> getOriginalOrder() {
		if (this == LEFT) {
			return new ArrayList<>(ORIGINAL_LEFT_ORDER);
		} else if (this == RIGHT) {
			return new ArrayList<>(ORIGINAL_RIGHT_ORDER);
		} else if (this == TOP) {
			return new ArrayList<>(ORIGINAL_TOP_ORDER);
		} else {
			return new ArrayList<>(ORIGINAL_BOTTOM_ORDER);
		}
	}

	public int getOriginalWidgetOrder(@NonNull String widgetId) {
		int order = getOriginalOrder().indexOf(widgetId);
		return order != -1 ? order : DEFAULT_ORDER;
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
		ListStringPreference preference = getOrderPreference(settings);
		List<String> pages = preference.getStringsListForProfile(appMode);
		if (!Algorithms.isEmpty(pages)) {
			if ((this == TOP || this == BOTTOM) &&
					preference.getRawModeValue(appMode).equals(preference.getProfileDefaultValue(appMode))) {
				return getDefaultPagedOrder(pages, appMode, widgetId, settings);
			} else {
				for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
					String page = pages.get(pageIndex);
					List<String> orders = Arrays.asList(page.split(","));
					int order = orders.indexOf(widgetId);
					if (order != -1) {
						return Pair.create(pageIndex, order);
					}
				}
			}
		}
		return Pair.create(0, DEFAULT_ORDER);
	}

	private Pair<Integer, Integer> getDefaultPagedOrder(@NonNull List<String> pages,
	                                                    @NonNull ApplicationMode appMode,
	                                                    @NonNull String widgetId,
	                                                    @NonNull OsmandSettings settings) {
		int pageIndex = 0;
		for (int page = 0; page < pages.size(); page++) {
			String pageString = pages.get(page);
			List<String> orders = Arrays.asList(pageString.split(","));
			boolean widgetInPageAvailable = false;
			for (int order = 0; order < orders.size(); order++) {
				if (WidgetsAvailabilityHelper.isWidgetVisibleByDefault(settings.getContext(), orders.get(order), appMode)) {
					widgetInPageAvailable = true;
					int widgetOrder = orders.indexOf(widgetId);
					if (widgetOrder != -1) {
						return Pair.create(pageIndex, widgetOrder);
					}
				}
			}
			if (widgetInPageAvailable) {
				pageIndex++;
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

	public boolean contains(@NonNull String widgetId, @NonNull OsmandSettings settings) {
		return contains(widgetId, settings, settings.getApplicationMode());
	}

	public boolean contains(@NonNull String widgetId, @NonNull OsmandSettings settings, @NonNull ApplicationMode appMode) {
		return getWidgetOrder(appMode, widgetId, settings) != DEFAULT_ORDER;
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

	public boolean isPanelVertical() {
		return this == TOP || this == BOTTOM;
	}

}