package net.osmand.plus.plugins.development.widget;

import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_FPS;

import android.view.View;

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
	private float cachedFps = -1f;
	private int cachedSecondaryFps = -1;

	public FPSTextInfoWidget(@NonNull MapActivity activity, @Nullable String customId, @Nullable WidgetsPanel panel) {
		super(activity, DEV_FPS, customId, panel);
		this.mapView = activity.getMapView();
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		updateSimpleWidgetInfo(null);
		setIcons(DEV_FPS);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		MapRendererView renderer = mapView.getMapRenderer();
		if (renderer != null) {
			float fps = mapView.calculateRenderFps();
			// Round to 1 decimal place to match "%.1f" formatting
			float roundedFps = Math.round(fps * 10f) / 10f;
			if (isUpdateNeeded() || cachedFps != roundedFps) {
				cachedFps = roundedFps;
				setText(OsmAndFormatter.formatFps(roundedFps), "FPS");
			}
		} else {
			if (!mapView.isMeasureFPS()) {
				mapView.setMeasureFPS(true);
			}
			int fps = (int) mapView.getFPS();
			int secondaryFps = (int) mapView.getSecondaryFPS();

			if (isUpdateNeeded() || cachedFps != fps || cachedSecondaryFps != secondaryFps) {
				cachedFps = fps;
				cachedSecondaryFps = secondaryFps;
				setText(String.valueOf(fps), secondaryFps + " FPS");
			}
		}
	}
}
