package net.osmand.plus.views.mapwidgets.widgetstates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

public class SunriseSunsetWidgetState extends WidgetState {

	private final WidgetType widgetType;
	private final OsmandPreference<Boolean> preference;

	public SunriseSunsetWidgetState(@NonNull OsmandApplication app, @Nullable String customId, WidgetType widgetType) {
		super(app);
		this.widgetType = widgetType;
		this.preference = registerPreference(customId);
	}

	@NonNull
	public WidgetType getWidgetType() {
		return widgetType;
	}

	@NonNull
	public OsmandPreference<Boolean> getPreference() {
		return preference;
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
	}

	@NonNull
	private OsmandPreference<Boolean> registerPreference(@Nullable String customId) {
		String prefId = getPrefId();
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerBooleanPreference(prefId, true).makeProfile();
	}

	private String getPrefId(){
		switch (widgetType){
			case SUNSET:
				return "show_sunset_info";
			case SUNRISE:
				return "show_sunrise_info";
			case SUN_POSITION:
			default:
				return "show_sun_position_info";
		}
	}
}
