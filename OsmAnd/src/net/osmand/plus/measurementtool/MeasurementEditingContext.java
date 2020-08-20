package net.osmand.plus.measurementtool;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.LocationsHolder;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.measurementtool.command.MeasurementCommandManager;
import net.osmand.plus.routing.RouteCalculationParams;
import net.osmand.plus.routing.RouteCalculationParams.RouteCalculationResultListener;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelper.RouteCalculationProgressCallback;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RouteExporter;
import net.osmand.router.RouteImporter;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxRouteApproximation;
import net.osmand.router.RouteResultPreparation;
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
	private int calculatedPairs;
	private CalculationMode calculationMode = WHOLE_TRACK;
	private SnapToRoadProgressListener progressListener;
	private ApplicationMode appMode = DEFAULT_APP_MODE;
	private RouteCalculationProgress calculationProgress;
	private final Queue<Pair<WptPt, WptPt>> roadSegmentsToCalculate = new ConcurrentLinkedQueue<>();
	private final Map<Pair<WptPt, WptPt>, RoadSegmentData> roadSegmentData = new ConcurrentHashMap<>();

	public enum CalculationMode {
		NEXT_SEGMENT,
		WHOLE_TRACK
	}

	public static class RoadSegmentData {
		private ApplicationMode appMode;
		private WptPt start;
		private WptPt end;
		private List<WptPt> points;
		private List<RouteSegmentResult> segments;
		private double distance;

		public RoadSegmentData(@NonNull ApplicationMode appMode, @NonNull WptPt start, @NonNull WptPt end,
							   @Nullable List<WptPt> points, @Nullable List<RouteSegmentResult> segments) {
			this.appMode = appMode;
			this.start = start;
			this.end = end;
			this.points = points;
			this.segments = segments;
			if (segments != null) {
				double distance = 0;
				for (RouteSegmentResult segment : segments) {
					distance += segment.getDistance();
				}
				this.distance = distance;
			} else if (points != null && points.size() > 1) {
				double distance = 0;
				for (int i = 1; i < points.size(); i++) {
					distance += MapUtils.getDistance(points.get(i - 1).lat, points.get(i - 1).lon,
							points.get(i).lat, points.get(i).lon);
				}
				this.distance = distance;
			}
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

		@Nullable
		public List<WptPt> getPoints() {
			return points != null ? Collections.unmodifiableList(points) : null;
		}

		@Nullable
		public List<RouteSegmentResult> getSegments() {
			return segments != null ? Collections.unmodifiableList(segments) : null;
		}

		public double getDistance() {
			return distance;
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

	public void updateCacheForSnap() {
		updateCacheForSnap(true);
	}

	public int getSelectedPointPosition() {
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

	public double getRouteDistance() {
		double distance = 0;
		for (RoadSegmentData data : roadSegmentData.values()) {
			distance += data.getDistance();
		}
		return distance;
	}

	public boolean hasRoute() {
		return !roadSegmentData.isEmpty();
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
		updateCacheForSnap(true);
	}

	public void addPoint(WptPt pt) {
		before.points.add(pt);
		updateCacheForSnap(false);
	}

	public void addPoint(int position, WptPt pt) {
		before.points.add(position, pt);
		updateCacheForSnap(false);
	}

	public void addPoints(List<WptPt> points) {
		before.points.addAll(points);
		updateCacheForSnap(false);
	}

	public WptPt removePoint(int position, boolean updateSnapToRoad) {
		if (position < 0 || position >= before.points.size()) {
			return new WptPt();
		}
		WptPt pt = before.points.remove(position);
		if (updateSnapToRoad) {
			updateCacheForSnap(false);
		}
		return pt;
	}

	public void clearSegments() {
		before.points.clear();
		after.points.clear();
		if (beforeCacheForSnap != null && afterCacheForSnap != null) {
			beforeCacheForSnap.points.clear();
			afterCacheForSnap.points.clear();
		}
	}

	public void scheduleRouteCalculateIfNotEmpty() {
		if (application == null || (before.points.size() == 0 && after.points.size() == 0)) {
			return;
		}
		roadSegmentsToCalculate.clear();
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
					roadSegmentsToCalculate.add(pair);
				}
			}
		}
	}

	private void recreateCacheForSnap(TrkSegment cache, TrkSegment original) {
		if (original.points.size() > 1) {
			for (int i = 0; i < original.points.size() - 1; i++) {
				Pair<WptPt, WptPt> pair = new Pair<>(original.points.get(i), original.points.get(i + 1));
				RoadSegmentData data = this.roadSegmentData.get(pair);
				List<WptPt> pts = data != null ? data.getPoints() : null;
				if (pts != null) {
					cache.points.addAll(pts);
				} else {
					scheduleRouteCalculateIfNotEmpty();
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
				int endIndex = pair.second.getTrkPtIndex();
				if (endIndex < 0 || endIndex < startIndex || endIndex >= points.size()) {
					endIndex = findPointIndex(pair.second, points, startIndex);
				}
				if (startIndex >= 0 && endIndex >= 0) {
					List<WptPt> pairPoints = new ArrayList<>();
					for (int j = startIndex; j < endIndex && j < points.size(); j++) {
						pairPoints.add(points.get(j));
						prevPointIndex = j;
					}
					if (points.size() > prevPointIndex + 1) {
						pairPoints.add(points.get(prevPointIndex + 1));
					}
					Iterator<RouteSegmentResult> it = segments.iterator();
					int k = endIndex - startIndex - 1;
					List<RouteSegmentResult> pairSegments = new ArrayList<>();
					if (k == 0 && !segments.isEmpty()) {
						pairSegments.add(segments.remove(0));
					} else {
						while (it.hasNext() && k > 0) {
							RouteSegmentResult s = it.next();
							pairSegments.add(s);
							it.remove();
							k -= Math.abs(s.getEndPointIndex() - s.getStartPointIndex());
						}
					}
					ApplicationMode appMode = ApplicationMode.valueOfStringKey(pair.first.getProfileType(), DEFAULT_APP_MODE);
					roadSegmentData.put(pair, new RoadSegmentData(appMode, pair.first, pair.second, pairPoints, pairSegments));
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

	private void updateCacheForSnap(boolean both) {
		recreateCacheForSnap(beforeCacheForSnap = new TrkSegment(), before);
		if (both) {
			recreateCacheForSnap(afterCacheForSnap = new TrkSegment(), after);
		}
	}

	void cancelSnapToRoad() {
		progressListener.hideProgressBar();
		roadSegmentsToCalculate.clear();
		if (calculationProgress != null) {
			calculationProgress.isCancelled = true;
		}
	}

	@Nullable
	private RouteCalculationParams getParams() {
		final Pair<WptPt, WptPt> currentPair = roadSegmentsToCalculate.poll();
		if (currentPair == null) {
			return null;
		}
		Location start = new Location("");
		start.setLatitude(currentPair.first.getLatitude());
		start.setLongitude(currentPair.first.getLongitude());

		LatLon end = new LatLon(currentPair.second.getLatitude(), currentPair.second.getLongitude());

		RouteRegion reg = new RouteRegion();
		reg.initRouteEncodingRule(0, "highway", RouteResultPreparation.UNMATCHED_HIGHWAY_TYPE);
		final RoutePlannerFrontEnd routePlannerFrontEnd = new RoutePlannerFrontEnd();

		final RouteCalculationParams params = new RouteCalculationParams();
		params.inSnapToRoadMode = true;
		params.start = start;

		ApplicationMode appMode = calculationMode == NEXT_SEGMENT
				? ApplicationMode.valueOfStringKey(currentPair.first.getProfileType(), null) : this.appMode;
		if (appMode == null) {
			appMode = DEFAULT_APP_MODE;
		}
		params.end = end;
		RoutingHelper.applyApplicationSettings(params, application.getSettings(), appMode);
		params.mode = appMode;
		params.ctx = application;
		params.calculationProgress = calculationProgress = new RouteCalculationProgress();
		params.calculationProgressCallback = new RouteCalculationProgressCallback() {

			@Override
			public void start() {
			}

			@Override
			public void updateProgress(int progress) {
				int pairs = calculatedPairs + roadSegmentsToCalculate.size();
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
		params.resultListener = new RouteCalculationResultListener() {
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
				List<RouteSegmentResult> originalRoute = route.getOriginalRoute();
				if (Algorithms.isEmpty(originalRoute)) {
					originalRoute = Collections.singletonList(routePlannerFrontEnd.generateStraightLineSegment(
							DEFAULT_APP_MODE.getDefaultSpeed(), new LocationsHolder(pts).getLatLonList()));
				}
				roadSegmentData.put(currentPair, new RoadSegmentData(route.getAppMode(), currentPair.first, currentPair.second, pts, originalRoute));
				updateCacheForSnap(true);
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

	@Nullable
	public GPXFile exportRouteAsGpx(@NonNull String gpxName) {
		if (application == null || before.points.isEmpty() || !hasRoute()) {
			return null;
		}
		List<RouteSegmentResult> route = new ArrayList<>();
		List<Location> locations = new ArrayList<>();
		before.points.get(0).setTrkPtIndex(0);
		int size = before.points.size();
		for (int i = 0; i < size - 1; i++) {
			Pair<WptPt, WptPt> pair = new Pair<>(before.points.get(i), before.points.get(i + 1));
			RoadSegmentData data = this.roadSegmentData.get(pair);
			if (data != null) {
				LocationsHolder locationsHolder = new LocationsHolder(data.points);
				locations.addAll(locationsHolder.getLocationsList());
				pair.second.setTrkPtIndex(locations.size() - 1);
				if (i < size - 2) {
					locations.remove(locations.size() - 1);
				}
				route.addAll(data.segments);
			}
		}
		return new RouteExporter(gpxName, route, locations, null).exportRoute();
	}

	interface SnapToRoadProgressListener {

		void showProgressBar();

		void updateProgress(int progress);

		void hideProgressBar();

		void refresh();
	}
}
