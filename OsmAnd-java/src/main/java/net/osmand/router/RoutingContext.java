package net.osmand.router;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.NativeLibrary;
import net.osmand.NativeLibrary.NativeRouteSearchResult;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadPoint;
import net.osmand.data.QuadRect;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentVisitor;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.router.RoutingConfiguration.DirectionPoint;
import net.osmand.util.MapUtils;


public class RoutingContext {

	public static boolean SHOW_GC_SIZE = false;
	 
	
	private final static Log log = PlatformUtil.getLog(RoutingContext.class);
	
	// Final context variables
	public final RoutingConfiguration config;
	public final RouteCalculationMode calculationMode;
	public final NativeLibrary nativeLib;
	public final Map<BinaryMapIndexReader, List<RouteSubregion>> map = new LinkedHashMap<BinaryMapIndexReader, List<RouteSubregion>>();
	public final Map<RouteRegion, BinaryMapIndexReader> reverseMap = new LinkedHashMap<RouteRegion, BinaryMapIndexReader>();
	
	// 0. Reference to native routingcontext for multiple routes
	public long nativeRoutingContext;
	public boolean keepNativeRoutingContext;
	
	// 1. Initial variables
	public int startX;
	public int startY;
	public long startRoadId;
	public int startSegmentInd;
	public boolean startTransportStop;
	public int targetX;
	public int targetY;
	public long targetRoadId;
	public int targetSegmentInd;
	public boolean targetTransportStop;
	
	public boolean publicTransport;
	
	
	public RouteCalculationProgress calculationProgress;
	public boolean leftSideNavigation;
	public List<RouteSegmentResult> previouslyCalculatedRoute;
	public PrecalculatedRouteDirection precalculatedRouteDirection;
	
	
	// 2. Routing memory cache (big objects)
	TLongObjectHashMap<List<RoutingSubregionTile>> indexedSubregions = new TLongObjectHashMap<List<RoutingSubregionTile>>();
	
	// Needs to be a sorted array list . Another option to use hashmap but it will be more memory expensive
	List<RoutingSubregionTile> subregionTiles = new ArrayList<RoutingSubregionTile>();
	
	// 3. Warm object caches
	ArrayList<RouteSegment> segmentsToVisitPrescripted = new ArrayList<BinaryRoutePlanner.RouteSegment>(5);
	ArrayList<RouteSegment> segmentsToVisitNotForbidden = new ArrayList<BinaryRoutePlanner.RouteSegment>(5);
	
	
	// 5. debug information (package accessor)
	public TileStatistics global = new TileStatistics();
	// updated by route planner in bytes
	public int memoryOverhead = 0;
	public float routingTime = 0;

	// callback of processing segments
	RouteSegmentVisitor visitor = null;

