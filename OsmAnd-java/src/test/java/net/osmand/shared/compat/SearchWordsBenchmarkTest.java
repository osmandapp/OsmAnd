package net.osmand.shared.compat;

import net.osmand.binary.CommonWords;
import net.osmand.map.OsmandRegions;
import net.osmand.util.LocationParser;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * The java half of {@code SearchWordsBenchmarkTest} in OsmAnd-shared: how long {@link CommonWords}
 * takes to build its words with those of the regions, which the search does once, before its first
 * phrase; to rank words by them; and how long {@link LocationParser} takes to read places typed as
 * coordinates. The words and phrases are the ones {@link CommonWordsCompatTest} and
 * {@link LocationParserCompatTest} write to their dumps, which this reads.
 *
 * <b>Not part of a normal run</b>: it only does anything when {@code OSMAND_SEARCH_WORDS_BENCHMARK}
 * is set, and measures java alone, in a jvm of its own; the copy is measured by the shared half.
 * <pre>
 * OSMAND_SEARCH_WORDS_BENCHMARK=1 ./gradlew :OsmAnd-java:test --tests "*SearchWordsBenchmarkTest" --rerun -i
 * </pre>
 * On the {@code regions.ocbf} of 24.09.2026 and master of 26.09.2026, best of five in ms; the copy
 * and native columns are the shared half, on the jvm and on the release simulator binary:
 * <pre>
 * what                               java    copy  native   count
 * first build in the process         57.0    43.4    28.6       1
 * build the words                    13.6    12.7    28.8       1
 * rank 91170 words                    2.1     1.9     6.1   51655
 * parse 78372 phrases               139.8   128.9  1085.9   54609
 * </pre>
 */
public class SearchWordsBenchmarkTest {

	private static final File REGIONS = new File("src/main/resources/net/osmand/map/regions.ocbf");
	private static final File WORDS = new File("build/common-words-java.txt");
	private static final File PHRASES = new File("build/location-parser-java.txt");
	private static final int WARMUP_ROUNDS = 3;
	private static final int MEASURED_ROUNDS = 5;

	/** The steps of {@code CommonWords.getInstance}, in its order. */
	private static final String[] BUILD = {
			"addCalculatedAddrCommonWords", "addCalculatedPoiCommonWords", "addAbbrevationsToCommon",
			"addManualAbbrevationsToFrequent", "addRegionNames", "addCalculatedAddrFrequentWords",
			"addCalculatedPoiFrequentWords",
	};

	@Test
	public void benchmarkSearchWords() throws Exception {
		Assume.assumeTrue("set OSMAND_SEARCH_WORDS_BENCHMARK to run", System.getenv("OSMAND_SEARCH_WORDS_BENCHMARK") != null);
		Assume.assumeTrue("run CommonWordsCompatTest and LocationParserCompatTest first", WORDS.exists() && PHRASES.exists());
		List<String> words = inputs(WORDS, "W ");
		List<String> phrases = inputs(PHRASES, "L ");
		net.osmand.PlatformUtil.setOsmandRegions(new OsmandRegions(REGIONS.getPath()));

		double[] best = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
		double first = 0;
		int ranked = 0;
		int places = 0;
		for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
			long started = System.nanoTime();
			CommonWords cw = build();
			double build = (System.nanoTime() - started) / 1e6;

			started = System.nanoTime();
			int r = 0;
			for (String w : words) {
				if (cw.getCommonSearch(w) != -1) {
					r++;
				}
			}
			double rank = (System.nanoTime() - started) / 1e6;

			started = System.nanoTime();
			int p = 0;
			for (String phrase : phrases) {
				if (LocationParser.parseLocation(phrase) != null) {
					p++;
				}
			}
			double parse = (System.nanoTime() - started) / 1e6;

			if (round == 0) {
				first = build;
			}
			if (round >= WARMUP_ROUNDS) {
				double[] times = {build, rank, parse};
				for (int i = 0; i < times.length; i++) {
					best[i] = Math.min(best[i], times[i]);
				}
				ranked = r;
				places = p;
			}
		}
		System.out.println();
		System.out.println("### search words, java, best of " + MEASURED_ROUNDS);
		row("first build in the process", first, 1);
		row("build the words", best[0], 1);
		row("rank " + words.size() + " words", best[1], ranked);
		row("parse " + phrases.size() + " phrases", best[2], places);
	}

	/** A new {@link CommonWords}, built as {@code getInstance} builds its own. */
	private static CommonWords build() throws ReflectiveOperationException {
		Constructor<CommonWords> constructor = CommonWords.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		CommonWords cw = constructor.newInstance();
		Method addCommon = CommonWords.class.getDeclaredMethod("addCommon", String.class);
		addCommon.setAccessible(true);
		addCommon.invoke(cw, "NUMBER_WITH_LESS_THAN_2_LETTERS");
		for (String step : BUILD) {
			Method m = CommonWords.class.getDeclaredMethod(step);
			m.setAccessible(true);
			m.invoke(cw);
		}
		return cw;
	}

	private static List<String> inputs(File dump, String prefix) throws IOException {
		List<String> result = new ArrayList<>();
		for (String line : Files.readAllLines(dump.toPath(), StandardCharsets.UTF_8)) {
			if (line.startsWith(prefix)) {
				String h = line.substring(prefix.length() + 1, line.indexOf('\t'));
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < h.length(); i += 4) {
					sb.append((char) Integer.parseInt(h.substring(i, i + 4), 16));
				}
				result.add(sb.toString());
			}
		}
		return result;
	}

	private static void row(String what, double ms, int count) {
		System.out.println(String.format(java.util.Locale.US, "  %-34s %9.1f %9d", what, ms, count));
	}
}
