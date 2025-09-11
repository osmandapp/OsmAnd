package net.osmand.plus.plugins.parking;

import static net.osmand.plus.views.mapwidgets.WidgetType.PARKING;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;

/**
 * control that shows a distance between
 * the current position on the map
 * and the location of the parked car
 */
public class ParkingMapWidget extends SimpleWidget {

	private final ParkingPositionPlugin plugin;

	private final float[] calculations = new float[1];
	private int cachedMeters;

	public ParkingMapWidget(@NonNull ParkingPositionPlugin plugin, @NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, PARKING, customId, widgetsPanel);
		this.plugin = plugin;

		setText(null, null);
		setIcons(PARKING);
		updateInfo(null);
	}

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> {
			OsmandMapTileView view = mapActivity.getMapView();
			AnimateDraggingMapThread thread = view.getAnimatedDraggingThread();
			LatLon parkingPoint = plugin.getParkingPosition();
			if (parkingPoint != null) {
				int fZoom = Math.max(view.getZoom(), 15);
				thread.startMoving(parkingPoint.getLatitude(), parkingPoint.getLongitude(), fZoom);
			}
		};
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		LatLon point = plugin.getParkingPosition();
		if (point != null && !app.getRoutingHelper().isFollowingMode()) {
			OsmandMapTileView view = mapActivity.getMapView();
			Location.distanceBetween(view.getLatitude(), view.getLongitude(), point.getLatitude(), point.getLongitude(), calculations);
			int d = (int) calculations[0];
			if (isUpdateNeeded() || distChanged(cachedMeters, d)) {
				cachedMeters = d;
				if (cachedMeters <= 20) {
					cachedMeters = 0;
					setText(null, null);
				} else {
					String distance = OsmAndFormatter.getFormattedDistance(cachedMeters, mapActivity.getApp());
					int ls = distance.lastIndexOf(' ');
					if (ls == -1) {
						setText(distance, null);
					} else {
						setText(distance.substring(0, ls), distance.substring(ls + 1));
					}
				}
			}
		} else if (cachedMeters != 0) {
			cachedMeters = 0;
			setText(null, null);
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}

	private boolean distChanged(int oldDist, int dist) {
		return oldDist == 0 || Math.abs(oldDist - dist) >= 30;
	}
}