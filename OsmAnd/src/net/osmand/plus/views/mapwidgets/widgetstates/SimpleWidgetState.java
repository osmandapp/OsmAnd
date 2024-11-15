package net.osmand.plus.views.mapwidgets.widgetstates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

public class SimpleWidgetState extends ResizableWidgetState {

	private final CommonPreference<Boolean> showIconPref;
	private final WidgetType widgetType;

	public SimpleWidgetState(@NonNull OsmandApplication app, @Nullable String customId, @NonNull WidgetType widgetType) {
		super(app, customId, widgetType);
		this.showIconPref = registerShowIconPref(customId, widgetType);
		this.widgetType = widgetType;
	}

	@NonNull
	private CommonPreference<Boolean> registerShowIconPref(@Nullable String customId, @NonNull WidgetType widgetType) {
		String prefId = "simple_widget_show_icon";
		prefId += widgetType.id;
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerBooleanPreference(prefId, true).makeProfile();
	}

	@NonNull
	public CommonPreference<Boolean> getShowIconPref() {
		return showIconPref;
	}

	@NonNull
	@Override
	public String getTitle() {
		return app.getString(widgetType.titleId);
	}

	@Override
	public int getSettingsIconId(boolean nightMode) {
		return nightMode ? widgetType.nightIconId : widgetType.dayIconId;
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copyPrefsFromMode(appMode, appMode, customId);
		super.copyPrefs(appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId){
		registerShowIconPref(customId, widgetType).setModeValue(appMode, showIconPref.getModeValue(sourceAppMode));
	}
}