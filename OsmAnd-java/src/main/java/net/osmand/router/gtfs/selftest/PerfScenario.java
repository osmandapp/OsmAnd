package net.osmand.router.gtfs.selftest;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.router.TransportRouteProviders;
import net.osmand.router.TransportRouteResult;
import net.osmand.router.gtfs.GtfsFeedImporter;
import net.osmand.router.gtfs.GtfsIndex;
import net.osmand.router.gtfs.GtfsIndexSource;
import net.osmand.router.gtfs.GtfsTransitRouteProvider;

/**
 * Repeated query timing (p50, p95), index open time, import heap peak on the smallest feed, and optionally
 * (option constrained=1, off by default because an OutOfMemoryError in a live app can hurt other threads) a full
 * import with the Java heap squeezed by a ballast to headroom=MB.
 */
final class PerfScenario {

	private PerfScenario() {
	}

	static void run(SelfTestEnv env, ScenarioResult r) throws Exception {
		File idx = env.ensureIndex();
		if (idx == null) {
			r.skip("no index and no zips");
			return;
		}
		int n = 30;
		try {
			n = Integer.parseInt(env.options.getOrDefault("queries", "30"));
		} catch (NumberFormatException ignored) {
			// keep the default
		}
		Runtime rt = Runtime.getRuntime();
		r.put("javaMaxMb", rt.maxMemory() >> 20);
		r.put("processors", rt.availableProcessors());

		long t0 = System.nanoTime();
		GtfsIndex opened = GtfsIndex.open(idx);
		r.put("indexOpenMs", (System.nanoTime() - t0) / 1_000_000);
		r.put("indexBytes", opened.getFileSize());
		t0 = System.nanoTime();
		boolean checksum = GtfsIndex.open(idx, true).verifyChecksum();
		r.put("indexOpenAndCrcMs", (System.nanoTime() - t0) / 1_000_000);
		r.expect(checksum, "index checksum");

		GtfsIndexSource source = new GtfsIndexSource(idx);
		t0 = System.nanoTime();
		GtfsIndexSource.Loaded loaded = source.get();
		r.put("sourceFirstGetMs", (System.nanoTime() - t0) / 1_000_000);
		r.expect(loaded != null, "index not usable");
		if (loaded == null) {
			return;
		}
		ZoneId zone = loaded.index.getZone();
		LocalDate from = LocalDate.ofEpochDay(loaded.index.getValidFrom()), to = LocalDate.ofEpochDay(loaded.index.getValidTo());
		LocalDate today = Instant.ofEpochSecond(env.nowSec.getAsLong()).atZone(zone).toLocalDate();
		LocalDate day = today.plusDays(1);
		if (day.isBefore(from) || day.isAfter(to)) {
			day = from;
		}
		TransportRouteProviders providers = new TransportRouteProviders(env.nowSec);
		providers.add(new GtfsTransitRouteProvider(source, () -> 800));
		SelfTestPlaces.Od od = SelfTestPlaces.all().get(0);
		long base = SelfTestUtil.epoch(day, LocalTime.of(6, 0), zone);

		List<Long> ms = new ArrayList<>();
		int answered = 0;
		long worst = 0;
		for (int i = 0; i < n; i++) {
			long dep = base + i * 47L * 60;
			long q0 = System.nanoTime();
			List<TransportRouteResult> res = providers.calculate(od.from, od.to, dep, new HashMap<String, String>(), null, null);
			long took = (System.nanoTime() - q0) / 1_000_000;
			ms.add(took);
			worst = Math.max(worst, took);
			if (res != null) {
				answered++;
			}
		}
		long[] sorted = SelfTestUtil.sortedCopy(ms);
		r.put("queries", n);
		r.put("answered", answered);
		r.put("firstQueryMs", ms.get(0));
		r.put("p50Ms", SelfTestUtil.percentile(sorted, 0.5));
		r.put("p95Ms", SelfTestUtil.percentile(sorted, 0.95));
		r.put("maxMs", worst);
		r.put("memoryAfterQueries", PlanMatrixScenario.memory(env));
		r.expect(answered >= n * 0.8, "only " + answered + " of " + n + " queries answered");
		r.expect(SelfTestUtil.percentile(sorted, 0.95) < 5000, "p95 above 5 s");

		// import heap peak on the smallest feed
		List<File> small = SelfTestUtil.smallZips(env.zips, 1);
		if (!small.isEmpty()) {
			File out = new File(env.workDir, "perf-import/gtfs.index");
			SelfTestUtil.deleteTree(out.getParentFile());
			System.gc();
			SelfTestUtil.HeapSampler s = SelfTestUtil.HeapSampler.start();
			long i0 = System.nanoTime();
			GtfsFeedImporter.Report rep = new GtfsFeedImporter(null).importFeeds(small, out);
			long importMs = (System.nanoTime() - i0) / 1_000_000;
			long peak = s.stop();
			r.put("smallImportFeed", small.get(0).getName());
			r.put("smallImportMs", importMs);
			r.put("smallImportJavaPeakMb", SelfTestUtil.mb(peak));
			r.put("smallImportTrips", rep.trips);
			SelfTestUtil.deleteTree(out.getParentFile());
		}

		if (env.option("constrained") && !env.zips.isEmpty()) {
			constrained(env, r);
		} else {
			r.note("constrained-heap import not run (option constrained=1 enables it)");
		}
	}

	private static void constrained(SelfTestEnv env, ScenarioResult r) throws Exception {
		int headroomMb = 160;
		try {
			headroomMb = Integer.parseInt(env.options.getOrDefault("headroom", "160"));
		} catch (NumberFormatException ignored) {
			// keep the default
		}
		Runtime rt = Runtime.getRuntime();
		List<byte[]> ballast = new ArrayList<>();
		File out = new File(env.workDir, "perf-constrained/gtfs.index");
		SelfTestUtil.deleteTree(out.getParentFile());
		Map<String, Object> m = new LinkedHashMap<>();
		try {
			long max = rt.maxMemory();
			long target = (long) headroomMb << 20;
			while (max - (rt.totalMemory() - rt.freeMemory()) > target + (8 << 20) && ballast.size() < 4096) {
				ballast.add(new byte[4 << 20]);
			}
			m.put("headroomMb", headroomMb);
			m.put("ballastMb", ballast.size() * 4L);
			m.put("javaMaxMb", max >> 20);
			SelfTestUtil.HeapSampler s = SelfTestUtil.HeapSampler.start();
			long t0 = System.nanoTime();
			String outcome;
			try {
				GtfsFeedImporter.Report rep = new GtfsFeedImporter(null).importFeeds(new ArrayList<>(env.zips), out);
				outcome = "imported " + rep.trips + " trips";
			} catch (GtfsFeedImporter.ImportException e) {
				outcome = "ImportException: " + e.getMessage();
			} catch (OutOfMemoryError e) {
				outcome = "OutOfMemoryError escaped the importer";
			}
			m.put("outcome", outcome);
			m.put("ms", (System.nanoTime() - t0) / 1_000_000);
			m.put("javaPeakMb", SelfTestUtil.mb(s.stop()));
			m.put("indexExistsAfter", out.isFile());
			m.put("leftovers", SelfTestUtil.leftovers(out.getParentFile()));
		} finally {
			ballast.clear();
			SelfTestUtil.deleteTree(out.getParentFile());
		}
		r.put("constrainedImport", m);
		r.expect(((List<?>) m.getOrDefault("leftovers", new ArrayList<>())).isEmpty(), "constrained import left files behind");
	}
}
