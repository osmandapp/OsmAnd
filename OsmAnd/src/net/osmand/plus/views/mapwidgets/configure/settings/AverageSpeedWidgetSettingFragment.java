package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.enums.SpeedConstants;
import net.osmand.plus.views.mapwidgets.AverageSpeedComputer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.AverageSpeedWidget;

public class AverageSpeedWidgetSettingFragment extends WidgetSettingsBaseFragment {

	private static final String KEY_TIME_INTERVAL = "time_interval";
	private static final String KEY_SKIP_STOPS = "skip_stops";

	private AverageSpeedWidget speedWidget;

	private long initialIntervalMillis;
	private boolean skipStops;

	private AverageSpeedIntervalCard averageSpeedIntervalCard;

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
			boolean defaultSkipStops = speedWidget.shouldSkipStops(appMode);

			initialIntervalMillis = bundle.getLong(KEY_TIME_INTERVAL, defaultInterval);
			skipStops = bundle.getBoolean(KEY_SKIP_STOPS, defaultSkipStops);
		} else {
			dismiss();
		}
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		themedInflater.inflate(R.layout.average_speed_widget_settings_fragment, container);

		setupIntervalSliderCard();
		setupSkipStopsSetting();
	}

	private void setupIntervalSliderCard() {
		averageSpeedIntervalCard = new AverageSpeedIntervalCard(requireMyActivity(), initialIntervalMillis);
		ViewGroup cardContainer = view.findViewById(R.id.average_speed_interval_card_container);
		cardContainer.addView(averageSpeedIntervalCard.build(cardContainer.getContext()));
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
		outState.putLong(KEY_TIME_INTERVAL, averageSpeedIntervalCard.getSelectedIntervalMillis());
		outState.putBoolean(KEY_SKIP_STOPS, skipStops);
	}

	@Override
	protected void applySettings() {
		speedWidget.setShouldSkipStops(appMode, skipStops);
		speedWidget.setMeasuredInterval(appMode, averageSpeedIntervalCard.getSelectedIntervalMillis());
	}
}