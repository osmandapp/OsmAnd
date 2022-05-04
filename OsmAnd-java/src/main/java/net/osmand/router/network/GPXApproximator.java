package net.osmand.router.network;

import net.osmand.data.LatLon;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.GPXUtilities.*;
import static net.osmand.router.network.NetworkRouteContext.*;
import static net.osmand.router.network.NetworkRouteSelector.*;

public class GPXApproximator {

	NetworkRouteSelector routeSelector;

	public GPXApproximator(NetworkRouteSelector routeSelector) {
		this.routeSelector = routeSelector;
		routeSelector.getNetworkRouteContext().getFilter().useFilter = false;
	}

	public List<Entity> approximate(GPXFile gpxFile) throws IOException {
		Map<RouteKey, GPXFile> res = new HashMap<>();
		List<NetworkRouteSegmentChain> lst = routeSelector.connectAlgorithmByGPX(gpxFile, res);
		return convertToEntities(lst);
	}

	private List<Entity> convertToEntities(List<NetworkRouteSegmentChain> res) {
		List<Entity> entityList = new ArrayList<>();
		for (NetworkRouteSegmentChain chain : res) {
			for (NetworkRouteSegment segment : chain.connected) {
				boolean plus = segment.start < segment.end;
				int ind = segment.start;
				Way way = new Way(-1);
				while (true) {
					if (segment.robj != null) {
						LatLon l = new LatLon(MapUtils.get31LatitudeY(segment.robj.getPoint31YTile(ind)),
								MapUtils.get31LongitudeX(segment.robj.getPoint31XTile(ind)));
						Node n = new Node(l.getLatitude(), l.getLongitude(), -1);
						way.addNode(n);
					}
					if (ind == segment.end) {
						break;
					}
					ind += plus ? 1 : -1;
				}
				if (way.getNodes().size() > 0) {
					entityList.add(way);
				}
			}
		}
		return entityList;
	}
}
