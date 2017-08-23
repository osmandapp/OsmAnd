package net.osmand.plus.measurementtool;

import android.util.Pair;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.views.Renderable;

import java.util.HashMap;
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
	private Pair<WptPt, WptPt> currentPair;
	private Queue<Pair<WptPt, WptPt>> snapToRoadPairsToCalculate = new LinkedList<>();
	private Map<Pair<WptPt, WptPt>, List<WptPt>> snappedToRoadPoints = new HashMap<>();

	private List<WptPt> measurementPoints = new LinkedList<>();

	public void setBefore(TrkSegment before) {
		this.before = before;
		addBeforeRenders();
	}

	public void setAfter(TrkSegment after) {
		this.after = after;
		addAfterRenders();
	}

	private void addBeforeRenders() {
		before.renders.add(new Renderable.StandardTrack(before.points, 17.2));
	}

	private void addAfterRenders() {
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

	public Queue<Pair<WptPt, WptPt>> getSnapToRoadPairsToCalculate() {
		return snapToRoadPairsToCalculate;
	}

	public void setSnapToRoadPairsToCalculate(Queue<Pair<WptPt, WptPt>> snapToRoadPairsToCalculate) {
		this.snapToRoadPairsToCalculate = snapToRoadPairsToCalculate;
	}

	public Map<Pair<WptPt, WptPt>, List<WptPt>> getSnappedPoints() {
		return snappedToRoadPoints;
	}

	public List<WptPt> getPoints() {
		return measurementPoints;
	}

	public int getPointsCount() {
		return measurementPoints.size();
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

	public void recreateSegments() {
		before = new TrkSegment();
		before.points.addAll(measurementPoints);
		addBeforeRenders();
		after = new TrkSegment();
	}

	public void recreateSegments(int position) {
		before = new TrkSegment();
		before.points.addAll(measurementPoints.subList(0, position));
		addBeforeRenders();
		after = new TrkSegment();
		if (position != measurementPoints.size() - 1) {
			after.points.addAll(measurementPoints.subList(position + 1, measurementPoints.size()));
			addAfterRenders();
		}
	}

	public void clearSegments() {
		before = new TrkSegment();
		after = new TrkSegment();
	}
}
