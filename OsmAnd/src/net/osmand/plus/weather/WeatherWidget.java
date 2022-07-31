package net.osmand.plus.weather;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

import org.jetbrains.annotations.NotNull;

public class WeatherWidget extends TextInfoWidget {

	public WeatherWidget(@NonNull MapActivity mapActivity, @Nullable WidgetType widgetType) {
		super(mapActivity, widgetType);
	}

}
