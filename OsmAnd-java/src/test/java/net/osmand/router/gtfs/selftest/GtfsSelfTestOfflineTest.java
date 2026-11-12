package net.osmand.router.gtfs.selftest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * The phone self-test scenarios run on the JVM. The clock and runner tests need nothing; the feed tests need the
 * MTA zips: pass them like the smoke test does,
 * <pre>JAVA_TOOL_OPTIONS="-Dgtfs.mta.dir=/fast/agent/cache/mta-gtfs -Dgtfs.selftest.out=/some/dir
 * -Djava.io.tmpdir=/roomy/dir -Dgtfs.selftest.now=1794448800" ./gradlew :OsmAnd-java:cleanTest :OsmAnd-java:test --tests '*GtfsSelfTestOfflineTest'</pre>
 * With gtfs.selftest.out the report.json and report.txt of the feed run are written there.
 */
public class GtfsSelfTestOfflineTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private SelfTestEnv host(List<File> zips) throws Exception {
		SelfTestEnv h = new SelfTestEnv(tmp.newFolder("work"), zips);
		h.sink = line -> System.out.println("[selftest] " + line);
		String now = System.getProperty("gtfs.selftest.now"); // epoch seconds: makes the "now" rows repeatable
		if (now != null && !now.isEmpty()) {
			long fixed = Long.parseLong(now);
			h.nowSec = () -> fixed;
		}
		h.info.put("host", "jvm " + System.getProperty("java.version"));
		return h;
	}

	private static List<File> mtaZips() {
		String dir = System.getProperty("gtfs.mta.dir");
		Assume.assumeTrue("gtfs.mta.dir is not set", dir != null && !dir.isEmpty());
		List<File> zips = new ArrayList<>();
		for (String name : new String[] {"gtfs_b", "gtfs_busco", "gtfs_bx", "gtfs_m", "gtfs_q", "gtfs_si", "gtfs_subway"}) {
			File f = new File(dir, name + ".zip");
			Assume.assumeTrue(f + " is missing", f.isFile());
			zips.add(f);
		}
		return zips;
	}

	private static void assertAllPassed(SelfTestReport report) {
		StringBuilder sb = new StringBuilder();
		for (ScenarioResult r : report.results) {
			if (r.status() == ScenarioResult.Status.FAIL) {
				sb.append(r.name).append(": ").append(r.errors).append('\n');
			}
		}
		if (sb.length() > 0) {
			fail(sb.toString());
		}
	}

	@Test
	public void clockFormatNeedsNoFeeds() throws Exception {
		SelfTestReport report = new SelfTestRunner().run(host(new ArrayList<File>()), "clock-format", null);
		assertEquals(1, report.results.size());
		assertAllPassed(report);
		assertTrue(report.toJson().contains("\"clock-format\""));
		assertTrue(report.toText().contains("PASS"));
	}

	@Test
	public void unknownScenarioFails() throws Exception {
		SelfTestReport report = new SelfTestRunner().run(host(new ArrayList<File>()), "nope", null);
		assertTrue(!report.allPassed());
	}

	@Test
	public void feedScenariosWithoutZipsSkip() throws Exception {
		SelfTestReport report = new SelfTestRunner().run(host(new ArrayList<File>()),
				"import-all,import-cancel,plan-matrix,guidance-sim,perf,remove-feed", null);
		assertEquals(6, report.results.size());
		for (ScenarioResult r : report.results) {
			assertEquals(r.name, ScenarioResult.Status.SKIP, r.status());
		}
	}

	@Test
	public void allScenariosOnTheMtaFeeds() throws Exception {
		List<File> zips = mtaZips();
		SelfTestEnv h = host(zips);
		SelfTestReport report = new SelfTestRunner().run(h, "all", null);
		String out = System.getProperty("gtfs.selftest.out");
		if (out != null && !out.isEmpty()) {
			File dir = new File(out);
			dir.mkdirs();
			Files.write(new File(dir, "report.json").toPath(), report.toJson().getBytes(StandardCharsets.UTF_8));
			Files.write(new File(dir, "report.txt").toPath(), report.toText().getBytes(StandardCharsets.UTF_8));
		}
		System.out.println(report.toText().split("\n\n==")[0]);
		assertEquals(Arrays.asList("import-all", "import-cancel", "import-bad-zip", "import-missing-stop-times",
				"low-space-simulation", "remove-feed", "remove-last-feed", "plan-matrix", "clock-format", "guidance-sim", "perf"),
				new SelfTestRunner().names());
		for (ScenarioResult r : report.results) {
			assertTrue(r.name + " skipped", r.status() != ScenarioResult.Status.SKIP);
		}
		assertAllPassed(report);
	}
}
