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
import net.osmand.plus.views.Renderable;
import net.osmand.router.RouteCalculationProgress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

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
	private Queue<Pair<WptPt, WptPt>> snapToRoadPairsToCalculate = new LinkedList<>();
	private Map<Pair<WptPt, WptPt>, List<WptPt>> snappedToRoadPoints = new ConcurrentHashMap<>();

	private List<WptPt> measurementPoints = new LinkedList<>();

	public void setApplication(OsmandApplication application) {
		this.application = application;
	}

	public MeasurementCommandManager getCommandManager() {
		return commandManager;
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
		if (measurementPoints.size() > 1) {
			for (int i = 0; i < measurementPoints.size() - 1; i++) {
				Pair<WptPt, WptPt> pair = new Pair<>(measurementPoints.get(i), measurementPoints.get(i + 1));
				List<WptPt> pts = snappedToRoadPoints.get(pair);
				if (pts != null) {
					before.points.addAll(pts);
				} else {
					if (inSnapToRoadMode) {
						scheduleRouteCalculateIfNotEmpty();
					}
					before.points.addAll(Arrays.asList(pair.first, pair.second));
				}
			}
		} else {
			before.points.addAll(measurementPoints);
		}
		addBeforeRenders();
		after = new TrkSegment();
	}

	public void splitSegments(int position) {
		before = new TrkSegment();
		before.points.addAll(measurementPoints.subList(0, position));
		addBeforeRenders();
		after = new TrkSegment();
		after.points.addAll(measurementPoints.subList(position, measurementPoints.size()));
		addAfterRenders();
	}

	public void clearSegments() {
		before = new TrkSegment();
		after = new TrkSegment();
	}

	void scheduleRouteCalculateIfNotEmpty() {
		if (application == null || measurementPoints.size() < 1) {
			return;
		}
		for (int i = 0; i < measurementPoints.size() - 1; i++) {
			Pair<WptPt, WptPt> pair = new Pair<>(measurementPoints.get(i), measurementPoints.get(i + 1));
			if (snappedToRoadPoints.get(pair) == null && !snapToRoadPairsToCalculate.contains(pair)) {
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
				recreateSegments();
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
