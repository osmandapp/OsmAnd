package net.osmand.plus.plugins.development.widget;

import android.os.SystemClock;

import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_FPS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.core.android.MapRendererView;

public class FPSTextInfoWidget extends SimpleWidget {

	private final OsmandMapTileView mapView;
	private static final int HALF_FRAME_BUFFER_LENGTH = 20;
	private long startMs = 0;
	private int startFrameId;
	private long middleMs = 0;
	private int middleFrameId;

	public FPSTextInfoWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, DEV_FPS, customId, widgetsPanel);
		this.mapView = mapActivity.getMapView();
		updateSimpleWidgetInfo(null);
		setIcons(DEV_FPS);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		MapRendererView mv = mapView.getMapRenderer();
		if (mv != null) {
			int frameId = mv.getFrameId();
			long now = SystemClock.elapsedRealtime();
			String fps = "-";
			if (frameId > startFrameId && now > startMs && startMs != 0) {
				fps = String.format("%.1f",
						1000.0 / (now - startMs) * (frameId - startFrameId));
			}
			if (startFrameId == 0 || (middleFrameId - startFrameId) > HALF_FRAME_BUFFER_LENGTH) {
				startMs = middleMs;
				startFrameId = middleFrameId;
			}
			if (middleFrameId == 0 || (frameId - middleFrameId) > HALF_FRAME_BUFFER_LENGTH) {
				middleMs = now;
				middleFrameId = frameId;
			}


			setText(fps, "FPS");
		} else {
			if (!mapView.isMeasureFPS()) {
				mapView.setMeasureFPS(true);
			}
			setText((int) mapView.getFPS() + "", (int) mapView.getSecondaryFPS() + " FPS");
		}
	}
}
