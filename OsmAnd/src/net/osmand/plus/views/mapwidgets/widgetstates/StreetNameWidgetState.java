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
	private static final String CUSTOM_APPROACH_THRESHOLDS_PREF_ID = "custom_approach_thresholds";
	private static final String APPROACH_POI_DISTANCE_PREF_ID = "approach_poi_distance";
	private static final String APPROACH_POI_TIME_PREF_ID = "approach_poi_time";

	private final CommonPreference<Boolean> showNextTurnPref;
	private final CommonPreference<Boolean> customApproachThresholdsPref;
	private final CommonPreference<Integer> approachPoiDistancePref;
	private final CommonPreference<Integer> approachPoiTimePref;
	private final WidgetType widgetType = WidgetType.STREET_NAME;

	public StreetNameWidgetState(@NonNull OsmandApplication app, @Nullable String customId) {
		super(app);
		showNextTurnPref = registerShowNextTurnPreference(customId);
		customApproachThresholdsPref = registerCustomApproachThresholdsPreference(customId);
		approachPoiDistancePref = registerApproachPoiDistancePreference(customId);
		approachPoiTimePref = registerApproachPoiTimePreference(customId);
	}

	public boolean isShowNextTurnEnabled(@NonNull ApplicationMode appMode) {
		return showNextTurnPref.getModeValue(appMode);
	}

	public void setShowNextTurnEnabled(@NonNull ApplicationMode appMode, boolean value) {
		showNextTurnPref.setModeValue(appMode, value);
	}

	public boolean isCustomApproachThresholdsEnabled(@NonNull ApplicationMode appMode) {
		return customApproachThresholdsPref.getModeValue(appMode);
	}

	public void setCustomApproachThresholdsEnabled(@NonNull ApplicationMode appMode, boolean value) {
		customApproachThresholdsPref.setModeValue(appMode, value);
	}

	public int getApproachPoiDistance(@NonNull ApplicationMode appMode) {
		return approachPoiDistancePref.getModeValue(appMode);
	}

	public void setApproachPoiDistance(@NonNull ApplicationMode appMode, int value) {
		approachPoiDistancePref.setModeValue(appMode, value);
	}

	public int getApproachPoiTime(@NonNull ApplicationMode appMode) {
		return approachPoiTimePref.getModeValue(appMode);
	}

	public void setApproachPoiTime(@NonNull ApplicationMode appMode, int value) {
		approachPoiTimePref.setModeValue(appMode, value);
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
		registerCustomApproachThresholdsPreference(customId).setModeValue(appMode, customApproachThresholdsPref.getModeValue(sourceAppMode));
		registerApproachPoiDistancePreference(customId).setModeValue(appMode, approachPoiDistancePref.getModeValue(sourceAppMode));
		registerApproachPoiTimePreference(customId).setModeValue(appMode, approachPoiTimePref.getModeValue(sourceAppMode));
	}

	@NonNull
	private CommonPreference<Boolean> registerShowNextTurnPreference(@Nullable String customId) {
		String prefId = Algorithms.isEmpty(customId) ? SHOW_NEXT_TURN_PREF_ID : SHOW_NEXT_TURN_PREF_ID + "_" + customId;
		return settings.registerBooleanPreference(prefId, false).makeProfile().cache();
	}

	@NonNull
	private CommonPreference<Boolean> registerCustomApproachThresholdsPreference(@Nullable String customId) {
		String prefId = Algorithms.isEmpty(customId) ? CUSTOM_APPROACH_THRESHOLDS_PREF_ID : CUSTOM_APPROACH_THRESHOLDS_PREF_ID + "_" + customId;
		return settings.registerBooleanPreference(prefId, false).makeProfile().cache();
	}

	@NonNull
	private CommonPreference<Integer> registerApproachPoiDistancePreference(@Nullable String customId) {
		String prefId = Algorithms.isEmpty(customId) ? APPROACH_POI_DISTANCE_PREF_ID : APPROACH_POI_DISTANCE_PREF_ID + "_" + customId;
		return settings.registerIntPreference(prefId, 0).makeProfile().cache();
	}

	@NonNull
	private CommonPreference<Integer> registerApproachPoiTimePreference(@Nullable String customId) {
		String prefId = Algorithms.isEmpty(customId) ? APPROACH_POI_TIME_PREF_ID : APPROACH_POI_TIME_PREF_ID + "_" + customId;
		return settings.registerIntPreference(prefId, 0).makeProfile().cache();
	}
}
