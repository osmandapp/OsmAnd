package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.IncompleteTransportRoute;
import net.osmand.data.LatLon;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.edit.Way;
import net.osmand.util.MapUtils;

public class TransportStopsRouteReader {
	public static final int MISSING_STOP_SEARCH_RADIUS = 50000;
	TLongObjectHashMap<TransportRoute> combinedRoutesCache = new TLongObjectHashMap<TransportRoute>();
	Map<BinaryMapIndexReader, TLongObjectHashMap<TransportRoute>> routesFilesCache = new LinkedHashMap<BinaryMapIndexReader, 
			TLongObjectHashMap<TransportRoute>>();
	
	
	public TransportStopsRouteReader(Collection<BinaryMapIndexReader> fls) {
		for(BinaryMapIndexReader r : fls) {
			routesFilesCache.put(r, new TLongObjectHashMap<TransportRoute>());
		}
	}
	
	public Collection<TransportStop> readMergedTransportStops(SearchRequest<TransportStop> sr) throws IOException {
		TLongObjectHashMap<TransportStop> loadedTransportStops = new TLongObjectHashMap<TransportStop>();

		for (BinaryMapIndexReader r : routesFilesCache.keySet()) {
			sr.clearSearchResults();
			List<TransportStop> stops = r.searchTransportIndex(sr);
			TLongObjectHashMap<TransportRoute> routesToLoad = mergeTransportStops(r, loadedTransportStops, stops);
			loadRoutes(r, routesToLoad);
			for (TransportStop stop : stops) {
				// skip missing stops
				if (stop.isMissingStop()) {
					continue;
				}
				long stopId = stop.getId();
				TransportStop multifileStop = loadedTransportStops.get(stopId);
				long[] rrs = stop.getReferencesToRoutes();
				// clear up so it won't be used because there is multi file stop
				stop.setReferencesToRoutes(null);
				if (rrs != null && !multifileStop.isDeleted()) {
					for (long rr : rrs) {
						TransportRoute route = routesToLoad.get(rr);
						if (route == null) {
							if (routesToLoad.containsKey(rr)) {
								System.err.println(String.format(
										"Something went wrong by loading combined route %d for stop %s", rr, stop));
							}
						} else {
							TransportRoute combinedRoute = getCombinedRoute(route);
							if (multifileStop == stop || (!multifileStop.hasRoute(combinedRoute.getId())
									&& !multifileStop.isRouteDeleted(combinedRoute.getId()))) {
								// duplicates won't be added
								multifileStop.addRouteId(combinedRoute.getId());
								multifileStop.addRoute(combinedRoute);
							}
						}
					}
				}
			}
		}
		// There should go stops with complete routes:
		return loadedTransportStops.valueCollection();
						
	}
	

	public TLongObjectHashMap<TransportRoute> mergeTransportStops(BinaryMapIndexReader reader,
			TLongObjectHashMap<TransportStop> loadedTransportStops, List<TransportStop> stops) throws IOException {
		Iterator<TransportStop> it = stops.iterator();
		TLongObjectHashMap<TransportRoute> routesToLoad = routesFilesCache.get(reader);
		
		while (it.hasNext()) {
			TransportStop stop = it.next();
			long stopId = stop.getId();
			TransportStop multifileStop = loadedTransportStops.get(stopId);
			long[] routesIds = stop.getRoutesIds();
			long[] delRIds = stop.getDeletedRoutesIds();
			if (multifileStop == null) {
				loadedTransportStops.put(stopId, stop);
				multifileStop = stop;
				if (!stop.isDeleted()) {
					putAll(routesToLoad, stop.getReferencesToRoutes());
				}
			} else if (multifileStop.isDeleted()) {
				// stop has nothing to load, so not needed
				it.remove();
			} else {
				if (delRIds != null) {
					for (long deletedRouteId : delRIds) {
						multifileStop.addDeletedRouteId(deletedRouteId);
					}
				}
				if (routesIds != null && routesIds.length > 0) {
					long[] refs = stop.getReferencesToRoutes();
					for (int i = 0; i < routesIds.length; i++) {
						long routeId = routesIds[i];
						if (!multifileStop.hasRoute(routeId) && !multifileStop.isRouteDeleted(routeId)) {
							if(!routesToLoad.containsKey(refs[i])) {
								routesToLoad.put(refs[i], null);
							}
						}
					}
				} else {
					if (stop.hasReferencesToRoutes()) {
						// old format
						putAll(routesToLoad, stop.getReferencesToRoutes());
					} else {
						// stop has noting to load, so not needed
						it.remove();
					}
				}
			}
		}
		
		return routesToLoad;
	}

