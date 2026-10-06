package net.osmand.router.sea;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.SearchFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

/**
 * Depth along a line from depth OBFs (*.depth.obf): depth contours (contour=depth, the level in metres below chart
 * datum in the depth or name tag) first, soundings (point=depth, the shoalest value of a grid cell in name) where no
 * two contour levels are near.
 *
 * A sample between two contours takes the level of the nearest one and the nearest one of another level, weighted
 * by distance. Files are tried in the given order and the first one that knows a sample wins - put regional files
 * (chart datum, 2 m contour) before the European or world ones (mean sea level), so datums are not blended.
 */
public class SeaDepthProfile {

	public static final double STEP_METERS = 200;
	public static final int MAX_SAMPLES = 2000;
	/** Two contour levels within this distance bracket a sample. */
	public static final double CONTOUR_RADIUS = 3000;
	/**
	 * The nearest soundings within this distance are averaged by inverse squared distance. Grids are 250 m near the
	 * coast in regional files and about 2 km offshore in Europe_points.
	 */
	public static final double SOUNDING_RADIUS = 3000;
	private static final int SOUNDINGS_PER_SAMPLE = 4;
	private static final int CONTOUR_ZOOM = 13;
	private static final int SOUNDING_ZOOM = 14;
	/** Contours and soundings are read in boxes this big along the line, not in the line's whole bounding box. */
	private static final double CHUNK_METERS = 10000;
	private static final double CELL = 500;

	public static final String CONTOURS = "contours";
	public static final String SOUNDINGS = "soundings";
	public static final String CONTOUR = "contour";

	public static class Sample {
		public double lat, lon;
		/** Metres from the start of the line. */
		public double distance;
		/** Metres below the datum, NaN when unknown. */
		public double depth = Double.NaN;
		/** {@link #CONTOURS} between two levels, {@link #SOUNDINGS}, {@link #CONTOUR} - one level only near - or null. */
		public String source;
		/** What each source alone gives, NaN when it has nothing: to compare them. */
		public double contourDepth = Double.NaN, soundingDepth = Double.NaN;
	}

	private final List<BinaryMapIndexReader> readers;

	public SeaDepthProfile(List<BinaryMapIndexReader> readers) {
		this.readers = readers;
	}

	public List<Sample> profile(List<LatLon> line) throws IOException {
		List<Sample> samples = resample(line);
		if (samples.isEmpty()) {
			return samples;
		}
		for (BinaryMapIndexReader reader : readers) {
			boolean missing = false;
			for (Sample s : samples) {
				missing |= s.source == null || CONTOUR.equals(s.source);
			}
			if (!missing) {
				break;
			}
			DepthData data = load(reader, samples);
			for (Sample s : samples) {
				if (s.source == null || CONTOUR.equals(s.source)) {
					data.fill(s);
				}
			}
		}
		return samples;
	}

	private static List<Sample> resample(List<LatLon> line) {
		List<Sample> samples = new ArrayList<>();
		double length = 0;
		for (int i = 1; i < line.size(); i++) {
			length += MapUtils.getDistance(line.get(i - 1), line.get(i));
		}
		if (line.size() < 2 || length == 0) {
			return samples;
		}
		double step = Math.max(STEP_METERS, length / MAX_SAMPLES);
		double next = 0, passed = 0;
		for (int i = 1; i < line.size(); i++) {
			LatLon a = line.get(i - 1), b = line.get(i);
			double d = MapUtils.getDistance(a, b);
			while (d > 0 && next <= passed + d) {
				double t = (next - passed) / d;
				Sample s = new Sample();
				s.lat = a.getLatitude() + (b.getLatitude() - a.getLatitude()) * t;
				s.lon = a.getLongitude() + (b.getLongitude() - a.getLongitude()) * t;
				s.distance = next;
				samples.add(s);
				next += step;
			}
			passed += d;
		}
		Sample last = new Sample();
		LatLon end = line.get(line.size() - 1);
		last.lat = end.getLatitude();
		last.lon = end.getLongitude();
		last.distance = length;
		if (samples.get(samples.size() - 1).distance < length - 1) {
			samples.add(last);
		}
		return samples;
	}

