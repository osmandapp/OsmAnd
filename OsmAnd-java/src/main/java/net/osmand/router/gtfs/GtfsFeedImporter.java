package net.osmand.router.gtfs;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import gnu.trove.list.array.TIntArrayList;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

/**
 * Builds a {@link GtfsIndex} file from one or more GTFS zip files.
 * <p>
 * The zips are read in place (nothing is extracted). stop_times is streamed trip by trip, so the heap
 * stays small whatever the feed size: each feed's connections are sorted on their own and written to a
 * temporary run file, and the runs are then merged through memory-mapped files straight into the
 * index (about 128 MB of heap for the 8 million rows of the five NYC bus feeds plus the subway).
 * Everything is written to a temporary file next to the target, synced and renamed over it, so a
 * failed or cancelled import leaves the previous index untouched.
 * <p>
 * Not handled, by design: frequencies.txt trips (skipped with a warning), pathways, fares, shapes.
 */
public final class GtfsFeedImporter {

	private static final Log LOG = PlatformUtil.getLog(GtfsFeedImporter.class);

	private static final double TRANSFER_MAX_METERS = 400;
	private static final int CANCEL_CHECK_ROWS = 1 << 14;
	private static final int BUFFER_BYTES = 1 << 18;
	private static final int RUN_CONNECTIONS = 1 << 20;

	/** Checks that a directory can take the given number of bytes; throws IOException if not. */
	public interface FreeSpaceCheck {
		void ensure(File dir, long requiredBytes) throws IOException;
	}

	/** The feed cannot be imported (the message says why), or the import was cancelled. */
	public static final class ImportException extends IOException {
		private final boolean cancelled;

		ImportException(String message, boolean cancelled, Throwable cause) {
			super(message, cause);
			this.cancelled = cancelled;
		}

		public boolean isCancelled() {
			return cancelled;
		}
	}

	/** What an import found. */
	public static final class Report {
		public long stopTimeRows;
		public int stops, trips, routes, connections, footpaths;
		public int tripsSkipped; // no usable times, unknown route or service, or listed in frequencies.txt
		public int rowsSkipped; // unknown trip or stop, or malformed time
		public int tripsInterpolated, tripsSorted;
		public long fileSize, millis;
		public final List<String> warnings = new ArrayList<>();
	}

	private final GtfsProgressListener progress;
	private FreeSpaceCheck spaceCheck = (dir, bytes) -> {
		long free = dir.getUsableSpace();
		if (free < bytes) {
			throw new IOException("Not enough free space for the GTFS index: need " + (bytes >> 20) + " MB, have "
					+ (free >> 20) + " MB");
		}
	};

	public GtfsFeedImporter(GtfsProgressListener progress) {
		this.progress = progress;
	}

	/** Replaces the default free-space test (File.getUsableSpace) of the target directory. */
	public GtfsFeedImporter setFreeSpaceCheck(FreeSpaceCheck check) {
		this.spaceCheck = check;
		return this;
	}

	public Report importFeed(File zip, File indexFile) throws IOException {
		return importFeeds(Collections.singletonList(zip), indexFile);
	}

	/** Imports the zips as one index; indices and service ids of different feeds never clash. */
	public Report importFeeds(List<File> zips, File indexFile) throws IOException {
		File dir = indexFile.getAbsoluteFile().getParentFile();
		dir.mkdirs();
		Build b = new Build(zips, new File(dir, indexFile.getName() + ".tmp"));
		try {
			b.deleteTemporaries(); // left over from a killed import
			b.run(dir);
			moveIntoPlace(b.out, indexFile);
			b.report.fileSize = indexFile.length();
			return b.report;
		} catch (OutOfMemoryError e) {
			b.release();
			throw new ImportException("Out of memory while importing the GTFS feed", false, e);
		} finally {
			b.deleteTemporaries();
		}
	}

