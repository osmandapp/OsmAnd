package net.osmand.plus.views.mapwidgets.widgets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.utils.AverageGlideComputer;
import net.osmand.plus.views.mapwidgets.utils.AverageSpeedComputer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

import java.util.Objects;

public class GlideAverageWidget extends GlideBaseWidget {

	private static final String MEASURED_INTERVAL_PREF_ID = "average_glide_measured_interval_millis";

	private final AverageGlideComputer averageGlideComputer;
	private final CommonPreference<Long> measuredIntervalPref;

	private String cachedFormattedGlideRatio = null;

	public GlideAverageWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, WidgetType.GLIDE_AVERAGE, customId, widgetsPanel);
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
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		if (isTimeToUpdate()) {
			long measuredInterval = measuredIntervalPref.get();
			String ratio = averageGlideComputer.getFormattedAverageGlideRatio(measuredInterval);
			if (!Objects.equals(ratio, cachedFormattedGlideRatio)) {
				cachedFormattedGlideRatio = ratio;
				if (Algorithms.isEmpty(ratio)) {
					setText(NO_VALUE, null);
				} else {
					setText(ratio, null);
				}
			}
		}
	}

	@Override
	public void copySettings(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copySettingsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copySettingsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		super.copySettingsFromMode(sourceAppMode, appMode, customId);
		registerMeasuredIntervalPref(customId).setModeValue(appMode, measuredIntervalPref.getModeValue(sourceAppMode));
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
