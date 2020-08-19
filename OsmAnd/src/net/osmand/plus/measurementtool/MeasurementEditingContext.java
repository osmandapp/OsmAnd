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
import net.osmand.router.RouteImporter;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxRouteApproximation;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationMode.NEXT_SEGMENT;
import static net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationMode.WHOLE_TRACK;

public class MeasurementEditingContext {

	public final static ApplicationMode DEFAULT_APP_MODE = ApplicationMode.DEFAULT;

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
	private boolean needUpdateCacheForSnap;
	private int calculatedPairs;
	private CalculationMode calculationMode = WHOLE_TRACK;
	private SnapToRoadProgressListener progressListener;
	private ApplicationMode appMode = DEFAULT_APP_MODE;
	private RouteCalculationProgress calculationProgress;
	private final Queue<Pair<WptPt, WptPt>> snapToRoadPairsToCalculate = new ConcurrentLinkedQueue<>();
	private final Map<Pair<WptPt, WptPt>, RoadSegmentData> roadSegmentData = new ConcurrentHashMap<>();

	public enum CalculationMode {
		NEXT_SEGMENT,
		WHOLE_TRACK,
		PREVIOUS_SEGMENT,
		ALL_PREVIOUS_SEGMENTS,
		ALL_NEXT_SEGMENTS
	}

	public static class RoadSegmentData {
		private ApplicationMode appMode;
		private WptPt start;
		private WptPt end;
		private List<WptPt> snappedToRoadPoints = new ArrayList<>();
		private List<RouteSegmentResult> snappedToRoadSegments = new ArrayList<>();

		public RoadSegmentData(ApplicationMode appMode, WptPt start, WptPt end, List<WptPt> snappedToRoadPoints, List<RouteSegmentResult> snappedToRoadSegments) {
			this.appMode = appMode;
			this.start = start;
			this.end = end;
			this.snappedToRoadPoints = snappedToRoadPoints;
			this.snappedToRoadSegments = snappedToRoadSegments;
		}

		public ApplicationMode getAppMode() {
			return appMode;
		}

		public WptPt getStart() {
			return start;
		}

		public WptPt getEnd() {
			return end;
		}

		public List<WptPt> getSnappedToRoadPoints() {
			return Collections.unmodifiableList(snappedToRoadPoints);
		}

		public List<RouteSegmentResult> getSnappedToRoadSegments() {
			return Collections.unmodifiableList(snappedToRoadSegments);
		}
	}

	public void setApplication(OsmandApplication application) {
		this.application = application;
	}

	MeasurementCommandManager getCommandManager() {
		return commandManager;
	}

	boolean isInAddPointMode() {
		return inAddPointMode;
	}

	public boolean isNeedUpdateCacheForSnap() {
		return needUpdateCacheForSnap;
	}

	public void setNeedUpdateCacheForSnap(boolean needUpdateCacheForSnap) {
		this.needUpdateCacheForSnap = needUpdateCacheForSnap;
		updateCacheForSnapIfNeeded(true);
	}

	public int getSelectedPointPosition() {
		return selectedPointPosition;
	}

	void setSelectedPointPosition(int selectedPointPosition) {
		this.selectedPointPosition = selectedPointPosition;
	}

	public ApplicationMode getSelectedPointAppMode() {
		return getPointAppMode(selectedPointPosition);
	}

	public ApplicationMode getBeforeSelectedPointAppMode() {
		return getPointAppMode(Math.max(selectedPointPosition - 1, 0));
	}

