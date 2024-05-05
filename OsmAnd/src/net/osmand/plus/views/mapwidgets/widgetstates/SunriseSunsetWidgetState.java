package net.osmand.plus.views.mapwidgets.widgetstates;

import static net.osmand.plus.settings.enums.SunPositionMode.SUN_POSITION_MODE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.SunPositionMode;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

public class SunriseSunsetWidgetState extends WidgetState {

	private final WidgetType widgetType;
	private final OsmandPreference<Boolean> preference;
	private final OsmandPreference<SunPositionMode> sunPositionPreference;


	public SunriseSunsetWidgetState(@NonNull OsmandApplication app, @Nullable String customId, WidgetType widgetType) {
		super(app);
		this.widgetType = widgetType;
		this.preference = registerPreference(customId);
		this.sunPositionPreference = registerSunPositionPreference(customId);
	}

	@NonNull
	public WidgetType getWidgetType() {
		return widgetType;
	}

	@NonNull
	public OsmandPreference<Boolean> getPreference() {
		return preference;
	}

	public OsmandPreference<SunPositionMode> getSunPositionPreference() {
		return sunPositionPreference;
	}

	@NonNull
	@Override
	public String getTitle() {
		return app.getString(widgetType.titleId);
	}

	@Override
	public int getSettingsIconId(boolean nightMode) {
		return widgetType.getIconId(nightMode);
	}

	@Override
	public void changeToNextState() {
		preference.set(!preference.get());
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		registerPreference(customId).setModeValue(appMode, preference.getModeValue(appMode));
		registerSunPositionPreference(customId).setModeValue(appMode, sunPositionPreference.getModeValue(appMode));
	}

	@NonNull
	private OsmandPreference<Boolean> registerPreference(@Nullable String customId) {
		String prefId = getPrefId();
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerBooleanPreference(prefId, true).makeProfile();
	}

	@NonNull
	private String getPrefId() {
		switch (widgetType) {
			case SUNSET:
				return "show_sunset_info";
			case SUNRISE:
				return "show_sunrise_info";
			case SUN_POSITION:
			default:
				return "show_sun_position_info";
		}
	}

	@NonNull
	private OsmandPreference<SunPositionMode> registerSunPositionPreference(@Nullable String customId) {
		String prefId = "sun_position_widget_mode";
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerEnumStringPreference(prefId, SUN_POSITION_MODE,
				SunPositionMode.values(), SunPositionMode.class).makeProfile();
	}
}
