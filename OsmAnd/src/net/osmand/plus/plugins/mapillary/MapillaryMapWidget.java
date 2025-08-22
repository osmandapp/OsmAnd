package net.osmand.plus.plugins.mapillary;

import static net.osmand.plus.views.mapwidgets.WidgetType.MAPILLARY;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;

public class MapillaryMapWidget extends SimpleWidget {

	public MapillaryMapWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, MAPILLARY, customId, widgetsPanel);

		setText(app.getString(R.string.mapillary), "");
		setIcons(MAPILLARY);
	}

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> MapillaryPlugin.openMapillary(mapActivity, null);
	}
}
