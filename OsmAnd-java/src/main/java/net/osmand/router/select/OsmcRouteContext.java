package net.osmand.router.select;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.router.BinaryRoutePlanner;

import java.io.IOException;
import java.util.*;

import static net.osmand.router.select.RouteSelector.*;

public class OsmcRouteContext {
	public int ZOOM_TO_LOAD_TILES = 16;
	TLongObjectHashMap<RoutesTile> indexedSubregions = new TLongObjectHashMap<>();
	private BinaryMapIndexReader reader;

	BinaryMapDataObject loadRouteSegment(int x31, int y31) {
		RoutesTile routesTile = getMapRouteTile(x31, y31);
		return routesTile.getRouteSegment(x31, y31);
	}

	RoutesTile getMapRouteTile(int x31, int y31) {
		long tileId = getTileId(x31, y31);
		if (!indexedSubregions.containsKey(tileId)) {
			RoutesTile routesTile = loadTile(x31, y31);
			indexedSubregions.put(tileId, routesTile);
		}
		RoutesTile routesTile = indexedSubregions.get(tileId);
		return routesTile;
	}

	private RoutesTile loadTile(int x31, int y31) {
		// TODO: 09.02.22
		final BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> req = buildTileRequest(x31, y31);
		RoutesTile routesTile = new RoutesTile();
		try {
			List<BinaryMapDataObject> objects = reader.searchMapIndex(req);
			if (!objects.isEmpty()) {

				for (BinaryMapDataObject bMdo : objects) {
					routesTile.add(bMdo);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return routesTile;
	}

	public void setReader(BinaryMapIndexReader reader) {
		this.reader = reader;
	}

	class RoutesTile {
		private final TLongObjectMap<BinaryMapDataObject> routes = new TLongObjectHashMap<>();

		public void add(BinaryMapDataObject bMdo) {
			for (int i = 0; i < bMdo.getPointsLength(); i++) {
				int x31 = bMdo.getPoint31XTile(i);
				int y31 = bMdo.getPoint31YTile(i);
				long l = getPointId(x31, y31);
				if (!routes.containsKey(l)) {
					routes.put(l, bMdo);
				}
			}
		}

		public BinaryMapDataObject getRouteSegment(int x31, int y31) {
			return routes.get(getPointId(x31, y31));
		}
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
//id -> object
						boolean publish = false;
						for (RouteType routeType : RouteType.values()) {

							Map<Integer, List<String>> objectTagMap = new HashMap<>();
							String prefix = ROUTE_PREFIX + routeType.getType() + "_";
							for (int i = 0; i < object.getObjectNames().keys().length; i++) {
								BinaryMapIndexReader.TagValuePair tp = object.getMapIndex().decodeType(object.getObjectNames().keys()[i]);
								if (tp != null && tp.tag != null && (tp.tag).startsWith(prefix)) {
									publish = true;
//									String tagWoPrefix = tp.tag;
//									String value = tagWoPrefix + ROUTE_KEY_VALUE_SEPARATOR
//											+ object.getObjectNames().get(object.getObjectNames().keys()[i]);
//									putTag(objectTagMap, value);
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
//									String tagWoPrefix = tp.tag;
//									String value = tagWoPrefix
//											+ (Algorithms.isEmpty(tp.value) ? "" : ROUTE_KEY_VALUE_SEPARATOR + tp.value);
//									putTag(objectTagMap, value);
								}
							}
//							if (!objectTagMap.isEmpty()) {
//								for (Map.Entry<Integer, List<String>> entry : objectTagMap.entrySet()) {
//									List<String> objectTagList = entry.getValue();
//									Collections.sort(objectTagList);
//									String objectTagKey = getRouteStringKeys(objectTagList, routeType);
//									if (Algorithms.stringsEqual(tagKey, objectTagKey)) {
//										foundSegmentList.add(object);
//									}
//								}
//							}
						}
						return publish;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}

//					private void putTag(Map<Integer, List<String>> objectTagMap, String value) {
//						List<String> tagList = objectTagMap.get(routeIdx);
//						if (tagList == null) {
//							tagList = new ArrayList<>();
//						}
//						tagList.add(value);
//						objectTagMap.put(routeIdx, tagList);
//					}

				});
	}

	private long getTileId(int x31, int y31) {
		int zmShift = 31 - ZOOM_TO_LOAD_TILES;
		return (long) (x31 >> zmShift) << ZOOM_TO_LOAD_TILES + ((long) (y31 >> zmShift));
	}

	private long getPointId(long x31, long y31) {
		return (x31 << 31) + y31;
	}
}
