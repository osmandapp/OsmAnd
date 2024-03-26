package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.osmand.ResultMatcher;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxRouteApproximation;
import net.osmand.util.MapUtils;

// TODO null finalPoints.routeToTarget
// TODO Native lib
// TODO attached roads
// TODO Use minPointApproximation
public class GpxSegmentsApproximation {

	private boolean initRoutingPoint(RoutePlannerFrontEnd frontEnd, GpxRouteApproximation gctx, GpxPoint start, double distThreshold) throws IOException {
		if (start != null && start.pnt == null) {
			gctx.routePointsSearched++;
			RouteSegmentPoint rsp = frontEnd.findRouteSegment(start.loc.getLatitude(), start.loc.getLongitude(), gctx.ctx, null, false);
			if (rsp != null) {
				if (MapUtils.getDistance(rsp.getPreciseLatLon(), start.loc) < distThreshold) {
					start.pnt = rsp;
				}
			}
		}
		if (start != null && start.pnt != null) {
			return true;
		}
		return false;
	}
	
	private static int LOOKUP_AHEAD = 10;
	public GpxRouteApproximation searchGpxApproximation(RoutePlannerFrontEnd frontEnd, GpxRouteApproximation gctx, List<GpxPoint> gpxPoints,
			ResultMatcher<GpxRouteApproximation> resultMatcher) throws IOException {

		long timeToCalculate = System.nanoTime();
//		NativeLibrary nativeLib = gctx.ctx.nativeLib;
//		if (nativeLib != null && useNativeApproximation) {
//			gctx = nativeLib.runNativeSearchGpxRoute(gctx, gpxPoints);
//		}
		float minPointApproximation = gctx.ctx.config.minPointApproximation;
		GpxPoint nextPoint = null;
		for(GpxPoint p : gpxPoints) {
			p.x31 = MapUtils.get31TileNumberX(p.loc.getLongitude());
			p.y31 = MapUtils.get31TileNumberY(p.loc.getLatitude());
		}
		for (int i = 0; i < gpxPoints.size(); i++) {
			if (initRoutingPoint(frontEnd, gctx, gpxPoints.get(i), minPointApproximation)) {
				nextPoint = gpxPoints.get(i);
				break;
			}
		}
		gctx.finalPoints.add(nextPoint);
		while (nextPoint != null && nextPoint.pnt != null) {
			double mindistglobal = 0;
			RouteSegmentResult fres = null;
			int minInd = -1;
			for (int j = nextPoint.ind + 1; j < Math.min(nextPoint.ind + LOOKUP_AHEAD, gpxPoints.size()); j++) {
				RouteSegmentResult[] res = new RouteSegmentResult[1];
				double mindistsqr = Double.POSITIVE_INFINITY;
				GpxPoint ps = gpxPoints.get(j);
				mindistsqr = distance(res, mindistsqr, nextPoint.pnt, ps);
				if (nextPoint.pnt.others != null) {
					for (RouteSegmentPoint oth : nextPoint.pnt.others) {
						mindistsqr = distance(res, mindistsqr, oth, ps);
					}
				}
				if (fres == null || mindistsqr <= mindistglobal) {
					fres = res[0];
					mindistglobal = mindistsqr;
					minInd = j;
				}
			}
			if (minInd < 0) {
				break;
			}
			nextPoint.routeToTarget = new ArrayList<RouteSegmentResult>();
			nextPoint.routeToTarget.add(fres);
			nextPoint.targetInd = minInd;
			nextPoint = gpxPoints.get(minInd);
			RouteSegment sg = gctx.ctx.loadRouteSegment(fres.getEndPointX(), fres.getEndPointY(), gctx.ctx.config.memoryLimitation);
			while (sg != null) {
				if (sg.getRoad().getId() != fres.getObject().getId() || sg.getSegmentEnd() != fres.getEndPointIndex()) {
					RouteSegmentPoint p = new RouteSegmentPoint(sg.getRoad(), sg.getSegmentStart(), sg.getSegmentEnd(),
							0);
					if (nextPoint.pnt == null) {
						nextPoint.pnt = p;
					} else {
						if (nextPoint.pnt.others == null) {
							nextPoint.pnt.others = new ArrayList<>();
						}
						nextPoint.pnt.others.add(p);
					}
				}
				sg = sg.getNext();
			}
			
			gctx.finalPoints.add(nextPoint);
		}
		if (gctx.ctx.calculationProgress != null) {
			gctx.ctx.calculationProgress.timeToCalculate = System.nanoTime() - timeToCalculate;
		}
		System.out.printf("Approximation took %.2f seconds\n", (System.nanoTime() - timeToCalculate) / 1.0e9);
		return gctx;
		
	}
	
	private double distance(RouteSegmentResult[] res, double mindistsqr, RouteSegmentPoint pnt, GpxPoint loc) {
		int segmentEnd = -1;
		double dist = 0;
		for (int i = Math.max(0, pnt.getSegmentStart() - LOOKUP_AHEAD); i < Math.min(pnt.getRoad().getPointsLength(),
				pnt.getSegmentStart() + LOOKUP_AHEAD); i++) {
			if(i == pnt.getSegmentStart()) {
				continue;
			}
			double d = MapUtils.squareDist31TileMetric(loc.x31, loc.y31, pnt.getRoad().getPoint31XTile(i), pnt.getRoad().getPoint31YTile(i));
			if (segmentEnd < 0 || d < dist) {
				segmentEnd = i;
				dist = d;
			}
		}
		dist += pnt.distToProj;
		if ((res[0] == null || dist < mindistsqr) && segmentEnd >= 0) {
			mindistsqr = dist;
			res[0] = new RouteSegmentResult(pnt.getRoad(), pnt.getSegmentStart(), segmentEnd);
		}
		return mindistsqr;
	}

}