	private void putAll(TLongObjectHashMap<TransportRoute> routesToLoad, long[] referencesToRoutes) {
		for(int k = 0 ; k < referencesToRoutes.length; k++) {
			if(!routesToLoad.containsKey(referencesToRoutes[k])) {
				routesToLoad.put(referencesToRoutes[k], null);
			}
		}
	}

	public void loadRoutes(BinaryMapIndexReader reader, TLongObjectHashMap<TransportRoute> localFileRoutes) throws IOException {
		// load/combine routes
		if (localFileRoutes.size() > 0) {
			TLongArrayList routesToLoad = new TLongArrayList(localFileRoutes.size()); 
			TLongObjectIterator<TransportRoute> it = localFileRoutes.iterator();
			while(it.hasNext()) {
				it.advance();
				if(it.value() == null) {
					routesToLoad.add(it.key());
				}
			}
			routesToLoad.sort();
			reader.loadTransportRoutes(routesToLoad.toArray(), localFileRoutes);
		}
	}

	private TransportRoute getCombinedRoute(TransportRoute route) throws IOException {
		if (!route.isIncomplete()) {
			return route;
		}
		TransportRoute c = combinedRoutesCache.get(route.getId());
		if (c == null) {
			c = combineRoute(route);
			combinedRoutesCache.put(route.getId(), c);
		}
		return c;
	}

	private TransportRoute combineRoute(TransportRoute route) throws IOException {
		// 1. Get all available route parts;
		List<TransportRoute> incompleteRoutes = findIncompleteRouteParts(route);
		if (incompleteRoutes == null) {
			return route;
		}
		// here could be multiple overlays between same points
		// It's better to remove them especially identical segments
		List<Way> allWays = getAllWays(incompleteRoutes);

		// 2. Get array of segments (each array size > 1):
		LinkedList<List<TransportStop>> stopSegments = parseRoutePartsToSegments(incompleteRoutes);

		// 3. Merge segments and remove excess missingStops (when they are closer then MISSING_STOP_SEARCH_RADIUS):
		// + Check for missingStops. If they present in the middle/there more then one segment - we have a hole in the
		// map data
		List<List<TransportStop>> mergedSegments = combineSegmentsOfSameRoute(stopSegments);

		// 4. Now we need to properly sort segments, proper sorting is minimizing distance between stops
		// So it is salesman problem, we have this solution at TspAnt, but if we know last or first segment we can solve
		// it straightforward
		List<TransportStop> firstSegment = null;
		List<TransportStop> lastSegment = null;
		for (List<TransportStop> l : mergedSegments) {
			if (!l.get(0).isMissingStop()) {
				firstSegment = l;
			}
			if (!l.get(l.size() - 1).isMissingStop()) {
				lastSegment = l;
			}
		}
		List<List<TransportStop>> sortedSegments = new ArrayList<List<TransportStop>>();
		if (firstSegment != null) {
			sortedSegments.add(firstSegment);
			mergedSegments.remove(firstSegment);
			while (!mergedSegments.isEmpty()) {
				List<TransportStop> last = sortedSegments.get(sortedSegments.size() - 1);
				List<TransportStop> add = findAndDeleteMinDistance(last.get(last.size() - 1).getLocation(),
						mergedSegments, true);
				sortedSegments.add(add);
			}

		} else if (lastSegment != null) {
			sortedSegments.add(lastSegment);
			mergedSegments.remove(lastSegment);
			while (!mergedSegments.isEmpty()) {
				List<TransportStop> first = sortedSegments.get(0);
				List<TransportStop> add = findAndDeleteMinDistance(first.get(0).getLocation(), mergedSegments, false);
				sortedSegments.add(0, add);
			}
		} else {
			sortedSegments = mergedSegments;
		}
		List<TransportStop> finalList = new ArrayList<TransportStop>();
		for (List<TransportStop> s : sortedSegments) {
			finalList.addAll(s);
		}
		// 5. Create combined TransportRoute and return it
		return new TransportRoute(route, finalList, allWays);
	}

