package net.osmand.plus.keyevent;

import android.view.MotionEvent;

import androidx.annotation.NonNull;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

public class TrackballController {

	private final MapActivity activity;

	public TrackballController(@NonNull MapActivity activity) {
		this.activity = activity;
	}

	public boolean onTrackballEvent(@NonNull MotionEvent event) {
		OsmandApplication app = activity.getMyApplication();
		if (app.getSettings().USE_TRACKBALL_FOR_MOVEMENTS.get()) {
			OsmandMapTileView mapView = app.getOsmandMap().getMapView();
			MapRendererView mapRenderer = mapView.getMapRenderer();
			int action = event.getAction();
			if (action == MotionEvent.ACTION_DOWN) {
				if (mapRenderer != null) {
					mapRenderer.suspendSymbolsUpdate();
				}
			} else if (action == MotionEvent.ACTION_MOVE) {
				onTrackballMove(app, event.getX(), event.getY());
				return true;
			} else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
				if (mapRenderer != null) {
					mapRenderer.resumeSymbolsUpdate();
				}
			}
		}
		return false;
	}

	private void onTrackballMove(@NonNull OsmandApplication app, float moveX, float moveY) {
		float dx = moveX * 15;
		float dy = moveY * 15;

		OsmandMap osmandMap = app.getOsmandMap();
		OsmandMapTileView mapView = osmandMap.getMapView();
		RotatedTileBox tileBox = mapView.getCurrentRotatedTileBox();
		QuadPoint cp = tileBox.getCenterPixelPoint();
		MapRendererView mapRenderer = mapView.getMapRenderer();
		LatLon newCenterLatLon;
		if (mapRenderer != null) {
			PointI point31 = new PointI();
			PointI pixel = new PointI((int) (cp.x + dx), (int) (cp.y + dy));
			boolean ok = mapRenderer.getLocationFromScreenPoint(pixel, point31);
			if (!ok) {
				return;
			}

			PointI target31 = mapRenderer.getState().getTarget31();
			int deltaX = point31.getX() - target31.getX();
			int deltaY = point31.getY() - target31.getY();
			PointI mapTarget31 = mapRenderer.getState().getFixedLocation31();
			int nextTargetX = mapTarget31.getX();
			int nextTargetY = mapTarget31.getY();
			if (Integer.MAX_VALUE - nextTargetX < deltaX) {
				deltaX -= Integer.MAX_VALUE;
				deltaX--;
			}
			if (Integer.MAX_VALUE - nextTargetY < deltaY) {
				deltaY -= Integer.MAX_VALUE;
				deltaY--;
			}
			nextTargetX += deltaX;
			nextTargetY += deltaY;
			if (nextTargetX < 0) {
				nextTargetX += Integer.MAX_VALUE;
				nextTargetX++;
			}
			if (nextTargetY < 0) {
				nextTargetY += Integer.MAX_VALUE;
				nextTargetY++;
			}
			newCenterLatLon = new LatLon(MapUtils.get31LatitudeY(nextTargetY), MapUtils.get31LongitudeX(nextTargetX));
		} else {
			newCenterLatLon = NativeUtilities.getLatLonFromPixel(null, tileBox,
					cp.x + dx, cp.y + dy);
		}
		osmandMap.setMapLocation(newCenterLatLon.getLatitude(), newCenterLatLon.getLongitude());
	}
}
