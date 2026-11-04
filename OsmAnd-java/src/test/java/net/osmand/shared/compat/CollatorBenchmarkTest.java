package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.binary.RouteDataObject;
import net.osmand.shared.api.KStringMatcherMode;
import net.osmand.shared.util.KCollatorStringMatcher;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Timing comparison of {@link KCollatorStringMatcher} against {@link CollatorStringMatcher}.
 *
 * <b>Disabled on purpose.</b> The reason the copy builds a collation key instead of calling a
 * collator per character position is speed. This is the tool for checking that claim on the jvm
 * against the real java matcher; the cross platform half, which is what gives the Kotlin/Native
 * numbers, is in
 * OsmAnd-shared/src/commonTest/kotlin/net/osmand/shared/util/KCollatorBenchmarkTest.kt.
 * Neither is a regression gate, and this one asserts only that both matchers found the same names,
 * so a slow machine can never turn the build red. What the copy has to answer is covered by
 * {@link CollatorCompatTest}, which does run.
 *
 * To measure, remove the {@code @Ignore} below, run it, and put the annotation back:
 * <pre>
 * ./gradlew :OsmAnd-java:test --tests "*CollatorBenchmarkTest" -i
 * </pre>
 *
 * Over 304 road names of the test maps and 14 queries, microseconds per {@code matches} call:
 * <pre>
 * CHECK_CONTAINS            java  28.39 us   copy  0.27 us   104x
 * CHECK_STARTS_FROM_SPACE   java   0.79 us   copy  0.28 us   2.8x
 * CHECK_ONLY_STARTS_WITH    java   0.38 us   copy  0.21 us   1.9x
 * </pre>
 * The contains mode is where java pays for its double loop over positions and lengths. The same
 * corpus on Kotlin/Native costs 0.77, 0.76 and 0.77 us, see the cross platform benchmark.
 */
@Ignore("benchmark, run manually")
public class CollatorBenchmarkTest {

	/** The same corpus as KCollatorBenchmarkTest in OsmAnd-shared, so the numbers can be compared. */
	private static final int NAMES = 300;
	private static final int WARMUP_ROUNDS = 2;
	private static final int MEASURED_ROUNDS = 3;

	private static final String[] QUERIES = {
			"a", "st", "str", "stras", "strasse", "ring", "haupt", "bahn", "weg 12",
			"ул", "улица", "ленина", "проспект", "42",
	};


	@Test
	public void matchingNames() throws IOException {
		List<String> names = names();
		System.out.println("names: " + names.size());
		for (StringMatcherMode mode : new StringMatcherMode[] {
				StringMatcherMode.CHECK_CONTAINS,
				StringMatcherMode.CHECK_STARTS_FROM_SPACE,
				StringMatcherMode.CHECK_ONLY_STARTS_WITH}) {
			long java = 0;
			long copy = 0;
			for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
				boolean measured = round >= WARMUP_ROUNDS;

				long started = System.nanoTime();
				int javaHits = countJava(mode, names);
				long javaRound = System.nanoTime() - started;

				started = System.nanoTime();
				int copyHits = countCopy(mode, names);
				long copyRound = System.nanoTime() - started;

				assertEquals(mode + " hits", javaHits, copyHits);
				if (measured) {
					java += javaRound;
					copy += copyRound;
				}
			}
			double javaPerCall = (double) java / MEASURED_ROUNDS / names.size() / QUERIES.length / 1000;
			double copyPerCall = (double) copy / MEASURED_ROUNDS / names.size() / QUERIES.length / 1000;
			System.out.printf("%-35s java %8.2f us   copy %8.2f us   %.1fx%n",
					mode, javaPerCall, copyPerCall, javaPerCall / copyPerCall);
		}
	}

	private int countJava(StringMatcherMode mode, List<String> names) {
		int hits = 0;
		for (String query : QUERIES) {
			CollatorStringMatcher matcher = new CollatorStringMatcher(query, mode);
			for (String name : names) {
				if (matcher.matches(name)) {
					hits++;
				}
			}
		}
		return hits;
	}

	private int countCopy(StringMatcherMode mode, List<String> names) {
		int hits = 0;
		for (String query : QUERIES) {
			KCollatorStringMatcher matcher =
					new KCollatorStringMatcher(query, KStringMatcherMode.valueOf(mode.name()));
			for (String name : names) {
				if (matcher.matches(name)) {
					hits++;
				}
			}
		}
		return hits;
	}

	private static List<String> names() throws IOException {
		Set<String> collected = new LinkedHashSet<>();
		for (RouteDataObject road : TestObf.roads(40000)) {
			if (road == null || road.names == null) {
				continue;
			}
			for (String name : road.names.valueCollection()) {
				if (name != null && !name.isEmpty()) {
					collected.add(name);
				}
			}
			if (collected.size() >= NAMES) {
				break;
			}
		}
		return new ArrayList<>(collected);
	}
}
