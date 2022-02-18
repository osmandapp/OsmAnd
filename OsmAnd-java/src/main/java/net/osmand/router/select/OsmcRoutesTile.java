package net.osmand.router.select;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.binary.BinaryMapDataObject;

class OsmcRoutesTile {
	private final TLongObjectMap<OsmcRouteSegment> routes = new TLongObjectHashMap<>();

	public void add(BinaryMapDataObject bMdo) {
		for (int i = 0; i < bMdo.getPointsLength(); i++) {
			int x31 = bMdo.getPoint31XTile(i);
			int y31 = bMdo.getPoint31YTile(i);
			long id = getPointId(x31, y31);
			OsmcRouteSegment segment = new OsmcRouteSegment(bMdo, x31, y31);
			if (!routes.containsKey(id)) {
				routes.put(id, segment);
			} else {
				OsmcRouteSegment routeSegment = routes.get(id);
				routeSegment.addObject(bMdo);
			}
		}
	}

	public TLongObjectMap<OsmcRouteSegment> getRoutes() {
		return routes;
	}

	public OsmcRouteSegment getRouteSegment(int x31, int y31) {
		return routes.get(getPointId(x31, y31));
	}

	private long getPointId(long x31, long y31) {
		return (x31 << 31) + y31;
	}
}
