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
import net.osmand.plus.measurementtool.command.ApplyGpxApproximationCommand;
import net.osmand.plus.measurementtool.command.MeasurementCommandManager;
import net.osmand.plus.measurementtool.command.MeasurementModeCommand;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationMode.WHOLE_TRACK;
import static net.osmand.plus.measurementtool.command.MeasurementModeCommand.MeasurementCommandType.APPROXIMATE_POINTS;

public class MeasurementEditingContext {

	public final static ApplicationMode DEFAULT_APP_MODE = ApplicationMode.DEFAULT;

	private OsmandApplication application;
	private final MeasurementCommandManager commandManager = new MeasurementCommandManager();

	private final TrkSegment before = new TrkSegment();
	private TrkSegment beforeCacheForSnap;
	private final TrkSegment after = new TrkSegment();
	private TrkSegment afterCacheForSnap;

	private GpxData gpxData;

	private int selectedPointPosition = -1;
	private WptPt originalPointToMove;

	private boolean inAddPointMode;
	private boolean inApproximationMode;
	private int calculatedPairs;
	private int pointsToCalculateSize;
	private CalculationMode lastCalculationMode = WHOLE_TRACK;
	private SnapToRoadProgressListener progressListener;
	private ApplicationMode appMode = DEFAULT_APP_MODE;
	private RouteCalculationProgress calculationProgress;
	private Map<Pair<WptPt, WptPt>, RoadSegmentData> roadSegmentData = new ConcurrentHashMap<>();


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

	public boolean hasChanges() {
		return commandManager.hasChanges();
	}