	private List<TransportStop> findAndDeleteMinDistance(LatLon location, List<List<TransportStop>> mergedSegments,
			boolean attachToBegin) {
		int ind = attachToBegin ? 0 : mergedSegments.get(0).size() - 1;
		double minDist = MapUtils.getDistance(mergedSegments.get(0).get(ind).getLocation(), location);
		int minInd = 0;
		for (int i = 1; i < mergedSegments.size(); i++) {
			ind = attachToBegin ? 0 : mergedSegments.get(i).size() - 1;
			double dist = MapUtils.getDistance(mergedSegments.get(i).get(ind).getLocation(), location);
			if (dist < minDist) {
				minInd = i;
			}
		}
		return mergedSegments.remove(minInd);
	}

	private List<Way> getAllWays(List<TransportRoute> parts) {
		List<Way> w = new ArrayList<Way>();
		for (TransportRoute t : parts) {
			w.addAll(t.getForwardWays());
		}
		return w;
	}

	private List<List<TransportStop>> combineSegmentsOfSameRoute(LinkedList<List<TransportStop>> segments) {
		LinkedList<List<TransportStop>> tempResultSegments = mergeSegments(segments, new LinkedList<List<TransportStop>>(), false);
		return mergeSegments(tempResultSegments, new ArrayList<List<TransportStop>>(), true);
	}
	
	
	private <T extends List<List<TransportStop>>> T mergeSegments(LinkedList<List<TransportStop>> segments, T resultSegments, 
			boolean mergeMissingSegs) {
		while (!segments.isEmpty()) {
			List<TransportStop> firstSegment = segments.poll();
			boolean merged = true;
			while (merged) {
				merged = false;
				Iterator<List<TransportStop>> it = segments.iterator();
				while (it.hasNext()) {
					List<TransportStop> segmentToMerge = it.next();
					if (mergeMissingSegs) {
						merged = tryToMergeMissingStops(firstSegment, segmentToMerge);
					} else {						
						merged = tryToMerge(firstSegment, segmentToMerge);
					}

					if (merged) {
						it.remove();
						break;
					}
				}
			}
			resultSegments.add(firstSegment);
		}
		return resultSegments;
	}

	private boolean tryToMerge(List<TransportStop> firstSegment, List<TransportStop> segmentToMerge) {
		if (firstSegment.size() < 2 || segmentToMerge.size() < 2) {
			return false;
		}
		// 1st we check that segments overlap by stop
		int commonStopFirst = 0;
		int commonStopSecond = 0;
		boolean found = false;
		for (; commonStopFirst < firstSegment.size(); commonStopFirst++) {
			for (commonStopSecond = 0; commonStopSecond < segmentToMerge.size() && !found; commonStopSecond++) {
				long lid1 = firstSegment.get(commonStopFirst).getId();
				long lid2 = segmentToMerge.get(commonStopSecond).getId();
				if (lid1 > 0 && lid2 == lid1) {
					found = true;
					break;
				}
			}
			if (found) {
				// important to increment break inside loop
				break;
			}
		}
		if (found && commonStopFirst < firstSegment.size()) {
			// we've found common stop so we can merge based on stops
			// merge last part first
			int leftPartFirst = firstSegment.size() - commonStopFirst;
			int leftPartSecond = segmentToMerge.size() - commonStopSecond;
			if (leftPartFirst < leftPartSecond
					|| (leftPartFirst == leftPartSecond && firstSegment.get(firstSegment.size() - 1).isMissingStop())) {
				while (firstSegment.size() > commonStopFirst) {
					firstSegment.remove(firstSegment.size() - 1);
				}
				for (int i = commonStopSecond; i < segmentToMerge.size(); i++) {
					firstSegment.add(segmentToMerge.get(i));
				}
			}
			// merge first part
			if (commonStopFirst < commonStopSecond
					|| (commonStopFirst == commonStopSecond && firstSegment.get(0).isMissingStop())) {
				firstSegment.subList(0, commonStopFirst + 1).clear();
				for (int i = commonStopSecond; i >= 0; i--) {
					firstSegment.add(0, segmentToMerge.get(i));
				}
			}
			return true;

		}

		return false;
	}

