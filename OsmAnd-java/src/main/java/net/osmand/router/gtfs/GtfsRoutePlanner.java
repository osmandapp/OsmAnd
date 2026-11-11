package net.osmand.router.gtfs;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gnu.trove.list.array.TIntArrayList;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;

import org.apache.commons.logging.Log;

/**
 * Earliest-arrival planner over a {@link GtfsIndex}, using the Connection Scan Algorithm
 * (Dibbelt, Pajor, Strasser, Wagner, "Intriguingly Simple and Fast Transit Routing"): the connections
 * (one vehicle hop between consecutive stops) are scanned once in departure order from the requested
 * time, and the earliest arrival at every stop is relaxed as they go.
 * <ul>
 * <li>Three service days are scanned together (yesterday, today, tomorrow) so that trips past midnight
 * and late-evening queries work; DST days are handled by {@link GtfsCalendar}.</li>
 * <li>Changing vehicles needs the transfer buffer, or the minimum time of transfers.txt. Footpaths
 * (transfers.txt and straight-line walks up to 400 m) are taken after leaving a vehicle and are not chained.</li>
 * <li>Boarding where pickup is not allowed, or alighting where drop-off is not allowed, is never planned.</li>
 * <li>Among equal arrival times the label with fewer rides wins, so the planner does not ride a different
 * line backwards just to catch a trip it could board at the start.</li>
 * <li>Alternatives: the best plan again without the route of its first ride, and again with a penalty per
 * transfer.</li>
 * </ul>
 * Walks are straight-line estimates; the caller replaces them with real pedestrian routes. A planner is
 * immutable after construction and {@link #plan} may be called from several threads.
 */
public final class GtfsRoutePlanner {

	private static final Log LOG = PlatformUtil.getLog(GtfsRoutePlanner.class);

	private static final int INF = Integer.MAX_VALUE / 2;
	private static final int CANCEL_CHECK_CONNECTIONS = 1 << 16;
	private static final int ACCESS = 0, BOARD = 1, RIDE = 2, FOOT = 3; // label kinds

	private final GtfsIndex index;
	private final int[] stopLat, stopLon;
	private final GtfsStopGrid grid;

	public GtfsRoutePlanner(GtfsIndex index) {
		this.index = index;
		int ns = index.getStopCount();
		stopLat = new int[ns];
		stopLon = new int[ns];
		TIntArrayList all = new TIntArrayList();
		for (int s = 0; s < ns; s++) {
			stopLat[s] = index.stopLat.get(s);
			stopLon[s] = index.stopLon.get(s);
			all.add(s);
		}
		grid = new GtfsStopGrid(stopLat, stopLon, all);
	}

	/**
	 * Itineraries from a to b leaving no earlier than the given time, best first. The list is empty when
	 * nothing is found within the options' horizon or when the progress listener cancelled the search.
	 * Whether walking the whole way is better is for the caller to judge.
	 */
	public List<GtfsItinerary> plan(LatLon from, LatLon to, long departEpochSec, GtfsPlanOptions options,
	                                GtfsProgressListener progress) {
		long t0 = System.nanoTime();
		Query q = new Query(from, to, departEpochSec, options == null ? new GtfsPlanOptions() : options, progress);
		List<GtfsItinerary> result = new ArrayList<>();
		if (q.cancelled()) {
			return result;
		}
		GtfsItinerary best = q.scan(q.active, 0);
		if (best == null || q.cancelled()) {
			return result;
		}
		result.add(best);
		Set<String> seen = new HashSet<>();
		seen.add(signature(best));
		if (q.o.maxItineraries > 1) {
			if (progress != null) {
				progress.onProgress("Alternatives", 50);
			}
			GtfsItinerary other = q.scan(q.without(firstRoute(best)), 0);
			addAlternative(result, seen, other, best, q.o);
			if (q.o.maxItineraries > 2 && !q.cancelled()) {
				addAlternative(result, seen, q.scan(q.active, q.o.transferPenaltySec), best, q.o);
			}
		}
		if (q.cancelled()) {
			result.clear();
			return result;
		}
		Collections.sort(result, (x, y) -> x.arriveSec != y.arriveSec ? Long.compare(x.arriveSec, y.arriveSec)
				: Integer.compare(x.transfers, y.transfers));
		while (result.size() > Math.max(1, q.o.maxItineraries)) {
			result.remove(result.size() - 1);
		}
		LOG.debug("GTFS plan: " + result.size() + " itineraries, " + q.scanned + " connections scanned, "
				+ (System.nanoTime() - t0) / 1000000 + " ms");
		return result;
	}

