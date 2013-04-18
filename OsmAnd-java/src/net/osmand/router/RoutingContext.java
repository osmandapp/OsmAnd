package net.osmand.router;


import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.osmand.PlatformUtil;
import net.osmand.NativeLibrary;
import net.osmand.NativeLibrary.NativeRouteSearchResult;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.RouteDataBorderLinePoint;
import net.osmand.binary.RouteDataObject;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentVisitor;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;


public class RoutingContext {

	public static final boolean SHOW_GC_SIZE = false;
	 
	
	private final static Log log = PlatformUtil.getLog(RoutingContext.class);
	public static final int OPTION_NO_LOAD = 0;
	public static final int OPTION_SMART_LOAD = 1;
	public static final int OPTION_IN_MEMORY_LOAD = 2;
	public static boolean USE_BORDER_LINES = false;
	// Final context variables
	public final RoutingConfiguration config;
	private final boolean useBaseMap;
	public final NativeLibrary nativeLib;
	public final Map<BinaryMapIndexReader, List<RouteSubregion>> map = new LinkedHashMap<BinaryMapIndexReader, List<RouteSubregion>>();
	public final Map<RouteRegion, BinaryMapIndexReader> reverseMap = new LinkedHashMap<RouteRegion, BinaryMapIndexReader>();
	
	// 1. Initial variables
	public long firstRoadId = 0;
	public int firstRoadDirection = 0;
	
	public int startX;
	public int startY;
	public int targetX;
	public int targetY;
	
	public RouteCalculationProgress calculationProgress;
	public List<RouteSegmentResult> previouslyCalculatedRoute;
	public BaseRouteResult baseRouteResult;

	// 2. Routing memory cache (big objects)
	TLongObjectHashMap<List<RoutingSubregionTile>> indexedSubregions = new TLongObjectHashMap<List<RoutingSubregionTile>>();
	TLongObjectHashMap<List<RouteDataObject>> tileRoutes = new TLongObjectHashMap<List<RouteDataObject>>();
	
	RouteDataBorderLine[] borderLines = new RouteDataBorderLine[0];
	int[] borderLineCoordinates = new int[0];
	int leftBorderBoundary;
	int rightBorderBoundary;
	
	// Needs to be a sorted array list . Another option to use hashmap but it will be more memory expensive
	List<RoutingSubregionTile> subregionTiles = new ArrayList<RoutingSubregionTile>();
	
	// 3. Warm object caches
	ArrayList<RouteSegment> segmentsToVisitPrescripted = new ArrayList<BinaryRoutePlanner.RouteSegment>(5);
	ArrayList<RouteSegment> segmentsToVisitNotForbidden = new ArrayList<BinaryRoutePlanner.RouteSegment>(5);

	
	// 5. debug information (package accessor)
	public TileStatistics global = new TileStatistics();
	// updated by route planner in bytes
	public int memoryOverhead = 0;
	
	
	long timeToLoad = 0;
	long timeToLoadHeaders = 0;
	long timeToFindInitialSegments = 0;
	long timeToCalculate = 0;
	
	int distinctLoadedTiles = 0;
	int maxLoadedTiles = 0;
	int loadedPrevUnloadedTiles = 0;
	int unloadedTiles = 0;
	public float routingTime = 0;
	public int loadedTiles = 0;
	public int visitedSegments = 0;
	public int relaxedSegments = 0;
	// callback of processing segments
	RouteSegmentVisitor visitor = null;


	// old planner
	public FinalRouteSegment finalRouteSegment;


	
	
	public RoutingContext(RoutingContext cp) {
		this.config = cp.config;
		this.map.putAll(cp.map);
		this.useBaseMap = cp.useBaseMap;
		this.reverseMap.putAll(cp.reverseMap);
		this.nativeLib = cp.nativeLib;
		// copy local data and clear caches
		for(RoutingSubregionTile tl : subregionTiles) {
			if(tl.isLoaded()) {
				subregionTiles.add(tl);
				for (RouteSegment rs : tl.routes.valueCollection()) {
					RouteSegment s = rs;
					while (s != null) {
						s.parentRoute = null;
						s.parentSegmentEnd = 0;
						s.distanceFromStart = 0;
						s.distanceToEnd = 0;
						s = s.next;
					}
				}
			}
		}
	}
	
