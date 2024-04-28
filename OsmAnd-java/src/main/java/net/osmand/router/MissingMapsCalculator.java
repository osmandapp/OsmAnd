package net.osmand.router;

import static net.osmand.util.CollectionUtils.addIfNotContains;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;

import net.osmand.binary.BinaryHHRouteReaderAdapter.HHRouteRegion;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class MissingMapsCalculator {

	protected static final Log LOG = PlatformUtil.getLog(MissingMapsCalculator.class);

	public static final double DISTANCE_TO_SPLIT = 50000;
	public static final double DISTANCE_TO_SKIP = 10000;
	private final OsmandRegions osmandRegions;
	private BinaryMapIndexReader reader;
	private List<String> lastKeyNames ;

	private static class Point {
		List<String> regions;
		long[] hhEditions; // 0 means routing data present but no HH data, null means no data at all
		TreeSet<Long> editionsUnique;
	}
	
	private static class RegisteredMap {
		BinaryMapIndexReader reader;
		boolean standard;
		long edition;
		String downloadName;
	}

	public MissingMapsCalculator() throws IOException {
		// could be cached
		osmandRegions = new OsmandRegions();
		reader = osmandRegions.prepareFile();
	}

	public MissingMapsCalculator(OsmandRegions osmandRegions) {
		this.osmandRegions = osmandRegions;
	}

	public MissingMapsCalculationResult calculateMissingMaps(RoutingContext ctx, List<LatLon> routePoints, boolean checkHHEditions) throws IOException {
		long tm = System.nanoTime();
		lastKeyNames = new ArrayList<>();
		String profile = ctx.getRouter().getProfile().getBaseProfile(); // use base profile
		Map<String, RegisteredMap> knownMaps = collectKnownMaps(ctx, profile);
		List<LatLon> filteredRoutePoints = filterTooClosePoints(routePoints);
		List<Point> pointsToCheck = collectPointsToCheck(filteredRoutePoints, knownMaps);

		List<String> usedMaps = new ArrayList<>();
		List<String> mapsToDownload = new ArrayList<>();
		List<String> mapsToUpdate = new ArrayList<>();
		Set<Long> presentTimestamps = null;
		for (Point p : pointsToCheck) {
			if (p.hhEditions == null) {
				if (p.regions.size() > 0) {
					addIfNotContains(mapsToDownload, p.regions.get(0));
				}
			} else if (checkHHEditions) {
				if (presentTimestamps == null) {
					presentTimestamps = new TreeSet<Long>(p.editionsUnique);
				} else if (!presentTimestamps.isEmpty()) {
					presentTimestamps.retainAll(p.editionsUnique);
				}
			} else {
				if (p.regions.size() > 0) {
					usedMaps.add(p.regions.get(0));
				}
			}
		}
		// maps to update
		if (presentTimestamps != null && presentTimestamps.isEmpty()) {
			long max = 0;
			for (Point p : pointsToCheck) {
				if (p.editionsUnique != null) {
					max = Math.max(p.editionsUnique.last(), max);
				}
			}
			for (Point p : pointsToCheck) {
				String region = null;
				boolean fresh = false;
				for (int i = 0; p.hhEditions != null && i < p.hhEditions.length; i++) {
					if (p.hhEditions[i] > 0) {
						region = p.regions.get(i);
						fresh = p.hhEditions[i] == max;
						if (fresh) {
							break;
						}
					}
				}
				if (region != null) {
					if (!fresh) {
						addIfNotContains(mapsToUpdate, region);
					} else {
						addIfNotContains(usedMaps, region);
					}
				}
			}
		} else if (presentTimestamps != null) {
			long selectedEdition = presentTimestamps.iterator().next();
			for (Point p : pointsToCheck) {
				for (int i = 0; p.hhEditions != null && i < p.hhEditions.length; i++) {
					if (p.hhEditions[i] == selectedEdition ) {
						addIfNotContains(usedMaps, p.regions.get(i));
						break;
					}
				}
			}
		}
		
//		System.out.println("Used maps: " + usedMaps);
		LatLon start = routePoints.get(0);
		LatLon end = routePoints.get(routePoints.size() - 1);
		MissingMapsCalculationResult result = new MissingMapsCalculationResult(ctx, start, end);
		if (mapsToDownload.isEmpty() && mapsToUpdate.isEmpty()) {
			return result;
		}
		result.requestMapsToUpdate = true;
		result.missingMaps = convert(mapsToDownload);
		result.mapsToUpdate = convert(mapsToUpdate);
		result.potentiallyUsedMaps = convert(usedMaps);

		LOG.info(String.format("Check missing maps %d points %.2f sec", pointsToCheck.size(),
				(System.nanoTime() - tm) / 1e9));
		return result;
	}

	private Map<String, RegisteredMap> collectKnownMaps(RoutingContext ctx, String profile) {
		Map<String, RegisteredMap> knownMaps = new TreeMap<>();
		for (BinaryMapIndexReader r : ctx.map.keySet()) {
			RegisteredMap rmap = new RegisteredMap();
			rmap.downloadName = Algorithms.getRegionName(r.getFile().getName());
			rmap.reader = r;
			rmap.standard = osmandRegions.getRegionDataByDownloadName(rmap.downloadName) != null;
			knownMaps.put(rmap.downloadName, rmap);
			for (HHRouteRegion rt : r.getHHRoutingIndexes()) {
				if (rt.profile.equals(profile)) {
					rmap.edition = rt.edition;
				}
			}
		}
		return knownMaps;
	}

	private List<LatLon> filterTooClosePoints(List<LatLon> route) {
		List<LatLon> result = new ArrayList<>();
		LatLon end = null;
		for (int i = 0; i < route.size() - 1; i++) {
			int endIndex = i + 1;
			LatLon start = route.get(i);
			end = route.get(endIndex);
			double distance = MapUtils.getDistance(start, end);
			while (distance < DISTANCE_TO_SKIP) {
				if (++endIndex >= route.size()) {
					break;
				}
				end = route.get(endIndex);
				distance = MapUtils.getDistance(start, end);
			}
			i = endIndex;
			result.add(start);
		}
		if (end != null) {
			result.add(end);
		}
		return result;
	}

	private List<Point> collectPointsToCheck(List<LatLon> routePoints, Map<String, RegisteredMap> knownMaps) throws IOException {
		List<Point> pointsToCheck = new ArrayList<>();

		LatLon end = null;
		for (int i = 0; i < routePoints.size(); i++) {
			LatLon prev = i == 0 ? routePoints.get(0) : routePoints.get(i - 1);
			end = routePoints.get(i);
			split(knownMaps, pointsToCheck, prev, end);
		}
		if (end != null) {
			addPoint(knownMaps, pointsToCheck, end);
		}

		return pointsToCheck;
	}

	private List<WorldRegion> convert(List<String> mapsToDownload) {
		if (mapsToDownload.isEmpty()) {
			return null;
		}
		List<WorldRegion> l = new ArrayList<>();
		for (String m : mapsToDownload) {
			WorldRegion wr = osmandRegions.getRegionDataByDownloadName(m);
			if (wr != null) {
				l.add(wr);
			}
		}
		return l;
	}

	private void addPoint(Map<String, RegisteredMap> knownMaps, List<Point> pointsToCheck, LatLon loc) throws IOException {
		List<BinaryMapDataObject> resList = osmandRegions.getRegionsToDownload(loc.getLatitude(), loc.getLongitude());
		boolean onlyJointMap = true;
		List<String> regions = new ArrayList<>();
		for (BinaryMapDataObject o : resList) {
			regions.add(osmandRegions.getDownloadName(o));
			if (!osmandRegions.isDownloadOfType(o, OsmandRegions.MAP_JOIN_TYPE)
					&& !osmandRegions.isDownloadOfType(o, OsmandRegions.ROADS_JOIN_TYPE)) {
				onlyJointMap = false;
			}
		}
		Collections.sort(regions, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return -Integer.compare(o1.length(), o2.length());
			}
		});
		if ((pointsToCheck.size() == 0 || !regions.equals(lastKeyNames)) && !onlyJointMap) {
			Point pnt = new Point();
			lastKeyNames = regions;
			pnt.regions = new ArrayList<>(regions);
			boolean hasHHEdition = addMapEditions(knownMaps, pnt);
			if (!hasHHEdition) {
				pnt.hhEditions = null; // recreate
				// check non-standard maps
				int x31 = MapUtils.get31TileNumberX(loc.getLongitude());
				int y31 = MapUtils.get31TileNumberY(loc.getLatitude());
				for (RegisteredMap r : knownMaps.values()) {
					if (!r.standard) {
						if (r.reader.containsRouteData() && r.reader.containsActualRouteData(x31, y31, null)) {
							pnt.regions.add(0, r.downloadName);
						}
					}
				}
				addMapEditions(knownMaps, pnt);
			}
			pointsToCheck.add(pnt);
		}
	}
	
	private boolean addMapEditions(Map<String, RegisteredMap> knownMaps, Point pnt) {
		boolean hhEditionPresent = false;
		for (int i = 0; i < pnt.regions.size(); i++) {
			String regionName = pnt.regions.get(i);
			if (knownMaps.containsKey(regionName)) {
				if (pnt.hhEditions == null) {
					pnt.hhEditions = new long[pnt.regions.size()];
					pnt.editionsUnique = new TreeSet<Long>();
				}
				pnt.hhEditions[i] = knownMaps.get(regionName).edition;
				hhEditionPresent |= pnt.hhEditions[i] > 0;
				pnt.editionsUnique.add(pnt.hhEditions[i]);
			}
		}
		return hhEditionPresent;
	}

	private void split(Map<String, RegisteredMap> knownMaps, List<Point> pointsToCheck, LatLon pnt, LatLon next) throws IOException {
		double dist = MapUtils.getDistance(pnt, next);
		if (dist < DISTANCE_TO_SKIP) {
			// skip point they too close
		} else if (dist < DISTANCE_TO_SPLIT) {
			addPoint(knownMaps, pointsToCheck, pnt);
			// pointsToCheck.add(e); // add only start end is separate
		} else {
			LatLon mid = MapUtils.calculateMidPoint(pnt, next);
			split(knownMaps, pointsToCheck, pnt, mid);
			split(knownMaps, pointsToCheck, mid, next);
		}
	}

	public void close() throws IOException {
		if (reader != null) {
			reader.close();
		}
	}
}