	private static final SearchFilter DEPTH_FILTER = new SearchFilter() {
		@Override
		public boolean accept(TIntArrayList types, MapIndex index) {
			for (int i = 0; i < types.size(); i++) {
				TagValuePair p = index.decodeType(types.get(i));
				if (p != null && ("contour".equals(p.tag) || "point".equals(p.tag)) && "depth".equals(p.value)) {
					return true;
				}
			}
			return false;
		}
	};

	private static DepthData load(BinaryMapIndexReader reader, List<Sample> samples) throws IOException {
		DepthData data = new DepthData((samples.get(0).lat + samples.get(samples.size() - 1).lat) / 2);
		// contours and soundings are read in separate passes: a soundings section can start at the contour zoom
		Set<Long> seenContours = new HashSet<>(), seenSoundings = new HashSet<>();
		double pad = (CONTOUR_RADIUS + 500) / 111000;
		int chunk = Math.max(1, (int) Math.round(CHUNK_METERS / Math.max(1, samples.size() > 1
				? samples.get(1).distance - samples.get(0).distance : STEP_METERS)));
		for (int from = 0; from < samples.size(); from += chunk) {
			double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;
			for (int i = from; i < Math.min(samples.size(), from + chunk + 1); i++) {
				Sample s = samples.get(i);
				minLat = Math.min(minLat, s.lat);
				maxLat = Math.max(maxLat, s.lat);
				minLon = Math.min(minLon, s.lon);
				maxLon = Math.max(maxLon, s.lon);
			}
			double lonPad = pad / Math.cos(Math.toRadians((minLat + maxLat) / 2));
			for (int zoom : new int[] { CONTOUR_ZOOM, SOUNDING_ZOOM }) {
				SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(
						MapUtils.get31TileNumberX(minLon - lonPad), MapUtils.get31TileNumberX(maxLon + lonPad),
						MapUtils.get31TileNumberY(maxLat + pad), MapUtils.get31TileNumberY(minLat - pad), zoom, DEPTH_FILTER);
				for (MapIndex index : reader.getMapIndexes()) {
					for (BinaryMapDataObject o : reader.searchMapIndex(req, index)) {
						if ((zoom == CONTOUR_ZOOM ? seenContours : seenSoundings).add(o.getId())) {
							data.add(o, zoom == CONTOUR_ZOOM);
						}
					}
				}
			}
		}
		return data;
	}

	/** Contour segments and soundings of one file, in a local metric grid. */
	private static class DepthData {
		final double metersPerDegreeLon;
		final Doubles segments = new Doubles(); // x1, y1, x2, y2, level
		final Doubles soundings = new Doubles(); // x, y, depth
		final Map<Long, TIntArrayList> segmentGrid = new HashMap<>();
		final Map<Long, TIntArrayList> soundingGrid = new HashMap<>();

		DepthData(double baseLat) {
			metersPerDegreeLon = SeaObstacles.METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(baseLat));
		}

		double x(double lon) {
			return lon * metersPerDegreeLon;
		}

		double y(double lat) {
			return lat * SeaObstacles.METERS_PER_DEGREE_LAT;
		}

		static long cell(double x, double y) {
			return (((long) Math.floor(x / CELL)) << 32) ^ (((long) Math.floor(y / CELL)) & 0xffffffffL);
		}

