package net.osmand.plus.views.mapwidgets.widgets;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class DistanceToPointWidget extends RightTextInfoWidget {

	private final OsmandMapTileView view;
	private final float[] calculations = new float[1];
	private int cachedMeters;

	public DistanceToPointWidget(@NonNull MapActivity mapActivity, @DrawableRes int dayIconId, @DrawableRes int nightIconId) {
		super(mapActivity);
		this.view = mapActivity.getMapView();
		if (dayIconId != 0 && nightIconId != 0) {
			setIcons(dayIconId, nightIconId);
		}
		setText(null, null);
		setOnClickListener(v -> click(view));
	}

	protected void click(final OsmandMapTileView view) {
		AnimateDraggingMapThread thread = view.getAnimatedDraggingThread();
		LatLon pointToNavigate = getPointToNavigate();
		if (pointToNavigate != null) {
			int fZoom = Math.max(view.getZoom(), 15);
			thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom, true);
		}
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		int d = getDistance();
		if (isUpdateNeeded() || RouteInfoWidgetsFactory.distChanged(cachedMeters, d)) {
			cachedMeters = d;
			if (cachedMeters <= 20) {
				cachedMeters = 0;
				setText(null, null);
			} else {
				String ds = OsmAndFormatter.getFormattedDistance(cachedMeters, view.getApplication());
				int ls = ds.lastIndexOf(' ');
				if (ls == -1) {
					setText(ds, null);
				} else {
					setText(ds.substring(0, ls), ds.substring(ls + 1));
				}
			}
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}

	public abstract LatLon getPointToNavigate();

	public int getDistance() {
		int d = 0;
		LatLon l = getPointToNavigate();
		if (l != null) {
			Location.distanceBetween(view.getLatitude(), view.getLongitude(), l.getLatitude(), l.getLongitude(), calculations);
			d = (int) calculations[0];
		}
		return d;
	}
}