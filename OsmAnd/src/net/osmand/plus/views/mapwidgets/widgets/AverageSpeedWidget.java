package net.osmand.plus.views.mapwidgets.widgets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.utils.AverageSpeedComputer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

public class AverageSpeedWidget extends TextInfoWidget {

	private static final String MEASURED_INTERVAL_PREF_ID = "average_speed_measured_interval_millis";
	private static final String SKIP_STOPS_PREF_ID = "average_speed_skip_stops";

	private static final int UPDATE_INTERVAL_MILLIS = 1000;

	private final AverageSpeedComputer averageSpeedComputer;

	private final CommonPreference<Long> measuredIntervalPref;
	private final CommonPreference<Boolean> skipStopsPref;

	private long lastUpdateTime;

	public AverageSpeedWidget(@NonNull MapActivity mapActivity, @Nullable String customId) {
		super(mapActivity, WidgetType.AVERAGE_SPEED);
		averageSpeedComputer = app.getAverageSpeedComputer();
		setIcons(WidgetType.AVERAGE_SPEED);
		measuredIntervalPref = registerMeasuredIntervalPref(customId);
		skipStopsPref = registerSkipStopsPref(customId);
	}

	@NonNull
	public Long getMeasuredInterval(@NonNull ApplicationMode appMode) {
		return measuredIntervalPref.getModeValue(appMode);
	}

	public void setMeasuredInterval(@NonNull ApplicationMode appMode, long measuredInterval) {
		measuredIntervalPref.setModeValue(appMode, measuredInterval);
	}

	@NonNull
	public Boolean shouldSkipStops(@NonNull ApplicationMode appMode) {
		return skipStopsPref.getModeValue(appMode);
	}

	public void setShouldSkipStops(@NonNull ApplicationMode appMode, boolean skipStops) {
		skipStopsPref.setModeValue(appMode, skipStops);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		long time = System.currentTimeMillis();
		if (isUpdateNeeded() || time - lastUpdateTime > UPDATE_INTERVAL_MILLIS) {
			lastUpdateTime = time;
			updateAverageSpeed();
		}
	}

	private void updateAverageSpeed() {
		long measuredInterval = measuredIntervalPref.get();
		boolean skipLowSpeed = skipStopsPref.get();
		float averageSpeed = averageSpeedComputer.getAverageSpeed(measuredInterval, skipLowSpeed);
		if (Float.isNaN(averageSpeed)) {
			setText(NO_VALUE, null);
		} else {
			FormattedValue formattedAverageSpeed = OsmAndFormatter.getFormattedSpeedValue(averageSpeed, app);
			setText(formattedAverageSpeed.value, formattedAverageSpeed.unit);
		}
	}

	@Override
	public void copySettings(@NonNull ApplicationMode appMode, @Nullable String customId) {
		registerMeasuredIntervalPref(customId).setModeValue(appMode, measuredIntervalPref.getModeValue(appMode));
		registerSkipStopsPref(customId).setModeValue(appMode, skipStopsPref.getModeValue(appMode));
	}

	@NonNull
	private CommonPreference<Long> registerMeasuredIntervalPref(@Nullable String customId) {
		String prefId = Algorithms.isEmpty(customId)
				? MEASURED_INTERVAL_PREF_ID
				: MEASURED_INTERVAL_PREF_ID + customId;
		return settings.registerLongPreference(prefId, AverageSpeedComputer.DEFAULT_INTERVAL_MILLIS)
				.makeProfile()
				.cache();
	}

	@NonNull
	private CommonPreference<Boolean> registerSkipStopsPref(@Nullable String customId) {
		String prefId = Algorithms.isEmpty(customId) ? SKIP_STOPS_PREF_ID : SKIP_STOPS_PREF_ID + customId;
		return settings.registerBooleanPreference(prefId, true)
				.makeProfile()
				.cache();
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}
}