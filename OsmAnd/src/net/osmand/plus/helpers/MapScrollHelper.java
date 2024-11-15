package net.osmand.plus.helpers;

import androidx.annotation.NonNull;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointD;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MapScrollHelper {

	private static final int LONG_PRESS_TIME_MS = 250;
	private static final int MAX_KEY_UP_TIME_MS = 10;
	private static final int SCROLL_PAUSE_MS = 3;

	private static final int SHORT_SHIFT_DELTA = 1;
	private static final int LONG_SHIFT_DELTA = 200;

	private final OsmandMap osmandMap;
	private final OsmandMapTileView mapView;

	private final Set<ScrollDirection> activeDirections = new HashSet<>();
	private final Map<ScrollDirection, Long> cancelDirectionTime = new HashMap<>();

	private volatile boolean isContinuousScrolling;

	public enum ScrollDirection {
		UP, DOWN, LEFT, RIGHT
	}

	public MapScrollHelper(@NonNull OsmandApplication app) {
		osmandMap = app.getOsmandMap();
		mapView = osmandMap.getMapView();
	}

	private final Runnable scrollingRunnable = () -> {
		long scrollStartTime = System.currentTimeMillis();

		// Start continuous scroll immediately after
		// any direction became active
		isContinuousScrolling = true;
		while (hasActiveDirections()) {
			scrollMap(activeDirections);
			// Wait so you don't scroll too fast
			threadSleep();
		}
		isContinuousScrolling = false;

		// Make a single long map move if the spent time permits
		if (System.currentTimeMillis() - scrollStartTime < LONG_PRESS_TIME_MS) {
			scrollMap(collectDirectionsForLongScroll());
		}

		// Setup finish location (for OpenGL only)
		stopScroll();
	};

	public void startScrolling(@NonNull ScrollDirection direction) {
		addDirection(direction);
		if (!isContinuousScrolling) {
			suspendSymbolsUpdate();
			new Thread(scrollingRunnable).start();
		}
	}

	public void addDirection(@NonNull ScrollDirection direction) {
		activeDirections.add(direction);
	}

	public void removeDirection(@NonNull ScrollDirection direction) {
		if (activeDirections.contains(direction)) {
			cancelDirectionTime.put(direction, System.currentTimeMillis());
		}
		activeDirections.remove(direction);
	}
	
	private boolean hasActiveDirections() {
		return !Algorithms.isEmpty(activeDirections);
	}

	public Set<ScrollDirection> collectDirectionsForLongScroll() {
		Set<ScrollDirection> directions = new HashSet<>();
		for (ScrollDirection direction : ScrollDirection.values()) {
			Long cancelTime = cancelDirectionTime.get(direction);
			if (cancelTime != null && System.currentTimeMillis() - cancelTime <= MAX_KEY_UP_TIME_MS) {
				directions.add(direction);
			}
		}
		return directions;
	}

	private void scrollMap(@NonNull Set<ScrollDirection> directions) {
		boolean left = directions.contains(ScrollDirection.LEFT);
		boolean right = directions.contains(ScrollDirection.RIGHT);
		boolean up = directions.contains(ScrollDirection.UP);
		boolean down = directions.contains(ScrollDirection.DOWN);

		int delta = isContinuousScrolling ? SHORT_SHIFT_DELTA : LONG_SHIFT_DELTA;
		int dx = (left ? -delta : 0) + (right ? delta : 0);
		int dy = (up ? -delta : 0) + (down ? delta : 0);

		MapRendererView renderer = mapView.getMapRenderer();
		if (renderer != null) {
			PointI point31 = new PointI();
			PointD newCenter = new PointD(
					(double) renderer.getWindowWidth() / 2.0 + (double) dx,
					(double) renderer.getWindowHeight() / 2.0 + (double) dy);
			if (renderer.getLocationFromScreenPoint(newCenter, point31)) {
				renderer.setTarget(point31, false, false);
			}
		} else {
			RotatedTileBox tileBox = mapView.getCurrentRotatedTileBox();
			QuadPoint center = tileBox.getCenterPixelPoint();
			LatLon l = tileBox.getLatLonFromPixel(center.x + dx, center.y + dy);
			osmandMap.setMapLocation(l.getLatitude(), l.getLongitude());
		}
	}

	private void stopScroll() {
		MapRendererView renderer = mapView.getMapRenderer();
		if (renderer != null) {
			PointI target31 = new PointI();
			PointI center = renderer.getState().getFixedPixel();
			renderer.getLocationFromElevatedPoint(center, target31);
			double y = MapUtils.get31LatitudeY(target31.getY());
			double x = MapUtils.get31LongitudeX(target31.getX());
			osmandMap.setMapLocation(y, x);
			renderer.resumeSymbolsUpdate();
		}
	}

	public void scrollMapAction(@NonNull ScrollDirection direction){
		scrollMap(Collections.singleton(direction));
		stopScroll();
	}

	private void threadSleep() {
		try {
			Thread.sleep(SCROLL_PAUSE_MS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void suspendSymbolsUpdate() {
		MapRendererView mapRenderer = mapView.getMapRenderer();
		if (mapRenderer != null) {
			mapRenderer.suspendSymbolsUpdate();
		}
	}
}