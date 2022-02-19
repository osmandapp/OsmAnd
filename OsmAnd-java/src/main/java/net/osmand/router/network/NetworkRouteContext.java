package net.osmand.router.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.router.network.NetworkRouteSelector.NetworkRouteSelectorFilter;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.util.MapUtils;

public class NetworkRouteContext {
	
	private static final int ZOOM_TO_LOAD_TILES = 15;
	
	private TLongObjectHashMap<NetworkRoutesTile> indexedTiles = new TLongObjectHashMap<>();
	private final BinaryMapIndexReader[] readers;
	final NetworkRouteSelectorFilter filter;

	public NetworkRouteContext(BinaryMapIndexReader[] readers, NetworkRouteSelectorFilter filter) {
		this.readers = readers;
		this.filter = filter;
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

	private NetworkRoutesTile loadTile(int x31, int y31) throws IOException {
		// TODO load routing tiles
		final BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> req = buildTileRequest(x31, y31);
		NetworkRoutesTile osmcRoutesTile = new NetworkRoutesTile();
		for (BinaryMapIndexReader reader : readers) {
			req.clearSearchResults();
			List<BinaryMapDataObject> objects = reader.searchMapIndex(req);
			for (BinaryMapDataObject obj : objects) {
				List<RouteKey> keys = filter.convert(obj);
				for(RouteKey rk : keys) {
					osmcRoutesTile.add(new NetworkRouteObject(obj, rk));
				}
			}
		}
		return osmcRoutesTile;
	}

	private BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> buildTileRequest(int x31, int y31) {
		int zm = (31 - ZOOM_TO_LOAD_TILES);
		int tileX = x31 >> zm;
		int tileY = y31 >> zm;
		int tileLeft = tileX << zm;
		int tileTop = tileY << zm;
		int tileRight = (tileX + 1) << zm;
		int tileBottom = (tileY + 1) << zm;
		return BinaryMapIndexReader.buildSearchRequest(tileLeft, tileRight, tileTop, tileBottom, ZOOM_TO_LOAD_TILES,
				new BinaryMapIndexReader.SearchFilter() {
					@Override
					public boolean accept(TIntArrayList types, BinaryMapIndexReader.MapIndex index) {
						return true;
					}
				}, new ResultMatcher<BinaryMapDataObject>() {
					@Override
					public boolean publish(BinaryMapDataObject object) {
						return true;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				});
	}
	
	
	private long getTileId(int x31, int y31) {
		int zmShift = (31 - ZOOM_TO_LOAD_TILES);
		return (long) (x31 >> zmShift) << (ZOOM_TO_LOAD_TILES + 1) + ((long) (y31 >> zmShift));
	}
	
	
	public class NetworkRoutePoint {
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
	
	public static class NetworkRouteObject {
		public int start;
		public int end;
		public BinaryMapDataObject obj;
		public RouteKey routeKey;
		
		public NetworkRouteObject next;
		public NetworkRouteObject prev;
		
		public NetworkRouteObject(BinaryMapDataObject obj, RouteKey routeKey) {
			this.obj = obj;
			this.end = obj.getPointsLength() - 1;
			this.start = 0;
			this.routeKey = routeKey;
		}
		
		public long getId() {
			return obj.getId();
		}

		public int getPointsLength() {
			return obj.getPointsLength();
		}

		public int getPoint31XTile(int i) {
			return obj.getPoint31XTile(i);
		}

		public int getPoint31YTile(int i) {
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
	}

	private class NetworkRoutesTile {
		private final TLongObjectMap<NetworkRoutePoint> routes = new TLongObjectHashMap<>();

		public void add(NetworkRouteObject obj) {
			for (int i = 0; i < obj.getPointsLength(); i++) {
				int x31 = obj.getPoint31XTile(i);
				int y31 = obj.getPoint31YTile(i);
				long id = convertPontToLong(x31, y31);
				NetworkRoutePoint point = routes.get(id);
				if (point == null) {
					point = new NetworkRoutePoint(x31, y31, id);
					routes.put(id, point);
				}
				point.addObject(obj);
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
