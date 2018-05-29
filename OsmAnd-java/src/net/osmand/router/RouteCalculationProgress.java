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
	public int loadedTiles = 0;
	public int visitedSegments = 0;
	
	public int totalIterations = 1;
	public int iteration = -1;
	
	public boolean isCancelled;
	public boolean requestPrivateAccessRouting;
	
	private static final float FIRST_ITERATION = 0.75f;
	public float getLinearProgress() {
		float p = Math.max(distanceFromBegin, distanceFromEnd);
		float all = totalEstimatedDistance * 1.25f;
		float pr = 0; 
		if (all > 0) {
			pr = Math.min(p * p / (all * all) * 100f, 99);
		}
		if(totalIterations > 1) {
			if(iteration <= 0) {
				return pr * FIRST_ITERATION;
			} else {
				return Math.min(FIRST_ITERATION * 100 + pr * (1 - FIRST_ITERATION), 99);
			}
		}
		return pr;
	}
}