	private static void addAlternative(List<GtfsItinerary> result, Set<String> seen, GtfsItinerary alt,
	                                   GtfsItinerary best, GtfsPlanOptions o) {
		if (alt != null && alt.arriveSec - best.arriveSec <= o.maxAlternativeDelaySec && seen.add(signature(alt))) {
			result.add(alt);
		}
	}

	private static String signature(GtfsItinerary it) {
		StringBuilder sb = new StringBuilder();
		for (GtfsLeg l : it.legs) {
			if (l.type == GtfsLeg.Type.RIDE) {
				sb.append(l.tripIdx).append(':').append(l.boardSeq).append(':').append(l.alightSeq).append(';');
			}
		}
		return sb.toString();
	}

	private int firstRoute(GtfsItinerary it) {
		for (GtfsLeg l : it.legs) {
			if (l.type == GtfsLeg.Type.RIDE) {
				return l.routeIdx;
			}
		}
		return -1;
	}

	/** State of one plan() call: the three service days and the walkable stops around both ends. */
	private final class Query {
		final GtfsPlanOptions o;
		final GtfsProgressListener progress;
		final LatLon from, to;
		final long departEpoch;
		final long dayStart; // epoch second of service time 0 of "today"
		final int departSec; // relative to dayStart, may be negative on a DST day
		final int[] offsets; // start of yesterday/today/tomorrow's service day relative to dayStart
		final long[][] active = new long[3][];
		final int[] accessWalk, egressWalk; // seconds, -1 if the stop is out of reach
		long scanned;

		// labels of the scan in progress
		final TIntArrayList kind = new TIntArrayList(), a = new TIntArrayList(), b = new TIntArrayList(),
				rides = new TIntArrayList(), time = new TIntArrayList(), day = new TIntArrayList();

		Query(LatLon from, LatLon to, long departEpoch, GtfsPlanOptions o, GtfsProgressListener progress) {
			this.o = o;
			this.progress = progress;
			this.from = from;
			this.to = to;
			this.departEpoch = departEpoch;
			GtfsCalendar cal = index.getCalendar();
			LocalDate today = cal.localDate(departEpoch);
			dayStart = cal.serviceDayStart(today);
			departSec = (int) (departEpoch - dayStart);
			offsets = cal.cursorOffsets(today);
			for (int k = 0; k < 3; k++) {
				active[k] = cal.activeTrips(today.plusDays(k - 1), index.tripService, index.getTripCount());
			}
			int ns = index.getStopCount();
			accessWalk = new int[ns];
			egressWalk = new int[ns];
			Arrays.fill(accessWalk, -1);
			Arrays.fill(egressWalk, -1);
			fill(accessWalk, from);
			fill(egressWalk, to);
		}

		private void fill(int[] walk, LatLon p) {
			int lat = (int) Math.round(p.getLatitude() * 1e7), lon = (int) Math.round(p.getLongitude() * 1e7);
			TIntArrayList near = new TIntArrayList();
			grid.within(lat, lon, o.maxWalkMeters, near);
			for (int i = 0; i < near.size(); i++) {
				int s = near.get(i);
				walk[s] = walkSeconds(GtfsIndex.distanceMeters(lat, lon, stopLat[s], stopLon[s]));
			}
		}

		int walkSeconds(double meters) {
			return (int) Math.round(meters * o.walkDetourFactor / o.walkSpeedMps);
		}

		boolean cancelled() {
			return progress != null && progress.isCancelled();
		}

		/** Trip sets of the three days without the trips of one route. */
		long[][] without(int route) {
			long[][] res = new long[3][];
			for (int k = 0; k < 3; k++) {
				res[k] = active[k].clone();
			}
			for (int t = 0; route >= 0 && t < index.getTripCount(); t++) {
				if (index.tripRoute.get(t) == route) {
					for (int k = 0; k < 3; k++) {
						res[k][t >> 6] &= ~(1L << (t & 63));
					}
				}
			}
			return res;
		}

		private int newLabel(int k, int x, int y, int ridesSoFar, int t, int d) {
			kind.add(k);
			a.add(x);
			b.add(y);
			rides.add(ridesSoFar);
			time.add(t);
			day.add(d);
			return kind.size() - 1;
		}

