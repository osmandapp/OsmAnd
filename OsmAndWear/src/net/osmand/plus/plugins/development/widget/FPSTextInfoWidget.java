package net.osmand.plus.plugins.development.widget;

import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_FPS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;

public class FPSTextInfoWidget extends SimpleWidget {

	private final OsmandMapTileView mapView;

	public FPSTextInfoWidget(@NonNull MapActivity activity, @Nullable String customId, @Nullable WidgetsPanel panel) {
		super(activity, DEV_FPS, customId, panel);
		this.mapView = activity.getMapView();
		updateSimpleWidgetInfo(null);
		setIcons(DEV_FPS);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		MapRendererView renderer = mapView.getMapRenderer();
		if (renderer != null) {
			float fps = mapView.calculateRenderFps();
			setText(OsmAndFormatter.formatFps(fps), "FPS");
		} else {
			if (!mapView.isMeasureFPS()) {
				mapView.setMeasureFPS(true);
			}
			setText(String.valueOf((int) mapView.getFPS()), (int) mapView.getSecondaryFPS() + " FPS");
		}
	}
}
