package net.osmand.router.gtfs.selftest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.router.gtfs.GtfsFeedImporter;
import net.osmand.router.gtfs.GtfsFeedStore;
import net.osmand.router.gtfs.GtfsIndex;
import net.osmand.router.gtfs.GtfsProgressListener;

/**
 * Import, failure and removal scenarios. They use scratch stores below the work directory and the same
 * classes the plugin's import uses (GtfsFeedStore.addFeed and rebuild, GtfsFeedImporter), in the same order as
 * GtfsFeedUpdate: copy, rebuild, undo on failure. The real app index is never touched.
 */
final class ImportScenarios {

	private ImportScenarios() {
	}

	/** Records progress callbacks and optionally cancels. */
	static final class Probe implements GtfsProgressListener {
		int calls, regressions, lastPercent = -1, maxPercent;
		final Set<String> stages = new LinkedHashSet<>();
		int cancelAtPercent = Integer.MAX_VALUE;
		boolean cancelNow;
		long firstCallNanos, lastCallNanos;

		@Override
		public void onProgress(String stage, int percent) {
			calls++;
			long now = System.nanoTime();
			if (firstCallNanos == 0) {
				firstCallNanos = now;
			}
			lastCallNanos = now;
			stages.add(stage);
			if (percent < lastPercent) {
				regressions++;
			}
			lastPercent = percent;
			maxPercent = Math.max(maxPercent, percent);
		}

		@Override
		public boolean isCancelled() {
			return cancelNow || lastPercent >= cancelAtPercent;
		}
	}

	static GtfsFeedStore freshStore(SelfTestEnv env, String name) {
		File dir = new File(env.workDir, name);
		SelfTestUtil.deleteTree(dir);
		dir.mkdirs();
		return new GtfsFeedStore(dir);
	}

	static File add(GtfsFeedStore store, File zip) throws IOException {
		try (InputStream in = new FileInputStream(zip)) {
			return store.addFeed(zip.getName(), in, zip.length(), null);
		}
	}

	/** A store with the given zips and a built index. */
	static GtfsFeedStore built(SelfTestEnv env, String name, List<File> zips) throws IOException {
		GtfsFeedStore store = freshStore(env, name);
		for (File z : zips) {
			add(store, z);
		}
		store.rebuild(null);
		return store;
	}

	private static File storeDir(GtfsFeedStore store) {
		return store.getIndexFile().getParentFile();
	}

	// ------------------------------------------------------------------ import-all

	static void importAll(SelfTestEnv env, ScenarioResult r) throws Exception {
		if (env.zips.isEmpty()) {
			r.skip("no gtfs_*.zip found");
			return;
		}
		GtfsFeedStore store = freshStore(env, "store-all");
		List<Map<String, Object>> copies = new ArrayList<>();
		long totalZip = 0;
		for (File z : env.zips) {
			long t0 = System.nanoTime();
			File added = add(store, z);
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("zip", z.getName());
			m.put("bytes", z.length());
			m.put("copyMs", (System.nanoTime() - t0) / 1_000_000);
			m.put("storedAs", added.getName());
			copies.add(m);
			totalZip += z.length();
		}
		r.put("feeds", copies);
		r.put("zipBytesTotal", totalZip);
		r.expect(store.listFeeds().size() == env.zips.size(), "feed count after copy " + store.listFeeds().size());

		Probe probe = new Probe();
		SelfTestUtil.HeapSampler sampler = SelfTestUtil.HeapSampler.start();
		long t0 = System.nanoTime();
		GtfsFeedImporter.Report rep = store.rebuild(probe);
		long importMs = (System.nanoTime() - t0) / 1_000_000;
		long peak = sampler.stop();
		r.expect(rep != null, "rebuild returned no report");
		if (rep == null) {
			return;
		}
		String sha1 = SelfTestUtil.sha256(store.getIndexFile());
		r.put("run1", report(rep, importMs, peak, probe, sha1));
		r.put("indexBytes", store.getIndexFile().length());
		r.put("indexSha256", sha1);
		r.expect(rep.stops > 0 && rep.trips > 0 && rep.connections > 0, "empty import: " + rep.stops + " stops");
		r.expect(probe.calls > 0, "no progress callbacks");
		r.expect(probe.maxPercent >= 99, "progress ended at " + probe.maxPercent);
		r.expect(SelfTestUtil.leftovers(storeDir(store)).isEmpty(), "leftover files " + SelfTestUtil.leftovers(storeDir(store)));

		GtfsIndex index = GtfsIndex.open(store.getIndexFile(), true);
		r.put("zone", String.valueOf(index.getZone()));
		r.put("validFromEpochDay", index.getValidFrom());
		r.put("validToEpochDay", index.getValidTo());
		r.put("validFrom", java.time.LocalDate.ofEpochDay(index.getValidFrom()).toString());
		r.put("validTo", java.time.LocalDate.ofEpochDay(index.getValidTo()).toString());
		r.expect(index.verifyChecksum(), "index checksum");
		r.expect(index.getStopCount() == rep.stops && index.getTripCount() == rep.trips,
				"index counts differ from the import report");

		// second run: the same zips again must give the same bytes
		store.deleteLeftovers();
		Probe probe2 = new Probe();
		sampler = SelfTestUtil.HeapSampler.start();
		t0 = System.nanoTime();
		GtfsFeedImporter.Report rep2 = store.rebuild(probe2);
		long ms2 = (System.nanoTime() - t0) / 1_000_000;
		long peak2 = sampler.stop();
		String sha2 = SelfTestUtil.sha256(store.getIndexFile());
		r.put("run2", report(rep2, ms2, peak2, probe2, sha2));
		r.expect(sha1.equals(sha2), "second import is not byte identical: " + sha1 + " vs " + sha2);
		r.expect(rep2 != null && rep2.stops == rep.stops && rep2.trips == rep.trips && rep2.connections == rep.connections,
				"second import counts differ");
		r.expect(store.listFeeds().size() == env.zips.size(), "feed count changed by the second run");

		// importing the same file name again must not overwrite or duplicate silently
		File again = add(store, env.zips.get(0));
		r.put("sameNameAgainStoredAs", again.getName());
		r.expect(!again.getName().equals(env.zips.get(0).getName()), "same name overwrote an existing feed");
		again.delete();
		r.expect(store.listFeeds().size() == env.zips.size(), "feed count wrong after removing the duplicate");
		env.setSharedIndex(store.getIndexFile());
	}

