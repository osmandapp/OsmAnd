package net.osmand.router;

public class RouteCalculationProgress {

	public int segmentNotFound = -1;
	public float distanceFromBegin;
	public float directDistance;
	public int directSegmentQueueSize;
	public float distanceFromEnd;
	public int reverseSegmentQueueSize;
	public float reverseDistance;
	public float totalEstimatedDistance = 0;
	
	public float routingCalculatedTime = 0;
	
	public int relaxedSegments = 0;
	public int visitedSegments = 0;
	public int visitedDirectSegments = 0;
	public int visitedOppositeSegments = 0;
	public int directQueueSize = 0;
	public int oppositeQueueSize = 0;
	
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
	
	private static final float INITIAL_PROGRESS = 0.05f;
	private static final float FIRST_ITERATION = 0.72f;
	
	public float getLinearProgress() {
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

	public void nextIteration() {
		iteration++;
		totalEstimatedDistance = 0;
	}
}
