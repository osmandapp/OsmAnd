package net.osmand.plus.views.mapwidgets.widgets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.utils.AverageGlideComputer;
import net.osmand.plus.views.mapwidgets.utils.AverageSpeedComputer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

public class GlideAverageWidget extends GlideBaseWidget {

	private static final String MEASURED_INTERVAL_PREF_ID = "average_glide_measured_interval_millis";

	private final AverageGlideComputer averageGlideComputer;
	private final CommonPreference<Long> measuredIntervalPref;

	public GlideAverageWidget(@NonNull MapActivity mapActivity, @Nullable String customId) {
		super(mapActivity, WidgetType.GLIDE_AVERAGE);
		averageGlideComputer = app.getAverageGlideComputer();
		measuredIntervalPref = registerMeasuredIntervalPref(customId);
		updateInfo(null);
	}

	@NonNull
	public Long getMeasuredInterval(@NonNull ApplicationMode appMode) {
		return measuredIntervalPref.getModeValue(appMode);
	}

	public void setMeasuredInterval(@NonNull ApplicationMode appMode, long measuredInterval) {
		measuredIntervalPref.setModeValue(appMode, measuredInterval);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		if (isUpdateNeeded() || isTimeToUpdate()) {
			lastUpdateTime = System.currentTimeMillis();
			updateAverageGlide();
		}
	}

	private void updateAverageGlide() {
		long measuredInterval = measuredIntervalPref.get();
		float averageVal = averageGlideComputer.getAverageGlideRatio(measuredInterval);
		if (Float.isNaN(averageVal)) {
			setText(NO_VALUE, null);
		} else {
			setText(format(averageVal), null);
		}
	}

	@Override
	public void copySettings(@NonNull ApplicationMode appMode, @Nullable String customId) {
		registerMeasuredIntervalPref(customId).setModeValue(appMode, measuredIntervalPref.getModeValue(appMode));
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
}