		void add(BinaryMapDataObject o, boolean contours) {
			MapIndex index = o.getMapIndex();
			boolean contour = false, point = false;
			for (int t : o.getTypes()) {
				TagValuePair p = index.decodeType(t);
				contour |= p != null && "contour".equals(p.tag) && "depth".equals(p.value);
				point |= p != null && "point".equals(p.tag) && "depth".equals(p.value);
			}
			double value = value(o, contour ? "depth" : null);
			if (Double.isNaN(value)) {
				return;
			}
			if (contour && contours) {
				for (int i = 1; i < o.getPointsLength(); i++) {
					double x1 = x(MapUtils.get31LongitudeX(o.getPoint31XTile(i - 1)));
					double y1 = y(MapUtils.get31LatitudeY(o.getPoint31YTile(i - 1)));
					double x2 = x(MapUtils.get31LongitudeX(o.getPoint31XTile(i)));
					double y2 = y(MapUtils.get31LatitudeY(o.getPoint31YTile(i)));
					int id = segments.size() / 5;
					segments.add(new double[] { x1, y1, x2, y2, value });
					// a segment longer than a cell is registered in every cell of its box
					for (long cx = (long) Math.floor(Math.min(x1, x2) / CELL); cx <= Math.floor(Math.max(x1, x2) / CELL); cx++) {
						for (long cy = (long) Math.floor(Math.min(y1, y2) / CELL); cy <= Math.floor(Math.max(y1, y2) / CELL); cy++) {
							put(segmentGrid, (cx << 32) ^ (cy & 0xffffffffL), id);
						}
					}
				}
			} else if (point && !contours && o.getPointsLength() > 0) {
				double x = x(MapUtils.get31LongitudeX(o.getPoint31XTile(0)));
				double y = y(MapUtils.get31LatitudeY(o.getPoint31YTile(0)));
				put(soundingGrid, cell(x, y), soundings.size() / 3);
				soundings.add(new double[] { x, y, value });
			}
		}

		/** The depth tag of a contour, else the name - soundings carry the value in the name only. */
		static double value(BinaryMapDataObject o, String tag) {
			String[] found = { null, null };
			if (o.getObjectNames() != null) {
				o.getObjectNames().forEachEntry((k, v) -> {
					TagValuePair p = o.getMapIndex().decodeType(k);
					if (p != null && p.tag.equals(tag)) {
						found[0] = v;
					} else if (p != null && p.tag.equals("name")) {
						found[1] = v;
					}
					return true;
				});
			}
			String v = found[0] != null ? found[0] : found[1];
			try {
				return v == null ? Double.NaN : Double.parseDouble(v.trim());
			} catch (NumberFormatException e) {
				return Double.NaN;
			}
		}

		static void put(Map<Long, TIntArrayList> grid, long key, int id) {
			TIntArrayList list = grid.get(key);
			if (list == null) {
				list = new TIntArrayList();
				grid.put(key, list);
			}
			list.add(id);
		}

