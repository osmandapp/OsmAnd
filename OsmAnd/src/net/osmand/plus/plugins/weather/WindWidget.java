package net.osmand.plus.plugins.weather;

import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_WIND_WIDGET;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public class WindWidget extends TextInfoWidget {

	public WindWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, WEATHER_WIND_WIDGET);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {

	}
}