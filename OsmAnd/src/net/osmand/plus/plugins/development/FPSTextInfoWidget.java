package net.osmand.plus.plugins.development;

import android.os.SystemClock;

import static net.osmand.plus.views.mapwidgets.WidgetType.FPS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.core.android.MapRendererView;

public class FPSTextInfoWidget extends TextInfoWidget {

	private final OsmandMapTileView mapView;
	private static final int FRAME_BUFFER_LENGTH = 20;
	private long startMs = 0;
	private int startFrameId;
	private long swapMs = 0;
	private int swapFrameId;

	public FPSTextInfoWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, FPS);
		this.mapView = mapActivity.getMapView();
		updateInfo(null);
		setIcons(FPS);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		MapRendererView mv = mapView.getMapRenderer();
		if (mv != null) {
			int frameId = mv.getFrameId();
			long now = SystemClock.elapsedRealtime();
			String fps = "- FPS";
			if (frameId > startFrameId && now > startMs && startMs != 0) {
				fps = String.format("%.1f FPS",
						1000.0 / (now - startMs) * (frameId - startFrameId));
			}
			if (startMs == 0) {
				startMs = now;
				startFrameId = frameId;
			} else if (swapMs == 0 && (frameId - startFrameId) > FRAME_BUFFER_LENGTH) {
				swapMs = now;
				swapFrameId = frameId;
			} else if (swapMs == 0 && (frameId - swapFrameId) > FRAME_BUFFER_LENGTH) {
				startMs = swapMs;
				startFrameId = swapFrameId;
				swapMs = now;
				swapFrameId = frameId;
			}

			setText("", fps);
		} else {
			if (!mapView.isMeasureFPS()) {
				mapView.setMeasureFPS(true);
			}
			setText("", (int) mapView.getFPS() + "/" + (int) mapView.getSecondaryFPS() + " FPS");
		}
	}
}
