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
	private List<TrkSegment> beforeSegments = new ArrayList<>();
	private List<TrkSegment> beforeSegmentsForSnap;
	private final TrkSegment after = new TrkSegment();
	private List<TrkSegment> afterSegments = new ArrayList<>();
	private List<TrkSegment> afterSegmentsForSnap;

	private GpxData gpxData;

	private int selectedPointPosition = -1;
	private WptPt originalPointToMove;

	private boolean inAddPointMode;
	private boolean inAddPointBeforeMode;
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

	public enum AdditionMode {
		UNDEFINED,
		ADD_AFTER,
		ADD_BEFORE,
	}

	public static class RoadSegmentData {
		private final ApplicationMode appMode;
		private final WptPt start;
		private final WptPt end;
		private final List<WptPt> points;
		private final List<RouteSegmentResult> segments;
		private final double distance;

		public RoadSegmentData(@NonNull ApplicationMode appMode, @NonNull WptPt start, @NonNull WptPt end,
							   @Nullable List<WptPt> points, @Nullable List<RouteSegmentResult> segments) {
			this.appMode = appMode;
			this.start = start;
			this.end = end;
			this.points = points;
			this.segments = segments;
			double distance = 0;
			if (points != null && points.size() > 1) {
				for (int i = 1; i < points.size(); i++) {
					distance += MapUtils.getDistance(points.get(i - 1).lat, points.get(i - 1).lon,
							points.get(i).lat, points.get(i).lon);
				}
			} else if (segments != null) {
				for (RouteSegmentResult segment : segments) {
					distance += segment.getDistance();
				}
			}
			this.distance = distance;
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

	public boolean isInAddPointBeforeMode() {
		return inAddPointBeforeMode;
	}

	public void updateSegmentsForSnap() {
		updateSegmentsForSnap(true);
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

	void setInAddPointMode(boolean inAddPointMode, boolean inAddPointAfterMode) {
		this.inAddPointMode = inAddPointMode;
		this.inAddPointBeforeMode = inAddPointAfterMode;
	}

	public boolean isInApproximationMode() {
		return inApproximationMode;
	}

	public void setInApproximationMode(boolean inApproximationMode) {
		this.inApproximationMode = inApproximationMode;
	}

	public List<List<WptPt>> getOriginalSegmentPointsList() {
		MeasurementModeCommand command = commandManager.getLastCommand();
		if (command.getType() == APPROXIMATE_POINTS) {
			return ((ApplyGpxApproximationCommand) command).getOriginalSegmentPointsList();
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

	public boolean isApproximationNeeded() {
		boolean hasDefaultPoints = false;
		boolean newData = isNewData();
		if (!newData) {
			List<WptPt> points = getPoints();
			WptPt prevPoint = null;
			for (WptPt point : points) {
				if (!point.hasProfile() && (prevPoint == null || !prevPoint.hasProfile())) {
					hasDefaultPoints = true;
					break;
				}
				prevPoint = point;
			}
		}
		return !newData && hasDefaultPoints && getPoints().size() > 2;
	}

	public boolean isSelectionNeedApproximation() {
		boolean hasDefaultPoints = false;
		boolean newData = isNewData();
		if (!newData && selectedPointPosition != -1) {
			WptPt selectedPoint = getPoints().get(selectedPointPosition);
			List<TrkSegment> segments = getBeforeSegments();
			List<WptPt> points = null;
			for (TrkSegment segment : segments) {
				if (segment.points.contains(selectedPoint)) {
					points = segment.points;
				}
			}
			WptPt prevPoint = null;
			if (points != null) {
				for (WptPt point : points) {
					if (!point.hasProfile() && (prevPoint == null || !prevPoint.hasProfile())) {
						hasDefaultPoints = true;
						break;
					}
					prevPoint = point;
				}
			}
		}
		return !newData && hasDefaultPoints && getPoints().size() > 2;
	}

	public void clearSnappedToRoadPoints() {
		roadSegmentData.clear();
	}

	List<TrkSegment> getBeforeTrkSegmentLine() {
		if (beforeSegmentsForSnap != null) {
			return beforeSegmentsForSnap;
		}
		return beforeSegments;
	}

	List<TrkSegment> getAfterTrkSegmentLine() {
		if (afterSegmentsForSnap != null) {
			return afterSegmentsForSnap;
		}
		return afterSegments;
	}

	public List<TrkSegment> getBeforeSegments() {
		return beforeSegments;
	}

	public List<TrkSegment> getAfterSegments() {
		return afterSegments;
	}

	public List<WptPt> getPoints() {
		return getBeforePoints();
	}

	public List<List<WptPt>> getPointsSegments(boolean plain, boolean route) {
		List<List<WptPt>> res = new ArrayList<>();
		List<WptPt> allPoints = getPoints();
		List<WptPt> segment = new ArrayList<>();
		String prevProfileType = null;
		for (WptPt point : allPoints) {
			String profileType = point.getProfileType();
			boolean isGap = point.isGap();
			boolean plainPoint = Algorithms.isEmpty(profileType) || (isGap && Algorithms.isEmpty(prevProfileType));
			boolean routePoint = !plainPoint;
			if (plain && plainPoint || route && routePoint) {
				segment.add(point);
				if (isGap) {
					res.add(segment);
					segment = new ArrayList<>();
				}
			}
			prevProfileType = profileType;
		}
		if (!segment.isEmpty()) {
			res.add(segment);
		}
		return res;
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

	public boolean isPointsEnoughToCalculateRoute() {
		return getPointsCount() >= 2;
	}

	public List<RouteSegmentResult> getAllRouteSegments() {
		List<RouteSegmentResult> allSegments = new ArrayList<>();
		for (Pair<WptPt, WptPt> key : getOrderedRoadSegmentDataKeys()) {
			RoadSegmentData data = roadSegmentData.get(key);
			if (data != null) {
				List<RouteSegmentResult> segments = data.getSegments();
				if (segments != null) {
					allSegments.addAll(segments);
				}
			}
		}
		return allSegments.size() > 0 ? allSegments : null;
	}

	void splitSegments(int position) {
		List<WptPt> points = new ArrayList<>();
		points.addAll(before.points);
		points.addAll(after.points);
		before.points.clear();
		after.points.clear();
		before.points.addAll(points.subList(0, position));
		after.points.addAll(points.subList(position, points.size()));
		updateSegmentsForSnap(true);
	}

	private void preAddPoint(int position, AdditionMode additionMode, WptPt point) {
		switch (additionMode) {
			case UNDEFINED: {
				if (appMode != MeasurementEditingContext.DEFAULT_APP_MODE) {
					point.setProfileType(appMode.getStringKey());
				}
				break;
			}
			case ADD_AFTER: {
				List<WptPt> points = getBeforePoints();
				if (position > 0 && position <= points.size()) {
					WptPt prevPt = points.get(position - 1);
					if (prevPt.isGap()) {
						point.setGap();
						if (position > 1) {
							WptPt pt = points.get(position - 2);
							if (pt.hasProfile()) {
								prevPt.setProfileType(pt.getProfileType());
							} else {
								prevPt.removeProfileType();
							}
						}
					} else if (prevPt.hasProfile()) {
						point.setProfileType(prevPt.getProfileType());
					}
				} else if (appMode != MeasurementEditingContext.DEFAULT_APP_MODE) {
					point.setProfileType(appMode.getStringKey());
				}
				break;
			}
			case ADD_BEFORE: {
				List<WptPt> points = getAfterPoints();
				if (position >= -1 && position + 1 < points.size()) {
					WptPt nextPt = points.get(position + 1);
					if (nextPt.hasProfile()) {
						point.setProfileType(nextPt.getProfileType());
					}
				} else if (appMode != MeasurementEditingContext.DEFAULT_APP_MODE) {
					point.setProfileType(appMode.getStringKey());
				}
				break;
			}
		}
	}

	public void addPoint(WptPt pt) {
		addPoint(pt, AdditionMode.UNDEFINED);
	}

	public void addPoint(WptPt pt, AdditionMode additionMode) {
		if (additionMode == AdditionMode.ADD_AFTER || additionMode == AdditionMode.ADD_BEFORE) {
			preAddPoint(additionMode == AdditionMode.ADD_BEFORE ? -1 : getBeforePoints().size(), additionMode, pt);
		}
		before.points.add(pt);
		updateSegmentsForSnap(false);
	}

	public void addPoint(int position, WptPt pt) {
		addPoint(position, pt, AdditionMode.UNDEFINED);
	}

	public void addPoint(int position, WptPt pt, AdditionMode additionMode) {
		if (additionMode == AdditionMode.ADD_AFTER || additionMode == AdditionMode.ADD_BEFORE) {
			preAddPoint(position, additionMode, pt);
		}
		before.points.add(position, pt);
		updateSegmentsForSnap(false);
	}

	public void addPoints(List<WptPt> points) {
		before.points.addAll(points);
		updateSegmentsForSnap(false);
	}

	public void replacePoints(List<WptPt> originalPoints, List<WptPt> points) {
		if (originalPoints.size() > 1) {
			int firstPointIndex = before.points.indexOf(originalPoints.get(0));
			int lastPointIndex = before.points.indexOf(originalPoints.get(originalPoints.size() - 1));
			List<WptPt> newPoints = new ArrayList<>();
			if (firstPointIndex != -1 && lastPointIndex != -1) {
				newPoints.addAll(before.points.subList(0, firstPointIndex));
				newPoints.addAll(points);
				if (before.points.size() > lastPointIndex + 1) {
					newPoints.addAll(before.points.subList(lastPointIndex + 1, before.points.size()));
				}
			} else {
				newPoints.addAll(points);
			}
			before.points = newPoints;
		} else {
			before.points = points;
		}
		updateSegmentsForSnap(false);
	}

	public WptPt removePoint(int position, boolean updateSnapToRoad) {
		if (position < 0 || position >= before.points.size()) {
			return new WptPt();
		}
		WptPt pt = before.points.get(position);
		if (position > 0 && pt.isGap()) {
			WptPt prevPt = before.points.get(position - 1);
			if (!prevPt.isGap()) {
				prevPt.setGap();
			}
		}
		before.points.remove(position);
		if (updateSnapToRoad) {
			updateSegmentsForSnap(false);
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
		if (beforeSegmentsForSnap != null) {
			beforeSegmentsForSnap.clear();
		}
	}

	public void clearAfterSegments() {
		after.points.clear();
		if (afterSegmentsForSnap != null) {
			afterSegmentsForSnap.clear();
		}
	}

	public boolean isFirstPointSelected() {
		return isBorderPointSelected(true);
	}

	public boolean isLastPointSelected() {
		return isBorderPointSelected(false);
	}

	private boolean isBorderPointSelected(boolean first) {
		WptPt selectedPoint = getPoints().get(selectedPointPosition);
		List<TrkSegment> segments = getBeforeSegments();
		int count = 0;
		for (TrkSegment segment : segments) {
			int i = segment.points.indexOf(selectedPoint);
			if (i != -1) {
				int segmentPosition = selectedPointPosition - count;
				return first ? segmentPosition == 0 : segmentPosition == segment.points.size() - 1;
			} else {
				count += segment.points.size();
			}
		}
		return false;
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
				WptPt startPoint = points.get(i);
				WptPt endPoint = points.get(i + 1);
				Pair<WptPt, WptPt> pair = new Pair<>(startPoint, endPoint);
				if (roadSegmentData.get(pair) == null && startPoint.hasProfile()) {
					res.add(pair);
				}
			}
		}
		return res;
	}

  private List<Pair<WptPt, WptPt>> getOrderedRoadSegmentDataKeys() {
		List<Pair<WptPt, WptPt>> keys = new ArrayList<>();
		for (List<WptPt> points : Arrays.asList(before.points, after.points)) {
			for (int i = 0; i < points.size() - 1; i++) {
				keys.add(new Pair<>(points.get(i), points.get(i + 1)));
			}
		}
		return keys;
	}
  
	private void recreateSegments(List<TrkSegment> segments, List<TrkSegment> segmentsForSnap, List<WptPt> points, boolean calculateIfNeeded) {
		List<Integer> roadSegmentIndexes = new ArrayList<>();
		TrkSegment s = new TrkSegment();
		segments.add(s);
		boolean defaultMode = true;
		if (points.size() > 1) {
			for (int i = 0; i < points.size(); i++) {
				WptPt point = points.get(i);
				s.points.add(point);
				String profileType = point.getProfileType();
				if (profileType != null) {
					boolean isDefault = profileType.equals(DEFAULT_APP_MODE.getStringKey());
					boolean isGap = point.isGap();
					if (defaultMode && !isDefault && !isGap) {
						roadSegmentIndexes.add(segments.size() - 1);
						defaultMode = false;
					}
					if (isGap) {
						if (!s.points.isEmpty()) {
							s = new TrkSegment();
							segments.add(s);
							defaultMode = true;
						}
					}
				}
			}
		} else {
			s.points.addAll(points);
		}
		if (s.points.isEmpty()) {
			segments.remove(s);
		}
		if (!segments.isEmpty()) {
			for (TrkSegment segment : segments) {
				TrkSegment segmentForSnap = new TrkSegment();
				for (int i = 0; i < segment.points.size() - 1; i++) {
					Pair<WptPt, WptPt> pair = new Pair<>(segment.points.get(i), segment.points.get(i + 1));
					RoadSegmentData data = this.roadSegmentData.get(pair);
					List<WptPt> pts = data != null ? data.getPoints() : null;
					if (pts != null) {
						segmentForSnap.points.addAll(pts);
					} else {
						if (calculateIfNeeded && roadSegmentIndexes.contains(segmentsForSnap.size())) {
							scheduleRouteCalculateIfNotEmpty();
						}
						segmentForSnap.points.addAll(Arrays.asList(pair.first, pair.second));
					}
				}
				if (segmentForSnap.points.isEmpty()) {
					segmentForSnap.points.addAll(segment.points);
				}
				segmentsForSnap.add(segmentForSnap);
			}
		} else if (!points.isEmpty()) {
			TrkSegment segmentForSnap = new TrkSegment();
			segmentForSnap.points.addAll(points);
			segmentsForSnap.add(segmentForSnap);
		}
	}

	void addPoints() {
		GpxData gpxData = getGpxData();
		if (gpxData == null || gpxData.getGpxFile() == null) {
			return;
		}
		List<TrkSegment> segments = gpxData.getGpxFile().getNonEmptyTrkSegments(false);
		if (Algorithms.isEmpty(segments)) {
			return;
		}
		for (int si = 0; si < segments.size(); si++) {
			TrkSegment segment = segments.get(si);
			List<WptPt> points = segment.points;
			if (segment.hasRoute()) {
				RouteImporter routeImporter = new RouteImporter(segment);
				List<RouteSegmentResult> routeSegments = routeImporter.importRoute();
				List<WptPt> routePoints = gpxData.getGpxFile().getRoutePoints(si);
				int prevPointIndex = 0;
				if (routePoints.isEmpty() && points.size() > 1) {
					routePoints.add(points.get(0));
					routePoints.add(points.get(points.size() - 1));
				}
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
						Iterator<RouteSegmentResult> it = routeSegments.iterator();
						int k = endIndex - startIndex - 1;
						List<RouteSegmentResult> pairSegments = new ArrayList<>();
						if (k == 0 && !routeSegments.isEmpty()) {
							pairSegments.add(routeSegments.remove(0));
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
				if (!routePoints.isEmpty() && si < segments.size() - 1) {
					routePoints.get(routePoints.size() - 1).setGap();
				}
				addPoints(routePoints);
			} else {
				addPoints(points);
				if (!points.isEmpty() && si < segments.size() - 1) {
					points.get(points.size() - 1).setGap();
				}
			}
		}
	}

	public List<WptPt> setPoints(GpxRouteApproximation gpxApproximation, List<WptPt> originalPoints, ApplicationMode mode) {
		if (gpxApproximation == null || Algorithms.isEmpty(gpxApproximation.finalPoints) || Algorithms.isEmpty(gpxApproximation.result)) {
			return null;
		}
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
		WptPt lastOriginalPoint = originalPoints.get(originalPoints.size() - 1);
		WptPt lastRoutePoint = routePoints.get(routePoints.size() - 1);
		if (lastOriginalPoint.isGap()) {
			lastRoutePoint.setGap();
		}
		replacePoints(originalPoints, routePoints);
		return routePoints;
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

	private void updateSegmentsForSnap(boolean both) {
		recreateSegments(beforeSegments = new ArrayList<>(),
				beforeSegmentsForSnap = new ArrayList<>(), before.points, true);
		if (both) {
			recreateSegments(afterSegments = new ArrayList<>(),
					afterSegmentsForSnap = new ArrayList<>(), after.points, true);
		}
	}

	private void updateSegmentsForSnap(boolean both, boolean calculateIfNeeded) {
		recreateSegments(beforeSegments = new ArrayList<>(),
				beforeSegmentsForSnap = new ArrayList<>(), before.points, calculateIfNeeded);
		if (both) {
			recreateSegments(afterSegments = new ArrayList<>(),
					afterSegmentsForSnap = new ArrayList<>(), after.points, calculateIfNeeded);
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
					progress = (int) (calculatedPairs * pairProgress + (float) progress / pairs);
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
						updateSegmentsForSnap(true, false);
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

	public List<List<WptPt>> getRoutePoints() {
		List<List<WptPt>> res = new ArrayList<>();
		List<WptPt> plainPoints = new ArrayList<>(before.points);
		plainPoints.addAll(after.points);
		List<WptPt> points = new ArrayList<>();
		for (WptPt point : plainPoints) {
			if (point.getTrkPtIndex() != -1) {
				points.add(point);
				if (point.isGap()) {
					res.add(points);
					points = new ArrayList<>();
				}
			}
		}
		if (!points.isEmpty()) {
			res.add(points);
		}
		return res;
	}

	@Nullable
	public GPXFile exportGpx(@NonNull String gpxName) {
		if (application == null || before.points.isEmpty()) {
			return null;
		}
		return RouteExporter.exportRoute(gpxName, getRouteSegments(), null);
	}

	private TrkSegment getRouteSegment(int startPointIndex, int endPointIndex) {
		List<RouteSegmentResult> route = new ArrayList<>();
		List<Location> locations = new ArrayList<>();
		for (int i = startPointIndex; i < endPointIndex; i++) {
			Pair<WptPt, WptPt> pair = new Pair<>(before.points.get(i), before.points.get(i + 1));
			RoadSegmentData data = this.roadSegmentData.get(pair);
			if (data != null && data.points != null && data.segments != null) {
				for (WptPt pt : data.points) {
					Location l = new Location("");
					l.setLatitude(pt.getLatitude());
					l.setLongitude(pt.getLongitude());
					if (!Double.isNaN(pt.ele)) {
						l.setAltitude(pt.ele);
					}
					locations.add(l);
				}
				pair.second.setTrkPtIndex(i + 1 < before.points.size() - 1 ? locations.size() : locations.size() - 1);
				route.addAll(data.segments);
			}
		}
		if (!locations.isEmpty() && !route.isEmpty()) {
			before.points.get(startPointIndex).setTrkPtIndex(0);
			return new RouteExporter("", route, locations, null).generateRouteSegment();
		} else if (endPointIndex - startPointIndex >= 0) {
			TrkSegment segment = new TrkSegment();
			segment.points = new ArrayList<>(before.points.subList(startPointIndex, endPointIndex + 1));
			return segment;
		}
		return null;
	}

	private List<TrkSegment> getRouteSegments() {
		List<TrkSegment> res = new ArrayList<>();
		List<Integer> lastPointIndexes = new ArrayList<>();
		for (int i = 0; i < before.points.size(); i++) {
			WptPt pt = before.points.get(i);
			if (pt.isGap()) {
				lastPointIndexes.add(i);
			}
		}
		if (lastPointIndexes.isEmpty() || lastPointIndexes.get(lastPointIndexes.size() - 1) < before.points.size() - 1) {
			lastPointIndexes.add(before.points.size() - 1);
		}
		int firstPointIndex = 0;
		for (Integer lastPointIndex : lastPointIndexes) {
			TrkSegment segment = getRouteSegment(firstPointIndex, lastPointIndex);
			if (segment != null) {
				res.add(segment);
			}
			firstPointIndex = lastPointIndex + 1;
		}
		return res;
	}

	interface SnapToRoadProgressListener {

		void showProgressBar();

		void updateProgress(int progress);

		void hideProgressBar();

		void refresh();
	}
}
