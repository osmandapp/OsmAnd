package net.osmand.router.network;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.RouteDataObject;
import net.osmand.router.network.NetworkRouteSelector.NetworkRouteSelectorFilter;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class NetworkRouteContext {
	
	private static final int ZOOM_TO_LOAD_TILES = 15;
	
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
				List<RouteSubregion> subregions = new ArrayList<BinaryMapRouteReaderAdapter.RouteSubregion>();
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

	public Map<RouteKey, List<NetworkRouteSegment>> loadRouteSegmentTile(int x31L, int y31T, int x31R, int y31B, RouteKey rKey)
			throws IOException {
		Map<RouteKey, List<NetworkRouteSegment>> map = new LinkedHashMap<>();
		int left = x31L >> (31 - ZOOM_TO_LOAD_TILES);
		int right = x31R >> (31 - ZOOM_TO_LOAD_TILES);
		int top = y31T >> (31 - ZOOM_TO_LOAD_TILES);
		int bottom = y31B >> (31 - ZOOM_TO_LOAD_TILES);
		for (int x = left; x <= right; x++) {
			for (int y = top; y <= bottom; y++) {
				NetworkRoutesTile osmcRoutesTile = getMapRouteTile(x << (31 - ZOOM_TO_LOAD_TILES),
						y << (31 - ZOOM_TO_LOAD_TILES));
				for (NetworkRoutePoint pnt : osmcRoutesTile.getRoutes().valueCollection()) {
					Iterator<NetworkRouteSegment> segments = pnt.objects.iterator();
					while (segments.hasNext()) {
						NetworkRouteSegment segment = segments.next();
						if (rKey != null && !segment.routeKey.equals(rKey)) {
							continue;
						}
						List<NetworkRouteSegment> lst = map.get(segment.routeKey);
						if (lst == null) {
							lst = new ArrayList<>();
							map.put(segment.routeKey, lst);
						}
						if (segment.start != 0) {
							continue;
						}
						lst.add(segment);
					}
				}
			}
		}
		return map;
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

	private NetworkRoutesTile getMapRouteTile(int x31, int y31) throws IOException {
		long tileId = getTileId(x31, y31);
		NetworkRoutesTile tile = indexedTiles.get(tileId);
		if (tile == null) {
			tile = loadTile(x31, y31, tileId);
			indexedTiles.put(tileId, tile);
		}
		return tile;
	}
	
	public boolean isRouting() {
		return routing;
	}
	

	private NetworkRoutesTile loadTile(int x31, int y31, long tileId) throws IOException {
		stats.loadedTiles++;
		int zm = (31 - ZOOM_TO_LOAD_TILES);
		int tileX = x31 >> zm;
		int tileY = y31 >> zm;
		int tileLeft = tileX << zm;
		int tileTop = tileY << zm;
		int tileRight = (tileX + 1) << zm;
		int tileBottom = (tileY + 1) << zm;
		if (routing) {
			SearchRequest<RouteDataObject> req = BinaryMapIndexReader.buildSearchRouteRequest(tileLeft, tileRight,
					tileTop, tileBottom, null);
			req.log = false;
			return loadRoutingDataTile(req, tileId);
		} else {
			SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(tileLeft, tileRight,
					tileTop, tileBottom, ZOOM_TO_LOAD_TILES, new BinaryMapIndexReader.SearchFilter() {
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
		for (Map.Entry<BinaryMapIndexReader, List<RouteSubregion>> reader : readers.entrySet()) {
			req.clearSearchResults();
			long nt = System.nanoTime();
			List<RouteSubregion> subregions = reader.getKey().searchRouteIndexTree(req, reader.getValue());
			stats.loadTimeNs += (System.nanoTime() - nt);
			for (RouteSubregion sub : subregions) {
				List<RouteDataObject> objects = loadedSubregions.get(sub);
				if (objects == null) {
					nt = System.nanoTime();
					objects = reader.getKey().loadRouteIndexData(sub);
					loadedSubregions.put(sub, objects);
					stats.loadTimeNs += (System.nanoTime() - nt);
				}
				for (RouteDataObject obj : objects) {
					if (obj == null) {
						continue;
					}
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

	private NetworkRoutesTile loadMapDataTile(BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> req, long tileId) throws IOException {
		NetworkRoutesTile osmcRoutesTile = new NetworkRoutesTile(tileId);
		for (BinaryMapIndexReader reader : readers.keySet()) {
			req.clearSearchResults();
			long nt = System.nanoTime();
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
		return osmcRoutesTile;
	}

	public static int getX31FromTileId(long tileId, int shift) {
		int zmShift = (31 - ZOOM_TO_LOAD_TILES);
		int xShift = (int) (tileId >> (ZOOM_TO_LOAD_TILES + 1));
		return (xShift + shift) << zmShift; 
	}
	
	public static int getY31FromTileId(long tileId, int shift) {
		int zmShift = (31 - ZOOM_TO_LOAD_TILES);
		long xShift = tileId >> (ZOOM_TO_LOAD_TILES + 1);
		int yShift = (int) (tileId - (xShift << (ZOOM_TO_LOAD_TILES + 1)));
		return (yShift + shift) << zmShift; 
	}
	
	public static long getTileId(int x31, int y31) {
		int zmShift = (31 - ZOOM_TO_LOAD_TILES);
		return (((long)x31 >> zmShift) << (ZOOM_TO_LOAD_TILES + 1)) + ((long) (y31 >> zmShift));
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
		private final long tileId;
		
		public NetworkRoutesTile(long tileId) {
			this.tileId = tileId;
			
		}

		public void add(BinaryMapDataObject obj, RouteKey rk) {
			int len = obj.getPointsLength();
			for (int i = 0; i < len; i++) {
				int x31 = obj.getPoint31XTile(i);
				int y31 = obj.getPoint31YTile(i);
				if (getTileId(x31, y31) != tileId) {
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
			}
		}

		public void add(RouteDataObject obj, RouteKey rk) {
			int len = obj.getPointsLength();
			for (int i = 0; i < len; i++) {
				int x31 = obj.getPoint31XTile(i);
				int y31 = obj.getPoint31YTile(i);
				if (getTileId(x31, y31) != tileId) {
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
			}
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
