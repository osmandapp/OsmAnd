package net.osmand.router.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
	
	public static long convertPontToLong(int x31, int y31) {
		return (((long) x31) << 32l) + y31;
	}
	
	public static int getXFromLong(long l) {
		return (int) (l >> 32);
	}
	
	public static int getYFromLong(long l) {
		return (int) (l - ((l >> 32) << 32));
	}
	
	
	public List<NetworkRoutePoint> loadNearRouteSegment(int x31, int y31, int radius) throws IOException {
		List<NetworkRoutePoint> nearSegments = new ArrayList<>();
		NetworkRoutesTile osmcRoutesTile = getMapRouteTile(x31, y31);
		double sqrRadius = radius * radius;
		for (NetworkRoutePoint segment : osmcRoutesTile.getRoutes().values()) {
			if (MapUtils.squareDist31TileMetric(segment.x31, segment.y31, x31, y31) < sqrRadius) {
				nearSegments.add(segment);
			}
		}
		return nearSegments;
	}
	
	public List<NetworkRouteObject> loadRouteSegment(long pnt) throws IOException {
		return loadRouteSegment(getXFromLong(pnt), getYFromLong(pnt));
	}
	
	public List<NetworkRouteObject> loadRouteSegment(int x31, int y31) throws IOException {
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
			tile = loadTile(x31, y31);
			indexedTiles.put(tileId, tile);
		}
		return tile;
	}
	
	public boolean isRouting() {
		return routing;
	}
	

	private NetworkRoutesTile loadTile(int x31, int y31) throws IOException {
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
			return loadRoutingDataTile(req);
		} else {
			SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(tileLeft, tileRight,
					tileTop, tileBottom, ZOOM_TO_LOAD_TILES, new BinaryMapIndexReader.SearchFilter() {
						@Override
						public boolean accept(TIntArrayList types, BinaryMapIndexReader.MapIndex index) {
							return true;
						}
					}, null);
			return loadMapDataTile(req);
		}
	}
	
	private NetworkRoutesTile loadRoutingDataTile(SearchRequest<RouteDataObject> req ) throws IOException {
		NetworkRoutesTile osmcRoutesTile = new NetworkRoutesTile();
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

	private NetworkRoutesTile loadMapDataTile(BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> req ) throws IOException {
		NetworkRoutesTile osmcRoutesTile = new NetworkRoutesTile();
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

	
	
	private long getTileId(int x31, int y31) {
		int zmShift = (31 - ZOOM_TO_LOAD_TILES);
		return (long) (x31 >> zmShift) << (ZOOM_TO_LOAD_TILES + 1) + ((long) (y31 >> zmShift));
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
		public final List<NetworkRouteObject> objects = new ArrayList<>();
		
		public NetworkRoutePoint(int x31, int y31, long id) {
			this.x31 = x31;
			this.y31 = y31;
			this.id = id;
		}

		public void addObject(NetworkRouteObject obj) {
			if (obj.getId() > 0) {
				for (NetworkRouteObject obj2 : objects) {
					if (obj.getId() == obj2.getId()) {
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
	
	
	public static class NetworkRouteObject {
		public int start;
		public int end;
		public BinaryMapDataObject obj;
		public RouteDataObject robj;
		public RouteKey routeKey;
		
		public NetworkRouteObject(BinaryMapDataObject obj, RouteKey routeKey, int start, int end) {
			this.obj = obj;
			this.end = end;
			this.start = start;
			this.routeKey = routeKey;
		}
		
		public NetworkRouteObject(RouteDataObject obj, RouteKey routeKey, int start, int end) {
			this.robj = obj;
			this.end = end;
			this.start = start;
			this.routeKey = routeKey;
		}
		
		public long getId() {
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

		public long getEndPointLong() {
			return convertPontToLong(getPoint31XTile(end), getPoint31YTile(end));
		}
		
		public long getStartPointLong() {
			return convertPontToLong(getPoint31XTile(start), getPoint31YTile(start));
		}

		public void inverse() {
			int t = start;
			start = end;
			end = t;
		}

		@Override
		public String toString() {
			return "NetworkRouteObject [start=" + start + ", end=" + end + ", obj=" + (robj != null ? robj : obj)
					+ ", routeKey=" + routeKey + "]";
		}
		
		
	}

	private static class NetworkRoutesTile {
		private final TLongObjectMap<NetworkRoutePoint> routes = new TLongObjectHashMap<>();

		public void add(BinaryMapDataObject obj, RouteKey rk) {
			int len = obj.getPointsLength();
			for (int i = 0; i < len; i++) {
				int x31 = obj.getPoint31XTile(i);
				int y31 = obj.getPoint31YTile(i);
				long id = convertPontToLong(x31, y31);
				NetworkRoutePoint point = routes.get(id);
				if (point == null) {
					point = new NetworkRoutePoint(x31, y31, id);
					routes.put(id, point);
				}
				if (i > 0) {
					point.addObject(new NetworkRouteObject(obj, rk, i, 0));
				}
				if (i < len - 1) {
					point.addObject(new NetworkRouteObject(obj, rk, i, len - 1));
				}
			}			
		}
		
		
		public void add(RouteDataObject obj, RouteKey rk) {
			int len = obj.getPointsLength();
			for (int i = 0; i < len; i++) {
				int x31 = obj.getPoint31XTile(i);
				int y31 = obj.getPoint31YTile(i);
				long id = convertPontToLong(x31, y31);
				NetworkRoutePoint point = routes.get(id);
				if (point == null) {
					point = new NetworkRoutePoint(x31, y31, id);
					routes.put(id, point);
				}
				if (i > 0) {
					point.addObject(new NetworkRouteObject(obj, rk, i, 0));
				}
				if (i < len - 1) {
					point.addObject(new NetworkRouteObject(obj, rk, i, len - 1));
				}
			}			
		}

		public TLongObjectMap<NetworkRoutePoint> getRoutes() {
			return routes;
		}

		public NetworkRoutePoint getRouteSegment(int x31, int y31) {
			return routes.get(convertPontToLong(x31, y31));
		}

	}

}
