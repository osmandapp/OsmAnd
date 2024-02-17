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

public class SimpleWidgetState extends WidgetState {

	private final OsmandPreference<WidgetSize> widgetSizePref;
	private final CommonPreference<Boolean> showIconPref;
	private final WidgetType widgetType;

	public SimpleWidgetState(@NonNull OsmandApplication app, @Nullable String customId, @NonNull WidgetType widgetType) {
		super(app);
		this.widgetSizePref = registerWidgetSizePref(customId, widgetType);
		this.showIconPref = registerShowIconPref(customId, widgetType);
		this.widgetType = widgetType;
	}

	@NonNull
	private OsmandPreference<WidgetSize> registerWidgetSizePref(@Nullable String customId, @NonNull WidgetType widgetType) {
		String prefId = "simple_widget_size";
		prefId += widgetType.id;
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerEnumStringPreference(prefId, WidgetSize.MEDIUM, WidgetSize.values(), WidgetSize.class)
				.makeProfile();
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
	public OsmandPreference<WidgetSize> getWidgetSizePref() {
		return widgetSizePref;
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
	public void changeToNextState() {
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		registerWidgetSizePref(customId, widgetType).setModeValue(appMode, widgetSizePref.getModeValue(appMode));
		registerShowIconPref(customId, widgetType).setModeValue(appMode, showIconPref.getModeValue(appMode));
	}
}