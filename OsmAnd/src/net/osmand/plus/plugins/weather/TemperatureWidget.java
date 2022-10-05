package net.osmand.plus.plugins.weather;

import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_TEMPERATURE_WIDGET;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public class TemperatureWidget extends TextInfoWidget {

	public TemperatureWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, WEATHER_TEMPERATURE_WIDGET);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {

	}
}
