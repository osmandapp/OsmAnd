package net.osmand.router.transport;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportSchedule;
import net.osmand.data.TransportStop;
import net.osmand.data.TransportStopExit;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.router.DistanceProvider;
import net.osmand.router.IDistanceProvider;
import net.osmand.router.NativeTransportRoute;
import net.osmand.router.NativeTransportRouteResultSegment;
import net.osmand.router.NativeTransportRoutingResult;
import net.osmand.router.NativeTransportStop;
import net.osmand.router.RouteCalculationProgress;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

public class TransportRoutePlanner {

	public static final int MIN_DIST_STOP_TO_GEOMETRY = 150;
	private static final boolean MEASURE_TIME = false;
	
	public static final long GEOMETRY_WAY_ID = -1;
	public static final long STOPS_WAY_ID = -2;
	private final static Log LOG = PlatformUtil.getLog(TransportRoutePlanner.class);
	private final IDistanceProvider distanceProvider;

	public TransportRoutePlanner(IDistanceProvider distanceProvider) {
		this.distanceProvider = distanceProvider;
	}

	public TransportRoutePlanner() {
		this.distanceProvider = new DistanceProvider();
	}

	public List<TransportRouteResult> buildRoute(
			ITransportRoutingContext ctx,
			LatLon start,
			LatLon end
	) throws IOException, InterruptedException {
		ctx.setStartCalcTime(System.currentTimeMillis());
		double totalDirectDistance = distanceProvider.getDistance(start, end);
		List<TransportRouteSegment> startStops = ctx.getTransportStops(start);
		List<TransportRouteSegment> endStops = ctx.getTransportStops(end);

		TLongObjectHashMap<TransportRouteSegment> endSegments = new TLongObjectHashMap<>();
		for (TransportRouteSegment s : endStops) {
			endSegments.put(s.getId(), s);
		}
		if (startStops.isEmpty()) {
			LOG.info("Public transport. Start stop is empty");
			return Collections.emptyList();
		}

		var cfg = ctx.getCfg();

		PriorityQueue<TransportRouteSegment> queue = new PriorityQueue<>(startStops.size(), new SegmentsComparator());
		for (TransportRouteSegment r : startStops) {
			r.walkDist = (float) distanceProvider.getDistance(r.getLocation(), start);
			r.totalTravelTime = r.walkDist / cfg.getWalkSpeed();
			queue.add(r);
		}
		
		double finishTime = cfg.getMaxRouteTime();
		ctx.setFinishTimeSeconds(cfg.getFinishTimeSeconds());
		if (totalDirectDistance > cfg.getMaxRouteDistance() && cfg.getMaxRouteIncreaseSpeed() > 0)  {
			int increaseTime = (int) ((totalDirectDistance - cfg.getMaxRouteDistance())
					* 3.6 / cfg.getMaxRouteIncreaseSpeed());
			finishTime += increaseTime;
			ctx.setFinishTimeSeconds(ctx.getFinishTimeSeconds() + increaseTime / 6);
		}
		double maxTravelTimeCmpToWalk = totalDirectDistance / cfg.getWalkSpeed() - (double) cfg.getChangeTime() / 2;
		var results = new ArrayList<TransportRouteSegment>();
		RouteCalculationProgress calcProgress;
		initProgressBar(ctx, start, end);
		while (!queue.isEmpty()) {
			long beginMs = MEASURE_TIME ? System.currentTimeMillis() : 0;
			calcProgress = ctx.getCalculationProgress();
			if (calcProgress != null && calcProgress.isCancelled) {
				return null;
			}
			TransportRouteSegment segment = queue.poll();
			TransportRouteSegment ex = ctx.getVisitedSegments().get(segment.getId());
			if (ex != null) {
				if (ex.totalTravelTime > segment.totalTravelTime) {
					System.err.printf(
						Locale.US,
						"%.1f (%s) > %.1f (%s)%n",
						ex.totalTravelTime,
						ex,
						segment.totalTravelTime,
						segment
					);
				}
				continue;
			}
			ctx.setVisitedRoutesCount(ctx.getVisitedSegments().size() + 1);
			ctx.getVisitedSegments().put(segment.getId(), segment);
			
			if (
				segment.totalTravelTime > finishTime + ctx.getFinishTimeSeconds() ||
				segment.totalTravelTime > maxTravelTimeCmpToWalk
			) {
				break;
			}
			long segmentId = segment.getId();
			TransportRouteSegment finish = null;
			double minDist = 0;
			double travelDist = 0;
			double travelTime = 0;
			final float routeTravelSpeed = cfg.getSpeedByRouteType(
					segment.getRoute().getType()
			);
			if (routeTravelSpeed == 0) {
				continue;
			}
			TransportStop prevStop = segment.getStop(segment.segStart);
			List<TransportRouteSegment> sgms = new ArrayList<TransportRouteSegment>();
			for (int ind = 1 + segment.segStart; ind < segment.getLength(); ind++) {
				calcProgress = ctx.getCalculationProgress();
				if (calcProgress != null && calcProgress.isCancelled) {
					return null;
				}
				segmentId ++;
				ctx.getVisitedSegments().put(segmentId, segment);
				TransportStop stop = segment.getStop(ind);

				// could be geometry size
				double segmentDist = distanceProvider.getDistance(
					prevStop.getLocation(),
					stop.getLocation()
				);
				travelDist += segmentDist;
				if (cfg.getUseSchedule()) {
					TransportSchedule sc = segment.road.getSchedule();
					int interval = sc.avgStopIntervals.get(ind - 1);
					travelTime += interval * 10;
				} else {
					travelTime += cfg.getStopTime() + segmentDist / routeTravelSpeed;
				}
				if (segment.totalTravelTime + travelTime > finishTime + ctx.getFinishTimeSeconds()) {
					break;
				}
				sgms.clear();
				if (segment.getDepth() < cfg.getMaxNumberOfChanges() + 1) {
					sgms = ctx.getTransportStops(stop.x31, stop.y31, true, sgms);
					ctx.setVisitedStops(ctx.getVisitedSegments().size() + 1);
					for (TransportRouteSegment sgm : sgms) {
						if (ctx.getCalculationProgress() != null && ctx.getCalculationProgress().isCancelled) {
							return null;
						}
						if (segment.wasVisited(sgm)) {
							continue;
						}
						if (ctx.getVisitedSegments().containsKey(sgm.getId())) {
							continue;
						}
						TransportRouteSegment nextSegment = new TransportRouteSegment(sgm);
						nextSegment.parentRoute = segment;
						nextSegment.parentStop = ind;
						nextSegment.walkDist = distanceProvider.getDistance(nextSegment.getLocation(), stop.getLocation());
						nextSegment.parentTravelTime = travelTime;
						nextSegment.parentTravelDist = travelDist;
						double walkTime =
								nextSegment.walkDist / cfg.getWalkSpeed()
								+ cfg.getChangeTime()
								+ cfg.getBoardingTime();
						nextSegment.totalTravelTime = segment.totalTravelTime + travelTime + walkTime;
						if (cfg.getUseSchedule()) {
							int tm = (sgm.departureTime - cfg.getScheduleTimeOfDay()) * 10;
							if (tm >= nextSegment.totalTravelTime) {
								nextSegment.totalTravelTime = tm;
								queue.add(nextSegment);
							}
						} else {
							queue.add(nextSegment);
						}
					}
				}
				TransportRouteSegment finalSegment = endSegments.get(segmentId);
				double distToEnd = distanceProvider.getDistance(stop.getLocation(), end);
				if (finalSegment != null && distToEnd < cfg.getWalkRadius()) {
					if (finish == null || minDist > distToEnd) {
						minDist = distToEnd;
						finish = new TransportRouteSegment(finalSegment);
						finish.parentRoute = segment;
						finish.parentStop = ind;
						finish.walkDist = distToEnd;
						finish.parentTravelTime = travelTime;
						finish.parentTravelDist = travelDist;

						double walkTime = distToEnd / cfg.getWalkSpeed();
						finish.totalTravelTime = segment.totalTravelTime + travelTime + walkTime;

					}
				}
				prevStop = stop;
			}
			if (finish != null) {
				if (finishTime > finish.totalTravelTime) {
					finishTime = finish.totalTravelTime;
				}
				if (finish.totalTravelTime < finishTime + ctx.getFinishTimeSeconds() &&
						(finish.totalTravelTime < maxTravelTimeCmpToWalk || results.isEmpty())) {
					results.add(finish);
				}
			}

			calcProgress = ctx.getCalculationProgress();
			if (calcProgress != null && calcProgress.isCancelled) {
				throw new InterruptedException("Route calculation interrupted");
			}
			if (MEASURE_TIME) {
				long time = System.currentTimeMillis() - beginMs;
				if (time > 10) {
					System.out.printf(
						Locale.US,
						"%d ms ref - %s id - %d%n",
						time,
						segment.road.getRef(),
						segment.road.getId()
					);
				}
			}
			updateCalculationProgress(ctx.getCalculationProgress(), queue);
			
		}
		return prepareResults(ctx, results);
	}
	
