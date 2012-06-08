package net.osmand.router;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteDataObject;
import net.osmand.osm.LatLon;


public class RouteSegmentResult {
	public LatLon startPoint;
	public LatLon endPoint;
	public RouteDataObject object;
	public int startPointIndex;
	public int endPointIndex;
}