	// old planner
	public FinalRouteSegment finalRouteSegment;


	
	RoutingContext(RoutingContext cp) {
		this.config = cp.config;
		this.map.putAll(cp.map);
		this.calculationMode = cp.calculationMode;
		this.leftSideNavigation = cp.leftSideNavigation;
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
	
	RoutingContext(RoutingConfiguration config, NativeLibrary nativeLibrary, BinaryMapIndexReader[] map, RouteCalculationMode calcMode) {
		this.calculationMode = calcMode;
		for (BinaryMapIndexReader mr : map) {
			List<RouteRegion> rr = mr.getRoutingIndexes();
			List<RouteSubregion> subregions = new ArrayList<BinaryMapRouteReaderAdapter.RouteSubregion>();
			for (RouteRegion r : rr) {
				List<RouteSubregion> subregs = calcMode == RouteCalculationMode.BASE ? r.getBaseSubregions() :
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

	public void setRouter(GeneralRouter router) {
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


	public int roadPriorityComparator(double o1DistanceFromStart, double o1DistanceToEnd, double o2DistanceFromStart, double o2DistanceToEnd) {
		return BinaryRoutePlanner.roadPriorityComparator(o1DistanceFromStart, o1DistanceToEnd, o2DistanceFromStart, o2DistanceToEnd,
				config.heuristicCoefficient);
	}
	
	public void initStartAndTargetPoints(RouteSegment start, RouteSegment end) {
		initTargetPoint(end);
		startX = start.road.getPoint31XTile(start.getSegmentStart());
		startY = start.road.getPoint31YTile(start.getSegmentStart());
		startRoadId = start.road.getId();
		startSegmentInd = start.getSegmentStart();
	}

	public void initTargetPoint(RouteSegment end) {
		targetX = end.road.getPoint31XTile(end.getSegmentStart());
		targetY = end.road.getPoint31YTile(end.getSegmentStart());
		targetRoadId = end.road.getId();
		targetSegmentInd = end.getSegmentStart();
	}
	
	public void unloadAllData() {
		unloadAllData(null);
	}
	
	public void unloadAllData(RoutingContext except) {
		for (RoutingSubregionTile tl : subregionTiles) {
			if (tl.isLoaded()) {
				if(except == null || except.searchSubregionTile(tl.subregion) < 0){
					tl.unload();
					if(calculationProgress != null) {
						calculationProgress.unloadedTiles ++;
					}
					global.size -= tl.tileStatistics.size;
				}
			}
		}
		subregionTiles.clear();
		indexedSubregions.clear();
	}
	
	private int searchSubregionTile(RouteSubregion subregion){
		RoutingSubregionTile key = new RoutingSubregionTile(subregion);
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
		return ind;
	}
	
	
	public RouteSegment loadRouteSegment(int x31, int y31, long memoryLimit) {
		long tileId = getRoutingTile(x31, y31, memoryLimit);
		TLongObjectHashMap<RouteDataObject> excludeDuplications = new TLongObjectHashMap<RouteDataObject>();
		RouteSegment original = null;
		List<RoutingSubregionTile> subregions = indexedSubregions.get(tileId);
		if (subregions != null) {
			for (int j = 0; j < subregions.size(); j++) {
				original = subregions.get(j).loadRouteSegment(x31, y31, this, excludeDuplications, 
						original, subregions, j);
			}
		}
		return original;
	}
	
	public void loadSubregionTile(final RoutingSubregionTile ts, boolean loadObjectsInMemory, List<RouteDataObject> toLoad, TLongHashSet excludeNotAllowed) {
		boolean wasUnloaded = ts.isUnloaded();
		int ucount = ts.getUnloadCont();
		if (nativeLib == null) {
			long now = System.nanoTime();

			List<DirectionPoint> points = Collections.emptyList();
			if (config.getDirectionPoints() != null) {
				//retrieve direction points for attach to routing
				points = config.getDirectionPoints().queryInBox(
						new QuadRect(ts.subregion.left, ts.subregion.top, ts.subregion.right, ts.subregion.bottom), new ArrayList<DirectionPoint>());
				int createType = ts.subregion.routeReg.findOrCreateRouteType(DirectionPoint.TAG, DirectionPoint.CREATE_TYPE);
				for (DirectionPoint d : points) {
					d.types.clear();
					for (Entry<String, String> e : d.getTags().entrySet()) {
						int type = ts.subregion.routeReg.searchRouteEncodingRule(e.getKey(), e.getValue());
						if (type != -1) {
							d.types.add(type);
						}
						d.types.add(createType);
					}
				}
			}

			try {
				BinaryMapIndexReader reader = reverseMap.get(ts.subregion.routeReg);
				ts.setLoadedNonNative();
				List<RouteDataObject> res = reader.loadRouteIndexData(ts.subregion);
				
				if (toLoad != null) {
					toLoad.addAll(res);
				} else {
					for (RouteDataObject ro : res) {
						if (ro != null) {
							if (config.routeCalculationTime != 0) {
								ro.processConditionalTags(config.routeCalculationTime);
							}
							if (config.router.acceptLine(ro)) {
								if (excludeNotAllowed != null && !excludeNotAllowed.contains(ro.getId())) {
									// don't attach point for route precalculation
									if (!config.router.attributes.containsKey(GeneralRouter.CHECK_ALLOW_PRIVATE_NEEDED)) {
										connectPoint(ts, ro, points);
									}
									ts.add(ro);
								}
							}
							if (excludeNotAllowed != null && ro.getId() > 0) {
								excludeNotAllowed.add(ro.getId());
								if (ts.excludedIds == null) {
									ts.excludedIds = new TLongHashSet();
								}
								ts.excludedIds.add(ro.getId());
							}
						}
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("Loading data exception", e);
			}
			if (calculationProgress != null) {
				calculationProgress.timeToLoad += (System.nanoTime() - now);
			}
			
		} else {
			long now = System.nanoTime();
			NativeRouteSearchResult ns = nativeLib.loadRouteRegion(ts.subregion, loadObjectsInMemory);
//			System.out.println(ts.subregion.shiftToData + " " + Arrays.toString(ns.objects));
			ts.setLoadedNative(ns, this);
			if (calculationProgress != null) {
				calculationProgress.timeToLoad += (System.nanoTime() - now);
			}
		}
		if (calculationProgress != null) {
			calculationProgress.loadedTiles++;
		}
		
		if (wasUnloaded) {
			if(ucount == 1) {
				if(calculationProgress != null) {
					calculationProgress.loadedPrevUnloadedTiles++;
				}
			}
		} else {
			if(global != null) {
				global.allRoutes += ts.tileStatistics.allRoutes;
				global.coordinates += ts.tileStatistics.coordinates;
			}
			if (calculationProgress != null) {
				calculationProgress.distinctLoadedTiles++;
			}
		}
		global.size += ts.tileStatistics.size;
	}

	

	private List<RoutingSubregionTile> loadTileHeaders(final int x31, final int y31) {
		final int zoomToLoad = 31 - config.ZOOM_TO_LOAD_TILES;
		int tileX = x31 >> zoomToLoad;
		int tileY = y31 >> zoomToLoad;
		return loadTileHeaders(zoomToLoad, tileX, tileY);
	}
	
	public void checkOldRoutingFiles(BinaryMapIndexReader key) {
		if(calculationMode == RouteCalculationMode.BASE && key.getDateCreated() < 1390172400000l) { // new SimpleDateFormat("dd-MM-yyyy").parse("20-01-2014").getTime()
			System.err.println("Old routing file : " + key.getDateCreated() + " " + new Date(key.getDateCreated()));
			String map = "";
			for (RouteRegion r : key.getRoutingIndexes()) {
				map = r.getName();
			}
 			throw new RuntimeException("Update map '"+map+ "' !");
		}		
	}
	
	public void checkOldRoutingFiles(int x31, int y31) {
		for (Entry<BinaryMapIndexReader, List<RouteSubregion>> r : map.entrySet()) {
			BinaryMapIndexReader reader = r.getKey();
			for(RouteRegion reg : reader.getRoutingIndexes()) {
				if(reg.contains(x31, y31)) {
					checkOldRoutingFiles(reader);
					break;
				}
			}
		}
	}
	
	public List<RoutingSubregionTile> loadAllSubregionTiles(BinaryMapIndexReader reader, RouteSubregion reg) throws IOException {
		List<RoutingSubregionTile> list = new ArrayList<RoutingContext.RoutingSubregionTile>();
		SearchRequest<RouteDataObject> request = BinaryMapIndexReader.buildSearchRouteRequest(0,
				Integer.MAX_VALUE, 0, Integer.MAX_VALUE, null);
		List<RouteSubregion> subregs = reader.searchRouteIndexTree(request, Collections.singletonList(reg));
		for(RouteSubregion s : subregs) {
			list.add(new RoutingSubregionTile(s));
		}
		return list;
	}

	public List<RoutingSubregionTile> loadTileHeaders(final int zoomToLoadM31, int tileX, int tileY) {
		SearchRequest<RouteDataObject> request = BinaryMapIndexReader.buildSearchRouteRequest(tileX << zoomToLoadM31,
				(tileX + 1) << zoomToLoadM31, tileY << zoomToLoadM31, (tileY + 1) << zoomToLoadM31, null);
		List<RoutingSubregionTile> collection = null;
		for (Entry<BinaryMapIndexReader, List<RouteSubregion>> r : map.entrySet()) {
			// NOTE: load headers same as we do in non-native (it is not native optimized)
			try {
				if (r.getValue().size() > 0) {
					long now = System.nanoTime();
					// int rg = r.getValue().get(0).routeReg.regionsRead;
					List<RouteSubregion> subregs = r.getKey().searchRouteIndexTree(request, r.getValue());
					if(subregs.size() > 0) {
						checkOldRoutingFiles(r.getKey());
					}
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
					if (calculationProgress != null) {
						calculationProgress.timeToLoadHeaders += (System.nanoTime() - now);
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("Loading data exception", e);
			}
		}
		return collection;
	}

	public void loadTileData(int x31, int y31, int zoomAround, final List<RouteDataObject> toFillIn) {
		loadTileData(x31, y31, zoomAround, toFillIn, false);
	}
	
	public void loadTileData(int x31, int y31, int zoomAround, final List<RouteDataObject> toFillIn, boolean allowDuplications) {
		int t =  config.ZOOM_TO_LOAD_TILES - zoomAround;
		int coordinatesShift = (1 << (31 - config.ZOOM_TO_LOAD_TILES));
		if(t <= 0) {
			t = 1;
			coordinatesShift = (1 << (31 - zoomAround));
		} else {
			t = 1 << t;
		}
		
		TLongHashSet ts = new TLongHashSet(); 
		for(int i = -t; i <= t; i++) {
			for(int j = -t; j <= t; j++) {
				ts.add(getRoutingTile(x31 +i*coordinatesShift, y31 + j*coordinatesShift, 0));		
			}
		}
		TLongIterator it = ts.iterator();
		TLongObjectHashMap<RouteDataObject> excludeDuplications = new TLongObjectHashMap<RouteDataObject>();
		while (it.hasNext()) {
			getAllObjects(it.next(), toFillIn, excludeDuplications);
			if (allowDuplications) {
				excludeDuplications.clear();
			}
		}
	}
	
	@SuppressWarnings("unused")
	private long getRoutingTile(int x31, int y31, long memoryLimit) {
		// long now = System.nanoTime();
		int zmShift = 31 - config.ZOOM_TO_LOAD_TILES;
		long xloc = x31 >> zmShift;
		long yloc = y31 >> zmShift;
		long tileId = (xloc << config.ZOOM_TO_LOAD_TILES) + yloc;
		if (memoryLimit == 0) {
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
				log.warn("Used after " + h2 / mb + " of " + Runtime.getRuntime().totalMemory() / mb );
			} else {
				float mb = (1 << 20);
				int sz2 = getCurrentEstimatedSize();
				log.warn("Unload tiles :  occupied before " + sz1 / mb + " Mb - now  " + sz2 / mb + "MB "
						+ memoryLimit / mb + " limit MB " + config.memoryLimitation / mb);
				long us2 = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
				log.warn("Used memory before " + us1 / mb + "after " + us1 / mb );
			}
		}
		if (!indexedSubregions.containsKey(tileId)) {
			List<RoutingSubregionTile> collection = loadTileHeaders(x31, y31);
			indexedSubregions.put(tileId, collection);
		}
		List<RoutingSubregionTile> subregions = indexedSubregions.get(tileId);
		if (subregions != null) {
			boolean load = false;
			for (RoutingSubregionTile ts : subregions) {
				if (!ts.isLoaded()) {
					load = true;
				}
			}
			if (load) {
				TLongHashSet excludeIds = new TLongHashSet();
				for (RoutingSubregionTile ts : subregions) {
					if (!ts.isLoaded()) {
						loadSubregionTile(ts, true, null, excludeIds);
					} else {
						if (ts.excludedIds != null) {
							excludeIds.addAll(ts.excludedIds);
						}
					}
				}
			}
		}
		// timeToLoad += (System.nanoTime() - now);
		return tileId;
	}

	
	private void connectPoint(final RoutingSubregionTile ts, RouteDataObject ro, List<DirectionPoint> points) {
		int createType = ro.region.findOrCreateRouteType(DirectionPoint.TAG, DirectionPoint.CREATE_TYPE);
		int deleteType = ro.region.findOrCreateRouteType(DirectionPoint.TAG, DirectionPoint.DELETE_TYPE);

		for (DirectionPoint np : points) {
			if (np.types.size() == 0) {
				continue;
			}

			int wptX = MapUtils.get31TileNumberX(np.getLongitude());
			int wptY = MapUtils.get31TileNumberY(np.getLatitude());

			int x = ro.getPoint31XTile(0);
			int y = ro.getPoint31YTile(0);

			double mindist = config.directionPointsRadius * 2;
			int indexToInsert = 0;
			int mprojx = 0;
			int mprojy = 0;
			for (int i = 1; i < ro.getPointsLength(); i++) {
				int nx = ro.getPoint31XTile(i);
				int ny = ro.getPoint31YTile(i);
				boolean sgnx = nx - wptX > 0;
				boolean sgx = x - wptX > 0;
				boolean sgny = ny - wptY > 0;
				boolean sgy = y - wptY > 0;
				boolean checkPreciseProjection = true;
				if (sgny == sgy && sgx == sgnx) {
					// Speedup: point outside of rect (line is diagonal) distance is likely be bigger
					double dist = MapUtils.squareRootDist31(wptX, wptY, Math.abs(nx - wptX) < Math.abs(x - wptX) ? nx : x,
							Math.abs(ny - wptY) < Math.abs(y - wptY) ? ny : y);
					checkPreciseProjection = dist < config.directionPointsRadius;
				}
				if (checkPreciseProjection) {
					QuadPoint pnt = MapUtils.getProjectionPoint31(wptX, wptY, x, y, nx, ny);
					int projx = (int) pnt.x;
					int projy = (int) pnt.y;
					double dist = MapUtils.squareRootDist31(wptX, wptY, projx, projy);
					if (dist < mindist) {
						indexToInsert = i;
						mindist = dist;
						mprojx = projx;
						mprojy = projy;
					}
				}


				x = nx;
				y = ny;
			}
			boolean sameRoadId = np.connected != null && np.connected.getId() == ro.getId();
			boolean pointShouldBeAttachedByDist = (mindist < config.directionPointsRadius && mindist < np.distance);

			double npAngle = np.getAngle();
			boolean restrictionByAngle = !Double.isNaN(npAngle);

			if (pointShouldBeAttachedByDist) {
				if (restrictionByAngle) {
					int oneWay = ro.getOneway();// -1 backward, 0 two way, 1 forward
					double forwardAngle = Math.toDegrees(ro.directionRoute(indexToInsert, true));
					if (oneWay == 1 || oneWay == 0) {
						double diff = Math.abs(MapUtils.degreesDiff(npAngle, forwardAngle));
						if (diff <= DirectionPoint.MAX_ANGLE_DIFF) {
							restrictionByAngle = false;
						}
					}
					if (restrictionByAngle && (oneWay == -1 || oneWay == 0)) {
						double diff = Math.abs(MapUtils.degreesDiff(npAngle, forwardAngle + 180));
						if (diff <= DirectionPoint.MAX_ANGLE_DIFF) {
							restrictionByAngle = false;
						}
					}
				}
				if (restrictionByAngle) {
					continue;
				}
				if (!sameRoadId) {
					//System.out.println(String.format("INSERT %s (%d-%d) %.0f m [%.5f, %.5f] - %d, %d ", ro, indexToInsert, indexToInsert + 1, mindist,
					//		MapUtils.get31LongitudeX(wptX), MapUtils.get31LatitudeY(wptY), wptX, wptY));
					if (np.connected != null) {
						// check old connected points
						int pointIndex = findPointIndex(np, createType);
						if (pointIndex != -1) {
							// set type "deleted" for old connected point
							np.connected.setPointTypes(pointIndex, new int[]{deleteType});
						} else {
							throw new RuntimeException();
						}
					}
				} else {
					int sameRoadPointIndex = findPointIndex(np, createType);
					if (sameRoadPointIndex != -1 && np.connected != null) {
						if (mprojx == np.connectedx && mprojy == np.connectedy) {
							continue;// was found the same point
						} else {
							// set type "deleted" for old connected point
							np.connected.setPointTypes(sameRoadPointIndex, new int[]{deleteType});
						}
					}
				}
				np.connectedx = mprojx;
				np.connectedy = mprojy;
				ro.insert(indexToInsert, mprojx, mprojy);
				ro.setPointTypes(indexToInsert, np.types.toArray());// np.types contains DirectionPoint.CREATE_TYPE
				np.distance = mindist;
				np.connected = ro;
			}
		}

	}

	private int findPointIndex(DirectionPoint np, int createType) {
		// using search by coordinates because by index doesn't work (parallel updates)
		int samePointIndex = -1;
		for (int i = 0; np.connected != null && i < np.connected.getPointsLength(); i++) {
			int tx = np.connected.getPoint31XTile(i);
			int ty = np.connected.getPoint31YTile(i);
			if (tx == np.connectedx && ty == np.connectedy && np.connected.hasPointType(i, createType)) {
				samePointIndex = i;
				break;
			}
		}
		return samePointIndex;
	}
	



	public boolean checkIfMemoryLimitCritical(long memoryLimit) {
		return getCurrentEstimatedSize() > 0.9 * memoryLimit;
	}
	
	public void unloadUnusedTiles(long memoryLimit) {
		float desirableSize = memoryLimit * 0.7f;
		List<RoutingSubregionTile> list = new ArrayList<RoutingSubregionTile>(subregionTiles.size() / 2);
		int loaded = 0;
		for(RoutingSubregionTile t : subregionTiles) {
			if(t.isLoaded()) {
				list.add(t);
				loaded++;
			}
		}
		if(calculationProgress != null) {
			calculationProgress.maxLoadedTiles = Math.max(calculationProgress.maxLoadedTiles, getCurrentlyLoadedTiles());
		}
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
			if(calculationProgress != null) {
				calculationProgress.unloadedTiles ++;
			}
			global.size -= unload.tileStatistics.size;
			// tile could be cleaned from routing tiles and deleted from whole list
			
		}
		for(RoutingSubregionTile t : subregionTiles) {
			t.access /= 3;
		}
	}
	
	private void getAllObjects(long tileId, final List<RouteDataObject> toFillIn, TLongObjectHashMap<RouteDataObject> excludeDuplications) {
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
		private TLongHashSet excludedIds = null;

		public RoutingSubregionTile(RouteSubregion subregion) {
			this.subregion = subregion;
		}
		
		public TLongObjectMap<RouteSegment> getRoutes() {
			return routes;
		}
		
		public void loadAllObjects(final List<RouteDataObject> toFillIn, RoutingContext ctx, TLongObjectHashMap<RouteDataObject> excludeDuplications) {
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
				TLongObjectHashMap<RouteDataObject> excludeDuplications, RouteSegment original, List<RoutingSubregionTile> subregions, int subregionIndex) {
			access++;
			if (routes != null) {
				long l = (((long) x31) << 31) + (long) y31;
				RouteSegment segment = routes.get(l);
				while (segment != null) {
					RouteDataObject ro = segment.road;
					RouteDataObject toCmp = excludeDuplications.get(calcRouteId(ro, segment.getSegmentStart()));
					if (!isExcluded(ro.id, subregions, subregionIndex)
							&& (toCmp == null || toCmp.getPointsLength() < ro.getPointsLength())) {
						excludeDuplications.put(calcRouteId(ro, segment.getSegmentStart()), ro);
						RouteSegment s = new RouteSegment(ro, segment.getSegmentStart());
						s.next = original;
						original = s;
					}
					segment = segment.next;
				}
			} else {
				throw new UnsupportedOperationException("Not clear how it could be used with native");
			}		
			return original;
		}

		private static boolean isExcluded(long id, List<RoutingSubregionTile> subregions, int subregionIndex) {
			for (int i = 0; i < subregionIndex; i++ ) {
				if (subregions.get(i).excludedIds != null && subregions.get(i).excludedIds.contains(id)) {
					return true;
				}
			}
			return false;
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
			excludedIds = null;
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
	
	public BinaryMapIndexReader[] getMaps() {
		return map.keySet().toArray(new BinaryMapIndexReader[0]);
	}

	public int getVisitedSegments() {
		if(calculationProgress != null) {
			return calculationProgress.visitedSegments; 
		}
		return 0;
	}

	public int getLoadedTiles() {
		if (calculationProgress != null) {
			return calculationProgress.loadedTiles;
		}
		return 0;
	}

	public synchronized void deleteNativeRoutingContext() {
		if (nativeRoutingContext != 0) {
			NativeLibrary.deleteNativeRoutingContext(nativeRoutingContext);
		}
		nativeRoutingContext = 0;
	}
	
	@Override
	protected void finalize() throws Throwable {
		deleteNativeRoutingContext();
		super.finalize();
	}

}
