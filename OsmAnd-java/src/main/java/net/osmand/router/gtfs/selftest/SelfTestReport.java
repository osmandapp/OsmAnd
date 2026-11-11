package net.osmand.router.gtfs.selftest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** The results of a run, as JSON for scripts and as plain text for people. */
public final class SelfTestReport {

	public final Map<String, Object> header = new LinkedHashMap<>();
	public final List<ScenarioResult> results = new ArrayList<>();
	public long startedMillis, endedMillis;

	public boolean allPassed() {
		for (ScenarioResult r : results) {
			if (r.status() == ScenarioResult.Status.FAIL) {
				return false;
			}
		}
		return !results.isEmpty();
	}

	public int count(ScenarioResult.Status s) {
		int n = 0;
		for (ScenarioResult r : results) {
			if (r.status() == s) {
				n++;
			}
		}
		return n;
	}

	public String toJson() {
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("schema", 1);
		root.put("header", header);
		root.put("startedMillis", startedMillis);
		root.put("endedMillis", endedMillis);
		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("pass", count(ScenarioResult.Status.PASS));
		summary.put("fail", count(ScenarioResult.Status.FAIL));
		summary.put("skip", count(ScenarioResult.Status.SKIP));
		summary.put("allPassed", allPassed());
		root.put("summary", summary);
		List<Object> list = new ArrayList<>();
		for (ScenarioResult r : results) {
			list.add(r.toMap());
		}
		root.put("scenarios", list);
		return gson.toJson(root);
	}

	public String toText() {
		StringBuilder sb = new StringBuilder();
		sb.append("GTFS self-test\n");
		for (Map.Entry<String, Object> e : header.entrySet()) {
			sb.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append('\n');
		}
		sb.append("\nSummary\n");
		for (ScenarioResult r : results) {
			sb.append(String.format(Locale.ROOT, "  %-4s %-26s %8d ms%s%n", r.status(), r.name, r.millis,
					r.errors.isEmpty() ? "" : "   " + r.errors.size() + " error(s)"));
		}
		sb.append(String.format(Locale.ROOT, "  pass=%d fail=%d skip=%d%n", count(ScenarioResult.Status.PASS),
				count(ScenarioResult.Status.FAIL), count(ScenarioResult.Status.SKIP)));
		for (ScenarioResult r : results) {
			sb.append("\n== ").append(r.name).append("  ").append(r.status()).append("  ").append(r.millis).append(" ms\n");
			for (String n : r.notes) {
				sb.append("  note: ").append(n).append('\n');
			}
			for (String e : r.errors) {
				sb.append("  ERROR: ").append(e).append('\n');
			}
			for (Map.Entry<String, Object> e : r.values.entrySet()) {
				render(sb, 1, e.getKey(), e.getValue());
			}
		}
		return sb.toString();
	}

	private static void indent(StringBuilder sb, int level) {
		for (int i = 0; i < level; i++) {
			sb.append("  ");
		}
	}

	private static void render(StringBuilder sb, int level, String key, Object v) {
		if (v instanceof Map) {
			indent(sb, level);
			sb.append(key).append(":\n");
			for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
				render(sb, level + 1, String.valueOf(e.getKey()), e.getValue());
			}
		} else if (v instanceof List) {
			List<?> l = (List<?>) v;
			boolean scalars = true;
			for (Object o : l) {
				if (o instanceof Map || o instanceof List) {
					scalars = false;
				}
			}
			indent(sb, level);
			if (scalars && l.size() <= 3 && l.toString().length() < 100) {
				sb.append(key).append(": ").append(l).append('\n');
				return;
			}
			sb.append(key).append(" [").append(l.size()).append("]:\n");
			for (Object o : l) {
				if (o instanceof Map) {
					render(sb, level + 1, "-", o);
				} else {
					indent(sb, level + 1);
					sb.append(o).append('\n');
				}
			}
		} else {
			indent(sb, level);
			sb.append(key).append(": ").append(v).append('\n');
		}
	}
}
