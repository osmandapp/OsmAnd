package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.COLLAPSED_PREFIX;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.HIDE_PREFIX;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.SETTINGS_SEPARATOR;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class MapWidgetInfo implements Comparable<MapWidgetInfo> {

	public static final String DELIMITER = "__";
	public static final int INVALID_ID = 0;

	public final String key;
	public final MapWidget widget;

	protected WidgetsPanel widgetPanel;
	public int priority;
	public int pageIndex;

	@DrawableRes
	private final int daySettingsIconId;
	@DrawableRes
	private final int nightSettingsIconId;
	@StringRes
	private final int messageId;
	private final String message;
	private final WidgetState widgetState;

	public MapWidgetInfo(@NonNull String key,
						 @NonNull MapWidget widget,
						 @DrawableRes int daySettingsIconId,
						 @DrawableRes int nightSettingsIconId,
						 @StringRes int messageId,
						 @Nullable String message,
						 int page,
						 int order,
						 @NonNull WidgetsPanel widgetPanel) {
		this.key = key;
		this.widget = widget;
		this.widgetState = widget.getWidgetState();
		this.daySettingsIconId = daySettingsIconId;
		this.nightSettingsIconId = nightSettingsIconId;
		this.messageId = messageId;
		this.message = message;
		this.pageIndex = page;
		this.priority = order;
		this.widgetPanel = widgetPanel;
	}

	public void setWidgetPanel(@NonNull WidgetsPanel widgetPanel) {
		this.widgetPanel = widgetPanel;
	}

	@NonNull
	public WidgetsPanel getWidgetPanel() {
		return widgetPanel;
	}

	public boolean isCustomWidget() {
		return key.contains(DELIMITER);
	}

	@Nullable
	public WidgetState getWidgetState() {
		return widgetState;
	}

	@DrawableRes
	public int getSettingsIconId(boolean nightMode) {
		if (widgetState != null) {
			return widgetState.getSettingsIconId(nightMode);
		} else {
			return nightMode ? nightSettingsIconId : daySettingsIconId;
		}
	}

	@DrawableRes
	public int getMapIconId(boolean nightMode) {
		if (widget instanceof TextInfoWidget) {
			TextInfoWidget textInfoWidget = (TextInfoWidget) widget;
			return textInfoWidget.getIconId(nightMode);
		}
		return 0;
	}

	public boolean isIconPainted() {
		int dayMapIconId = getMapIconId(false);
		int nightMapIconId = getMapIconId(true);
		int daySettingsIconId = getSettingsIconId(false);
		int nightSettingsIconId = getSettingsIconId(true);
		if (dayMapIconId != 0 && nightMapIconId != 0) {
			return dayMapIconId != nightMapIconId;
		} else if (daySettingsIconId != 0 && nightSettingsIconId != 0) {
			return daySettingsIconId != nightSettingsIconId;
		}
		return false;
	}

	@Nullable
	public WidgetType getWidgetType() {
		return widget.getWidgetType();
	}

	public boolean isExternal() {
		return widget.isExternal();
	}

	@NonNull
	public String getTitle(@NonNull Context ctx) {
		String message = getMessage();
		return message != null ? message : ctx.getString(getMessageId());
	}

	@NonNull
	public String getStateIndependentTitle(@NonNull Context context) {
		return message != null ? message : context.getString(getMessageId());
	}

	@Nullable
	public String getMessage() {
		return widgetState != null ? widgetState.getTitle() : message;
	}

	@StringRes
	public int getMessageId() {
		return messageId;
	}

	public void setExternalProviderPackage(@NonNull String externalProviderPackage) {
	}

	@Nullable
	public String getExternalProviderPackage() {
		return null;
	}

	@NonNull
	public abstract WidgetsPanel getUpdatedPanel();

	public boolean isEnabledForAppMode(@NonNull ApplicationMode appMode) {
		List<String> widgetsVisibility = getWidgetsVisibility(appMode);
		if (widgetsVisibility.contains(key) || widgetsVisibility.contains(COLLAPSED_PREFIX + key)) {
			return true;
		} else if (widgetsVisibility.contains(HIDE_PREFIX + key)) {
			return false;
		}
		return WidgetsAvailabilityHelper.isWidgetVisibleByDefault(getApp(), key, appMode);
	}

	public void enableDisableForMode(@NonNull ApplicationMode appMode, @Nullable Boolean enabled) {
		List<String> widgetsVisibility = getWidgetsVisibility(appMode);
		widgetsVisibility.remove(key);
		widgetsVisibility.remove(COLLAPSED_PREFIX + key);
		widgetsVisibility.remove(HIDE_PREFIX + key);

		if (enabled != null && (!isCustomWidget() || enabled)) {
			widgetsVisibility.add(enabled ? key : HIDE_PREFIX + key);
		}

		StringBuilder newVisibilityString = new StringBuilder();
		for (String visibility : widgetsVisibility) {
			newVisibilityString.append(visibility).append(SETTINGS_SEPARATOR);
		}

		getVisibilityPreference().setModeValue(appMode, newVisibilityString.toString());

		OsmandPreference<?> settingsPref = widget.getWidgetSettingsPrefToReset(appMode);
		if ((enabled == null || !enabled) && settingsPref != null) {
			settingsPref.resetModeToDefault(appMode);
		}
	}

	@NonNull
	private List<String> getWidgetsVisibility(@NonNull ApplicationMode appMode) {
		String widgetsVisibilityString = getVisibilityPreference().getModeValue(appMode);
		return new ArrayList<>(Arrays.asList(widgetsVisibilityString.split(SETTINGS_SEPARATOR)));
	}

	@NonNull
	private OsmandPreference<String> getVisibilityPreference() {
		return getSettings().MAP_INFO_CONTROLS;
	}

	@NonNull
	private OsmandSettings getSettings() {
		return getApp().getSettings();
	}

	@NonNull
	private OsmandApplication getApp() {
		return widget.getMyApplication();
	}

	@Override
	public int hashCode() {
		String message = getMessage();
		return message != null ? message.hashCode() : getMessageId();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (getClass() != obj.getClass()) {
			return false;
		}
		MapWidgetInfo other = (MapWidgetInfo) obj;
		return Algorithms.stringsEqual(key, other.key)
				&& getMessageId() == other.getMessageId();
	}

	@Override
	public int compareTo(@NonNull MapWidgetInfo another) {
		if (equals(another)) {
			return 0;
		}
		if (pageIndex != another.pageIndex) {
			return pageIndex - another.pageIndex;
		}
		if (priority != another.priority) {
			return priority - another.priority;
		}
		if (!Algorithms.stringsEqual(key, another.key)) {
			return key.compareTo(another.key);
		}
		return getMessageId() - another.getMessageId();
	}

	@NonNull
	@Override
	public String toString() {
		return key;
	}
}