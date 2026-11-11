package net.osmand.router.gtfs.selftest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Outcome of one self-test scenario: status, measured values, notes and error text. */
public final class ScenarioResult {

	public enum Status {
		PASS, FAIL, SKIP
	}

	public final String name;
	public final Map<String, Object> values = new LinkedHashMap<>();
	public final List<String> notes = new ArrayList<>();
	public final List<String> errors = new ArrayList<>();
	private boolean skipped;
	public long millis;

	public ScenarioResult(String name) {
		this.name = name;
	}

	public ScenarioResult put(String key, Object value) {
		values.put(key, value);
		return this;
	}

	public ScenarioResult note(String text) {
		notes.add(text);
		return this;
	}

	/** Records a failed expectation. */
	public ScenarioResult fail(String text) {
		errors.add(text);
		return this;
	}

	/** Records an expectation; the message is the error text when it does not hold. */
	public boolean expect(boolean condition, String message) {
		if (!condition) {
			errors.add(message);
		}
		return condition;
	}

	public ScenarioResult skip(String reason) {
		skipped = true;
		notes.add("SKIP: " + reason);
		return this;
	}

	public Status status() {
		if (!errors.isEmpty()) {
			return Status.FAIL;
		}
		return skipped ? Status.SKIP : Status.PASS;
	}

	Map<String, Object> toMap() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("name", name);
		m.put("status", status().name());
		m.put("millis", millis);
		m.put("values", values);
		m.put("notes", notes);
		m.put("errors", errors);
		return m;
	}
}
