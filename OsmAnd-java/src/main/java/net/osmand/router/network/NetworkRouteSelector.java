package net.osmand.router.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
import net.osmand.router.network.NetworkRouteContext.NetworkRouteObject;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class NetworkRouteSelector {
	
	
	private static final String ROUTE_PREFIX = "route_";
	private static final String ROUTE_KEY_VALUE_SEPARATOR = "_";
	
	
	private final NetworkRouteContext rCtx;
	
	public NetworkRouteSelector(BinaryMapIndexReader[] files, NetworkRouteSelectorFilter filter) {
		if (filter == null) {
			filter = new NetworkRouteSelectorFilter();
		}
		rCtx = new NetworkRouteContext(files, filter);
	}
	
	public static class NetworkRouteSelectorFilter {
		public Set<RouteKey> keyFilter = null; // null - all
		public Set<RouteType> typeFilter = null; // null -  all
		
		public List<RouteKey> convert(BinaryMapDataObject bMdo) {
			List<RouteKey> keys = RouteType.getRouteKeys(bMdo);
			if (keyFilter == null && typeFilter == null) {
				return keys;
			}
			Iterator<RouteKey> it = keys.iterator();
			while (it.hasNext()) {
				RouteKey key = it.next();
				if (keyFilter != null && !keyFilter.contains(key)) {
					it.remove();
				} else if (typeFilter != null && !typeFilter.contains(key.type)) {
					it.remove();
				}
			}
			return keys;
		}
	}
	

	public Map<RouteKey, GPXFile> getRoutes(RenderedObject renderedObject) throws IOException {
		int x = renderedObject.getX().get(0);
		int y = renderedObject.getY().get(0);
		Map<RouteKey, GPXFile> res = new LinkedHashMap<RouteKey, GPXUtilities.GPXFile>();
		for (NetworkRouteObject segment : getRouteSegments(x, y)) {
			LinkedList<NetworkRouteObject> lst = new LinkedList<>();
			lst.add(segment);
			boolean growStart = true;
			while (growStart) {
				growStart = grow(lst, true);
			}
			boolean growEnd = true;
			while (growEnd) {
				growEnd = grow(lst, false);
			}
			res.put(segment.routeKey, createGpxFile(lst));
			
		}
		return res;
	}
	
	public Map<RouteKey, GPXFile> getRoutes(QuadRect bBox) throws IOException {
		// TODO search by bbox
		return null;
	}

	private List<NetworkRouteObject> getRouteSegments(int x, int y) throws IOException {
		return rCtx.loadRouteSegment(x, y);
	}
	
	private boolean grow(LinkedList<NetworkRouteObject> lst, boolean toFirst) throws IOException {
		NetworkRouteObject obj = toFirst ? lst.getFirst() : lst.getLast();
		NetworkRouteObject lastObj = !toFirst ? lst.getFirst() : lst.getLast();
		long pnt = toFirst ? obj.getStartPointLong() : obj.getEndPointLong();
		for (NetworkRouteObject ld : rCtx.loadRouteSegment(pnt)) {
			// TODO 1. approximate growth (with hole)
			// TODO 2. growth in the middle (cut)
			// TODO 3. roundabout ??
			if (ld.routeKey.equals(obj.routeKey) && ld.getId() != obj.getId() && lastObj.getId() != ld.getId()) {
				if (pnt == ld.getStartPointLong()) {
					if (toFirst) {
						ld.inverse();
						lst.addFirst(ld);
					} else {
						lst.addLast(ld);
					}
					return true;
				}
				if (pnt == ld.getEndPointLong()) {
					if (toFirst) {
						lst.addFirst(ld);
					} else {
						ld.inverse();
						lst.addLast(ld);
					}
					return true;
				}
			}
		}
		return false;
		
	}


	private NetworkRouteObject getNearestSegment(List<NetworkRouteObject> foundSegmentList, int x, int y) {
		NetworkRouteObject nearestSegment = foundSegmentList.get(0);
		double minDistance = getMinDistance(x, y, nearestSegment);
		for (NetworkRouteObject segment : foundSegmentList) {
			double segmentDistance = getMinDistance(x, y, segment);
			if (segmentDistance < minDistance) {
				minDistance = segmentDistance;
				nearestSegment = segment;
			}
		}
		return nearestSegment;
	}
	
	private double getMinDistance(int x, int y, NetworkRouteObject segment) {
		int last = segment.getPointsLength() - 1;
		return Math.min(MapUtils.squareDist31TileMetric(x, y, segment.getPoint31XTile(0), segment.getPoint31YTile(0)),
				MapUtils.squareDist31TileMetric(x, y, segment.getPoint31XTile(last), segment.getPoint31YTile(last)));
	}

	
	private GPXFile createGpxFile(List<NetworkRouteObject> segmentList) {
		GPXFile gpxFile = new GPXFile(null, null, null);
		GPXUtilities.Track track = new GPXUtilities.Track();
		for (NetworkRouteObject segment : segmentList) {
			GPXUtilities.TrkSegment trkSegment = new GPXUtilities.TrkSegment();
			int inc = segment.start < segment.end ? 1 : -1;
			for (int i = segment.start;; i += inc) {
				GPXUtilities.WptPt point = new GPXUtilities.WptPt();
				point.lat = MapUtils.get31LatitudeY(segment.getPoint31YTile(i));
				point.lon = MapUtils.get31LongitudeX(segment.getPoint31XTile(i));
				trkSegment.points.add(point);
				if (i == segment.end) {
					break;
				}
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
