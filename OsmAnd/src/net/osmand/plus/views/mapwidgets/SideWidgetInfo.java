package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.COLLAPSED_PREFIX;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.HIDE_PREFIX;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.SETTINGS_SEPARATOR;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.LEFT;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.RIGHT;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SideWidgetInfo extends MapWidgetInfo {

	private String externalProviderPackage;

	public SideWidgetInfo(@NonNull String key,
	                      @NonNull TextInfoWidget widget,
	                      @DrawableRes int daySettingsIconId,
	                      @DrawableRes int nightSettingsIconId,
	                      @StringRes int messageId,
	                      @Nullable String message,
	                      int page,
	                      int order,
	                      @NonNull WidgetsPanel widgetPanel) {
		super(key, widget, daySettingsIconId, nightSettingsIconId, messageId, message, page, order, widgetPanel);

		if (getMessage() != null) {
			widget.setContentTitle(getMessage());
		} else if (getMessageId() != MapWidgetInfo.INVALID_ID) {
			widget.setContentTitle(getMessageId());
		}
	}

	public void setExternalProviderPackage(@NonNull String externalProviderPackage) {
		this.externalProviderPackage = externalProviderPackage;
	}

	@Nullable
	public String getExternalProviderPackage() {
		return externalProviderPackage;
	}

	@NonNull
	@Override
	public WidgetsPanel getUpdatedPanel() {
		OsmandSettings settings = widget.getMyApplication().getSettings();
		WidgetType widgetType = getWidgetType();
		if (widgetType != null) {
			if (widgetType.defaultPanel == LEFT && RIGHT.contains(key, settings)) {
				widgetPanel = RIGHT;
			} else if (widgetType.defaultPanel == RIGHT && LEFT.contains(key, settings)) {
				widgetPanel = LEFT;
			} else {
				widgetPanel = widgetType.defaultPanel;
			}
		} else {
			widgetPanel = LEFT.contains(key, settings) ? LEFT : RIGHT;
		}
		return widgetPanel;
	}

	@Override
	public boolean isEnabledForAppMode(@NonNull ApplicationMode appMode) {
		List<String> widgetsVisibility = getWidgetsVisibility(appMode);
		if (widgetsVisibility.contains(key) || widgetsVisibility.contains(COLLAPSED_PREFIX + key)) {
			return true;
		} else if (widgetsVisibility.contains(HIDE_PREFIX + key)) {
			return false;
		}
		return WidgetsAvailabilityHelper.isWidgetVisibleByDefault(getApp(), key, appMode);
	}

	@Override
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
	private OsmandSettings getSettings() {
		return getApp().getSettings();
	}

	@NonNull
	private OsmandApplication getApp() {
		return widget.getMyApplication();
	}

	@NonNull
	private OsmandPreference<String> getVisibilityPreference() {
		return getSettings().MAP_INFO_CONTROLS;
	}
}