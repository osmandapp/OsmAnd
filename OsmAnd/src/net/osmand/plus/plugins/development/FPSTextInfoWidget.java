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
	private static final int HALF_FRAME_BUFFER_LENGTH = 20;
	private long startMs = 0;
	private int startFrameId;
	private long middleMs = 0;
	private int middleFrameId;

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
			if (startFrameId == 0 || (middleFrameId - startFrameId) > HALF_FRAME_BUFFER_LENGTH) {
				startMs = middleMs;
				startFrameId = middleFrameId;
			}
			if (middleFrameId == 0 || (frameId - middleFrameId) > HALF_FRAME_BUFFER_LENGTH) {
				middleMs = now;
				middleFrameId = frameId;
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
