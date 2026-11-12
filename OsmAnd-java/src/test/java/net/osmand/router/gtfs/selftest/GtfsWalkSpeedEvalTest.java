package net.osmand.router.gtfs.selftest;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.osmand.router.gtfs.GtfsIndex;
import net.osmand.router.gtfs.GtfsIndexSource;
import net.osmand.router.gtfs.GtfsItinerary;
import net.osmand.router.gtfs.GtfsLeg;
import net.osmand.router.gtfs.GtfsPlanOptions;
import net.osmand.router.gtfs.GtfsRoutePlanner;

/**
 * Not a regression test: compares the plan-matrix rows planned at two walking speeds and prints the arrival
 * deltas (needs -Dgtfs.mta.dir like the other feed tests; -Dgtfs.eval.out=DIR also writes walk-speed.txt,
 * -Dgtfs.eval.speeds=1.25,1.35 and -Dgtfs.selftest.now=EPOCH change the speeds and the clock). It only checks
 * that walking faster never makes the best arrival later.
 */
public class GtfsWalkSpeedEvalTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static String lines(GtfsIndex ix, GtfsItinerary it) {
		StringBuilder sb = new StringBuilder();
		for (GtfsLeg l : it.legs) {
			if (l.type == GtfsLeg.Type.RIDE) {
				sb.append(sb.length() > 0 ? "+" : "").append(ix.getRouteShortName(l.routeIdx));
			}
		}
		return sb.toString();
	}

	@Test
	public void walkSpeedAgainstThePlanMatrix() throws Exception {
		String dir = System.getProperty("gtfs.mta.dir");
		Assume.assumeTrue("gtfs.mta.dir is not set", dir != null && !dir.isEmpty());
		List<File> zips = new ArrayList<>();
		for (String name : new String[] {"gtfs_b", "gtfs_busco", "gtfs_bx", "gtfs_m", "gtfs_q", "gtfs_si", "gtfs_subway"}) {
			File f = new File(dir, name + ".zip");
			Assume.assumeTrue(f + " is missing", f.isFile());
			zips.add(f);
		}
		String[] sp = System.getProperty("gtfs.eval.speeds", "1.25,1.35").split(",");
		double lo = Double.parseDouble(sp[0]), hi = Double.parseDouble(sp[1]);
		SelfTestEnv env = new SelfTestEnv(tmp.newFolder("work"), zips);
		String now = System.getProperty("gtfs.selftest.now");
		if (now != null && !now.isEmpty()) {
			long fixed = Long.parseLong(now);
			env.nowSec = () -> fixed;
		}
		GtfsIndexSource source = new GtfsIndexSource(env.ensureIndex());
		GtfsIndexSource.Loaded loaded = source.get();
		assertTrue("index", loaded != null);
		GtfsIndex ix = loaded.index;
		GtfsRoutePlanner planner = loaded.planner;
		ZoneId zone = ix.getZone();
		StringBuilder out = new StringBuilder();
		out.append(String.format("%-52s %-9s %-9s %6s  %s%n", "row", "arr " + lo, "arr " + hi, "delta", "lines (same unless shown)"));
		int rows = 0, faster = 0, otherLines = 0;
		long sum = 0;
		for (SelfTestPlaces.Od od : SelfTestPlaces.all()) {
			for (PlanMatrixScenario.When w : PlanMatrixScenario.times(env, ix, new ScenarioResult("eval"))) {
				if (!od.expectRoute || !w.inService || !w.mandatory) {
					continue;
				}
				GtfsPlanOptions a = new GtfsPlanOptions(), b = new GtfsPlanOptions();
				a.walkSpeedMps = lo;
				b.walkSpeedMps = hi;
				List<GtfsItinerary> ra = planner.plan(od.from, od.to, w.epochSec, a, null);
				List<GtfsItinerary> rb = planner.plan(od.from, od.to, w.epochSec, b, null);
				if (ra.isEmpty() || rb.isEmpty()) {
					continue;
				}
				GtfsItinerary x = ra.get(0), y = rb.get(0);
				assertTrue(od.name + " @ " + w.label + ": faster walking arrives later", y.arriveSec <= x.arriveSec);
				rows++;
				long delta = x.arriveSec - y.arriveSec;
				sum += delta;
				faster += delta > 0 ? 1 : 0;
				String la = lines(ix, x), lb = lines(ix, y);
				otherLines += la.equals(lb) ? 0 : 1;
				out.append(String.format("%-52s %-9s %-9s %5ds  %s%s  walk %d->%d m%n", (od.name + " @ " + w.label).substring(0,
						Math.min(52, (od.name + " @ " + w.label).length())),
						Instant.ofEpochSecond(x.arriveSec).atZone(zone).toLocalTime(),
						Instant.ofEpochSecond(y.arriveSec).atZone(zone).toLocalTime(), delta, la,
						la.equals(lb) ? "" : "  ->  " + lb, x.walkMetersAdjusted, y.walkMetersAdjusted));
			}
		}
		out.append(String.format("rows %d, arrive earlier at %.2f m/s: %d, best lines differ: %d, total delta %d s (mean %.0f s)%n",
				rows, hi, faster, otherLines, sum, rows == 0 ? 0.0 : (double) sum / rows));
		System.out.println(out);
		String o = System.getProperty("gtfs.eval.out");
		if (o != null && !o.isEmpty()) {
			new File(o).mkdirs();
			Files.write(new File(o, "walk-speed.txt").toPath(), out.toString().getBytes(StandardCharsets.UTF_8));
		}
	}
}
