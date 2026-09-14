package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

import net.osmand.PlatformUtil;
import net.osmand.binary.ObfConstants;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportSchedule;
import net.osmand.data.TransportStop;
import net.osmand.data.TransportStopExit;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

public class TransportRoutePlanner {
	
	private static final boolean MEASURE_TIME = false;
	
	private static final int MIN_DIST_STOP_TO_GEOMETRY = 150;
	public static final long GEOMETRY_WAY_ID = -1;
	public static final long STOPS_WAY_ID = -2;
	
	static long TRACE_ONBOARD_ID = 0; //19728216l
	static long TRACE_CHANGE_ID = 0; //19728216l
	
	private final static Log LOG = PlatformUtil.getLog(TransportRoutePlanner.class);

	public List<TransportRouteResult> buildRoute(TransportRoutingContext ctx, LatLon start, LatLon end) throws IOException, InterruptedException {
		System.out.println("[FERRY_PT_PROBE] MARK buildRoute() ENTRY build=MARKER-B1");
		long nonce = 0;
		ctx.startCalcTime = System.currentTimeMillis();
		double totalDistance = MapUtils.getDistance(start, end);
		List<TransportRouteSegment> startStops = ctx.getTransportStops(start);
		List<TransportRouteSegment> endStops = ctx.getTransportStops(end);

		// [FERRY_PT_PROBE] draft, testing a NEW hypothesis for issue #17773: startStops/endStops are
		// picked purely by straight-line distance to the start/end point (same class of bug as the
		// mid-route/finish issues above), with no check that a real path (not across open water)
		// actually exists. If the true start is on one shore and a ferry's boarding stop happens to
		// be geodesically "close enough" across the strait, buildRoute() will treat it as directly
		// walkable and skip whatever LOCAL ferry should have been ridden first. Dumping the full
		// candidate list (type, id, name, walkDist) here to see the raw picture before designing a fix.
		for (TransportRouteSegment s : startStops) {
			double d = MapUtils.getDistance(s.getLocation(), start);
			System.out.println("[FERRY_PT_PROBE] START candidate: " + s.road.getType() + " id=" + s.road.getId()
					+ " '" + s.road.getName() + "' walkDist=" + String.format("%.1f", d) + " m");
		}
		for (TransportRouteSegment s : endStops) {
			double d = MapUtils.getDistance(s.getLocation(), end);
			System.out.println("[FERRY_PT_PROBE] END candidate: " + s.road.getType() + " id=" + s.road.getId()
					+ " '" + s.road.getName() + "' walkDist=" + String.format("%.1f", d) + " m");
		}

		TLongObjectHashMap<TransportRouteSegment> endSegments = new TLongObjectHashMap<TransportRouteSegment>();
		for (TransportRouteSegment s : endStops) {
//			System.out.printf(" END %s %.3f - %d \n", s, s.distFromStart, s.getId()); 
			endSegments.put(s.getId(), s);
		}
		if (startStops.size() == 0) {
			LOG.info("Public transport. Start stop is empty");
			return Collections.emptyList();
		}
		PriorityQueue<TransportRouteSegment> queue = new PriorityQueue<>(startStops.size(), new SegmentsComparator());
		for (TransportRouteSegment r : startStops) {
			r.walkDist = (float) MapUtils.getDistance(r.getLocation(), start);
			r.distFromStart = r.walkDist / ctx.cfg.walkSpeed;
			if (TRACE_ONBOARD_ID != 0) {
				long id = ObfConstants.getOsmIdFromBinaryMapObjectId(r.road.getId());
				if (id == TRACE_ONBOARD_ID) {
					System.out.println(r.distFromStart + " " + id + " " + r);
				}
			}
			r.nonce = nonce++;
			queue.add(r);
		}

		double finishTime = ctx.cfg.maxRouteTime;
		if (totalDistance > ctx.cfg.maxRouteDistance && ctx.cfg.maxRouteIncreaseSpeed > 0)  {
			int increaseTime = (int) ((totalDistance - ctx.cfg.maxRouteDistance) 
					* 3.6 / ctx.cfg.maxRouteIncreaseSpeed);
			finishTime += increaseTime;
		}
		double maxTravelTimeCmpToWalk = totalDistance / ctx.cfg.walkSpeed;
		List<TransportRouteSegment> results = new ArrayList<TransportRouteSegment>();
		initProgressBar(ctx, start, end);
		while (!queue.isEmpty()) {
			long beginMs = MEASURE_TIME ? System.currentTimeMillis() : 0;
			if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
				return null;
			}
			TransportRouteSegment segment = queue.poll();
			long segIdWithParent = segmentWithParentId(segment, segment.parentRoute);
			TransportRouteSegment ex = ctx.visitedSegments.get(segIdWithParent);
			if (ex != null) {
				if (ex.distFromStart > segment.distFromStart) {
					System.err.println(String.format("%.1f (%s) > %.1f (%s)", ex.distFromStart, ex, segment.distFromStart, segment));
				}
				continue;
			}
			ctx.visitedRoutesCount++;
			ctx.visitedSegments.put(segIdWithParent, segment);
			// [FERRY_PT_PROBE] draft, investigating issue #17773 (debug_32.txt, Sandbanks Ferry not
			// found): log every time a ferry TransportRouteSegment is popped off the priority queue,
			// so we can see whether/when it gets processed relative to the BREAK below, and what its
			// segStart/distFromStart were at pop time.
			if ("ferry".equals(segment.road.getType())) {
				System.out.println("[FERRY_PT_PROBE] POPPED ferry segment: id=" + segment.road.getId()
						+ " '" + segment.road.getName() + "' segStart=" + segment.segStart
						+ ", distFromStart=" + String.format("%.1f", segment.distFromStart)
						+ ", finishTime=" + String.format("%.1f", finishTime)
						+ ", finishTime*increase=" + String.format("%.1f", finishTime * ctx.cfg.increaseForAlternativesRoutes));
			}
			
			if (segment.distFromStart > finishTime * ctx.cfg.increaseForAlternativesRoutes ||
					segment.distFromStart > maxTravelTimeCmpToWalk) {
				// [FERRY_PT_PROBE] draft, testing hypothesis for issue #17773: this is a HARD break
				// of the whole search (not a skip/continue for this branch only). Logging here to see
				// whether a cheap "finish" (e.g. a single-ferry direct-walk completion) shrinks
				// finishTime early enough to cut off other, still-unexplored bus contexts before they
				// ever reach their own ferry-to-ferry transfer attempt.
				System.out.println("[FERRY_PT_PROBE] BREAK main loop: segment=" + segment.road.getRef()
						+ " id=" + segment.road.getId() + " '" + segment.road.getName()
						+ "' distFromStart=" + String.format("%.1f", segment.distFromStart)
						+ ", finishTime=" + String.format("%.1f", finishTime)
						+ ", finishTime*increase=" + String.format("%.1f", finishTime * ctx.cfg.increaseForAlternativesRoutes)
						+ ", maxTravelTimeCmpToWalk=" + String.format("%.1f", maxTravelTimeCmpToWalk));
				break;
			}
			TransportRouteSegment finish = null;
			double minDist = 0;
			double travelDist = 0;

			int seconds = segment.road.calcIntervalInSeconds();
			// [FERRY_PT_PROBE] TEMP debug fix for issue #17773 (debug_33.txt, Sandbanks Ferry never
			// competitive): using interval/2 as the boarding/wait estimate makes a standalone
			// route=ferry PT entity with a real OSM interval=* tag (e.g. ~20 min => ~600s here) look
			// far more expensive than a non-ferry route (e.g. a bus) that happens to physically cross
			// the very same ferry mid-route - the bus's crossing is priced as an ordinary road hop
			// (geodesic distance / bus speed) with NO wait penalty at all, since it's just "next stop"
			// on an already-boarded ride, not a boarding event. Same physical ferry crossing, two very
			// different costs -> the cheap (bus) one always wins the accept-threshold competition and
			// the ferry-only finish gets rejected (see [FERRY_PT_PROBE] ferry finish REJECTED logs).
			// Temporarily disabling the interval/2 estimate entirely (always use getBoardingTime())
			// so standalone ferry finishes stop being penalized relative to buses that silently ride
			// the same ferry for free.
			// TODO(#17773): this is a one-sided temp fix (removes the ferry's penalty) - properly fix
			// by making the comparison symmetric instead: ALSO charge non-ferry routes an interval/2
			// (or equivalent) wait estimate for any mid-route hop that actually crosses a ferry, so
			// standalone ferry PT entities and ferry-carrying bus/car routes compete on equal footing.
			// Revert this disablement once that symmetric fix is in place.
			boolean useIntervalHalfAsWait = false;
			double travelTime = (useIntervalHalfAsWait && seconds > 0) ? (double) seconds / 2 : ctx.cfg.getBoardingTime(segment.road.getType());
			//double travelTime = seconds > 0 ? (double) seconds / 2 : ctx.cfg.getBoardingTime(segment.road.getType());

			final float routeTravelSpeed = ctx.cfg.getSpeedByRouteType(segment.road.getType());
			if (routeTravelSpeed == 0) {
				continue;
			}
			TransportStop prevStop = segment.getStop(segment.segStart);
			List<TransportRouteSegment> sgms = new ArrayList<TransportRouteSegment>();
			if (TRACE_ONBOARD_ID != 0) {
				long id = ObfConstants.getOsmIdFromBinaryMapObjectId(segment.road.getId());
				if (id == TRACE_ONBOARD_ID) {
					System.out.printf("-> %d (%d) %.2f (parent %s) \n", id, segment.segStart, segment.distFromStart, segment.parentRoute);
				}
			}
			for (int ind = 1 + segment.segStart; ind < segment.getLength(); ind++) {
				if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
					return null;
				}
				segIdWithParent ++;
				ctx.visitedSegments.put(segIdWithParent, segment);
				TransportStop stop = segment.getStop(ind);
				// could be geometry size
				double segmentDist = MapUtils.getDistance(prevStop.getLocation(), stop.getLocation());
				travelDist += segmentDist;
				if (ctx.cfg.useSchedule) {
					TransportSchedule sc = segment.road.getSchedule();
					int interval = sc.avgStopIntervals.get(ind - 1);
					travelTime += interval * 10;
				} else {
					int stopTime = ctx.cfg.getStopTime(segment.road.getType());
					travelTime += stopTime + segmentDist / routeTravelSpeed;
				}
				if (segment.distFromStart + travelTime > finishTime * ctx.cfg.increaseForAlternativesRoutes) {
					break;
				}
				sgms.clear();
				if (segment.getDepth() < ctx.cfg.maxNumberOfChanges + 1) {
					sgms = ctx.getTransportStops(stop.x31, stop.y31, true, sgms);
					ctx.visitedStops++;
					for (TransportRouteSegment sgm : sgms) {
						if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
							return null;
						}
						if (segment.wasVisited(sgm)) {
							continue;
						}
						// [FERRY_PT_PROBE] new bug found for issue #17773 (debug_28.txt): wasVisited()
						// above only blocks re-riding the EXACT same road id, but a ferry's forward and
						// backward directions are two DIFFERENT road ids (base<<1 / (base<<1)+1), so it
						// does not catch boarding a ferry's reverse-direction sibling right after riding
						// it (or vice versa) - a degenerate "ride out, immediately ride straight back"
						// loop with 0.0 m walk at the transfer and zero real displacement. This used to
						// be an accidental side effect of the shared (non-nonce) dedup key colliding
						// across contexts, but is not reliably blocked now that ferry-parent transfers
						// get a unique key per calling context. Confirmed in debug_28.txt: a route that
						// rides ferry1-reverse then immediately ferry1-forward (or, in a longer case,
						// ferry2-reverse -> ferry1-reverse -> ferry1-forward) got ADDED to the results,
						// and its inflated ferry count fooled filterIncompleteFerryRoutes()'s "more
						// ferries = more complete crossing" heuristic into preferring it over the
						// correct, non-looped route. Block it explicitly using the same (id>>1)
						// same-physical-route comparison already used by isMidCrossingFerryStop above.
						if ("ferry".equals(segment.road.getType()) && "ferry".equals(sgm.road.getType())
								&& (segment.road.getId().longValue() >> 1) == (sgm.road.getId().longValue() >> 1)) {
							continue;
						}
						if (ctx.visitedSegments.containsKey(segmentWithParentId(sgm, segment))) {
							// [FERRY_PT_PROBE] draft, testing hypothesis for issue #17773: this dedup
							// key is (parent ROAD id << 30) + target segment id - it does NOT depend on
							// which earlier bus/path got us onto "segment" (the currently ridden road),
							// only on the road itself. So if two different starting contexts both ride
							// the SAME ferry1 and both try to transfer onto the SAME ferry2 at the SAME
							// stop, only the first one to arrive here claims this transfer - every other
							// context silently loses the ability to ever complete its own "both ferries"
							// route, even though it's a genuinely different overall journey.
							if ("ferry".equals(segment.road.getType()) && "ferry".equals(sgm.road.getType())) {
								System.out.println("[FERRY_PT_PROBE] transfer to ferry ALREADY CLAIMED by another context: riding "
										+ segment.road.getRef() + " id=" + segment.road.getId()
										+ " (this segment distFromStart=" + segment.distFromStart + "), wanted to transfer to "
										+ sgm.road.getRef() + " id=" + sgm.road.getId() + " at stop " + stop.getId());
							}
							continue;
						}
						TransportRouteSegment nextSegment = new TransportRouteSegment(sgm);
						nextSegment.parentRoute = segment;
						nextSegment.parentStop = ind;
						nextSegment.walkDist = MapUtils.getDistance(nextSegment.getLocation(), stop.getLocation());
						nextSegment.parentTravelTime = travelTime;
						nextSegment.parentTravelDist = travelDist;
						double walkTime = nextSegment.walkDist / ctx.cfg.walkSpeed + 
								ctx.cfg.getChangeTime(segment.road.getType(), sgm.road.getType());
						nextSegment.distFromStart = segment.distFromStart + travelTime + walkTime;
						nextSegment.nonce = nonce++;
						if (ctx.cfg.useSchedule) {
							int tm = (sgm.departureTime - ctx.cfg.scheduleTimeOfDay) * 10;
							if (tm >= nextSegment.distFromStart) {
								nextSegment.distFromStart = tm;
								queue.add(nextSegment);
							}
						} else {
							queue.add(nextSegment);
						}
						if (TRACE_CHANGE_ID != 0) {
							long from = ObfConstants.getOsmIdFromBinaryMapObjectId(segment.road.getId());
							long to = ObfConstants.getOsmIdFromBinaryMapObjectId(sgm.road.getId());
							System.out.printf("? Change %d (%d) -> %d (%d) %.3f\n", from, ind, to, sgm.segStart,
									nextSegment.distFromStart);
						}
					}
				}
				TransportRouteSegment finalSegment = endSegments.get(segment.getId() + ind - segment.segStart);
				double distToEnd = MapUtils.getDistance(stop.getLocation(), end);
				// [FERRY_PT_PROBE] draft, investigating issue #17773 (debug_32.txt, Sandbanks Ferry not
				// found): log every stop visited while riding a ferry, whether a finalSegment (END
				// candidate for this exact road+index) was found, and the geodesic distance to the end
				// point - to see whether the ferry ever gets close enough to complete a finish, or is
				// silently missing a matching END candidate at this index.
				if ("ferry".equals(segment.road.getType())) {
					System.out.println("[FERRY_PT_PROBE] ferry stop while riding: id=" + segment.road.getId()
							+ " '" + segment.road.getName() + "', ind=" + ind + " stop id=" + stop.getId()
							+ ", finalSegment=" + (finalSegment == null ? "null" : ("id=" + finalSegment.road.getId()))
							+ ", distToEnd=" + String.format("%.1f", distToEnd) + " m, walkRadius=" + ctx.cfg.walkRadius);
				}
//				if (finalSegment != null && distToEnd < 100 && finalSegment.road.getType().equals("ferry") ||
//						finalSegment != null && distToEnd < ctx.cfg.walkRadius && !finalSegment.road.getType().equals("ferry")
//				) {
				// [FERRY_PT_PROBE] fix for issue #17773 (Nordoleden/Hyppeln investigation): if we are
				// currently riding a ferry AND this exact stop also offers a DIFFERENT ferry
				// continuation (sgms, just computed above), this stop MIGHT be a synthetic
				// mid-crossing transfer node (see orphan-ferry-way OBF generation notes) rather than
				// a real dock a pedestrian could walk further from - but only might: a real, tagged
				// terminal (e.g. amenity=ferry_terminal, like Hyppeln on Nordöleden) can legitimately
				// sit at exactly the same node where two named ferry legs happen to connect, and
				// ending the trip there is perfectly valid. So this additionally requires
				// stop.isSyntheticTerminal() - set at generation time, true ONLY for a stop the
				// generator itself fabricated (a way endpoint forced into being a stop with no real
				// backing OSM tag), never for a real pre-existing tagged terminal. Earlier versions of
				// this check suppressed ANY shared ferry stop unconditionally, which wrongly discarded
				// the correct (shortest) finish at Hyppeln and caused forward/backward asymmetry.
				// IMPORTANT: every direct/backward TransportRoute pair in this codebase is generated
				// as id=(base<<1) / id=(base<<1)+1 (both IndexTransportCreator V1 and our new
				// way-based branch use this), so the REVERSE direction of the very route we are
				// currently riding always shares this id pair and always shows up in sgms at BOTH
				// endpoints too - not just at a real mid-crossing node. Comparing (id >> 1) excludes
				// that same-pair reverse-direction match, which is what was wrongly suppressing every
				// ferry finish (including legitimate ones) and made buildRoute() return zero routes
				// after the first version of this check.
				boolean isMidCrossingFerryStop = false;
				if ("ferry".equals(segment.road.getType()) && stop.isSyntheticTerminal()) {
					long currentRoutePairId = segment.road.getId().longValue() >> 1;
					for (TransportRouteSegment sgm : sgms) {
						if ("ferry".equals(sgm.road.getType())
								&& (sgm.road.getId().longValue() >> 1) != currentRoutePairId) {
							isMidCrossingFerryStop = true;
							System.out.println("[FERRY_PT_PROBE] mid-crossing ferry stop detected: riding "
									+ segment.road.getRef() + " id=" + segment.road.getId()
									+ " '" + segment.road.getName() + "', stop id=" + stop.getId()
									+ ", continuation found: " + sgm.road.getRef() + " id=" + sgm.road.getId()
									+ " '" + sgm.road.getName() + "'");
							break;
						}
					}
				}
				// [FERRY_PT_PROBE] draft, testing a NEW hypothesis: isMidCrossingFerryStop above only
				// fires when we are CURRENTLY riding a ferry. But a finish reached via a non-ferry
				// mode (e.g. a bus) at a stop that ALSO has a ferry departing from it nearby could be
				// exactly the same "walk across water" bug, just one hop removed - the bus got us to
				// one shore, and the pure-geodesic distToEnd check has no idea the destination is
				// actually across a strait the bus never crossed. Purely diagnostic for now - does not
				// change which finishes get created, only logs when this situation occurs.
				if (finalSegment != null && distToEnd < ctx.cfg.walkRadius && !"ferry".equals(segment.road.getType())) {
					for (TransportRouteSegment sgm : sgms) {
						if ("ferry".equals(sgm.road.getType())) {
							System.out.println("[FERRY_PT_PROBE] NON-FERRY finish near a ferry stop: riding "
									+ segment.road.getRef() + " id=" + segment.road.getId()
									+ " (" + segment.road.getType() + ") '" + segment.road.getName()
									+ "', stop id=" + stop.getId() + ", distToEnd=" + String.format("%.1f", distToEnd)
									+ " m, nearby ferry: " + sgm.road.getRef() + " id=" + sgm.road.getId()
									+ " '" + sgm.road.getName() + "'");
							break;
						}
					}
				}
				if (finalSegment != null && distToEnd < ctx.cfg.walkRadius) {
					if (isMidCrossingFerryStop) {
						System.out.println("[FERRY_PT_PROBE] SUPPRESSED finish at mid-crossing ferry stop "
								+ stop.getId() + " (would have walked " + String.format("%.1f", distToEnd) + " m)");
					}
				}
				if (finalSegment != null && distToEnd < ctx.cfg.walkRadius && !isMidCrossingFerryStop) {
					if (finish == null || minDist > distToEnd) {
						minDist = distToEnd;
						finish = new TransportRouteSegment(finalSegment);
						finish.parentRoute = segment;
						finish.parentStop = ind;
						finish.walkDist = distToEnd;
						finish.parentTravelTime = travelTime;
						finish.parentTravelDist = travelDist;
						double walkTime = distToEnd / ctx.cfg.walkSpeed;
						finish.distFromStart = segment.distFromStart + travelTime + walkTime;
						finish.nonce = nonce++;
					}
				}
				prevStop = stop;
			}
			if (finish != null) {
				// [FERRY_PT_PROBE] draft, investigating issue #17773 (debug_33.txt, Sandbanks Ferry
				// finish gets computed but never lands in results): log the exact numbers used by
				// the accept/reject decision below for every FERRY finish candidate, whether or not
				// it ends up shrinking finishTime or being added to results.
				if ("ferry".equals(segment.road.getType())) {
					System.out.println("[FERRY_PT_PROBE] ferry finish candidate: segment id=" + segment.road.getId()
							+ " '" + segment.road.getName() + "', walkDist=" + String.format("%.1f", finish.walkDist)
							+ ", finish.distFromStart=" + String.format("%.1f", finish.distFromStart)
							+ ", finishTime(before)=" + String.format("%.1f", finishTime)
							+ ", finishTime*increase=" + String.format("%.1f", finishTime * ctx.cfg.increaseForAlternativesRoutes)
							+ ", maxTravelTimeCmpToWalk=" + String.format("%.1f", maxTravelTimeCmpToWalk)
							+ ", results.size()=" + results.size());
				}
				if (finishTime > finish.distFromStart) {
					// [FERRY_PT_PROBE] draft, testing hypothesis for issue #17773: log every time
					// finishTime shrinks, and by which finish/segment - to check whether a cheap
					// (e.g. single-ferry) finish shrinks it prematurely and triggers the hard break
					// above before other legitimate, more expensive bus contexts get explored.
					System.out.println("[FERRY_PT_PROBE] finishTime shrunk from " + String.format("%.1f", finishTime)
							+ " to " + String.format("%.1f", finish.distFromStart)
							+ " by finish via segment=" + segment.road.getRef() + " id=" + segment.road.getId()
							+ " '" + segment.road.getName() + "', walkDist=" + String.format("%.1f", finish.walkDist));
					finishTime = finish.distFromStart;
				}
				boolean willAdd = finish.distFromStart < finishTime * ctx.cfg.increaseForAlternativesRoutes &&
						(finish.distFromStart < maxTravelTimeCmpToWalk || results.size() == 0);
				if ("ferry".equals(segment.road.getType()) && !willAdd) {
					// [FERRY_PT_PROBE] draft: log exactly why a ferry finish candidate got REJECTED
					// (not added to results) - which of the two conditions failed.
					System.out.println("[FERRY_PT_PROBE] ferry finish REJECTED: finish.distFromStart="
							+ String.format("%.1f", finish.distFromStart)
							+ " vs finishTime*increase=" + String.format("%.1f", finishTime * ctx.cfg.increaseForAlternativesRoutes)
							+ " (cond1=" + (finish.distFromStart < finishTime * ctx.cfg.increaseForAlternativesRoutes)
							+ "), vs maxTravelTimeCmpToWalk=" + String.format("%.1f", maxTravelTimeCmpToWalk)
							+ " (cond2=" + (finish.distFromStart < maxTravelTimeCmpToWalk) + "), results.size()=" + results.size());
				}
				if (willAdd) {
					results.add(finish);
					// Stop when results reached range [1000 min, 2500 (for default limit * changes), 5000 max]
					int optimalLimitOfResults = 25 * ctx.cfg.ptLimitResultsByNumber * ctx.cfg.maxNumberOfChanges;
					if (results.size() > Math.min(Math.max(1000, optimalLimitOfResults), 5000)) {
						break;
					}
				}
			}

			if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
				throw new InterruptedException("Route calculation interrupted");
			}
			if (MEASURE_TIME) {
				long time = System.currentTimeMillis() - beginMs;
				if (time > 10) {
					System.out.println(String.format("%d ms ref - %s id - %d", time, segment.road.getRef(),
							segment.road.getId()));
				}
			}
			updateCalculationProgress(ctx, queue);
			
		}

		//TODO: delete after debug
		// [FERRY_PT_PROBE] BUG FOUND: this used to check only result.road.getType() - i.e. only the
		// LAST ridden transit route before the final walk. A route like ferry1 -> ferry2 -> bus ->
		// destination legitimately uses both ferries but ENDS on a bus, so it was being classified as
		// "not ferry" and silently dropped by "return prepareResults(ctx, ferryResults)" below, even
		// though it's exactly the kind of route we want. Fixed by walking the whole parentRoute chain
		// and checking whether ANY leg of the journey was a ferry, not just the last one.
		List<TransportRouteSegment> ferryResults = new ArrayList<TransportRouteSegment>();
		List<TransportRouteSegment> notFerryResults = new ArrayList<TransportRouteSegment>();

		for (TransportRouteSegment result : results) {
			boolean usesFerry = false;
			TransportRouteSegment cur = result;
			while (cur != null) {
				if ("ferry".equals(cur.road.getType())) {
					usesFerry = true;
					break;
				}
				cur = cur.parentRoute;
			}
			if (usesFerry) {
				ferryResults.add(result);
			} else {
				notFerryResults.add(result);
			}
		}


		System.out.println("[FERRY_PT_PROBE] buildRoute() finished: results.size()=" + results.size()
				+ " ferryResults.size()=" + ferryResults.size()
				+ " notFerryResults.size()=" + notFerryResults.size());
		return prepareResults(ctx, ferryResults);

		// [FERRY_PT_PROBE] reverted the ferry-only debug filter (issue #17773, debug_30.txt):
		// restricting the final result list to ferryResults only was a temporary hack to make
		// ferry-containing candidates easy to spot in the logs while debugging the ferry-search
		// bugs above. It was silently dropping every valid non-ferry route whenever the search
        // didn't happen to also find a ferry-based alternative for that start/end pair (confirmed
		// e.g. for the Sandbanks Ferry test: results.size()=1 notFerryResults.size()=1 but
		// ferryResults.size()=0, so prepareResults() got an empty list and OsmAnd reported "no
		// route found" even though a perfectly valid bus-only route existed). Back to returning
		// the full results list now that the ferry-specific bugs are fixed.
