package net.osmand.router.transport;

import net.osmand.data.LatLon;
import net.osmand.router.RouteCalculationProgress;

import java.io.IOException;
import java.util.List;

import gnu.trove.map.hash.TLongObjectHashMap;

public interface ITransportRoutingContext {
	List<TransportRouteSegment> getTransportStops(LatLon loc) throws IOException;
	List<TransportRouteSegment> getTransportStops(int x, int y, boolean change, List<TransportRouteSegment> res) throws IOException;
	ITransportRoutingConfiguration getCfg();
	RouteCalculationProgress getCalculationProgress();
	TLongObjectHashMap<TransportRouteSegment> getVisitedSegments();
	void setFinishTimeSeconds(int finishTimeSeconds);
	int getFinishTimeSeconds();
	void setStartCalcTime(long startCalcTime);
	long getStartCalcTime();
	void setVisitedRoutesCount(int visitedRoutesCount);
	int getVisitedRoutesCount();
	void setVisitedStops(int visitedStops);
	int getVisitedStops();
	int getQuadTreeSize();
	long getReadTime();
	long getLoadTime();
	int getLoadedWays();
	int getWrongLoadedWays();
}
