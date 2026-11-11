package net.osmand.router.gtfs.selftest;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs named scenarios one after the other and collects a {@link SelfTestReport}. The Android-free
 * scenarios are registered here; the app adds its own (plugin-state) with {@link #registerFirst}.
 */
public final class SelfTestRunner {

	private final Map<String, SelfTestScenario> scenarios = new LinkedHashMap<>();

	public SelfTestRunner() {
		register("import-all", ImportScenarios::importAll);
		register("import-cancel", ImportScenarios::importCancel);
		register("import-bad-zip", ImportScenarios::importBadZip);
		register("import-missing-stop-times", ImportScenarios::importMissingStopTimes);
		register("low-space-simulation", ImportScenarios::lowSpace);
		register("remove-feed", ImportScenarios::removeFeed);
		register("remove-last-feed", ImportScenarios::removeLastFeed);
		register("plan-matrix", PlanMatrixScenario::run);
		register("clock-format", (host, r) -> ClockFormatScenario.run(host, r));
		register("guidance-sim", GuidanceSimScenario::run);
		register("perf", PerfScenario::run);
	}

	public void register(String name, SelfTestScenario s) {
		scenarios.put(name, s);
	}

	/** Puts a scenario in front of the others (it runs first). */
	public void registerFirst(String name, SelfTestScenario s) {
		Map<String, SelfTestScenario> old = new LinkedHashMap<>(scenarios);
		scenarios.clear();
		scenarios.put(name, s);
		scenarios.putAll(old);
	}

	public List<String> names() {
		return new ArrayList<>(scenarios.keySet());
	}

	/** "all" or empty gives every scenario; otherwise a comma list, run in registry order. */
	public List<String> select(String spec) {
		if (spec == null || spec.trim().isEmpty() || spec.trim().equalsIgnoreCase("all")) {
			return names();
		}
		List<String> wanted = new ArrayList<>();
		for (String s : spec.split(",")) {
			if (!s.trim().isEmpty()) {
				wanted.add(s.trim());
			}
		}
		List<String> res = new ArrayList<>();
		for (String n : scenarios.keySet()) {
			if (wanted.contains(n)) {
				res.add(n);
			}
		}
		return res;
	}

	public List<String> unknown(String spec) {
		List<String> bad = new ArrayList<>();
		if (spec != null && !spec.trim().equalsIgnoreCase("all")) {
			for (String s : spec.split(",")) {
				String n = s.trim();
				if (!n.isEmpty() && !scenarios.containsKey(n)) {
					bad.add(n);
				}
			}
		}
		return bad;
	}

	/** Called after each scenario with the partial report (to write it out as the run goes). */
	public interface Progress {
		void scenarioDone(SelfTestReport partial, ScenarioResult last);
	}

	public SelfTestReport run(SelfTestEnv host, String spec, Progress progress) {
		SelfTestReport report = new SelfTestReport();
		report.startedMillis = System.currentTimeMillis();
		report.header.putAll(host.info);
		report.header.put("requested", spec == null ? "all" : spec);
		report.header.put("zips", zipNames(host));
		report.header.put("options", host.options);
		List<String> bad = unknown(spec);
		if (!bad.isEmpty()) {
			ScenarioResult r = new ScenarioResult("unknown-scenarios");
			r.fail("unknown scenario name(s) " + bad + "; known: " + names());
			report.results.add(r);
		}
		for (String name : select(spec)) {
			ScenarioResult r = new ScenarioResult(name);
			host.log("scenario " + name + " start");
			long t0 = System.nanoTime();
			try {
				scenarios.get(name).run(host, r);
			} catch (Throwable e) { // a scenario may hit OutOfMemoryError; the run goes on
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String[] lines = sw.toString().split("\n");
				r.fail("uncaught " + e + "\n" + String.join("\n", Arrays.copyOf(lines, Math.min(lines.length, 12))));
			}
			r.millis = (System.nanoTime() - t0) / 1_000_000;
			host.log("scenario " + name + " " + r.status() + " in " + r.millis + " ms" + (r.errors.isEmpty() ? ""
					: ": " + r.errors.get(0)));
			report.results.add(r);
			report.endedMillis = System.currentTimeMillis();
			if (progress != null) {
				progress.scenarioDone(report, r);
			}
		}
		report.endedMillis = System.currentTimeMillis();
		return report;
	}

	private static List<String> zipNames(SelfTestEnv host) {
		List<String> l = new ArrayList<>();
		for (File f : host.zips) {
			l.add(f.getName() + " (" + f.length() + " B)");
		}
		return l;
	}
}
