package net.osmand.router;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;

public class MissingMapsCalculator {

	protected static final Log LOG = PlatformUtil.getLog(MissingMapsCalculator.class);

	public static final double DISTANCE_SPLIT = 15000;
	public static final double DISTANCE_SKIP = 10000;
	private OsmandRegions or;
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
		or = new OsmandRegions();
		reader = or.prepareFile();
	}

	public MissingMapsCalculator(OsmandRegions osmandRegions) {
		or = osmandRegions;
	}


	public boolean checkIfThereAreMissingMaps(RoutingContext ctx, LatLon start, List<LatLon> targets, boolean checkHHEditions)
			throws IOException {
//		start = testLatLons(targets);
		long tm = System.nanoTime();
		lastKeyNames = new ArrayList<String>();
		List<Point> pointsToCheck = new ArrayList<>();
		String profile = ctx.getRouter().getProfile().getBaseProfile(); // use base profile
		Map<String, RegisteredMap> knownMaps = new TreeMap<>();
		
		for (BinaryMapIndexReader r : ctx.map.keySet()) {
			RegisteredMap rmap = new RegisteredMap();
			rmap.downloadName = Algorithms.getRegionName(r.getFile().getName());
			rmap.reader = r;
			rmap.standard = or.getRegionDataByDownloadName(rmap.downloadName) != null;
			if (rmap.downloadName.toLowerCase().startsWith(WorldRegion.WORLD + "_")) {
				continue; // avoid including World_seamarks
			}
			knownMaps.put(rmap.downloadName, rmap);
			for (HHRouteRegion rt : r.getHHRoutingIndexes()) {
				if (rt.profile.equals(profile)) {
					rmap.edition = rt.edition;
				}
			}
		}
		LatLon end = null;
		LatLon prev = start;
		for (int i = 0; i < targets.size(); i++) {
			end = targets.get(i);
			if (MapUtils.getDistance(prev, end) < DISTANCE_SKIP) {
				// skip point they too close
				continue;
			}
			split(ctx, knownMaps, pointsToCheck, prev, end);
			prev = end;
		}
		if (end != null) {
			addPoint(ctx, knownMaps, pointsToCheck, end);
		}
		
		List<LatLon> points = CollectionUtils.asOneList(Collections.singletonList(start), targets);
		MissingMapsCalculationResult result = new MissingMapsCalculationResult(ctx, points);
		Set<Long> presentTimestamps = null;
		for (Point p : pointsToCheck) {
			if (p.hhEditions == null) {
				if (p.regions.size() > 0) {
					result.addMissingMaps(p.regions.get(0));
					
				}
			} else if (checkHHEditions) {
				if (presentTimestamps == null) {
					presentTimestamps = new TreeSet<>(p.editionsUnique);
				} else if (!presentTimestamps.isEmpty()) {
					presentTimestamps.retainAll(p.editionsUnique);
				}
			} else {
				if (p.regions.size() > 0) {
					result.addUsedMaps(p.regions.get(0));
					
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
						result.addMapToUpdate(region);
					} else {
						result.addUsedMaps(region);
					}
				}
			}
		} else if (presentTimestamps != null) {
			long selectedEdition = presentTimestamps.iterator().next();
			for (Point p : pointsToCheck) {
				for (int i = 0; p.hhEditions != null && i < p.hhEditions.length; i++) {
					if (p.hhEditions[i] == selectedEdition ) {
						result.addUsedMaps(p.regions.get(i));
						break;
					}
				}
			}
		}

		if(!result.hasMissingMaps()) {
			return false;
		}

		ctx.calculationProgress.missingMapsCalculationResult = result.prepare(or);

		LOG.info(String.format("Check missing maps %d points %.2f sec", pointsToCheck.size(),
				(System.nanoTime() - tm) / 1e9));
		return true;
	}

	protected LatLon testLatLons(List<LatLon> targets) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(MissingMapsCalculator.class.getResourceAsStream("/latlons.test.txt")));
		targets.clear();
		String s = null;
		while ((s = r.readLine()) != null) {
			String[] ls = s.split(",");
			targets.add(new LatLon(Double.parseDouble(ls[1].trim()), Double.parseDouble(ls[0].trim())));
		}
		return targets.get(0);
	}


	private void addPoint(RoutingContext ctx, Map<String, RegisteredMap> knownMaps, List<Point> pointsToCheck, LatLon loc) throws IOException {
		List<BinaryMapDataObject> resList = or.getRegionsToDownload(loc.getLatitude(), loc.getLongitude());
		boolean onlyJointMap = true;
		List<String> regions = new ArrayList<String>();
		for (BinaryMapDataObject o : resList) {
			boolean hasMapType = or.isDownloadOfType(o, OsmandRegions.MAP_TYPE);
			boolean hasRoadsType = or.isDownloadOfType(o, OsmandRegions.ROADS_TYPE);
			boolean hasMapJoinType = or.isDownloadOfType(o, OsmandRegions.MAP_JOIN_TYPE);
			boolean hasRoadsJoinType = or.isDownloadOfType(o, OsmandRegions.ROADS_JOIN_TYPE);
			if (hasMapType || hasRoadsType || hasMapJoinType || hasRoadsJoinType) {
				regions.add(or.getDownloadName(o));
				if (!hasMapJoinType && !hasRoadsJoinType) {
					onlyJointMap = false;
				}
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
			pnt.regions = new ArrayList<String>(regions);
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

	private void split(RoutingContext ctx, Map<String, RegisteredMap> knownMaps, List<Point> pointsToCheck, LatLon pnt, LatLon next) throws IOException {
		double dist = MapUtils.getDistance(pnt, next);
		if (dist < DISTANCE_SPLIT) {
			addPoint(ctx, knownMaps, pointsToCheck, pnt);
			// pointsToCheck.add(e); // add only start end is separate
		} else {
			LatLon mid = MapUtils.calculateMidPoint(pnt, next);
			split(ctx, knownMaps, pointsToCheck, pnt, mid);
			split(ctx, knownMaps, pointsToCheck, mid, next);
		}
	}

	public void close() throws IOException {
		if (reader != null) {
			reader.close();
		}
	}

}