	private void initProgressBar(ITransportRoutingContext ctx, LatLon start, LatLon end) {
		if (ctx.getCalculationProgress() != null) {
			ctx.getCalculationProgress().distanceFromEnd = 0;
			ctx.getCalculationProgress().reverseSegmentQueueSize = 0;
			ctx.getCalculationProgress().directSegmentQueueSize = 0;
			float speed = ctx.getCfg().getDefaultTravelSpeed() + 1; // assume
			ctx.getCalculationProgress().totalEstimatedDistance = (float) (distanceProvider.getDistance(start, end) / speed);
		}
	}

	private void updateCalculationProgress(RouteCalculationProgress calcProgress, PriorityQueue<TransportRouteSegment> queue) {
		if (calcProgress != null) {
			calcProgress.directSegmentQueueSize = queue.size();
			if (!queue.isEmpty()) {
				TransportRouteSegment peek = queue.peek();
				calcProgress.distanceFromBegin = (float) Math.max(
						peek.totalTravelTime,
						calcProgress.distanceFromBegin
				);
			}
		}		
	}

	private List<TransportRouteResult> prepareResults(ITransportRoutingContext ctx, List<TransportRouteSegment> results) {
		Collections.sort(results, new SegmentsComparator());
		var lst = new ArrayList<TransportRouteResult>();
		System.out.printf(
				Locale.US,
				"Calculated %.1f seconds, found %d results, visited %d routes / %d stops, loaded %d tiles (%d ms read, %d ms total), loaded ways %d (%d wrong)%n",
				(System.currentTimeMillis() - ctx.getStartCalcTime()) / 1000.0,
				results.size(),
				ctx.getVisitedRoutesCount(),
				ctx.getVisitedStops(),
				ctx.getQuadTreeSize(),
				ctx.getReadTime() / (1000 * 1000),
				ctx.getLoadTime() / (1000 * 1000),
				ctx.getLoadedWays(),
				ctx.getWrongLoadedWays()
		);
	    RouteCalculationProgress calcProgress;
		for (var res : results) {
			calcProgress = ctx.getCalculationProgress();
			if (calcProgress != null && calcProgress.isCancelled) {
				return null;
			}
			var route = new TransportRouteResult(ctx);
			route.routeTime = res.totalTravelTime;
			route.finishWalkDist = res.walkDist;
			TransportRouteSegment p = res;
			while (p != null) {
				calcProgress = ctx.getCalculationProgress();
				if (calcProgress != null && calcProgress.isCancelled) {
					return null;
				}
				if (p.parentRoute != null) {
					var sg = new TransportRouteResultSegment(this, this.distanceProvider);
					sg.route = p.parentRoute.road;
					sg.start = p.parentRoute.segStart;
					sg.end = p.parentStop;
					sg.walkDist = p.parentRoute.walkDist;
					sg.walkTime = sg.walkDist / ctx.getCfg().getWalkSpeed();
					sg.depTime = p.departureTime;
					sg.travelDistApproximate = p.parentTravelDist;
					sg.travelTime = p.parentTravelTime;
					route.segments.add(0, sg);
				}
				p = p.parentRoute;
			}
			// test if faster routes fully included
			boolean include = false;
			for(TransportRouteResult s : lst) {
				calcProgress = ctx.getCalculationProgress();
				if (calcProgress != null && calcProgress.isCancelled) {
					return null;
				}
				if (includeRoute(s, route)) {
					include = true;
					break;
				}
			}
			if(!include) {
				lst.add(route);
				System.out.println(route.toString());
			} else {
//				System.err.println(route.toString());
			}
		}
		return lst;
	}