	private static void moveIntoPlace(File tmp, File target) throws IOException {
		try {
			Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/** Chunked five-column connection store: no huge contiguous array and no copy on growth. */
	private static final class ConnStore {
		static final int SHIFT = 18, SIZE = 1 << SHIFT, MASK = SIZE - 1;
		int[][][] chunks = new int[5][0][];
		int size;

		void add(int dep, int arr, int from, int to, int trip) {
			int ch = size >> SHIFT, o = size & MASK;
			if (ch == chunks[0].length) {
				for (int k = 0; k < 5; k++) {
					chunks[k] = Arrays.copyOf(chunks[k], ch + 1);
					chunks[k][ch] = new int[SIZE];
				}
			}
			chunks[0][ch][o] = dep;
			chunks[1][ch][o] = arr;
			chunks[2][ch][o] = from;
			chunks[3][ch][o] = to;
			chunks[4][ch][o] = trip;
			size++;
		}

		int get(int col, int i) {
			return chunks[col][i >> SHIFT][i & MASK];
		}

		void clear() {
			chunks = new int[5][0][];
			size = 0;
		}
	}

	/** Buffered sequential int writer to a temporary file. */
	private static final class IntSink {
		final File file;
		final FileChannel ch;
		final ByteBuffer buf = ByteBuffer.allocateDirect(BUFFER_BYTES).order(ByteOrder.LITTLE_ENDIAN);
		long count;

		IntSink(File file) throws IOException {
			this.file = file;
			ch = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING);
		}

		void put(int v) throws IOException {
			if (!buf.hasRemaining()) {
				flush();
			}
			buf.putInt(v);
			count++;
		}

		void flush() throws IOException {
			((java.nio.Buffer) buf).flip();
			while (buf.hasRemaining()) {
				ch.write(buf);
			}
			((java.nio.Buffer) buf).clear();
		}

		void close() throws IOException {
			flush();
			ch.close();
		}
	}

	/** Raised inside a feed pass when stop_times turns out not to be grouped by trip. */
	private static final class UngroupedException extends RuntimeException {
		UngroupedException() {
			super(null, null, false, false);
		}
	}

	/** One import. */
	private final class Build {
		final List<File> zips;
		final File out;
		final Report report = new Report();
		final Map<String, Integer> pool = new HashMap<>();
		final List<String> strings = new ArrayList<>();
		final TIntArrayList stopLat = new TIntArrayList(), stopLon = new TIntArrayList(), stopName = new TIntArrayList(),
				stopId = new TIntArrayList(), stopParent = new TIntArrayList();
		final BitSet used = new BitSet();
		final TIntArrayList routeShort = new TIntArrayList(), routeLong = new TIntArrayList(),
				routeType = new TIntArrayList(), routeColor = new TIntArrayList(), routeId = new TIntArrayList();
		final TIntArrayList tripRoute = new TIntArrayList(), tripService = new TIntArrayList(),
				tripHeadsign = new TIntArrayList(), tripFirst = new TIntArrayList(), tripCount = new TIntArrayList();
		final TIntArrayList tfFrom = new TIntArrayList(), tfTo = new TIntArrayList(), tfType = new TIntArrayList(),
				tfTime = new TIntArrayList();
		final GtfsCalendar.Builder calendar = new GtfsCalendar.Builder();
		final List<File> runFiles = new ArrayList<>();
		final TIntArrayList runSizes = new TIntArrayList();
		final List<File> stopTimeFiles = new ArrayList<>(); // three per feed
		String zoneId;
		long totalConnections, totalStopTimes;
		long stopTimesBytesDone, stopTimesBytesTotal;

		// per feed
		ConnStore store = new ConnStore();
		IntSink[] sinks;
		long feedStopTimes; // entries written for this feed
		// per trip
		int[] bs = new int[256], ba = new int[256], bd = new int[256], bq = new int[256], bf = new int[256];
		int bn;
		// footpaths (CSR by from stop)
		int[] selfTransfer, footOffset, footTo, footValue;

		Build(List<File> zips, File out) {
			this.zips = zips;
			this.out = out;
		}

		void deleteTemporaries() {
			release();
			File[] files = out.getParentFile().listFiles();
			for (File f : files == null ? new File[0] : files) {
				if (f.getName().startsWith(out.getName())) {
					f.delete();
				}
			}
		}

		void release() {
			store.clear();
			if (sinks != null) {
				for (IntSink s : sinks) {
					try {
						s.ch.close();
					} catch (IOException e) {
						// closing on the way out
					}
				}
				sinks = null;
			}
		}

		void progress(String stage, int percent) throws ImportException {
			if (progress != null) {
				progress.onProgress(stage, percent);
			}
			checkCancelled();
		}

		void checkCancelled() throws ImportException {
			if (progress != null && progress.isCancelled()) {
				throw new ImportException("GTFS import cancelled", true, null);
			}
		}

		// Trip ids are unique and many (250 thousand for NYC): keep them as raw bytes, not as String objects,
		// and append them to the string table when the index is written.
		byte[] tripIdBytes = new byte[1 << 16];
		int tripIdLen;
		final TIntArrayList tripIdEnd = new TIntArrayList();

		void addTripId(String s) {
			byte[] b = s.getBytes(StandardCharsets.UTF_8);
			if (tripIdLen + b.length > tripIdBytes.length) {
				tripIdBytes = Arrays.copyOf(tripIdBytes, Math.max(tripIdBytes.length * 2, tripIdLen + b.length));
			}
			System.arraycopy(b, 0, tripIdBytes, tripIdLen, b.length);
			tripIdLen += b.length;
			tripIdEnd.add(tripIdLen);
		}

		int string(String s) {
			Integer i = pool.get(s);
			if (i == null) {
				i = strings.size();
				strings.add(s);
				pool.put(s, i);
			}
			return i;
		}

		void run(File dir) throws IOException {
			long start = System.currentTimeMillis();
			if (zips.isEmpty()) {
				throw new ImportException("No GTFS feed to import", false, null);
			}
			long required = 16L << 20;
			for (File zip : zips) {
				try (ZipFile z = openZip(zip)) {
					ZipEntry e = findEntries(z).get("stop_times.txt");
					long size = e == null ? 0 : (e.getSize() >= 0 ? e.getSize() : 10 * e.getCompressedSize());
					stopTimesBytesTotal += size;
					required += size + size / 8;
				}
			}
			spaceCheck.ensure(dir, required);
			long hash = 0xcbf29ce484222325L;
			for (int f = 0; f < zips.size(); f++) {
				File zip = zips.get(f);
				String feed = zip.getName().replaceFirst("(?i)\\.zip$", "");
				try (ZipFile z = openZip(zip)) {
					hash = importFeed(z, feed, f, hash);
				} catch (ZipException e) {
					throw new ImportException(zip.getName() + ": damaged zip file (" + e.getMessage() + ")", false, e);
				}
				progress("Reading " + feed, (int) (80 * stopTimesBytesDone / Math.max(1, stopTimesBytesTotal)));
			}
			if (zoneId == null) {
				report.warnings.add("No agency_timezone in the feed: assuming UTC, service days may be shifted");
				zoneId = "UTC";
			}
			if (totalConnections > Integer.MAX_VALUE / 8) {
				throw new ImportException("Feed is too large for the index format", false, null);
			}
			report.connections = (int) totalConnections;
			progress("Footpaths", 82);
			buildFootpaths();
			progress("Writing index", 85);
			write(hash);
			report.millis = System.currentTimeMillis() - start;
			LOG.info("GTFS import: " + report.stopTimeRows + " stop times, " + report.connections + " connections, "
					+ report.trips + " trips, " + report.stops + " stops, " + report.millis + " ms");
			progress("Done", 100);
		}

		ZipFile openZip(File zip) throws IOException {
			try {
				return new ZipFile(zip);
			} catch (ZipException e) {
				throw new ImportException(zip.getName() + ": not a valid zip file (" + e.getMessage() + ")", false, e);
			}
		}

		/** Entries by plain file name (feeds are sometimes zipped inside a folder); the shallowest wins. */
		Map<String, ZipEntry> findEntries(ZipFile z) {
			Map<String, ZipEntry> res = new HashMap<>();
			for (java.util.Enumeration<? extends ZipEntry> en = z.entries(); en.hasMoreElements(); ) {
				ZipEntry e = en.nextElement();
				String name = e.getName();
				String base = name.substring(name.lastIndexOf('/') + 1);
				ZipEntry old = res.get(base);
				if (!e.isDirectory() && (old == null || old.getName().length() > name.length())) {
					res.put(base, e);
				}
			}
			return res;
		}

		GtfsCsv csv(ZipFile z, ZipEntry e, String what) throws IOException {
			try {
				return new GtfsCsv(z.getInputStream(e));
			} catch (IOException ex) {
				throw new ImportException(what + ": " + ex.getMessage(), false, ex);
			}
		}

		long importFeed(ZipFile z, String feed, int feedIndex, long hash) throws IOException {
			Map<String, ZipEntry> files = findEntries(z);
			String zipName = new File(z.getName()).getName();
			for (String required : new String[] {"stops.txt", "routes.txt", "trips.txt", "stop_times.txt"}) {
				if (!files.containsKey(required)) {
					throw new ImportException(zipName + ": missing " + required, false, null);
				}
			}
			if (!files.containsKey("calendar.txt") && !files.containsKey("calendar_dates.txt")) {
				throw new ImportException(zipName + ": needs calendar.txt or calendar_dates.txt", false, null);
			}
			List<String> names = new ArrayList<>(files.keySet());
			Collections.sort(names);
			for (String n : names) {
				ZipEntry e = files.get(n);
				hash = fnv(fnv(fnv(hash, n.getBytes(StandardCharsets.UTF_8)), e.getSize()), e.getCrc());
			}
			hash = fnv(hash, feed.getBytes(StandardCharsets.UTF_8));
			int stopBase = stopLat.size(), routeBase = routeShort.size(), tripBase = tripRoute.size();

			// stops
			Map<String, Integer> stopMap = new HashMap<>();
			List<String> parents = new ArrayList<>();
			try (GtfsCsv c = csv(z, files.get("stops.txt"), zipName + " stops.txt")) {
				int id = c.column("stop_id"), name = c.column("stop_name"), lat = c.column("stop_lat"),
						lon = c.column("stop_lon"), parent = c.column("parent_station");
				while (c.next()) {
					String sid = c.get(id);
					if (sid.isEmpty() || stopMap.containsKey(sid)) {
						report.rowsSkipped++;
						continue;
					}
					stopMap.put(sid, stopLat.size() - stopBase);
					stopLat.add((int) Math.round(c.getDouble(lat, 0) * 1e7));
					stopLon.add((int) Math.round(c.getDouble(lon, 0) * 1e7));
					stopName.add(string(c.get(name)));
					stopId.add(string(feed + ":" + sid));
					stopParent.add(-1);
					parents.add(c.get(parent));
				}
			}
			for (int i = 0; i < parents.size(); i++) {
				Integer p = parents.get(i).isEmpty() ? null : stopMap.get(parents.get(i));
				if (p != null) {
					stopParent.set(stopBase + i, stopBase + p);
				}
			}
			// agency time zone
			ZipEntry agency = files.get("agency.txt");
			if (agency != null) {
				try (GtfsCsv c = csv(z, agency, zipName + " agency.txt")) {
					if (c.next() && !c.isEmpty(c.column("agency_timezone"))) {
						String tz = c.get(c.column("agency_timezone"));
						try {
							ZoneId.of(tz);
							if (zoneId == null) {
								zoneId = tz;
							} else if (!zoneId.equals(tz)) {
								report.warnings.add(zipName + ": time zone " + tz + " differs from " + zoneId + ", using the latter");
							}
						} catch (DateTimeException e) {
							report.warnings.add(zipName + ": unknown time zone '" + tz + "'");
						}
					}
				}
			}
			// routes
			Map<String, Integer> routeMap = new HashMap<>();
			try (GtfsCsv c = csv(z, files.get("routes.txt"), zipName + " routes.txt")) {
				int id = c.column("route_id"), sn = c.column("route_short_name"), ln = c.column("route_long_name"),
						ty = c.column("route_type"), co = c.column("route_color");
				while (c.next()) {
					routeMap.put(c.get(id), routeShort.size() - routeBase);
					routeShort.add(string(c.get(sn)));
					routeLong.add(string(c.get(ln)));
					routeId.add(string(feed + ":" + c.get(id)));
					routeType.add(c.getInt(ty, 3));
					int color = GtfsIndex.NO_COLOR;
					try {
						color = c.isEmpty(co) ? color : Integer.parseInt(c.get(co), 16) & 0xFFFFFF;
					} catch (NumberFormatException e) {
						// no colour
					}
					routeColor.add(color);
				}
			}
			// calendar
			calendar.startFeed();
			if (files.containsKey("calendar.txt")) {
				try (GtfsCsv c = csv(z, files.get("calendar.txt"), zipName + " calendar.txt")) {
					calendar.readCalendar(c);
				}
			}
			if (files.containsKey("calendar_dates.txt")) {
				try (GtfsCsv c = csv(z, files.get("calendar_dates.txt"), zipName + " calendar_dates.txt")) {
					calendar.readCalendarDates(c);
				}
			}
			// frequencies: not supported, the trips they describe are skipped
			Set<String> frequencyTrips = new HashSet<>();
			if (files.containsKey("frequencies.txt")) {
				try (GtfsCsv c = csv(z, files.get("frequencies.txt"), zipName + " frequencies.txt")) {
					int id = c.column("trip_id");
					while (c.next()) {
						frequencyTrips.add(c.get(id));
					}
				}
				if (!frequencyTrips.isEmpty()) {
					report.warnings.add(zipName + ": frequencies.txt lists " + frequencyTrips.size()
							+ " trips, frequency-based trips are not supported and are skipped");
				}
			}
			// trips
			Map<String, Integer> tripMap = new HashMap<>();
			boolean[] skip;
			try (GtfsCsv c = csv(z, files.get("trips.txt"), zipName + " trips.txt")) {
				int id = c.column("trip_id"), rt = c.column("route_id"), sv = c.column("service_id"),
						hs = c.column("trip_headsign");
				TIntArrayList skipped = new TIntArrayList();
				while (c.next()) {
					String tid = c.get(id);
					if (tripMap.containsKey(tid)) {
						report.rowsSkipped++;
						continue;
					}
					Integer r = routeMap.get(c.get(rt));
					int s = calendar.serviceIndex(c.get(sv));
					tripMap.put(tid, tripRoute.size() - tripBase);
					tripRoute.add(r == null ? -1 : routeBase + r);
					tripService.add(s);
					tripHeadsign.add(string(c.get(hs)));
					addTripId(feed + ":" + tid);
					tripFirst.add(0);
					tripCount.add(0);
					if (r == null || s < 0 || frequencyTrips.contains(tid)) {
						skipped.add(tripRoute.size() - 1 - tripBase);
					}
				}
				skip = new boolean[tripMap.size()];
				for (int i = 0; i < skipped.size(); i++) {
					skip[skipped.get(i)] = true;
				}
				report.tripsSkipped += skipped.size();
			}
			// transfers
			if (files.containsKey("transfers.txt")) {
				try (GtfsCsv c = csv(z, files.get("transfers.txt"), zipName + " transfers.txt")) {
					int f = c.column("from_stop_id"), t = c.column("to_stop_id"), ty = c.column("transfer_type"),
							mt = c.column("min_transfer_time");
					while (c.next()) {
						Integer a = stopMap.get(c.get(f)), b = stopMap.get(c.get(t));
						if (a != null && b != null) {
							tfFrom.add(stopBase + a);
							tfTo.add(stopBase + b);
							tfType.add(c.getInt(ty, 0));
							tfTime.add(c.getInt(mt, -1));
						}
					}
				}
			}
			// stop_times
			stopTimeEntries(z, files.get("stop_times.txt"), zipName, stopMap, tripMap, skip, stopBase, tripBase);
			report.stops += stopMap.size();
			report.trips += tripMap.size();
			report.routes += routeMap.size();
			return hash;
		}

		void stopTimeEntries(ZipFile z, ZipEntry entry, String zipName, Map<String, Integer> stopMap,
		                     Map<String, Integer> tripMap, boolean[] skip, int stopBase, int tripBase) throws IOException {
			boolean buffered = false;
			while (true) {
				resetFeed();
				long rowsBefore = report.stopTimeRows;
				int skippedBefore = report.rowsSkipped, sortedBefore = report.tripsSorted,
						interpBefore = report.tripsInterpolated, tripSkipBefore = report.tripsSkipped;
				try (GtfsCsv c = csv(z, entry, zipName + " stop_times.txt")) {
					readStopTimes(c, stopMap, tripMap, skip, stopBase, tripBase, buffered);
					break;
				} catch (UngroupedException e) {
					// rows of one trip are not adjacent: start over, collecting and sorting all rows first
					buffered = true;
					report.stopTimeRows = rowsBefore;
					report.rowsSkipped = skippedBefore;
					report.tripsSorted = sortedBefore;
					report.tripsInterpolated = interpBefore;
					report.tripsSkipped = tripSkipBefore;
				}
			}
			stopTimesBytesDone += entry.getSize() >= 0 ? entry.getSize() : 0;
			for (IntSink s : sinks) {
				s.close();
				stopTimeFiles.add(s.file);
			}
			totalStopTimes += feedStopTimes;
			sinks = null;
			flushRun();
		}

		/** Starts the per-feed output (connections in memory, stop times in three temporary columns). */
		void resetFeed() throws IOException {
			release();
			store = new ConnStore();
			feedStopTimes = 0;
			bn = 0;
			sinks = new IntSink[3];
			for (int i = 0; i < 3; i++) {
				sinks[i] = new IntSink(new File(out.getPath() + ".st" + stopTimeFiles.size() / 3 + "-" + i));
			}
		}

		void readStopTimes(GtfsCsv c, Map<String, Integer> stopMap, Map<String, Integer> tripMap, boolean[] skip,
		                   int stopBase, int tripBase, boolean buffered) throws IOException {
			int cTrip = c.column("trip_id"), cArr = c.column("arrival_time"), cDep = c.column("departure_time"),
					cStop = c.column("stop_id"), cSeq = c.column("stop_sequence"), cPick = c.column("pickup_type"),
					cDrop = c.column("drop_off_type");
			if (cTrip < 0 || cStop < 0) {
				throw new ImportException("stop_times.txt has no trip_id or stop_id column", false, null);
			}
			// the zip entry size is counted by the caller; progress here is by rows
			byte[] lastTrip = new byte[0];
			int lastLen = -1, cur = -1; // cur: local trip index, -2 skipped/unknown
			BitSet closed = new BitSet();
			TIntArrayList rTrip = new TIntArrayList(), rSeq = new TIntArrayList(), rStop = new TIntArrayList(),
					rArr = new TIntArrayList(), rDep = new TIntArrayList(), rFlag = new TIntArrayList();
			long rows = 0;
			while (c.next()) {
				report.stopTimeRows++;
				if ((++rows & (CANCEL_CHECK_ROWS - 1)) == 0) {
					checkCancelled();
				}
				if (buffered || lastLen < 0 || !c.fieldEquals(cTrip, lastTrip, lastLen)) {
					if (!buffered) {
						endTrip(cur, tripBase);
					}
					lastTrip = c.getBytes(cTrip);
					lastLen = lastTrip.length;
					Integer t = tripMap.get(new String(lastTrip, StandardCharsets.UTF_8));
					cur = t == null || skip[t] ? -2 : t;
					if (!buffered && cur >= 0) {
						if (closed.get(cur)) {
							throw new UngroupedException();
						}
						closed.set(cur);
					}
				}
				if (cur < 0) {
					if (cur == -2) {
						report.rowsSkipped++;
					}
					continue;
				}
				Integer stop = stopMap.get(c.get(cStop));
				int arr = c.getTime(cArr), dep = c.getTime(cDep);
				if (stop == null || arr == GtfsCsv.TIME_MALFORMED || dep == GtfsCsv.TIME_MALFORMED) {
					report.rowsSkipped++;
					continue;
				}
				int flags = (c.getInt(cPick, 0) == 1 ? GtfsIndex.FLAG_NO_PICKUP : 0)
						| (c.getInt(cDrop, 0) == 1 ? GtfsIndex.FLAG_NO_DROP_OFF : 0);
				int seq = c.getInt(cSeq, buffered ? rTrip.size() : bn);
				if (buffered) {
					rTrip.add(cur);
					rSeq.add(seq);
					rStop.add(stopBase + stop);
					rArr.add(arr);
					rDep.add(dep);
					rFlag.add(flags);
				} else {
					addToTrip(stopBase + stop, arr, dep, seq, flags);
				}
			}
			if (!buffered) {
				endTrip(cur, tripBase);
				return;
			}
			// group the collected rows by trip (counting sort keeps file order inside a trip)
			int nt = tripMap.size(), n = rTrip.size();
			int[] off = new int[nt + 1];
			for (int i = 0; i < n; i++) {
				off[rTrip.get(i) + 1]++;
			}
			for (int t = 0; t < nt; t++) {
				off[t + 1] += off[t];
			}
			int[] order = new int[n], fill = Arrays.copyOf(off, nt);
			for (int i = 0; i < n; i++) {
				order[fill[rTrip.get(i)]++] = i;
			}
			for (int t = 0; t < nt; t++) {
				for (int j = off[t]; j < off[t + 1]; j++) {
					int i = order[j];
					addToTrip(rStop.get(i), rArr.get(i), rDep.get(i), rSeq.get(i), rFlag.get(i));
				}
				endTrip(t, tripBase);
			}
		}

		void addToTrip(int stop, int arr, int dep, int seq, int flags) {
			if (bn == bs.length) {
				int cap = bn * 2;
				bs = Arrays.copyOf(bs, cap);
				ba = Arrays.copyOf(ba, cap);
				bd = Arrays.copyOf(bd, cap);
				bq = Arrays.copyOf(bq, cap);
				bf = Arrays.copyOf(bf, cap);
			}
			bs[bn] = stop;
			ba[bn] = arr;
			bd[bn] = dep;
			bq[bn] = seq;
			bf[bn] = flags;
			bn++;
		}

		/** Orders, completes and writes the stops collected for one trip (local index, or < 0 for none). */
		void endTrip(int localTrip, int tripBase) throws IOException {
			int n = bn;
			bn = 0;
			if (localTrip < 0 || n == 0) {
				return;
			}
			if (n < 2) {
				report.tripsSkipped++;
				return;
			}
			boolean sorted = true;
			for (int i = 1; i < n; i++) {
				if (bq[i] < bq[i - 1]) {
					sorted = false;
					break;
				}
			}
			if (!sorted) {
				report.tripsSorted++;
				long[] keys = new long[n];
				for (int i = 0; i < n; i++) {
					keys[i] = ((long) bq[i] << 32) | i; // sequence first, file order breaks ties
				}
				Arrays.sort(keys);
				int[] s = new int[n], a = new int[n], d = new int[n], f = new int[n];
				for (int i = 0; i < n; i++) {
					int j = (int) keys[i];
					s[i] = bs[j];
					a[i] = ba[j];
					d[i] = bd[j];
					f[i] = bf[j];
				}
				System.arraycopy(s, 0, bs, 0, n);
				System.arraycopy(a, 0, ba, 0, n);
				System.arraycopy(d, 0, bd, 0, n);
				System.arraycopy(f, 0, bf, 0, n);
			}
			// A blank arrival or departure takes the other one; stops with neither are interpolated
			// between the neighbouring timed stops. First and last stop must have a time.
			boolean interpolate = false;
			for (int i = 0; i < n; i++) {
				if (ba[i] < 0 && bd[i] >= 0) {
					ba[i] = bd[i];
				} else if (bd[i] < 0 && ba[i] >= 0) {
					bd[i] = ba[i];
				} else if (ba[i] < 0) {
					interpolate = true;
				}
			}
			if (interpolate) {
				if (ba[0] < 0 || ba[n - 1] < 0) {
					report.tripsSkipped++;
					return;
				}
				int prev = 0;
				for (int i = 1; i < n; i++) {
					if (ba[i] >= 0) {
						for (int j = prev + 1; j < i; j++) {
							int t = bd[prev] + (int) ((long) (ba[i] - bd[prev]) * (j - prev) / (i - prev));
							ba[j] = t;
							bd[j] = t;
						}
						prev = i;
					}
				}
				report.tripsInterpolated++;
			}
			int trip = tripBase + localTrip;
			tripFirst.set(trip, (int) (totalStopTimes + feedStopTimes));
			tripCount.set(trip, n);
			for (int i = 0; i < n; i++) {
				if (i > 0) { // times never run backwards
					ba[i] = Math.max(ba[i], bd[i - 1]);
				}
				bd[i] = Math.max(bd[i], ba[i]);
				sinks[0].put(bs[i] | bf[i]);
				sinks[1].put(ba[i]);
				sinks[2].put(bd[i]);
				used.set(bs[i]);
			}
			feedStopTimes += n;
			for (int i = 0; i + 1 < n; i++) {
				int flags = ((bf[i] & GtfsIndex.FLAG_NO_PICKUP) != 0 ? 1 : 0)
						| ((bf[i + 1] & GtfsIndex.FLAG_NO_DROP_OFF) != 0 ? 2 : 0);
				store.add(bd[i], ba[i + 1], bs[i], bs[i + 1], trip << 2 | flags);
			}
			if (store.size >= RUN_CONNECTIONS) {
				flushRun(); // bounds the memory of the sort whatever the size of the feed
			}
		}

		/** Sorts the feed's connections by departure and writes them as a run file (5 columns). */
		void flushRun() throws IOException {
			int n = store.size;
			if (n == 0) {
				return;
			}
			long[] keys = new long[n];
			for (int i = 0; i < n; i++) {
				keys[i] = ((long) store.get(0, i) << 32) | i;
			}
			Arrays.sort(keys);
			File f = new File(out.getPath() + ".run" + runFiles.size());
			try (FileChannel ch = FileChannel.open(f.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING)) {
				ByteBuffer bb = ByteBuffer.allocateDirect(BUFFER_BYTES).order(ByteOrder.LITTLE_ENDIAN);
				for (int col = 0; col < 5; col++) {
					for (int i = 0; i < n; i++) {
						if (!bb.hasRemaining()) {
							((java.nio.Buffer) bb).flip();
							while (bb.hasRemaining()) {
								ch.write(bb);
							}
							((java.nio.Buffer) bb).clear();
						}
						bb.putInt(store.get(col, (int) keys[i]));
					}
				}
				((java.nio.Buffer) bb).flip();
				while (bb.hasRemaining()) {
					ch.write(bb);
				}
			}
			runFiles.add(f);
			runSizes.add(n);
			totalConnections += n;
			store.clear();
		}

		// ------------------------------------------------------------ footpaths

		void buildFootpaths() {
			int ns = stopLat.size();
			selfTransfer = new int[ns];
			int[] lat = stopLat.toArray(), lon = stopLon.toArray();
			Map<Integer, TIntArrayList> children = new HashMap<>();
			for (int i = 0; i < ns; i++) {
				if (stopParent.get(i) >= 0) {
					children.computeIfAbsent(stopParent.get(i), k -> new TIntArrayList()).add(i);
				}
			}
			// transfers.txt rows name stations as well as platforms: expand them to the stops trips use
			Map<Long, Integer> edges = new LinkedHashMap<>(); // from << 32 | to -> value, -1 = banned
			for (int r = 0; r < tfFrom.size(); r++) {
				int type = tfType.get(r), time = tfTime.get(r);
				for (int x : expand(tfFrom.get(r), children)) {
					for (int y : expand(tfTo.get(r), children)) {
						long key = ((long) x << 32) | y;
						if (type == 3) {
							edges.put(key, -1);
						} else if (x == y) {
							if (time >= 0) {
								selfTransfer[x] = time;
							}
						} else if (time >= 0 || type == 1) {
							edges.merge(key, Math.max(time, 0) << 1 | 1, GtfsFeedImporter::mergeEdge);
						} else {
							double m = GtfsIndex.distanceMeters(lat[x], lon[x], lat[y], lon[y]);
							edges.putIfAbsent(key, (int) Math.round(m) << 1);
						}
					}
				}
			}
			TIntArrayList usedStops = new TIntArrayList();
			for (int i = used.nextSetBit(0); i >= 0; i = used.nextSetBit(i + 1)) {
				usedStops.add(i);
			}
			GtfsStopGrid grid = new GtfsStopGrid(lat, lon, usedStops);
			TIntArrayList eFrom = new TIntArrayList(), eTo = new TIntArrayList(), eValue = new TIntArrayList();
			for (Map.Entry<Long, Integer> e : edges.entrySet()) {
				if (e.getValue() >= 0) {
					eFrom.add((int) (e.getKey() >> 32));
					eTo.add((int) (long) e.getKey());
					eValue.add(e.getValue());
				}
			}
			TIntArrayList near = new TIntArrayList();
			for (int i = 0; i < usedStops.size(); i++) {
				int a = usedStops.get(i);
				near.clear();
				grid.within(lat[a], lon[a], TRANSFER_MAX_METERS, near);
				for (int j = 0; j < near.size(); j++) {
					int b = near.get(j);
					if (b != a && !edges.containsKey(((long) a << 32) | b)) {
						eFrom.add(a);
						eTo.add(b);
						eValue.add((int) Math.round(GtfsIndex.distanceMeters(lat[a], lon[a], lat[b], lon[b])) << 1);
					}
				}
			}
			int nf = eFrom.size();
			footOffset = new int[ns + 1];
			for (int i = 0; i < nf; i++) {
				footOffset[eFrom.get(i) + 1]++;
			}
			for (int i = 0; i < ns; i++) {
				footOffset[i + 1] += footOffset[i];
			}
			footTo = new int[nf];
			footValue = new int[nf];
			int[] cur = Arrays.copyOf(footOffset, ns);
			for (int i = 0; i < nf; i++) {
				int p = cur[eFrom.get(i)]++;
				footTo[p] = eTo.get(i);
				footValue[p] = eValue.get(i);
			}
			report.footpaths = nf;
		}

		int[] expand(int stop, Map<Integer, TIntArrayList> children) {
			TIntArrayList res = new TIntArrayList();
			if (used.get(stop)) {
				res.add(stop);
			}
			TIntArrayList kids = children.get(stop);
			for (int i = 0; kids != null && i < kids.size(); i++) {
				if (used.get(kids.get(i))) {
					res.add(kids.get(i));
				}
			}
			return res.toArray();
		}

		// ------------------------------------------------------------ output

		void write(long feedHash) throws IOException {
			int tzString = string(zoneId); // register before the string table size is fixed
			int nPool = strings.size(), nStr = nPool + tripIdEnd.size(), ns = stopLat.size(), nt = tripRoute.size(), nr = routeShort.size(),
					nf = footTo.length;
			GtfsCalendar cal = calendar.build(ZoneId.of(zoneId));
			byte[][] sb = new byte[nStr][];
			int strBytes = tripIdLen;
			for (int i = 0; i < nPool; i++) {
				sb[i] = strings.get(i).getBytes(StandardCharsets.UTF_8);
				strBytes += sb[i].length;
			}
			for (int i = 0; i < tripIdEnd.size(); i++) { // trip ids follow the pooled strings
				int from = i == 0 ? 0 : tripIdEnd.get(i - 1);
				sb[nPool + i] = Arrays.copyOfRange(tripIdBytes, from, tripIdEnd.get(i));
			}
			int[] counts = new int[GtfsIndex.COUNTS];
			counts[GtfsIndex.C_STR] = nStr;
			counts[GtfsIndex.C_STR_BYTES] = strBytes;
			counts[GtfsIndex.C_STOPS] = ns;
			counts[GtfsIndex.C_FOOT] = nf;
			counts[GtfsIndex.C_ROUTES] = nr;
			counts[GtfsIndex.C_TRIPS] = nt;
			counts[GtfsIndex.C_SERVICES] = cal.getServiceCount();
			counts[GtfsIndex.C_EXCEPTIONS] = cal.exceptionDay.length;
			counts[GtfsIndex.C_CONN] = (int) totalConnections;
			counts[GtfsIndex.C_STOP_TIMES] = (int) totalStopTimes;
			long[] off = GtfsIndex.sectionOffsets(counts);
			ByteBuffer t = ByteBuffer.allocate((int) off[GtfsIndex.S_CONN]).order(ByteOrder.LITTLE_ENDIAN);
			int from = calendar.getValidFrom(), to = calendar.getValidTo();
			GtfsIndex.putHeader(t, zips.size(), feedHash, from, to, tzString, counts);
			((java.nio.Buffer) t).position((int) off[GtfsIndex.S_STR_OFFS]);
			int acc = 0;
			for (byte[] b : sb) {
				t.putInt(acc);
				acc += b.length;
			}
			t.putInt(acc);
			((java.nio.Buffer) t).position((int) off[GtfsIndex.S_STR_BYTES]);
			for (byte[] b : sb) {
				t.put(b);
			}
			((java.nio.Buffer) t).position((int) off[GtfsIndex.S_STOPS]);
			put(t, stopLat);
			put(t, stopLon);
			put(t, stopName);
			put(t, stopId);
			put(t, stopParent);
			put(t, selfTransfer);
			put(t, footOffset);
			((java.nio.Buffer) t).position((int) off[GtfsIndex.S_FOOT]);
			put(t, footTo);
			put(t, footValue);
			((java.nio.Buffer) t).position((int) off[GtfsIndex.S_ROUTES]);
			put(t, routeShort);
			put(t, routeLong);
			put(t, routeType);
			put(t, routeColor);
			put(t, routeId);
			((java.nio.Buffer) t).position((int) off[GtfsIndex.S_TRIPS]);
			put(t, tripRoute);
			put(t, tripService);
			put(t, tripHeadsign);
			put(t, tripFirst);
			put(t, tripCount);
			for (int i = 0; i < nt; i++) {
				t.putInt(nPool + i); // trips are numbered in the order their ids were added
			}
			((java.nio.Buffer) t).position((int) off[GtfsIndex.S_CALENDAR]);
			put(t, cal.weekdayMask);
			put(t, cal.startDay);
			put(t, cal.endDay);
			put(t, cal.exceptionOffset);
			((java.nio.Buffer) t).position((int) off[GtfsIndex.S_EXCEPTIONS]);
			put(t, cal.exceptionDay);
			put(t, cal.exceptionType);
			((java.nio.Buffer) t).position(0);
			try (FileChannel ch = FileChannel.open(out.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					StandardOpenOption.READ, StandardOpenOption.TRUNCATE_EXISTING)) {
				while (t.hasRemaining()) {
					ch.write(t);
				}
				merge(ch, off[GtfsIndex.S_CONN], (int) totalConnections);
				progress("Writing index", 93);
				ch.position(off[GtfsIndex.S_STOP_TIMES]);
				for (int col = 0; col < 3; col++) {
					for (int f = 0; f < zips.size(); f++) {
						File src = stopTimeFiles.get(f * 3 + col);
						try (FileChannel in = FileChannel.open(src.toPath(), StandardOpenOption.READ)) {
							long done = 0, size = in.size();
							while (done < size) {
								done += in.transferTo(done, size - done, ch); // appends at ch's position
							}
						}
					}
				}
				if (ch.size() < off[GtfsIndex.SECTIONS]) {
					ch.write(ByteBuffer.wrap(new byte[1]), off[GtfsIndex.SECTIONS] - 1); // alignment padding
				}
				checksum(ch, off[GtfsIndex.SECTIONS]);
				ch.force(true);
			}
		}

		void checksum(FileChannel ch, long size) throws IOException {
			CRC32 crc = new CRC32();
			ByteBuffer bb = ByteBuffer.allocateDirect(1 << 20);
			long pos = GtfsIndex.CRC_FROM;
			while (pos < size) {
				((java.nio.Buffer) bb).clear();
				int r = ch.read(bb, pos);
				if (r <= 0) {
					break;
				}
				((java.nio.Buffer) bb).flip();
				crc.update(bb);
				pos += r;
			}
			ByteBuffer cb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
			cb.putLong(crc.getValue());
			((java.nio.Buffer) cb).flip();
			ch.write(cb, GtfsIndex.H_CRC);
		}

		/** K-way merge of the sorted runs into the five connection columns. */
		void merge(FileChannel ch, long base, int total) throws IOException {
			int k = runFiles.size();
			IntBuffer[][] cols = new IntBuffer[k][5];
			int[] pos = new int[k], cnt = new int[k];
			List<FileChannel> open = new ArrayList<>();
			try {
				for (int r = 0; r < k; r++) {
					FileChannel rc = FileChannel.open(runFiles.get(r).toPath(), StandardOpenOption.READ);
					open.add(rc);
					cnt[r] = runSizes.get(r);
					ByteBuffer mb = rc.map(FileChannel.MapMode.READ_ONLY, 0, rc.size());
					for (int col = 0; col < 5; col++) {
						ByteBuffer d = mb.duplicate();
						((java.nio.Buffer) d).position(col * cnt[r] * 4);
						((java.nio.Buffer) d).limit((col + 1) * cnt[r] * 4);
						cols[r][col] = d.slice().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
					}
				}
				ByteBuffer[] ob = new ByteBuffer[5];
				long[] wpos = new long[5];
				for (int col = 0; col < 5; col++) {
					ob[col] = ByteBuffer.allocateDirect(BUFFER_BYTES).order(ByteOrder.LITTLE_ENDIAN);
					wpos[col] = base + 4L * total * col;
				}
				int written = 0;
				while (true) {
					int best = -1, bestDep = Integer.MAX_VALUE;
					for (int r = 0; r < k; r++) {
						if (pos[r] < cnt[r] && cols[r][0].get(pos[r]) < bestDep) {
							bestDep = cols[r][0].get(pos[r]);
							best = r;
						}
					}
					if (best < 0) {
						break;
					}
					for (int col = 0; col < 5; col++) {
						if (!ob[col].hasRemaining()) {
							wpos[col] = drain(ch, ob[col], wpos[col]);
						}
						ob[col].putInt(cols[best][col].get(pos[best]));
					}
					pos[best]++;
					if ((++written & 0xFFFFF) == 0) {
						checkCancelled();
					}
				}
				for (int col = 0; col < 5; col++) {
					drain(ch, ob[col], wpos[col]);
				}
				if (written != total) {
					throw new IOException("Internal error: merged " + written + " connections, expected " + total);
				}
			} finally {
				for (FileChannel rc : open) {
					rc.close();
				}
			}
		}

		long drain(FileChannel ch, ByteBuffer bb, long pos) throws IOException {
			((java.nio.Buffer) bb).flip();
			while (bb.hasRemaining()) {
				pos += ch.write(bb, pos);
			}
			((java.nio.Buffer) bb).clear();
			return pos;
		}
	}

	/** Two values for one stop pair: a ban wins, then a transfers.txt time, then the smaller one. */
	private static int mergeEdge(int a, int b) {
		if (a < 0) {
			return a;
		}
		if ((a & 1) != (b & 1)) {
			return (a & 1) == 1 ? a : b;
		}
		return Math.min(a, b);
	}

	private static void put(ByteBuffer b, TIntArrayList l) {
		for (int i = 0; i < l.size(); i++) {
			b.putInt(l.get(i));
		}
	}

	private static void put(ByteBuffer b, int[] a) {
		for (int v : a) {
			b.putInt(v);
		}
	}

	private static long fnv(long h, long v) {
		for (int i = 0; i < 8; i++) {
			h = (h ^ ((v >>> (8 * i)) & 0xff)) * 0x100000001b3L;
		}
		return h;
	}

	private static long fnv(long h, byte[] data) {
		for (byte b : data) {
			h = (h ^ (b & 0xff)) * 0x100000001b3L;
		}
		return h;
	}
}
