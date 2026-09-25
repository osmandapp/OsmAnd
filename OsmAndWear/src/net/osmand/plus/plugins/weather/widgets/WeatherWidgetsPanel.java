package net.osmand.plus.plugins.weather.widgets;

import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_CLOUD;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRECIPITATION;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRESSURE;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_TEMPERATURE;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_WIND_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_AIR_PRESSURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_CLOUDS_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_PRECIPITATION_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_TEMPERATURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_WIND_WIDGET;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.WidgetsContainer;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WeatherWidgetsPanel extends LinearLayout implements WidgetsContainer {
	private final List<WeatherWidget> weatherWidgets = new ArrayList<>();
	public boolean nightMode;

	public WeatherWidgetsPanel(@NonNull Context context) {
		this(context, null);
	}

	public WeatherWidgetsPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WeatherWidgetsPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public WeatherWidgetsPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public void setupWidgets(@NonNull MapActivity activity, boolean isNightMode) {
		removeAllViews();
		createWidgets(activity);
		for (WeatherWidget widget : weatherWidgets) {
			View view = widget.getView();
			LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
					0,
					LayoutParams.MATCH_PARENT,
					1.0f
			);
			view.setLayoutParams(param);
			addView(view);
			View divider = UiUtilities.getInflater(activity, isNightMode).inflate(R.layout.vertical_divider, null);
			LinearLayout.LayoutParams dividerParam = new LinearLayout.LayoutParams(
					1,
					LayoutParams.MATCH_PARENT
			);
			divider.setLayoutParams(dividerParam);
			addView(divider);
		}
	}

	private void createWidgets(@NonNull MapActivity activity) {
		weatherWidgets.add(createCustomLayoutWidgetForParams(activity, WEATHER_TEMPERATURE_WIDGET, WidgetType.getDuplicateWidgetId(WEATHER_TEMPERATURE_WIDGET), R.layout.widget_custom_vertical));
		weatherWidgets.add(createCustomLayoutWidgetForParams(activity, WEATHER_AIR_PRESSURE_WIDGET, WidgetType.getDuplicateWidgetId(WEATHER_AIR_PRESSURE_WIDGET), R.layout.widget_custom_vertical));
		weatherWidgets.add(createCustomLayoutWidgetForParams(activity, WEATHER_WIND_WIDGET, WidgetType.getDuplicateWidgetId(WEATHER_WIND_WIDGET), R.layout.widget_custom_vertical));
		weatherWidgets.add(createCustomLayoutWidgetForParams(activity, WEATHER_PRECIPITATION_WIDGET, WidgetType.getDuplicateWidgetId(WEATHER_PRECIPITATION_WIDGET), R.layout.widget_custom_vertical));
		weatherWidgets.add(createCustomLayoutWidgetForParams(activity, WEATHER_CLOUDS_WIDGET, WidgetType.getDuplicateWidgetId(WEATHER_CLOUDS_WIDGET), R.layout.widget_custom_vertical));
	}

	public WeatherWidget createCustomLayoutWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @LayoutRes int customLayoutId) {
		switch (widgetType) {
			case WEATHER_TEMPERATURE_WIDGET:
				return new CustomWeatherWidget(mapActivity, widgetType, customId, WEATHER_BAND_TEMPERATURE);
			case WEATHER_PRECIPITATION_WIDGET:
				return new CustomWeatherWidget(mapActivity, widgetType, customId, WEATHER_BAND_PRECIPITATION);
			case WEATHER_WIND_WIDGET:
				return new CustomWeatherWidget(mapActivity, widgetType, customId, WEATHER_BAND_WIND_SPEED);
			case WEATHER_CLOUDS_WIDGET:
				return new CustomWeatherWidget(mapActivity, widgetType, customId, WEATHER_BAND_CLOUD);
			case WEATHER_AIR_PRESSURE_WIDGET:
				return new CustomWeatherWidget(mapActivity, widgetType, customId, WEATHER_BAND_PRESSURE);
		}
		return null;
	}

	public void setSelectedDate(@Nullable Date date) {
		for (WeatherWidget widget : weatherWidgets) {
			widget.setDateTime(date);
		}
	}

	@Override
	public void update(DrawSettings drawSettings) {
		for (WeatherWidget widget : weatherWidgets) {
			widget.updateInfo(drawSettings);
		}
	}

	@Override
	public void updateColors(@NonNull TextState textState) {
		for (WeatherWidget widget : weatherWidgets) {
			View widgetView = widget.getView();
			textState.night = nightMode;
			widget.updateColors(textState);
			widgetView.findViewById(R.id.widget_bg).setBackgroundResource(nightMode ? R.color.list_background_color_dark : R.color.widget_background_color_light);
			((TextView) widgetView.findViewById(R.id.widget_text)).setTextColor(ColorUtilities.getPrimaryTextColor(getContext(), nightMode));
			((TextView) widgetView.findViewById(R.id.widget_text_small)).setTextColor(ColorUtilities.getSecondaryTextColor(getContext(), nightMode));
		}
	}
}
