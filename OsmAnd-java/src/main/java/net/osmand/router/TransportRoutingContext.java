package net.osmand.router;

import net.osmand.NativeLibrary;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.router.TransportRoutePlanner.TransportRouteSegment;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

public class TransportRoutingContext {

	public NativeLibrary library;
	public RouteCalculationProgress calculationProgress;
	public TLongObjectHashMap<TransportRouteSegment> visitedSegments = new TLongObjectHashMap<TransportRouteSegment>();
	public TransportRoutingConfiguration cfg;
	public TLongObjectHashMap<TransportRoute> combinedRoutesCache = new TLongObjectHashMap<TransportRoute>();
	public Map<TransportStop, List<TransportRoute>> missingStopsCache = new HashMap<TransportStop, List<TransportRoute>>();

	public TLongObjectHashMap<List<TransportRouteSegment>> quadTree;
	// Here we don't limit files by bbox, so it could be an issue while searching for multiple unused files
	// Incomplete routes usually don't need more files than around Max-BBOX of start/end,
	// so here an improvement could be introduced
	final TransportStopsRouteReader transportStopsReader;

	// stats
	public long startCalcTime;
	public int visitedRoutesCount;
	public int visitedStops;
	public int wrongLoadedWays;
	public int loadedWays;
	public long loadTime;
	public long readTime;

	private final int walkRadiusIn31;
	private final int walkChangeRadiusIn31;

	public TransportRoutingContext(TransportRoutingConfiguration cfg, NativeLibrary library, BinaryMapIndexReader... readers) {
		this.cfg = cfg;
		walkRadiusIn31 = (int) (cfg.walkRadius / MapUtils.getTileDistanceWidth(31));
		walkChangeRadiusIn31 = (int) (cfg.walkChangeRadius / MapUtils.getTileDistanceWidth(31));
		quadTree = new TLongObjectHashMap<List<TransportRouteSegment>>();
		this.library = library;
		transportStopsReader = new TransportStopsRouteReader(Arrays.asList(readers));
	}

	public List<TransportRouteSegment> getTransportStops(LatLon loc) throws IOException {
		int y = MapUtils.get31TileNumberY(loc.getLatitude());
		int x = MapUtils.get31TileNumberX(loc.getLongitude());
		return getTransportStops(x, y, false, new ArrayList<TransportRouteSegment>());
	}

	public List<TransportRouteSegment> getTransportStops(int x, int y, boolean change, List<TransportRouteSegment> res) throws IOException {
		return loadNativeTransportStops(x, y, change, res);
	}

	private List<TransportRouteSegment> loadNativeTransportStops(int sx, int sy, boolean change, List<TransportRouteSegment> res) throws IOException {
		long nanoTime = System.nanoTime();
		int d = change ? walkChangeRadiusIn31 : walkRadiusIn31;
		int lx = (sx - d ) >> (31 - cfg.ZOOM_TO_LOAD_TILES);
		int rx = (sx + d ) >> (31 - cfg.ZOOM_TO_LOAD_TILES);
		int ty = (sy - d ) >> (31 - cfg.ZOOM_TO_LOAD_TILES);
		int by = (sy + d ) >> (31 - cfg.ZOOM_TO_LOAD_TILES);
		for(int x = lx; x <= rx; x++) {
			for(int y = ty; y <= by; y++) {
				long tileId = (((long)x) << (cfg.ZOOM_TO_LOAD_TILES + 1)) + y;
				List<TransportRouteSegment> list = quadTree.get(tileId);
				if(list == null) {
					list = loadTile(x, y);
					quadTree.put(tileId, list);
				}
				for(TransportRouteSegment r : list) {
					TransportStop st = r.getStop(r.segStart);
					if (Math.abs(st.x31 - sx) > walkRadiusIn31 || Math.abs(st.y31 - sy) > walkRadiusIn31) {
						wrongLoadedWays++;
					} else {
						loadedWays++;
						res.add(r);
					}
				}
			}
		}
		loadTime += System.nanoTime() - nanoTime;
		return res;
	}


	private List<TransportRouteSegment> loadTile(int x, int y) throws IOException {
		long nanoTime = System.nanoTime();
		List<TransportRouteSegment> lst = new ArrayList<TransportRouteSegment>();
		int pz = (31 - cfg.ZOOM_TO_LOAD_TILES);
		BinaryMapIndexReader.SearchRequest<TransportStop> sr = BinaryMapIndexReader.buildSearchTransportRequest(x << pz, (x + 1) << pz,
				y << pz, (y + 1) << pz, -1, null);
		Collection<TransportStop> stops = transportStopsReader.readMergedTransportStops(sr);
		loadTransportSegments(stops, lst);
		readTime += System.nanoTime() - nanoTime;
		return lst;
	}

	private void loadTransportSegments(Collection<TransportStop> stops, List<TransportRouteSegment> lst) throws IOException {
		for(TransportStop s : stops) {
			if (s.isDeleted() || s.getRoutes() == null) {
				continue;
			}
			for (TransportRoute route : s.getRoutes()) {
				int stopIndex = -1;
				double dist = TransportRoute.SAME_STOP;
				for (int k = 0; k < route.getForwardStops().size(); k++) {
					TransportStop st = route.getForwardStops().get(k);
					if(st.getId().longValue() == s.getId().longValue() ) {
						stopIndex = k;
						break;
					}
					double d = MapUtils.getDistance(st.getLocation(), s.getLocation());
					if (d < dist) {
						stopIndex = k;
						dist = d;
					}
				}
				if (stopIndex != -1) {
					if (cfg != null && cfg.useSchedule) {
						loadScheduleRouteSegment(lst, route, stopIndex);
					} else {
						TransportRouteSegment segment = new TransportRouteSegment(route, stopIndex);
						lst.add(segment);
					}
				} else {
					System.err.println(String.format(Locale.US, "Routing error: missing stop '%s' in route '%s' id: %d",
							s.toString(), route.getRef(), route.getId() / 2));
				}
			}
		}
	}

	private void loadScheduleRouteSegment(List<TransportRouteSegment> lst, TransportRoute route, int stopIndex) {
		if(route.getSchedule() != null) {
			TIntArrayList ti = route.getSchedule().tripIntervals;
			int cnt = ti.size();
			int t = 0;
			// improve by using exact data
			int stopTravelTime = 0;
			TIntArrayList avgStopIntervals = route.getSchedule().avgStopIntervals;
			for (int i = 0; i < stopIndex; i++) {
				if (avgStopIntervals.size() > i) {
					stopTravelTime += avgStopIntervals.getQuick(i);
				}
			}
			for(int i = 0; i < cnt; i++) {
				t += ti.getQuick(i);
				int startTime = t + stopTravelTime;
				if(startTime >= cfg.scheduleTimeOfDay && startTime <= cfg.scheduleTimeOfDay + cfg.scheduleMaxTime ) {
					TransportRouteSegment segment = new TransportRouteSegment(route, stopIndex, startTime);
					lst.add(segment);
				}
			}
		}
	}
}
