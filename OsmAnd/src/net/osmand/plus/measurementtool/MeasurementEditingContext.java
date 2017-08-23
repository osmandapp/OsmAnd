package net.osmand.plus.measurementtool;

import android.util.Pair;
import android.view.View;
import android.widget.ProgressBar;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteCalculationParams;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.Renderable;
import net.osmand.router.RouteCalculationProgress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class MeasurementEditingContext {

	private MapActivity mapActivity;

	private TrkSegment before = new TrkSegment();
	// cache should be deleted if after changed or snappedToRoadPoints
	private TrkSegment beforeCacheForSnap;

	private TrkSegment after = new TrkSegment();
	// cache should be deleted if after changed or snappedToRoadPoints
	private TrkSegment afterCacheForSnap;

	private boolean inMovePointMode;
	private boolean inAddPointBeforeMode;
	private boolean inAddPointAfterMode;

	private ProgressBar progressBar;
	private boolean inSnapToRoadMode;
	private ApplicationMode snapToRoadAppMode;
	private RouteCalculationProgress calculationProgress;
	private Queue<Pair<WptPt, WptPt>> snapToRoadPairsToCalculate = new LinkedList<>();
	private Map<Pair<WptPt, WptPt>, List<WptPt>> snappedToRoadPoints = new HashMap<>();

	private List<WptPt> measurementPoints = new LinkedList<>();

	public void setMapActivity(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	public void setBefore(TrkSegment before) {
		this.before = before;
		addBeforeRenders();
	}

	public void setAfter(TrkSegment after) {
		this.after = after;
		addAfterRenders();
	}

	private void addBeforeRenders() {
		before.renders.add(new Renderable.StandardTrack(before.points, 17.2));
	}

	private void addAfterRenders() {
		after.renders.add(new Renderable.StandardTrack(after.points, 17.2));
	}

	public boolean isInMovePointMode() {
		return inMovePointMode;
	}

	public void setInMovePointMode(boolean inMovePointMode) {
		this.inMovePointMode = inMovePointMode;
	}

	public boolean isInSnapToRoadMode() {
		return inSnapToRoadMode;
	}

	public void setInAddPointBeforeMode(boolean inAddPointBeforeMode) {
		this.inAddPointBeforeMode = inAddPointBeforeMode;
	}

	public boolean isInAddPointBeforeMode() {
		return inAddPointBeforeMode;
	}

	public void setInAddPointAfterMode(boolean inAddPointAfterMode) {
		this.inAddPointAfterMode = inAddPointAfterMode;
	}

	public boolean isInAddPointAfterMode() {
		return inAddPointAfterMode;
	}

	public void setInSnapToRoadMode(boolean inSnapToRoadMode) {
		inSnapToRoadMode = inSnapToRoadMode;
	}

	public ApplicationMode getSnapToRoadAppMode() {
		return snapToRoadAppMode;
	}

	public void setSnapToRoadAppMode(ApplicationMode snapToRoadAppMode) {
		this.snapToRoadAppMode = snapToRoadAppMode;
	}

	public Queue<Pair<WptPt, WptPt>> getSnapToRoadPairsToCalculate() {
		return snapToRoadPairsToCalculate;
	}

	public void setSnapToRoadPairsToCalculate(Queue<Pair<WptPt, WptPt>> snapToRoadPairsToCalculate) {
		this.snapToRoadPairsToCalculate = snapToRoadPairsToCalculate;
	}

	public Map<Pair<WptPt, WptPt>, List<WptPt>> getSnappedPoints() {
		return snappedToRoadPoints;
	}

	public List<WptPt> getPoints() {
		return measurementPoints;
	}

	public int getPointsCount() {
		return measurementPoints.size();
	}

	public TrkSegment getBeforeTrkSegmentLine() {
		// check if all segments calculated for snap to road
//		if(beforeCacheForSnap != null) {
//			return	beforeCacheForSnap;
//		}
		// calculate beforeCacheForSnap
		return before;
	}

	public TrkSegment getAfterTrkSegmentLine() {
		return after;
	}

	public void recreateSegments() {
		before = new TrkSegment();
		before.points.addAll(measurementPoints);
		addBeforeRenders();
		after = new TrkSegment();
	}

	public void recreateSegments(int position) {
		before = new TrkSegment();
		before.points.addAll(measurementPoints.subList(0, position));
		addBeforeRenders();
		after = new TrkSegment();
		if (position != measurementPoints.size() - 1) {
			after.points.addAll(measurementPoints.subList(position + 1, measurementPoints.size()));
			addAfterRenders();
		}
	}

	public void clearSegments() {
		before = new TrkSegment();
		after = new TrkSegment();
	}

	public void scheduleRouteCalculateIfNotEmpty(ProgressBar progressBar) {
		if (mapActivity == null || measurementPoints.size() < 1) {
			return;
		}
		for (int i = 0; i < measurementPoints.size() - 1; i++) {
			snapToRoadPairsToCalculate.add(new Pair<>(measurementPoints.get(i), measurementPoints.get(i + 1)));
		}
		this.progressBar = progressBar;
		if (!snapToRoadPairsToCalculate.isEmpty()) {
			mapActivity.getMyApplication().getRoutingHelper().startRouteCalculationThread(getParams(), true, true);
			progressBar.setVisibility(View.VISIBLE);
		}
	}

	public void cancelSnapToRoad() {
		if (calculationProgress != null) {
			calculationProgress.isCancelled = true;
		}
	}

	private RouteCalculationParams getParams() {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();

		final Pair<WptPt, WptPt> currentPair = snapToRoadPairsToCalculate.poll();

		Location start = new Location("");
		start.setLatitude(currentPair.first.getLatitude());
		start.setLongitude(currentPair.first.getLongitude());

		LatLon end = new LatLon(currentPair.second.getLatitude(), currentPair.second.getLongitude());

		final RouteCalculationParams params = new RouteCalculationParams();
		params.start = start;
		params.end = end;
		params.leftSide = settings.DRIVING_REGION.get().leftHandDriving;
		params.fast = settings.FAST_ROUTE_MODE.getModeValue(snapToRoadAppMode);
		params.type = settings.ROUTER_SERVICE.getModeValue(snapToRoadAppMode);
		params.mode = snapToRoadAppMode;
		params.ctx = app;
		params.calculationProgress = calculationProgress = new RouteCalculationProgress();
		params.calculationProgressCallback = new RoutingHelper.RouteCalculationProgressCallback() {
			@Override
			public void updateProgress(int progress) {
				progressBar.setProgress(progress);
			}

			@Override
			public void requestPrivateAccessRouting() {

			}

			@Override
			public void finish() {
				mapActivity.refreshMap();
			}
		};
		params.resultListener = new RouteCalculationParams.RouteCalculationResultListener() {
			@Override
			public void onRouteCalculated(List<Location> locations) {
				ArrayList<WptPt> pts = new ArrayList<>(locations.size());
				for (Location loc : locations) {
					WptPt pt = new WptPt();
					pt.lat = loc.getLatitude();
					pt.lon = loc.getLongitude();
					pts.add(pt);
				}
				snappedToRoadPoints.put(currentPair, pts);

				if (!snapToRoadPairsToCalculate.isEmpty()) {
					mapActivity.getMyApplication().getRoutingHelper().startRouteCalculationThread(getParams(), true, true);
				} else {
					mapActivity.getMyApplication().runInUIThread(new Runnable() {
						@Override
						public void run() {
							progressBar.setVisibility(View.GONE);
						}
					});
				}
			}
		};

		return params;
	}
}
