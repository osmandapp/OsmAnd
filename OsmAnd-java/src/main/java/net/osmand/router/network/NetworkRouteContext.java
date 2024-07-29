package net.osmand.router.network;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.RouteDataObject;
import net.osmand.router.network.NetworkRouteSelector.NetworkRouteSelectorFilter;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class NetworkRouteContext {
	
	public static final int ZOOM_TO_LOAD_TILES = 15;
	public static final int ZOOM_TO_LOAD_TILES_SHIFT_L = ZOOM_TO_LOAD_TILES + 1;
	public static final int ZOOM_TO_LOAD_TILES_SHIFT_R = 31 - ZOOM_TO_LOAD_TILES;

	private final TLongObjectHashMap<NetworkRoutesTile> indexedTiles = new TLongObjectHashMap<>();
	private final NetworkRouteSelectorFilter filter;
	private final Map<BinaryMapIndexReader, List<RouteSubregion>> readers = new LinkedHashMap<>();
	private final Map<RouteSubregion, List<RouteDataObject>> loadedSubregions = new HashMap<>();
	private final boolean routing;
	private NetworkRouteContextStats stats;

	public NetworkRouteContext(BinaryMapIndexReader[] readers, NetworkRouteSelectorFilter filter, boolean routing) {
		this.filter = filter;
		this.routing = routing;
		this.stats = new NetworkRouteContextStats();
		for(BinaryMapIndexReader r : readers) {
			if (!routing) {
				this.readers.put(r, null);
			} else {
				List<RouteSubregion> subregions = new ArrayList<>();
				for (RouteRegion rInd : r.getRoutingIndexes()) {
					List<RouteSubregion> subregs = rInd.getSubregions();
					// create copy to avoid leaks to original structure
					for (RouteSubregion rs : subregs) {
						subregions.add(new RouteSubregion(rs));
					}
				}
				this.readers.put(r, subregions);
			}
		}
	}

	public static long convertPointToLong(int x31, int y31) {
		return (((long) x31) << 32) + y31;
	}

	public static int getXFromLong(long l) {
		return (int) (l >> 32);
	}

	public static int getYFromLong(long l) {
		return (int) (l - ((l >> 32) << 32));
	}

	Map<RouteKey, List<NetworkRouteSegment>> loadRouteSegmentsBbox(int x31L, int y31T, int x31R, int y31B, RouteKey rKey)
			throws IOException {
		Map<RouteKey, List<NetworkRouteSegment>> map = new LinkedHashMap<>();
		int left = x31L >> ZOOM_TO_LOAD_TILES_SHIFT_R;
		int right = x31R >> ZOOM_TO_LOAD_TILES_SHIFT_R;
		int top = y31T >> ZOOM_TO_LOAD_TILES_SHIFT_R;
		int bottom = y31B >> ZOOM_TO_LOAD_TILES_SHIFT_R;
		for (int x = left; x <= right; x++) {
			for (int y = top; y <= bottom; y++) {
				loadRouteSegmentIntersectingTile(x, y, rKey, map);
			}
		}
		return map;
	}

	Map<RouteKey, List<NetworkRouteSegment>> loadRouteSegmentIntersectingTile(int x, int y, RouteKey routeKey,
			Map<RouteKey, List<NetworkRouteSegment>> map) throws IOException {
		NetworkRoutesTile osmcRoutesTile = getMapRouteTile(x << ZOOM_TO_LOAD_TILES_SHIFT_L, y << ZOOM_TO_LOAD_TILES_SHIFT_L);
		for (NetworkRouteSegment segment : osmcRoutesTile.uniqueSegments.values()) {
			if (loadOnlyRouteWithKey(routeKey) && !segment.routeKey.equals(routeKey)) {
				continue;
			}
			List<NetworkRouteSegment> routeSegments = map.get(segment.routeKey);
			if (routeSegments == null) {
				routeSegments = new ArrayList<>();
				map.put(segment.routeKey, routeSegments);
			}
			routeSegments.add(segment);
		}
		return map;
	}

	boolean loadOnlyRouteWithKey(RouteKey rKey) {
		return rKey != null;
	}

	public List<NetworkRouteSegment> loadNearRouteSegment(int x31, int y31, double radius) throws IOException {
		List<NetworkRoutePoint> nearPoints = new ArrayList<>();
		NetworkRoutesTile osmcRoutesTile = getMapRouteTile(x31, y31);
		double sqrRadius = radius * radius;
		for (NetworkRoutePoint pnt : osmcRoutesTile.getRoutes().valueCollection()) {
			double dist = MapUtils.squareDist31TileMetric(pnt.x31, pnt.y31, x31, y31);
			if (dist < sqrRadius) {
				// use as single thread cache
				pnt.localVar = dist;
				nearPoints.add(pnt);
			}
		}
		Collections.sort(nearPoints, new Comparator<NetworkRoutePoint>() {
			@Override
			public int compare(NetworkRoutePoint o1, NetworkRoutePoint o2) {
				return Double.compare(o1.localVar, o2.localVar);
			}
		});
		if (nearPoints.size() == 0) {
			return Collections.emptyList();
		}
		List<NetworkRouteSegment> objs = new ArrayList<>();
		for (NetworkRoutePoint pnt : nearPoints) {
			objs.addAll(pnt.objects);
		}
		return objs;
	}

	public List<NetworkRouteSegment> loadRouteSegment(int x31, int y31) throws IOException {
		NetworkRoutesTile osmcRoutesTile = getMapRouteTile(x31, y31);
		NetworkRoutePoint point = osmcRoutesTile.getRouteSegment(x31, y31);
		if (point == null) {
			return Collections.emptyList();
		}
		return point.objects;
	}

	public NetworkRoutePoint getClosestNetworkRoutePoint(int sx31, int sy31) throws IOException {
		NetworkRoutesTile osmcRoutesTile = getMapRouteTile(sx31, sy31);
		double minDistance = Double.MAX_VALUE;
		NetworkRoutePoint nearPoint = null;
		for (NetworkRoutePoint pt : osmcRoutesTile.routes.valueCollection()) {
			double distance = MapUtils.squareRootDist31(sx31, sy31, pt.x31, pt.y31);
			if (distance < minDistance) {
				nearPoint = pt;
				minDistance = distance;
			}
		}
		return nearPoint;
	}

	private NetworkRoutesTile getMapRouteTile(int x31, int y31) throws IOException {
		long tileId = getTileId(x31, y31);
		NetworkRoutesTile tile = indexedTiles.get(tileId);
		if (tile == null) {
			tile = loadTile(x31 >> ZOOM_TO_LOAD_TILES_SHIFT_R, y31 >> ZOOM_TO_LOAD_TILES_SHIFT_R, tileId);
			indexedTiles.put(tileId, tile);
		}
		return tile;
	}

	public boolean isRouting() {
		return routing;
	}

	private NetworkRoutesTile loadTile(int x, int y, long tileId) throws IOException {
		stats.loadedTiles++;
		if (routing) {
			SearchRequest<RouteDataObject> req = BinaryMapIndexReader.buildSearchRouteRequest(
					x << ZOOM_TO_LOAD_TILES_SHIFT_L, (x + 1) << ZOOM_TO_LOAD_TILES_SHIFT_L,
					y << ZOOM_TO_LOAD_TILES_SHIFT_L, (y + 1) << ZOOM_TO_LOAD_TILES_SHIFT_L, null);
			req.log = false;
			return loadRoutingDataTile(req, tileId);
		} else {
			SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(
					x << ZOOM_TO_LOAD_TILES_SHIFT_L, (x + 1) << ZOOM_TO_LOAD_TILES_SHIFT_L,
					y << ZOOM_TO_LOAD_TILES_SHIFT_L, (y + 1) << ZOOM_TO_LOAD_TILES_SHIFT_L, ZOOM_TO_LOAD_TILES,
					new BinaryMapIndexReader.SearchFilter() {
						@Override
						public boolean accept(TIntArrayList types, BinaryMapIndexReader.MapIndex index) {
							return true;
						}
					}, null);
			req.log = false;
			return loadMapDataTile(req, tileId);
		}
	}

	private NetworkRoutesTile loadRoutingDataTile(SearchRequest<RouteDataObject> req, long tileId) throws IOException {
		NetworkRoutesTile osmcRoutesTile = new NetworkRoutesTile(tileId);
		HashSet<Long> deletedIds = new HashSet<>();
		Map<Long, RouteRegion> usedIds = new HashMap<>();
		for (Map.Entry<BinaryMapIndexReader, List<RouteSubregion>> readerSubregions : readers.entrySet()) {
			req.clearSearchResults();
			long nt = System.nanoTime();
			BinaryMapIndexReader reader = readerSubregions.getKey();
			synchronized (reader) {
				List<RouteSubregion> routeSubregions = readerSubregions.getValue();
				List<RouteSubregion> subregions = reader.searchRouteIndexTree(req, routeSubregions);
				stats.loadTimeNs += (System.nanoTime() - nt);
				for (RouteSubregion sub : subregions) {
					List<RouteDataObject> objects = loadedSubregions.get(sub);
					if (objects == null) {
						nt = System.nanoTime();
						objects = reader.loadRouteIndexData(sub);
						loadedSubregions.put(sub, objects);
						stats.loadTimeNs += (System.nanoTime() - nt);
					}
					for (RouteDataObject obj : objects) {
						if (obj == null) {
							continue;
						}
						if (deletedIds.contains(obj.id)) {
							// live-updates, osmand_change=delete
							continue;
						}
						if (obj.isRoadDeleted()) {
							deletedIds.add(obj.id);
							continue;
						}
						if (usedIds.containsKey(obj.id) && usedIds.get(obj.id) != obj.region) {
							// live-update, changed tags
							continue;
						}
						stats.loadedObjects++;
						List<RouteKey> keys = filter.convert(obj);
						for (RouteKey rk : keys) {
							stats.loadedRoutes++;
							osmcRoutesTile.add(obj, rk);
						}
						usedIds.put(obj.id, obj.region);
					}
				}
			}
		}
		return osmcRoutesTile;
	}

	private NetworkRoutesTile loadMapDataTile(BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> req, long tileId) throws IOException {
		NetworkRoutesTile osmcRoutesTile = new NetworkRoutesTile(tileId);
		for (BinaryMapIndexReader reader : readers.keySet()) {
			req.clearSearchResults();
			long nt = System.nanoTime();
			synchronized (reader) {
				List<BinaryMapDataObject> objects = reader.searchMapIndex(req);
				stats.loadTimeNs += (System.nanoTime() - nt);
				for (BinaryMapDataObject obj : objects) {
					stats.loadedObjects++;
					List<RouteKey> keys = filter.convert(obj);
					for (RouteKey rk : keys) {
						stats.loadedRoutes++;
						osmcRoutesTile.add(obj, rk);
					}
				}
			}
		}
		return osmcRoutesTile;
	}

	public static int getXFromTileId(long tileId) {
		return (int) (tileId >> ZOOM_TO_LOAD_TILES_SHIFT_R);
	}

	public static int getYFromTileId(long tileId) {
		long xShifted = tileId >> ZOOM_TO_LOAD_TILES_SHIFT_R;
		return (int) (tileId - (xShifted << ZOOM_TO_LOAD_TILES_SHIFT_L));
	}

	public static long getTileId(int x31, int y31) {
		return getTileId(x31, y31, ZOOM_TO_LOAD_TILES_SHIFT_R);
	}

	public static long getTileId(int x, int y, int shiftR) {
		return (((long) x >> shiftR) << ZOOM_TO_LOAD_TILES_SHIFT_L) + (long) (y >> shiftR);
	}

	public NetworkRouteContextStats getStats() {
		return stats;
	}

	public void clearData() {
		indexedTiles.clear();
		loadedSubregions.clear();
		stats = new NetworkRouteContextStats();
	}

	public void clearStats() {
		stats = new NetworkRouteContextStats();
	}

	public static class NetworkRoutePoint {
		public final int x31;
		public final int y31;
		public final long id;
		public final List<NetworkRouteSegment> objects = new ArrayList<>();
		public double localVar;

		public NetworkRoutePoint(int x31, int y31, long id) {
			this.x31 = x31;
			this.y31 = y31;
			this.id = id;
		}

		public void addObject(NetworkRouteSegment obj) {
			if (obj.getId() > 0) {
				for (NetworkRouteSegment obj2 : objects) {
					if (obj.getId() == obj2.getId() && obj.direction() == obj2.direction()
							&& Algorithms.objectEquals(obj.routeKey, obj2.routeKey)) {
						return;
					}
				}
			}
			objects.add(obj);
		}
	}

	public static class NetworkRouteContextStats {
		
		public int loadedTiles;
		public int loadedObjects;
		public int loadedRoutes;
		public long loadTimeNs;
	}

	public static class NetworkRouteSegment {
		public final int start;
		public final int end;
		public final BinaryMapDataObject obj;
		public final RouteDataObject robj;
		public final RouteKey routeKey;

		public NetworkRouteSegment(BinaryMapDataObject obj, RouteKey routeKey, int start, int end) {
			this.robj = null;
			this.obj = obj;
			this.start = start;
			this.end = end;
			this.routeKey = routeKey;
		}

		public NetworkRouteSegment(NetworkRouteSegment segment, int start, int end) {
			this.robj = segment.robj;
			this.obj = segment.obj;
			this.start = start;
			this.end = end;
			this.routeKey = segment.routeKey;
		}

		public NetworkRouteSegment(RouteDataObject obj, RouteKey routeKey, int start, int end) {
			this.robj = obj;
			this.obj = null;
			this.start = start;
			this.end = end;
			this.routeKey = routeKey;
		}

		public boolean direction() {
			return end > start;
		}

		public long getId() {
			if (robj != null) {
				return robj.getId();
			}
			return obj.getId();
		}

		public int getPointsLength() {
			if (robj != null) {
				return robj.getPointsLength();
			}
			return obj.getPointsLength();
		}

		public int getPoint31XTile(int i) {
			if (robj != null) {
				return robj.getPoint31XTile(i);
			}
			return obj.getPoint31XTile(i);
		}

		public int getPoint31YTile(int i) {
			if (robj != null) {
				return robj.getPoint31YTile(i);
			}
			return obj.getPoint31YTile(i);
		}

		public String getRouteName() {
			String name = routeKey.getValue("name");
			if (name.isEmpty()) {
				name = routeKey.getValue("ref");
			}
			if (!name.isEmpty()) {
				return name;
			}
			if (robj != null) {
				return robj.getName();
			}
			return obj.getName();
		}

		public int getStartPointX() {
			return getPoint31XTile(start);
		}

		public int getStartPointY() {
			return getPoint31YTile(start);
		}

		public int getEndPointX() {
			return getPoint31XTile(end);
		}
		
		public int getEndPointY() {
			return getPoint31YTile(end);
		}

		@Override
		public String toString() {
			return "NetworkRouteObject [start=" + start + ", end=" + end + ", obj=" + (robj != null ? robj : obj)
					+ ", routeKey=" + routeKey + "]";
		}

		public NetworkRouteSegment inverse() {
			return new NetworkRouteSegment(this, end, start);
		}
	}

	private static class NetworkRoutesTile {
		private final TLongObjectMap<NetworkRoutePoint> routes = new TLongObjectHashMap<>();
		private final Map<String, NetworkRouteSegment> uniqueSegments = new HashMap<>();
		private final long tileId;

		public NetworkRoutesTile(long tileId) {
			this.tileId = tileId;

		}

		public void add(BinaryMapDataObject obj, RouteKey rk) {
			int len = obj.getPointsLength();
			boolean intersects = false; 
			int px = 0, py = 0;
			for (int i = 0; i < len; i++) {
				int x31 = obj.getPoint31XTile(i);
				int y31 = obj.getPoint31YTile(i);
				intersects = intersects || (i > 0 && intersects(x31, y31, px, py));
				if (getTileId(x31, y31) != tileId) {
					px = x31;
					py = y31;
					continue;
				}
				intersects = true;
				long id = convertPointToLong(x31, y31);
				NetworkRoutePoint point = routes.get(id);
				if (point == null) {
					point = new NetworkRoutePoint(x31, y31, id);
					routes.put(id, point);
				}
				if (i > 0) {
					point.addObject(new NetworkRouteSegment(obj, rk, i, 0));
				}
				if (i < len - 1) {
					point.addObject(new NetworkRouteSegment(obj, rk, i, len - 1));
				}
			}
			if (intersects) {
				addUnique(new NetworkRouteSegment(obj, rk, 0, len - 1));
			}
		}

		private void addUnique(NetworkRouteSegment networkRouteSegment) {
			uniqueSegments.put(networkRouteSegment.routeKey + "_" + networkRouteSegment.getId(), networkRouteSegment);
		}

		public void add(RouteDataObject obj, RouteKey rk) {
			int len = obj.getPointsLength();
			boolean intersects = false; 
			int px = 0, py = 0;
			for (int i = 0; i < len; i++) {
				int x31 = obj.getPoint31XTile(i);
				int y31 = obj.getPoint31YTile(i);
				intersects = intersects || (i > 0 && intersects(x31, y31, px, py));
				if (getTileId(x31, y31) != tileId) {
					px = x31;
					py = y31;
					continue;
				}
				long id = convertPointToLong(x31, y31);
				NetworkRoutePoint point = routes.get(id);
				if (point == null) {
					point = new NetworkRoutePoint(x31, y31, id);
					routes.put(id, point);
				}
				if (i > 0) {
					point.addObject(new NetworkRouteSegment(obj, rk, i, 0));
				}
				if (i < len - 1) {
					point.addObject(new NetworkRouteSegment(obj, rk, i, len - 1));
				}
				px = x31;
				py = y31;
			}
			if (intersects) {
				addUnique(new NetworkRouteSegment(obj, rk, 0, len - 1));
			}
		}

		// this method should be fast enough and without multiplications 
		// cause tiles are big enough and situation will be rare for long lines
		private boolean intersects(int x31, int y31, int px, int py) {
			long currentTile = getTileId(x31, y31);
			long previousTile = getTileId(px, py);
			if (currentTile == tileId || previousTile == tileId) {
				return true;
			}
			if (currentTile == previousTile) {
				return false;
			}
			int xprevTile = getXFromTileId(previousTile);
			int yprevTile = getYFromTileId(previousTile);
			int xcurrTile = getXFromTileId(currentTile);
			int ycurrTile = getYFromTileId(currentTile);
			if ((ycurrTile == yprevTile && Math.abs(xcurrTile - xprevTile) <= 1)
					|| (xcurrTile == xprevTile && Math.abs(ycurrTile - yprevTile) <= 1)) {
				// speed up for neighbor tiles that couldn't intersect tileId
				return false;
			}
			
			if (Math.abs(x31 - px) <= 2 && Math.abs(y31 - py) <= 2) {
				// return when points too close to avoid rounding int errors
				return false;
			}
			// use recursive method to quickly find intersection
			if (intersects(x31, y31, x31 / 2 + px / 2, y31 / 2 + py / 2)) {
				return true;
			}
			if (intersects(px, py, x31 / 2 + px / 2, y31 / 2 + py / 2)) {
				return true;
			}
			return false;
		}

		public TLongObjectMap<NetworkRoutePoint> getRoutes() {
			return routes;
		}

		public NetworkRoutePoint getRouteSegment(int x31, int y31) {
			if (getTileId(x31, y31) != tileId) {
				System.err.println(String.format("Wrong tile id !!! %d != %d", getTileId(x31, y31), tileId));
			}
			return routes.get(convertPointToLong(x31, y31));
		}

	}

}
