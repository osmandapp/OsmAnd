package net.osmand.plus.measurementtool;

import android.util.Pair;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.measurementtool.command.MeasurementCommandManager;
import net.osmand.plus.routing.RouteCalculationParams;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.router.RouteCalculationProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MeasurementEditingContext {

	private OsmandApplication application;
	private final MeasurementCommandManager commandManager = new MeasurementCommandManager();

	private TrkSegment before = new TrkSegment();
	// cache should be deleted if before changed or snappedToRoadPoints
	private TrkSegment beforeCacheForSnap;

	private TrkSegment after = new TrkSegment();
	// cache should be deleted if after changed or snappedToRoadPoints
	private TrkSegment afterCacheForSnap;

	private boolean inMovePointMode;
	private boolean inAddPointBeforeMode;
	private boolean inAddPointAfterMode;

	private int selectedPointPosition = -1;
	private WptPt originalPointToMove;

	private boolean inSnapToRoadMode;
	private SnapToRoadProgressListener progressListener;
	private ApplicationMode snapToRoadAppMode;
	private RouteCalculationProgress calculationProgress;
	private Queue<Pair<WptPt, WptPt>> snapToRoadPairsToCalculate = new ConcurrentLinkedQueue<>();
	private Map<Pair<WptPt, WptPt>, List<WptPt>> snappedToRoadPoints = new ConcurrentHashMap<>();

	public void setApplication(OsmandApplication application) {
		this.application = application;
	}

	public MeasurementCommandManager getCommandManager() {
		return commandManager;
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

	public int getSelectedPointPosition() {
		return selectedPointPosition;
	}

	public void setSelectedPointPosition(int selectedPointPosition) {
		this.selectedPointPosition = selectedPointPosition;
	}

	public WptPt getOriginalPointToMove() {
		return originalPointToMove;
	}

	public void setOriginalPointToMove(WptPt originalPointToMove) {
		this.originalPointToMove = originalPointToMove;
	}

	public boolean isInAddPointAfterMode() {
		return inAddPointAfterMode;
	}

	public void setInSnapToRoadMode(boolean inSnapToRoadMode) {
		this.inSnapToRoadMode = inSnapToRoadMode;
	}

	public void setProgressListener(SnapToRoadProgressListener progressListener) {
		this.progressListener = progressListener;
	}

	public ApplicationMode getSnapToRoadAppMode() {
		return snapToRoadAppMode;
	}

	public void setSnapToRoadAppMode(ApplicationMode snapToRoadAppMode) {
		this.snapToRoadAppMode = snapToRoadAppMode;
	}

	public Map<Pair<WptPt, WptPt>, List<WptPt>> getSnappedPoints() {
		return snappedToRoadPoints;
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

	public List<WptPt> getPoints() {
		return getBeforePoints();
	}

	public List<WptPt> getBeforePoints() {
		return before.points;
	}

	public List<WptPt> getAfterPoints() {
		return after.points;
	}

	public int getPointsCount() {
		return before.points.size();
	}

	public void splitSegments(int position) {
		List<WptPt> points = new ArrayList<>();
		points.addAll(before.points);
		points.addAll(after.points);
		before.points.clear();
		after.points.clear();
		if (position == before.points.size() + after.points.size()) {
			before.points.addAll(points);
		} else {
			before.points.addAll(points.subList(0, position));
			after.points.addAll(points.subList(position, points.size()));
		}
	}

	public void addPoint(WptPt pt) {
		before.points.add(pt);
	}

	public void addPoint(int position, WptPt pt) {
		before.points.add(position, pt);
	}

	public void addPoints(List<WptPt> points) {
		before.points.addAll(points);
	}

	public WptPt removePoint(int position) {
		return before.points.remove(position);
	}

	public void clearSegments() {
		before.points.clear();
		after.points.clear();
	}

	void scheduleRouteCalculateIfNotEmpty() {
		if (application == null || before.points.size() < 1) {
			return;
		}
		snapToRoadPairsToCalculate.clear();
		for (int i = 0; i < before.points.size() - 1; i++) {
			Pair<WptPt, WptPt> pair = new Pair<>(before.points.get(i), before.points.get(i + 1));
			if (snappedToRoadPoints.get(pair) == null) {
				snapToRoadPairsToCalculate.add(pair);
			}
		}
		RoutingHelper routingHelper = application.getRoutingHelper();
		if (!snapToRoadPairsToCalculate.isEmpty() && progressListener != null && !routingHelper.isRouteBeingCalculated()) {
			routingHelper.startRouteCalculationThread(getParams(), true, true);
			application.runInUIThread(new Runnable() {
				@Override
				public void run() {
					progressListener.showProgressBar();
				}
			});
		}
	}

	void cancelSnapToRoad() {
		progressListener.hideProgressBar();
		snapToRoadPairsToCalculate.clear();
		if (calculationProgress != null) {
			calculationProgress.isCancelled = true;
		}
	}

	private RouteCalculationParams getParams() {
		OsmandSettings settings = application.getSettings();

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
		params.ctx = application;
		params.calculationProgress = calculationProgress = new RouteCalculationProgress();
		params.calculationProgressCallback = new RoutingHelper.RouteCalculationProgressCallback() {
			@Override
			public void updateProgress(int progress) {
				progressListener.updateProgress(progress);
			}

			@Override
			public void requestPrivateAccessRouting() {

			}

			@Override
			public void finish() {
				progressListener.refreshMap();
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
				progressListener.refreshMap();
				if (!snapToRoadPairsToCalculate.isEmpty()) {
					application.getRoutingHelper().startRouteCalculationThread(getParams(), true, true);
				} else {
					application.runInUIThread(new Runnable() {
						@Override
						public void run() {
							progressListener.hideProgressBar();
						}
					});
				}
			}
		};

		return params;
	}

	interface SnapToRoadProgressListener {

		void showProgressBar();

		void updateProgress(int progress);

		void hideProgressBar();

		void refreshMap();
	}
}
