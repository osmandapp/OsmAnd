package net.osmand.router.network;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.ResultMatcher;
import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.router.network.NetworkRouteSelector.NetworkRouteSelectorFilter;
import net.osmand.util.MapUtils;

import static net.osmand.router.network.NetworkRouteSelector.*;

import java.io.IOException;
import java.util.*;

public class NetworkRouteContext {
	
	private static final int ZOOM_TO_LOAD_TILES = 15;
	
	private TLongObjectHashMap<NetworkRoutesTile> indexedSubregions = new TLongObjectHashMap<>();
	private final BinaryMapIndexReader[] readers;
	final NetworkRouteSelectorFilter filter;

	public NetworkRouteContext(BinaryMapIndexReader[] readers, NetworkRouteSelectorFilter filter) {
		this.readers = readers;
		this.filter = filter;
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
	
	public BinaryMapIndexReader[] getReaders() {
		return readers;
	}

	public List<BinaryMapDataObject> loadRouteSegment(int x31, int y31) throws IOException {
		NetworkRoutesTile osmcRoutesTile = getMapRouteTile(x31, y31);
		NetworkRoutePoint point = osmcRoutesTile.getRouteSegment(x31, y31);
		if (point == null) {
			return Collections.emptyList();
		}
		return point.binaryMapDataObjects;
	}

	public NetworkRoutesTile getMapRouteTile(int x31, int y31) throws IOException {
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
		for (BinaryMapIndexReader reader : readers) {
			List<BinaryMapDataObject> objects = reader.searchMapIndex(req);
			for (BinaryMapDataObject bMdo : objects) {
				if (filter.accept(bMdo)) {
					osmcRoutesTile.add(bMdo);
				}
			}
		}
		return osmcRoutesTile;
	}

	private BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> buildTileRequest(int x, int y) {
		int tileX = x >> ZOOM_TO_LOAD_TILES;
		int tileY = y >> ZOOM_TO_LOAD_TILES;
		int tileLeft = tileX << ZOOM_TO_LOAD_TILES;
		int tileTop = tileY << ZOOM_TO_LOAD_TILES;
		int tileRight = (tileX + 1) << ZOOM_TO_LOAD_TILES;
		int tileBottom = (tileY + 1) << ZOOM_TO_LOAD_TILES;
		return BinaryMapIndexReader.buildSearchRequest(tileLeft, tileRight, tileTop, tileBottom, ZOOM_TO_LOAD_TILES,
				new BinaryMapIndexReader.SearchFilter() {
					@Override
					public boolean accept(TIntArrayList types, BinaryMapIndexReader.MapIndex index) {
						return true;
					}
				}, new ResultMatcher<BinaryMapDataObject>() {
					@Override
					public boolean publish(BinaryMapDataObject object) {
						boolean publish = false;
						publish = publish || checkObject(object, object.getObjectNames().keys());
						publish = publish || checkObject(object, object.getAdditionalTypes());
						publish = publish || checkObject(object, object.getTypes());
						return publish;
					}

					private boolean checkObject(BinaryMapDataObject object, int[] allTypes) {
						for (int allType : allTypes) {
							BinaryMapIndexReader.TagValuePair tp = object.getMapIndex().decodeType(allType);
							if (tp != null && RouteType.isRoute(tp.tag)) {
								return true;
							}
						}
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				});
	}
	
	
	public MapIndex getMapIndexPerObject(RenderedObject renderedObject) {
		int x31 = renderedObject.getX().get(0);
		int y31 = renderedObject.getY().get(0);
		for (BinaryMapIndexReader reader : readers) {
			for (MapIndex mapIndex : reader.getMapIndexes()) {
				for (BinaryMapIndexReader.MapRoot root : mapIndex.getRoots()) {
					if (root.getMinZoom() <= ZOOM_TO_LOAD_TILES && root.getMaxZoom() >= ZOOM_TO_LOAD_TILES) {
						if (x31 >= root.getLeft() && x31 <= root.getRight() && root.getTop() <= y31
								&& root.getBottom() >= y31) {
							return mapIndex;
						}
					}
				}
			}
		}
		return null;
	}

	private long getTileId(int x31, int y31) {
		int zmShift = (31 - ZOOM_TO_LOAD_TILES);
		return (long) (x31 >> zmShift) << (ZOOM_TO_LOAD_TILES + 1) + ((long) (y31 >> zmShift));
	}
	
	public class NetworkRoutePoint {
		public final int x31;
		public final int y31;
		public final List<BinaryMapDataObject> binaryMapDataObjects = new ArrayList<>();
		
		public NetworkRoutePoint(int x31, int y31) {
			this.x31 = x31;
			this.y31 = y31;
		}


		public void addObject(BinaryMapDataObject bMdo) {
			if (bMdo.getId() > 0) {
				for (BinaryMapDataObject obj : binaryMapDataObjects) {
					if (obj.getId() == bMdo.getId()) {
						return;
					}
				}
			}
			binaryMapDataObjects.add(bMdo);
		}

	}


	private class NetworkRoutesTile {
		private final TLongObjectMap<NetworkRoutePoint> routes = new TLongObjectHashMap<>();

		public void add(BinaryMapDataObject bMdo) {
			for (int i = 0; i < bMdo.getPointsLength(); i++) {
				int x31 = bMdo.getPoint31XTile(i);
				int y31 = bMdo.getPoint31YTile(i);
				long id = getPointId(x31, y31);
				NetworkRoutePoint point = routes.get(id);
				if (point == null) {
					point = new NetworkRoutePoint(x31, y31);
					routes.put(id, point);
				}
				point.addObject(bMdo);
			}
		}

		public TLongObjectMap<NetworkRoutePoint> getRoutes() {
			return routes;
		}

		public NetworkRoutePoint getRouteSegment(int x31, int y31) {
			return routes.get(getPointId(x31, y31));
		}

		private long getPointId(long x31, long y31) {
			return (x31 << 31) + y31;
		}
	}

}
