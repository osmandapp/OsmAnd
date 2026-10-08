package net.osmand.plus.views.mapwidgets.widgets;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.utils.AverageGlideComputer;
import net.osmand.plus.views.mapwidgets.utils.AverageSpeedComputer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgetstates.GlideAverageWidgetState;
import net.osmand.util.Algorithms;

import java.util.Objects;

public class GlideAverageWidget extends GlideBaseWidget {

	private static final String MEASURED_INTERVAL_PREF_ID = "average_glide_measured_interval_millis";

	private final GlideAverageWidgetState widgetState;
	private final AverageGlideComputer averageGlideComputer;
	private final CommonPreference<Long> measuredIntervalPref;

	private String cachedText = null;
	private boolean forceUpdate; // becomes 'true' when widget state switched

	public GlideAverageWidget(@NonNull MapActivity mapActivity, @NonNull GlideAverageWidgetState widgetState, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, WidgetType.GLIDE_AVERAGE, customId, widgetsPanel);
		this.widgetState = widgetState;
		averageGlideComputer = app.getAverageGlideComputer();
		measuredIntervalPref = registerMeasuredIntervalPref(customId);
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		updateModeIcons();
		updateInfo(null);
		updateWidgetName();
	}

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> {
			forceUpdate = true;
			widgetState.changeToNextState();
			updateModeIcons();
			updateInfo(null);
			updateWidgetName();
		};
	}

	@Nullable
	@Override
	protected String getWidgetName() {
		if (widgetState != null) {
			return getString(isInVerticalSpeedState() ? R.string.average_vertical_speed : R.string.average_glide_ratio);
		}
		return widgetType != null ? getString(widgetType.titleId) : null;
	}

	@Override
	public GlideAverageWidgetState getWidgetState() {
		return widgetState;
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
		if (forceUpdate || isTimeToUpdate()) {
			markUpdated();
			long measuredInterval = measuredIntervalPref.get();
			FormattedValue value = isInVerticalSpeedState()
					? formatVerticalSpeed(averageGlideComputer.getAverageVerticalSpeed(measuredInterval))
					: formatRatio(averageGlideComputer.getFormattedAverageGlideRatio(measuredInterval));
			String text = value != null ? value.value + " " + value.unit : null;
			if (forceUpdate || !Objects.equals(text, cachedText)) {
				cachedText = text;
				if (value == null) {
					setText(NO_VALUE, null);
				} else {
					setText(value.value, value.unit);
				}
			}
			forceUpdate = false;
		}
	}

	@Nullable
	private FormattedValue formatRatio(@Nullable String ratio) {
		return Algorithms.isEmpty(ratio) ? null : new FormattedValue(0, ratio, null);
	}

	@Nullable
	private FormattedValue formatVerticalSpeed(@Nullable Double metersPerSecond) {
		if (metersPerSecond == null) {
			return null;
		}
		String unit;
		float speed;
		if (settings.ALTITUDE_METRIC.get().shouldUseFeet()) {
			speed = (float) (metersPerSecond * OsmAndFormatter.FEET_IN_ONE_METER);
			unit = getString(R.string.ltr_or_rtl_combine_via_slash, getString(R.string.foot), getString(R.string.shared_string_sec));
		} else {
			speed = metersPerSecond.floatValue();
			unit = getString(R.string.m_s);
		}
		return OsmAndFormatter.formatValue(speed, unit, true, 1, app);
	}

	private void updateModeIcons() {
		if (isInVerticalSpeedState()) {
			setIcons(R.drawable.widget_vertical_average_speed_day, R.drawable.widget_vertical_average_speed_night);
		} else {
			setIcons(widgetType);
		}
	}

	public boolean isInVerticalSpeedState() {
		return widgetState.getPreference().get();
	}

	@Override
	public void copySettings(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copySettingsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copySettingsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		super.copySettingsFromMode(sourceAppMode, appMode, customId);
		widgetState.copyPrefsFromMode(sourceAppMode, appMode, customId);
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
