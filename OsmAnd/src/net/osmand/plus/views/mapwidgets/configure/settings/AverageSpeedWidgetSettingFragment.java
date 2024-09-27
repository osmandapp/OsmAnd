package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.shared.settings.enums.SpeedConstants;
import net.osmand.plus.views.mapwidgets.utils.AverageSpeedComputer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.AverageSpeedWidget;

public class AverageSpeedWidgetSettingFragment extends BaseSimpleWidgetSettingsFragment {

	private static final String KEY_TIME_INTERVAL = "time_interval";
	private static final String KEY_COUNT_STOPS = "count_stops";

	private AverageSpeedWidget speedWidget;

	private long initialIntervalMillis;
	private boolean countStops;

	private TimeIntervalCard timeIntervalCard;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return WidgetType.AVERAGE_SPEED;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		MapWidgetInfo widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
		if (widgetInfo != null) {
			speedWidget = ((AverageSpeedWidget) widgetInfo.widget);

			long defaultInterval = speedWidget.getMeasuredInterval(appMode);
			boolean defaultCountStops = !speedWidget.shouldSkipStops(appMode); // pref ui was inverted

			countStops = bundle.getBoolean(KEY_COUNT_STOPS, defaultCountStops);
			initialIntervalMillis = bundle.getLong(KEY_TIME_INTERVAL, defaultInterval);
		} else {
			dismiss();
		}
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		themedInflater.inflate(R.layout.average_speed_widget_settings_fragment, container);

		setupIntervalSliderCard();
		setupSkipStopsSetting();
		themedInflater.inflate(R.layout.divider, container);
		super.setupContent(themedInflater, container);
		setupSettingAction(themedInflater, container);
	}

	private void setupIntervalSliderCard() {
		timeIntervalCard = new TimeIntervalCard(requireMyActivity(), initialIntervalMillis);
		ViewGroup cardContainer = view.findViewById(R.id.average_speed_interval_card_container);
		cardContainer.addView(timeIntervalCard.build(cardContainer.getContext()));
	}

	private void setupSkipStopsSetting() {
		View skipStopsContainer = view.findViewById(R.id.skip_stops_container);
		TextView skipStopsDesc = view.findViewById(R.id.skip_stops_desc);
		CompoundButton skipStopsToggle = view.findViewById(R.id.skip_stops_toggle);

		SpeedConstants speedSystem = settings.SPEED_SYSTEM.getModeValue(appMode);
		String speedToSkip = String.valueOf(AverageSpeedComputer.getConvertedSpeedToSkip(speedSystem));
		String speedUnit = speedSystem.toShortString();
		String formattedSpeedToSkip = getString(R.string.ltr_or_rtl_combine_via_space, speedToSkip, speedUnit);
		skipStopsDesc.setText(getString(R.string.average_speed_skip_stops_desc, formattedSpeedToSkip));

		skipStopsToggle.setChecked(countStops);
		skipStopsToggle.setOnCheckedChangeListener((buttonView, isChecked) -> countStops = isChecked);

		skipStopsContainer.setOnClickListener(v -> skipStopsToggle.setChecked(!skipStopsToggle.isChecked()));
		skipStopsContainer.setBackground(getPressedStateDrawable());
	}

	private void setupSettingAction(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		themedInflater.inflate(R.layout.divider, container);
		View actionView = themedInflater.inflate(R.layout.setting_action_button, null);
		actionView.setBackground(getPressedStateDrawable());
		actionView.setOnClickListener(v -> speedWidget.resetAverageSpeed());
		TextView title = actionView.findViewById(R.id.action_title);
		title.setText(R.string.reset_average_speed);
		container.addView(actionView);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(KEY_TIME_INTERVAL, timeIntervalCard.getSelectedIntervalMillis());
		outState.putBoolean(KEY_COUNT_STOPS, countStops);
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		speedWidget.setShouldSkipStops(appMode, !countStops);
		speedWidget.setMeasuredInterval(appMode, timeIntervalCard.getSelectedIntervalMillis());
	}
}