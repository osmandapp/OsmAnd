package net.osmand.plus.helpers;

import androidx.annotation.NonNull;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapScrollHelper {

	private static final int LONG_PRESS_TIME_MS = 250;
	private static final int MAX_KEY_UP_TIME_MS = 10;
	private static final int REFRESHING_DELAY_MS = 3;
	private static final int SMALL_SCROLLING_UNIT = 1;
	private static final int BIG_SCROLLING_UNIT = 200;

	private final Set<ScrollDirection> activeDirections = new HashSet<>();
	private final Map<ScrollDirection, Long> cancelDirectionTime = new HashMap<>();
	private final MapActivity mapActivity;

	private volatile boolean isContinuousScrolling;
	private volatile long continuousScrollingStartTime;

	public enum ScrollDirection {
		UP, DOWN, LEFT, RIGHT
	}

	public MapScrollHelper(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	private final Runnable scrollingRunnable = () -> {
		isContinuousScrolling = true;
		while (hasActiveDirections()) {
			moveMap(true, false);
			try {
				Thread.sleep(REFRESHING_DELAY_MS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (System.currentTimeMillis() - continuousScrollingStartTime < LONG_PRESS_TIME_MS) {
			List<ScrollDirection> lastDirections = getLastDirections();
			addDirections(lastDirections);
			moveMap(false, false);
			removeDirections(lastDirections);
		}
		moveMap(false, true);
		isContinuousScrolling = false;
	};

	public boolean isContinuousScrolling() {
		return isContinuousScrolling;
	}

	public void startScrolling(@NonNull ScrollDirection direction) {
		continuousScrollingStartTime = System.currentTimeMillis();
		addDirection(direction);
		if (!isContinuousScrolling) {
			new Thread(scrollingRunnable).start();
		}
	}
	
	public void addDirections(List<ScrollDirection> directions) {
		activeDirections.addAll(directions);
	}

	public void removeDirections(List<ScrollDirection> directions) {
		for (ScrollDirection direction : directions) {
			cancelDirectionTime.remove(direction);
			activeDirections.remove(direction);
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

	private void moveMap(boolean continuousScroll, boolean stop) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandMapTileView mapView = mapActivity.getMapView();
		RotatedTileBox tileBox = mapView.getCurrentRotatedTileBox();
		QuadPoint cp = tileBox.getCenterPixelPoint();
		MapRendererView renderer = mapView.getMapRenderer();
		if (stop) {
			if (renderer != null) {
				PointI target31 = new PointI();
				renderer.getLocationFromElevatedPoint(renderer.getState().getFixedPixel(), target31);
				app.getOsmandMap().setMapLocation(MapUtils.get31LatitudeY(target31.getY()), MapUtils.get31LongitudeX(target31.getX()));
			}
			return;
		}
		int scrollUnit = continuousScroll ? SMALL_SCROLLING_UNIT : BIG_SCROLLING_UNIT;
		boolean left = activeDirections.contains(ScrollDirection.LEFT);
		boolean right = activeDirections.contains(ScrollDirection.RIGHT);
		boolean up = activeDirections.contains(ScrollDirection.UP);
		boolean down = activeDirections.contains(ScrollDirection.DOWN);
		int dx = (left ? -scrollUnit : 0) + (right ? scrollUnit : 0);
		int dy = (up ? -scrollUnit : 0) + (down ? scrollUnit : 0);
		if (renderer != null) {
			PointI point31 = new PointI();
			PointI center = renderer.getState().getFixedPixel();
			if (renderer.getLocationFromScreenPoint(new PointI((int) (center.getX() + dx), (int) (center.getY() + dy)), point31)) {
				renderer.setTarget(point31, false, false);
			}
		} else {
			LatLon l = NativeUtilities.getLatLonFromPixel(renderer, tileBox, cp.x + dx, cp.y + dy);
			app.getOsmandMap().setMapLocation(l.getLatitude(), l.getLongitude());
		}
	}

	public List<ScrollDirection> getLastDirections() {
		List<ScrollDirection> result = new ArrayList<>();
		for (ScrollDirection direction : ScrollDirection.values()) {
			Long timeUp = cancelDirectionTime.get(direction);
			if (timeUp != null && System.currentTimeMillis() - timeUp <= MAX_KEY_UP_TIME_MS) {
				result.add(direction);
			}
		}
		return result;
	}
}