	private boolean includeRoute(TransportRouteResult fastRoute, TransportRouteResult testRoute) {
		if(testRoute.segments.size() < fastRoute.segments.size()) {
			return false;
		}
		int j = 0;
		for(int i = 0; i < fastRoute.segments.size(); i++, j++) {
			TransportRouteResultSegment fs = fastRoute.segments.get(i);
			while(j < testRoute.segments.size()) {
				TransportRouteResultSegment ts = testRoute.segments.get(j);
				if(fs.route.getId().longValue() != ts.route.getId().longValue()) {
					j++;	
				} else {
					break;
				}
			}
			if(j >= testRoute.segments.size()) {
				return false;
			}
		}
		
		return true;
	}

	private static class SegmentsComparator implements Comparator<TransportRouteSegment> {
		@Override
		public int compare(TransportRouteSegment o1, TransportRouteSegment o2) {
			return Double.compare(o1.totalTravelTime, o2.totalTravelTime);
		}
	}

	public static String formatTransportTime(int i) {
		int h = i / 60 / 6;
		int mh = i - h * 60 * 6;
		int m = mh / 6;
		int s = (mh - m * 6) * 10;
		return String.format(Locale.US, "%02d:%02d:%02d ", h, m, s);
	}
	
	public static List<TransportRouteResult> convertToTransportRoutingResult(NativeTransportRoutingResult[] res,
                                                                             TransportRoutingConfiguration cfg) {
		// cache for converted TransportRoutes:
		TLongObjectHashMap<TransportRoute> convertedRoutesCache = new TLongObjectHashMap<>();
		TLongObjectHashMap<TransportStop> convertedStopsCache = new TLongObjectHashMap<>();

		if (res.length == 0) {
			LOG.info("Public transport. No route found");
			return new ArrayList<>();
		}
		List<TransportRouteResult> convertedRes = new ArrayList<>();
		for (NativeTransportRoutingResult ntrr : res) {
			TransportRouteResult trr = new TransportRouteResult(cfg);
			trr.setFinishWalkDist(ntrr.finishWalkDist);
			trr.setRouteTime(ntrr.routeTime);

			for (NativeTransportRouteResultSegment ntrs : ntrr.segments) {
				TransportRouteResultSegment trs = new TransportRouteResultSegment(new TransportRoutePlanner(), new DistanceProvider());
				trs.route = convertTransportRoute(ntrs.route, convertedRoutesCache, convertedStopsCache);
				trs.walkTime = ntrs.walkTime;
				trs.travelDistApproximate = ntrs.travelDistApproximate;
				trs.travelTime = ntrs.travelTime;
				trs.start = ntrs.start;
				trs.end = ntrs.end;
				trs.walkDist = ntrs.walkDist;
				trs.depTime = ntrs.depTime;

				trr.addSegment(trs);
			}
			convertedRes.add(trr);
		}
		convertedStopsCache.clear();
		convertedRoutesCache.clear();
		return convertedRes;
	}