	private static Map<String, Object> report(GtfsFeedImporter.Report rep, long ms, long peakHeap, Probe p, String sha) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (rep == null) {
			m.put("report", null);
			return m;
		}
		m.put("importMs", ms);
		m.put("reportMillis", rep.millis);
		m.put("stopTimeRows", rep.stopTimeRows);
		m.put("stops", rep.stops);
		m.put("routes", rep.routes);
		m.put("trips", rep.trips);
		m.put("connections", rep.connections);
		m.put("footpaths", rep.footpaths);
		m.put("tripsSkipped", rep.tripsSkipped);
		m.put("rowsSkipped", rep.rowsSkipped);
		m.put("tripsInterpolated", rep.tripsInterpolated);
		m.put("tripsSorted", rep.tripsSorted);
		m.put("indexBytes", rep.fileSize);
		m.put("javaHeapPeakMb", SelfTestUtil.mb(peakHeap));
		m.put("progressCalls", p.calls);
		m.put("progressRegressions", p.regressions);
		m.put("progressStages", new ArrayList<>(p.stages));
		m.put("sha256", sha);
		m.put("warnings", rep.warnings.size() > 20 ? new ArrayList<>(rep.warnings.subList(0, 20)) : rep.warnings);
		return m;
	}

	// ------------------------------------------------------------------ failure scenarios

	/** The extra zip for "adding a feed that then fails": any zip not in the baseline, else a copy of one. */
	private static File extraZip(SelfTestEnv env, List<File> baseline) {
		for (File z : env.zips) {
			if (!baseline.contains(z)) {
				return z;
			}
		}
		return baseline.get(0);
	}

	private static boolean unchanged(ScenarioResult r, String what, GtfsFeedStore store, String shaBefore, long lenBefore)
			throws IOException {
		boolean ok = store.hasIndex() && SelfTestUtil.sha256(store.getIndexFile()).equals(shaBefore)
				&& store.getIndexFile().length() == lenBefore;
		r.expect(ok, what + ": the previous index changed or vanished");
		List<String> left = SelfTestUtil.leftovers(storeDir(store));
		r.expect(left.isEmpty(), what + ": leftover files " + left);
		return ok;
	}

	static void importCancel(SelfTestEnv env, ScenarioResult r) throws Exception {
		if (env.zips.isEmpty()) {
			r.skip("no zips");
			return;
		}
		List<File> base = SelfTestUtil.smallZips(env.zips, 1);
		GtfsFeedStore store = built(env, "store-cancel", base);
		String sha = SelfTestUtil.sha256(store.getIndexFile());
		long len = store.getIndexFile().length();
		r.put("baselineSha256", sha);

		// 1: cancel while copying
		File extra = extraZip(env, base);
		boolean cancelledCopy = false;
		try (InputStream in = new FileInputStream(extra)) {
			store.addFeed("extra.zip", in, extra.length(), () -> true);
		} catch (GtfsFeedStore.CancelledException e) {
			cancelledCopy = true;
		}
		r.put("copyCancelled", cancelledCopy);
		r.expect(cancelledCopy, "cancelling the copy did not raise CancelledException");
		r.expect(store.listFeeds().size() == 1, "cancelled copy left a feed: " + store.listFeeds().size());
		unchanged(r, "cancel during copy", store, sha, len);

		// 2: cancel while building (at 25 percent), then undo the added zip as GtfsFeedUpdate does
		File added = add(store, extra);
		Probe probe = new Probe();
		probe.cancelAtPercent = 25;
		boolean cancelled = false;
		String cls = "none";
		long t0 = System.nanoTime();
		try {
			store.rebuild(probe);
		} catch (IOException e) {
			cls = e.getClass().getSimpleName();
			cancelled = e instanceof GtfsFeedImporter.ImportException && ((GtfsFeedImporter.ImportException) e).isCancelled();
		}
		r.put("buildCancelled", cancelled);
		r.put("cancelException", cls);
		r.put("cancelLatencyMs", (System.nanoTime() - t0) / 1_000_000);
		r.put("cancelStoppedAtPercent", probe.lastPercent);
		r.expect(cancelled, "cancelling the build was not reported as a cancellation (" + cls + ")");
		added.delete();
		unchanged(r, "cancel during build", store, sha, len);
		r.expect(store.listFeeds().size() == 1, "feed list not back to 1");
	}

	static void importBadZip(SelfTestEnv env, ScenarioResult r) throws Exception {
		if (env.zips.isEmpty()) {
			r.skip("no zips");
			return;
		}
		List<File> base = SelfTestUtil.smallZips(env.zips, 1);
		GtfsFeedStore store = built(env, "store-bad", base);
		String sha = SelfTestUtil.sha256(store.getIndexFile());
		long len = store.getIndexFile().length();
		File junkDir = new File(env.workDir, "junk");
		junkDir.mkdirs();

		// a: random bytes, no zip signature: refused by the copy
		File notZip = new File(junkDir, "notzip.zip");
		try (java.io.FileOutputStream o = new java.io.FileOutputStream(notZip)) {
			o.write("this is not a zip file at all, just text".getBytes("UTF-8"));
		}
		String err = null;
		try (InputStream in = new FileInputStream(notZip)) {
			store.addFeed("notzip.zip", in, notZip.length(), null);
		} catch (IOException e) {
			err = e.getMessage();
		}
		r.put("notZipError", err);
		r.expect(err != null && err.contains("not a zip"), "plain text was not refused as 'not a zip': " + err);
		r.expect(store.listFeeds().size() == 1, "refused file was still stored");
		unchanged(r, "not a zip", store, sha, len);

		// b: zip signature but truncated (first 64 KiB of a real feed): copy passes, the build must fail
		File trunc = SelfTestUtil.firstBytes(base.get(0), new File(junkDir, "truncated.zip"), 64 * 1024);
		r.put("truncated", buildMustFail(r, store, trunc, "truncated zip", sha, len));

		// c: a valid zip that is not GTFS (no stops.txt)
		File other = SelfTestUtil.zip(new File(junkDir, "other.zip"), "readme.txt", "hello");
		r.put("notGtfs", buildMustFail(r, store, other, "zip without GTFS files", sha, len));
	}

	/** Adds the file, expects the rebuild to fail, undoes the add, and checks the old index. */
	private static Map<String, Object> buildMustFail(ScenarioResult r, GtfsFeedStore store, File zip, String what,
	                                                 String sha, long len) throws IOException {
		Map<String, Object> m = new LinkedHashMap<>();
		File added;
		try {
			added = add(store, zip);
		} catch (IOException e) {
			m.put("refusedAtCopy", e.getMessage());
			unchanged(r, what, store, sha, len);
			return m;
		}
		String cls = "none", msg = null;
		try {
			store.rebuild(null);
		} catch (IOException | RuntimeException e) {
			cls = e.getClass().getSimpleName();
			msg = e.getMessage();
		}
		m.put("exception", cls);
		m.put("message", msg);
		r.expect(!"none".equals(cls), what + ": the build did not fail");
		added.delete();
		unchanged(r, what, store, sha, len);
		return m;
	}

	static void importMissingStopTimes(SelfTestEnv env, ScenarioResult r) throws Exception {
		if (env.zips.isEmpty()) {
			r.skip("no zips");
			return;
		}
		List<File> base = SelfTestUtil.smallZips(env.zips, 1);
		GtfsFeedStore store = built(env, "store-nost", base);
		String sha = SelfTestUtil.sha256(store.getIndexFile());
		long len = store.getIndexFile().length();
		File junkDir = new File(env.workDir, "junk-nost");
		junkDir.mkdirs();
		String stops = "stop_id,stop_name,stop_lat,stop_lon\nA,Alpha,40.70,-74.00\nB,Beta,40.71,-74.00\n";
		String routes = "route_id,route_short_name,route_type\nR1,1,1\n";
		String trips = "route_id,service_id,trip_id\nR1,S1,T1\n";
		String cal = "service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date\n"
				+ "S1,1,1,1,1,1,1,1,20260101,20271231\n";
		File noSt = SelfTestUtil.zip(new File(junkDir, "no-stop-times.zip"), "stops.txt", stops, "routes.txt", routes,
				"trips.txt", trips, "calendar.txt", cal);
		r.put("noStopTimes", buildMustFail(r, store, noSt, "feed without stop_times.txt", sha, len));
		File noStops = SelfTestUtil.zip(new File(junkDir, "no-stops.zip"), "routes.txt", routes, "trips.txt", trips,
				"stop_times.txt", "trip_id,arrival_time,departure_time,stop_id,stop_sequence\nT1,08:00:00,08:00:00,A,1\n",
				"calendar.txt", cal);
		r.put("noStops", buildMustFail(r, store, noStops, "feed without stops.txt", sha, len));
	}

	static void lowSpace(SelfTestEnv env, ScenarioResult r) throws Exception {
		if (env.zips.isEmpty()) {
			r.skip("no zips");
			return;
		}
		List<File> base = SelfTestUtil.smallZips(env.zips, 1);
		GtfsFeedStore store = built(env, "store-space", base);
		String sha = SelfTestUtil.sha256(store.getIndexFile());
		long len = store.getIndexFile().length();
		r.put("usableSpaceMb", SelfTestUtil.mb(env.workDir.getUsableSpace()));

		// the copy asks for more space than any volume has
		String err = null;
		File extra = extraZip(env, base);
		try (InputStream in = new FileInputStream(extra)) {
			store.addFeed("huge.zip", in, 1L << 50, null);
		} catch (IOException e) {
			err = e.getMessage();
		}
		r.put("copyError", err);
		r.expect(err != null && err.startsWith("Not enough free space"), "copy did not refuse for lack of space: " + err);
		r.expect(store.listFeeds().size() == 1, "feed stored despite the space check");
		unchanged(r, "low space at copy", store, sha, len);

		// the importer's own free-space seam says no
		String err2 = null;
		try {
			new GtfsFeedImporter(null).setFreeSpaceCheck((dir, bytes) -> {
				throw new IOException("Not enough free space for the GTFS index: need " + (bytes >> 20) + " MB, have 1 MB");
			}).importFeeds(zipsOf(store), store.getIndexFile());
		} catch (IOException e) {
			err2 = e.getMessage();
		}
		r.put("importError", err2);
		r.expect(err2 != null && err2.startsWith("Not enough free space"), "import did not stop for lack of space: " + err2);
		unchanged(r, "low space at import", store, sha, len);
	}

	private static List<File> zipsOf(GtfsFeedStore store) {
		List<File> res = new ArrayList<>();
		for (GtfsFeedStore.Feed f : store.listFeeds()) {
			res.add(new File(new File(storeDir(store), "feeds"), f.name));
		}
		return res;
	}

	// ------------------------------------------------------------------ removal

	/** The plugin's removal: stage, rebuild, commit or undo. Returns the report (null when no feed is left). */
	private static GtfsFeedImporter.Report remove(GtfsFeedStore store, String name) throws IOException {
		if (!store.stageRemoval(name)) {
			throw new IOException("cannot stage " + name);
		}
		try {
			GtfsFeedImporter.Report rep = store.rebuild(null);
			store.finishRemoval(name, true);
			return rep;
		} catch (IOException | RuntimeException e) {
			store.finishRemoval(name, false);
			throw e;
		}
	}

	static void removeFeed(SelfTestEnv env, ScenarioResult r) throws Exception {
		List<File> two = SelfTestUtil.smallZips(env.zips, 2);
		if (two.size() < 2) {
			r.skip("needs two zips");
			return;
		}
		GtfsFeedStore store = built(env, "store-remove", two);
		ProviderHarness h = new ProviderHarness(store);
		h.refresh();
		r.expect(h.registered, "provider not registered with an index and the plugin enabled");
		String shaFull = SelfTestUtil.sha256(store.getIndexFile());
		GtfsIndex full = GtfsIndex.open(store.getIndexFile());
		int stopsFull = full.getStopCount();
		r.put("before", stopCounts(full));

		File victim = two.get(0);
		long t0 = System.nanoTime();
		GtfsFeedImporter.Report rep = remove(store, victim.getName());
		r.put("removeMs", (System.nanoTime() - t0) / 1_000_000);
		h.source.invalidate();
		h.refresh();
		r.expect(rep != null, "removing one of two feeds left no index");
		r.expect(store.listFeeds().size() == 1, "feed list after removal " + store.listFeeds().size());
		r.expect(h.registered, "provider unregistered although an index remains");
		r.expect(SelfTestUtil.leftovers(storeDir(store)).isEmpty(), "leftovers " + SelfTestUtil.leftovers(storeDir(store)));
		GtfsIndex one = GtfsIndex.open(store.getIndexFile(), true);
		r.put("after", stopCounts(one));
		r.expect(one.getFeedCount() == 1, "feed count after removal " + one.getFeedCount());
		r.expect(one.getStopCount() < stopsFull, "stop count did not shrink");
		r.expect(!SelfTestUtil.sha256(store.getIndexFile()).equals(shaFull), "index unchanged by a removal");

		// restore by importing the zip again: same bytes as before
		add(store, victim);
		store.rebuild(null);
		h.source.invalidate();
		h.refresh();
		String shaBack = SelfTestUtil.sha256(store.getIndexFile());
		r.put("restoredSha256", shaBack);
		r.put("originalSha256", shaFull);
		r.expect(shaFull.equals(shaBack), "re-import did not restore the original index bytes");
		r.expect(h.registered, "provider not registered after restore");
	}

	private static Map<String, Object> stopCounts(GtfsIndex i) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("feeds", i.getFeedCount());
		m.put("stops", i.getStopCount());
		m.put("trips", i.getTripCount());
		m.put("bytes", i.getFileSize());
		return m;
	}

	static void removeLastFeed(SelfTestEnv env, ScenarioResult r) throws Exception {
		List<File> one = SelfTestUtil.smallZips(env.zips, 1);
		if (one.isEmpty()) {
			r.skip("no zips");
			return;
		}
		GtfsFeedStore store = built(env, "store-last", one);
		ProviderHarness h = new ProviderHarness(store);
		h.refresh();
		String sha = SelfTestUtil.sha256(store.getIndexFile());
		r.expect(h.registered, "provider not registered with an index");
		r.expect(!h.providers.isEmpty(), "provider list empty with an index");

		GtfsFeedImporter.Report rep = remove(store, one.get(0).getName());
		h.source.invalidate();
		h.refresh();
		r.put("reportAfterRemoval", rep == null ? "null (index removed)" : "report");
		r.expect(rep == null, "removing the last feed returned a report");
		r.expect(!store.hasIndex(), "index file still exists after removing the last feed");
		r.expect(store.listFeeds().isEmpty(), "feeds remain");
		r.expect(!h.registered, "provider still registered without an index");
		r.expect(h.providers.isEmpty(), "provider list not empty");
		r.expect(SelfTestUtil.leftovers(storeDir(store)).isEmpty(), "leftovers " + SelfTestUtil.leftovers(storeDir(store)));
		// a request now falls through to the stock planner
		Object answer = h.providers.calculate(new net.osmand.data.LatLon(40.635, -73.9625), new net.osmand.data.LatLon(40.7553,
				-73.9869), null, new java.util.HashMap<String, String>(), null, null);
		r.expect(answer == null, "request answered although no provider is registered");

		add(store, one.get(0));
		store.rebuild(null);
		h.source.invalidate();
		h.refresh();
		r.expect(store.hasIndex() && h.registered, "not restored by re-import");
		String shaBack = SelfTestUtil.sha256(store.getIndexFile());
		r.put("restoredSha256", shaBack);
		r.expect(sha.equals(shaBack), "re-import did not restore the original index bytes");
	}
}
