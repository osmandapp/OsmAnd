package net.osmand.plus.plugins.mapillary;

import static net.osmand.plus.views.mapwidgets.WidgetParams.MAPILLARY;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public class MapillaryMapWidget extends TextInfoWidget {

	public MapillaryMapWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity);

		setText(app.getString(R.string.mapillary), "");
		setIcons(MAPILLARY);
		setOnClickListener(v -> MapillaryPlugin.openMapillary(mapActivity, null));
	}
}
