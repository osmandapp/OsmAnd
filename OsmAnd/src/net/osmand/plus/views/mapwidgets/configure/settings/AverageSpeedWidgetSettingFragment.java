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
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.AverageSpeedWidget;

public class AverageSpeedWidgetSettingFragment extends BaseSimpleWidgetInfoFragment {

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
	protected void setupMainContent(@NonNull ViewGroup container) {
		inflate(R.layout.average_speed_widget_settings_fragment, container);

		setupIntervalSliderCard();
		setupSkipStopsSetting();
		inflate(R.layout.divider, container);
		setupSettingAction(container);
	}

	private void setupIntervalSliderCard() {
		timeIntervalCard = new TimeIntervalCard(requireMyActivity(), initialIntervalMillis);
		ViewGroup cardContainer = view.findViewById(R.id.average_speed_interval_card_container);
		cardContainer.addView(timeIntervalCard.build(cardContainer.getContext()));
	}

	private void setupSkipStopsSetting() {
		View container = view.findViewById(R.id.skip_stops_container);
		TextView title = container.findViewById(R.id.title);
		TextView description = container.findViewById(R.id.description);
		CompoundButton compoundButton = container.findViewById(R.id.compound_button);

		SpeedConstants speedSystem = settings.SPEED_SYSTEM.getModeValue(appMode);
		String speedToSkip = String.valueOf(AverageSpeedComputer.getConvertedSpeedToSkip(speedSystem));
		String speedUnit = speedSystem.toShortString();
		String formattedSpeedToSkip = getString(R.string.ltr_or_rtl_combine_via_space, speedToSkip, speedUnit);
		title.setText(R.string.average_speed_skip_stops);
		description.setText(getString(R.string.average_speed_skip_stops_desc, formattedSpeedToSkip));

		compoundButton.setChecked(countStops);
		compoundButton.setOnCheckedChangeListener((buttonView, isChecked) -> countStops = isChecked);

		container.setOnClickListener(v -> compoundButton.setChecked(!compoundButton.isChecked()));
		container.setBackground(getPressedStateDrawable());
	}

	private void setupSettingAction(@NonNull ViewGroup container) {
		inflate(R.layout.divider, container, true);
		View actionView = inflate(R.layout.setting_action_button);
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