package net.osmand.plus.measurementtool;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.measurementtool.command.MeasurementCommandManager;
import net.osmand.plus.routing.RouteCalculationParams;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.router.RouteCalculationProgress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MeasurementEditingContext {

	private OsmandApplication application;
	private final MeasurementCommandManager commandManager = new MeasurementCommandManager();

	private final TrkSegment before = new TrkSegment();
	private TrkSegment beforeCacheForSnap;
	private final TrkSegment after = new TrkSegment();
	private TrkSegment afterCacheForSnap;

	private NewGpxData newGpxData;

	private int selectedPointPosition = -1;
	private WptPt originalPointToMove;

	private boolean inAddPointMode;
	private boolean inSnapToRoadMode;
	private boolean needUpdateCacheForSnap;
	private int calculatedPairs;
	private SnapToRoadProgressListener progressListener;
	private ApplicationMode snapToRoadAppMode;
	private RouteCalculationProgress calculationProgress;
	private final Queue<Pair<WptPt, WptPt>> snapToRoadPairsToCalculate = new ConcurrentLinkedQueue<>();
	private final Map<Pair<WptPt, WptPt>, List<WptPt>> snappedToRoadPoints = new ConcurrentHashMap<>();

	public void setApplication(OsmandApplication application) {
		this.application = application;
	}

	MeasurementCommandManager getCommandManager() {
		return commandManager;
	}

	boolean isInAddPointMode() {
		return inAddPointMode;
	}

	boolean isInSnapToRoadMode() {
		return inSnapToRoadMode;
	}

	public boolean isNeedUpdateCacheForSnap() {
		return needUpdateCacheForSnap;
	}

	public void setNeedUpdateCacheForSnap(boolean needUpdateCacheForSnap) {
		this.needUpdateCacheForSnap = needUpdateCacheForSnap;
		updateCacheForSnapIfNeeded(true);
	}

	int getSelectedPointPosition() {
		return selectedPointPosition;
	}

	void setSelectedPointPosition(int selectedPointPosition) {
		this.selectedPointPosition = selectedPointPosition;
	}

	WptPt getOriginalPointToMove() {
		return originalPointToMove;
	}

	void setOriginalPointToMove(WptPt originalPointToMove) {
		this.originalPointToMove = originalPointToMove;
	}

	void setInAddPointMode(boolean inAddPointMode) {
		this.inAddPointMode = inAddPointMode;
	}

	void setInSnapToRoadMode(boolean inSnapToRoadMode) {
		this.inSnapToRoadMode = inSnapToRoadMode;
	}

	NewGpxData getNewGpxData() {
		return newGpxData;
	}

	public void setNewGpxData(NewGpxData newGpxData) {
		this.newGpxData = newGpxData;
	}

	void setProgressListener(SnapToRoadProgressListener progressListener) {
		this.progressListener = progressListener;
	}

	ApplicationMode getSnapToRoadAppMode() {
		return snapToRoadAppMode;
	}

	void setSnapToRoadAppMode(ApplicationMode snapToRoadAppMode) {
		if (this.snapToRoadAppMode != null
				&& !this.snapToRoadAppMode.getStringKey().equals(snapToRoadAppMode.getStringKey())) {
			snappedToRoadPoints.clear();
			updateCacheForSnapIfNeeded(true);
		}
		this.snapToRoadAppMode = snapToRoadAppMode;
	}

	TrkSegment getBeforeTrkSegmentLine() {
		if (beforeCacheForSnap != null) {
			return beforeCacheForSnap;
		}
		return before;
	}

	TrkSegment getAfterTrkSegmentLine() {
		if (afterCacheForSnap != null) {
			return afterCacheForSnap;
		}
		return after;
	}

	public List<WptPt> getPoints() {
		return getBeforePoints();
	}

	List<WptPt> getBeforePoints() {
		return before.points;
	}

	List<WptPt> getAfterPoints() {
		return after.points;
	}

	public int getPointsCount() {
		return before.points.size();
	}

	void splitSegments(int position) {
		List<WptPt> points = new ArrayList<>();
		points.addAll(before.points);
		points.addAll(after.points);
		before.points.clear();
		after.points.clear();
		before.points.addAll(points.subList(0, position));
		after.points.addAll(points.subList(position, points.size()));
		updateCacheForSnapIfNeeded(true);
	}

	public void addPoint(WptPt pt) {
		before.points.add(pt);
		updateCacheForSnapIfNeeded(false);
	}

	public void addPoint(int position, WptPt pt) {
		before.points.add(position, pt);
		updateCacheForSnapIfNeeded(false);
	}

	public void addPoints(List<WptPt> points) {
		before.points.addAll(points);
		updateCacheForSnapIfNeeded(false);
	}

	public WptPt removePoint(int position, boolean updateSnapToRoad) {
		if (position < 0 || position >= before.points.size()) {
			return new WptPt();
		}
		WptPt pt = before.points.remove(position);
		if (updateSnapToRoad) {
			updateCacheForSnapIfNeeded(false);
		}
		return pt;
	}

	public void clearSegments() {
		before.points.clear();
		after.points.clear();
		if (inSnapToRoadMode) {
			if (beforeCacheForSnap != null && afterCacheForSnap != null) {
				beforeCacheForSnap.points.clear();
				afterCacheForSnap.points.clear();
			}
			needUpdateCacheForSnap = true;
		} else {
			beforeCacheForSnap = null;
			afterCacheForSnap = null;
			needUpdateCacheForSnap = false;
		}
	}

	void scheduleRouteCalculateIfNotEmpty() {
		needUpdateCacheForSnap = true;
		if (application == null || (before.points.size() == 0 && after.points.size() == 0)) {
			return;
		}
		snapToRoadPairsToCalculate.clear();
		findPointsToCalculate(Arrays.asList(before.points, after.points));
		RoutingHelper routingHelper = application.getRoutingHelper();
		if (progressListener != null && !routingHelper.isRouteBeingCalculated()) {
			RouteCalculationParams params = getParams();
			if (params != null) {
				routingHelper.startRouteCalculationThread(params, true, true);
				application.runInUIThread(new Runnable() {
					@Override
					public void run() {
						progressListener.showProgressBar();
					}
				});
			}
		}
	}

	private void findPointsToCalculate(List<List<WptPt>> pointsList) {
		for (List<WptPt> points : pointsList) {
			for (int i = 0; i < points.size() - 1; i++) {
				Pair<WptPt, WptPt> pair = new Pair<>(points.get(i), points.get(i + 1));
				if (snappedToRoadPoints.get(pair) == null) {
					snapToRoadPairsToCalculate.add(pair);
				}
			}
		}
	}

	private void recreateCacheForSnap(TrkSegment cache, TrkSegment original) {
		if (original.points.size() > 1) {
			for (int i = 0; i < original.points.size() - 1; i++) {
				Pair<WptPt, WptPt> pair = new Pair<>(original.points.get(i), original.points.get(i + 1));
				List<WptPt> pts = snappedToRoadPoints.get(pair);
				if (pts != null) {
					cache.points.addAll(pts);
				} else {
					if (inSnapToRoadMode) {
						scheduleRouteCalculateIfNotEmpty();
					}
					cache.points.addAll(Arrays.asList(pair.first, pair.second));
				}
			}
		} else {
			cache.points.addAll(original.points);
		}
	}

	private void updateCacheForSnapIfNeeded(boolean both) {
		if (needUpdateCacheForSnap) {
			recreateCacheForSnap(beforeCacheForSnap = new TrkSegment(), before);
			if (both) {
				recreateCacheForSnap(afterCacheForSnap = new TrkSegment(), after);
			}
		}
	}

	void cancelSnapToRoad() {
		progressListener.hideProgressBar();
		snapToRoadPairsToCalculate.clear();
		if (calculationProgress != null) {
			calculationProgress.isCancelled = true;
		}
	}

	@Nullable
	private RouteCalculationParams getParams() {
		final Pair<WptPt, WptPt> currentPair = snapToRoadPairsToCalculate.poll();
		if (currentPair == null) {
			return null;
		}
		Location start = new Location("");
		start.setLatitude(currentPair.first.getLatitude());
		start.setLongitude(currentPair.first.getLongitude());

		LatLon end = new LatLon(currentPair.second.getLatitude(), currentPair.second.getLongitude());

		final RouteCalculationParams params = new RouteCalculationParams();
		params.inSnapToRoadMode = true;
		params.start = start;
		params.end = end;
		RoutingHelper.applyApplicationSettings(params, application.getSettings(), snapToRoadAppMode);
		params.mode = snapToRoadAppMode;
		params.ctx = application;
		params.calculationProgress = calculationProgress = new RouteCalculationProgress();
		params.calculationProgressCallback = new RoutingHelper.RouteCalculationProgressCallback() {

			@Override
			public void start() {
			}

			@Override
			public void updateProgress(int progress) {
				int pairs = calculatedPairs + snapToRoadPairsToCalculate.size();
				if (pairs != 0) {
					int pairProgress = 100 / pairs;
					progress = calculatedPairs * pairProgress + progress / pairs;
				}
				progressListener.updateProgress(progress);
			}

			@Override
			public void requestPrivateAccessRouting() {
			}

			@Override
			public void finish() {
				calculatedPairs = 0;
			}
		};
		params.resultListener = new RouteCalculationParams.RouteCalculationResultListener() {
			@Override
			public void onRouteCalculated(RouteCalculationResult route) {
				List<Location> locations = route.getRouteLocations();
				ArrayList<WptPt> pts = new ArrayList<>(locations.size());
				double prevAltitude = Double.NaN;
				for (Location loc : locations) {
					WptPt pt = new WptPt();
					pt.lat = loc.getLatitude();
					pt.lon = loc.getLongitude();
					if (loc.hasAltitude()) {
						prevAltitude = loc.getAltitude();
						pt.ele = prevAltitude;
					} else if (!Double.isNaN(prevAltitude)) {
						pt.ele = prevAltitude;
					}
					pts.add(pt);
				}
				calculatedPairs++;
				snappedToRoadPoints.put(currentPair, pts);
				updateCacheForSnapIfNeeded(true);
				application.runInUIThread(new Runnable() {
					@Override
					public void run() {
						progressListener.refresh();
					}
				});
				RouteCalculationParams params = getParams();
				if (params != null) {
					application.getRoutingHelper().startRouteCalculationThread(params, true, true);
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

	public void exportRouteAsGpx(@NonNull String gpxName, @Nullable ExportAsGpxListener exportListener) {
		if (application == null || (before.points.size() == 0 && after.points.size() == 0)) {
			return;
		}
		RoutingHelper routingHelper = application.getRoutingHelper();
		if (!routingHelper.isRouteBeingCalculated()) {
			RouteCalculationParams params = getExportAsGpxParams(gpxName, exportListener);
			if (params != null) {
				routingHelper.startRouteCalculationThread(params, true, true);
			}
		}
	}

	@Nullable
	private RouteCalculationParams getExportAsGpxParams(@NonNull final String gpxName, @Nullable final ExportAsGpxListener exportListener) {
		List<List<WptPt>> pointList = Arrays.asList(before.points, after.points);
		WptPt startPoint = null;
		WptPt endPoint = null;
		List<WptPt> intermediatePoints = new ArrayList<>();
		for (List<WptPt> points : pointList) {
			for (WptPt point : points) {
				if (startPoint == null) {
					startPoint = point;
				} else {
					intermediatePoints.add(point);
					endPoint = point;
				}
			}
		}
		if (endPoint != null) {
			intermediatePoints.remove(endPoint);
		}
		if (startPoint == null || endPoint == null) {
			return null;
		}

		Location start = new Location("");
		start.setLatitude(startPoint.getLatitude());
		start.setLongitude(startPoint.getLongitude());
		LatLon end = new LatLon(endPoint.getLatitude(), endPoint.getLongitude());
		List<LatLon> intermediates = null;
		if (!intermediatePoints.isEmpty()) {
			intermediates = new ArrayList<>();
			for (WptPt point : intermediatePoints) {
				intermediates.add(new LatLon(point.getLatitude(), point.getLongitude()));
			}
		}
		final RouteCalculationParams params = new RouteCalculationParams();
		params.inSnapToRoadMode = true;
		params.start = start;
		params.end = end;
		params.intermediates = intermediates;
		RoutingHelper.applyApplicationSettings(params, application.getSettings(), snapToRoadAppMode);
		params.mode = snapToRoadAppMode;
		params.ctx = application;
		params.calculationProgress = calculationProgress = new RouteCalculationProgress();
		params.calculationProgressCallback = new RoutingHelper.RouteCalculationProgressCallback() {

			@Override
			public void start() {
			}

			@Override
			public void updateProgress(int progress) {
			}

			@Override
			public void requestPrivateAccessRouting() {
			}

			@Override
			public void finish() {
				calculatedPairs = 0;
			}
		};
		params.resultListener = new RouteCalculationParams.RouteCalculationResultListener() {
			@Override
			public void onRouteCalculated(RouteCalculationResult route) {
				GPXFile gpx = application.getRoutingHelper().generateGPXFileWithRoute(route, gpxName);
				if (exportListener != null) {
					exportListener.onExportAsGpxFinished(gpx);
				}
			}
		};
		return params;
	}

	interface SnapToRoadProgressListener {

		void showProgressBar();

		void updateProgress(int progress);

		void hideProgressBar();

		void refresh();
	}

	interface ExportAsGpxListener {
		void onExportAsGpxFinished(GPXFile gpx);
	}
}
