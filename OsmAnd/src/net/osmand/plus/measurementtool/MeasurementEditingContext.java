package net.osmand.plus.measurementtool;

import android.util.Pair;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.router.RoutingContext;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class MeasurementEditingContext {

	private TrkSegment before = new TrkSegment();
	// cache should be deleted if after changed or snappedToRoadPoints
	private TrkSegment beforeCacheForSnap;

	private TrkSegment after = new TrkSegment();
	// cache should be deleted if after changed or snappedToRoadPoints
	private TrkSegment afterCacheForSnap;

	private boolean inMovePointMode;

	private boolean isInSnapToRoadMode;
	private ApplicationMode snapToRoadAppMode;
	private RoutingContext routingContext;
	private Queue<Pair<WptPt, WptPt>> snapToRoadPairsToCalculate = new LinkedList<>();
	private Map<Pair<WptPt, WptPt>, List<WptPt>> snappedToRoadPoints = new LinkedHashMap<>();

	public void setBefore(TrkSegment before) {
		this.before = before;
	}

	public void setAfter(TrkSegment after) {
		this.after = after;
	}

	public boolean isInMovePointMode() {
		return inMovePointMode;
	}

	public void setInMovePointMode(boolean inMovePointMode) {
		this.inMovePointMode = inMovePointMode;
	}

	public boolean isInSnapToRoadMode() {
		return isInSnapToRoadMode;
	}

	public void setInSnapToRoadMode(boolean inSnapToRoadMode) {
		isInSnapToRoadMode = inSnapToRoadMode;
	}

	public ApplicationMode getSnapToRoadAppMode() {
		return snapToRoadAppMode;
	}

	public void setSnapToRoadAppMode(ApplicationMode snapToRoadAppMode) {
		this.snapToRoadAppMode = snapToRoadAppMode;
	}

	public RoutingContext getRoutingContext() {
		return routingContext;
	}

	public void setRoutingContext(RoutingContext routingContext) {
		this.routingContext = routingContext;
	}

	public Queue<Pair<WptPt, WptPt>> getSnapToRoadPairsToCalculate() {
		return snapToRoadPairsToCalculate;
	}

	public void setSnapToRoadPairsToCalculate(Queue<Pair<WptPt, WptPt>> snapToRoadPairsToCalculate) {
		this.snapToRoadPairsToCalculate = snapToRoadPairsToCalculate;
	}

	public Map<Pair<WptPt, WptPt>, List<WptPt>> getSnappedToRoadPoints() {
		return snappedToRoadPoints;
	}

	public void setSnappedToRoadPoints(Map<Pair<WptPt, WptPt>, List<WptPt>> snappedToRoadPoints) {
		this.snappedToRoadPoints = snappedToRoadPoints;
	}

	public TrkSegment getBeforeTrkSegmentLine() {
		// check if all segments calculated for snap to road
//		if(beforeCacheForSnap != null) {
//			return	beforeCacheForSnap;
//		}
		// calculate beforeCacheForSnap
		return null;
	}

	public TrkSegment getAfterTrkSegmentLine() {
		return null;
	}

	public void scheduleRouteCalculateIfNotEmpty() {

	}
}
