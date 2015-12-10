package net.osmand.router;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

public class RoutePlannerFrontEnd {
	
	private boolean useOldVersion;
	protected static final Log log = PlatformUtil.getLog(RoutePlannerFrontEnd.class);
	public boolean useSmartRouteRecalculation = true; 

	public RoutePlannerFrontEnd(boolean useOldVersion) {
		this.useOldVersion = useOldVersion;
	}
	
	public enum RouteCalculationMode {
		BASE,
		NORMAL,
		COMPLEX
	}
	
	public RoutingContext buildRoutingContext(RoutingConfiguration config, NativeLibrary nativeLibrary, BinaryMapIndexReader[] map, RouteCalculationMode rm) {
		return new RoutingContext(config, nativeLibrary, map, rm);
	}
	
	public RoutingContext buildRoutingContext(RoutingConfiguration config, NativeLibrary nativeLibrary, BinaryMapIndexReader[] map) {
		return new RoutingContext(config, nativeLibrary, map, RouteCalculationMode.NORMAL);
	}
	
	
	private static double squareDist(int x1, int y1, int x2, int y2) {
		// translate into meters 
		double dy = MapUtils.convert31YToMeters(y1, y2);
		double dx = MapUtils. convert31XToMeters(x1, x2);
		return dx * dx + dy * dy;
	}
	
