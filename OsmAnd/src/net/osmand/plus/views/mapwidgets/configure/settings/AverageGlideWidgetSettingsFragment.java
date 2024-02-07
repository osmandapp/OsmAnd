package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.GlideAverageWidget;

public class AverageGlideWidgetSettingsFragment extends BaseSimpleWidgetSettingsFragment {

	private static final String KEY_TIME_INTERVAL = "time_interval";

	private GlideAverageWidget widget;

	private long initialIntervalMillis;

	private TimeIntervalCard timeIntervalCard;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return WidgetType.GLIDE_AVERAGE;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		MapWidgetInfo widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
		if (widgetInfo != null) {
			widget = ((GlideAverageWidget) widgetInfo.widget);
			long defaultInterval = widget.getMeasuredInterval(appMode);
			initialIntervalMillis = bundle.getLong(KEY_TIME_INTERVAL, defaultInterval);
		} else {
			dismiss();
		}
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		themedInflater.inflate(R.layout.fragment_widget_settings_average_glide_ratio, container);

		timeIntervalCard = new TimeIntervalCard(requireMyActivity(), initialIntervalMillis);
		ViewGroup cardContainer = view.findViewById(R.id.time_interval_card_container);
		cardContainer.addView(timeIntervalCard.build(cardContainer.getContext()));
		themedInflater.inflate(R.layout.divider, container);
		super.setupContent(themedInflater, container);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(KEY_TIME_INTERVAL, timeIntervalCard.getSelectedIntervalMillis());
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		widget.setMeasuredInterval(appMode, timeIntervalCard.getSelectedIntervalMillis());
	}
}
