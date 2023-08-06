package net.osmand.plus.plugins.weather.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_AIR_PRESSURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_CLOUDS_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_PRECIPITATION_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_TEMPERATURE_WIDGET;
import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_WIND_WIDGET;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.views.controls.SideWidgetsPanel;
import net.osmand.plus.views.controls.WidgetsPagerAdapter;
import net.osmand.plus.views.controls.WidgetsPagerAdapter.VisiblePages;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class WeatherWidgetsPanel extends SideWidgetsPanel {

	private final WeatherPlugin plugin;
	private final List<WeatherWidget> weatherWidgets = new ArrayList<>();

	private VisiblePages visiblePages;

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
		plugin = PluginsHelper.getPlugin(WeatherPlugin.class);
	}

	public void setupWidgets(@NonNull MapActivity activity) {
		createWidgets(activity);

		List<View> views = new ArrayList<>(weatherWidgets.size());
		for (WeatherWidget widget : weatherWidgets) {
			views.add(widget.getView());
		}
		visiblePages = new VisiblePages(views);
	}

	private void createWidgets(@NonNull MapActivity activity) {
		weatherWidgets.add(plugin.createMapWidgetForParams(activity, WEATHER_TEMPERATURE_WIDGET));
		weatherWidgets.add(plugin.createMapWidgetForParams(activity, WEATHER_AIR_PRESSURE_WIDGET));
		weatherWidgets.add(plugin.createMapWidgetForParams(activity, WEATHER_WIND_WIDGET));
		weatherWidgets.add(plugin.createMapWidgetForParams(activity, WEATHER_PRECIPITATION_WIDGET));
		weatherWidgets.add(plugin.createMapWidgetForParams(activity, WEATHER_CLOUDS_WIDGET));
	}

	public void setSelectedDate(@Nullable Date date) {
		for (WeatherWidget widget : weatherWidgets) {
			widget.setDateTime(date);
		}
	}

	@Override
	public void update(DrawSettings drawSettings) {
		super.update(drawSettings);

		for (WeatherWidget widget : weatherWidgets) {
			widget.updateInfo(drawSettings);
		}
	}

	@Override
	public void updateColors(@NonNull TextState textState) {
		super.updateColors(textState);

		for (WeatherWidget widget : weatherWidgets) {
			widget.updateColors(textState);
		}
	}

	@Override
	protected WidgetsPagerAdapter createPagerAdapter() {
		return new WidgetsPagerAdapter(getMyApplication(), rightSide ? WidgetsPanel.RIGHT : WidgetsPanel.LEFT) {
			@NonNull
			@Override
			public VisiblePages collectVisiblePages() {
				return visiblePages != null ? visiblePages : new VisiblePages(Collections.emptyMap());
			}
		};
	}
}