	public RoutingContext(RoutingConfiguration config, NativeLibrary nativeLibrary, BinaryMapIndexReader[] map) {
		this(config, nativeLibrary, map, false);
	}
	
	public RoutingContext(RoutingConfiguration config, NativeLibrary nativeLibrary, BinaryMapIndexReader[] map, boolean useBasemap) {
		this.useBaseMap = useBasemap;
		for (BinaryMapIndexReader mr : map) {
			List<RouteRegion> rr = mr.getRoutingIndexes();
			List<RouteSubregion> subregions = new ArrayList<BinaryMapRouteReaderAdapter.RouteSubregion>();
			for (RouteRegion r : rr) {
				List<RouteSubregion> subregs = useBaseMap ? r.getBaseSubregions() :
					r.getSubregions();
				for (RouteSubregion rs : subregs) {
					subregions.add(new RouteSubregion(rs));
				}
				this.reverseMap.put(r, mr);
			}
			this.map.put(mr, subregions);
		}
		this.config = config;
		this.nativeLib = nativeLibrary;
	}
	
	
	public RouteSegmentVisitor getVisitor() {
		return visitor;
	}
	
	public int getCurrentlyLoadedTiles() {
		int cnt = 0;
		for(RoutingSubregionTile t : this.subregionTiles){
			if(t.isLoaded()) {
				cnt++;
			}
		}
		return cnt;
	}
	
	public int getCurrentEstimatedSize(){
		return global.size;
	}
	
	
	public void setVisitor(RouteSegmentVisitor visitor) {
		this.visitor = visitor;
	}

	public void setRouter(VehicleRouter router) {
		config.router = router;
	}
	
	public void setHeuristicCoefficient(float heuristicCoefficient) {
		config.heuristicCoefficient = heuristicCoefficient;
	}

	public VehicleRouter getRouter() {
		return config.router;
	}

	public boolean planRouteIn2Directions() {
		return config.planRoadDirection == 0;
	}

	public int getPlanRoadDirection() {
		return config.planRoadDirection;
	}

	public void setPlanRoadDirection(int planRoadDirection) {
		config.planRoadDirection = planRoadDirection;
	}

	public int roadPriorityComparator(double o1DistanceFromStart, double o1DistanceToEnd, double o2DistanceFromStart, double o2DistanceToEnd) {
		return BinaryRoutePlanner.roadPriorityComparator(o1DistanceFromStart, o1DistanceToEnd, o2DistanceFromStart, o2DistanceToEnd,
				config.heuristicCoefficient);
	}
	
	public void registerRouteDataObject(RouteDataObject o ) {
		if(!getRouter().acceptLine(o)){
			return;
		}
		for(int k = 0; k<o.getPointsLength(); k++) {
			int x31 = o.getPoint31XTile(k);
			int y31 = o.getPoint31YTile(k);
			long tileId = getRoutingTile(x31, y31, 0, OPTION_NO_LOAD);
			List<RouteDataObject> routes = tileRoutes.get(tileId);
			if(routes == null){
				routes = new ArrayList<RouteDataObject>();
				tileRoutes.put(tileId, routes);
			}
			if(!routes.contains(o)){
				routes.add(o);
			}
		}
	}
	
	public void unloadAllData() {
		unloadAllData(null);
	}
	
	public void unloadAllData(RoutingContext except) {
		for (RoutingSubregionTile tl : subregionTiles) {
			if (tl.isLoaded()) {
				if(except == null || except.searchSubregionTile(tl.subregion) < 0){
					tl.unload();
					unloadedTiles ++;
					global.size -= tl.tileStatistics.size;
				}
			}
		}
		subregionTiles.clear();
		tileRoutes.clear();		
		indexedSubregions.clear();
	}
	
