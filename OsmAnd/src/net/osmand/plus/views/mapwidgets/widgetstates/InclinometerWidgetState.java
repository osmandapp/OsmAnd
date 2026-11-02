package net.osmand.plus.views.mapwidgets.widgetstates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

public class InclinometerWidgetState extends WidgetState {

	public static final int MIN_THRESHOLD_ANGLE = 0;
	public static final int MAX_THRESHOLD_ANGLE = 60;

	private static final int DEFAULT_PITCH_WARNING_ANGLE = 25;
	private static final int DEFAULT_PITCH_CRITICAL_ANGLE = 35;
	private static final int DEFAULT_ROLL_WARNING_ANGLE = 20;
	private static final int DEFAULT_ROLL_CRITICAL_ANGLE = 30;

	private final WidgetType widgetType;
	private final OsmandPreference<Boolean> percentUnitsPref;
	private final OsmandPreference<Integer> warningAnglePref;
	private final OsmandPreference<Integer> criticalAnglePref;
	private final OsmandPreference<Boolean> soundAlertPref;

	public InclinometerWidgetState(@NonNull OsmandApplication app, @Nullable String customId, @NonNull WidgetType widgetType) {
		super(app);
		this.widgetType = widgetType;
		this.percentUnitsPref = registerPercentUnitsPref(customId);
		this.warningAnglePref = registerWarningAnglePref(customId);
		this.criticalAnglePref = registerCriticalAnglePref(customId);
		this.soundAlertPref = registerSoundAlertPref(customId);
	}

	@NonNull
	public WidgetType getWidgetType() {
		return widgetType;
	}

	@NonNull
	public OsmandPreference<Boolean> getPercentUnitsPref() {
		return percentUnitsPref;
	}

	@NonNull
	public OsmandPreference<Integer> getWarningAnglePref() {
		return warningAnglePref;
	}

	@NonNull
	public OsmandPreference<Integer> getCriticalAnglePref() {
		return criticalAnglePref;
	}

	@NonNull
	public OsmandPreference<Boolean> getSoundAlertPref() {
		return soundAlertPref;
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
		percentUnitsPref.set(!percentUnitsPref.get());
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copyPrefsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		registerPercentUnitsPref(customId).setModeValue(appMode, percentUnitsPref.getModeValue(sourceAppMode));
		registerWarningAnglePref(customId).setModeValue(appMode, warningAnglePref.getModeValue(sourceAppMode));
		registerCriticalAnglePref(customId).setModeValue(appMode, criticalAnglePref.getModeValue(sourceAppMode));
		registerSoundAlertPref(customId).setModeValue(appMode, soundAlertPref.getModeValue(sourceAppMode));
	}

	@NonNull
	private OsmandPreference<Boolean> registerPercentUnitsPref(@Nullable String customId) {
		return settings.registerBooleanPreference(getPrefId("percent_units", customId), false).makeProfile();
	}

	@NonNull
	private OsmandPreference<Integer> registerWarningAnglePref(@Nullable String customId) {
		int defaultAngle = widgetType == WidgetType.INCLINOMETER_PITCH ? DEFAULT_PITCH_WARNING_ANGLE : DEFAULT_ROLL_WARNING_ANGLE;
		return settings.registerIntPreference(getPrefId("warning_angle", customId), defaultAngle).makeProfile();
	}

	@NonNull
	private OsmandPreference<Integer> registerCriticalAnglePref(@Nullable String customId) {
		int defaultAngle = widgetType == WidgetType.INCLINOMETER_PITCH ? DEFAULT_PITCH_CRITICAL_ANGLE : DEFAULT_ROLL_CRITICAL_ANGLE;
		return settings.registerIntPreference(getPrefId("critical_angle", customId), defaultAngle).makeProfile();
	}

	@NonNull
	private OsmandPreference<Boolean> registerSoundAlertPref(@Nullable String customId) {
		return settings.registerBooleanPreference(getPrefId("sound_alert", customId), false).makeProfile();
	}

	@NonNull
	private String getPrefId(@NonNull String name, @Nullable String customId) {
		String prefId = widgetType.id + "_" + name;
		return Algorithms.isEmpty(customId) ? prefId : prefId + "_" + customId;
	}

	// Slope angle to grade percent, as shown on road signs
	public static float toPercent(float degrees) {
		return (float) (Math.tan(Math.toRadians(degrees)) * 100);
	}
}