		/** Scans the connections and returns the best itinerary, or null. */
		GtfsItinerary scan(long[][] trips, int transferPenalty) {
			kind.clear();
			a.clear();
			b.clear();
			rides.clear();
			time.clear();
			day.clear();
			int ns = index.getStopCount(), nt = index.getTripCount(), nc = index.getConnectionCount();
			int[] arrival = new int[ns], arrivalLabel = new int[ns], ready = new int[ns], readyLabel = new int[ns];
			Arrays.fill(arrival, INF);
			Arrays.fill(ready, INF);
			Arrays.fill(arrivalLabel, -1);
			Arrays.fill(readyLabel, -1);
			for (int s = 0; s < ns; s++) {
				if (accessWalk[s] >= 0) {
					int t = departSec + accessWalk[s];
					arrival[s] = ready[s] = t;
					arrivalLabel[s] = readyLabel[s] = newLabel(ACCESS, s, -1, 0, t, 1);
				}
			}
			int[] boarding = new int[nt * 3]; // label of the boarding of a trip on a service day
			Arrays.fill(boarding, -1);
			int[] pos = new int[3];
			for (int k = 0; k < 3; k++) {
				pos[k] = index.firstConnectionAt(departSec - offsets[k]);
			}
			int limit = departSec + o.horizonSec;
			int bestArrival = INF, bestLabel = -1, bestStop = -1;
			while (true) {
				int k = -1, eff = Integer.MAX_VALUE;
				for (int i = 0; i < 3; i++) {
					if (pos[i] < nc && index.connDep.get(pos[i]) + offsets[i] < eff) {
						eff = index.connDep.get(pos[i]) + offsets[i];
						k = i;
					}
				}
				if (k < 0 || eff > limit || eff >= bestArrival) {
					break;
				}
				int c = pos[k]++;
				if ((++scanned & (CANCEL_CHECK_CONNECTIONS - 1)) == 0 && cancelled()) {
					return null;
				}
				int packed = index.connTrip.get(c), trip = packed >>> 2;
				if ((trips[k][trip >> 6] & (1L << (trip & 63))) == 0) {
					continue;
				}
				int ti = trip * 3 + k, f = index.connFrom.get(c);
				int board = boarding[ti];
				if ((packed & 1) == 0 && ready[f] <= eff
						&& (board < 0 || rides.get(readyLabel[f]) + 1 < rides.get(board))) {
					// first reachable boarding of the trip, or one with fewer rides before it
					board = boarding[ti] = newLabel(BOARD, c, readyLabel[f], rides.get(readyLabel[f]) + 1, eff, k);
				}
				if (board < 0 || (packed & 2) != 0) {
					continue;
				}
				int stop = index.connTo.get(c), at = index.connArr.get(c) + offsets[k], r = rides.get(board);
				boolean improved = before(at, r, arrival[stop], arrivalLabel[stop]);
				int ride = -1;
				if (improved) {
					ride = newLabel(RIDE, c, board, r, at, k);
					arrival[stop] = at;
					arrivalLabel[stop] = ride;
				}
				int stay = index.stopMinTransfer.get(stop) > 0 ? index.stopMinTransfer.get(stop) : o.transferBufferSec;
				if (before(at + stay + transferPenalty, r, ready[stop], readyLabel[stop])) {
					ride = ride >= 0 ? ride : newLabel(RIDE, c, board, r, at, k);
					ready[stop] = at + stay + transferPenalty;
					readyLabel[stop] = ride;
				}
				if (!improved) {
					continue; // footpaths from an earlier arrival at this stop are at least as good
				}
				if (egressWalk[stop] >= 0 && before(at + egressWalk[stop], r, bestArrival, bestLabel)) {
					bestArrival = at + egressWalk[stop];
					bestLabel = ride;
					bestStop = stop;
				}
				for (int e = index.footOffset.get(stop), end = index.footOffset.get(stop + 1); e < end; e++) {
					int v = index.footValue.get(e), n = index.footTo.get(e), w = v >> 1;
					boolean listed = (v & 1) == 1; // minimum transfer time from transfers.txt
					if (!listed && w > o.maxTransferWalkMeters) {
						continue;
					}
					int arrive = at + (listed ? w : walkSeconds(w));
					int readyAt = arrive + (listed ? 0 : o.transferBufferSec) + transferPenalty;
					boolean newArrival = before(arrive, r, arrival[n], arrivalLabel[n]);
					boolean newReady = before(readyAt, r, ready[n], readyLabel[n]);
					if (!newArrival && !newReady) {
						continue;
					}
					int foot = newLabel(FOOT, n, ride, r, arrive, k);
					if (newArrival) {
						arrival[n] = arrive;
						arrivalLabel[n] = foot;
						if (egressWalk[n] >= 0 && before(arrive + egressWalk[n], r, bestArrival, bestLabel)) {
							bestArrival = arrive + egressWalk[n];
							bestLabel = foot;
							bestStop = n;
						}
					}
					if (newReady) {
						ready[n] = readyAt;
						readyLabel[n] = foot;
					}
				}
			}
			if (bestLabel < 0) {
				return null;
			}
			return itinerary(bestLabel, bestStop, bestArrival, rides.get(bestLabel));
		}