	private int searchSubregionTile(RouteSubregion subregion){
		RoutingSubregionTile key = new RoutingSubregionTile(subregion);
		long now = System.nanoTime();
		int ind = Collections.binarySearch(subregionTiles, key, new Comparator<RoutingSubregionTile>() {
			@Override
			public int compare(RoutingSubregionTile o1, RoutingSubregionTile o2) {
				if(o1.subregion.left == o2.subregion.left) {
					return 0;
				}
				return o1.subregion.left < o2.subregion.left ? 1 : -1;
			}
		});
		if (ind >= 0) {
			for (int i = ind; i <= subregionTiles.size(); i++) {
				if (i == subregionTiles.size() || subregionTiles.get(i).subregion.left > subregion.left) {
					ind = -i - 1;
					return ind;
				}
				if (subregionTiles.get(i).subregion == subregion) {
					return i;
				}
			}
		}
		timeToLoadHeaders += (System.nanoTime() - now);
		return ind;
	}
	
	public void loadBorderPoints() throws IOException {
		Iterator<Entry<RouteRegion, BinaryMapIndexReader>> it = reverseMap.entrySet().iterator();
		int sleft = Math.min(startX, targetX);
		int sright= Math.max(startX, targetX);
		int stop = Math.min(startY, targetY);
		int sbottom= Math.max(startY, targetY);
		// one tile of 12th zoom around (?)
		int zoomAround = 10;
		int distAround = 1 << (31 - zoomAround);
		leftBorderBoundary = sleft - distAround;
		rightBorderBoundary = sright + distAround;
		SearchRequest<RouteDataBorderLinePoint> req = BinaryMapIndexReader.buildSearchRouteBorderRequest(sleft, sright, stop, sbottom);
		while(it.hasNext()) {
			Entry<RouteRegion, BinaryMapIndexReader> entry = it.next();
			entry.getValue().searchBorderPoints(req, entry.getKey());
		}
		TIntObjectHashMap<RouteDataBorderLine> lines = new TIntObjectHashMap<RoutingContext.RouteDataBorderLine>();
		for(RouteDataBorderLinePoint p : req.getSearchResults()) {
			if(config.router.acceptLine(p) && p.x > leftBorderBoundary && p.x < rightBorderBoundary) {
				if(!lines.containsKey(p.y)) {
					RouteDataBorderLine line = new RouteDataBorderLine(p.y);
					lines.put(p.y, line);
					RouteDataBorderLinePoint lft = new RouteDataBorderLinePoint(p.region);
					lft.y = p.y;
					lft.id = Long.MIN_VALUE;
					lft.x = leftBorderBoundary;
					line.borderPoints.add(lft);
					RouteDataBorderLinePoint rht = new RouteDataBorderLinePoint(p.region);
					rht.y = p.y;
					rht.id = Long.MIN_VALUE;
					rht.x = rightBorderBoundary;
					line.borderPoints.add(rht);
				}
				lines.get(p.y).borderPoints.add(p);
			}
		}
		borderLines =  lines.values(new RouteDataBorderLine[lines.size()]);
		Arrays.sort(borderLines);
		borderLineCoordinates = new int[borderLines.length];
		for(int i=0; i<borderLineCoordinates.length; i++) {
			borderLineCoordinates[i] = borderLines[i].borderLine;
			// FIXME borders approach
			// not less then 14th zoom
			if(i > 0 && borderLineCoordinates[i - 1] >> 17 == borderLineCoordinates[i] >> 17) {
				throw new IllegalStateException();
			}
			System.out.println("Line " + (borderLineCoordinates[i] >> 17) +
					" points " + borderLines[i].borderPoints.size() /* + " " +borderLines[i].borderPoints*/);
		}
		
		updateDistanceForBorderPoints(startX, startY, true);
		updateDistanceForBorderPoints(targetX, targetY, false);
		
	}

