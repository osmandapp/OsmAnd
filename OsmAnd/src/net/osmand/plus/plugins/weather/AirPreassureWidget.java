package net.osmand.plus.plugins.weather;

import static net.osmand.plus.views.mapwidgets.WidgetType.WEATHER_AIR_PRESSURE_WIDGET;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public class AirPreassureWidget extends TextInfoWidget {

	public AirPreassureWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, WEATHER_AIR_PRESSURE_WIDGET);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {

	}
}