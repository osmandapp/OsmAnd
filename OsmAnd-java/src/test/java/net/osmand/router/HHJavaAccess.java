package net.osmand.router;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.binary.BinaryHHRouteReaderAdapter.HHRouteRegion;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.router.HHRouteDataStructure.HHRouteRegionPointsCtx;
import net.osmand.router.HHRouteDataStructure.HHRoutingContext;
import net.osmand.router.HHRouteDataStructure.NetworkDBPoint;
import net.osmand.router.HHRouteDataStructure.NetworkDBSegment;

/**
 * What the java-versus-copy tests in {@code net.osmand.shared.compat} need of the java HH classes
 * and cannot reach from there: the context's graph fields and an edge's ends are package private.
 * Loads the graph the way {@link HHRoutePlanner#initHCtx} does, without a planner.
 */
public final class HHJavaAccess {

	private HHJavaAccess() {
	}

	/** The vertices of one HH section of one file, ready for their edges to be loaded. */
	public static HHRoutingContext<NetworkDBPoint> load(BinaryMapIndexReader file, HHRouteRegion region, int routingProfile)
			throws IOException, SQLException {
		HHRoutingContext<NetworkDBPoint> hctx = new HHRoutingContext<>();
		hctx.regions.add(new HHRouteRegionPointsCtx<>((short) 0, region, file, routingProfile));
		hctx.pointsById = hctx.loadNetworkPoints(NetworkDBPoint.class);
		hctx.clusterOutPoints = HHRoutePlanner.groupByClusters(hctx.pointsById, true);
		hctx.clusterInPoints = HHRoutePlanner.groupByClusters(hctx.pointsById, false);
		for (NetworkDBPoint pnt : hctx.pointsById.valueCollection()) {
			pnt.markSegmentsNotLoaded();
			hctx.regions.get(pnt.mapId).pntsByFileId.put(pnt.fileId, pnt);
		}
		return hctx;
	}

	public static TLongObjectHashMap<NetworkDBPoint> points(HHRoutingContext<NetworkDBPoint> hctx) {
		return hctx.pointsById;
	}

	/** The search counters of a route, whose fields are package private. */
	public static String stats(HHRouteDataStructure.HHNetworkRouteRes route) {
		HHRouteDataStructure.RoutingStats s = route.stats;
		if (s == null) {
			return "no stats";
		}
		return "visited " + s.visitedVertices + " unique " + s.uniqueVisitedVertices + " added " + s.addedVertices
				+ " firstMet " + s.firstRouteVisitedVertices + " edges " + s.loadEdgesCnt;
	}

	/** The edges of a vertex in one direction as "from>to cost direction shortcut", or null while not loaded. */
	public static List<String> edges(NetworkDBPoint point, boolean reverse) {
		List<NetworkDBSegment> segments = point.connected(reverse);
		if (segments == null) {
			return null;
		}
		List<String> edges = new ArrayList<>(segments.size());
		for (NetworkDBSegment s : segments) {
			edges.add(s.start.index + ">" + s.end.index + " " + s.dist + " " + s.direction + " " + s.shortcut);
		}
		return edges;
	}
}
