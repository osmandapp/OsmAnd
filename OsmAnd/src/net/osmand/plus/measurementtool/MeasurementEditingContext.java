package net.osmand.plus.measurementtool;

import static net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationMode.WHOLE_TRACK;
import static net.osmand.plus.measurementtool.command.MeasurementModeCommand.MeasurementCommandType.APPROXIMATE_POINTS;
import static net.osmand.plus.routing.TransportRoutingHelper.PUBLIC_TRANSPORT_KEY;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.LocationsHolder;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.measurementtool.command.ApplyGpxApproximationCommand;
import net.osmand.plus.measurementtool.command.MeasurementCommandManager;
import net.osmand.plus.measurementtool.command.MeasurementModeCommand;
import net.osmand.plus.routing.IRouteSettingsListener;
import net.osmand.plus.routing.RouteCalculationParams;
import net.osmand.plus.routing.RouteCalculationProgressListener;
import net.osmand.plus.routing.RoutingHelper;
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

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MeasurementEditingContext implements IRouteSettingsListener {

	private static final Log LOG = PlatformUtil.getLog(MeasurementEditingContext.class);
	public static final ApplicationMode DEFAULT_APP_MODE = ApplicationMode.DEFAULT;
	private static final int MIN_METERS_BETWEEN_INTERMEDIATES = 100;

	private final OsmandApplication application;
	private final MeasurementCommandManager commandManager = new MeasurementCommandManager();

	private final TrkSegment before = new TrkSegment();
	private List<TrkSegment> beforeSegments = new ArrayList<>();
	private List<TrkSegment> beforeSegmentsForSnap;
	private final TrkSegment after = new TrkSegment();
	private List<TrkSegment> afterSegments = new ArrayList<>();
	private List<TrkSegment> afterSegmentsForSnap;

	private GpxData gpxData;
	private int selectedSegment = -1;

	private int selectedPointPosition = -1;
	private WptPt originalPointToMove;

	private boolean inAddPointMode;
	private boolean inAddPointBeforeMode;
	private boolean inApproximationMode;
	private int calculatedPairs;
	private int pointsToCalculateSize;
	private CalculationMode lastCalculationMode = WHOLE_TRACK;
	private ApplicationMode appMode;
	private boolean calculatedTimeSpeed;
	private boolean checkApproximation = true;
	private boolean insertIntermediates;

	private SnapToRoadProgressListener progressListener;
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

	public MeasurementEditingContext(OsmandApplication app) {
		this.application = app;
		appMode = app.getSettings().getApplicationMode();
		if (PUBLIC_TRANSPORT_KEY.equals(appMode.getRoutingProfile())) {
			appMode = ApplicationMode.DEFAULT;
		}
	}

	public void setupRouteSettingsListener() {
		if (application != null) {
			application.getRoutingHelper().addRouteSettingsListener(this);
		}
	}

	public void resetRouteSettingsListener() {
		if (application != null) {
			application.getRoutingHelper().removeRouteSettingsListener(this);
		}
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

	public void clearCommands() {
		commandManager.clearCommands();
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

	public boolean hasCalculatedTimeSpeed() {
		return calculatedTimeSpeed;
	}

	public boolean shouldCheckApproximation() {
		return checkApproximation;
	}

	public void setShouldCheckApproximation(boolean checkApproximation) {
		this.checkApproximation = checkApproximation;
	}

	public void setInsertIntermediates(boolean insertIntermediates) {
		if (this.insertIntermediates != insertIntermediates) {
			this.insertIntermediates = insertIntermediates;
			recalculateRouteSegments(DEFAULT_APP_MODE);
		}
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

	public int getSelectedSegment() {
		return selectedSegment;
	}

	public void setSelectedSegment(int selectedSegment) {
		this.selectedSegment = selectedSegment;
	}

	public boolean hasRoutePoints() {
		return gpxData != null && gpxData.getGpxFile() != null && gpxData.getGpxFile().hasRtePt();
	}

	public boolean hasElevationData() {
		return gpxData != null && gpxData.getGpxFile() != null && gpxData.getGpxFile().getAnalysis(0).hasElevationData();
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
					if (appMode != DEFAULT_APP_MODE || !pair.first.lastPoint || !pair.second.firstPoint) {
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
		boolean hasDefaultPointsOnly = false;
		boolean newData = isNewData();
		if (!newData) {
			List<WptPt> points = getPoints();
			hasDefaultPointsOnly = true;
			for (WptPt point : points) {
				if (point.hasProfile()) {
					hasDefaultPointsOnly = false;
				}
				if (!hasDefaultPointsOnly) {
					break;
				}
			}
		}
		return !newData && getPoints().size() > 2 && hasDefaultPointsOnly;
	}

	public boolean hasTimestamps() {
		if (!isNewData()) {
			for (WptPt point : getPoints()) {
				if (point.time != 0) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isAddNewSegmentAllowed() {
		return beforeSegments.size() > 0 && beforeSegments.get(beforeSegments.size() - 1).points.size() >= 2;
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

	public List<WptPt> getAllBeforePoints() {
		List<WptPt> points = new ArrayList<>();
		for (TrkSegment segment : getBeforeTrkSegmentLine()) {
			points.addAll(segment.points);
		}
		return points;
	}

	public List<List<WptPt>> getSegmentsPoints() {
		return getSegmentsPoints(true, true);
	}

	@NonNull
	public List<List<WptPt>> getSegmentsPoints(boolean plain, boolean route) {
		List<List<WptPt>> res = new ArrayList<>();
		List<WptPt> allPoints = getAllBeforePoints();
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

	public List<RouteSegmentResult> getOrderedRoadSegmentData() {
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

	@NonNull
	public List<RouteSegmentResult> getRoadSegmentData(@Nullable ApplicationMode mode) {
		List<RouteSegmentResult> res = new ArrayList<>();
		if (mode == null) {
			for (RoadSegmentData data : roadSegmentData.values()) {
				List<RouteSegmentResult> segments = data.getSegments();
				if (!Algorithms.isEmpty(segments)) {
					res.addAll(segments);
				}
			}
		} else {
			for (Entry<Pair<WptPt, WptPt>, RoadSegmentData> dataEntry : roadSegmentData.entrySet()) {
				WptPt firstPoint = dataEntry.getKey().first;
				if (mode.getStringKey().equals(firstPoint.getProfileType())) {
					List<RouteSegmentResult> segments = dataEntry.getValue().getSegments();
					if (!Algorithms.isEmpty(segments)) {
						res.addAll(segments);
					}
				}
			}
		}
		return res;
	}

	public void recalculateRouteSegments(@Nullable ApplicationMode mode) {
		boolean changed = false;
		if (mode == null) {
			roadSegmentData.clear();
			changed = true;
		} else {
			String modeKey = mode.getStringKey();
			boolean isDefaultMode = modeKey.equals(DEFAULT_APP_MODE.getStringKey());
			for (Pair<WptPt, WptPt> pair : getOrderedRoadSegmentDataKeys()) {
				String pointModeKey = pair.first.getProfileType();
				boolean recalculateStraightSegment = isDefaultMode
						&& (pointModeKey == null || modeKey.equals(pointModeKey))
						&& shouldAddIntermediates(pair.first, pair.second);
				if (recalculateStraightSegment) {
					roadSegmentData.remove(pair);
					changed = true;
				} else if (modeKey.equals(pointModeKey)) {
					RoadSegmentData data = roadSegmentData.get(pair);
					if (data != null) {
						roadSegmentData.remove(pair);
						changed = true;
					}
				}
			}
		}
		if (changed) {
			updateSegmentsForSnap(false);
		}
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
				if (appMode != DEFAULT_APP_MODE) {
					point.setProfileType(appMode.getStringKey());
				}
				break;
			}
			case ADD_AFTER: {
				List<WptPt> points = getBeforePoints();
				if (position > 0 && position <= points.size()) {
					WptPt prevPt = points.get(position - 1);
					if (prevPt.isGap()) {
						if (position == points.size() && getAfterPoints().size() == 0) {
							if (appMode != DEFAULT_APP_MODE) {
								point.setProfileType(appMode.getStringKey());
							}
						} else {
							point.setGap();
							if (position > 1) {
								WptPt pt = points.get(position - 2);
								if (pt.hasProfile()) {
									prevPt.setProfileType(pt.getProfileType());
								} else {
									prevPt.removeProfileType();
								}
							}
						}
					} else if (prevPt.hasProfile()) {
						point.setProfileType(prevPt.getProfileType());
					}
				} else if (appMode != DEFAULT_APP_MODE) {
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
				} else if (appMode != DEFAULT_APP_MODE) {
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
			int firstPointIndex = getPointIndexToReplace(before.points, originalPoints.get(0));
			int lastPointIndex = getPointIndexToReplace(before.points, originalPoints.get(originalPoints.size() - 1));
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

	private int getPointIndexToReplace(@NonNull List<WptPt> points, @NonNull WptPt point) {
		for (int i = 0; i < points.size(); i++) {
			WptPt pt = points.get(i);
			if (point == pt) {
				return i;
			}
		}
		return -1;
	}

	public WptPt removePoint(int position, boolean updateSnapToRoad) {
		if (position < 0 || position >= before.points.size()) {
			return new WptPt();
		}
		WptPt pt = before.points.get(position);
		if (updateSnapToRoad && position > 0 && pt.isGap()) {
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

	public void splitPoints(int selectedPointPosition, boolean after) {
		int pointIndex = after ? selectedPointPosition : selectedPointPosition - 1;
		if (pointIndex >= 0 && pointIndex < before.points.size()) {
			WptPt point = before.points.get(pointIndex);
			WptPt nextPoint = before.points.size() > pointIndex + 1 ? before.points.get(pointIndex + 1) : null;
			WptPt newPoint = new WptPt(point);
			newPoint.copyExtensions(point);
			newPoint.setGap();
			before.points.remove(pointIndex);
			before.points.add(pointIndex, newPoint);
			if (newPoint != null) {
				roadSegmentData.remove(new Pair<>(point, nextPoint));
			}
			updateSegmentsForSnap(false);
		}
	}

	public void joinPoints(int selectedPointPosition) {
		WptPt gapPoint = null;
		int gapIndex = -1;
		if (isFirstPointSelected(selectedPointPosition, false)) {
			if (selectedPointPosition - 1 >= 0) {
				gapPoint = before.points.get(selectedPointPosition - 1);
				gapIndex = selectedPointPosition - 1;
			}
		} else if (isLastPointSelected(selectedPointPosition, false)) {
			gapPoint = before.points.get(selectedPointPosition);
			gapIndex = selectedPointPosition;
		}
		if (gapPoint != null) {
			WptPt newPoint = new WptPt(gapPoint);
			newPoint.copyExtensions(gapPoint);
			newPoint.removeProfileType();
			before.points.remove(gapIndex);
			before.points.add(gapIndex, newPoint);
			updateSegmentsForSnap(false);
		}
	}

	public void clearSegments() {
		clearBeforeSegments();
		clearAfterSegments();
		clearSnappedToRoadPoints();
	}

	public void clearBeforeSegments() {
		before.points.clear();
		if (beforeSegments != null) {
			beforeSegments.clear();
		}
		if (beforeSegmentsForSnap != null) {
			beforeSegmentsForSnap.clear();
		}
	}

	public void clearAfterSegments() {
		after.points.clear();
		if (afterSegments != null) {
			afterSegments.clear();
		}
		if (afterSegmentsForSnap != null) {
			afterSegmentsForSnap.clear();
		}
	}

	public boolean canSplit(boolean after) {
		WptPt selectedPoint = getPoints().get(selectedPointPosition);
		List<TrkSegment> segments = getBeforeSegments();
		for (TrkSegment segment : segments) {
			int i = segment.points.indexOf(selectedPoint);
			if (i != -1) {
				return after ? i < segment.points.size() - 2 : i > 1;
			}
		}
		return false;
	}

	public boolean isFirstPointSelected(boolean outer) {
		return isFirstPointSelected(selectedPointPosition, outer);
	}

	public boolean isFirstPointSelected(int selectedPointPosition, boolean outer) {
		if (outer) {
			return selectedPointPosition == 0;
		} else {
			return isBorderPointSelected(selectedPointPosition, true);
		}
	}

	public boolean isLastPointSelected(boolean outer) {
		return isLastPointSelected(selectedPointPosition, outer);
	}

	public boolean isLastPointSelected(int selectedPointPosition, boolean outer) {
		if (outer) {
			return selectedPointPosition == getPoints().size() - 1;
		} else {
			return isBorderPointSelected(selectedPointPosition, false);
		}
	}

	private boolean isBorderPointSelected(int selectedPointPosition, boolean first) {
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

	@NonNull
	public ApplicationMode getSelectedPointAppMode() {
		return getPointAppMode(selectedPointPosition);
	}

	@NonNull
	public ApplicationMode getBeforeSelectedPointAppMode() {
		return getPointAppMode(Math.max(selectedPointPosition - 1, 0));
	}

	@NonNull
	public ApplicationMode getLastPointAppMode() {
		return getPointAppMode(getPointsCount() - 1);
	}

	@NonNull
	private ApplicationMode getPointAppMode(int pointPosition) {
		String profileType = getPoints().get(pointPosition).getProfileType();
		return ApplicationMode.valueOfStringKey(profileType, DEFAULT_APP_MODE);
	}

	public void scheduleRouteCalculateIfNotEmpty() {
		if (application == null || (before.points.size() == 0 && after.points.size() == 0)) {
			return;
		}
		RoutingHelper routingHelper = application.getRoutingHelper();
		if (progressListener != null && !routingHelper.isRouteBeingCalculated()) {
			RouteCalculationParams params = getParams(true);
			if (params != null) {
				application.runInUIThread(() -> progressListener.showProgressBar());
				routingHelper.startRouteCalculationThread(params);
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

				boolean calculate = roadSegmentData.get(pair) == null
						&& (startPoint.hasProfile() || hasRoute() || shouldAddIntermediates(startPoint, endPoint));
				if (calculate) {
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
				WptPt nextPoint = i + 1 < points.size() ? points.get(i + 1) : null;

				s.points.add(point);
				String profileType = point.getProfileType();
				boolean addIntermediates = nextPoint != null && shouldAddIntermediates(point, nextPoint);
				if (profileType != null || addIntermediates) {
					boolean isDefault = profileType == null || profileType.equals(DEFAULT_APP_MODE.getStringKey());
					boolean isGap = point.isGap();
					if (defaultMode && (!isDefault || addIntermediates) && !isGap) {
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
		GPXFile gpxFile = gpxData.getGpxFile();
		if (gpxFile.hasRtePt() && !gpxFile.hasTrkPt()) {
			addPoints(gpxFile.getRoutePoints());
			return;
		}
		List<TrkSegment> segments = gpxFile.getNonEmptyTrkSegments(false);
		if (Algorithms.isEmpty(segments)) {
			return;
		}
		if (selectedSegment != -1 && segments.size() > selectedSegment) {
			TrkSegment segment = segments.get(selectedSegment);
			if (segment.hasRoute()) {
				addPoints(collectRoutePointsFromSegment(segment, selectedSegment));
			} else {
				addPoints(segment.points);
			}
		} else {
			for (int si = 0; si < segments.size(); si++) {
				TrkSegment segment = segments.get(si);
				if (segment.hasRoute()) {
					List<WptPt> routePoints = collectRoutePointsFromSegment(segment, si);
					if (!routePoints.isEmpty() && si < segments.size() - 1) {
						routePoints.get(routePoints.size() - 1).setGap();
					}
					addPoints(routePoints);
				} else {
					List<WptPt> points = segment.points;
					addPoints(points);
					if (!points.isEmpty() && si < segments.size() - 1) {
						points.get(points.size() - 1).setGap();
					}
				}
			}
		}
	}

	private List<WptPt> collectRoutePointsFromSegment(TrkSegment segment, int segmentIndex) {
		List<WptPt> routePoints = gpxData.getGpxFile().getRoutePoints(segmentIndex);
		int prevPointIndex = 0;
		List<WptPt> points = segment.points;
		if (routePoints.isEmpty() && points.size() > 1) {
			routePoints.add(points.get(0));
			routePoints.add(points.get(points.size() - 1));
		}

		RouteImporter routeImporter = new RouteImporter(segment, routePoints);
		List<RouteSegmentResult> routeSegments = routeImporter.importRoute();

		for (int i = 0; i < routePoints.size() - 1; i++) {
			Pair<WptPt, WptPt> pair = new Pair<>(routePoints.get(i), routePoints.get(i + 1));
			int startIndex = pair.first.getTrkPtIndex();
			if (startIndex < 0 || startIndex < prevPointIndex || startIndex >= points.size()) {
				startIndex = MeasurementEditingContextUtils.findPointIndex(pair.first, points, prevPointIndex);
			}
			int endIndex = pair.second.getTrkPtIndex();
			if (endIndex < 0 || endIndex < startIndex || endIndex >= points.size()) {
				endIndex = MeasurementEditingContextUtils.findPointIndex(pair.second, points, startIndex);
			}
			// end index is not inclusive, so increment it to include last point of TrkSegment
			if (endIndex + 1 == points.size()) {
				endIndex++;
			}

			if (startIndex >= 0 && endIndex >= 0) {
				List<WptPt> pairPoints = new ArrayList<>();
				for (int j = startIndex; j < endIndex && j < points.size(); j++) {
					pairPoints.add(points.get(j));
					prevPointIndex = j;
				}
				if (points.size() > prevPointIndex + 1 && i == routePoints.size() - 2) {
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
		return routePoints;
	}

	public List<WptPt> setPoints(GpxRouteApproximation gpxApproximation, List<WptPt> originalPoints, ApplicationMode mode, boolean useExternalTimestamps) {
		if (gpxApproximation == null || Algorithms.isEmpty(gpxApproximation.finalPoints) || Algorithms.isEmpty(gpxApproximation.result)) {
			return null;
		}
		List<GpxPoint> gpxPoints = gpxApproximation.finalPoints;
		WptPt firstOriginalPoint = originalPoints.get(0);
		WptPt lastOriginalPoint = originalPoints.get(originalPoints.size() - 1);
		int originalPointIndex = -1;
		long lastOriginalPointTime = 0;
		double dist = 0;
		WptPt originalPoint = null;
		List<RouteSegmentResult> pendingSegments = new ArrayList<>();
		boolean modifySegments = useExternalTimestamps && firstOriginalPoint.time > 0 && lastOriginalPoint.time > 0;
		if (modifySegments) {
			originalPointIndex = 1;
			lastOriginalPointTime = firstOriginalPoint.time;
			originalPoint = originalPoints.get(originalPointIndex);
		}
		List<WptPt> routePoints = new ArrayList<>();
		List<RouteSegmentResult> allSegments = new ArrayList<>();
		WptPt addedPoint = null;
		for (int i = 0; i < gpxPoints.size(); i++) {
			GpxPoint gp1 = gpxPoints.get(i);
			boolean lastGpxPoint = MeasurementEditingContextUtils.isLastGpxPoint(gpxPoints, i);
			List<WptPt> points = new ArrayList<>();
			List<RouteSegmentResult> segments = new ArrayList<>();
			for (int k = 0; k < gp1.routeToTarget.size(); k++) {
				RouteSegmentResult seg = gp1.routeToTarget.get(k);
				if (seg.getStartPointIndex() != seg.getEndPointIndex()) {
					segments.add(seg);
				}
			}
			List<RouteSegmentResult> modifiedSegments = new ArrayList<>();
			boolean duplicatePoint = needDuplicatePoint(gpxPoints, i);
			for (int k = 0; k < segments.size(); k++) {
				RouteSegmentResult seg = segments.get(k);
				boolean includeEndPoint = (duplicatePoint || lastGpxPoint) && k == segments.size() - 1;
				if (!modifySegments) {
					MeasurementEditingContextUtils.fillPointsArray(points, seg, includeEndPoint);
				} else {
					int ind = seg.getStartPointIndex();
					boolean plus = seg.isForwardDirection();
					float[] heightArray = seg.getObject().calculateHeightArray();
					boolean segmentAdded = false;
					while (ind != seg.getEndPointIndex()) {
						WptPt prevAddedPoint = addedPoint;
						addedPoint = MeasurementEditingContextUtils.addPointToArray(points, seg, ind, heightArray);
						if (prevAddedPoint != null) {
							dist += MapUtils.getDistance(prevAddedPoint.lat, prevAddedPoint.lon, addedPoint.lat, addedPoint.lon);
						}
						ind = plus ? ind + 1 : ind - 1;
						if (originalPoint != null && MapUtils.getDistance(originalPoint.lat, originalPoint.lon, addedPoint.lat, addedPoint.lon) < 20) {
							if (ind != seg.getEndPointIndex()) {
							/* Could be used for more precise estimation
							RouteSegmentResult newSeg = new RouteSegmentResult(seg.getObject(), seg.getStartPointIndex(), ind);
							modifiedSegments.add(newSeg);
							pendingSegments.add(newSeg);
							seg = new RouteSegmentResult(seg.getObject(), ind, seg.getEndPointIndex());
							*/
							} else {
								modifiedSegments.add(seg);
								pendingSegments.add(seg);
								segmentAdded = true;
							}
							long originalPointTime = originalPoint.time;
							if (originalPointIndex + 1 < originalPoints.size()) {
								originalPoint = originalPoints.get(++originalPointIndex);
							}
							if (originalPointTime > 0 && originalPointTime > lastOriginalPointTime
									&& originalPoint != lastOriginalPoint && originalPoint.time > originalPointTime) {
								double speed = dist / ((originalPointTime - lastOriginalPointTime) / 1000.0);
								if (speed > 0 && !pendingSegments.isEmpty()) {
									for (RouteSegmentResult segment : pendingSegments) {
										segment.setSegmentSpeed((float) speed);
									}
									dist = 0;
									pendingSegments.clear();
									lastOriginalPointTime = originalPointTime;
								}
							}
						}
					}
					if (!segmentAdded) {
						modifiedSegments.add(seg);
						pendingSegments.add(seg);
					}
					if (lastGpxPoint && k == segments.size() - 1) {
						WptPt prevAddedPoint = addedPoint;
						addedPoint = MeasurementEditingContextUtils.addPointToArray(points, seg, ind, heightArray);
						if (prevAddedPoint != null) {
							dist += MapUtils.getDistance(prevAddedPoint.lat, prevAddedPoint.lon, addedPoint.lat, addedPoint.lon);
						}
						if (originalPoint != null) {
							long originalPointTime = lastOriginalPoint.time;
							if (originalPointTime > 0 && originalPointTime > lastOriginalPointTime) {
								double speed = dist / ((originalPointTime - lastOriginalPointTime) / 1000.0);
								if (speed > 0) {
									for (RouteSegmentResult segment : pendingSegments) {
										segment.setSegmentSpeed((float) speed);
									}
									dist = 0;
									pendingSegments.clear();
									lastOriginalPointTime = originalPointTime;
								}
							}
						}
					}
					if (duplicatePoint && k == segments.size() - 1) {
						MeasurementEditingContextUtils.addPointToArray(points, seg, ind, heightArray);
					}
				}
			}
			if (modifySegments) {
				segments = modifiedSegments;
			}
			allSegments.addAll(segments);

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

		if (modifySegments) {
			RouteResultPreparation.recalculateTimeDistance(allSegments);
			/* Could be used after split segments
			RouteResultPreparation preparation = new RouteResultPreparation();
			for (RouteSegmentResult r : allSegments) {
				r.setTurnType(null);
				r.setDescription("");
			}
			preparation.prepareTurnResults(gpxApproximation.ctx, allSegments);
			*/
			calculatedTimeSpeed = true;
		} else {
			calculatedTimeSpeed = false;
		}

		double calculatedDuration = 0;
		for (RouteSegmentResult s : allSegments) {
			calculatedDuration += s.getSegmentTime();
		}
		long originalDuration = lastOriginalPoint.time - firstOriginalPoint.time;
		LOG.debug("Approximation result: start=" + firstOriginalPoint.lat + ", " + firstOriginalPoint.lon +
				" finish=" + lastOriginalPoint.lat + ", " + lastOriginalPoint.lon +
				" calculatedTime=" + calculatedDuration + "s originalTime=" + originalDuration / 1000.0 + "s");
		WptPt lastRoutePoint = routePoints.get(routePoints.size() - 1);
		if (lastOriginalPoint.isGap()) {
			lastRoutePoint.setGap();
		}
		replacePoints(originalPoints, routePoints);
		return routePoints;
	}

	private boolean needDuplicatePoint(List<GpxPoint> gpxPoints, int index) {
		if (index == gpxPoints.size() - 1) {
			return false;
		}
		List<RouteSegmentResult> routeToTarget = gpxPoints.get(index).routeToTarget;
		List<RouteSegmentResult> routeToTargetNext = gpxPoints.get(index + 1).routeToTarget;
		return routeToTarget.get(routeToTarget.size() - 1).getEndPoint()
				.equals(routeToTargetNext.get(0).getStartPoint());
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
		Pair<WptPt, WptPt> currentPair = pointsToCalculate.get(0);
		Location start = new Location("");
		start.setLatitude(currentPair.first.getLatitude());
		start.setLongitude(currentPair.first.getLongitude());

		LatLon end = new LatLon(currentPair.second.getLatitude(), currentPair.second.getLongitude());

		RouteRegion reg = new RouteRegion();
		reg.initRouteEncodingRule(0, "highway", RouteResultPreparation.UNMATCHED_HIGHWAY_TYPE);

		RouteCalculationParams params = new RouteCalculationParams();
		params.start = start;

		ApplicationMode appMode = ApplicationMode.valueOfStringKey(currentPair.first.getProfileType(), DEFAULT_APP_MODE);
		params.end = end;
		RoutingHelper.applyApplicationSettings(params, application.getSettings(), appMode);
		params.mode = appMode;
		params.ctx = application;
		params.calculationProgress = calculationProgress = new RouteCalculationProgress();
		params.calculationProgressListener = new RouteCalculationProgressListener() {

			@Override
			public void onCalculationStart() {
			}

			@Override
			public void onUpdateCalculationProgress(int progress) {
				int pairs = pointsToCalculateSize;
				if (pairs != 0) {
					float pairProgress = 100f / pairs;
					progress = (int) (calculatedPairs * pairProgress + (float) progress / pairs);
				}
				progressListener.updateProgress(progress);
			}

			@Override
			public void onRequestPrivateAccessRouting() {
			}

			@Override
			public void onUpdateMissingMaps(@Nullable List<WorldRegion> missingMaps, boolean onlineSearch) {
			}

			@Override
			public void onCalculationFinish() {
				calculatedPairs = 0;
				pointsToCalculateSize = 0;
			}
		};
		params.alternateResultListener = route -> {
			List<Location> locations = route.getRouteLocations();
			List<WptPt> pts = new ArrayList<>(locations.size());
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
			params.calculationProgressListener.onUpdateCalculationProgress(0);
			List<RouteSegmentResult> originalRoute = route.getOriginalRoute();
			if (Algorithms.isEmpty(originalRoute)) {
				if (pts.size() >= 2 && insertIntermediates) {
					pts = insertIntermediatePoints(pts);
				}
				originalRoute = Collections.singletonList(RoutePlannerFrontEnd.generateStraightLineSegment(
						DEFAULT_APP_MODE.getDefaultSpeed(), new LocationsHolder(pts).getLatLonList()));
			}
			roadSegmentData.put(currentPair, new RoadSegmentData(route.getAppMode(), currentPair.first, currentPair.second, pts, originalRoute));
			application.runInUIThread(() -> {
				updateSegmentsForSnap(true, false);
				progressListener.refresh();
				RouteCalculationParams params1 = getParams(false);
				if (params1 != null) {
					application.getRoutingHelper().startRouteCalculationThread(params1);
				} else {
					progressListener.hideProgressBar();
				}
			});
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
		List<WptPt> points = null;
		GpxData gpxData = getGpxData();
		if (gpxData != null && gpxData.getGpxFile() != null) {
			points = gpxData.getGpxFile().getPoints();
		}

		return RouteExporter.exportRoute(gpxName, getRouteSegments(), points, getRoutePoints());
	}

	private TrkSegment getRouteSegment(int startPointIndex, int endPointIndex) {
		List<RouteSegmentResult> route = new ArrayList<>();
		List<Location> locations = new ArrayList<>();
		List<Integer> routePointIndexes = new ArrayList<>();
		routePointIndexes.add(0);

		for (int i = startPointIndex; i < endPointIndex; i++) {
			Pair<WptPt, WptPt> pair = new Pair<>(before.points.get(i), before.points.get(i + 1));
			RoadSegmentData data = this.roadSegmentData.get(pair);
			List<WptPt> dataPoints = data != null ? data.getPoints() : null;
			List<RouteSegmentResult> dataSegments = data != null ? data.getSegments() : null;
			if (dataPoints != null && dataSegments != null) {
				for (WptPt pt : dataPoints) {
					Location l = new Location("");
					l.setLatitude(pt.getLatitude());
					l.setLongitude(pt.getLongitude());
					if (!Double.isNaN(pt.ele)) {
						l.setAltitude(pt.ele);
					}
					locations.add(l);
				}

				int routePointIndex = i + 1 == endPointIndex ? locations.size() - 1 : locations.size();
				pair.second.setTrkPtIndex(routePointIndex);
				route.addAll(dataSegments);
				routePointIndexes.add(routePointIndex);
			}
		}
		if (!locations.isEmpty() && !route.isEmpty()) {
			before.points.get(startPointIndex).setTrkPtIndex(0);
			return new RouteExporter("", route, locations, routePointIndexes, null).generateRouteSegment();
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

	@NonNull
	private List<WptPt> insertIntermediatePoints(@NonNull List<WptPt> originalPoints) {
		if (originalPoints.size() < 2) {
			return originalPoints;
		}

		List<WptPt> newPoints = new ArrayList<>();

		for (int i = 0; i < originalPoints.size() - 1; i++) {
			WptPt startPoint = originalPoints.get(i);
			WptPt endPoint = originalPoints.get(i + 1);

			newPoints.add(startPoint);

			List<WptPt> intermediatePoints = calculateIntermediatePoints(startPoint, endPoint);
			if (!Algorithms.isEmpty(intermediatePoints)) {
				newPoints.addAll(intermediatePoints);
			}

			if (i + 1 == originalPoints.size() - 1) {
				newPoints.add(endPoint);
			}
		}

		return newPoints;
	}

	@Nullable
	private List<WptPt> calculateIntermediatePoints(@NonNull WptPt start, @NonNull WptPt end) {
		double distance = MapUtils.getDistance(start.lat, start.lon, end.lat, end.lon);
		int intermediatePointsCount = (int) (distance / MIN_METERS_BETWEEN_INTERMEDIATES) - 1;
		if (intermediatePointsCount < 1) {
			return null;
		}

		List<WptPt> points = new ArrayList<>(intermediatePointsCount);
		for (int i = 0; i < intermediatePointsCount; i++) {
			double coeff = (double) (i + 1) / ( intermediatePointsCount + 1);
			LatLon intermediateLatLon = MapUtils.calculateIntermediatePoint(start.lat, start.lon, end.lat, end.lon, coeff);
			WptPt intermediatePoint = new WptPt();
			intermediatePoint.lat = intermediateLatLon.getLatitude();
			intermediatePoint.lon = intermediateLatLon.getLongitude();
			points.add(intermediatePoint);
		}

		return points;
	}

	private boolean shouldAddIntermediates(@NonNull WptPt start, @NonNull WptPt end) {
		return insertIntermediates
				&& (start.getProfileType() == null || start.getProfileType().equals(DEFAULT_APP_MODE.getStringKey()))
				&& !end.isGap()
				&& (int) (MapUtils.getDistance(start.lat, start.lon, end.lat, end.lon) / MIN_METERS_BETWEEN_INTERMEDIATES) >= 2;
	}

	public boolean isInMultiProfileMode() {
		Set<String> profiles = new HashSet<>();
		List<TrkSegment> allSegments = new ArrayList<>();
		allSegments.addAll(beforeSegments);
		allSegments.addAll(afterSegments);
		for (TrkSegment segment : allSegments) {
			List<WptPt> points = segment.points;
			if (Algorithms.isEmpty(points)) {
				continue;
			}
			for (int i = 0; i < points.size() / 2 + 1; i++) {
				WptPt left = points.get(i);
				int rightIdx = points.size() - 1 - i;
				WptPt right = points.get(rightIdx);
				if (!left.isGap() && i + 1 < points.size()) {
					profiles.add(left.getProfileType());
				}
				if (!right.isGap() && rightIdx + 1 < points.size()) {
					profiles.add(right.getProfileType());
				}
				if (profiles.size() >= 2) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void onRouteSettingsChanged(@Nullable ApplicationMode mode) {
		recalculateRouteSegments(mode);
	}
}