	private static TransportRoute convertTransportRoute(NativeTransportRoute nr,
                                                        TLongObjectHashMap<TransportRoute> convertedRoutesCache,
                                                        TLongObjectHashMap<TransportStop> convertedStopsCache) {
		TransportRoute r = new TransportRoute();
		r.setId(nr.id);
		r.setLocation(nr.routeLat, nr.routeLon);
		r.setName(nr.name);
		r.setEnName(nr.enName);
		if (nr.namesLng.length > 0 && nr.namesLng.length == nr.namesNames.length) {
			for (int i = 0; i < nr.namesLng.length; i++) {
				r.setName(nr.namesLng[i], nr.namesNames[i]);
			}
		}
		r.setFileOffset(nr.fileOffset);
		r.setForwardStops(convertTransportStops(nr.forwardStops, convertedStopsCache));
		r.setRef(nr.ref);
		r.setOperator(nr.routeOperator);
		r.setType(nr.type);
		r.setDist(nr.dist);
		r.setColor(nr.color);

		if (nr.intervals != null && nr.intervals.length > 0 && nr.avgStopIntervals != null
				&& nr.avgStopIntervals.length > 0 && nr.avgWaitIntervals != null && nr.avgWaitIntervals.length > 0) {
			r.setSchedule(new TransportSchedule(new TIntArrayList(nr.intervals), new TIntArrayList(nr.avgStopIntervals),
					new TIntArrayList(nr.avgWaitIntervals)));
		}

		for (int i = 0; i < nr.waysIds.length; i++) {
			List<Node> wnodes = new ArrayList<>();
			for (int j = 0; j < nr.waysNodesLats[i].length; j++) {
				wnodes.add(new Node(nr.waysNodesLats[i][j], nr.waysNodesLons[i][j], -1));
			}
			r.addWay(new Way(nr.waysIds[i], wnodes));
		}

		if (convertedRoutesCache.get(r.getId()) == null) {
			convertedRoutesCache.put(r.getId(), r);
		}
		return r;
	}