	private boolean tryToMergeMissingStops(List<TransportStop> firstSegment, List<TransportStop> segmentToMerge) {
		// no common stops, so try to connect to the end or beginning
		// beginning
		boolean merged = false;
		if (MapUtils.getDistance(firstSegment.get(0).getLocation(),
				segmentToMerge.get(segmentToMerge.size() - 1).getLocation()) < MISSING_STOP_SEARCH_RADIUS 
				&& firstSegment.get(0).isMissingStop() && segmentToMerge.get(segmentToMerge.size() - 1).isMissingStop()) {
			firstSegment.remove(0);
			for (int i = segmentToMerge.size() - 2; i >= 0; i--) {
				firstSegment.add(0, segmentToMerge.get(i));
			}
			merged = true;
		} else if (MapUtils.getDistance(firstSegment.get(firstSegment.size() - 1).getLocation(),
				segmentToMerge.get(0).getLocation()) < MISSING_STOP_SEARCH_RADIUS
				&& segmentToMerge.get(0).isMissingStop() && firstSegment.get(firstSegment.size() - 1).isMissingStop()) {
			firstSegment.remove(firstSegment.size() - 1);
			for (int i = 1; i < segmentToMerge.size(); i++) {
				firstSegment.add(segmentToMerge.get(i));
			}
			merged = true;
		}
		return merged;
	}

	private LinkedList<List<TransportStop>> parseRoutePartsToSegments(List<TransportRoute> routeParts) {
		LinkedList<List<TransportStop>> segs = new LinkedList<List<TransportStop>>();
		// here we assume that missing stops come in pairs <A, B, C, MISSING, MISSING, D, E...>
		// we don't add segments with 1 stop cause they are irrelevant further
		for (TransportRoute part : routeParts) {
			List<TransportStop> newSeg = new ArrayList<TransportStop>();
			for (TransportStop s : part.getForwardStops()) {
				newSeg.add(s);
				if (s.isMissingStop()) {
					if (newSeg.size() > 1) {
						segs.add(newSeg);
						newSeg = new ArrayList<TransportStop>();
					}
				}
			}
			if (newSeg.size() > 1) {
				segs.add(newSeg);
			}
		}
		return segs;
	}

	private List<TransportRoute> findIncompleteRouteParts(TransportRoute baseRoute) throws IOException {
		List<TransportRoute> allRoutes = null;
		for (BinaryMapIndexReader bmir : routesFilesCache.keySet()) {
			// here we could limit routeMap indexes by only certain bbox around start / end (check comment on field)
			IncompleteTransportRoute ptr = bmir.getIncompleteTransportRoutes().get(baseRoute.getId());
			if (ptr != null) {
				TLongArrayList lst = new TLongArrayList();
				while (ptr != null) {
					lst.add(ptr.getRouteOffset());
					ptr = ptr.getNextLinkedRoute();
				}
				if (lst.size() > 0) {
					if (allRoutes == null) {
						allRoutes = new ArrayList<TransportRoute>();
					}
					allRoutes.addAll(bmir.getTransportRoutes(lst.toArray()).valueCollection());
				}
			}
		}
		return allRoutes;
	}
}
