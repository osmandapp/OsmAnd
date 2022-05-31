package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
import net.osmand.plus.settings.enums.SpeedConstants;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.AverageSpeedComputer;
import net.osmand.plus.views.mapwidgets.WidgetParams;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import androidx.annotation.NonNull;

public class AverageSpeedWidgetSettingFragment extends WidgetSettingsBaseFragment {

	private static final String KEY_TIME_INTERVAL = "time_interval";
	private static final String KEY_SKIP_STOPS = "skip_stops";

	private long selectedIntervalMillis;
	private boolean skipStops;

	@NonNull
	@Override
	public WidgetParams getWidget() {
		return WidgetParams.AVERAGE_SPEED;
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

		setupIntervalsSlider();
		setupMinMaxIntervals();
		setupSkipStopsSetting();
	}

	private void setupIntervalsSlider() {
		TextView interval = view.findViewById(R.id.interval);
		TextView selectedIntervalText = view.findViewById(R.id.selected_interval);
		Slider slider = view.findViewById(R.id.interval_slider);

		Map<Long, String> intervals = getAvailableIntervals();
		List<Entry<Long, String>> intervalsList = new ArrayList<>(intervals.entrySet());
		int initialIntervalIndex = getInitialIntervalIndex();

		slider.setValueFrom(0);
		slider.setValueTo(intervals.size() - 1);
		slider.setValue(initialIntervalIndex);
		slider.clearOnChangeListeners();
		slider.addOnChangeListener((slider1, intervalIndex, fromUser) -> {
			Entry<Long, String> newInterval = intervalsList.get((int) intervalIndex);
			selectedIntervalMillis = newInterval.getKey();
			selectedIntervalText.setText(newInterval.getValue());
		});
		UiUtilities.setupSlider(slider, nightMode, ColorUtilities.getActiveColor(app, nightMode), true);

		interval.setText(getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.shared_string_interval), ""));
		selectedIntervalText.setText(intervalsList.get(initialIntervalIndex).getValue());
	}

	private int getInitialIntervalIndex() {
		List<Long> intervals = new ArrayList<>(getAvailableIntervals().keySet());
		for (int i = 0; i < intervals.size(); i++) {
			long interval = intervals.get(i);
			if (selectedIntervalMillis == interval) {
				return i;
			}
		}

		return 0;
	}

	private void setupMinMaxIntervals() {
		List<String> intervals = new ArrayList<>(getAvailableIntervals().values());
		String minIntervalValue = intervals.get(0);
		String maxIntervalValue = intervals.get(intervals.size() - 1);

		TextView minInterval = view.findViewById(R.id.min_interval);
		TextView maxInterval = view.findViewById(R.id.max_interval);

		minInterval.setText(minIntervalValue);
		maxInterval.setText(maxIntervalValue);
	}

	@NonNull
	private Map<Long, String> getAvailableIntervals() {
		Map<Long, String> intervals = new LinkedHashMap<>();
		for (long interval : AverageSpeedComputer.MEASURED_INTERVALS) {
			boolean seconds = interval < 60 * 1000;
			String timeInterval = seconds
					? String.valueOf(interval / 1000)
					: String.valueOf(interval / 1000 / 60);
			String timeUnit = interval < 60 * 1000
					? getString(R.string.shared_string_sec)
					: getString(R.string.shared_string_minute_lowercase);
			intervals.put(interval, getString(R.string.ltr_or_rtl_combine_via_space, timeInterval, timeUnit));
		}
		return intervals;
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