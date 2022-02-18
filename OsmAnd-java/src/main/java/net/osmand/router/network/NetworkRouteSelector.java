package net.osmand.router.network;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.data.QuadRect;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.*;

public class NetworkRouteSelector {
	public static final int ZOOM = 15;
	public static final String ROUTE_PREFIX = "route_";
	public static final int DEVIATE = 200;
	public static final int SPLIT_RADIUS = 50000;
	final BinaryMapIndexReader[] files;
	// ROUTE_KEY: {route_bicycle_1=, route_bicycle_1_node_network=rcn, route_bicycle_1_ref=67-68} -> "route_bicycle___node_network_rcn___ref_67-68"
	public static final String ROUTE_KEY_SEPARATOR = "___";
	public static final String ROUTE_KEY_VALUE_SEPARATOR = "_";
	NetworkRouteContext rCtx;
	String routeKey;

	public enum RouteType {

		HIKING("hiking"),
		BICYCLE("bicycle"),
		MTB("mtb"),
		HORSE("horse");
		private final String type;

		RouteType(String type) {
			this.type = type;
		}

		public String getType() {
			return type;
		}

		public String getTypeWithPrefix() {
			return ROUTE_PREFIX + type;
		}


		public static boolean isRoute(String tag) {
			for (RouteType routeType : values()) {
				if (tag.startsWith(ROUTE_PREFIX + routeType.type)) {
					return true;
				}
			}
			return false;
		}
	}

	public NetworkRouteSelector(BinaryMapIndexReader[] files) {
		this.files = files;
		rCtx = new NetworkRouteContext();
	}

	public List<GPXFile> getRoutes(RenderedObject renderedObject, String routeKey) {
		this.routeKey = routeKey;
		QuadRect qr = new QuadRect();
		qr.left = qr.right = renderedObject.getX().get(0);
		qr.top = qr.bottom = renderedObject.getY().get(0);
		MapIndex mapIndex = getMapIndexPerObject(renderedObject);
		if (mapIndex == null) {
			return null;
		}
		return getRoutes(qr);
	}

	public static Set<String> getRouteStringKeys(RenderedObject o, Set<RouteType> typeSet) {
		List<String> allTagsList = new ArrayList<>(o.getTags().keySet());
		Set<String> routeTagsList = new HashSet<>();
		int routeQuantity = getRouteQuantity(allTagsList);
		if (routeQuantity != 0) {
			if (typeSet == null) {
				typeSet = new HashSet<>();
				typeSet.addAll(Arrays.asList(RouteType.values()));
			}
			for (RouteType routeType : typeSet) {
				for (int routeIdx = 1; routeIdx <= routeQuantity; routeIdx++) {
					StringBuilder tagKey = new StringBuilder();
					boolean start = true;
					for (String tag : allTagsList) {
						if (tag.startsWith(ROUTE_PREFIX + routeType.type + "_" + routeIdx)) {
							if (start) {
								start = false;
								tagKey.append(ROUTE_PREFIX).append(routeType.type);
							} else {
								tagKey.append(ROUTE_KEY_SEPARATOR)
										.append(tag.substring((ROUTE_PREFIX + routeType.type + "_" + routeIdx).length() + 1))
										.append(ROUTE_KEY_VALUE_SEPARATOR).append(o.getTags().get(tag));
							}
						}
					}
					if (tagKey.length() > 0) {
						routeTagsList.add(tagKey.toString());
					}
				}
			}
		}
		return routeTagsList;
	}

	static int getRouteQuantity(List<String> allTagsList) {
		Collections.sort(allTagsList);
		int routeQuantity = 0;
		for (int i = allTagsList.size() - 1; i > 0; i--) {
			String tag = allTagsList.get(i);
			if (tag.startsWith(ROUTE_PREFIX)) {
				int number = Algorithms.extractIntegerNumber(tag);
				if (routeQuantity < number) {
					routeQuantity = number;
				}
			}
		}
		return routeQuantity;
	}

	public static String getRouteStringKey(List<String> allTagsList, String routeTypeWithPrefix) {
		int routeQuantity = getRouteQuantity(allTagsList);
		if (routeQuantity != 0) {
			for (int routeIdx = 1; routeIdx <= routeQuantity; routeIdx++) {
				StringBuilder tagKey = new StringBuilder();
				boolean start = true;
				for (String tag : allTagsList) {
					if (tag.startsWith(routeTypeWithPrefix + "_" + routeIdx)) {
						if (start) {
							start = false;
							tagKey.append(routeTypeWithPrefix);
						} else {
							tagKey.append(ROUTE_KEY_SEPARATOR)
									.append(tag.substring((routeTypeWithPrefix + "_" + routeIdx).length() + 1));
						}
					}
				}
				if (tagKey.length() > 0) {
					return tagKey.toString();
				}
			}
		}
		return null;
	}

