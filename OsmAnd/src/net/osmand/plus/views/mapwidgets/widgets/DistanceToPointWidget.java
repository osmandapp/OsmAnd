package net.osmand.plus.views.mapwidgets.widgets;

import android.view.View;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory;

public abstract class DistanceToPointWidget extends TextInfoWidget {

	private final OsmandMapTileView view;
	private float[] calculations = new float[1];
	private int cachedMeters;

	public DistanceToPointWidget(MapActivity ma, int res, int resNight) {
		super(ma);
		this.view = ma.getMapView();
		if (res != 0 && resNight != 0) {
			setIcons(res, resNight);
		}
		setText(null, null);
		setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				click(view);
			}
		});
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
	public boolean updateInfo(OsmandMapLayer.DrawSettings drawSettings) {
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
			return true;
		}
		return false;
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