		void fill(Sample s) {
			double px = x(s.lon), py = y(s.lat);
			// per contour level: the nearest point, as {distance, dx, dy}
			Map<Double, double[]> levels = new HashMap<>();
			int r = (int) Math.ceil(CONTOUR_RADIUS / CELL);
			long cx0 = (long) Math.floor(px / CELL), cy0 = (long) Math.floor(py / CELL);
			Set<Integer> done = new HashSet<>();
			for (long cx = cx0 - r; cx <= cx0 + r; cx++) {
				for (long cy = cy0 - r; cy <= cy0 + r; cy++) {
					TIntArrayList ids = segmentGrid.get((cx << 32) ^ (cy & 0xffffffffL));
					for (int k = 0; ids != null && k < ids.size(); k++) {
						int id = ids.get(k);
						if (!done.add(id)) {
							continue;
						}
						int p = id * 5;
						double ax = segments.get(p), ay = segments.get(p + 1);
						double vx = segments.get(p + 2) - ax, vy = segments.get(p + 3) - ay;
						double l2 = vx * vx + vy * vy;
						double t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * vx + (py - ay) * vy) / l2));
						double dx = ax + t * vx - px, dy = ay + t * vy - py, d = Math.hypot(dx, dy);
						double[] known = levels.get(segments.get(p + 4));
						if (d <= CONTOUR_RADIUS && (known == null || d < known[0])) {
							levels.put(segments.get(p + 4), new double[] { d, dx, dy });
						}
					}
				}
			}
			// the nearest contour, and the nearest one of another level on the other side of the sample: a sample
			// between two contours lies between their levels, two contours on the same side - the edge of a file's
			// coverage, a bank seen from outside - say nothing about it
			Map.Entry<Double, double[]> first = null, second = null;
			for (Map.Entry<Double, double[]> e : levels.entrySet()) {
				if (first == null || e.getValue()[0] < first.getValue()[0]) {
					first = e;
				}
			}
			for (Map.Entry<Double, double[]> e : levels.entrySet()) {
				if (first == null || e.getKey().equals(first.getKey())) {
					continue;
				}
				double[] a = first.getValue(), b = e.getValue();
				boolean opposite = a[0] < 1 || a[1] * b[1] + a[2] * b[2] < 0;
				if (opposite && (second == null || b[0] < second.getValue()[0])) {
					second = e;
				}
			}
			double soundings = soundings(px, py);
			if (Double.isNaN(s.soundingDepth)) {
				s.soundingDepth = soundings;
			}
			if (first != null && second != null) {
				double da = first.getValue()[0], db = second.getValue()[0];
				s.depth = (first.getKey() * db + second.getKey() * da) / Math.max(da + db, 1e-6);
				s.contourDepth = s.depth;
				s.source = CONTOURS;
				return;
			}
			if (!Double.isNaN(soundings)) {
				s.depth = soundings;
				s.source = SOUNDINGS;
				return;
			}
			if (first != null && s.source == null) {
				s.depth = first.getKey();
				s.source = CONTOUR;
			}
		}

		/** Inverse distance weighted soundings around a point, NaN when there are none. */
		double soundings(double px, double py) {
			int r = (int) Math.ceil(SOUNDING_RADIUS / CELL);
			long cx0 = (long) Math.floor(px / CELL), cy0 = (long) Math.floor(py / CELL);
			double[] dist = new double[SOUNDINGS_PER_SAMPLE], value = new double[SOUNDINGS_PER_SAMPLE];
			int count = 0;
			for (long cx = cx0 - r; cx <= cx0 + r; cx++) {
				for (long cy = cy0 - r; cy <= cy0 + r; cy++) {
					TIntArrayList ids = soundingGrid.get((cx << 32) ^ (cy & 0xffffffffL));
					for (int k = 0; ids != null && k < ids.size(); k++) {
						int p = ids.get(k) * 3;
						double d = Math.hypot(soundings.get(p) - px, soundings.get(p + 1) - py);
						if (d > SOUNDING_RADIUS) {
							continue;
						}
						// keep the nearest few, sorted by distance
						if (count == SOUNDINGS_PER_SAMPLE && d >= dist[count - 1]) {
							continue;
						}
						int at = count < SOUNDINGS_PER_SAMPLE ? count++ : SOUNDINGS_PER_SAMPLE - 1;
						while (at > 0 && dist[at - 1] > d) {
							dist[at] = dist[at - 1];
							value[at] = value[at - 1];
							at--;
						}
						dist[at] = d;
						value[at] = soundings.get(p + 2);
					}
				}
			}
			if (count == 0) {
				return Double.NaN;
			}
			double num = 0, den = 0;
			for (int i = 0; i < count; i++) {
				double w = 1 / Math.max(dist[i] * dist[i], 1);
				num += w * value[i];
				den += w;
			}
			return num / den;
		}
	}

	/** A growable double array. */
	private static class Doubles {
		private double[] values = new double[1024];
		private int size;

		void add(double[] add) {
			if (size + add.length > values.length) {
				values = java.util.Arrays.copyOf(values, Math.max(values.length * 2, size + add.length));
			}
			System.arraycopy(add, 0, values, size, add.length);
			size += add.length;
		}

		double get(int i) {
			return values[i];
		}

		int size() {
			return size;
		}
	}
}
