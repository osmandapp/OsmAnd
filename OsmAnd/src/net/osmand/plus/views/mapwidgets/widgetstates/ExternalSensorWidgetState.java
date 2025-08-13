package net.osmand.plus.views.mapwidgets.widgetstates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.ExternalDeviceShowMode;
import net.osmand.util.Algorithms;

public class ExternalSensorWidgetState extends WidgetState {

	public static final String SHOW_MODE_ID = "show_mode";

	private final SensorWidgetDataFieldType widgetType;
	private final OsmandPreference<ExternalDeviceShowMode> showModePreference;


	public ExternalSensorWidgetState(@NonNull OsmandApplication app, @Nullable String customId, @NonNull SensorWidgetDataFieldType widgetType) {
		super(app);
		this.widgetType = widgetType;
		this.showModePreference = registerShowModePreference(customId);
	}

	@NonNull
	public OsmandPreference<ExternalDeviceShowMode> getShowModePreference() {
		return showModePreference;
	}

	@NonNull
	@Override
	public String getTitle() {
		return app.getString(widgetType.getWidgetType().titleId);
	}

	@Override
	public int getSettingsIconId(boolean nightMode) {
		return nightMode ? widgetType.nightIconId : widgetType.dayIconId;
	}

	@Override
	public void changeToNextState() {
		ExternalDeviceShowMode currentMode = showModePreference.get();
		showModePreference.set(currentMode.next());
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copyPrefsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		registerShowModePreference(customId).setModeValue(appMode, showModePreference.getModeValue(sourceAppMode));
	}

	@NonNull
	private OsmandPreference<ExternalDeviceShowMode> registerShowModePreference(@Nullable String customId) {
		String prefId = SHOW_MODE_ID;
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerEnumStringPreference(prefId, ExternalDeviceShowMode.SENSOR_DATA,
				ExternalDeviceShowMode.values(), ExternalDeviceShowMode.class).makeProfile();
	}
}