	public void setChangesSaved() {
		commandManager.resetChangesCounter();
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

	public boolean isInApproximationMode() {
		return inApproximationMode;
	}

	public void setInApproximationMode(boolean inApproximationMode) {
		this.inApproximationMode = inApproximationMode;
	}

	public List<WptPt> getOriginalTrackPointList() {
		MeasurementModeCommand command = commandManager.getLastCommand();
		if (command.getType() == APPROXIMATE_POINTS) {
			return ((ApplyGpxApproximationCommand) command).getPoints();
		}
		return null;
	}

	@Nullable
	GpxData getGpxData() {
		return gpxData;
	}

	public boolean isNewData() {
		return gpxData == null;
	}

	public void setGpxData(GpxData gpxData) {
		this.gpxData = gpxData;
	}

	public boolean hasRoutePoints() {
		return gpxData != null && gpxData.getGpxFile() != null && gpxData.getGpxFile().hasRtePt();
	}

	public boolean hasSavedRoute() {
		return gpxData != null && gpxData.getGpxFile() != null && gpxData.getGpxFile().hasRoute();
	}

	public CalculationMode getLastCalculationMode() {
		return lastCalculationMode;
	}

	public void setLastCalculationMode(CalculationMode lastCalculationMode) {
		this.lastCalculationMode = lastCalculationMode;
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
		for (List<WptPt> points : Arrays.asList(before.points, after.points)) {
			for (int i = 0; i < points.size() - 1; i++) {
				Pair<WptPt, WptPt> pair = new Pair<>(points.get(i), points.get(i + 1));
				RoadSegmentData data = this.roadSegmentData.get(pair);
				if (data == null) {
					if (appMode != MeasurementEditingContext.DEFAULT_APP_MODE || !pair.first.lastPoint || !pair.second.firstPoint) {
						distance += MapUtils.getDistance(pair.first.getLatitude(), pair.first.getLongitude(),
								pair.second.getLatitude(), pair.second.getLongitude());
					}
				} else {
					distance += data.getDistance();
				}
			}
		}
		return distance;
	}

	public Map<Pair<WptPt, WptPt>, RoadSegmentData> getRoadSegmentData() {
		return new HashMap<>(roadSegmentData);
	}

	public void setRoadSegmentData(Map<Pair<WptPt, WptPt>, RoadSegmentData> roadSegmentData) {
		this.roadSegmentData = new ConcurrentHashMap<>(roadSegmentData);
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
		clearSnappedToRoadPoints();
	}

	public void clearBeforeSegments() {
		before.points.clear();
		if (beforeCacheForSnap != null) {
			beforeCacheForSnap.points.clear();
		}
	}

	public void clearAfterSegments() {
		after.points.clear();
		if (afterCacheForSnap != null) {
			afterCacheForSnap.points.clear();
		}
	}

	public boolean isFirstPointSelected() {
		return selectedPointPosition == 0;
	}

	public boolean isLastPointSelected() {
		return selectedPointPosition == getPoints().size() - 1;
	}

	public ApplicationMode getSelectedPointAppMode() {
		return getPointAppMode(selectedPointPosition);
	}

	public ApplicationMode getBeforeSelectedPointAppMode() {
		return getPointAppMode(Math.max(selectedPointPosition - 1, 0));
	}

	private ApplicationMode getPointAppMode(int pointPosition) {
		String profileType = getPoints().get(pointPosition).getProfileType();
		return ApplicationMode.valueOfStringKey(profileType, MeasurementEditingContext.DEFAULT_APP_MODE);
	}

	public void scheduleRouteCalculateIfNotEmpty() {
		if (application == null || (before.points.size() == 0 && after.points.size() == 0)) {
			return;
		}
		RoutingHelper routingHelper = application.getRoutingHelper();
		if (progressListener != null && !routingHelper.isRouteBeingCalculated()) {
			RouteCalculationParams params = getParams(true);
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

	private List<Pair<WptPt, WptPt>> getPointsToCalculate() {
		List<Pair<WptPt, WptPt>> res = new ArrayList<>();
		for (List<WptPt> points : Arrays.asList(before.points, after.points)) {
			for (int i = 0; i < points.size() - 1; i++) {
				Pair<WptPt, WptPt> pair = new Pair<>(points.get(i), points.get(i + 1));
				if (roadSegmentData.get(pair) == null) {
					res.add(pair);
				}
			}
		}
		return res;
	}

	private void recreateCacheForSnap(TrkSegment cache, TrkSegment original, boolean calculateIfNeeded) {
		boolean hasDefaultModeOnly = true;
		if (original.points.size() > 1) {
			for (int i = 0; i < original.points.size(); i++) {
				String profileType = original.points.get(i).getProfileType();
				if (profileType != null && !profileType.equals(DEFAULT_APP_MODE.getStringKey())) {
					hasDefaultModeOnly = false;
					break;
				}
			}
		}
		if (original.points.size() > 1) {
			for (int i = 0; i < original.points.size() - 1; i++) {
				Pair<WptPt, WptPt> pair = new Pair<>(original.points.get(i), original.points.get(i + 1));
				RoadSegmentData data = this.roadSegmentData.get(pair);
				List<WptPt> pts = data != null ? data.getPoints() : null;
				if (pts != null) {
					cache.points.addAll(pts);
				} else {
					if (calculateIfNeeded && !hasDefaultModeOnly) {
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
		GpxData gpxData = getGpxData();
		if (gpxData == null || gpxData.getTrkSegment() == null || Algorithms.isEmpty(gpxData.getTrkSegment().points)) {
			return;
		}
		List<WptPt> points = gpxData.getTrkSegment().points;
		if (isTrackSnappedToRoad()) {
			RouteImporter routeImporter = new RouteImporter(gpxData.getGpxFile());
			List<RouteSegmentResult> segments = routeImporter.importRoute();
			List<WptPt> routePoints = gpxData.getGpxFile().getRoutePoints();
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

	public void setPoints(GpxRouteApproximation gpxApproximation, ApplicationMode mode) {
		if (gpxApproximation == null || Algorithms.isEmpty(gpxApproximation.finalPoints) || Algorithms.isEmpty(gpxApproximation.result)) {
			return;
		}
		roadSegmentData.clear();
		List<WptPt> routePoints = new ArrayList<>();
		List<GpxPoint> gpxPoints = gpxApproximation.finalPoints;
		for (int i = 0; i < gpxPoints.size(); i++) {
			GpxPoint gp1 = gpxPoints.get(i);
			boolean lastGpxPoint = isLastGpxPoint(gpxPoints, i);
			List<WptPt> points = new ArrayList<>();
			List<RouteSegmentResult> segments = new ArrayList<>();
			for (int k = 0; k < gp1.routeToTarget.size(); k++) {
				RouteSegmentResult seg = gp1.routeToTarget.get(k);
				if (seg.getStartPointIndex() != seg.getEndPointIndex()) {
					segments.add(seg);
				}
			}
			for (int k = 0; k < segments.size(); k++) {
				RouteSegmentResult seg = segments.get(k);
				fillPointsArray(points, seg, lastGpxPoint && k == segments.size() - 1);
			}
			if (!points.isEmpty()) {
				WptPt wp1 = new WptPt();
				wp1.lat = gp1.loc.getLatitude();
				wp1.lon = gp1.loc.getLongitude();
				wp1.setProfileType(mode.getStringKey());
				routePoints.add(wp1);
				WptPt wp2 = new WptPt();
				if (lastGpxPoint) {
					wp2.lat = points.get(points.size() - 1).getLatitude();
					wp2.lon = points.get(points.size() - 1).getLongitude();
					routePoints.add(wp2);
				} else {
					GpxPoint gp2 = gpxPoints.get(i + 1);
					wp2.lat = gp2.loc.getLatitude();
					wp2.lon = gp2.loc.getLongitude();
				}
				wp2.setProfileType(mode.getStringKey());
				Pair<WptPt, WptPt> pair = new Pair<>(wp1, wp2);
				roadSegmentData.put(pair, new RoadSegmentData(appMode, pair.first, pair.second, points, segments));
			}
			if (lastGpxPoint) {
				break;
			}
		}
		addPoints(routePoints);
	}

	private boolean isLastGpxPoint(List<GpxPoint> gpxPoints, int index) {
		if (index == gpxPoints.size() - 1) {
			return true;
		} else {
			for (int i = index + 1; i < gpxPoints.size(); i++) {
				GpxPoint gp = gpxPoints.get(i);
				for (int k = 0; k < gp.routeToTarget.size(); k++) {
					RouteSegmentResult seg = gp.routeToTarget.get(k);
					if (seg.getStartPointIndex() != seg.getEndPointIndex()) {
						return false;
					}
				}

			}
		}
		return true;
	}

	private void fillPointsArray(List<WptPt> points, RouteSegmentResult seg, boolean includeEndPoint) {
		int ind = seg.getStartPointIndex();
		boolean plus = seg.isForwardDirection();
		float[] heightArray = seg.getObject().calculateHeightArray();
		while (ind != seg.getEndPointIndex()) {
			addPointToArray(points, seg, ind, heightArray);
			ind = plus ? ind + 1 : ind - 1;
		}
		if (includeEndPoint) {
			addPointToArray(points, seg, ind, heightArray);
		}
	}

	private void addPointToArray(List<WptPt> points, RouteSegmentResult seg, int index, float[] heightArray) {
		LatLon l = seg.getPoint(index);
		WptPt pt = new WptPt();
		if (heightArray != null && heightArray.length > index * 2 + 1) {
			pt.ele = heightArray[index * 2 + 1];
		}
		pt.lat = l.getLatitude();
		pt.lon = l.getLongitude();
		points.add(pt);
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
		GpxData gpxData = getGpxData();
		return gpxData != null && gpxData.getTrkSegment() != null
				&& !gpxData.getTrkSegment().points.isEmpty()
				&& gpxData.getGpxFile().hasRoute();
	}

	private void updateCacheForSnap(boolean both) {
		recreateCacheForSnap(beforeCacheForSnap = new TrkSegment(), before, true);
		if (both) {
			recreateCacheForSnap(afterCacheForSnap = new TrkSegment(), after, true);
		}
	}

	private void updateCacheForSnap(boolean both, boolean calculateIfNeeded) {
		recreateCacheForSnap(beforeCacheForSnap = new TrkSegment(), before, calculateIfNeeded);
		if (both) {
			recreateCacheForSnap(afterCacheForSnap = new TrkSegment(), after, calculateIfNeeded);
		}
	}


	void cancelSnapToRoad() {
		progressListener.hideProgressBar();
		if (calculationProgress != null) {
			calculationProgress.isCancelled = true;
		}
	}

	@Nullable
	private RouteCalculationParams getParams(boolean resetCounter) {
		List<Pair<WptPt, WptPt>> pointsToCalculate = getPointsToCalculate();
		if (Algorithms.isEmpty(pointsToCalculate)) {
			return null;
		}
		if (resetCounter) {
			calculatedPairs = 0;
			pointsToCalculateSize = pointsToCalculate.size();
		}
		final Pair<WptPt, WptPt> currentPair = pointsToCalculate.get(0);
		Location start = new Location("");
		start.setLatitude(currentPair.first.getLatitude());
		start.setLongitude(currentPair.first.getLongitude());

		LatLon end = new LatLon(currentPair.second.getLatitude(), currentPair.second.getLongitude());

		RouteRegion reg = new RouteRegion();
		reg.initRouteEncodingRule(0, "highway", RouteResultPreparation.UNMATCHED_HIGHWAY_TYPE);

		final RouteCalculationParams params = new RouteCalculationParams();
		params.inSnapToRoadMode = true;
		params.start = start;

		ApplicationMode appMode = ApplicationMode.valueOfStringKey(currentPair.first.getProfileType(), DEFAULT_APP_MODE);
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
				int pairs = pointsToCalculateSize;
				if (pairs != 0) {
					float pairProgress = 100f / pairs;
					progress = (int)(calculatedPairs * pairProgress + (float) progress / pairs);
				}
				progressListener.updateProgress(progress);
			}

			@Override
			public void requestPrivateAccessRouting() {
			}

			@Override
			public void finish() {
				calculatedPairs = 0;
				pointsToCalculateSize = 0;
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
				params.calculationProgressCallback.updateProgress(0);
				List<RouteSegmentResult> originalRoute = route.getOriginalRoute();
				if (Algorithms.isEmpty(originalRoute)) {
					originalRoute = Collections.singletonList(RoutePlannerFrontEnd.generateStraightLineSegment(
							DEFAULT_APP_MODE.getDefaultSpeed(), new LocationsHolder(pts).getLatLonList()));
				}
				roadSegmentData.put(currentPair, new RoadSegmentData(route.getAppMode(), currentPair.first, currentPair.second, pts, originalRoute));
				application.runInUIThread(new Runnable() {
					@Override
					public void run() {
						updateCacheForSnap(true, false);
						progressListener.refresh();
						RouteCalculationParams params = getParams(false);
						if (params != null) {
							application.getRoutingHelper().startRouteCalculationThread(params, true, true);
						} else {
							progressListener.hideProgressBar();
						}
					}
				});
			}
		};
		return params;
	}

	public List<WptPt> getDistinctRoutePoints() {
		List<WptPt> res = new ArrayList<>();
		List<WptPt> points = new ArrayList<>(before.points);
		points.addAll(after.points);
		int size = points.size();
		for (int i = 0; i < size - 1; i++) {
			Pair<WptPt, WptPt> pair = new Pair<>(points.get(i), points.get(i + 1));
			RoadSegmentData data = this.roadSegmentData.get(pair);
			if (data != null) {
				res.addAll(data.points);
				if (i < size - 2) {
					res.remove(res.size() - 1);
				}
			}
		}
		return res;
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
				for (WptPt pt : data.points) {
					Location l = new Location("");
					l.setLatitude(pt.getLatitude());
					l.setLongitude(pt.getLongitude());
					if (!Double.isNaN(pt.ele)) {
						l.setAltitude(pt.ele);
					}
					locations.add(l);
				}
				pair.second.setTrkPtIndex(i < size - 1 ? locations.size() : locations.size() - 1);
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
