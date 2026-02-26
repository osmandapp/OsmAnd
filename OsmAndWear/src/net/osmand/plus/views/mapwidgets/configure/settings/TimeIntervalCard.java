package net.osmand.plus.views.mapwidgets.configure.settings;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.utils.AverageValueComputer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TimeIntervalCard extends BaseCard {

	private final Map<Long, String> availableIntervals;

	private long selectedIntervalMillis;

	public TimeIntervalCard(@NonNull FragmentActivity activity, long initialIntervalMillis) {
		super(activity);
		selectedIntervalMillis = initialIntervalMillis;
		availableIntervals = collectAvailableIntervals();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_select_time_interval;
	}

	public long getSelectedIntervalMillis() {
		return selectedIntervalMillis;
	}

	@Override
	protected void updateContent() {
		setupIntervalSlider();
		setupMinMaxIntervals();
	}

	private void setupIntervalSlider() {
		TextView interval = view.findViewById(R.id.interval);
		TextView selectedIntervalText = view.findViewById(R.id.selected_interval);
		Slider slider = view.findViewById(R.id.interval_slider);

		List<Entry<Long, String>> intervalsList = new ArrayList<>(availableIntervals.entrySet());
		int initialIntervalIndex = getInitialIntervalIndex();

		slider.setValueFrom(0);
		slider.setValueTo(availableIntervals.size() - 1);
		slider.setValue(initialIntervalIndex);
		slider.clearOnChangeListeners();
		slider.addOnChangeListener((slider1, intervalIndex, fromUser) -> {
			Entry<Long, String> newInterval = intervalsList.get((int) intervalIndex);
			selectedIntervalMillis = newInterval.getKey();
			selectedIntervalText.setText(newInterval.getValue());
		});
		UiUtilities.setupSlider(slider, nightMode, ColorUtilities.getActiveColor(app, nightMode), true);

		String intervalStr = app.getString(R.string.shared_string_interval);
		interval.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon, intervalStr, ""));
		selectedIntervalText.setText(intervalsList.get(initialIntervalIndex).getValue());
	}

	private int getInitialIntervalIndex() {
		List<Long> intervals = new ArrayList<>(availableIntervals.keySet());
		for (int i = 0; i < intervals.size(); i++) {
			long interval = intervals.get(i);
			if (selectedIntervalMillis == interval) {
				return i;
			}
		}
		return 0;
	}

	protected void setupMinMaxIntervals() {
		List<String> intervals = new ArrayList<>(availableIntervals.values());
		String minIntervalValue = intervals.get(0);
		String maxIntervalValue = intervals.get(intervals.size() - 1);

		TextView minInterval = view.findViewById(R.id.min_interval);
		TextView maxInterval = view.findViewById(R.id.max_interval);

		minInterval.setText(minIntervalValue);
		maxInterval.setText(maxIntervalValue);
	}

	@NonNull
	private Map<Long, String> collectAvailableIntervals() {
		Map<Long, String> intervals = new LinkedHashMap<>();
		for (long interval : AverageValueComputer.MEASURED_INTERVALS) {
			boolean seconds = interval < 60 * 1000;
			String timeInterval = seconds
					? String.valueOf(interval / 1000)
					: String.valueOf(interval / 1000 / 60);
			String timeUnit = interval < 60 * 1000
					? app.getString(R.string.shared_string_sec)
					: app.getString(R.string.shared_string_minute_lowercase);
			String formattedInterval = app.getString(R.string.ltr_or_rtl_combine_via_space, timeInterval, timeUnit);
			intervals.put(interval, formattedInterval);
		}
		return intervals;
	}
}