	public RouteSegmentPoint findRouteSegment(double lat, double lon, RoutingContext ctx, List<RouteSegmentPoint> list) throws IOException {
		int px = MapUtils.get31TileNumberX(lon);
		int py = MapUtils.get31TileNumberY(lat);
		ArrayList<RouteDataObject> dataObjects = new ArrayList<RouteDataObject>();
		ctx.loadTileData(px, py, 17, dataObjects);
		if (dataObjects.isEmpty()) {
			ctx.loadTileData(px, py, 15, dataObjects);
		}
		if(list == null) {
			list = new ArrayList<BinaryRoutePlanner.RouteSegmentPoint>();
		}
		for (RouteDataObject r : dataObjects) {
			if (r.getPointsLength() > 1) {
				RouteSegmentPoint road = null;
				for (int j = 1; j < r.getPointsLength(); j++) {
					QuadPoint pr = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(j - 1), 
							r.getPoint31YTile(j - 1), r.getPoint31XTile(j ), r.getPoint31YTile(j ));
					double currentsDistSquare = squareDist((int) pr.x, (int)pr.y, px, py);
					if (road == null || currentsDistSquare < road.distSquare) {
						RouteDataObject ro = new RouteDataObject(r);
						road = new RouteSegmentPoint(ro, j, currentsDistSquare);
						road.preciseX = (int) pr.x;
						road.preciseY = (int) pr.y;
					}
				}
				if(road != null) {
					list.add(road);
				}
			}
		}
		Collections.sort(list, new Comparator<RouteSegmentPoint>() {

			@Override
			public int compare(RouteSegmentPoint o1, RouteSegmentPoint o2) {
				return Double.compare(o1.distSquare, o2.distSquare);
			}
		});
		if(list.size() > 0) {
			RouteSegmentPoint ps = list.get(0);
			ps.others = list;
			return ps;
		}
		return null;
	}
	
	
	public List<RouteSegmentResult> searchRoute(final RoutingContext ctx, LatLon start, LatLon end, List<LatLon> intermediates) throws IOException, InterruptedException {
		return searchRoute(ctx, start, end, intermediates, null);
	}
	
	public void setUseFastRecalculation(boolean use) {
		useSmartRouteRecalculation = use;
	}
			
	
	public List<RouteSegmentResult> searchRoute(final RoutingContext ctx, LatLon start, LatLon end, List<LatLon> intermediates, 
			PrecalculatedRouteDirection routeDirection) throws IOException, InterruptedException {
		if(ctx.calculationProgress == null) {
			ctx.calculationProgress = new RouteCalculationProgress();
		}
		boolean intermediatesEmpty = intermediates == null || intermediates.isEmpty();
		double maxDistance = MapUtils.getDistance(start, end);
		if(!intermediatesEmpty) {
			LatLon b = start;
			for(LatLon l : intermediates) {
				maxDistance = Math.max(MapUtils.getDistance(b, l), maxDistance);
				b = l;
			}
		}
		if(ctx.calculationMode == RouteCalculationMode.COMPLEX && routeDirection == null
				&& maxDistance > ctx.config.DEVIATION_RADIUS * 6) {
			RoutingContext nctx = buildRoutingContext(ctx.config, ctx.nativeLib, ctx.getMaps(), RouteCalculationMode.BASE);
			nctx.calculationProgress = ctx.calculationProgress ;
			List<RouteSegmentResult> ls = searchRoute(nctx, start, end, intermediates);
			routeDirection = PrecalculatedRouteDirection.build(ls, ctx.config.DEVIATION_RADIUS, ctx.getRouter().getMaxDefaultSpeed());
		}
		if(intermediatesEmpty && ctx.nativeLib != null) {
			ctx.startX = MapUtils.get31TileNumberX(start.getLongitude());
			ctx.startY = MapUtils.get31TileNumberY(start.getLatitude());
			ctx.targetX = MapUtils.get31TileNumberX(end.getLongitude());
			ctx.targetY = MapUtils.get31TileNumberY(end.getLatitude());
			RouteSegment recalculationEnd = getRecalculationEnd(ctx);
			if(recalculationEnd != null) {
				ctx.initTargetPoint(recalculationEnd);
			}
			if(routeDirection != null) {
				ctx.precalculatedRouteDirection = routeDirection.adopt(ctx);
			} 
			List<RouteSegmentResult> res = runNativeRouting(ctx, recalculationEnd);
			if(res != null) {
				new RouteResultPreparation().printResults(ctx, start, end, res);
			}
			makeStartEndPointsPrecise(res, start, end, intermediates);
			return res;	
		}
		int indexNotFound = 0;
		List<RouteSegmentPoint> points = new ArrayList<RouteSegmentPoint>();
		if(!addSegment(start, ctx, indexNotFound++, points)){
			return null;
		}
		if (intermediates != null) {
			for (LatLon l : intermediates) {
				if (!addSegment(l, ctx, indexNotFound++, points)) {
					return null;
				}
			}
		}
		if(!addSegment(end, ctx, indexNotFound++, points)){
			return null;
		}
		List<RouteSegmentResult> res = searchRoute(ctx, points, routeDirection);
		// make start and end more precise
		makeStartEndPointsPrecise(res, start, end, intermediates);
		if(res != null) {
			new RouteResultPreparation().printResults(ctx, start, end, res);
		}
		return res;
	}

	protected void makeStartEndPointsPrecise(List<RouteSegmentResult> res, LatLon start, LatLon end, List<LatLon> intermediates) {
		if (res.size() > 0) {
			updateResult(res.get(0), start, true);
			updateResult(res.get(res.size() - 1), end, false);
			if (intermediates != null) {
				int k = 1;
				for (int i = 0; i < intermediates.size(); i++) {
					LatLon ll = intermediates.get(i);
					int px = MapUtils.get31TileNumberX(ll.getLongitude());
					int py = MapUtils.get31TileNumberY(ll.getLatitude());
					for (; k < res.size(); k++) {
						double currentsDist = projectDistance(res, k, px, py);
						if (currentsDist < 500 * 500) {
							for (int k1 = k + 1; k1 < res.size(); k1++) {
								double c2 = projectDistance(res, k1, px, py);
								if (c2 < currentsDist) {
									k = k1;
									currentsDist = c2;
								} else if (k1 - k > 15) {
									break;
								}
							}
							updateResult(res.get(k), ll, false);
							if (k < res.size() - 1) {
								updateResult(res.get(k + 1), ll, true);
							}
							break;
						}
					}
				}
			}
		}
	}

	protected double projectDistance(List<RouteSegmentResult> res, int k, int px, int py) {
		RouteSegmentResult sr = res.get(k);
		RouteDataObject r = sr.getObject();
		QuadPoint pp = MapUtils.getProjectionPoint31(px, py, 
				r.getPoint31XTile(sr.getStartPointIndex()), r.getPoint31YTile(sr.getStartPointIndex()),
				r.getPoint31XTile(sr.getEndPointIndex()), r.getPoint31YTile(sr.getEndPointIndex()));
		double currentsDist = squareDist((int) pp.x, (int)pp.y, px, py);
		return currentsDist;
	}
	
	private void updateResult(RouteSegmentResult routeSegmentResult, LatLon point, boolean st) {
		int px = MapUtils.get31TileNumberX(point.getLongitude());
		int py = MapUtils.get31TileNumberY(point.getLatitude());
		int pind = st ? routeSegmentResult.getStartPointIndex()  : routeSegmentResult.getEndPointIndex();
		
		RouteDataObject r = routeSegmentResult.getObject();
		QuadPoint before = null;
		QuadPoint after = null;
		if(pind > 0) {
			before = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(pind - 1), 
					r.getPoint31YTile(pind - 1), r.getPoint31XTile(pind ), r.getPoint31YTile(pind ));
		}
		if(pind < r.getPointsLength() - 1) {
			after = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(pind + 1), 
					r.getPoint31YTile(pind + 1), r.getPoint31XTile(pind ), r.getPoint31YTile(pind ));
		}
		int insert = 0;
		double dd = MapUtils.getDistance(point, MapUtils.get31LatitudeY(r.getPoint31YTile(pind)),
				MapUtils.get31LongitudeX(r.getPoint31XTile(pind)));
		double ddBefore = Double.POSITIVE_INFINITY;
		double ddAfter = Double.POSITIVE_INFINITY;
		QuadPoint i = null;
		if(before != null) {
			ddBefore = MapUtils.getDistance(point, MapUtils.get31LatitudeY((int) before.y),
					MapUtils.get31LongitudeX((int) before.x));
			if(ddBefore < dd) {
				insert = -1;
				i = before;
			}
		}
		
		if(after != null) {
			ddAfter = MapUtils.getDistance(point, MapUtils.get31LatitudeY((int) after.y),
					MapUtils.get31LongitudeX((int) after.x));
			if(ddAfter < dd && ddAfter < ddBefore) {
				insert = 1;
				i = after;
			}
		}		
		
		if (insert != 0) {
			if (st && routeSegmentResult.getStartPointIndex() < routeSegmentResult.getEndPointIndex()) {
				routeSegmentResult.setEndPointIndex(routeSegmentResult.getEndPointIndex() + 1);
			}
			if (!st && routeSegmentResult.getStartPointIndex() > routeSegmentResult.getEndPointIndex()) {
				routeSegmentResult.setStartPointIndex(routeSegmentResult.getStartPointIndex() + 1);
			}
			if (insert > 0) {
				r.insert(pind + 1, (int) i.x, (int) i.y);
				if (st) {
					routeSegmentResult.setStartPointIndex(routeSegmentResult.getStartPointIndex() + 1);
				}	
				if (!st) {
					routeSegmentResult.setEndPointIndex(routeSegmentResult.getEndPointIndex() + 1);
				}
			} else {
				r.insert(pind, (int) i.x, (int) i.y);
			}
			
		}
		
	}

	private boolean addSegment(LatLon s, RoutingContext ctx, int indexNotFound, List<RouteSegmentPoint> res) throws IOException {
		RouteSegmentPoint f = findRouteSegment(s.getLatitude(), s.getLongitude(), ctx, null);
		if(f == null){
			ctx.calculationProgress.segmentNotFound = indexNotFound;
			return false;
		} else {
			log.info("Route segment found " + f.getRoad().id + " " + f.getRoad().getName());
			res.add(f);
			return true;
		}
		
	}
	
	private List<RouteSegmentResult> searchRouteInternalPrepare(final RoutingContext ctx, RouteSegmentPoint start, RouteSegmentPoint end, 
			PrecalculatedRouteDirection routeDirection) throws IOException, InterruptedException {
		RouteSegment recalculationEnd = getRecalculationEnd(ctx);
		if(recalculationEnd != null) {
			ctx.initStartAndTargetPoints(start, recalculationEnd);
		} else {
			ctx.initStartAndTargetPoints(start, end);
		}
		if(routeDirection != null) {
			ctx.precalculatedRouteDirection = routeDirection.adopt(ctx);
		}
		if (ctx.nativeLib != null) {
			return runNativeRouting(ctx, recalculationEnd);
		} else {
			refreshProgressDistance(ctx);
			// Split into 2 methods to let GC work in between
			if(useOldVersion) {
				new BinaryRoutePlannerOld().searchRouteInternal(ctx, start, end);
			} else {
				ctx.finalRouteSegment = new BinaryRoutePlanner().searchRouteInternal(ctx, start, end, recalculationEnd);
			}
			// 4. Route is found : collect all segments and prepare result
			return new RouteResultPreparation().prepareResult(ctx, ctx.finalRouteSegment);
		}
	}
	
	public RouteSegment getRecalculationEnd(final RoutingContext ctx) {
		RouteSegment recalculationEnd = null;
		boolean runRecalculation = ctx.previouslyCalculatedRoute != null && ctx.previouslyCalculatedRoute.size() > 0
				&& ctx.config.recalculateDistance != 0;
		if (runRecalculation) {
			List<RouteSegmentResult> rlist = new ArrayList<RouteSegmentResult>();
			float distanceThreshold = ctx.config.recalculateDistance;
			float threshold = 0;
			for (RouteSegmentResult rr : ctx.previouslyCalculatedRoute) {
				threshold += rr.getDistance();
				if (threshold > distanceThreshold) {
					rlist.add(rr);
				}
			}
			runRecalculation = rlist.size() > 0;
			if (rlist.size() > 0) {
				RouteSegment previous = null;
				for (int i = 0; i <= rlist.size() - 1; i++) {
					RouteSegmentResult rr = rlist.get(i);
					RouteSegment segment = new RouteSegment(rr.getObject(), rr.getEndPointIndex());
					if (previous != null) {
						previous.setParentRoute(segment);
						previous.setParentSegmentEnd(rr.getStartPointIndex());
					} else {
						recalculationEnd = segment;
					}
					previous = segment;
				}
			}
		}
		return recalculationEnd;
	}


	private void refreshProgressDistance(RoutingContext ctx) {
		if(ctx.calculationProgress != null) {
			ctx.calculationProgress.distanceFromBegin = 0;
			ctx.calculationProgress.distanceFromEnd = 0;
			ctx.calculationProgress.reverseSegmentQueueSize = 0;
			ctx.calculationProgress.directSegmentQueueSize = 0;
			float rd = (float) MapUtils.squareRootDist31(ctx.startX, ctx.startY, ctx.targetX, ctx.targetY);
			float speed = 0.9f * ctx.config.router.getMaxDefaultSpeed();
			ctx.calculationProgress.totalEstimatedDistance = (float) (rd /  speed); 
		}
		
	}

	private List<RouteSegmentResult> runNativeRouting(final RoutingContext ctx, RouteSegment recalculationEnd) throws IOException {
		refreshProgressDistance(ctx);
		RouteRegion[] regions = ctx.reverseMap.keySet().toArray(new BinaryMapRouteReaderAdapter.RouteRegion[ctx.reverseMap.size()]);
		ctx.checkOldRoutingFiles(ctx.startX, ctx.startY);
		ctx.checkOldRoutingFiles(ctx.targetX, ctx.targetY);
		
		long time = System.currentTimeMillis();
		RouteSegmentResult[] res = ctx.nativeLib.runNativeRouting(ctx.startX, ctx.startY, ctx.targetX, ctx.targetY,
				ctx.config, regions, ctx.calculationProgress, ctx.precalculatedRouteDirection, ctx.calculationMode == RouteCalculationMode.BASE);
		log.info("Native routing took " + (System.currentTimeMillis() - time) / 1000f + " seconds");
		ArrayList<RouteSegmentResult> result = new ArrayList<RouteSegmentResult>(Arrays.asList(res));
		if(recalculationEnd != null) {
			log.info("Native routing use precalculated route");
			RouteSegment current = recalculationEnd;
			while(current.getParentRoute() != null) {
				RouteSegment pr = current.getParentRoute();
				result.add(new RouteSegmentResult(pr.getRoad(), current.getParentSegmentEnd(), pr.getSegmentStart()));
				current = pr;
			}
		}
		ctx.routingTime = ctx.calculationProgress.routingCalculatedTime;
		ctx.visitedSegments = ctx.calculationProgress.visitedSegments;
		ctx.loadedTiles = ctx.calculationProgress.loadedTiles;
		return new RouteResultPreparation().prepareResult(ctx, result);
	}
	

	private List<RouteSegmentResult> searchRoute(final RoutingContext ctx, List<RouteSegmentPoint> points, PrecalculatedRouteDirection routeDirection) 
			throws IOException, InterruptedException {
		if (points.size() <= 2) {
			if(!useSmartRouteRecalculation) {
				ctx.previouslyCalculatedRoute = null;
			}
			return searchRoute(ctx, points.get(0), points.get(1), routeDirection);
		}

		ArrayList<RouteSegmentResult> firstPartRecalculatedRoute = null;
		ArrayList<RouteSegmentResult> restPartRecalculatedRoute = null;
		if (ctx.previouslyCalculatedRoute != null) {
			List<RouteSegmentResult> prev = ctx.previouslyCalculatedRoute;
			long id = points.get(1).getRoad().id;
			int ss = points.get(1).getSegmentStart();
			int px = points.get(1).getRoad().getPoint31XTile(ss);
			int py = points.get(1).getRoad().getPoint31YTile(ss);
			for (int i = 0; i < prev.size(); i++) {
				RouteSegmentResult rsr = prev.get(i);
				if (id == rsr.getObject().getId()) {
					if (MapUtils.getDistance(rsr.getPoint(rsr.getEndPointIndex()), MapUtils.get31LatitudeY(py),
							MapUtils.get31LongitudeX(px)) < 50) {
						firstPartRecalculatedRoute = new ArrayList<RouteSegmentResult>(i + 1);
						restPartRecalculatedRoute = new ArrayList<RouteSegmentResult>(prev.size() - i);
						for (int k = 0; k < prev.size(); k++) {
							if (k <= i) {
								firstPartRecalculatedRoute.add(prev.get(k));
							} else {
								restPartRecalculatedRoute.add(prev.get(k));
							}
						}
						System.out.println("Recalculate only first part of the route");
						break;
					}
				}
			}
		}
		List<RouteSegmentResult> results = new ArrayList<RouteSegmentResult>();
		for (int i = 0; i < points.size() - 1; i++) {
			RoutingContext local = new RoutingContext(ctx);
			if (i == 0) {
				if (useSmartRouteRecalculation) {
					local.previouslyCalculatedRoute = firstPartRecalculatedRoute;
				}
			}
			local.visitor = ctx.visitor;
			local.calculationProgress = ctx.calculationProgress;
			List<RouteSegmentResult> res = searchRouteInternalPrepare(local, points.get(i), points.get(i + 1), routeDirection);

			results.addAll(res);
			ctx.distinctLoadedTiles += local.distinctLoadedTiles;
			ctx.loadedTiles += local.loadedTiles;
			ctx.visitedSegments += local.visitedSegments;
			ctx.loadedPrevUnloadedTiles += local.loadedPrevUnloadedTiles;
			ctx.timeToCalculate += local.timeToCalculate;
			ctx.timeToLoad += local.timeToLoad;
			ctx.timeToLoadHeaders += local.timeToLoadHeaders;
			ctx.relaxedSegments += local.relaxedSegments;
			ctx.routingTime += local.routingTime;

			local.unloadAllData(ctx);
			if (restPartRecalculatedRoute != null) {
				results.addAll(restPartRecalculatedRoute);
				break;
			}
		}
		ctx.unloadAllData();
		return results;

	}
	
	@SuppressWarnings("static-access")
	private List<RouteSegmentResult> searchRoute(final RoutingContext ctx, RouteSegmentPoint start, RouteSegmentPoint end, 
			PrecalculatedRouteDirection routeDirection) throws IOException, InterruptedException {
		if(ctx.SHOW_GC_SIZE){
			long h1 = ctx.runGCUsedMemory();
			float mb = (1 << 20);
			log.warn("Used before routing " + h1 / mb+ " actual");
		}
		List<RouteSegmentResult> result = searchRouteInternalPrepare(ctx, start, end, routeDirection);
		if (RoutingContext.SHOW_GC_SIZE) {
			int sz = ctx.global.size;
			log.warn("Subregion size " + ctx.subregionTiles.size() + " " + " tiles " + ctx.indexedSubregions.size());
			ctx.runGCUsedMemory();
			long h1 = ctx.runGCUsedMemory();
			ctx.unloadAllData();
			ctx.runGCUsedMemory();
			long h2 = ctx.runGCUsedMemory();
			float mb = (1 << 20);
			log.warn("Unload context :  estimated " + sz / mb + " ?= " + (h1 - h2) / mb + " actual");
		}
		return result;
	}
	
	

}
