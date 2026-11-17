package net.osmand.plus.measurementtool;

import static net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationMode.WHOLE_TRACK;
import static net.osmand.plus.measurementtool.command.MeasurementModeCommand.MeasurementCommandType.APPROXIMATE_POINTS;
import static net.osmand.plus.routing.TransportRoutingHelper.PUBLIC_TRANSPORT_KEY;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXUtilities;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.measurementtool.command.ApplyGpxApproximationCommand;
import net.osmand.plus.measurementtool.command.MeasurementCommandManager;
import net.osmand.plus.measurementtool.command.MeasurementModeCommand;
import net.osmand.plus.routing.IRouteSettingsListener;
import net.osmand.plus.routing.RouteCalculationParams;
import net.osmand.plus.routing.RouteCalculationProgressListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.*;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.*;
import java.util.Map.Entry;
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
		for (List<WptPt> points : Arrays.asList(before.getPoints(), after.getPoints())) {
			for (int i = 0; i < points.size() - 1; i++) {
				Pair<WptPt, WptPt> pair = new Pair<>(points.get(i), points.get(i + 1));
				RoadSegmentData data = this.roadSegmentData.get(pair);
				if (data == null) {
					if (appMode != DEFAULT_APP_MODE || !pair.first.getLastPoint() || !pair.second.getFirstPoint()) {
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
				if (point.getTime() != 0) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isAddNewSegmentAllowed() {
		return beforeSegments.size() > 0 && beforeSegments.get(beforeSegments.size() - 1).getPoints().size() >= 2;
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
			points.addAll(segment.getPoints());
		}
		return points;
	}

	public List<List<WptPt>> getSegmentsPoints() {
		return getSegmentsPoints(true, true);
	}

	@NonNull
	public List<List<WptPt>> getSegmentsPoints(boolean plain, boolean route) {
		List<List<WptPt>> res = new ArrayList<>();
		List<WptPt> beforePoints = getPoints();
		List<WptPt> allBeforePoints = getAllBeforePoints();
		List<WptPt> allPoints = beforePoints.size() > 2 ? beforePoints : allBeforePoints;
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
		return before.getPoints();
	}

	List<WptPt> getAfterPoints() {
		return after.getPoints();
	}

	public int getPointsCount() {
		return before.getPoints().size();
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
		points.addAll(before.getPoints());
		points.addAll(after.getPoints());
		before.getPoints().clear();
		after.getPoints().clear();
		before.getPoints().addAll(points.subList(0, position));
		after.getPoints().addAll(points.subList(position, points.size()));
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
		before.getPoints().add(pt);
		updateSegmentsForSnap(false);
	}

	public void addPoint(int position, WptPt pt) {
		addPoint(position, pt, AdditionMode.UNDEFINED);
	}

	public void addPoint(int position, WptPt pt, AdditionMode additionMode) {
		if (additionMode == AdditionMode.ADD_AFTER || additionMode == AdditionMode.ADD_BEFORE) {
			preAddPoint(position, additionMode, pt);
		}
		before.getPoints().add(position, pt);
		updateSegmentsForSnap(false);
	}

	public void addPoints(List<WptPt> points) {
		before.getPoints().addAll(points);
		updateSegmentsForSnap(false);
	}

	public void replacePoints(List<WptPt> originalPoints, List<WptPt> points) {
		if (originalPoints.size() > 1) {
			int firstPointIndex = getPointIndexToReplace(before.getPoints(), originalPoints.get(0));
			int lastPointIndex = getPointIndexToReplace(before.getPoints(), originalPoints.get(originalPoints.size() - 1));
			List<WptPt> newPoints = new ArrayList<>();
			if (firstPointIndex != -1 && lastPointIndex != -1) {
				newPoints.addAll(before.getPoints().subList(0, firstPointIndex));
				newPoints.addAll(points);
				if (before.getPoints().size() > lastPointIndex + 1) {
					newPoints.addAll(before.getPoints().subList(lastPointIndex + 1, before.getPoints().size()));
				}
			} else {
				newPoints.addAll(points);
			}
			before.setPoints(newPoints);
		} else {
			before.setPoints(points);
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
		if (position < 0 || position >= before.getPoints().size()) {
			return new WptPt();
		}
		WptPt pt = before.getPoints().get(position);
		if (updateSnapToRoad && position > 0 && pt.isGap()) {
			WptPt prevPt = before.getPoints().get(position - 1);
			if (!prevPt.isGap()) {
				prevPt.setGap();
			}
		}
		before.getPoints().remove(position);
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
		if (pointIndex >= 0 && pointIndex < before.getPoints().size()) {
			WptPt point = before.getPoints().get(pointIndex);
			WptPt nextPoint = before.getPoints().size() > pointIndex + 1 ? before.getPoints().get(pointIndex + 1) : null;
			WptPt newPoint = new WptPt(point);
			newPoint.copyExtensions(point);
			newPoint.setGap();
			before.getPoints().remove(pointIndex);
			before.getPoints().add(pointIndex, newPoint);
			roadSegmentData.remove(new Pair<>(point, nextPoint));
			updateSegmentsForSnap(false);
		}
	}

	public void joinPoints(int selectedPointPosition) {
		WptPt gapPoint = null;
		int gapIndex = -1;
		if (isFirstPointSelected(selectedPointPosition, false)) {
			if (selectedPointPosition - 1 >= 0) {
				gapPoint = before.getPoints().get(selectedPointPosition - 1);
				gapIndex = selectedPointPosition - 1;
			}
		} else if (isLastPointSelected(selectedPointPosition, false)) {
			gapPoint = before.getPoints().get(selectedPointPosition);
			gapIndex = selectedPointPosition;
		}
		if (gapPoint != null) {
			WptPt newPoint = new WptPt(gapPoint);
			newPoint.copyExtensions(gapPoint);
			newPoint.removeProfileType();
			before.getPoints().remove(gapIndex);
			before.getPoints().add(gapIndex, newPoint);
			updateSegmentsForSnap(false);
		}
	}

	public void clearSegments() {
		clearBeforeSegments();
		clearAfterSegments();
		clearSnappedToRoadPoints();
	}

	public void clearBeforeSegments() {
		before.getPoints().clear();
		if (beforeSegments != null) {
			beforeSegments.clear();
		}
		if (beforeSegmentsForSnap != null) {
			beforeSegmentsForSnap.clear();
		}
	}

	public void clearAfterSegments() {
		after.getPoints().clear();
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
			int i = segment.getPoints().indexOf(selectedPoint);
			if (i != -1) {
				return after ? i < segment.getPoints().size() - 2 : i > 1;
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
			int i = segment.getPoints().indexOf(selectedPoint);
			if (i != -1) {
				int segmentPosition = selectedPointPosition - count;
				return first ? segmentPosition == 0 : segmentPosition == segment.getPoints().size() - 1;
			} else {
				count += segment.getPoints().size();
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
		if (application == null || (before.getPoints().size() == 0 && after.getPoints().size() == 0)) {
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
		for (List<WptPt> points : Arrays.asList(before.getPoints(), after.getPoints())) {
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
		for (List<WptPt> points : Arrays.asList(before.getPoints(), after.getPoints())) {
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

				s.getPoints().add(point);
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
						if (!s.getPoints().isEmpty()) {
							s = new TrkSegment();
							segments.add(s);
							defaultMode = true;
						}
					}
				}
			}
		} else {
			s.getPoints().addAll(points);
		}
		if (s.getPoints().isEmpty()) {
			segments.remove(s);
		}
		if (!segments.isEmpty()) {
			for (TrkSegment segment : segments) {
				TrkSegment segmentForSnap = new TrkSegment();
				for (int i = 0; i < segment.getPoints().size() - 1; i++) {
					Pair<WptPt, WptPt> pair = new Pair<>(segment.getPoints().get(i), segment.getPoints().get(i + 1));
					RoadSegmentData data = this.roadSegmentData.get(pair);
					List<WptPt> pts = data != null ? data.getPoints() : null;
					if (pts != null) {
						segmentForSnap.getPoints().addAll(pts);
					} else {
						if (calculateIfNeeded && roadSegmentIndexes.contains(segmentsForSnap.size())) {
							scheduleRouteCalculateIfNotEmpty();
						}
						segmentForSnap.getPoints().addAll(Arrays.asList(pair.first, pair.second));
					}
				}
				if (segmentForSnap.getPoints().isEmpty()) {
					segmentForSnap.getPoints().addAll(segment.getPoints());
				}
				segmentsForSnap.add(segmentForSnap);
			}
		} else if (!points.isEmpty()) {
			TrkSegment segmentForSnap = new TrkSegment();
			segmentForSnap.getPoints().addAll(points);
			segmentsForSnap.add(segmentForSnap);
		}
	}

	void addPoints() {
		GpxData gpxData = getGpxData();
		if (gpxData == null || gpxData.getGpxFile() == null) {
			return;
		}
		GpxFile gpxFile = gpxData.getGpxFile();
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
				addPoints(segment.getPoints());
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
					List<WptPt> points = segment.getPoints();
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
		List<WptPt> points = segment.getPoints();
		if (routePoints.isEmpty() && points.size() > 1) {
			routePoints.add(points.get(0));
			routePoints.add(points.get(points.size() - 1));
		}

		GPXUtilities.TrkSegment jTrkSegment = SharedUtil.jTrkSegment(segment);
		List<GPXUtilities.WptPt> jRoutePoints = SharedUtil.jWptPtList(routePoints);
		RouteImporter routeImporter = new RouteImporter(jTrkSegment, jRoutePoints);
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

	public List<WptPt> setPoints(GpxRouteApproximation gpxApproximation, List<WptPt> originalPoints,
                                 ApplicationMode mode, boolean useExternalTimestamps) {
		if (gpxApproximation == null ||
				Algorithms.isEmpty(gpxApproximation.finalPoints) || Algorithms.isEmpty(originalPoints)) {
			return null;
		}

		calculatedTimeSpeed = useExternalTimestamps;

		List<GpxPoint> gpxPoints = gpxApproximation.finalPoints;
		WptPt firstOriginalPoint = originalPoints.get(0);
		WptPt lastOriginalPoint = originalPoints.get(originalPoints.size() - 1);
		List<WptPt> routePoints = new ArrayList<>();
		List<RouteSegmentResult> allSegments = new ArrayList<>();
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
			boolean duplicatePoint = needDuplicatePoint(gpxPoints, i);
			for (int k = 0; k < segments.size(); k++) {
				RouteSegmentResult seg = segments.get(k);
				boolean includeEndPoint = (duplicatePoint || lastGpxPoint) && k == segments.size() - 1;
				MeasurementEditingContextUtils.fillPointsArray(points, seg, includeEndPoint);
			}
			allSegments.addAll(segments);

			if (!points.isEmpty()) {
				WptPt wp1 = new WptPt();
				wp1.setLat(gp1.loc.getLatitude());
				wp1.setLon(gp1.loc.getLongitude());
				wp1.setProfileType(mode.getStringKey());
				routePoints.add(wp1);
				WptPt wp2 = new WptPt();
				if (lastGpxPoint) {
					wp2.setLat(points.get(points.size() - 1).getLatitude());
					wp2.setLon(points.get(points.size() - 1).getLongitude());
					routePoints.add(wp2);
				} else {
					GpxPoint gp2 = gpxPoints.get(i + 1);
					wp2.setLat(gp2.loc.getLatitude());
					wp2.setLon(gp2.loc.getLongitude());
				}
				wp2.setProfileType(mode.getStringKey());
				Pair<WptPt, WptPt> pair = new Pair<>(wp1, wp2);
				roadSegmentData.put(pair, new RoadSegmentData(appMode, pair.first, pair.second, points, segments));
			}
			if (lastGpxPoint) {
				break;
			}
		}

		double calculatedDuration = 0;
		for (RouteSegmentResult s : allSegments) {
			calculatedDuration += s.getSegmentTime();
		}
		long originalDuration = lastOriginalPoint.getTime() - firstOriginalPoint.getTime();
		LOG.debug("Approximation result: start=" + firstOriginalPoint.getLat() + ", " + firstOriginalPoint.getLon() +
				" finish=" + lastOriginalPoint.getLat() + ", " + lastOriginalPoint.getLon() +
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
		return !Algorithms.isEmpty(routeToTarget) && !Algorithms.isEmpty(routeToTargetNext)
				&& routeToTarget.get(routeToTarget.size() - 1).getEndPoint()
				.equals(routeToTargetNext.get(0).getStartPoint());
	}

	private void updateSegmentsForSnap(boolean both) {
		recreateSegments(beforeSegments = new ArrayList<>(),
				beforeSegmentsForSnap = new ArrayList<>(), before.getPoints(), true);
		if (both) {
			recreateSegments(afterSegments = new ArrayList<>(),
					afterSegmentsForSnap = new ArrayList<>(), after.getPoints(), true);
		}
	}

	private void updateSegmentsForSnap(boolean both, boolean calculateIfNeeded) {
		recreateSegments(beforeSegments = new ArrayList<>(),
				beforeSegmentsForSnap = new ArrayList<>(), before.getPoints(), calculateIfNeeded);
		if (both) {
			recreateSegments(afterSegments = new ArrayList<>(),
					afterSegmentsForSnap = new ArrayList<>(), after.getPoints(), calculateIfNeeded);
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
				pt.setLat(loc.getLatitude());
				pt.setLon(loc.getLongitude());
				if (loc.hasAltitude()) {
					prevAltitude = loc.getAltitude();
					pt.setEle(prevAltitude);
				} else if (!Double.isNaN(prevAltitude)) {
					pt.setEle(prevAltitude);
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
				List<LatLon> latLonList = new ArrayList<>(pts.size());
				for (WptPt pt : pts) {
					latLonList.add(new LatLon(pt.getLatitude(), pt.getLongitude()));
				}
				originalRoute = Collections.singletonList(RoutePlannerFrontEnd.generateStraightLineSegment(
						DEFAULT_APP_MODE.getDefaultSpeed(), latLonList));
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

	public List<List<GPXUtilities.WptPt>> getRoutePoints() {
		List<List<GPXUtilities.WptPt>> res = new ArrayList<>();
		List<WptPt> plainPoints = new ArrayList<>(before.getPoints());
		plainPoints.addAll(after.getPoints());
		List<WptPt> points = new ArrayList<>();
		for (WptPt point : plainPoints) {
			if (point.getTrkPtIndex() != -1) {
				points.add(point);
				if (point.isGap()) {
					res.add(SharedUtil.jWptPtList(points));
					points = new ArrayList<>();
				}
			}
		}
		if (!points.isEmpty()) {
			res.add(SharedUtil.jWptPtList(points));
		}
		return res;
	}

	@Nullable
	public GpxFile exportGpx(@NonNull String gpxName) {
		if (application == null || before.getPoints().isEmpty()) {
			return null;
		}
		List<WptPt> points = null;
		GpxData gpxData = getGpxData();
		if (gpxData != null && gpxData.getGpxFile() != null) {
			points = gpxData.getGpxFile().getPointsList();
		}
		List<GPXUtilities.WptPt> jPoints = points != null ? SharedUtil.jWptPtList(points) : null;
		return SharedUtil.kGpxFile(
				RouteExporter.exportRoute(gpxName, getRouteSegments(), jPoints, getRoutePoints()));
	}

	private GPXUtilities.TrkSegment getRouteSegment(int startPointIndex, int endPointIndex) {
		List<RouteSegmentResult> route = new ArrayList<>();
		List<Location> locations = new ArrayList<>();
		List<Integer> routePointIndexes = new ArrayList<>();
		routePointIndexes.add(0);

		for (int i = startPointIndex; i < endPointIndex; i++) {
			Pair<WptPt, WptPt> pair = new Pair<>(before.getPoints().get(i), before.getPoints().get(i + 1));
			RoadSegmentData data = this.roadSegmentData.get(pair);
			List<WptPt> dataPoints = data != null ? data.getPoints() : null;
			List<RouteSegmentResult> dataSegments = data != null ? data.getSegments() : null;
			if (dataPoints != null && dataSegments != null) {
				for (WptPt pt : dataPoints) {
					Location l = new Location("");
					l.setLatitude(pt.getLatitude());
					l.setLongitude(pt.getLongitude());
					if (!Double.isNaN(pt.getEle())) {
						l.setAltitude(pt.getEle());
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
			before.getPoints().get(startPointIndex).setTrkPtIndex(0);
			return new RouteExporter("", route, locations, routePointIndexes, null).generateRouteSegment();
		} else if (endPointIndex - startPointIndex >= 0) {
			GPXUtilities.TrkSegment segment = new GPXUtilities.TrkSegment();
			segment.points = SharedUtil.jWptPtList(
					new ArrayList<>(before.getPoints().subList(startPointIndex, endPointIndex + 1)));
			return segment;
		}
		return null;
	}

	private List<GPXUtilities.TrkSegment> getRouteSegments() {
		List<GPXUtilities.TrkSegment> res = new ArrayList<>();
		List<Integer> lastPointIndexes = new ArrayList<>();
		for (int i = 0; i < before.getPoints().size(); i++) {
			WptPt pt = before.getPoints().get(i);
			if (pt.isGap()) {
				lastPointIndexes.add(i);
			}
		}
		if (lastPointIndexes.isEmpty() || lastPointIndexes.get(lastPointIndexes.size() - 1) < before.getPoints().size() - 1) {
			lastPointIndexes.add(before.getPoints().size() - 1);
		}
		int firstPointIndex = 0;
		for (Integer lastPointIndex : lastPointIndexes) {
			GPXUtilities.TrkSegment segment = getRouteSegment(firstPointIndex, lastPointIndex);
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
		double distance = MapUtils.getDistance(start.getLat(), start.getLon(), end.getLat(), end.getLon());
		int intermediatePointsCount = (int) (distance / MIN_METERS_BETWEEN_INTERMEDIATES) - 1;
		if (intermediatePointsCount < 1) {
			return null;
		}

		List<WptPt> points = new ArrayList<>(intermediatePointsCount);
		for (int i = 0; i < intermediatePointsCount; i++) {
			double coeff = (double) (i + 1) / (intermediatePointsCount + 1);
			LatLon intermediateLatLon = MapUtils.calculateIntermediatePoint(start.getLat(), start.getLon(), end.getLat(), end.getLon(), coeff);
			WptPt intermediatePoint = new WptPt();
			intermediatePoint.setLat(intermediateLatLon.getLatitude());
			intermediatePoint.setLon(intermediateLatLon.getLongitude());
			points.add(intermediatePoint);
		}

		return points;
	}

	private boolean shouldAddIntermediates(@NonNull WptPt start, @NonNull WptPt end) {
		return insertIntermediates
				&& (start.getProfileType() == null || start.getProfileType().equals(DEFAULT_APP_MODE.getStringKey()))
				&& !end.isGap()
				&& (int) (MapUtils.getDistance(start.getLat(), start.getLon(), end.getLat(), end.getLon()) / MIN_METERS_BETWEEN_INTERMEDIATES) >= 2;
	}

	public boolean isInMultiProfileMode() {
		Set<String> profiles = new HashSet<>();
		List<TrkSegment> allSegments = new ArrayList<>();
		allSegments.addAll(beforeSegments);
		allSegments.addAll(afterSegments);
		for (TrkSegment segment : allSegments) {
			List<WptPt> points = segment.getPoints();
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
