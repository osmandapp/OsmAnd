package net.osmand.router;

import net.osmand.map.WorldRegion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RouteCalculationProgress {

	public int segmentNotFound = -1;
	public float distanceFromBegin;
	public float directDistance;
	public int directSegmentQueueSize;
	public float distanceFromEnd;
	public int reverseSegmentQueueSize;
	public float reverseDistance;
	public float totalEstimatedDistance = 0;

	public float totalApproximateDistance = 0;
	public float approximatedDistance;

	public float routingCalculatedTime = 0;

	public int visitedSegments = 0;
	public int visitedDirectSegments = 0;
	public int visitedOppositeSegments = 0;
	public int directQueueSize = 0;
	public int oppositeQueueSize = 0;
	public int finalSegmentsFound = 0;

	public int totalIterations = 1;
	public int iteration = -1;
	
	public long timeNanoToCalcDeviation = 0;
	public long timeToLoad = 0;
	public long timeToLoadHeaders = 0;
	public long timeToFindInitialSegments = 0;
	public long timeToCalculate = 0;
	
	public int distinctLoadedTiles = 0;
	public int maxLoadedTiles = 0;
	public int loadedPrevUnloadedTiles = 0;
	public int unloadedTiles = 0;
	public int loadedTiles = 0;
	
	public boolean isCancelled;
	public boolean requestPrivateAccessRouting;

	public long routeCalculationStartTime;
	public List<WorldRegion> missingMaps;
	private HHIteration hhIterationStep;
	

	private static final float INITIAL_PROGRESS = 0.05f;
	private static final float FIRST_ITERATION = 0.72f;
	
	
	
	public static RouteCalculationProgress capture(RouteCalculationProgress cp) {
		RouteCalculationProgress p = new RouteCalculationProgress();
		p.timeNanoToCalcDeviation = cp.timeNanoToCalcDeviation;
		p.timeToCalculate = cp.timeToCalculate;
		p.timeToLoadHeaders = cp.timeToLoadHeaders;
		p.timeToFindInitialSegments = cp.timeToFindInitialSegments;
		p.timeToLoad = cp.timeToLoad;
		
		p.visitedSegments = cp.visitedSegments;
		p.directQueueSize = cp.directQueueSize;
		p.reverseSegmentQueueSize = cp.reverseSegmentQueueSize;
		p.visitedDirectSegments = cp.visitedDirectSegments;
		p.visitedOppositeSegments = cp.visitedOppositeSegments;
		p.finalSegmentsFound = cp.finalSegmentsFound;
		
		p.loadedTiles = cp.loadedTiles;
		p.distinctLoadedTiles = cp.distinctLoadedTiles;
		p.maxLoadedTiles = cp.maxLoadedTiles;
		p.loadedPrevUnloadedTiles = cp.loadedPrevUnloadedTiles;
		
		cp.maxLoadedTiles = 0;
		return p;
	}
	
	public Map<String, Object> getInfo(RouteCalculationProgress firstPhase) {
		TreeMap<String, Object> map = new TreeMap<String, Object>();
		TreeMap<String, Object> tiles = new TreeMap<String, Object>();
		if (firstPhase == null) {
			firstPhase = new RouteCalculationProgress();
		}
		map.put("tiles", tiles);
		tiles.put("loadedTiles", this.loadedTiles - firstPhase.loadedTiles);
		tiles.put("loadedTilesDistinct", this.distinctLoadedTiles - firstPhase.distinctLoadedTiles);
		tiles.put("loadedTilesPrevUnloaded", this.loadedPrevUnloadedTiles - firstPhase.loadedPrevUnloadedTiles);
		tiles.put("loadedTilesMax", Math.max(this.maxLoadedTiles, this.distinctLoadedTiles));
		tiles.put("unloadedTiles", this.unloadedTiles - firstPhase.unloadedTiles);
		Map<String, Object> segms = new LinkedHashMap<String, Object>();
		map.put("segments", segms);
		segms.put("visited", this.visitedSegments - firstPhase.visitedSegments);
		segms.put("queueDirectSize", this.directQueueSize - firstPhase.directQueueSize);
		segms.put("queueOppositeSize", this.reverseSegmentQueueSize  - firstPhase.reverseSegmentQueueSize);
		segms.put("visitedDirectPoints", this.visitedDirectSegments - firstPhase.visitedDirectSegments);
		segms.put("visitedOppositePoints", this.visitedOppositeSegments - firstPhase.visitedOppositeSegments);
		segms.put("finalSegmentsFound", this.finalSegmentsFound -  firstPhase.finalSegmentsFound);
		Map<String, Object> time = new LinkedHashMap<String, Object>();
		map.put("time", time);
		float timeToCalc = (float) ((this.timeToCalculate - firstPhase.timeToCalculate) / 1.0e9);
		time.put("timeToCalculate", timeToCalc);
		float timeToLoad = (float) ((this.timeToLoad - firstPhase.timeToLoad) / 1.0e9);
		time.put("timeToLoad", timeToLoad);
		float timeToLoadHeaders= (float) ((this.timeToLoadHeaders - firstPhase.timeToLoadHeaders) / 1.0e9);
		time.put("timeToLoadHeaders", timeToLoadHeaders);
		float timeToFindInitialSegments = (float) ((this.timeToFindInitialSegments - firstPhase.timeToFindInitialSegments) / 1.0e9);
		time.put("timeToFindInitialSegments", timeToFindInitialSegments);
		float timeExtra = (float) ((this.timeNanoToCalcDeviation - firstPhase.timeNanoToCalcDeviation) / 1.0e9);
		time.put("timeExtra", timeExtra);
		Map<String, Object> metrics = new LinkedHashMap<String, Object>();
		map.put("metrics", metrics);
		if (timeToLoad + timeToLoadHeaders > 0) {
			metrics.put("tilesPerSec", (this.loadedTiles - firstPhase.loadedTiles) / (timeToLoad + timeToLoadHeaders));
		}
		float pureTime = timeToCalc - (timeToLoad + timeToLoadHeaders + timeToFindInitialSegments);
		if (pureTime > 0) {
			metrics.put("segmentsPerSec", (this.visitedSegments - firstPhase.visitedSegments) / pureTime);
		} else {
			metrics.put("segmentsPerSec", (float) 0);
		}
		return map;
	}
	
	public float getLinearProgress() {
		if(hhIterationStep != null) {
			double progress = 0;
			for (HHIteration i : HHIteration.values()) {
				if (i == hhIterationStep) {
					break;
				}
				progress += i.approxStepLength;
			}
			double intermediateProgress = 0.5;
			// 1. implement 2-3 reiterations progress
			// 2. implement in progress start/finish
			// 3. implement in progress routing
			// 4. implement in progress detailed
			progress += intermediateProgress * hhIterationStep.approxStepLength;
			return (float) Math.min(progress * 100f, 99);
		}
		float p = Math.max(distanceFromBegin, distanceFromEnd);
		float all = totalEstimatedDistance * 1.35f;
		float pr = 0;
		if (all > 0) {
			pr = Math.min(p * p / (all * all), 1);
		}
		float progress = INITIAL_PROGRESS;
		if (totalIterations <= 1) {
			progress = INITIAL_PROGRESS + pr * (1 - INITIAL_PROGRESS);
		} else if (totalIterations <= 2) {
			if (iteration < 1) {
				progress = pr * FIRST_ITERATION + INITIAL_PROGRESS;
			} else {
				progress = (INITIAL_PROGRESS + FIRST_ITERATION) + pr * (1 - FIRST_ITERATION - INITIAL_PROGRESS);
			}
		} else {
			progress = (float) ((iteration + Math.min(pr, 0.7)) / totalIterations);
		}
		return Math.min(progress * 100f, 99);
	}

	public float getApproximationProgress() {
		float progress = 0;
		if (totalApproximateDistance > 0) {
			progress = approximatedDistance / totalApproximateDistance;
		}
		progress = INITIAL_PROGRESS + progress * (1 - INITIAL_PROGRESS);
		return Math.min(progress * 100f, 99);
	}

	public void nextIteration() {
		iteration++;
		totalEstimatedDistance = 0;
	}

	public enum HHIteration {
		SELECT_REGIONS(0.05), LOAD_POINS(0.05), START_END_POINT(0.25), ROUTING(0.25), DETAILED(0.3), ALTERNATIVES(0.1), DONE(0);
		public final double approxStepLength;
		
		HHIteration(double approximate) {
			this.approxStepLength = approximate;
		}
	}
	public void hhIteration(HHIteration step) {
		this.hhIterationStep = step;
	}
}
