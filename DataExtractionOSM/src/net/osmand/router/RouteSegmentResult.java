package net.osmand.router;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.osm.LatLon;


public class RouteSegmentResult {
	public LatLon startPoint;
	public LatLon endPoint;
	public BinaryMapDataObject object;
	public int startPointIndex;
	public int endPointIndex;
}