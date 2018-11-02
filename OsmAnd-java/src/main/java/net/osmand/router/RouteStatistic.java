package net.osmand.router;

import java.util.List;
import java.util.Map;

public interface RouteStatistic {

    float getTotalDistance();

    List<RouteStatistics.RouteSegmentAttribute> getElements();

    Map<String, RouteStatistics.RouteSegmentAttribute> getPartition();
}
