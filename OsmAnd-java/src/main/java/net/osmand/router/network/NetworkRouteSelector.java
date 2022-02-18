package net.osmand.router.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.QuadRect;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class NetworkRouteSelector {
	
	
	private static final String ROUTE_PREFIX = "route_";
	private static final String ROUTE_KEY_VALUE_SEPARATOR = "_";
	
	
	private final NetworkRouteContext rCtx;
	
	public NetworkRouteSelector(BinaryMapIndexReader[] files, NetworkRouteSelectorFilter filter) {
		rCtx = new NetworkRouteContext(files, filter);
	}
	
	public static class NetworkRouteSelectorFilter {
		public Set<RouteKey> keyFilter = null; // null - all
		public Set<RouteType> typeFilter = null; // null -  all
		
		
		public boolean accept(BinaryMapDataObject bMdo) {
			if (keyFilter == null && typeFilter == null) {
				return true;
			}
			List<RouteKey> keys = RouteType.getRouteKeys(bMdo);
			for (RouteKey k : keys) {
				if (keyFilter != null && keyFilter.contains(k)) {
					return true;
				}
				if (typeFilter != null && typeFilter.contains(k.type)) {
					return true;
				}
			}
			return false;
		}
	}
	

	public Map<RouteKey, GPXFile> getRoutes(RenderedObject renderedObject) throws IOException {
		QuadRect qr = new QuadRect();
		qr.left = qr.right = renderedObject.getX().get(0);
		qr.top = qr.bottom = renderedObject.getY().get(0);
		return getRoutes(qr);
	}
	
	public Map<RouteKey, GPXFile> getRoutes(QuadRect bBox) throws IOException {
		Map<RouteKey, GPXFile> res = new LinkedHashMap<RouteKey, GPXUtilities.GPXFile>();
		List<BinaryMapDataObject> finalSegmentList = new ArrayList<>();
		int x = (int) bBox.left;
		int y = (int) bBox.bottom;
		int xStart;
		int yStart;
		for (BinaryMapDataObject segment : getRouteSegments(x, y)) {
			finalSegmentList.add(segment);
			xStart = segment.getPoint31XTile(0);
			yStart = segment.getPoint31YTile(0);
			x = segment.getPoint31XTile(segment.getPointsLength() - 1);
			y = segment.getPoint31YTile(segment.getPointsLength() - 1);
			getRoutePart(finalSegmentList, x, y);
			getRoutePart(finalSegmentList, xStart, yStart);
		}
		if (!finalSegmentList.isEmpty()) {
			// TODO
			res.put(rCtx.filter.keyFilter.iterator().next(), createGpxFile(finalSegmentList));
			finalSegmentList.clear();
		}
		return res;
	}

	private List<BinaryMapDataObject> getRouteSegments(int x, int y) throws IOException {
		return rCtx.loadRouteSegment(x, y);
	}
	
	private void getRoutePart(List<BinaryMapDataObject> finalSegmentList, int x, int y) throws IOException {
		List<BinaryMapDataObject> foundSegmentList = new ArrayList<>();
		boolean exit = false;
		while (!exit) {
			foundSegmentList.addAll(getRouteSegments(x, y));
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
				foundSegmentList.addAll(getRouteSegments(x, y));
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

	private BinaryMapDataObject processRoundabout(BinaryMapDataObject foundSegment,
			List<BinaryMapDataObject> finalSegmentList) throws IOException {
		List<BinaryMapDataObject> foundSegmentList = new ArrayList<>();
		for (int i = 0; i < foundSegment.getPointsLength(); i++) {
			foundSegmentList.clear();
			for (BinaryMapDataObject o : rCtx.loadRouteSegment(foundSegment.getPoint31XTile(i),
					foundSegment.getPoint31YTile(i))) {
				foundSegmentList.add(o);
			}
			List<BinaryMapDataObject> segments = rCtx.loadRouteSegment(foundSegment.getPoint31XTile(i), foundSegment.getPoint31YTile(i));
			foundSegmentList.addAll(segments);
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


	private boolean isRoundabout(BinaryMapDataObject segment) {
		int last = segment.getPointsLength() - 1;
		return last != 0 && segment.getPoint31XTile(last) == segment.getPoint31XTile(0)
				&& segment.getPoint31YTile(last) == segment.getPoint31YTile(0);
	}

	private boolean isConnected(BinaryMapDataObject segment, int xc, int yc) {
		int last = segment.getPointsLength() - 1;
		return (xc == segment.getPoint31XTile(last) && yc == segment.getPoint31YTile(last))
				|| (xc == segment.getPoint31XTile(0) && yc == segment.getPoint31YTile(0));
	}

	
	private GPXFile createGpxFile(List<BinaryMapDataObject> segmentList) {
		GPXFile gpxFile = new GPXFile(null, null, null);
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
		gpxFile.tracks.add(track);
		return gpxFile;
	}


	public static class RouteKey {
		public final RouteType type;
		public final Set<String> set = new TreeSet<String>();
		
		public RouteKey(RouteType routeType) {
			this.type = routeType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((set == null) ? 0 : set.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RouteKey other = (RouteKey) obj;
			if (set == null) {
				if (other.set != null)
					return false;
			} else if (!set.equals(other.set))
				return false;
			if (type != other.type)
				return false;
			return true;
		}
	}
	
	public enum RouteType {

		HIKING("hiking"),
		BICYCLE("bicycle"),
		MTB("mtb"),
		HORSE("horse");
		private final String tag;

		RouteType(String tag) {
			this.tag = tag;
		}

		public String getType() {
			return tag;
		}

		public String getTagWithPrefix() {
			return ROUTE_PREFIX + tag + "_";
		}


		public static boolean isRoute(String tag) {
			if (tag == null || tag.length() == 0) {
				return false;
			}
			for (RouteType routeType : values()) {
				if (tag.startsWith(routeType.getTagWithPrefix())) {
					return true;
				}
			}
			return false;
		}
		
		public static List<RouteKey> getRouteStringKeys(RenderedObject o) {
			Map<String, String> tags = o.getTags();
			return getRouteKeys(tags);
		}
		
		public static List<RouteKey> getRouteKeys(BinaryMapDataObject bMdo) {
			Map<String, String> tags = new TreeMap<>();
			for (int i = 0; i < bMdo.getObjectNames().keys().length; i++) {
				int keyInd = bMdo.getObjectNames().keys()[i];
				TagValuePair tp = bMdo.getMapIndex().decodeType(keyInd);
				String value = bMdo.getObjectNames().get(keyInd);
				if (tp != null) {
					tags.put(tp.tag, value);
				}
			}
			int[] tps = bMdo.getAdditionalTypes();
			for (int i = 0; i < tps.length; i++) {
				TagValuePair tp = bMdo.getMapIndex().decodeType(tps[i]);
				if (tp != null) {
					tags.put(tp.tag, tp.value);
				}
			}
			tps = bMdo.getTypes();
			for (int i = 0; i < tps.length; i++) {
				TagValuePair tp = bMdo.getMapIndex().decodeType(tps[i]);
				if (tp != null) {
					tags.put(tp.tag, tp.value);
				}
			}
			return getRouteKeys(tags);
		}

		private static int getRouteQuantity(Map<String, String> tags, RouteType rType) {
			int q = 0;
			for (String tag : tags.keySet()) {
				if (tag.startsWith(rType.getTagWithPrefix())) {
					int num = Algorithms.extractIntegerNumber(tag);
					if (num > 0 && tag.equals(rType.getTagWithPrefix() + num)) {
						q = Math.max(q, num);
					}
				}
			}
			return q;
		}
		
		private static List<RouteKey> getRouteKeys(Map<String, String> tags) {
			List<RouteKey> lst = new ArrayList<RouteKey>();
			for (RouteType routeType : RouteType.values()) {
				int rq = getRouteQuantity(tags, routeType);
				for (int routeIdx = 1; routeIdx <= rq; routeIdx++) {
					String prefix = routeType.getTagWithPrefix() + routeIdx;
					RouteKey key = new RouteKey(routeType);
					for (Map.Entry<String, String> e : tags.entrySet()) {
						String tag = e.getKey();
						if (tag.startsWith(prefix)) {
							String tagSubname = tag.substring(prefix.length());
							if (Algorithms.isEmpty(e.getValue())) {
								key.set.add(routeType.getTagWithPrefix() + tagSubname);
							} else {
								key.set.add(routeType.getTagWithPrefix() + tagSubname + ROUTE_KEY_VALUE_SEPARATOR
										+ e.getValue());
							}
						}
					}
					lst.add(key);
				}
			}
			return lst;
		}
	}
}
