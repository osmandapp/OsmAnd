package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.search.rules.SearchModRules;
import net.osmand.search.rules.SearchModRules.SearchModRuleOwner;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Timing of the search rules, {@link net.osmand.shared.search.rules.SearchModRules}, against
 * {@link SearchModRules#forSearch()}, over the locales and words of {@link SearchRulesCompatTest}: the rules of every
 * locale read, the forms of every word asked in every locale, whether every word of a name penalizes an object of
 * each owner, the way the spatial search asks it ({@code dictionary(locale).isCommonSkipOtherCnt}), and whether
 * every word is a part of a house number.
 *
 * <b>Not part of a normal run</b>: it only does anything when {@code OSMAND_SEARCH_RULES_BENCHMARK} is set. It asserts
 * only that both find as many, so a slow machine never turns the build red. The native half is
 * {@code SearchModRulesBenchmarkTest.kt} in OsmAnd-shared, over what {@code SearchRulesCompatTest} wrote out.
 * <pre>
 * OSMAND_SEARCH_RULES_BENCHMARK=1 ./gradlew :OsmAnd-java:test --tests "*SearchRulesBenchmarkTest" --rerun -i
 * </pre>
 */
public class SearchRulesBenchmarkTest {

	private static final int WARMUP_ROUNDS = 3;
	private static final int MEASURED_ROUNDS = 5;

	@Test
	public void searchRules() throws IOException {
		Assume.assumeTrue("set OSMAND_SEARCH_RULES_BENCHMARK to run", System.getenv("OSMAND_SEARCH_RULES_BENCHMARK") != null);
		// its own copy: the one of the compat test is where the native tests read the rules
		File dir = SearchRulesCompatTest.rulesDir("search-rules-benchmark");
		List<String> locales = new ArrayList<>();
		for (String l : SearchRulesCompatTest.locales(dir)) {
			// the two that throw in both
			if (!"_".equals(l) && !"-".equals(l)) {
				locales.add(l);
			}
		}
		List<String> words = SearchRulesCompatTest.words(SearchModRules.forSearch(), locales);
		List<Set<String>> splits = new ArrayList<>();
		for (String w : words) {
			splits.add(SearchRulesCompatTest.split(w));
		}
		SearchModRuleOwner[] owners = SearchModRuleOwner.values();
		net.osmand.shared.search.rules.SearchModRules.SearchModRuleOwner[] kowners =
				net.osmand.shared.search.rules.SearchModRules.SearchModRuleOwner.values();
		long[] best = new long[8];
		Arrays.fill(best, Long.MAX_VALUE);
		long[] jfound = new long[4];
		long[] kfound = new long[4];
		for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
			long[] t = new long[8];
			Arrays.fill(jfound, 0);
			Arrays.fill(kfound, 0);

			long started = System.nanoTime();
			SearchModRules j = SearchModRules.forSearch();
			for (String l : locales) {
				jfound[0] += j.dictionary(l).locale().length();
			}
			t[0] = System.nanoTime() - started;
			started = System.nanoTime();
			net.osmand.shared.search.rules.SearchModRules k =
					net.osmand.shared.search.rules.SearchModRules.Companion.forSearch(dir.getPath());
			for (String l : locales) {
				kfound[0] += k.dictionary(l).locale().length();
			}
			t[1] = System.nanoTime() - started;

			started = System.nanoTime();
			for (String l : locales) {
				for (String w : words) {
					jfound[1] += j.dictionary(l).getQueryForms(w).size();
				}
			}
			t[2] = System.nanoTime() - started;
			started = System.nanoTime();
			for (String l : locales) {
				for (String w : words) {
					kfound[1] += k.dictionary(l).getQueryForms(w).size();
				}
			}
			t[3] = System.nanoTime() - started;

			started = System.nanoTime();
			for (String l : locales) {
				for (String w : words) {
					for (SearchModRuleOwner o : owners) {
						jfound[2] += j.dictionary(l).isCommonSkipOtherCnt(w, o) ? 1 : 0;
					}
				}
			}
			t[4] = System.nanoTime() - started;
			started = System.nanoTime();
			for (String l : locales) {
				for (String w : words) {
					for (net.osmand.shared.search.rules.SearchModRules.SearchModRuleOwner o : kowners) {
						kfound[2] += k.dictionary(l).isCommonSkipOtherCnt(w, o) ? 1 : 0;
					}
				}
			}
			t[5] = System.nanoTime() - started;

			started = System.nanoTime();
			for (String l : locales) {
				for (int i = 0; i < words.size(); i++) {
					jfound[3] += j.dictionary(l).likelyPartOfBuilding(words.get(i), splits.get(i)) ? 1 : 0;
				}
			}
			t[6] = System.nanoTime() - started;
			started = System.nanoTime();
			for (String l : locales) {
				for (int i = 0; i < words.size(); i++) {
					kfound[3] += k.dictionary(l).likelyPartOfBuilding(words.get(i), splits.get(i)) ? 1 : 0;
				}
			}
			t[7] = System.nanoTime() - started;
			if (round >= WARMUP_ROUNDS) {
				for (int n = 0; n < t.length; n++) {
					best[n] = Math.min(best[n], t[n]);
				}
			}
		}
		for (int n = 0; n < jfound.length; n++) {
			assertEquals("found " + n, jfound[n], kfound[n]);
		}
		int asked = locales.size() * words.size();
		System.out.println("### search rules, best of " + MEASURED_ROUNDS + " in ms: java / copy");
		String[] names = {"read " + locales.size() + " locales", "query forms " + asked,
				"skip penalty " + asked * owners.length, "part of building " + asked};
		for (int n = 0; n < names.length; n++) {
			System.out.println(String.format("%-28s %9.2f %9.2f   found %d", names[n], best[2 * n] / 1e6,
					best[2 * n + 1] / 1e6, jfound[n]));
		}
	}
}
