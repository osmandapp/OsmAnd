package net.osmand.router.select;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;

import java.io.IOException;
import java.util.*;

import static net.osmand.router.select.RouteSelector.*;

public class OsmcRouteContext {
	public int ZOOM_TO_LOAD_TILES = 16;
	TLongObjectHashMap<OsmcRoutesTile> indexedSubregions = new TLongObjectHashMap<>();
	private BinaryMapIndexReader reader;

	OsmcRouteSegment loadRouteSegment(int x31, int y31) {
		OsmcRoutesTile osmcRoutesTile = getMapRouteTile(x31, y31);
		return osmcRoutesTile.getRouteSegment(x31, y31);
	}

	OsmcRoutesTile getMapRouteTile(int x31, int y31) {
		long tileId = getTileId(x31, y31);
		if (!indexedSubregions.containsKey(tileId)) {
			OsmcRoutesTile osmcRoutesTile = loadTile(x31, y31);
			indexedSubregions.put(tileId, osmcRoutesTile);
		}
		return indexedSubregions.get(tileId);
	}

	private OsmcRoutesTile loadTile(int x31, int y31) {
		// TODO: 09.02.22
		final BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> req = buildTileRequest(x31, y31);
		OsmcRoutesTile osmcRoutesTile = new OsmcRoutesTile();
		try {
			List<BinaryMapDataObject> objects = reader.searchMapIndex(req);
			if (!objects.isEmpty()) {
				for (BinaryMapDataObject bMdo : objects) {
					osmcRoutesTile.add(bMdo);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
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
}
