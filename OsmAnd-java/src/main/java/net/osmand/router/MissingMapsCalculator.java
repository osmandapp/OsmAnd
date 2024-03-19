package net.osmand.router;

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
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.util.MapUtils;

public class MissingMapsCalculator {
	protected static final Log log = PlatformUtil.getLog(MissingMapsCalculator.class);

	public static final double DISTANCE_SPLIT = 50000;
	private OsmandRegions or;
	private BinaryMapIndexReader reader;
	private List<String> keyNamesLocal = new ArrayList<String>();

	private static class Point {
		List<String> regions;
		long[] timestamps;
		TreeSet<Long> timestampsUnique;

	}

	public MissingMapsCalculator() throws IOException {
		// could be cached
		or = new OsmandRegions();
		reader = or.prepareFile();
	}

	public boolean checkIfThereAreMissingMaps(RoutingContext ctx, LatLon start, List<LatLon> targets)
			throws IOException {
		long tm = System.nanoTime();
		List<Point> pointsToCheck = new ArrayList<>();
		String profile = ctx.getRouter().getProfile().toString().toLowerCase(); // use base profile
		Map<String, Long> knownMaps = new TreeMap<>();
		for (BinaryMapIndexReader r : ctx.map.keySet()) {
			for (HHRouteRegion rt : r.getHHRoutingIndexes()) {
				if (rt.profile.equals(profile)) {
					for (RouteRegion p : r.getRoutingIndexes()) {
						knownMaps.put(p.getName().toLowerCase(), rt.edition);
					}
				}
			}
		}
		LatLon end = null;
		for (int i = 0; i < targets.size(); i++) {
			LatLon s = i == 0 ? start : targets.get(i - 1);
			end = targets.get(i);
			split(knownMaps, s, end, pointsToCheck);
		}
		if (end != null) {
			addPoint(knownMaps, pointsToCheck, end);
		}
		Set<String> mapsToDownload = new TreeSet<String>();
		Set<String> mapsToUpdate = new TreeSet<String>();
		Set<Long> presentTimestamps = null;
		for (Point p : pointsToCheck) {
			if (p.timestamps == null) {
				if (p.regions.size() > 0) {
					mapsToDownload.add(p.regions.get(0));
				}
			} else {
				if (presentTimestamps == null) {
					presentTimestamps = new TreeSet<Long>(p.timestampsUnique);
				} else if (!presentTimestamps.isEmpty()) {
					presentTimestamps.retainAll(p.timestampsUnique);
				}
			}
		}
		// maps to update
		if (presentTimestamps != null && presentTimestamps.isEmpty()) {
			long max = 0;
			for (Point p : pointsToCheck) {
				if (p.timestampsUnique != null) {
					max = Math.max(p.timestampsUnique.last(), max);
				}
			}
			for (Point p : pointsToCheck) {
				String region = null;
				for (int i = 0; p.timestamps != null && i < p.timestamps.length; i++) {
					if (p.timestamps[i] > 0) {
						if (p.timestamps[i] != max) {
							region = p.regions.get(i);
						} else {
							region = null;
							break;
						}
					}
				}
				if (region != null) {
					mapsToUpdate.add(region);
				}
			}
		}
		if (mapsToDownload.isEmpty() && mapsToUpdate.isEmpty()) {
			return false;
		}
		ctx.calculationProgress.missingMaps = convert(mapsToDownload);
		ctx.calculationProgress.mapsToUpdate = convert(mapsToUpdate);

		log.info(String.format("Check missing maps %d points %.2f sec", pointsToCheck.size(),
				(System.nanoTime() - tm) / 1e9));
		return true;
	}

	private List<WorldRegion> convert(Set<String> mapsToDownload) {
		if (mapsToDownload.isEmpty()) {
			return null;
		}
		List<WorldRegion> l = new ArrayList<WorldRegion>();
		for (String m : mapsToDownload) {
			WorldRegion wr = or.getRegionDataByDownloadName(m);
			if (wr != null) {
				l.add(wr);
			}
		}
		return l;
	}

	private void addPoint(Map<String, Long> knownMaps, List<Point> pointsToCheck, LatLon s) throws IOException {
		or.getRegionsToDownload(s.getLatitude(), s.getLongitude(), keyNamesLocal);
		Collections.sort(keyNamesLocal, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return -Integer.compare(o1.length(), o2.length());
			}
		});
		if (pointsToCheck.size() == 0 || !pointsToCheck.get(pointsToCheck.size() - 1).regions.equals(keyNamesLocal)) {
			Point pnt = new Point();
			pnt.regions = new ArrayList<String>(keyNamesLocal);
			for (int i = 0; i < pnt.regions.size(); i++) {
				String regionName = pnt.regions.get(i);
				if (knownMaps.containsKey(regionName)) {
					if (pnt.timestamps == null) {
						pnt.timestamps = new long[pnt.regions.size()];
						pnt.timestampsUnique = new TreeSet<Long>();
					}
					pnt.timestamps[i] = knownMaps.get(regionName);
					pnt.timestampsUnique.add(pnt.timestamps[i]);
				}
			}
			pointsToCheck.add(pnt);
		}
	}

	private void split(Map<String, Long> knownMaps, LatLon s, LatLon e, List<Point> pointsToCheck) throws IOException {
		if (MapUtils.getDistance(s, e) < DISTANCE_SPLIT) {
			addPoint(knownMaps, pointsToCheck, s);
			// pointsToCheck.add(e); // add only start end is separate
		} else {
			LatLon mid = MapUtils.calculateMidPoint(s, e);
			split(knownMaps, s, mid, pointsToCheck);
			split(knownMaps, mid, e, pointsToCheck);
		}
	}

	public void close() throws IOException {
		reader.close();
	}

	public String getErrorMessage(RoutingContext ctx) {
		String msg = "";
		if (ctx.calculationProgress.mapsToUpdate != null) {
			msg = ctx.calculationProgress.mapsToUpdate + " need to be updated";
		}
		if (ctx.calculationProgress.missingMaps != null) {
			if (msg.length() > 0) {
				msg += " and ";
			}
			msg = ctx.calculationProgress.missingMaps + " need to be downloaded";
		}
		msg = "To calculate the route maps " + msg;
		return msg;
	}

}
