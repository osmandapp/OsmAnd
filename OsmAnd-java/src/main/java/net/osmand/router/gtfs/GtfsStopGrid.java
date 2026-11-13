package net.osmand.router.gtfs;

import java.util.HashMap;
import java.util.Map;

import gnu.trove.list.array.TIntArrayList;

/** Bucket grid over stop coordinates (degrees * 1e7) for "all stops within r metres" lookups. */
final class GtfsStopGrid {

	private static final double CELL_DEG = 0.01;

	private final int[] lat, lon;
	private final Map<Long, TIntArrayList> cells = new HashMap<>();

	/** Grid over the stops of the list. */
	GtfsStopGrid(int[] lat, int[] lon, TIntArrayList stops) {
		this.lat = lat;
		this.lon = lon;
		for (int i = 0; i < stops.size(); i++) {
			int s = stops.get(i);
			cells.computeIfAbsent(key(cell(lat[s]), cell(lon[s])), k -> new TIntArrayList()).add(s);
		}
	}

	private static int cell(int e7) {
		return (int) Math.floor(e7 / 1e7 / CELL_DEG);
	}

	private static long key(int cy, int cx) {
		return ((long) cy << 32) ^ (cx & 0xffffffffL);
	}

	/** Adds every grid stop within the distance of the point to result. */
	void within(int pLat, int pLon, double meters, TIntArrayList result) {
		double dLat = meters / 111320.0;
		double dLon = dLat / Math.max(0.01, Math.cos(Math.toRadians(pLat / 1e7)));
		int y0 = cell(pLat - (int) (dLat * 1e7)), y1 = cell(pLat + (int) (dLat * 1e7));
		int x0 = cell(pLon - (int) (dLon * 1e7)), x1 = cell(pLon + (int) (dLon * 1e7));
		for (int y = y0; y <= y1; y++) {
			for (int x = x0; x <= x1; x++) {
				TIntArrayList l = cells.get(key(y, x));
				for (int i = 0; l != null && i < l.size(); i++) {
					int s = l.get(i);
					if (GtfsIndex.distanceMeters(pLat, pLon, lat[s], lon[s]) <= meters) {
						result.add(s);
					}
				}
			}
		}
	}
}