//		return prepareResults(ctx, results);
	}

	private long segmentWithParentId(TransportRouteSegment segment, TransportRouteSegment parent) {
		long key = ((parent != null ? ObfConstants.getOsmIdFromBinaryMapObjectId(parent.road.getId()) : 0) << 30l)
				+ segment.getId();
		// [FERRY_PT_PROBE] draft v2, testing hypothesis for issue #17773: this key is otherwise
		// based only on the PARENT'S ROAD id, so two different contexts that both end up riding
		// the same ferry (e.g. one via a direct walk to its dock, another after riding an earlier
		// ferry1 first) and both try to transfer onto the SAME next leg (another ferry, a bus,
		// anything) from the same stop collide into one shared "visited" state - confirmed by
		// debug_25/debug_26/debug_27 logs ("transfer to ferry ALREADY CLAIMED by another context",
		// and separately by a5164e59-debug_27.txt where a ferry1->ferry2->bus route was never
		// produced even though ferry1->ferry2 and ferry2->bus were each found independently).
		// Only the fastest-arriving context's transfer survives; every other context silently
		// loses the ability to ever continue its own journey past that ferry, even though it's a
		// genuinely different journey with a genuinely different arrival time. Mixing in the
		// parent's nonce (unique per TransportRouteSegment instance) makes the key unique per
		// calling context instead of per road, whenever the PARENT (the road we are transferring
		// FROM) is a ferry - not just for ferry-to-ferry transfers as in the first version of this
		// fix - since the ambiguous-arrival-context problem exists for ANY leg following a ferry,
		// not only another ferry. Every other transfer (parent not a ferry) keeps the original
		// (intentional) shared-state dedup untouched.
		if (parent != null && "ferry".equals(parent.road.getType())) {
			key = key * 1000003L + parent.nonce;
		}
		return key;
	}
	
	private void initProgressBar(TransportRoutingContext ctx, LatLon start, LatLon end) {
		if (ctx.calculationProgress != null) {
			ctx.calculationProgress.distanceFromEnd = 0;
			ctx.calculationProgress.reverseSegmentQueueSize = 0;
			ctx.calculationProgress.directSegmentQueueSize = 0;
			float speed = (float) ctx.cfg.defaultTravelSpeed + 1; // assume
			ctx.calculationProgress.totalEstimatedDistance = (float) (MapUtils.getDistance(start, end) / speed);
		}
	}

	private void updateCalculationProgress(TransportRoutingContext ctx, PriorityQueue<TransportRouteSegment> queue) {
		if (ctx.calculationProgress != null) {
			ctx.calculationProgress.directSegmentQueueSize = queue.size();
			if (queue.size() > 0) {
				TransportRouteSegment peek = queue.peek();
				ctx.calculationProgress.distanceFromBegin = (float) Math.max(peek.distFromStart,
						ctx.calculationProgress.distanceFromBegin);
			}
		}		
	}

	private List<TransportRouteResult> prepareResults(TransportRoutingContext ctx, List<TransportRouteSegment> results) {
		System.out.println("[FERRY_PT_PROBE] MARK prepareResults() ENTRY build=MARKER-B1 results.size()=" + results.size());
		Collections.sort(results, new SegmentsComparator());

		List<TransportRouteResult> lst = new ArrayList<TransportRouteResult>();
		System.out.println(String.format(Locale.US, "Calculated %.1f seconds, found %d results, visited %d routes / %d stops, loaded %d tiles (%d ms read, %d ms total), loaded ways %d (%d wrong)",
				(System.currentTimeMillis() - ctx.startCalcTime) / 1000.0, results.size(), 
				ctx.visitedRoutesCount, ctx.visitedStops, 
				ctx.quadTree.size(), ctx.readTime / (1000 * 1000), ctx.loadTime / (1000 * 1000),
				ctx.loadedWays, ctx.wrongLoadedWays));
		// [FERRY_PT_PROBE] temporary: dump every raw candidate (before exclude/alternative/limit
		// filtering) so we can see whether a route exists among them and, if so, why it did/didn't
		// make it into the final displayed list. Remove once issue #17773 ferry PT routing is verified.
		List<TransportRouteResult> debugRoutesList = new ArrayList<TransportRouteResult>();
		for (int probeIdx = 0; probeIdx < results.size(); probeIdx++) {
			TransportRouteSegment probeRes = results.get(probeIdx);
			TransportRouteResult probeRoute = new TransportRouteResult(ctx);
			probeRoute.routeTime = probeRes.distFromStart;
			probeRoute.finishWalkDist = probeRes.walkDist;
			TransportRouteSegment pp = probeRes;
			while (pp != null) {
				if (pp.parentRoute != null) {
					TransportRouteResultSegment sg = new TransportRouteResultSegment();
					sg.route = pp.parentRoute.road;
					sg.start = pp.parentRoute.segStart;
					sg.end = pp.parentStop;
					sg.walkDist = pp.parentRoute.walkDist;
					sg.walkTime = sg.walkDist / ctx.cfg.walkSpeed;
					sg.depTime = pp.departureTime;
					sg.travelDistApproximate = pp.parentTravelDist;
					sg.travelTime = pp.parentTravelTime;
					probeRoute.segments.add(0, sg);
				}
				pp = pp.parentRoute;
			}
			System.out.println("[FERRY_PT_PROBE] raw #" + probeIdx + " " + probeRoute.toString());
			debugRoutesList.add(probeRoute);
		}


		List<TransportRouteResult> routesWithFerry = new ArrayList<TransportRouteResult>();

		for (TransportRouteSegment res : results) {
			if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
				return null;
			}

			TransportRouteResult route = getParsedTransportRouteResult(ctx, res);
			if (route.getFerryCount() > 0) {
				routesWithFerry.add(route);
				continue;
			}

			// test if faster routes fully included
			boolean exclude = false;
			exclude = shouldExclude(ctx, lst, route);

			/*
			for (TransportRouteResult s : lst) {
				if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
					//return null;
					break;
				}
				if (excludeRoute(ctx, s, route)) {
					exclude = true;
					System.out.println("[FERRY_PT_PROBE] EXCLUDED (dominated): " + route.toString());
					break;
				}
			}
			if (!exclude) {
				for (TransportRouteResult s : lst) {
					if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
						//return null;
						break;
					}
					if (checkAlternative(ctx, s, route)) {
						System.out.println("[FERRY_PT_PROBE] ALT " + s.getSegments().get(0).route + " " + route.toString());
						exclude = true;
						break;
					}
				}
			}
			 */

			if (!exclude) {
				int limitByNumber = ctx.cfg.ptLimitResultsByNumber;
				if (limitByNumber > 0 && lst.size() >= limitByNumber) {
					System.out.println("[FERRY_PT_PROBE] TRUNCATED ptLimitResultsByNumber (" + limitByNumber
							+ ") reached, remaining candidate: " + route.toString());
					break;
				}
				System.out.println("[FERRY_PT_PROBE] ADDED: " + route.toString());
				lst.add(route);
			}
		}


		Collections.sort(routesWithFerry, new FerrySegmentsComparator());

		routesWithFerry = filterIncompleteFerryRoutes(routesWithFerry);

		for (TransportRouteResult routeeWithFerry : routesWithFerry) {
			if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
				return null;
			}

			// test if faster routes fully included
			boolean exclude = false;
			exclude = shouldExclude(ctx, lst, routeeWithFerry);

			if (!exclude) {
				int limitByNumber = ctx.cfg.ptLimitResultsByNumber;
				if (limitByNumber > 0 && lst.size() >= limitByNumber) {
					System.out.println("[FERRY_PT_PROBE] TRUNCATED ptLimitResultsByNumber (" + limitByNumber
							+ ") reached, remaining candidate: " + routeeWithFerry.toString());
					break;
				}
				System.out.println("[FERRY_PT_PROBE] ADDED: " + routeeWithFerry.toString());
				lst.add(routeeWithFerry);
			}
		}


		for (TransportRouteResult r : lst) {
			for (int i = 0; i < r.getSegments().size(); i++) {
				String mainRef = r.getSegments().get(i).route.getRef();
				Map<String, TransportRouteResultSegment> alts = new LinkedHashMap<>();
				for (TransportRouteResult alt : r.alternativeRoutes) {
					TransportRouteResultSegment rs = alt.getSegments().get(i);
					String altRef = rs.route.getRef();
					if (!Algorithms.isEmpty(altRef) && !altRef.equals(mainRef)) {
						alts.putIfAbsent(altRef, rs);
					}
				}
				r.getSegments().get(i).alternatives.addAll(alts.values());
			}
		}

		return lst;
	}

	private boolean shouldExclude(TransportRoutingContext ctx, List<TransportRouteResult> lst, TransportRouteResult route) {
		// test if faster routes fully included
		boolean exclude = false;
		for (TransportRouteResult s : lst) {
			if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
				//return null;
				break;
			}
			if (excludeRoute(ctx, s, route)) {
				exclude = true;
				System.out.println("[FERRY_PT_PROBE] EXCLUDED (dominated): " + route.toString());
				break;
			}
		}
		if (!exclude) {
			for (TransportRouteResult s : lst) {
				if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
					//return null;
					break;
				}
				if (checkAlternative(ctx, s, route)) {
					System.out.println("[FERRY_PT_PROBE] ALT " + s.getSegments().get(0).route + " " + route.toString());
					exclude = true;
					break;
				}
			}
		}
		return exclude;
	}

	private static TransportRouteResult getParsedTransportRouteResult(TransportRoutingContext ctx, TransportRouteSegment res) {
		TransportRouteResult route = new TransportRouteResult(ctx);
		route.routeTime = res.distFromStart;
		route.finishWalkDist = res.walkDist;
		TransportRouteSegment p = res;
		while (p != null) {
			if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
				return null;
			}
			if (p.parentRoute != null) {
				TransportRouteResultSegment sg = new TransportRouteResultSegment();
				sg.route = p.parentRoute.road;
				sg.start = p.parentRoute.segStart;
				sg.end = p.parentStop;
				sg.walkDist = p.parentRoute.walkDist;
				sg.walkTime = sg.walkDist / ctx.cfg.walkSpeed;
				sg.depTime = p.departureTime;
				sg.travelDistApproximate = p.parentTravelDist;
				sg.travelTime = p.parentTravelTime;
				route.segments.add(0, sg);
			}
			p = p.parentRoute;
		}
		return route;
	}

	// [FERRY_PT_PROBE] draft v2, testing hypothesis for issue #17773: group routes by their
	// NON-ferry segments (same bus/tram context, same route ids, same order) - this is what
	// makes two candidates "the same underlying trip". Within each group, keep only the route
	// with the most ferry segments (the most complete crossing for that context) and drop the
	// rest. Unlike v1 (subset-of-ferry-ids over the whole candidate list), this won't merge two
	// routes that only coincidentally share a ferry leg but start on different buses.
	// [bus_1, ferry_1, bus_2;  bus_1, ferry_2, bus_2;  bus_1, ferry_1, ferry_2, bus_2]
	//   -> [bus_1, ferry_1, ferry_2, bus_2]
	private static List<TransportRouteResult> filterIncompleteFerryRoutes(List<TransportRouteResult> routesWithFerry) {
		Map<String, TransportRouteResult> bestRouteByNonFerryContext = new LinkedHashMap<>();
		for (TransportRouteResult route : routesWithFerry) {
			String nonFerryContextKey = getNonFerryContextKey(route);
			TransportRouteResult bestSoFar = bestRouteByNonFerryContext.get(nonFerryContextKey);
			if (bestSoFar == null || route.getFerryCount() > bestSoFar.getFerryCount()) {
				bestRouteByNonFerryContext.put(nonFerryContextKey, route);
			}
		}
		return new ArrayList<>(bestRouteByNonFerryContext.values());
	}

	// Builds a key from the route ids of the NON-ferry segments only, in order - two routes get
	// the same key iff their surrounding (non-ferry) journey is identical; the ferry segments
	// in between are exactly what's allowed to differ between routes sharing a key.
	private static String getNonFerryContextKey(TransportRouteResult route) {
		StringBuilder key = new StringBuilder();
		for (TransportRouteResultSegment segment : route.getSegments()) {
			if (!"ferry".equals(segment.route.getType())) {
				key.append(segment.route.getId()).append(';');
			}
		}
		return key.toString();
	}

	private boolean excludeRoute(TransportRoutingContext ctx, TransportRouteResult fastRoute, TransportRouteResult testRoute) {
		if (sameRouteWithExtraSegments(fastRoute, testRoute)) {
			return true;
		}
		double fastRouteWalkDist = Math.max(fastRoute.getWalkDist(),
				ctx.cfg.combineAltRoutesDiffStops * ctx.cfg.increaseForAltRoutesWalking);
		if (fastRouteWalkDist * ctx.cfg.increaseForAltRoutesWalking < testRoute.getWalkDist()) {
			// remove routes where we need to walk x3
			return true;
		}
		for (TransportRouteResult alt : fastRoute.getAlternativeRoutes()) {
			if (sameRouteWithExtraSegments(alt, testRoute)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkAlternative(TransportRoutingContext ctx, TransportRouteResult fastRoute, TransportRouteResult testRoute) {
		boolean alternativeRoute = false;
		if (testRoute.segments.size() == fastRoute.segments.size()) {
			double sumDiffs = 0; 
			alternativeRoute = true;
			for (int i = 0; i < fastRoute.segments.size(); i++) {
				TransportRouteResultSegment seg1 = fastRoute.segments.get(i);
				TransportRouteResultSegment seg2 = testRoute.segments.get(i);
				double startDiff = MapUtils.getDistance(seg1.getStart().getLocation(), seg2.getStart().getLocation());
				double endDiff = MapUtils.getDistance(seg1.getEnd().getLocation(), seg2.getEnd().getLocation());
//				if (seg1.getStart().getId().longValue() != seg2.getStart().getId().longValue()
//						|| seg1.getEnd().getId().longValue() == seg2.getEnd().getId().longValue()) {
				sumDiffs += startDiff;
				sumDiffs += endDiff;
				if (startDiff > ctx.cfg.combineAltRoutesDiffStops || endDiff > ctx.cfg.combineAltRoutesDiffStops) {
					alternativeRoute = false;
					break;
				}
			}
			if (alternativeRoute && sumDiffs < ctx.cfg.combineAltRoutesSumDiffStops) {
				fastRoute.alternativeRoutes.add(testRoute);
			}
		}
		return alternativeRoute;
	}

	private boolean sameRouteWithExtraSegments(TransportRouteResult fastRoute, TransportRouteResult testRoute) {
//		if (testRoute.segments.size() < fastRoute.segments.size()) {
//			return false;
//		}
		if (testRoute.getFilteredChanges() < fastRoute.getFilteredChanges()) {
			return false;
		}
		int j = 0;
		boolean sameRouteWithExtraSegments = true;
		for (int i = 0; i < fastRoute.segments.size(); i++, j++) {
			TransportRouteResultSegment fs = fastRoute.segments.get(i);
			while (j < testRoute.segments.size()) {
				TransportRouteResultSegment ts = testRoute.segments.get(j);
				if (fs.route.getId().longValue() != ts.route.getId().longValue()) {
					j++;
				} else {
					break;
				}
			}
			if (j >= testRoute.segments.size()) {
				sameRouteWithExtraSegments = false;
				break;
			}
		}
		return sameRouteWithExtraSegments;
	}

	private static class SegmentsComparator implements Comparator<TransportRouteSegment> {

		public SegmentsComparator() {
		}

		@Override
		public int compare(TransportRouteSegment o1, TransportRouteSegment o2) {
			int cmpDist = Double.compare(o1.distFromStart, o2.distFromStart);
			return cmpDist == 0 ? Long.compare(o1.getId() + o1.nonce, o2.getId() + o2.nonce) : cmpDist;
		}
	}

	private static class FerrySegmentsComparator implements Comparator<TransportRouteResult> {

		public FerrySegmentsComparator() {
		}

		@Override
		public int compare(TransportRouteResult o1, TransportRouteResult o2) {

			//prefer more ferry segments. less walking distance
			int cmpFerryCount = Integer.compare(o2.getFerryCount(), o1.getFerryCount());
			return cmpFerryCount == 0 ? Double.compare(o1.getWalkDist(), o2.getWalkDist()) : cmpFerryCount;
		}
	}
	
	public static class TransportRouteResultSegment {
		
		private static final boolean DISPLAY_FULL_SEGMENT_ROUTE = false;
		private static final int DISPLAY_SEGMENT_IND = 0;
		public TransportRoute route;
		public double walkTime;
		public double travelDistApproximate;
		public double travelTime;
		public int start;
		public int end;
		public double walkDist ;
		public int depTime;
		
		public List<TransportRouteResultSegment> alternatives = new ArrayList<>();
		
		public TransportRouteResultSegment() {
		}
		
		public int getArrivalTime() {
			if(route.getSchedule() != null && depTime != -1) {
				int tm = depTime;
				TIntArrayList intervals = route.getSchedule().avgStopIntervals;
				for(int i = start; i <= end; i++) {
					if(i == end) {
						return tm;
					}
					if(intervals.size() > i) {
						tm += intervals.get(i); 
					} else {
						break;
					}
				}
			}
			return -1;
		}
		
		public double getTravelTime() {
			return travelTime;
		}
		
		public TransportStop getStart() {
			return route.getForwardStops().get(start);
		}
		
		public TransportStop getEnd() {
			return route.getForwardStops().get(end);
		}

		public List<TransportStop> getTravelStops() {
			return route.getForwardStops().subList(start, end + 1);
		}

		public QuadRect getSegmentRect() {
			double left = 0, right = 0;
			double top = 0, bottom = 0;
			for (Node n : getNodes()) {
				if (left == 0 && right == 0) {
					left = n.getLongitude();
					right = n.getLongitude();
					top = n.getLatitude();
					bottom = n.getLatitude();
				} else {
					left = Math.min(left, n.getLongitude());
					right = Math.max(right, n.getLongitude());
					top = Math.max(top, n.getLatitude());
					bottom = Math.min(bottom, n.getLatitude());
				}
			}
			return left == 0 && right == 0 ? null : new QuadRect(left, top, right, bottom);
		}

		public List<Node> getNodes() {
			List<Node> nodes = new ArrayList<>();
			List<Way> ways = getGeometry();
			for (Way way : ways) {
				nodes.addAll(way.getNodes());
			}
			return nodes;
		}
		
		private static class SearchNodeInd { 
			int ind = -1;
			Way way = null;
			double dist = MIN_DIST_STOP_TO_GEOMETRY;
		}

		public List<Way> getGeometry() {
			route.mergeForwardWays();
			if (DISPLAY_FULL_SEGMENT_ROUTE) {
				System.out.println("TOTAL SEGMENTS: " + route.getForwardWays().size());
				if (route.getForwardWays().size() > DISPLAY_SEGMENT_IND && DISPLAY_SEGMENT_IND != -1) {
					return Collections.singletonList(route.getForwardWays().get(DISPLAY_SEGMENT_IND));
				}
				return route.getForwardWays();				
			}
			List<Way> ways = route.getForwardWays();
			
			final LatLon startLoc = getStart().getLocation();
			final LatLon endLoc = getEnd().getLocation();
			SearchNodeInd startInd = new SearchNodeInd();
			SearchNodeInd endInd = new SearchNodeInd();
			for (int i = 0;  i < ways.size() ; i++) {
				List<Node> nodes = ways.get(i).getNodes();
				for (int j = 0; j < nodes.size(); j++) {
					Node n = nodes.get(j);
					if (MapUtils.getDistance(startLoc, n.getLatitude(), n.getLongitude()) < startInd.dist) {
						startInd.dist = MapUtils.getDistance(startLoc, n.getLatitude(), n.getLongitude());
						startInd.ind = j;
						startInd.way = ways.get(i);
					}
					if (MapUtils.getDistance(endLoc, n.getLatitude(), n.getLongitude()) < endInd.dist) {
						endInd.dist = MapUtils.getDistance(endLoc, n.getLatitude(), n.getLongitude());
						endInd.ind = j;
						endInd.way = ways.get(i);
					} 
				}
			}
			boolean validOneWay = startInd.way != null && startInd.way == endInd.way && startInd.ind <= endInd.ind;
			if (validOneWay) {
				Way way = new Way(GEOMETRY_WAY_ID);
				for (int k = startInd.ind; k <= endInd.ind; k++) {
					way.addNode(startInd.way.getNodes().get(k));
				}
				return Collections.singletonList(way);
			}
			boolean validContinuation = startInd.way != null && endInd.way != null &&
					startInd.way != endInd.way;
			if (validContinuation) {
				Node ln = startInd.way.getLastNode();
				Node fn = endInd.way.getFirstNode();
				// HERE we need to check other ways for continuation
				if (ln != null && fn != null && MapUtils.getDistance(ln.getLatLon(), fn.getLatLon()) < TransportStopsRouteReader.MISSING_STOP_SEARCH_RADIUS) {
					validContinuation = true;
				} else {
					validContinuation = false;
				}
			}
			if (validContinuation) {
				List<Way> two = new ArrayList<Way>();
				Way way = new Way(GEOMETRY_WAY_ID);
				for (int k = startInd.ind; k < startInd.way.getNodes().size(); k++) {
					way.addNode(startInd.way.getNodes().get(k));
				}
				two.add(way);
				way = new Way(GEOMETRY_WAY_ID);
				for (int k = 0; k <= endInd.ind; k++) {
					way.addNode(endInd.way.getNodes().get(k));
				}
				two.add(way);
				return two;
			}
			Way way = new Way(STOPS_WAY_ID);
			for (int i = start; i <= end; i++) {
				LatLon l = getStop(i).getLocation();
				Node n = new Node(l.getLatitude(), l.getLongitude(), -1);
				way.addNode(n);
			}
			return Collections.singletonList(way);
		}
		
		public double getTravelDist() {
			double d = 0;
			for (int k = start; k < end; k++) {
				d += MapUtils.getDistance(route.getForwardStops().get(k).getLocation(),
						route.getForwardStops().get(k + 1).getLocation());
			}
			return d;
		}

		public TransportStop getStop(int i) {
			return route.getForwardStops().get(i);
		}
	}

	public static String formatTransportTime(int i) {
		int h = i / 60 / 6;
		int mh = i - h * 60 * 6;
		int m = mh / 6;
		int s = (mh - m * 6) * 10;
		return String.format(Locale.US, "%02d:%02d:%02d ", h, m, s);
	}
	
	public static class TransportRouteSegment {

		long nonce;
		final int segStart;
		final TransportRoute road;
		final int departureTime;
		private static final int SHIFT = 10; // assume less than 1024 stops
		private static final int SHIFT_DEPTIME = 14; // assume less than 1024 stops
		
		TransportRouteSegment parentRoute = null;
		int parentStop; // last stop to exit for parent route
		double parentTravelTime; // travel time for parent route
		double parentTravelDist; // travel distance for parent route (inaccurate) 
		// walk distance to start route location (or finish in case last segment)
		double walkDist = 0;
		// main field accumulated all time spent from beginning of journey
		double distFromStart = 0;
		
		public TransportRouteSegment(TransportRoute road, int stopIndex) {
			this.road = road;
			this.segStart = (short) stopIndex;
			this.departureTime = -1;
		}
		
		public TransportRouteSegment(TransportRoute road, int stopIndex, int depTime) {
			this.road = road;
			this.segStart = (short) stopIndex;
			this.departureTime = depTime;
		}
		
		public TransportRouteSegment(TransportRouteSegment c) {
			this.road = c.road;
			this.segStart = c.segStart;
			this.departureTime = c.departureTime;
		}

		public boolean wasVisited(TransportRouteSegment rrs) {
			if (rrs.road.getId().longValue() == road.getId().longValue() && 
					rrs.departureTime == departureTime) {
				return true;
			}
			if(parentRoute != null) {
				return parentRoute.wasVisited(rrs);
			}
			return false;
		}

		public TransportStop getStop(int i) {
			return road.getForwardStops().get(i);
		}

		public LatLon getLocation() {
			return road.getForwardStops().get(segStart).getLocation();
		}

		public int getLength() {
			return road.getForwardStops().size();
		}
		
		public long getId() {
			long l = road.getId();

			l = l << SHIFT_DEPTIME;
			if (departureTime >= (1 << SHIFT_DEPTIME)) {
				throw new IllegalStateException("too long dep time" + departureTime);
			}
			l += (departureTime + 1);

			l = l << SHIFT;
			if (segStart >= (1 << SHIFT)) {
				throw new IllegalStateException("too many stops " + road.getId() + " " + segStart);
			}
			l += segStart;

			if (l < 0) {
				throw new IllegalStateException("too long id " + road.getId());
			}
			return l;
		}

		public int getDepth() {
			if(parentRoute != null) {
				return parentRoute.getDepth() + 1;
			}
			return 1;
		}
		
		@Override
		public String toString() {
			return String.format("Route: %s, stop: %s %s", road.getName(), road.getForwardStops().get(segStart).getName(),
					departureTime == -1 ? "" : formatTransportTime(departureTime) );
		}
	}

	public static List<TransportRouteResult> convertToTransportRoutingResult(NativeTransportRoutingResult[] res,
			TransportRoutingConfiguration cfg) {
		// cache for converted TransportRoutes:
		TLongObjectHashMap<TransportRoute> convertedRoutesCache = new TLongObjectHashMap<>();
		TLongObjectHashMap<TransportStop> convertedStopsCache = new TLongObjectHashMap<>();

		if (res.length == 0) {
			LOG.info("Public transport. No route found");
			return new ArrayList<TransportRouteResult>();
		}
		List<TransportRouteResult> convertedRes = new ArrayList<TransportRouteResult>();
		for (NativeTransportRoutingResult ntrr : res) {
			TransportRouteResult trr = new TransportRouteResult(cfg);
			trr.setFinishWalkDist(ntrr.finishWalkDist);
			trr.setRouteTime(ntrr.routeTime);

			trr.getSegments().addAll(
					convertToTransportRouteResultSegment(ntrr.segments, convertedRoutesCache, convertedStopsCache));

			// alternativeRoutes (1-level recursion)
			if (ntrr.alternativeRoutes != null && ntrr.alternativeRoutes.length > 0) {
				trr.alternativeRoutes = convertToTransportRoutingResult(ntrr.alternativeRoutes, cfg);
			}

			convertedRes.add(trr);
		}
		convertedStopsCache.clear();
		convertedRoutesCache.clear();
		return convertedRes;
	}

	private static List<TransportRouteResultSegment> convertToTransportRouteResultSegment(
			NativeTransportRouteResultSegment[] nativeSegments, TLongObjectHashMap<TransportRoute> convertedRoutesCache,
			TLongObjectHashMap<TransportStop> convertedStopsCache) {
		List<TransportRouteResultSegment> results = new ArrayList<>();
		if (nativeSegments != null) {
			for (NativeTransportRouteResultSegment ntrs : nativeSegments) {
				TransportRouteResultSegment trs = new TransportRouteResultSegment();

				trs.route = convertTransportRoute(ntrs.route, convertedRoutesCache, convertedStopsCache);
				trs.walkTime = ntrs.walkTime;
				trs.travelDistApproximate = ntrs.travelDistApproximate;
				trs.travelTime = ntrs.travelTime;
				trs.start = ntrs.start;
				trs.end = ntrs.end;
				trs.walkDist = ntrs.walkDist;
				trs.depTime = ntrs.depTime;

				// alternatives (1-level recursion)
				if (ntrs.alternatives != null && ntrs.alternatives.length > 0) {
					trs.alternatives = convertToTransportRouteResultSegment(ntrs.alternatives,
							convertedRoutesCache, convertedStopsCache);
				}

				results.add(trs);
			}
		}
		return results;
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
