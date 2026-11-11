package net.osmand.router.gtfs;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Read-only view of the flat index file that {@link GtfsFeedImporter} writes from one or more GTFS
 * feeds. The file is little-endian and memory-mapped, so opening it costs no heap and a query only
 * touches the pages it scans. Index numbers (stop, route, trip) are global over all feeds in the file.
 * <p>
 * Layout: a fixed header (magic, version, feed count, CRC32, file size, feed hash, valid dates,
 * agency time zone, section counts and offsets), then 8-byte aligned sections, each a set of parallel
 * int32 columns:
 * <pre>
 * strings   offsets[n+1], UTF-8 bytes
 * stops     lat, lon (degrees * 1e7), name, id, parent (-1), min transfer time at the stop, footpath offsets[n+1]
 * footpaths to, value (metres * 2 for a walk estimate, or seconds * 2 + 1 for a transfers.txt minimum)
 * routes    short name, long name, type, colour (0xRRGGBB or -1)
 * trips     route, service, headsign, first stop-time entry, stop-time count
 * calendar  weekday mask, first day, last day, exception offsets[n+1]; then exception day, type
 * connections  departure, arrival, from stop, to stop, trip * 4 + flags     (sorted by departure)
 * stop times   stop + flags, arrival, departure                             (contiguous per trip, in stop order)
 * </pre>
 * Times are seconds since the start of the service day and may exceed 24 hours.
 * A bad magic, a version other than {@link #VERSION}, a wrong size or a failed checksum is reported with a
 * {@link GtfsIndexException}; the planner never works on such a file.
 */
public final class GtfsIndex {

	public static final int VERSION = 1;

	static final byte[] MAGIC = "OSMGTFSI".getBytes(StandardCharsets.US_ASCII);
	static final int HEADER_SIZE = 192;
	static final int CRC_FROM = 24; // the checksum covers everything after the file size field
	static final int H_VERSION = 8, H_FEEDS = 12, H_CRC = 16, H_SIZE = 24, H_HASH = 32, H_FROM = 40, H_TO = 44,
			H_TZ = 48, H_COUNTS = 56, H_OFFSETS = 96;

	// counts
	static final int C_STR = 0, C_STR_BYTES = 1, C_STOPS = 2, C_FOOT = 3, C_ROUTES = 4, C_TRIPS = 5, C_SERVICES = 6,
			C_EXCEPTIONS = 7, C_CONN = 8, C_STOP_TIMES = 9, COUNTS = 10;
	// sections, in file order
	static final int S_STR_OFFS = 0, S_STR_BYTES = 1, S_STOPS = 2, S_FOOT = 3, S_ROUTES = 4, S_TRIPS = 5,
			S_CALENDAR = 6, S_EXCEPTIONS = 7, S_CONN = 8, S_STOP_TIMES = 9, SECTIONS = 10;

	public static final int FLAG_NO_PICKUP = 1 << 29; // in stop-time entries
	public static final int FLAG_NO_DROP_OFF = 1 << 30;
	public static final int STOP_MASK = FLAG_NO_PICKUP - 1;

	public static final int NO_COLOR = -1;

	private final MappedByteBuffer map;
	private final long fileSize;
	private final long feedHash;
	private final int feedCount, validFrom, validTo;
	private final int[] counts = new int[COUNTS];
	private final IntBuffer strOffsets;
	private final long strBase;
	private final GtfsCalendar calendar;

	final IntBuffer stopLat, stopLon, stopName, stopId, stopParent, stopMinTransfer, footOffset;
	final IntBuffer footTo, footValue;
	final IntBuffer routeShort, routeLong, routeType, routeColor;
	final IntBuffer tripRoute, tripService, tripHeadsign, tripFirst, tripCount;
	final IntBuffer connDep, connArr, connFrom, connTo, connTrip;
	final IntBuffer stopTimeStop, stopTimeArr, stopTimeDep;

	/** Opens the index and checks its structure (magic, version, size, section bounds). */
	public static GtfsIndex open(File file) throws IOException {
		return open(file, false);
	}

	/** As {@link #open(File)}; with verify, also reads the whole file once to check the CRC32. */
	public static GtfsIndex open(File file, boolean verifyChecksum) throws IOException {
		GtfsIndex index = new GtfsIndex(file);
		if (verifyChecksum && !index.verifyChecksum()) {
			throw new GtfsIndexException("GTFS index checksum mismatch: " + file);
		}
		return index;
	}

	private GtfsIndex(File file) throws IOException {
		try (FileChannel ch = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			fileSize = ch.size();
			if (fileSize < HEADER_SIZE || fileSize > Integer.MAX_VALUE) {
				throw new GtfsIndexException("Not a GTFS index (size " + fileSize + "): " + file);
			}
			map = ch.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
		}
		map.order(ByteOrder.LITTLE_ENDIAN);
		byte[] magic = new byte[MAGIC.length];
		ByteBuffer head = map.duplicate();
		head.get(magic);
		if (!Arrays.equals(magic, MAGIC)) {
			throw new GtfsIndexException("Not a GTFS index (bad magic): " + file);
		}
		if (map.getInt(H_VERSION) != VERSION) {
			throw new GtfsIndexException("Unsupported GTFS index version " + map.getInt(H_VERSION) + ", expected "
					+ VERSION + ": re-import the feed");
		}
		if (map.getLong(H_SIZE) != fileSize) {
			throw new GtfsIndexException("GTFS index is truncated or corrupted: " + file);
		}
		feedCount = map.getInt(H_FEEDS);
		feedHash = map.getLong(H_HASH);
		validFrom = map.getInt(H_FROM);
		validTo = map.getInt(H_TO);
		for (int i = 0; i < COUNTS; i++) {
			counts[i] = map.getInt(H_COUNTS + 4 * i);
			if (counts[i] < 0) {
				throw new GtfsIndexException("GTFS index header is corrupted: " + file);
			}
		}
		long[] off = sectionOffsets(counts);
		for (int i = 0; i < SECTIONS; i++) {
			if (map.getLong(H_OFFSETS + 8 * i) != off[i]) {
				throw new GtfsIndexException("GTFS index sections do not match its header: " + file);
			}
		}
		if (off[SECTIONS] != fileSize) {
			throw new GtfsIndexException("GTFS index size does not match its header: " + file);
		}
		int ns = counts[C_STOPS], nf = counts[C_FOOT], nr = counts[C_ROUTES], nt = counts[C_TRIPS],
				nv = counts[C_SERVICES], nx = counts[C_EXCEPTIONS], nc = counts[C_CONN], nst = counts[C_STOP_TIMES];
		strOffsets = view(off[S_STR_OFFS], counts[C_STR] + 1);
		strBase = off[S_STR_BYTES];
		long p = off[S_STOPS];
		stopLat = view(p, ns);
		stopLon = view(p + 4L * ns, ns);
		stopName = view(p + 8L * ns, ns);
		stopId = view(p + 12L * ns, ns);
		stopParent = view(p + 16L * ns, ns);
		stopMinTransfer = view(p + 20L * ns, ns);
		footOffset = view(p + 24L * ns, ns + 1);
		footTo = view(off[S_FOOT], nf);
		footValue = view(off[S_FOOT] + 4L * nf, nf);
		p = off[S_ROUTES];
		routeShort = view(p, nr);
		routeLong = view(p + 4L * nr, nr);
		routeType = view(p + 8L * nr, nr);
		routeColor = view(p + 12L * nr, nr);
		p = off[S_TRIPS];
		tripRoute = view(p, nt);
		tripService = view(p + 4L * nt, nt);
		tripHeadsign = view(p + 8L * nt, nt);
		tripFirst = view(p + 12L * nt, nt);
		tripCount = view(p + 16L * nt, nt);
		p = off[S_CONN];
		connDep = view(p, nc);
		connArr = view(p + 4L * nc, nc);
		connFrom = view(p + 8L * nc, nc);
		connTo = view(p + 12L * nc, nc);
		connTrip = view(p + 16L * nc, nc);
		p = off[S_STOP_TIMES];
		stopTimeStop = view(p, nst);
		stopTimeArr = view(p + 4L * nst, nst);
		stopTimeDep = view(p + 8L * nst, nst);
		ZoneId zone;
		try {
			zone = ZoneId.of(string(map.getInt(H_TZ)));
		} catch (DateTimeException e) {
			throw new GtfsIndexException("GTFS index has an unknown time zone: " + file);
		}
		p = off[S_CALENDAR];
		int[] mask = ints(view(p, nv)), first = ints(view(p + 4L * nv, nv)), last = ints(view(p + 8L * nv, nv));
		int[] excOff = ints(view(p + 12L * nv, nv + 1));
		p = off[S_EXCEPTIONS];
		calendar = new GtfsCalendar(mask, first, last, excOff, ints(view(p, nx)), ints(view(p + 4L * nx, nx)), zone);
	}

	private IntBuffer view(long offset, long n) {
		ByteBuffer b = map.duplicate();
		b.position((int) offset);
		b.limit((int) (offset + 4 * n));
		return b.slice().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
	}

	private static int[] ints(IntBuffer b) {
		int[] a = new int[b.remaining()];
		b.duplicate().get(a);
		return a;
	}

	/** Byte offsets of all sections (and the end of the file as the last element) for the given counts. */
	static long[] sectionOffsets(int[] c) {
		long[] size = new long[SECTIONS];
		size[S_STR_OFFS] = 4L * (c[C_STR] + 1);
		size[S_STR_BYTES] = c[C_STR_BYTES];
		size[S_STOPS] = 4L * (7L * c[C_STOPS] + 1);
		size[S_FOOT] = 8L * c[C_FOOT];
		size[S_ROUTES] = 16L * c[C_ROUTES];
		size[S_TRIPS] = 20L * c[C_TRIPS];
		size[S_CALENDAR] = 4L * (4L * c[C_SERVICES] + 1);
		size[S_EXCEPTIONS] = 8L * c[C_EXCEPTIONS];
		size[S_CONN] = 20L * c[C_CONN];
		size[S_STOP_TIMES] = 12L * c[C_STOP_TIMES];
		long[] off = new long[SECTIONS + 1];
		long p = HEADER_SIZE;
		for (int i = 0; i < SECTIONS; i++) {
			off[i] = p;
			p += (size[i] + 7) & ~7L;
		}
		off[SECTIONS] = p;
		return off;
	}

	/** Writes the fixed header; the CRC field is left zero for the writer to patch. */
	static void putHeader(ByteBuffer b, int feedCount, long feedHash, int validFrom, int validTo, int tzString,
	                      int[] counts) {
		long[] off = sectionOffsets(counts);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.position(0);
		b.put(MAGIC);
		b.putInt(H_VERSION, VERSION);
		b.putInt(H_FEEDS, feedCount);
		b.putLong(H_SIZE, off[SECTIONS]);
		b.putLong(H_HASH, feedHash);
		b.putInt(H_FROM, validFrom);
		b.putInt(H_TO, validTo);
		b.putInt(H_TZ, tzString);
		for (int i = 0; i < COUNTS; i++) {
			b.putInt(H_COUNTS + 4 * i, counts[i]);
		}
		for (int i = 0; i < SECTIONS; i++) {
			b.putLong(H_OFFSETS + 8 * i, off[i]);
		}
	}

	/** Reads the whole file once and compares its CRC32 with the one in the header. */
	public boolean verifyChecksum() {
		CRC32 crc = new CRC32();
		ByteBuffer b = map.duplicate();
		b.position(CRC_FROM);
		crc.update(b);
		return crc.getValue() == map.getLong(H_CRC);
	}

	// ---- header

	public long getFeedHash() {
		return feedHash;
	}

	public int getFeedCount() {
		return feedCount;
	}

	/** First and last day (epoch days) any service is defined for; validFrom > validTo if there is none. */
	public int getValidFrom() {
		return validFrom;
	}

	public int getValidTo() {
		return validTo;
	}

	public ZoneId getZone() {
		return calendar.getZone();
	}

	public GtfsCalendar getCalendar() {
		return calendar;
	}

	public long getFileSize() {
		return fileSize;
	}

	public int getStopCount() {
		return counts[C_STOPS];
	}

	public int getRouteCount() {
		return counts[C_ROUTES];
	}

	public int getTripCount() {
		return counts[C_TRIPS];
	}

	public int getConnectionCount() {
		return counts[C_CONN];
	}

	public int getFootpathCount() {
		return counts[C_FOOT];
	}

	public int getStopTimeCount() {
		return counts[C_STOP_TIMES];
	}

	// ---- stops, routes, trips

	public double getStopLat(int stop) {
		return stopLat.get(stop) / 1e7;
	}

	public double getStopLon(int stop) {
		return stopLon.get(stop) / 1e7;
	}

	public String getStopName(int stop) {
		return string(stopName.get(stop));
	}

	/** Stop id as "feed:stop_id", where feed is the import file name without ".zip". */
	public String getStopId(int stop) {
		return string(stopId.get(stop));
	}

	/** Parent station of the stop, or -1. */
	public int getStopParent(int stop) {
		return stopParent.get(stop);
	}

	public String getRouteShortName(int route) {
		return string(routeShort.get(route));
	}

	public String getRouteLongName(int route) {
		return string(routeLong.get(route));
	}

	/** GTFS route_type (0 tram, 1 subway, 2 rail, 3 bus, ...). */
	public int getRouteType(int route) {
		return routeType.get(route);
	}

	/** Route colour as 0xRRGGBB, or {@link #NO_COLOR}. */
	public int getRouteColor(int route) {
		return routeColor.get(route);
	}

	/** Route of the trip, or -1 if trips.txt named an unknown route. */
	public int getTripRoute(int trip) {
		return tripRoute.get(trip);
	}

	public String getTripHeadsign(int trip) {
		return string(tripHeadsign.get(trip));
	}

	/** Number of stops of the trip in the stop-time section (0 if the trip has no usable stop times). */
	public int getTripStopCount(int trip) {
		return tripCount.get(trip);
	}

	/** Stop index at position seq (0-based, in travel order) of the trip. */
	public int getTripStop(int trip, int seq) {
		return stopTimeStop.get(tripFirst.get(trip) + seq) & STOP_MASK;
	}

	/** Scheduled arrival, seconds since the start of the service day. */
	public int getTripArrival(int trip, int seq) {
		return stopTimeArr.get(tripFirst.get(trip) + seq);
	}

	public int getTripDeparture(int trip, int seq) {
		return stopTimeDep.get(tripFirst.get(trip) + seq);
	}

	String string(int i) {
		int s = strOffsets.get(i), e = strOffsets.get(i + 1);
		byte[] b = new byte[e - s];
		ByteBuffer d = map.duplicate();
		d.position((int) strBase + s);
		d.get(b);
		return new String(b, StandardCharsets.UTF_8);
	}

	/** First connection departing at or after the time (service-day seconds). */
	int firstConnectionAt(int time) {
		int lo = 0, hi = counts[C_CONN];
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (connDep.get(mid) < time) {
				lo = mid + 1;
			} else {
				hi = mid;
			}
		}
		return lo;
	}

	/** Approximate distance in metres between two points given in degrees * 1e7 (flat-earth, fine below 2 km). */
	static double distanceMeters(int lat1, int lon1, int lat2, int lon2) {
		double dy = (lat2 - lat1) / 1e7 * 111320.0;
		double dx = (lon2 - lon1) / 1e7 * 111320.0 * Math.cos(Math.toRadians((lat1 + lat2) / 2e7));
		return Math.sqrt(dx * dx + dy * dy);
	}
}
