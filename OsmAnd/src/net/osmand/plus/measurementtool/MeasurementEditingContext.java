package net.osmand.plus.measurementtool;

import android.util.Pair;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.views.Renderable;
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
	private boolean inAddPointBeforeMode;
	private boolean inAddPointAfterMode;

	private boolean isInSnapToRoadMode;
	private ApplicationMode snapToRoadAppMode;
	private RoutingContext routingContext;
	private Queue<Pair<WptPt, WptPt>> snapToRoadPairsToCalculate = new LinkedList<>();
	private Map<Pair<WptPt, WptPt>, List<WptPt>> snappedToRoadPoints = new LinkedHashMap<>();

	public void setBefore(TrkSegment before) {
		this.before = before;
		addBeforeRenders();
	}

	public void setAfter(TrkSegment after) {
		this.after = after;
		addAfterRenders();
	}

	public void addBeforeRenders() {
		before.renders.add(new Renderable.StandardTrack(before.points, 17.2));
	}

	public void addAfterRenders() {
		after.renders.add(new Renderable.StandardTrack(after.points, 17.2));
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

	public void setInAddPointBeforeMode(boolean inAddPointBeforeMode) {
		this.inAddPointBeforeMode = inAddPointBeforeMode;
	}

	public boolean isInAddPointBeforeMode() {
		return inAddPointBeforeMode;
	}

	public void setInAddPointAfterMode(boolean inAddPointAfterMode) {
		this.inAddPointAfterMode = inAddPointAfterMode;
	}

	public boolean isInAddPointAfterMode() {
		return inAddPointAfterMode;
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
		return before;
	}

	public TrkSegment getAfterTrkSegmentLine() {
		return after;
	}

	public void scheduleRouteCalculateIfNotEmpty() {

	}
}