	public ApplicationMode getPointAppMode(int pointPosition) {
		String profileType = getPoints().get(pointPosition).getProfileType();
		return ApplicationMode.valueOfStringKey(profileType, DEFAULT_APP_MODE);
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

	boolean isInSnapToRoadMode() {
		return appMode != DEFAULT_APP_MODE;
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

	public CalculationMode getCalculationMode() {
		return calculationMode;
	}

	public void setCalculationMode(CalculationMode calculationMode) {
		this.calculationMode = calculationMode;
	}

	void setProgressListener(SnapToRoadProgressListener progressListener) {
		this.progressListener = progressListener;
	}

	@NonNull
	public ApplicationMode getAppMode() {
		return appMode;
	}

	public void setAppMode(@NonNull ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public void resetAppMode() {
		this.appMode = DEFAULT_APP_MODE;
	}

	public void clearSnappedToRoadPoints() {
		roadSegmentData.clear();
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

	public void trimBefore(int selectedPointPosition) {
		splitSegments(selectedPointPosition);
		clearBeforeSegments();
	}

	public void trimAfter(int selectedPointPosition) {
		splitSegments(selectedPointPosition + 1);
		clearAfterSegments();
	}

	public void clearSegments() {
		clearBeforeSegments();
		clearAfterSegments();
	}

	public void clearBeforeSegments() {
		before.points.clear();
		if (isInSnapToRoadMode()) {
			if (beforeCacheForSnap != null) {
				beforeCacheForSnap.points.clear();
			}
			needUpdateCacheForSnap = true;
		} else {
			beforeCacheForSnap = null;
			needUpdateCacheForSnap = false;
		}
	}

	public void clearAfterSegments() {
		after.points.clear();
		if (isInSnapToRoadMode()) {
			if (afterCacheForSnap != null) {
				afterCacheForSnap.points.clear();
			}
			needUpdateCacheForSnap = true;
		} else {
			afterCacheForSnap = null;
			needUpdateCacheForSnap = false;
		}
	}

	public boolean isFirstPointSelected() {
		return selectedPointPosition == 0;
	}

	public boolean isLastPointSelected() {
		return selectedPointPosition == getPoints().size() - 1;
	}

	public void scheduleRouteCalculateIfNotEmpty() {
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
				if (roadSegmentData.get(pair) == null) {
					snapToRoadPairsToCalculate.add(pair);
				}
			}
		}
	}

	private void recreateCacheForSnap(TrkSegment cache, TrkSegment original) {
		if (original.points.size() > 1) {
			for (int i = 0; i < original.points.size() - 1; i++) {
				Pair<WptPt, WptPt> pair = new Pair<>(original.points.get(i), original.points.get(i + 1));
				RoadSegmentData data = this.roadSegmentData.get(pair);
				List<WptPt> pts = data != null ? data.getSnappedToRoadPoints() : null;
				if (pts != null) {
					cache.points.addAll(pts);
				} else {
					if (isInSnapToRoadMode()) {
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
			RouteImporter routeImporter = new RouteImporter(newGpxData.getGpxFile());
			List<RouteSegmentResult> segments = routeImporter.importRoute();
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
					List<WptPt> segmentPoints = new ArrayList<>();
					for (int j = startIndex; j < endIndex && j < points.size(); j++) {
						segmentPoints.add(points.get(j));
						prevPointIndex = j;
					}
					Iterator<RouteSegmentResult> it = segments.iterator();
					int k = endIndex - startIndex;
					while (it.hasNext() && k >= 0) {
						RouteSegmentResult s = it.next();
						it.remove();
						k -= Math.abs(s.getEndPointIndex() - s.getStartPointIndex());
					}
					ApplicationMode appMode = ApplicationMode.valueOfStringKey(pair.first.getProfileType(), DEFAULT_APP_MODE);
					roadSegmentData.put(pair, new RoadSegmentData(appMode, pair.first, pair.second, segmentPoints, segments));
				}
			}
			addPoints(routePoints);
		} else {
			addPoints(points);
		}
	}

	public void setPoints(GpxRouteApproximation gpxApproximation) {
		if (gpxApproximation == null || Algorithms.isEmpty(gpxApproximation.finalPoints) || Algorithms.isEmpty(gpxApproximation.result)) {
			return;
		}
		roadSegmentData.clear();
		List<WptPt> routePoints = new ArrayList<>();
		List<GpxPoint> gpxPoints = gpxApproximation.finalPoints;
		for (int i = 0; i < gpxPoints.size() - 1; i++) {
			GpxPoint rp1 = gpxPoints.get(i);
			GpxPoint rp2 = gpxPoints.get(i + 1);
			WptPt p1 = new WptPt();
			p1.lat = rp1.loc.getLatitude();
			p1.lon = rp1.loc.getLongitude();
			if (i == 0) {
				routePoints.add(p1);
			}
			WptPt p2 = new WptPt();
			p2.lat = rp2.loc.getLatitude();
			p2.lon = rp2.loc.getLongitude();
			routePoints.add(p2);
			Pair<WptPt, WptPt> pair = new Pair<>(p1, p2);
			List<WptPt> points = new ArrayList<>();
			List<RouteSegmentResult> segments = new ArrayList<>();
			for (RouteSegmentResult seg : rp1.routeToTarget) {
				segments.add(seg);
				if (seg.isForwardDirection()) {
					for (int ik = seg.getStartPointIndex(); ik <= seg.getEndPointIndex(); ik++) {
						LatLon l = seg.getPoint(ik);
						WptPt pt = new WptPt();
						pt.lat = l.getLatitude();
						pt.lon = l.getLongitude();
						points.add(pt);
					}
				} else {
					for (int ik = seg.getEndPointIndex(); ik >= seg.getStartPointIndex(); ik--) {
						LatLon l = seg.getPoint(ik);
						WptPt pt = new WptPt();
						pt.lat = l.getLatitude();
						pt.lon = l.getLongitude();
						points.add(pt);
					}
				}
			}
			roadSegmentData.put(pair, new RoadSegmentData(appMode, pair.first, pair.second, points, segments));
		}
		addPoints(routePoints);
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
				&& newGpxData.getGpxFile().hasRoute();
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

		ApplicationMode currentPointSnapToRoadMode;
		if (calculationMode == NEXT_SEGMENT) {
			currentPointSnapToRoadMode = ApplicationMode.valueOfStringKey(currentPair.first.getProfileType(),
					null);
		} else {
			currentPointSnapToRoadMode = appMode;
		}
		params.end = end;
		if (currentPointSnapToRoadMode == null) {
			ApplicationMode straightLine = DEFAULT_APP_MODE;
			RoutingHelper.applyApplicationSettings(params, application.getSettings(), straightLine);
			params.mode = straightLine;
		} else {
			RoutingHelper.applyApplicationSettings(params, application.getSettings(), currentPointSnapToRoadMode);
			params.mode = currentPointSnapToRoadMode;
		}
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
				roadSegmentData.put(currentPair, new RoadSegmentData(route.getAppMode(), currentPair.first, currentPair.second, pts, route.getOriginalRoute()));
				int trkptIndex = currentPair.first.getTrkPtIndex();
				trkptIndex += pts.size() - 1;
				currentPair.second.setTrkPtIndex(trkptIndex);
				if (route.getAppMode().equals(DEFAULT_APP_MODE)) {
					currentPair.second.removeProfileType();
				} else {
					currentPair.second.setProfileType(route.getAppMode().getStringKey());
				}
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
		if (appMode == null) {
			ApplicationMode straightLine = ApplicationMode.AIRCRAFT;
			RoutingHelper.applyApplicationSettings(params, application.getSettings(), straightLine);
			params.mode = straightLine;
		} else {
			RoutingHelper.applyApplicationSettings(params, application.getSettings(), appMode);
			params.mode = appMode;
		}
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