		/** Earlier time wins, then fewer rides. */
		private boolean before(int t, int ridesOfNew, int oldTime, int oldLabel) {
			return t < oldTime || (t == oldTime && oldLabel >= 0 && ridesOfNew < rides.get(oldLabel));
		}

		// ---------------------------------------------------------------- result

		private LatLon stopPoint(int stop) {
			return new LatLon(stopLat[stop] / 1e7, stopLon[stop] / 1e7);
		}

		private long epoch(int sec) {
			return dayStart + sec;
		}

		GtfsItinerary itinerary(int last, int lastStop, int arrival, int rideCount) {
			List<GtfsLeg> legs = new ArrayList<>();
			long end = epoch(arrival);
			int egress = egressWalk[lastStop];
			legs.add(GtfsLeg.walk(end - egress, end, lastStop, -1, stopPoint(lastStop), to,
					(int) Math.round(distance(stopLat[lastStop], stopLon[lastStop], to))));
			int cur = last;
			while (true) {
				int k = kind.get(cur);
				if (k == ACCESS) {
					int s = a.get(cur);
					legs.add(GtfsLeg.walk(epoch(time.get(cur)) - accessWalk[s], epoch(time.get(cur)), -1, s, from,
							stopPoint(s), (int) Math.round(distance(stopLat[s], stopLon[s], from))));
					break;
				} else if (k == FOOT) {
					int ride = b.get(cur), fromStop = index.connTo.get(a.get(ride)), toStop = a.get(cur);
					legs.add(GtfsLeg.walk(epoch(time.get(ride)), epoch(time.get(cur)), fromStop, toStop,
							stopPoint(fromStop), stopPoint(toStop), (int) Math.round(GtfsIndex.distanceMeters(
							stopLat[fromStop], stopLon[fromStop], stopLat[toStop], stopLon[toStop]))));
					cur = ride;
				} else { // RIDE: the boarding label tells where it began and what came before
					int board = b.get(cur);
					legs.add(rideLeg(a.get(board), a.get(cur), day.get(board)));
					cur = b.get(board);
				}
			}
			Collections.reverse(legs);
			GtfsLeg first = legs.get(0), second = legs.size() > 1 ? legs.get(1) : null;
			if (first.type == GtfsLeg.Type.WALK && second != null && second.startSec > departEpoch) {
				// leave as late as possible: the wait happens at home, not at the stop
				long start = Math.max(departEpoch, second.startSec - (first.endSec - first.startSec));
				legs.set(0, first.withTimes(start, start + (first.endSec - first.startSec)));
			}
			legs.removeIf(l -> l.type == GtfsLeg.Type.WALK && l.startSec == l.endSec && l.walkMeters == 0);
			return new GtfsItinerary(legs, legs.get(0).startSec, legs.get(legs.size() - 1).endSec, rideCount - 1);
		}

		private double distance(int lat, int lon, LatLon p) {
			return GtfsIndex.distanceMeters(lat, lon, (int) Math.round(p.getLatitude() * 1e7),
					(int) Math.round(p.getLongitude() * 1e7));
		}

		/** Ride from the boarding connection to the alighting one, with the stops in between. */
		private GtfsLeg rideLeg(int boardConn, int alightConn, int k) {
			int trip = index.connTrip.get(boardConn) >>> 2, first = index.tripFirst.get(trip),
					count = index.tripCount.get(trip);
			int fromStop = index.connFrom.get(boardConn), toStop = index.connTo.get(alightConn);
			int boardSeq = -1, alightSeq = -1;
			for (int i = 0; i < count && alightSeq < 0; i++) {
				int stop = index.stopTimeStop.get(first + i) & GtfsIndex.STOP_MASK;
				if (boardSeq < 0 && stop == fromStop && index.stopTimeDep.get(first + i) == index.connDep.get(boardConn)) {
					boardSeq = i;
				} else if (boardSeq >= 0 && stop == toStop && index.stopTimeArr.get(first + i) == index.connArr.get(alightConn)) {
					alightSeq = i;
				}
			}
			int n = alightSeq - boardSeq + 1;
			int[] stops = new int[n];
			long[] arr = new long[n], dep = new long[n];
			for (int i = 0; i < n; i++) {
				int e = first + boardSeq + i;
				stops[i] = index.stopTimeStop.get(e) & GtfsIndex.STOP_MASK;
				arr[i] = epoch(index.stopTimeArr.get(e) + offsets[k]);
				dep[i] = epoch(index.stopTimeDep.get(e) + offsets[k]);
			}
			int route = index.tripRoute.get(trip);
			return GtfsLeg.ride(trip, route, boardSeq, alightSeq, stops, arr, dep, stopPoint(fromStop),
					stopPoint(toStop), index.getTripId(trip), index.getRouteId(route));
		}
	}
}
