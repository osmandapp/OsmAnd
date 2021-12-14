package net.osmand.router;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.QuadRect;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

import static net.osmand.NativeLibrary.*;

public class RouteSelector {
	public static final String ROUTE_PREFIX = "route_";
	public static final int DEVIATE = 200;
	public static final int SPLIT_RADIUS = 50000;
	final BinaryMapIndexReader[] files;
	// ROUTE_KEY: {route_bicycle_1=, route_bicycle_1_node_network=rcn, route_bicycle_1_ref=67-68} -> "route_bicycle___node_network_rcn___ref_67-68"
	private static final String ROUTE_KEY_SEPARATOR = "___";
	private static final String ROUTE_KEY_VALUE_SEPARATOR = "_";
	
	public RouteSelector(BinaryMapIndexReader[] files) {
		this.files = files;
	}

	public List<GPXFile> getRoutes(RenderedObject renderedObject) {
		QuadRect qr = new QuadRect();
		qr.left = qr.right = renderedObject.getLabelX();
		qr.top = qr.bottom = renderedObject.getLabelY();
		MapIndex mapIndex = getMapIndexPerObject(renderedObject);
		return getRoutes(qr, mapIndex, null, null);
	}

	public List<GPXFile> getRoutes(QuadRect bbox, MapIndex mapIndex, Set<String> routeKeys, Set<String> routePrefixes) {
		List<Way> wayList = new ArrayList<>();
		List<BinaryMapDataObject> foundSegmentList = new ArrayList<>();
		List<BinaryMapDataObject> finalSegmentList = new ArrayList<>();
		BinaryMapDataObject startSegment = null;

		int x = renderedObject.getX().get(0);
		int y = renderedObject.getY().get(0);
		int xStart = 0;
		int yStart = 0;
		long id = renderedObject.getId();

		try {

			final SearchRequest<BinaryMapDataObject> req = buildSearchRequest(foundSegmentList, x, y, false);
			foundSegmentList.clear();
			indexReader.searchMapIndex(req, mapIndex);
			if (!foundSegmentList.isEmpty()) {
				for (BinaryMapDataObject foundSegment : foundSegmentList) {
					if (id == foundSegment.getId()) {
						startSegment = foundSegment;
						break;
					}
				}
				BinaryMapDataObject segment = startSegment;
				finalSegmentList.add(segment);
				xStart = segment.getPoint31XTile(0);
				yStart = segment.getPoint31YTile(0);
				x = segment.getPoint31XTile(segment.getPointsLength() - 1);
				y = segment.getPoint31YTile(segment.getPointsLength() - 1);
			}
			getRoutePart(finalSegmentList, x, y, indexReader, mapIndex, false);
			getRoutePart(finalSegmentList, xStart, yStart, indexReader, mapIndex, false);

		} catch (IOException e) {
			e.printStackTrace();
		}
		for (BinaryMapDataObject segment : finalSegmentList) {
			Way w = new Way(-1);
			if (segment.getPointsLength() > 1) {
				for (int i = 0; i < segment.getPointsLength(); i++) {
					x = segment.getPoint31XTile(i);
					y = segment.getPoint31YTile(i);
					Node n = new Node(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x), -1);
					w.addNode(n);
				}
				wayList.add(w);
			}
		}
		return wayList;
	}
	
	private MapIndex getMapIndexPerObject(RenderedObject renderedObject) {
		// TODO Auto-generated method stub
		return null;
	}


	private void getRoutePart(List<BinaryMapDataObject> finalSegmentList, int x, int y,
	                          BinaryMapIndexReader indexReader, BinaryMapIndexReader.MapIndex mapIndex, boolean split)
			throws IOException {
		List<BinaryMapDataObject> foundSegmentList = new ArrayList<>();
		boolean exit = false;
		while (!exit) {
			SearchRequest<BinaryMapDataObject> req = buildSearchRequest(foundSegmentList, x, y, split);
			foundSegmentList.clear();
			indexReader.searchMapIndex(req, mapIndex);
			exit = true;
			Iterator<BinaryMapDataObject> i = foundSegmentList.iterator();
			while(i.hasNext()){
				BinaryMapDataObject s = i.next();
				if(!isConnected(s, x, y) && !isRoundabout(s)){
					i.remove();
					continue;
				}
				for(BinaryMapDataObject fo:finalSegmentList) {
					if (s.getId() == fo.getId()){
						i.remove();
						break;
					}
				}
			}

			if (foundSegmentList.isEmpty()) {
				req = buildSearchRequest(foundSegmentList, x, y, true);
				foundSegmentList.clear();
				indexReader.searchMapIndex(req, mapIndex);
				removeExistedSegments(finalSegmentList, foundSegmentList);

				if (!foundSegmentList.isEmpty()) {
					BinaryMapDataObject foundSegment = getNearestSegment(foundSegmentList, x, y);
					foundSegmentList.clear();
					foundSegmentList.add(foundSegment);
					int xb = foundSegment.getPoint31XTile(0);
					int yb = foundSegment.getPoint31YTile(0);
					int xe = foundSegment.getPoint31XTile(foundSegment.getPointsLength() - 1);
					int ye = foundSegment.getPoint31YTile(foundSegment.getPointsLength() - 1);
					double distBegin = MapUtils.squareDist31TileMetric(x, y, xb, yb);
					double distEnd = MapUtils.squareDist31TileMetric(x, y, xe, ye);
					if (distBegin < distEnd) {
						x = xb;
						y = yb;
					} else {
						x = xe;
						y = ye;
					}
				}
			}

			for (BinaryMapDataObject foundSegment : foundSegmentList) {
				if (isRoundabout(foundSegment)) {
					finalSegmentList.add(foundSegment);
					foundSegment = processRoundabout(foundSegment, finalSegmentList, indexReader, mapIndex);
					int xb = foundSegment.getPoint31XTile(0);
					int yb = foundSegment.getPoint31YTile(0);
					int xe = foundSegment.getPoint31XTile(foundSegment.getPointsLength() - 1);
					int ye = foundSegment.getPoint31YTile(foundSegment.getPointsLength() - 1);
					double distBegin = MapUtils.squareDist31TileMetric(x, y, xb, yb);
					double distEnd = MapUtils.squareDist31TileMetric(x, y, xe, ye);
					if (distBegin < distEnd) {
						x = xb;
						y = yb;
					} else {
						x = xe;
						y = ye;
					}
				}
				finalSegmentList.add(foundSegment);
				int xNext = foundSegment.getPoint31XTile(foundSegment.getPointsLength() - 1);
				int yNext = foundSegment.getPoint31YTile(foundSegment.getPointsLength() - 1);
				if (xNext == x && yNext == y) {
					xNext = foundSegment.getPoint31XTile(0);
					yNext = foundSegment.getPoint31YTile(0);
				}
				if (foundSegmentList.size() > 1) {
					getRoutePart(finalSegmentList, xNext, yNext, indexReader, mapIndex, split);
				} else {
					exit = false;
					x = xNext;
					y = yNext;
				}
			}
		}
	}

	private void removeExistedSegments(List<BinaryMapDataObject> finalSegmentList,
	                                   List<BinaryMapDataObject> foundSegmentList) {
		Iterator<BinaryMapDataObject> it = foundSegmentList.iterator();
		while(it.hasNext()){
			BinaryMapDataObject o = it.next();
			for(BinaryMapDataObject fo: finalSegmentList) {
				if (o.getId() == fo.getId()){
					it.remove();
					break;
				}
			}
		}
	}

	private BinaryMapDataObject getNearestSegment(List<BinaryMapDataObject> foundSegmentList, int x, int y) {
		BinaryMapDataObject nearestSegment = foundSegmentList.get(0);
		double minDistance = getMinDistance(x, y, nearestSegment);
		for (BinaryMapDataObject segment : foundSegmentList) {
			double segmentDistance = getMinDistance(x, y, segment);
			if (segmentDistance < minDistance) {
				minDistance = segmentDistance;
				nearestSegment = segment;
			}
		}
		return nearestSegment;
	}

	private double getMinDistance(int x, int y, BinaryMapDataObject segment) {
		int last = segment.getPointsLength() - 1;
		return Math.min(MapUtils.squareDist31TileMetric(x, y, segment.getPoint31XTile(0), segment.getPoint31YTile(0)),
				MapUtils.squareDist31TileMetric(x, y, segment.getPoint31XTile(last), segment.getPoint31YTile(last)));
	}

	private BinaryMapDataObject processRoundabout(BinaryMapDataObject foundSegment, List<BinaryMapDataObject> finalSegmentList,
	                                              BinaryMapIndexReader indexReader,
	                                              BinaryMapIndexReader.MapIndex mapIndex) throws IOException {
		List<BinaryMapDataObject> foundSegmentList = new ArrayList<>();
		for (int i = 0; i < foundSegment.getPointsLength(); i++) {
			final SearchRequest<BinaryMapDataObject> req = buildSearchRequest(foundSegmentList,
					foundSegment.getPoint31XTile(i), foundSegment.getPoint31YTile(i), false);
			foundSegmentList.clear();
			indexReader.searchMapIndex(req, mapIndex);
			if (!foundSegmentList.isEmpty()) {
				removeExistedSegments(finalSegmentList, foundSegmentList);
				if (!foundSegmentList.isEmpty()) {
					break;
				}
			}
		}
		if (!foundSegmentList.isEmpty()) {
			return foundSegmentList.get(0);
		}
		return foundSegment;
	}

	private SearchRequest<BinaryMapDataObject> buildSearchRequest(final List<BinaryMapDataObject> foundSegmentList,
	                                                              int xc, int yc, boolean split) {
		int deviate = split ? SPLIT_RADIUS : DEVIATE;
		return BinaryMapIndexReader.buildSearchRequest(xc - deviate, xc + deviate,
				yc - deviate, yc + deviate,
				15, new BinaryMapIndexReader.SearchFilter() {
					@Override
					public boolean accept(TIntArrayList types, BinaryMapIndexReader.MapIndex index) {
						return true;
					}
				},
				new ResultMatcher<BinaryMapDataObject>() {
					@Override
					public boolean publish(BinaryMapDataObject object) {
						Map<Integer, List<String>> objectTagMap = new HashMap<>();
						for (int routeIdx = 1; routeIdx <= getRouteQuantity(object); routeIdx++) {
							String prefix = ROUTE_PREFIX + type + "_" + routeIdx;
							for (int i = 0; i < object.getObjectNames().keys().length; i++) {
								TagValuePair tp = object.getMapIndex().decodeType(object.getObjectNames().keys()[i]);
								if (tp != null && tp.tag != null && (tp.tag).startsWith(prefix)) {
									String value = object.getObjectNames().get(object.getObjectNames().keys()[i]);
									putTag(objectTagMap, routeIdx, value);
								}
							}
							for (int i = 0; i < object.getTypes().length; i++) {
								TagValuePair tp = object.getMapIndex().decodeType(object.getTypes()[i]);
								if (tp != null && tp.tag != null && (tp.tag).startsWith(prefix)) {
									String value = (tp.value == null) ? "" : tp.value;
									putTag(objectTagMap, routeIdx, value);
								}
							}
							for (int i = 0; i < object.getAdditionalTypes().length; i++) {
								TagValuePair tp = object.getMapIndex().decodeType(object.getAdditionalTypes()[i]);
								if (tp != null && tp.tag != null && (tp.tag).startsWith(prefix)) {
									String value = (tp.value == null) ? "" : tp.value;
									putTag(objectTagMap, routeIdx, value);
								}
							}
						}
						if (!objectTagMap.isEmpty()) {
							for (Map.Entry<Integer, List<String>> entry : objectTagMap.entrySet()) {
								List<String> objectTagList = entry.getValue();
								Collections.sort(objectTagList);
								StringBuilder objectTagKey = new StringBuilder();
								for (String s : objectTagList) {
									objectTagKey.append(s);
								}
								if (Algorithms.stringsEqual(tagKey, objectTagKey.toString())) {
									foundSegmentList.add(object);
								}
							}
						}
						return false;
					}

					private void putTag(Map<Integer, List<String>> objectTagMap, int routeIdx, String value) {
						List<String> tagList = objectTagMap.get(routeIdx);
						if (tagList == null) {
							tagList = new ArrayList<>();
						}
						tagList.add(value);
						objectTagMap.put(routeIdx, tagList);
					}

					private int getRouteQuantity(BinaryMapDataObject object) {
						List<String> tagsList = new ArrayList<>();
						for (int i = 0; i < object.getAdditionalTypes().length; i++) {
							TagValuePair tp = object.getMapIndex().decodeType(object.getAdditionalTypes()[i]);
							tagsList.add(tp.tag);
						}
						Collections.sort(tagsList);
						int routeQuantity = 0;
						for (int i = tagsList.size() - 1; i > 0; i--) {
							String tag = tagsList.get(i);
							if (tag.startsWith(ROUTE_PREFIX + type)) {
								routeQuantity = Algorithms.extractIntegerNumber(tag);
								break;
							}
						}
						return routeQuantity;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				});
	}

	private boolean isRoundabout(BinaryMapDataObject segment) {
		int last = segment.getPointsLength() - 1;
		return last != 0 && segment.getPoint31XTile(last) == segment.getPoint31XTile(0)
				&& segment.getPoint31YTile(last) == segment.getPoint31YTile(0);
	}

	private boolean isConnected(BinaryMapDataObject segment, int xc, int yc) {
		int last = segment.getPointsLength() - 1;
		return xc == segment.getPoint31XTile(last) && yc == segment.getPoint31YTile(last)
				|| xc == segment.getPoint31XTile(0) && yc == segment.getPoint31YTile(0);
	}

}
