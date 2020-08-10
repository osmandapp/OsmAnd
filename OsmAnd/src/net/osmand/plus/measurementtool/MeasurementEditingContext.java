package net.osmand.plus.measurementtool;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.measurementtool.command.MeasurementCommandManager;
import net.osmand.plus.routing.RouteCalculationParams;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxRouteApproximation;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationType.*;

public class MeasurementEditingContext {

	public enum CalculationType {
		NEXT_SEGMENT,
		WHOLE_TRACK
	}

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
	private CalculationType calculationType = WHOLE_TRACK;

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

	public boolean isNewData() {
		return newGpxData == null;
	}

	public void setNewGpxData(NewGpxData newGpxData) {
		this.newGpxData = newGpxData;
	}

	public boolean hasRoutePoints() {
		return newGpxData != null && newGpxData.getGpxFile() != null && newGpxData.getGpxFile().hasRtePt();
	}

	public CalculationType getCalculationType() {
		return calculationType;
	}

	public void setCalculationType(CalculationType calculationType) {
		this.calculationType = calculationType;
	}

	void setProgressListener(SnapToRoadProgressListener progressListener) {
		this.progressListener = progressListener;
	}

	public ApplicationMode getSnapToRoadAppMode() {
		return snapToRoadAppMode;
	}

	void setSnapToRoadAppMode(ApplicationMode snapToRoadAppMode) {
		if (this.snapToRoadAppMode != null && snapToRoadAppMode != null
				&& !this.snapToRoadAppMode.getStringKey().equals(snapToRoadAppMode.getStringKey())) {
			if (calculationType == WHOLE_TRACK) {
				snappedToRoadPoints.clear();
				updateCacheForSnapIfNeeded(true);
			}
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

	void addPoints() {
		NewGpxData newGpxData = getNewGpxData();
		if (newGpxData == null || newGpxData.getTrkSegment() == null || Algorithms.isEmpty(newGpxData.getTrkSegment().points)) {
			return;
		}
		List<WptPt> points = newGpxData.getTrkSegment().points;
		if (isTrackSnappedToRoad()) {
			List<WptPt> routePoints = newGpxData.getGpxFile().getRoutePoints();
			int prevPointIndex = 0;
			for (int i = 0; i < routePoints.size() - 1; i++) {
				Pair<WptPt, WptPt> pair = new Pair<>(routePoints.get(i), routePoints.get(i + 1));
				int startIndex = pair.first.getTrkPtIndex();
				if (startIndex < 0 || startIndex < prevPointIndex || startIndex >= points.size()) {
					startIndex = findPointIndex(pair.first, points, prevPointIndex);
				}
				int endIndex = pair.second.getTrkPtIndex() + 1;
				if (endIndex < 0 || endIndex < startIndex || endIndex >= points.size()) {
					endIndex = findPointIndex(pair.second, points, startIndex);
				}
				if (startIndex >= 0 && endIndex >= 0) {
					List<WptPt> cacheSegment = new ArrayList<>();
					for (int j = startIndex; j < endIndex && j < points.size(); j++) {
						cacheSegment.add(points.get(j));
						prevPointIndex = j;
					}
					snappedToRoadPoints.put(pair, cacheSegment);
				}
			}
			addPoints(routePoints);
		} else {
			addPoints(points);
		}
	}

	void setPoints(GpxRouteApproximation gpxApproximation) {
		if (gpxApproximation == null || Algorithms.isEmpty(gpxApproximation.finalPoints) || Algorithms.isEmpty(gpxApproximation.result)) {
			return;
		}
		clearSegments();
		List<GpxPoint> routePoints = gpxApproximation.finalPoints;
		for (int i = 0; i < routePoints.size() - 1; i++) {
			GpxPoint rp1 = routePoints.get(i);
			GpxPoint rp2 = routePoints.get(i + 1);
			WptPt p1 = new WptPt();
			p1.lat = rp1.loc.getLatitude();
			p1.lon = rp1.loc.getLongitude();
			WptPt p2 = new WptPt();
			p2.lat = rp2.loc.getLatitude();
			p2.lon = rp2.loc.getLongitude();
			Pair<WptPt, WptPt> pair = new Pair<>(p1, p2);
			List<WptPt> cacheSegment = new ArrayList<>();
			for (RouteSegmentResult seg : rp1.routeToTarget) {
				int start = seg.isForwardDirection() ? seg.getStartPointIndex() : seg.getEndPointIndex();
				int end = seg.isForwardDirection() ? seg.getEndPointIndex() : seg.getStartPointIndex();
				for (int ik = start; ik <= end; ik++) {
					LatLon l = seg.getPoint(ik);
					WptPt pt = new WptPt();
					pt.lat = l.getLatitude();
					pt.lon = l.getLongitude();
					cacheSegment.add(pt);
				}
			}
			snappedToRoadPoints.put(pair, cacheSegment);
		}
	}

	private int findPointIndex(WptPt point, List<WptPt> points, int firstIndex) {
		double minDistance = Double.MAX_VALUE;
		int index = 0;
		for (int i = Math.max(0, firstIndex); i < points.size(); i++) {
			double distance = MapUtils.getDistance(point.lat, point.lon, points.get(i).lat, points.get(i).lon);
			if (distance < minDistance) {
				minDistance = distance;
				index = i;
			}
		}
		return index;
	}

	boolean isTrackSnappedToRoad() {
		NewGpxData newGpxData = getNewGpxData();
		return newGpxData != null && newGpxData.getTrkSegment() != null
				&& !newGpxData.getTrkSegment().points.isEmpty()
				&& !newGpxData.getGpxFile().getRoutePoints().isEmpty();
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
				int trkptIndex = currentPair.first.getTrkPtIndex();
				trkptIndex += pts.size() - 1;
				currentPair.second.setTrkPtIndex(trkptIndex);
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
