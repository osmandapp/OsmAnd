package net.osmand.plus.plugins.development;

import static net.osmand.plus.views.mapwidgets.WidgetType.FPS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public class FPSTextInfoWidget extends TextInfoWidget {

	private final OsmandMapTileView mapView;

	public FPSTextInfoWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, FPS);
		this.mapView = mapActivity.getMapView();
		updateInfo(null);
		setIcons(FPS);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		if (!mapView.isMeasureFPS()) {
			mapView.setMeasureFPS(true);
		}
		setText("", (int) mapView.getFPS() + "/" + (int) mapView.getSecondaryFPS() + " FPS");
	}
}
