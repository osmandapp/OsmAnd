package net.osmand.router.network;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.util.MapUtils;

import static net.osmand.router.network.NetworkRouteSelector.*;

import java.io.IOException;
import java.util.*;

public class NetworkRouteContext {
	private static final int ZOOM_TO_LOAD_TILES = 16;
	
	TLongObjectHashMap<NetworkRoutesTile> indexedSubregions = new TLongObjectHashMap<>();
	private BinaryMapIndexReader reader;

	List<NetworkRouteSegment> loadNearRouteSegment(int x31, int y31, int radius) throws IOException {
		List<NetworkRouteSegment> nearSegments = new ArrayList<>();
		NetworkRoutesTile osmcRoutesTile = getMapRouteTile(x31, y31);
		double sqrRadius = radius * radius;
		for (NetworkRouteSegment segment : osmcRoutesTile.getRoutes().values()) {
			if (MapUtils.squareDist31TileMetric(segment.x31, segment.y31, x31, y31) < sqrRadius) {
				nearSegments.add(segment);
			}
		}
		return nearSegments;
	}

	NetworkRouteSegment loadRouteSegment(int x31, int y31) throws IOException {
		NetworkRoutesTile osmcRoutesTile = getMapRouteTile(x31, y31);
		return osmcRoutesTile.getRouteSegment(x31, y31);
	}

	NetworkRoutesTile getMapRouteTile(int x31, int y31) throws IOException {
		long tileId = getTileId(x31, y31);
		if (!indexedSubregions.containsKey(tileId)) {
			NetworkRoutesTile osmcRoutesTile = loadTile(x31, y31);
			indexedSubregions.put(tileId, osmcRoutesTile);
		}
		return indexedSubregions.get(tileId);
	}

	private NetworkRoutesTile loadTile(int x31, int y31) throws IOException {
		final BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> req = buildTileRequest(x31, y31);
		NetworkRoutesTile osmcRoutesTile = new NetworkRoutesTile();
		List<BinaryMapDataObject> objects = reader.searchMapIndex(req);
		if (!objects.isEmpty()) {
			for (BinaryMapDataObject bMdo : objects) {
				osmcRoutesTile.add(bMdo);
			}
		}
		return osmcRoutesTile;
	}

	public void setReader(BinaryMapIndexReader reader) {
		this.reader = reader;
	}

	private BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> buildTileRequest(int x, int y) {
		int tileX = x >> ZOOM;
		int tileY = y >> ZOOM;
		int tileLeft = tileX << ZOOM;
		int tileTop = tileY << ZOOM;
		int tileRight = (tileX + 1) << ZOOM;
		int tileBottom = (tileY + 1) << ZOOM;
		return BinaryMapIndexReader.buildSearchRequest(tileLeft, tileRight,
				tileTop, tileBottom,
				ZOOM, new BinaryMapIndexReader.SearchFilter() {
					@Override
					public boolean accept(TIntArrayList types, BinaryMapIndexReader.MapIndex index) {
						return true;
					}
				},
				new ResultMatcher<BinaryMapDataObject>() {
					@Override
					public boolean publish(BinaryMapDataObject object) {
						boolean publish = false;
						for (RouteType routeType : RouteType.values()) {
							String prefix = ROUTE_PREFIX + routeType.getType() + "_";
							for (int i = 0; i < object.getObjectNames().keys().length; i++) {
								BinaryMapIndexReader.TagValuePair tp = object.getMapIndex()
										.decodeType(object.getObjectNames().keys()[i]);
								if (tp != null && tp.tag != null && (tp.tag).startsWith(prefix)) {
									publish = true;
								}
							}
							int[] allTypes = Arrays.copyOf(object.getTypes(), object.getTypes().length
									+ object.getAdditionalTypes().length);
							System.arraycopy(object.getAdditionalTypes(), 0, allTypes, object.getTypes().length,
									object.getAdditionalTypes().length);
							for (int allType : allTypes) {
								BinaryMapIndexReader.TagValuePair tp = object.getMapIndex().decodeType(allType);
								if (tp != null && tp.tag != null && (tp.tag).startsWith(prefix)) {
									publish = true;
								}
							}
						}
						return publish;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				});
	}

	private long getTileId(int x31, int y31) {
		int zmShift = 31 - ZOOM_TO_LOAD_TILES;
		return (long) (x31 >> zmShift) << ZOOM_TO_LOAD_TILES + ((long) (y31 >> zmShift));
	}
	
	class NetworkRoutesTile {
		private final TLongObjectMap<NetworkRouteSegment> routes = new TLongObjectHashMap<>();

		public void add(BinaryMapDataObject bMdo) {
			for (int i = 0; i < bMdo.getPointsLength(); i++) {
				int x31 = bMdo.getPoint31XTile(i);
				int y31 = bMdo.getPoint31YTile(i);
				long id = getPointId(x31, y31);
				NetworkRouteSegment segment = new NetworkRouteSegment(bMdo, x31, y31);
				if (!routes.containsKey(id)) {
					routes.put(id, segment);
				} else {
					NetworkRouteSegment routeSegment = routes.get(id);
					routeSegment.addObject(bMdo);
				}
			}
		}

		public TLongObjectMap<NetworkRouteSegment> getRoutes() {
			return routes;
		}

		public NetworkRouteSegment getRouteSegment(int x31, int y31) {
			return routes.get(getPointId(x31, y31));
		}

		private long getPointId(long x31, long y31) {
			return (x31 << 31) + y31;
		}
	}

}
