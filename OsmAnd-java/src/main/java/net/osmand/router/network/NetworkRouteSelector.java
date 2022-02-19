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
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadRect;
import net.osmand.router.network.NetworkRouteContext.NetworkRouteSegment;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class NetworkRouteSelector {
	
	private static final String ROUTE_KEY_VALUE_SEPARATOR = "__";

	private static final int MAX_ITERATIONS = 8192;
	// works only if road in same tile
	private static final double MAX_RADIUS_HOLE = 30;
	
	private final NetworkRouteContext rCtx;
	
	// TODO 0. search by bbox
	// TODO 1. Fix routing tags
	// TODO 2. growth in the middle (cut) 
	// TODO 3. roundabout ?? 
	// TODO 4. round routes
	// TODO 5. step back technique 
	// TO test:
	// https://www.openstreetmap.org/way/23246638 
	// https://www.openstreetmap.org/relation/138401#map=19/51.06795/7.37955
	// https://www.openstreetmap.org/relation/145490#map=16/51.0607/7.3596
	
	public NetworkRouteSelector(BinaryMapIndexReader[] files, NetworkRouteSelectorFilter filter) {
		this(files, filter, false);
	}
	
	public NetworkRouteSelector(BinaryMapIndexReader[] files, NetworkRouteSelectorFilter filter, boolean routing) {
		if (filter == null) {
			filter = new NetworkRouteSelectorFilter();
		}
		rCtx = new NetworkRouteContext(files, filter, routing);
	}
	
	public NetworkRouteContext getNetworkRouteContext() {
		return rCtx;
	}
	
	public Map<RouteKey, GPXFile> getRoutes(RenderedObject renderedObject) throws IOException {
		int x = renderedObject.getX().get(0);
		int y = renderedObject.getY().get(0);
		Map<RouteKey, GPXFile> res = new LinkedHashMap<RouteKey, GPXUtilities.GPXFile>();
		for (NetworkRouteSegment segment : getRouteSegments(x, y)) {
			LinkedList<NetworkRouteSegment> lst = new LinkedList<>();
			lst.add(segment);
			int it = 0;
			while (it++ < MAX_ITERATIONS) {
				if (!grow(lst, true, false)) {
					if (!grow(lst, true, true)) {
						it = 0;
						break;
					}
				}
			}
			while (it++ < MAX_ITERATIONS) {
				if(!grow(lst, false, false)) {
					if(!grow(lst, false, true)) {
						it = 0;
						break;
					}
				}
			}
			if (it != 0) {
				throw new IllegalStateException("Route likely has a loop: " + lst.subList(lst.size() - 20, lst.size() - 1));
			}
			res.put(segment.routeKey, createGpxFile(lst));
		}
		return res;
	}
	
	public Map<RouteKey, GPXFile> getRoutes(QuadRect bBox) throws IOException {
		return null;
	}

	private List<NetworkRouteSegment> getRouteSegments(int x, int y) throws IOException {
		return rCtx.loadRouteSegment(x, y);
	}
	
	private boolean grow(LinkedList<NetworkRouteSegment> lst, boolean toFirst, boolean approximate) throws IOException {
		NetworkRouteSegment obj = toFirst ? lst.getFirst() : lst.getLast();
		NetworkRouteSegment lastObj = !toFirst ? lst.getFirst() : lst.getLast();
		int x31 = toFirst ? obj.getStartPointX() : obj.getEndPointX();
		int y31 = toFirst ? obj.getStartPointY() : obj.getEndPointY();
		List<NetworkRouteSegment> objs = approximate ? rCtx.loadNearRouteSegment(x31, y31, MAX_RADIUS_HOLE) : 
			rCtx.loadRouteSegment(x31, y31);
		for (NetworkRouteSegment ld : objs) {
			// System.out.println(ld.getId() >> 7);
			if (ld.routeKey.equals(obj.routeKey) && ld.getId() != obj.getId() && lastObj.getId() != ld.getId()) {
				if (toFirst) {
					lst.addFirst(ld);
				} else {
					lst.addLast(ld);
				}
				return true;
			}
		}
		return false;
	}

	private GPXFile createGpxFile(List<NetworkRouteSegment> segmentList) {
		GPXFile gpxFile = new GPXFile(null, null, null);
		GPXUtilities.Track track = new GPXUtilities.Track();
		for (NetworkRouteSegment segment : segmentList) {
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


	public static class NetworkRouteSelectorFilter {
		public Set<RouteKey> keyFilter = null; // null - all
		public Set<RouteType> typeFilter = null; // null -  all
		
		public List<RouteKey> convert(BinaryMapDataObject obj) {
			return filterKeys(RouteType.getRouteKeys(obj));
		}

		public List<RouteKey> convert(RouteDataObject obj) {
			return filterKeys(RouteType.getRouteKeys(obj));
		}


		private List<RouteKey> filterKeys(List<RouteKey> keys) {
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

		@Override
		public String toString() {
			return "Route [type=" + type + ", set=" + set + "]";
		}
		
	}
	
	public enum RouteType {

		HIKING("hiking"),
		BICYCLE("bicycle"),
		MTB("mtb"),
		HORSE("horse");
		private final String tagPrefix;

		RouteType(String tag) {
			this.tagPrefix = "route_" + tag + "_";
		}


		public static List<RouteKey> getRouteKeys(RouteDataObject obj) {
			Map<String, String> tags = new TreeMap<>();
			for (int i = 0; obj.nameIds != null && i < obj.nameIds.length; i++) {
				int nameId = obj.nameIds[i];
				String value = obj.names.get(nameId);
				RouteTypeRule rt = obj.region.quickGetEncodingRule(nameId);
				if (rt != null) {
					tags.put(rt.getTag(), value);
				}
			}
			for (int i = 0; obj.types != null && i < obj.types.length; i++) {
				RouteTypeRule rt = obj.region.quickGetEncodingRule(obj.types[i]);
				if (rt != null) {
					tags.put(rt.getTag(), rt.getValue());
				}
			}
			return getRouteKeys(tags);
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
				if (tag.startsWith(rType.tagPrefix)) {
					int num = Algorithms.extractIntegerNumber(tag);
					if (num > 0 && tag.equals(rType.tagPrefix + num)) {
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
					String prefix = routeType.tagPrefix + routeIdx;
					RouteKey key = new RouteKey(routeType);
					for (Map.Entry<String, String> e : tags.entrySet()) {
						String tag = e.getKey();
						if (tag.startsWith(prefix)) {
							String tagPart = routeType.tagPrefix + tag.substring(prefix.length());
							if (Algorithms.isEmpty(e.getValue())) {
								key.set.add(tagPart);
							} else {
								key.set.add(tagPart + ROUTE_KEY_VALUE_SEPARATOR + e.getValue());
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