	private static List<TransportStop> convertTransportStops(NativeTransportStop[] nstops,
                                                             TLongObjectHashMap<TransportStop> convertedStopsCache) {
		List<TransportStop> stops = new ArrayList<>();
		for (NativeTransportStop ns : nstops) {
			if (convertedStopsCache != null && convertedStopsCache.get(ns.id) != null) {
				stops.add(convertedStopsCache.get(ns.id));
				continue;
			}
			TransportStop s = new TransportStop();
			s.setId(ns.id);
			s.setLocation(ns.stopLat, ns.stopLon);
			s.setName(ns.name);
			s.setEnName(ns.enName);
			if (ns.namesLng.length > 0 && ns.namesLng.length == ns.namesNames.length) {
				for (int i = 0; i < ns.namesLng.length; i++) {
					s.setName(ns.namesLng[i], ns.namesNames[i]);
				}
			}
			s.setFileOffset(ns.fileOffset);
			// convert to long as C++ doesn't support int
			if (ns.referencesToRoutes != null) {
				long[] r = new long[ns.referencesToRoutes.length];
				for (int k = 0; k < r.length; k++) {
					r[k] = ns.referencesToRoutes[k];
				}
				s.setReferencesToRoutes(r);
			}
			s.setDeletedRoutesIds(ns.deletedRoutesIds);
			s.setRoutesIds(ns.routesIds);
			s.distance = ns.distance;
			s.x31 = ns.x31;
			s.y31 = ns.y31;

			if (ns.pTStopExit_refs != null && ns.pTStopExit_refs.length > 0) {
				for (int i = 0; i < ns.pTStopExit_refs.length; i++) {
					s.addExit(
							new TransportStopExit(ns.pTStopExit_x31s[i], ns.pTStopExit_y31s[i], ns.pTStopExit_refs[i]));
				}
			}

			if (convertedStopsCache == null) {
				convertedStopsCache = new TLongObjectHashMap<>();
			}
			if (convertedStopsCache.get(s.getId()) == null) {
				convertedStopsCache.put(s.getId(), s);
			}
			stops.add(s);
		}
		return stops;
	}
}