	public List<GPXFile> getRoutes(QuadRect bBox) {
		List<GPXFile> gpxFileList = new ArrayList<>();
		List<BinaryMapDataObject> finalSegmentList = new ArrayList<>();
		int x = (int) bBox.left;
		int y = (int) bBox.bottom;
		int xStart;
		int yStart;

		try {
			for (BinaryMapIndexReader indexReader : files) {
				rCtx.setReader(indexReader);
				NetworkRouteSegment routeSegment = getSegment(x, y);
				for (BinaryMapDataObject segment : routeSegment.getObjectsByRouteKey(routeKey)) {
					finalSegmentList.add(segment);
					xStart = segment.getPoint31XTile(0);
					yStart = segment.getPoint31YTile(0);
					x = segment.getPoint31XTile(segment.getPointsLength() - 1);
					y = segment.getPoint31YTile(segment.getPointsLength() - 1);
					getRoutePart(finalSegmentList, x, y);
					getRoutePart(finalSegmentList, xStart, yStart);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (!finalSegmentList.isEmpty()) {
			gpxFileList.add(createGpxFile(finalSegmentList));
			finalSegmentList.clear();
		}
		return gpxFileList;
	}

	private NetworkRouteSegment getSegment(int x, int y) throws IOException {
		return rCtx.loadRouteSegment(x, y);
	}

	GPXFile createGpxFile(List<BinaryMapDataObject> segmentList) {
		GPXFile gpxFile = null;
		if (!segmentList.isEmpty()) {
			GPXUtilities.Track track = new GPXUtilities.Track();
			for (BinaryMapDataObject segment : segmentList) {
				GPXUtilities.TrkSegment trkSegment = new GPXUtilities.TrkSegment();
				for (int i = 0; i < segment.getPointsLength(); i++) {
					GPXUtilities.WptPt point = new GPXUtilities.WptPt();
					point.lat = MapUtils.get31LatitudeY(segment.getPoint31YTile(i));
					point.lon = MapUtils.get31LongitudeX(segment.getPoint31XTile(i));
					trkSegment.points.add(point);
				}
				track.segments.add(trkSegment);
			}
			gpxFile = new GPXFile(null, null, null);
			gpxFile.tracks = new ArrayList<>();
			gpxFile.tracks.add(track);
		}
		return gpxFile;
	}

	private MapIndex getMapIndexPerObject(RenderedObject renderedObject) {
		int zoom = ZOOM;
		int x31 = renderedObject.getX().get(0);
		int y31 = renderedObject.getY().get(0);
		for (BinaryMapIndexReader reader : files) {
			for (MapIndex mapIndex : reader.getMapIndexes()) {
				for (BinaryMapIndexReader.MapRoot root : mapIndex.getRoots()) {
					if (root.getMinZoom() <= zoom && root.getMaxZoom() >= zoom) {
						if (x31 >= root.getLeft() && x31 <= root.getRight()
								&& root.getTop() <= y31 && root.getBottom() >= y31) {
							return mapIndex;
						}
					}
				}
			}
		}
		return null;
	}

	private void getRoutePart(List<BinaryMapDataObject> finalSegmentList, int x, int y)
			throws IOException {
		List<BinaryMapDataObject> foundSegmentList = new ArrayList<>();
		boolean exit = false;
		while (!exit) {
			NetworkRouteSegment routeSegment = rCtx.loadRouteSegment(x, y);
			foundSegmentList.addAll(routeSegment.getObjectsByRouteKey(routeKey));
			exit = true;
			Iterator<BinaryMapDataObject> i = foundSegmentList.iterator();
			while (i.hasNext()) {
				BinaryMapDataObject s = i.next();
				if (!isConnected(s, x, y) && !isRoundabout(s)) {
					i.remove();
					continue;
				}
				for (BinaryMapDataObject fs : finalSegmentList) {
					if (s.getId() == fs.getId()) {
						i.remove();
						break;
					}
				}
			}

			if (foundSegmentList.isEmpty()) {
				// TODO: find split segment
				foundSegmentList.addAll(rCtx.loadRouteSegment(x,y).getObjectsByRouteKey(routeKey));
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
					x = distBegin < distEnd ? xb : xe;
					y = distBegin < distEnd ? yb : ye;
				}
			}

			for (BinaryMapDataObject foundSegment : foundSegmentList) {
				if (isRoundabout(foundSegment)) {
					finalSegmentList.add(foundSegment);
					foundSegment = processRoundabout(foundSegment, finalSegmentList);
					int xb = foundSegment.getPoint31XTile(0);
					int yb = foundSegment.getPoint31YTile(0);
					int xe = foundSegment.getPoint31XTile(foundSegment.getPointsLength() - 1);
					int ye = foundSegment.getPoint31YTile(foundSegment.getPointsLength() - 1);
					double distBegin = MapUtils.squareDist31TileMetric(x, y, xb, yb);
					double distEnd = MapUtils.squareDist31TileMetric(x, y, xe, ye);
					x = distBegin < distEnd ? xb : xe;
					y = distBegin < distEnd ? yb : ye;
				}
				finalSegmentList.add(foundSegment);
				int xNext = foundSegment.getPoint31XTile(foundSegment.getPointsLength() - 1);
				int yNext = foundSegment.getPoint31YTile(foundSegment.getPointsLength() - 1);
				if (xNext == x && yNext == y) {
					xNext = foundSegment.getPoint31XTile(0);
					yNext = foundSegment.getPoint31YTile(0);
				}
				if (foundSegmentList.size() > 1) {
					getRoutePart(finalSegmentList, xNext, yNext);
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
		while (it.hasNext()) {
			BinaryMapDataObject o = it.next();
			for (BinaryMapDataObject fo : finalSegmentList) {
				if (o.getId() == fo.getId()) {
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

	private BinaryMapDataObject processRoundabout(BinaryMapDataObject foundSegment,
	                                              List<BinaryMapDataObject> finalSegmentList) throws IOException {
		List<BinaryMapDataObject> foundSegmentList = new ArrayList<>();
		for (int i = 0; i < foundSegment.getPointsLength(); i++) {
			foundSegmentList.clear();
			foundSegmentList.addAll(rCtx.loadRouteSegment(foundSegment.getPoint31XTile(i), foundSegment.getPoint31YTile(i))
					.getObjectsByRouteKey(routeKey));
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
