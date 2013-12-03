package net.osmand.router;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

public class RoutePlannerFrontEnd {
	
	private boolean useOldVersion;
	protected static final Log log = PlatformUtil.getLog(RoutePlannerFrontEnd.class);

	public RoutePlannerFrontEnd(boolean useOldVersion) {
		this.useOldVersion = useOldVersion;
	}
	
	
	private static double squareDist(int x1, int y1, int x2, int y2) {
		// translate into meters 
		double dy = MapUtils.convert31YToMeters(y1, y2);
		double dx = MapUtils. convert31XToMeters(x1, x2);
		return dx * dx + dy * dy;
	}
	
	public RouteSegment findRouteSegment(double lat, double lon, RoutingContext ctx) throws IOException {
		int px = MapUtils.get31TileNumberX(lon);
		int py = MapUtils.get31TileNumberY(lat);
		ArrayList<RouteDataObject> dataObjects = new ArrayList<RouteDataObject>();
		ctx.loadTileData(px, py, 17, dataObjects);
		if (dataObjects.isEmpty()) {
			ctx.loadTileData(px, py, 15, dataObjects);
		}
		// Candidate
		RouteDataObject road = null;
		int index = -1;
		int candidateX = -1;
		int candidateY = -1;
		double sdist = 0;

		for (RouteDataObject r : dataObjects) {
			if (r.getPointsLength() > 1) {
				for (int j = 1; j < r.getPointsLength(); j++) {
					QuadPoint pr = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(j-1), 
							r.getPoint31YTile(j-1), r.getPoint31XTile(j), r.getPoint31YTile(j));
					double currentsDist = squareDist((int)pr.x, (int)pr.y, px, py);
					if (road == null || currentsDist < sdist) {
						// New candidate
						road = r;
						index = j;
						candidateX = (int)pr.x;
						candidateY = (int)pr.y;
						sdist = currentsDist;
					}
				}
			}
		}
		if (road != null) {
			if ((candidateX == road.pointsX[index-1]) && (candidateY == road.pointsY[index-1]))
			{
				// Projection has same coordinates. None new.
				return new RouteSegment(road, index-1);
			}
			if ((candidateX == road.pointsX[index]) && (candidateY == road.pointsY[index]))
			{
				// Projection has same coordinates. None new.
				return new RouteSegment(road, index);
			}
			// Add projection to a new version of the road.
			RouteDataObject proj = new RouteDataObject(road);
			proj.insert(index, candidateX, candidateY);
			// re-register the best road because one more point was inserted
			ctx.registerRouteDataObject(proj);
			return new RouteSegment(proj, index);
		}
		return null;
	}
	
	public List<RouteSegmentResult> searchRoute(final RoutingContext ctx, LatLon start, LatLon end, List<LatLon> intermediates, boolean leftSideNavigation) throws IOException, InterruptedException {
		if(ctx.calculationProgress == null) {
			ctx.calculationProgress = new RouteCalculationProgress();
		}
		if((intermediates == null || intermediates.isEmpty()) && useOldVersion && ctx.nativeLib != null) {
			ctx.startX = MapUtils.get31TileNumberX(start.getLongitude());
			ctx.startY = MapUtils.get31TileNumberY(start.getLatitude());
			ctx.targetX = MapUtils.get31TileNumberX(end.getLongitude());
			ctx.targetY = MapUtils.get31TileNumberY(end.getLatitude());
			List<RouteSegmentResult> res = runNativeRouting(ctx, leftSideNavigation);
			if(res != null) {
				new RouteResultPreparation().printResults(ctx, start, end, res);
			}
			return res;
		}
		int indexNotFound = 0;
		List<RouteSegment> points = new ArrayList<BinaryRoutePlanner.RouteSegment>();
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
		List<RouteSegmentResult> res = searchRoute(ctx, points, leftSideNavigation);
		if(res != null) {
			new RouteResultPreparation().printResults(ctx, start, end, res);
		}
		return res;
	}
	
	private boolean addSegment(LatLon s, RoutingContext ctx, int indexNotFound, List<RouteSegment> res) throws IOException {
		RouteSegment f = findRouteSegment(s.getLatitude(), s.getLongitude(), ctx);
		if(f == null){
			ctx.calculationProgress.segmentNotFound = indexNotFound;
			return false;
		} else {
			log.info("Route segment found " + f.getRoad().id + " " + f.getRoad().getName());
			res.add(f);
			return true;
		}
		
	}
	
	private List<RouteSegmentResult> searchRouteInternalPrepare(final RoutingContext ctx, RouteSegment start, RouteSegment end, boolean leftSideNavigation) throws IOException, InterruptedException {
		ctx.targetX = end.road.getPoint31XTile(end.getSegmentStart());
		ctx.targetY = end.road.getPoint31YTile(end.getSegmentStart());
		ctx.startX = start.road.getPoint31XTile(start.getSegmentStart());
		ctx.startY = start.road.getPoint31YTile(start.getSegmentStart());
		if (ctx.nativeLib != null && useOldVersion) {
			return runNativeRouting(ctx, leftSideNavigation);
		} else {
			refreshProgressDistance(ctx);
			// Split into 2 methods to let GC work in between
			if(useOldVersion) {
				new BinaryRoutePlannerOld().searchRouteInternal(ctx, start, end);
			} else {
				ctx.finalRouteSegment =  new BinaryRoutePlanner().searchRouteInternal(ctx, start, end);
			}
			// 4. Route is found : collect all segments and prepare result
			return new RouteResultPreparation().prepareResult(ctx, ctx.finalRouteSegment, leftSideNavigation);
		}
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

	private List<RouteSegmentResult> runNativeRouting(final RoutingContext ctx, boolean leftSideNavigation) throws IOException {
		refreshProgressDistance(ctx);
		RouteRegion[] regions = ctx.reverseMap.keySet().toArray(new BinaryMapRouteReaderAdapter.RouteRegion[ctx.reverseMap.size()]);
		RouteSegmentResult[] res = ctx.nativeLib.runNativeRouting(ctx.startX, ctx.startY, ctx.targetX, ctx.targetY,
				ctx.config, regions, ctx.calculationProgress);
		ArrayList<RouteSegmentResult> result = new ArrayList<RouteSegmentResult>(Arrays.asList(res));
		ctx.routingTime = ctx.calculationProgress.routingCalculatedTime;
		ctx.visitedSegments = ctx.calculationProgress.visitedSegments;
		ctx.loadedTiles = ctx.calculationProgress.loadedTiles;
		return new RouteResultPreparation().prepareResult(ctx, leftSideNavigation, result);
	}
	

	private List<RouteSegmentResult> searchRoute(final RoutingContext ctx, List<RouteSegment> points, boolean leftSideNavigation) throws IOException, InterruptedException {
		if(points.size() > 2) {
			ArrayList<RouteSegmentResult> firstPartRecalculatedRoute = null;
			ArrayList<RouteSegmentResult> restPartRecalculatedRoute = null;
			if (ctx.previouslyCalculatedRoute != null) {
				List<RouteSegmentResult> prev = ctx.previouslyCalculatedRoute;
				long id = points.get(1).getRoad().id;
				int ss = points.get(1).getSegmentStart();
				for (int i = 0; i < prev.size(); i++) {
					RouteSegmentResult rsr = prev.get(i);
					if (id == rsr.getObject().getId() && ss == rsr.getEndPointIndex()) {
						firstPartRecalculatedRoute = new ArrayList<RouteSegmentResult>(i + 1);
						restPartRecalculatedRoute = new ArrayList<RouteSegmentResult>(prev.size() - i);
						for(int k = 0; k < prev.size(); k++) {
							if(k <= i) {
								firstPartRecalculatedRoute.add(prev.get(k));
							} else {
								restPartRecalculatedRoute.add(prev.get(k));
							}
						}
						break;
					}
				}
			}
			List<RouteSegmentResult> results = new ArrayList<RouteSegmentResult>();
			for (int i = 0; i < points.size() - 1; i++) {
				RoutingContext local = new RoutingContext(ctx);
				if(i == 0) {
					local.previouslyCalculatedRoute = firstPartRecalculatedRoute;
				}
				local.visitor = ctx.visitor;
				local.calculationProgress = ctx.calculationProgress;
				List<RouteSegmentResult> res = searchRouteInternalPrepare(local, points.get(i), points.get(i + 1), leftSideNavigation);

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
				if(restPartRecalculatedRoute != null) {
					results.addAll(restPartRecalculatedRoute);
					break;
				}
			}
			ctx.unloadAllData();
			return results;
		}
		return searchRoute(ctx, points.get(0), points.get(1), leftSideNavigation);
	}
	
	@SuppressWarnings("static-access")
	private List<RouteSegmentResult> searchRoute(final RoutingContext ctx, RouteSegment start, RouteSegment end, boolean leftSideNavigation) throws IOException, InterruptedException {
		if(ctx.SHOW_GC_SIZE){
			long h1 = ctx.runGCUsedMemory();
			float mb = (1 << 20);
			log.warn("Used before routing " + h1 / mb+ " actual");
		}
		List<RouteSegmentResult> result = searchRouteInternalPrepare(ctx, start, end, leftSideNavigation);
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
