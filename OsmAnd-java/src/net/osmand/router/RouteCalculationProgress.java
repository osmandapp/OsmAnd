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
	
	public boolean isCancelled;
	
}
