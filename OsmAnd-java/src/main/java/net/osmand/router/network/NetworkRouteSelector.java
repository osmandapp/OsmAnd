package net.osmand.router.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
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

	private static final boolean GROW_ALGORITHM = false;
	private static final int MAX_ITERATIONS = 16000;
	// works only if road in same tile
	private static final double MAX_RADIUS_HOLE = 30;
	private static final double CONNECT_POINTS_DISTANCE = 20;

	
	private final NetworkRouteContext rCtx;
	
	// TODO 0. Search by bbox
	// TODO 1. FIX & implement work with routing tags
	// TODO TEST 2.1 growth in the middle - Test 1 
	// TODO      2.2 roundabout 
	// TODO      2.3 step back technique 
	// TEST:
	// 1. Round routes
	// 2. Loop & middle & roundabout: https://www.openstreetmap.org/way/23246638#map=19/47.98180/11.28338
	//    Lots deviations https://www.openstreetmap.org/relation/1075081#map=8/47.656/10.456
	// 3. https://www.openstreetmap.org/relation/1200009#map=8/60.592/10.940
	// 4. + https://www.openstreetmap.org/relation/138401#map=19/51.06795/7.37955
	// 5. + https://www.openstreetmap.org/relation/145490#map=16/51.0607/7.3596
	
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
		return getRoutes(x, y);
	}

	public Map<RouteKey, GPXFile> getRoutes(int x, int y) throws IOException {
		Map<RouteKey, GPXFile> res = new LinkedHashMap<RouteKey, GPXUtilities.GPXFile>();
		for (NetworkRouteSegment segment : rCtx.loadRouteSegment(x, y)) {
			if (res.containsKey(segment.routeKey)) {
				continue;
			}
			if (GROW_ALGORITHM) {
				growAlgorithm(segment, res);
			} else {
				connectAlgorithm(segment, res);
			}
		}
		return res;
	}
	
	public Map<RouteKey, GPXFile> getRoutes(QuadRect bBox) throws IOException {
		// TODO
		throw new UnsupportedOperationException();
	}
	
	
	private static class NetworkRouteSegmentChain {
		// simplified linked list (to not create list of chains)
		NetworkRouteSegmentChain alternatives;
		
		NetworkRouteSegment start;
		List<NetworkRouteSegment> connected;
		
		public int getEndPointX() {
			NetworkRouteSegment s = start;
			if(connected != null) {
				s = connected.get(connected.size() - 1);
			}
			return s.getEndPointX();
		}
		
		public int getEndPointY() {
			NetworkRouteSegment s = start;
			if(connected != null) {
				s = connected.get(connected.size() - 1);
			}
			return s.getEndPointY();
		}
		
		public void addChain(NetworkRouteSegmentChain toAdd) {
			if (toAdd.alternatives != null) {
				throw new IllegalArgumentException();
			}
			if (connected == null) {
				connected = new ArrayList<>();
			}
			connected.add(toAdd.start);
			if (toAdd.connected != null) {
				connected.addAll(toAdd.connected);
			}
		}
	}
	
	private void connectAlgorithm(NetworkRouteSegment segment, Map<RouteKey, GPXFile> res) throws IOException {
		RouteKey rkey = segment.routeKey;
		List<NetworkRouteSegment> loaded = new ArrayList<>();
		debug("START ", null, segment);
		loadData(segment, rkey, loaded);
		
		Map<Long, NetworkRouteSegmentChain> chains = createChainStructure(loaded);
		
		// Merged
		int merged = 1;
		while (merged > 0) {
			merged = connectSimpleStraight(chains);
			System.out.println("Simple merged: " + merged);
			merged += connectInverseStraight(chains);
			System.out.println("Inverse merged: " + merged);
		}
		
		List<NetworkRouteSegment> lst = flattenChainStructure(chains);
		GPXFile fl = createGpxFile(lst);
		res.put(segment.routeKey, fl);
		System.out.println("Segments size: " + fl.tracks.get(0).segments.size());
		debug("FINISH " + lst.size(), null, segment);
	}
	
	private int connectInverseStraight(Map<Long, NetworkRouteSegmentChain> chains) {
		int merged = 0;
		boolean changed = true;
		while (changed) {
			changed = false;
			for (Entry<Long, NetworkRouteSegmentChain> e : chains.entrySet()) {
				NetworkRouteSegmentChain c = e.getValue();
				if (c.alternatives != null) {
					long oldpnt = e.getKey();
					NetworkRouteSegmentChain alt = c.alternatives;
					long pnt = NetworkRouteContext.convertPontToLong(alt.getEndPointX(), alt.getEndPointY());
					if (chains.containsKey(pnt) || alt.alternatives != null) {
						continue;
					}
					NetworkRouteSegmentChain newChain = new NetworkRouteSegmentChain();
					List<NetworkRouteSegment> lst = new ArrayList<>();
					lst.add(c.start);
					if(c.connected != null) {
						lst.addAll(c.connected);
					}
					lst.add(0, alt.start.inverse());
					if (alt.connected != null) {
						for (NetworkRouteSegment s : alt.connected) {
							lst.add(0, s.inverse());
						}
					}
					newChain.start = lst.remove(0);
					newChain.connected = lst;
					NetworkRouteSegmentChain old = chains.put(pnt, newChain);
					if (old != null) {
						throw new IllegalStateException();
					}
					chains.remove(oldpnt);
					changed = true;
					merged++;
					break;
				}
			}
		}
		return merged;
	}

	private int connectSimpleStraight(Map<Long, NetworkRouteSegmentChain> chains) {
		int merged = 0;
		boolean changed = true;
		while (changed) {
			changed = false;
			mainLoop: for (NetworkRouteSegmentChain c : chains.values()) {
				NetworkRouteSegmentChain it = c;
				while (it != null) {
					long pnt = NetworkRouteContext.convertPontToLong(it.getEndPointX(), it.getEndPointY());
					NetworkRouteSegmentChain endChain = chains.get(pnt);
					if (endChain != null && endChain != c && endChain.alternatives == null) {
						it.addChain(endChain);
						chains.remove(pnt);
						changed = true;
						merged++;
						break mainLoop;
					}
					it = it.alternatives;
				}
			}
		}
		return merged;
	}

	private List<NetworkRouteSegment> flattenChainStructure(Map<Long, NetworkRouteSegmentChain> chains) {
		List<NetworkRouteSegment> lst = new ArrayList<>();
//		int i = 0;
		for (NetworkRouteSegmentChain c : chains.values()) {
//			if (i++ > 1) {
//				break;
//			}
			lst.add(c.start);
			
			if (c.connected != null) {
				lst.addAll(c.connected);
			}
			while (c.alternatives != null) {
				c = c.alternatives;
				lst.add(c.start);
				if (c.connected != null) {
					lst.addAll(c.connected);
				}
			}
		}
		return lst;
	}

	private Map<Long, NetworkRouteSegmentChain> createChainStructure(List<NetworkRouteSegment> lst) {
		Map<Long, NetworkRouteSegmentChain> chains = new LinkedHashMap<>();
		for (NetworkRouteSegment s : lst) {
			NetworkRouteSegmentChain chain = new NetworkRouteSegmentChain();
			chain.start = s;
			long pnt = NetworkRouteContext.convertPontToLong(s.getStartPointX(), s.getStartPointY());
			NetworkRouteSegmentChain existing = chains.get(pnt);
			if (existing == null) {
				chains.put(pnt, chain);
			} else {
				while (existing.alternatives != null) {
					existing = existing.alternatives;
				}
				existing.alternatives = chain;
			}
		}
		return chains;
	}

	private void loadData(NetworkRouteSegment segment, RouteKey rkey, List<NetworkRouteSegment> lst)
			throws IOException {
		TLongArrayList queue = new TLongArrayList();
		Set<Long> visitedTiles = new HashSet<>();
		Set<Long> objIds = new HashSet<>();
		long start = NetworkRouteContext.getTileId(segment.getStartPointX(), segment.getStartPointY());
		long end = NetworkRouteContext.getTileId(segment.getEndPointX(), segment.getEndPointY());
		queue.add(start);
		queue.add(end);
		while (!queue.isEmpty()) {
			long tile = queue.get(queue.size() - 1);
			queue.remove(queue.size() - 1, 1);
			if (!visitedTiles.add(tile)) {
				continue;
			}
			int left = NetworkRouteContext.getX31FromTileId(tile, 0);
			int top = NetworkRouteContext.getY31FromTileId(tile, 0);
			int right = NetworkRouteContext.getX31FromTileId(tile, 1);
			int bottom = NetworkRouteContext.getY31FromTileId(tile, 1);
			Map<RouteKey, List<NetworkRouteSegment>> tiles = rCtx.loadRouteSegmentTile(left, top, right - 1, bottom - 1, rkey);
			List<NetworkRouteSegment> loaded = tiles.get(rkey);
			int sz = loaded == null ? 0 : loaded.size();
			System.out.println(String.format("Load tile %d: %d segments", tile, sz));
			if (sz == 0) {
				continue;
			}
			for (NetworkRouteSegment s : loaded) {
				if (objIds.add(s.getId())) {
					lst.add(s);
				}
			}
			queue.add(NetworkRouteContext.getTileId(right, bottom));
			queue.add(NetworkRouteContext.getTileId(right, top));
			queue.add(NetworkRouteContext.getTileId(right, top - 1));
			queue.add(NetworkRouteContext.getTileId(left - 1, bottom));
			queue.add(NetworkRouteContext.getTileId(left - 1, top));
			queue.add(NetworkRouteContext.getTileId(left - 1, top - 1));
			queue.add(NetworkRouteContext.getTileId(left, bottom));
			// queue.add(NetworkRouteContext.getTileId(left, top)); // same
			queue.add(NetworkRouteContext.getTileId(left, top - 1));
		}
	}
	
	
	private void growAlgorithm(NetworkRouteSegment segment, Map<RouteKey, GPXFile> res) throws IOException {
		List<NetworkRouteSegment> lst = new ArrayList<>();
		TLongHashSet visitedIds = new TLongHashSet();
		visitedIds.add(segment.getId());
		lst.add(segment.inverse());
		debug("START ", null, segment);
		int it = 0;
		while (it++ < MAX_ITERATIONS) {
			if (!grow(lst, visitedIds, true, false)) {
				if (!grow(lst, visitedIds, true, true)) {
					it = 0;
					break;
				}
			}
		}
		Collections.reverse(lst);
		for (int i = 0; i < lst.size(); i++) {
			lst.set(i, lst.get(i).inverse());
		}
		while (it++ < MAX_ITERATIONS) {
			if (!grow(lst, visitedIds, false, false)) {
				if (!grow(lst, visitedIds, false, true)) {
					it = 0;
					break;
				}
			}
		}
		if (it != 0) {
			RouteKey rkey = segment.routeKey;
			TIntArrayList ids = new TIntArrayList();
			for (int i = lst.size() - 1; i > 0 && i > lst.size() - 50; i--) {
				ids.add((int) (lst.get(i).getId() >> 7));
			}
			String msg = "Route likely has a loop: " + rkey + " iterations " + it + " ids " + ids;
			System.err.println(msg); // throw new IllegalStateException();
		}
		res.put(segment.routeKey, createGpxFile(lst));
		debug("FINISH " + lst.size(), null, segment);

	}
	
	private void debug(String msg, Boolean reverse, NetworkRouteSegment ld) {
		System.out.println(msg + (reverse == null ? "" : (reverse ? '-' : '+')) + " " + ld);
	}
	
	private boolean grow(List<NetworkRouteSegment> lst, TLongHashSet visitedIds, boolean reverse, boolean approximate) throws IOException {
		int lastInd = lst.size() - 1;
		NetworkRouteSegment obj = lst.get(lastInd);
		List<NetworkRouteSegment> objs = approximate ? rCtx.loadNearRouteSegment(obj.getEndPointX(), obj.getEndPointY(), MAX_RADIUS_HOLE) : 
			rCtx.loadRouteSegment(obj.getEndPointX(), obj.getEndPointY());
		for (NetworkRouteSegment ld : objs) {
			debug("  CHECK", reverse, ld);
			if (ld.routeKey.equals(obj.routeKey) && !visitedIds.contains(ld.getId()) ) {
				// && ld.getId() != obj.getId() && otherSide.getId() != ld.getId()) {
				// visitedIds.add((ld.getId() << 14) + (reverse ? ld.end : ld.start))
				if (visitedIds.add(ld.getId())) { // forbid same segment twice
					debug(">ACCEPT", reverse, ld);
					lst.add(ld);
					return true;
				} else {
					// loop
					return false;
				}
			}
		}
		return false;
	}

	

	private GPXFile createGpxFile(List<NetworkRouteSegment> segmentList) {
		GPXFile gpxFile = new GPXFile(null, null, null);
		GPXUtilities.Track track = new GPXUtilities.Track();
		GPXUtilities.TrkSegment trkSegment = new GPXUtilities.TrkSegment();
		for (NetworkRouteSegment segment : segmentList) {
			int inc = segment.start < segment.end ? 1 : -1;
			for (int i = segment.start;; i += inc) {
				GPXUtilities.WptPt point = new GPXUtilities.WptPt();
				point.lat = MapUtils.get31LatitudeY(segment.getPoint31YTile(i));
				point.lon = MapUtils.get31LongitudeX(segment.getPoint31XTile(i));
				if (i == segment.start && trkSegment.points.size() > 0) {
					WptPt lst = trkSegment.points.get(trkSegment.points.size() - 1);
					double dst = MapUtils.getDistance(lst.lat, lst.lon, point.lat, point.lon);
					if (dst > 1) {
						if (dst > CONNECT_POINTS_DISTANCE) {
							track.segments.add(trkSegment);
							trkSegment = new GPXUtilities.TrkSegment();
						}
						trkSegment.points.add(point);
					}
				} else {
					trkSegment.points.add(point);
				}
				if (i == segment.end) {
					break;
				}
			}
		}
		track.segments.add(trkSegment);
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
