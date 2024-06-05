package net.osmand.router;

import java.util.LinkedHashMap;
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
	public MissingMapsCalculationResult missingMapsCalculationResult;

	private int hhIterationStep = HHIteration.HH_NOT_STARTED.ordinal();
	private int hhTargetsDone, hhTargetsTotal;
	private double hhCurrentStepProgress;

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

	public float getLinearProgressHH() {
		float progress = 0;
		for (HHIteration i : HHIteration.values()) {
			if (i.ordinal() == hhIterationStep) {
				progress += hhCurrentStepProgress * (float) i.approxStepPercent; // current step
				break;
			} else {
				progress += (float) i.approxStepPercent; // passed step
			}
		}

		// 1. implement 2-3 reiterations progress

		if (hhTargetsTotal > 0) {
			progress = (100f * hhTargetsDone + progress) / hhTargetsTotal; // intermediate points
		}

		return Math.min(progress, 99);
	}

	public float getLinearProgress() {
		if(hhIterationStep != HHIteration.HH_NOT_STARTED.ordinal()) {
			return getLinearProgressHH();
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
		HH_NOT_STARTED(0), // hhIteration is not filled
		SELECT_REGIONS(5),
		LOAD_POINTS(5),
		START_END_POINT(15),
		ROUTING(25),
		DETAILED(50),
		ALTERNATIVES(0), // disabled
		DONE(0); // success

		public final int approxStepPercent;

		HHIteration(int approximate) {
			this.approxStepPercent = approximate;
		}
	}

	public void hhIteration(HHIteration step) {
		this.hhIterationStep = step.ordinal();
		this.hhCurrentStepProgress = 0;
	}

	public void hhIterationProgress(double k) {
		// validate 0-100% and disallow to progress back
		if (k >= 0 && k <= 1.0 && k > this.hhCurrentStepProgress) {
			this.hhCurrentStepProgress = k;
		}
	}

	public void hhTargetsProgress(int done, int total) {
		this.hhTargetsDone = done;
		this.hhTargetsTotal = total;
	}
}