	protected void updateDistanceForBorderPoints(int sX, int sy, boolean distanceToStart) {
		boolean plus = borderLines.length > 0 && sy < borderLines[0].borderLine;
		if(borderLines.length > 0 && !plus && sy< borderLines[borderLines.length - 1].borderLine){
			throw new IllegalStateException();
		}
		// calculate min distance to start
		for(int i=0; i<borderLines.length; i++) {
			int ind = plus ? i : borderLines.length - i - 1;
			for(RouteDataBorderLinePoint ps : borderLines[ind].borderPoints){
				float res = (float) Math.sqrt(MapUtils.squareDist31TileMetric(sX, sy, ps.x, ps.y)) ; 
				if(i > 0){
					int prevInd = plus ? i - 1 : borderLines.length - i;
					double minDist = 0;
					for(RouteDataBorderLinePoint prevs : borderLines[prevInd].borderPoints){
						double d = Math.sqrt(MapUtils.squareDist31TileMetric(prevs.x, prevs.y, ps.x, ps.y)) + 
								(distanceToStart? prevs.distanceToStartPoint :  prevs.distanceToEndPoint);
						if(minDist == 0 || d < minDist) {
							minDist = d;
						}
					}
					if (minDist > 0) {
//						System.out.println("Border line " + i + " exp="+res + " min="+ minDist);
						res = (float) minDist;
					}
				}
				if(distanceToStart){
					ps.distanceToStartPoint = res;
				} else {
					ps.distanceToEndPoint = res;
				}
			}
			
		}
	}

	// returns from 0 to borderLineCoordinates.length inclusive
	public int searchBorderLineIndex(int y) {
		int k = Arrays.binarySearch(borderLineCoordinates, y);
		if( k < 0) {
			k = -(k + 1);
		}
		return k;
	}
	
	public RouteSegment loadRouteSegment(int x31, int y31, int memoryLimit) {
		long tileId = getRoutingTile(x31, y31, memoryLimit, OPTION_SMART_LOAD);
		TLongObjectHashMap<RouteDataObject> excludeDuplications = new TLongObjectHashMap<RouteDataObject>();
		RouteSegment original = null;
		if (tileRoutes.containsKey(tileId)) {
			List<RouteDataObject> routes = tileRoutes.get(tileId);
			if (routes != null) {
				for (RouteDataObject ro : routes) {
					for (int i = 0; i < ro.pointsX.length; i++) {
						if (ro.getPoint31XTile(i) == x31 && ro.getPoint31YTile(i) == y31) {
							excludeDuplications.put(calcRouteId(ro, i), ro);
							RouteSegment segment = new RouteSegment(ro, i);
							segment.next = original;
							original = segment;
						}
					}
				}
			}
		}
		List<RoutingSubregionTile> subregions = indexedSubregions.get(tileId);
		if (subregions != null) {
			for (RoutingSubregionTile rs : subregions) {
				original = rs.loadRouteSegment(x31, y31, this, excludeDuplications, original);
			}
		}
		return original;
	}
	
