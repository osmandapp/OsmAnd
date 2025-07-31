package net.osmand.plus.views.mapwidgets.widgetstates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

public class StreetNameWidgetState extends WidgetState {

	private static final String SHOW_NEXT_TURN_PREF_ID = "show_next_turn_info";

	private final CommonPreference<Boolean> showNextTurnPref;
	private final WidgetType widgetType = WidgetType.STREET_NAME;

	public StreetNameWidgetState(@NonNull OsmandApplication app, @Nullable String customId) {
		super(app);
		showNextTurnPref = registerShowNextTurnPreference(customId);
	}

	public boolean isShowNextTurnEnabled(@NonNull ApplicationMode appMode) {
		return showNextTurnPref.getModeValue(appMode);
	}

	public void setShowNextTurnEnabled(@NonNull ApplicationMode appMode, boolean value) {
		showNextTurnPref.setModeValue(appMode, value);
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
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copyPrefsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode,
	                              @NonNull ApplicationMode appMode, @Nullable String customId) {
		registerShowNextTurnPreference(customId).setModeValue(appMode, showNextTurnPref.getModeValue(sourceAppMode));
	}

	@NonNull
	private CommonPreference<Boolean> registerShowNextTurnPreference(@Nullable String customId) {
		String prefId = SHOW_NEXT_TURN_PREF_ID;
		if (!Algorithms.isEmpty(customId)) {
			prefId += "_" + customId;
		}
		return settings.registerBooleanPreference(prefId, false).makeProfile().cache();
	}
}
