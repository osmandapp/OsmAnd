package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.settings.enums.SpeedConstants;
import net.osmand.plus.views.mapwidgets.AverageSpeedComputer;
import net.osmand.plus.views.mapwidgets.WidgetType;

import androidx.annotation.NonNull;

public class AverageSpeedWidgetSettingFragment extends BaseAverageSpeedSettingFragment {

	private static final String KEY_TIME_INTERVAL = "time_interval";
	private static final String KEY_SKIP_STOPS = "skip_stops";

	private boolean skipStops;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return WidgetType.AVERAGE_SPEED;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);

		long defaultInterval = settings.AVERAGE_SPEED_MEASURED_INTERVAL_MILLIS.getModeValue(appMode);
		boolean defaultSkipStops = settings.AVERAGE_SPEED_SKIP_STOPS.getModeValue(appMode);

		selectedIntervalMillis = bundle.getLong(KEY_TIME_INTERVAL, defaultInterval);
		skipStops = bundle.getBoolean(KEY_SKIP_STOPS, defaultSkipStops);
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		themedInflater.inflate(R.layout.average_speed_widget_settings_fragment, container);

		setupIntervalSlider();
		setupMinMaxIntervals();
		setupSkipStopsSetting();
	}

	private void setupSkipStopsSetting() {
		View skipStopsContainer = view.findViewById(R.id.skip_stops_container);
		TextView skipStopsDesc = view.findViewById(R.id.skip_stops_desc);
		CompoundButton skipStopsToggle = view.findViewById(R.id.skip_stops_toggle);

		SpeedConstants speedSystem = settings.SPEED_SYSTEM.getModeValue(appMode);
		String speedToSkip = String.valueOf(AverageSpeedComputer.getConvertedSpeedToSkip(speedSystem));
		String speedUnit = speedSystem.toShortString(app);
		String formattedSpeedToSkip = getString(R.string.ltr_or_rtl_combine_via_space, speedToSkip, speedUnit);
		skipStopsDesc.setText(getString(R.string.average_speed_skip_stops_desc, formattedSpeedToSkip));

		skipStopsToggle.setChecked(skipStops);
		skipStopsToggle.setOnCheckedChangeListener((buttonView, isChecked) -> skipStops = isChecked);

		skipStopsContainer.setOnClickListener(v -> skipStopsToggle.setChecked(!skipStopsToggle.isChecked()));
		skipStopsContainer.setBackground(getPressedStateDrawable());
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(KEY_TIME_INTERVAL, selectedIntervalMillis);
		outState.putBoolean(KEY_SKIP_STOPS, skipStops);
	}

	@Override
	protected void applySettings() {
		settings.AVERAGE_SPEED_MEASURED_INTERVAL_MILLIS.setModeValue(appMode, selectedIntervalMillis);
		settings.AVERAGE_SPEED_SKIP_STOPS.setModeValue(appMode, skipStops);
	}
}