	private void loadSubregionTile(final RoutingSubregionTile ts, boolean loadObjectsInMemory) {
		boolean wasUnloaded = ts.isUnloaded();
		int ucount = ts.getUnloadCont();
		if (nativeLib == null) {
			long now = System.nanoTime();
			try {
				BinaryMapIndexReader reader = reverseMap.get(ts.subregion.routeReg);
				ts.setLoadedNonNative();
				List<RouteDataObject> res = reader.loadRouteIndexData(ts.subregion);
//				System.out.println(ts.subregion.shiftToData + " " + res);
				for(RouteDataObject ro : res){
					if(ro != null && config.router.acceptLine(ro)) {
						ts.add(ro);
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("Loading data exception", e);
			}

			timeToLoad += (System.nanoTime() - now);
		} else {
			long now = System.nanoTime();
			NativeRouteSearchResult ns = nativeLib.loadRouteRegion(ts.subregion, loadObjectsInMemory);
//			System.out.println(ts.subregion.shiftToData + " " + Arrays.toString(ns.objects));
			ts.setLoadedNative(ns, this);
			timeToLoad += (System.nanoTime() - now);
		}
		loadedTiles++;
		if (wasUnloaded) {
			if(ucount == 1) {
				loadedPrevUnloadedTiles++;
			}
		} else {
			if(global != null) {
				global.allRoutes += ts.tileStatistics.allRoutes;
				global.coordinates += ts.tileStatistics.coordinates;
			}
			distinctLoadedTiles++;
		}
		global.size += ts.tileStatistics.size;
	}

	private List<RoutingSubregionTile> loadTileHeaders(final int x31, final int y31) {
		final int zoomToLoad = 31 - config.ZOOM_TO_LOAD_TILES;
		int tileX = x31 >> zoomToLoad;
		int tileY = y31 >> zoomToLoad;
		
		SearchRequest<RouteDataObject> request = BinaryMapIndexReader.buildSearchRouteRequest(tileX << zoomToLoad,
				(tileX + 1) << zoomToLoad, tileY << zoomToLoad, (tileY + 1) << zoomToLoad, null);
		List<RoutingSubregionTile> collection = null;
		for (Entry<BinaryMapIndexReader, List<RouteSubregion>> r : map.entrySet()) {
			// NOTE: load headers same as we do in non-native (it is not native optimized)
			try {
				if (r.getValue().size() > 0) {
					long now = System.nanoTime();
					// int rg = r.getValue().get(0).routeReg.regionsRead;
					List<RouteSubregion> subregs = r.getKey().searchRouteIndexTree(request, r.getValue());
					for (RouteSubregion sr : subregs) {
						int ind = searchSubregionTile(sr);
						RoutingSubregionTile found;
						if (ind < 0) {
							found = new RoutingSubregionTile(sr);
							subregionTiles.add(-(ind + 1), found);
						} else {
							found = subregionTiles.get(ind);
						}
						if (collection == null) {
							collection = new ArrayList<RoutingContext.RoutingSubregionTile>(4);
						}
						collection.add(found);
					}
					timeToLoadHeaders += (System.nanoTime() - now);
				}
			} catch (IOException e) {
				throw new RuntimeException("Loading data exception", e);
			}
		}
		return collection;
	}

	public void loadTileData(int x31, int y31, int zoomAround, final List<RouteDataObject> toFillIn) {
		int t =  config.ZOOM_TO_LOAD_TILES - zoomAround;
		int coordinatesShift = (1 << (31 - config.ZOOM_TO_LOAD_TILES));
		if(t <= 0) {
			t = 1;
			coordinatesShift = (1 << (31 - zoomAround));
		} else {
			t = 1 << t;
		}
		
		TLongHashSet ts = new TLongHashSet(); 
		long now = System.nanoTime();
		for(int i = -t; i <= t; i++) {
			for(int j = -t; j <= t; j++) {
				ts.add(getRoutingTile(x31 +i*coordinatesShift, y31 + j*coordinatesShift, 0, OPTION_IN_MEMORY_LOAD));		
			}
		}
		TLongIterator it = ts.iterator();
		while(it.hasNext()){
			getAllObjects(it.next(), toFillIn);
		}
		timeToFindInitialSegments += (System.nanoTime() - now);
	}
	
	@SuppressWarnings("unused")
	private long getRoutingTile(int x31, int y31, int memoryLimit, int loadOptions){
//		long now = System.nanoTime();
		long xloc = x31 >> (31 - config.ZOOM_TO_LOAD_TILES);
		long yloc = y31 >> (31 - config.ZOOM_TO_LOAD_TILES);
		long tileId = (xloc << config.ZOOM_TO_LOAD_TILES) + yloc;
		if (loadOptions != OPTION_NO_LOAD) {
			if( memoryLimit == 0){
				memoryLimit = config.memoryLimitation;
			}
			if (getCurrentEstimatedSize() > 0.9 * memoryLimit) {
				int sz1 = getCurrentEstimatedSize();
				long h1 = 0;
				if (SHOW_GC_SIZE && sz1 > 0.7 * memoryLimit) {
					runGCUsedMemory();
					h1 = runGCUsedMemory();
				}
				int clt = getCurrentlyLoadedTiles();
				long us1 = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
				unloadUnusedTiles(memoryLimit);
				if (h1 != 0 && getCurrentlyLoadedTiles() != clt) {
					int sz2 = getCurrentEstimatedSize();
					runGCUsedMemory();
					long h2 = runGCUsedMemory();
					float mb = (1 << 20);
					log.warn("Unload tiles :  estimated " + (sz1 - sz2) / mb + " ?= " + (h1 - h2) / mb + " actual");
					log.warn("Used after " + h2 / mb + " of " + Runtime.getRuntime().totalMemory() / mb + " max "
							+ maxMemory() / mb);
				} else {
					 float mb = (1 << 20);
					 int sz2 = getCurrentEstimatedSize();
					 log.warn("Unload tiles :  occupied before " + sz1 / mb + " Mb - now  " + sz2 / mb + "MB " + 
					 memoryLimit/mb + " limit MB " + config.memoryLimitation/mb);
					 long us2 = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
					 log.warn("Used memory before " + us1 / mb + "after " + us1 / mb + " of max " + maxMemory() / mb);
				}
			}
			if (!indexedSubregions.containsKey(tileId)) {
				List<RoutingSubregionTile> collection = loadTileHeaders(x31, y31);
				indexedSubregions.put(tileId, collection);
			}
			List<RoutingSubregionTile> subregions = indexedSubregions.get(tileId);
			if (subregions != null) {
				for (RoutingSubregionTile ts : subregions) {
					if (!ts.isLoaded()) {
						loadSubregionTile(ts, loadOptions == OPTION_IN_MEMORY_LOAD);
					}
				}
			}
		}
		// timeToLoad += (System.nanoTime() - now);
		return tileId;
	}

	private long maxMemory() {
		// AVIAN FIXME
//		return Runtime.getRuntime().maxMemory();
		return 0;
	}

	
	
	public boolean checkIfMemoryLimitCritical(int memoryLimit) {
		return getCurrentEstimatedSize() > 0.9 * memoryLimit;
	}
	
	public void unloadUnusedTiles(int memoryLimit) {
		float desirableSize = memoryLimit * 0.7f;
		List<RoutingSubregionTile> list = new ArrayList<RoutingSubregionTile>(subregionTiles.size() / 2);
		int loaded = 0;
		for(RoutingSubregionTile t : subregionTiles) {
			if(t.isLoaded()) {
				list.add(t);
				loaded++;
			}
		}
		maxLoadedTiles = Math.max(maxLoadedTiles, getCurrentlyLoadedTiles());
		Collections.sort(list, new Comparator<RoutingSubregionTile>() {
			private int pow(int base, int pw) {
				int r = 1;
				for (int i = 0; i < pw; i++) {
					r *= base;
				}
				return r;
			}
			@Override
			public int compare(RoutingSubregionTile o1, RoutingSubregionTile o2) {
				int v1 = (o1.access + 1) * pow(10, o1.getUnloadCont() -1);
				int v2 = (o2.access + 1) * pow(10, o2.getUnloadCont() -1);
				return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
			}
		});
		int i = 0;
		while(getCurrentEstimatedSize() >= desirableSize && (list.size() - i) > loaded / 5 && i < list.size()) {
			RoutingSubregionTile unload = list.get(i);
			i++;
//			System.out.println("Unload " + unload);
			unload.unload();
			unloadedTiles ++;
			global.size -= unload.tileStatistics.size;
			// tile could be cleaned from routing tiles and deleted from whole list
			
		}
		for(RoutingSubregionTile t : subregionTiles) {
			t.access /= 3;
		}
	}
	
	private void getAllObjects(long tileId, final List<RouteDataObject> toFillIn) {
		TLongObjectHashMap<RouteDataObject> excludeDuplications = new TLongObjectHashMap<RouteDataObject>();
		if (tileRoutes.containsKey(tileId)) {
			List<RouteDataObject> routes = tileRoutes.get(tileId);
			if (routes != null) {
				for (RouteDataObject ro : routes) {
					if (!excludeDuplications.contains(ro.id)) {
						excludeDuplications.put(ro.id, ro);
						toFillIn.add(ro);
					}
				}
			}
		}
		List<RoutingSubregionTile> subregions = indexedSubregions.get(tileId);
		if (subregions != null) {
			for (RoutingSubregionTile rs : subregions) {
				rs.loadAllObjects(toFillIn, this, excludeDuplications);
			}
		}
	}
	
	
	
	protected static long runGCUsedMemory()  {
		Runtime runtime = Runtime.getRuntime();
		long usedMem1 = runtime.totalMemory() - runtime.freeMemory();
		long usedMem2 = Long.MAX_VALUE;
		int cnt = 4;
		while (cnt-- >= 0) {
			for (int i = 0; (usedMem1 < usedMem2) && (i < 1000); ++i) {
				// AVIAN FIXME
				runtime.runFinalization();
				runtime.gc();
				Thread.yield();

				usedMem2 = usedMem1;
				usedMem1 = runtime.totalMemory() - runtime.freeMemory();
			}
		}
		return usedMem1;
	}
	
	
	private static long calcRouteId(RouteDataObject o, int ind) {
		return (o.getId() << 10) + ind;
	}


	public static class RoutingSubregionTile {
		public final RouteSubregion subregion;
		// make it without get/set for fast access
		public int access;
		public TileStatistics tileStatistics = new TileStatistics();
		
		private NativeRouteSearchResult searchResult = null;
		private int isLoaded = 0;
		private TLongObjectMap<RouteSegment> routes = null;

		public RoutingSubregionTile(RouteSubregion subregion) {
			this.subregion = subregion;
		}
		
		private void loadAllObjects(final List<RouteDataObject> toFillIn, RoutingContext ctx, TLongObjectHashMap<RouteDataObject> excludeDuplications) {
			if(routes != null) {
				Iterator<RouteSegment> it = routes.valueCollection().iterator();
				while(it.hasNext()){
					RouteSegment rs = it.next();
					while(rs != null){
						RouteDataObject ro = rs.road;
						if (!excludeDuplications.contains(ro.id)) {
							excludeDuplications.put(ro.id, ro);
							toFillIn.add(ro);
						}
						rs = rs.next;
					}
				}
			} else if(searchResult != null) {
				RouteDataObject[] objects = searchResult.objects;
				if(objects != null) {
					for(RouteDataObject ro : objects) {
						if (ro != null && !excludeDuplications.contains(ro.id)) {
							excludeDuplications.put(ro.id, ro);
							toFillIn.add(ro);
						}
					}
				}
			}
		}
		
		private RouteSegment loadRouteSegment(int x31, int y31, RoutingContext ctx, 
				TLongObjectHashMap<RouteDataObject> excludeDuplications, RouteSegment original) {
			if(searchResult == null && routes == null) {
				return original;
			}
			access++;
			if (searchResult == null) {
				long l = (((long) x31) << 31) + (long) y31;
				RouteSegment segment = routes.get(l);
				while (segment != null) {
					RouteDataObject ro = segment.road;
					RouteDataObject toCmp = excludeDuplications.get(calcRouteId(ro, segment.getSegmentStart()));
					if (toCmp == null || toCmp.getPointsLength() < ro.getPointsLength()) {
						excludeDuplications.put(calcRouteId(ro, segment.getSegmentStart()), ro);
						RouteSegment s = new RouteSegment(ro, segment.getSegmentStart());
						s.next = original;
						original = s;
					}
					segment = segment.next;
				}
				return original;
			}
			// Native use case
			long nanoTime = System.nanoTime();
			RouteDataObject[] res = ctx.nativeLib.getDataObjects(searchResult, x31, y31);
			ctx.timeToLoad += (System.nanoTime() - nanoTime);
			if (res != null) {
				for (RouteDataObject ro : res) {
					
					boolean accept = ro != null;
					if (ctx != null) {
						accept = ctx.getRouter().acceptLine(ro);
					}
					if (accept) {
						for (int i = 0; i < ro.pointsX.length; i++) {
							if (ro.getPoint31XTile(i) == x31 && ro.getPoint31YTile(i) == y31) {
								RouteDataObject toCmp = excludeDuplications.get(calcRouteId(ro, i));
								if (toCmp == null || toCmp.getPointsLength() < ro.getPointsLength()) {
									RouteSegment segment = new RouteSegment(ro, i);
									segment.next = original;
									original = segment;
									excludeDuplications.put(calcRouteId(ro, i), ro);
								}
							}
						}
					}
				}
			}
			return original;
		}
		
		public boolean isLoaded() {
			return isLoaded > 0;
		}
		
		public int getUnloadCont(){
			return Math.abs(isLoaded);
		}
		
		public boolean isUnloaded() {
			return isLoaded < 0;
		}
		
		public void unload() {
			if(isLoaded == 0) {
				this.isLoaded = -1;	
			} else {
				isLoaded = -Math.abs(isLoaded);
			}
			if(searchResult != null) {
				searchResult.deleteNativeResult();
			}
			searchResult = null;
			routes = null;
		}
		
		public void setLoadedNonNative(){
			isLoaded = Math.abs(isLoaded) + 1;
			routes = new TLongObjectHashMap<BinaryRoutePlanner.RouteSegment>();
			tileStatistics = new TileStatistics();
		}
		
		public void add(RouteDataObject ro) {
			tileStatistics.addObject(ro);
			for (int i = 0; i < ro.pointsX.length; i++) {
				int x31 = ro.getPoint31XTile(i);
				int y31 = ro.getPoint31YTile(i);
				long l = (((long) x31) << 31) + (long) y31;
				RouteSegment segment = new RouteSegment(ro, i);
				if (!routes.containsKey(l)) {
					routes.put(l, segment);
				} else {
					RouteSegment orig = routes.get(l);
					while (orig.next != null) {
						orig = orig.next;
					}
					orig.next = segment;
				}
			}
		}
		
		public void setLoadedNative(NativeRouteSearchResult r, RoutingContext ctx) {
			isLoaded = Math.abs(isLoaded) + 1;
			tileStatistics = new TileStatistics();
			if (r.objects != null) {
				searchResult = null;
				routes = new TLongObjectHashMap<BinaryRoutePlanner.RouteSegment>();
				for (RouteDataObject ro : r.objects) {
					if (ro != null && ctx.config.router.acceptLine(ro)) {
						add(ro);
					}
				}
			} else {
				searchResult = r;
				tileStatistics.size += 100;
			}
		}
	}
	
	static int getEstimatedSize(RouteDataObject o) {
		// calculate size
		int sz = 0;
		sz += 8 + 4; // overhead
		if (o.names != null) {
			sz += 12;
			TIntObjectIterator<String> it = o.names.iterator();
			while(it.hasNext()) {
				it.advance();
				String vl = it.value();
				sz += 12 + vl.length();
			}
			sz += 12 + o.names.size() * 25;
		}
		sz += 8; // id
		// coordinates
		sz += (8 + 4 + 4 * o.getPointsLength()) * 4;
		sz += o.types == null ? 4 : (8 + 4 + 4 * o.types.length);
		sz += o.restrictions == null ? 4 : (8 + 4 + 8 * o.restrictions.length);
		sz += 4;
		if (o.pointTypes != null) {
			sz += 8 + 4 * o.pointTypes.length;
			for (int i = 0; i < o.pointTypes.length; i++) {
				sz += 4;
				if (o.pointTypes[i] != null) {
					sz += 8 + 8 * o.pointTypes[i].length;
				}
			}
		}
		// Standard overhead?
		return  (int) (sz * 3.5);
	}
	
	protected static class TileStatistics {
		public int size = 0;
		public int allRoutes = 0;
		public int coordinates = 0;
		
		@Override
		public String toString() {
			return "All routes " + allRoutes + 
					" size " + (size / 1024f) + " KB coordinates " + coordinates + " ratio coord " + (((float)size) / coordinates)
					+ " ratio routes " + (((float)size) / allRoutes);
		}

		public void addObject(RouteDataObject o) {
			allRoutes++;
			coordinates += o.getPointsLength() * 2;
			size += getEstimatedSize(o);
		}

		
	}
	
	protected static class RouteDataBorderLine implements Comparable<RouteDataBorderLine>{
		final List<RouteDataBorderLinePoint> borderPoints = new ArrayList<RouteDataBorderLinePoint>();
		final int borderLine;
		
		public RouteDataBorderLine(int borderLine) {
			this.borderLine = borderLine;
		}


		@Override
		public int compareTo(RouteDataBorderLine o) {
			if(o.borderLine == borderLine) {
				return 0;
			}
			return borderLine < o.borderLine? -1 : 1;
		}
	}


}