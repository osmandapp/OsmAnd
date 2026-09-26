package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.PlatformUtil;
import net.osmand.binary.Abbreviations;
import net.osmand.binary.CommonWords;
import net.osmand.binary.RouteDataObject;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.shared.util.KSearchAlgorithms;
import net.osmand.util.SearchAlgorithms;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * {@link net.osmand.shared.binary.CommonWords} and {@link net.osmand.shared.binary.Abbreviations}
 * against the originals in OsmAnd-java, both with the words of the regions of the
 * {@code regions.ocbf} that OsmAnd-java downloads into its resources.
 *
 * The dictionaries are compared whole, in order. Then every word they hold, every word of the
 * regions and the words of the road names of the test maps are asked of both, with the house
 * numbers and short words the dictionaries treat apart. The abbreviations are compared the same
 * way, and so are the letter counts of {@link KSearchAlgorithms} that both classes use.
 *
 * {@link #javaDumpIsWritten} also writes java's answers to {@code build/common-words-java.txt},
 * which {@code CommonWordsTest} in OsmAnd-shared holds the copy to on Kotlin/Native. With them go
 * the letters and digits of every character of the basic plane as java sees them, which that test
 * compares to what Kotlin/Native sees.
 */
public class CommonWordsCompatTest {

	private static final File REGIONS = new File("src/main/resources/net/osmand/map/regions.ocbf");

	/** Read by {@code CommonWordsTest} in OsmAnd-shared, from {@code ../OsmAnd-java/build}. */
	private static final File JAVA_DUMP = new File("build/common-words-java.txt");

	private static final String[] TRICKY_WORDS = {
			"", "a", "A", "e", "st", "St", "ST", "saint", "street", "straße", "strasse", "улица", "ул",
			"18", "18b", "18B", "18bis", "#3", "3rd", "31st", "100", "1a1", "a1", "-", "--", "1-2", "½",
			"٣", "٣٣", "no.", "dr.", "mc", "mc.", "о", "остров", "the", "und", "и", "NUMBER_WITH_LESS_THAN_2_LETTERS",
			"nc", "NC", "42", "new", "york", "paris", "london", "münchen", "munchen", "kyiv", "київ", "東京",
			"bis", "ter", "quater", "bldg", "apt", "fl", "2bis", "12a", "12ab", "ab12", "4th",
	};

	private static CommonWords java;
	private static net.osmand.shared.binary.CommonWords copy;
	private static List<String> words;

	@BeforeClass
	public static void open() throws IOException {
		assertTrue(REGIONS + " is missing; :OsmAnd-java:processResources downloads it", REGIONS.exists());
		PlatformUtil.setOsmandRegions(new OsmandRegions(REGIONS.getPath()));
		net.osmand.shared.binary.CommonWords.Companion.setOsmandRegions(
				new net.osmand.shared.map.OsmandRegions(REGIONS.getPath()));
		java = CommonWords.getInstance();
		copy = net.osmand.shared.binary.CommonWords.getInstance();

		Set<String> collected = new LinkedHashSet<>();
		Collections.addAll(collected, TRICKY_WORDS);
		collected.addAll(dictionary(java, "commonWordsDictionary").keySet());
		collected.addAll(dictionary(java, "frequentlyUsedWordsDictionary").keySet());
		collected.addAll(regionNames());
		for (Map.Entry<String, String> e : Abbreviations.getSearchabbreviations().entrySet()) {
			collected.add(e.getKey());
			collected.add(e.getValue());
			collected.add(e.getValue().toLowerCase());
		}
		for (RouteDataObject road : TestObf.roads(2000)) {
			if (road == null || road.names == null) {
				continue;
			}
			for (String name : road.names.valueCollection()) {
				if (name != null) {
					collected.addAll(SearchAlgorithms.splitAndNormalize(name, false));
				}
			}
		}
		for (String w : new ArrayList<>(collected)) {
			collected.add(w.toUpperCase());
		}
		words = new ArrayList<>(collected);
		assertTrue("words: " + words.size(), words.size() > 20000);
	}

	@Test
	public void dictionariesAreTheSame() {
		for (String field : new String[] {"commonWordsDictionary", "frequentlyUsedWordsDictionary"}) {
			assertEquals(field, new ArrayList<>(dictionary(java, field).entrySet()),
					new ArrayList<>(dictionary(copy, field).entrySet()));
		}
		assertEquals("regionNames", field(java, "regionNames"), field(copy, "regionNames"));
	}

	@Test
	public void answersAreTheSame() {
		for (String w : words) {
			assertEquals(line(java, w), copyLine(w));
		}
	}

	@Test
	public void abbreviationsAreTheSame() {
		assertEquals(new TreeMap<>(Abbreviations.getAbbreviations()),
				new TreeMap<>(net.osmand.shared.binary.Abbreviations.INSTANCE.getAbbreviations()));
		assertEquals(new TreeMap<>(Abbreviations.getSearchabbreviations()),
				new TreeMap<>(net.osmand.shared.binary.Abbreviations.INSTANCE.getSearchabbreviations()));
		for (String w : words) {
			assertEquals(abbreviationLine(w, false), abbreviationLine(w, true));
		}
	}

	@Test
	public void lettersAreCountedTheSameWay() {
		for (String w : words) {
			assertEquals(lettersLine(w, false), lettersLine(w, true));
		}
		for (int c = 0; c <= 0xFFFF; c++) {
			String w = String.valueOf((char) c);
			assertEquals(lettersLine(w, false), lettersLine(w, true));
		}
	}

	@Test
	public void javaDumpIsWritten() throws IOException {
		StringBuilder sb = new StringBuilder();
		for (String w : words) {
			sb.append(line(java, w)).append('\n');
			sb.append(abbreviationLine(w, false)).append('\n');
			sb.append(lettersLine(w, false)).append('\n');
		}
		sb.append("C ");
		for (int c = 0; c <= 0xFFFF; c++) {
			int flags = (Character.isLetter((char) c) ? 1 : 0) | (Character.isDigit((char) c) ? 2 : 0)
					| (Character.isDefined((char) c) ? 4 : 0);
			sb.append(Character.forDigit(flags, 16));
		}
		sb.append('\n');
		JAVA_DUMP.getParentFile().mkdirs();
		Files.write(JAVA_DUMP.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
	}

	/** {@code W word common search geocoding frequent isCommon}, as {@code CommonWordsTest} builds it. */
	private static String line(CommonWords cw, String w) {
		return "W " + hex(w) + "\t" + cw.getCommon(w) + "\t" + cw.getCommonSearch(w) + "\t"
				+ cw.getCommonGeocoding(w) + "\t" + cw.getFrequentlyUsed(w) + "\t" + cw.isCommon(w);
	}

	private static String copyLine(String w) {
		return "W " + hex(w) + "\t" + copy.getCommon(w) + "\t" + copy.getCommonSearch(w) + "\t"
				+ copy.getCommonGeocoding(w) + "\t" + copy.getFrequentlyUsed(w) + "\t" + copy.isCommon(w);
	}

	/**
	 * {@code A word parts conjunction replaced skip building buildingAlone ref}: the parts are the
	 * ones the spatial search splits a house number into and hands in with it.
	 */
	private static String abbreviationLine(String w, boolean ofCopy) {
		Set<String> parts = SearchAlgorithms.getBuildingCompareSet(w, null);
		StringBuilder sb = new StringBuilder("A ").append(hex(w)).append('\t');
		boolean first = true;
		for (String p : parts) {
			sb.append(first ? "" : ",").append(hex(p));
			first = false;
		}
		net.osmand.shared.binary.Abbreviations k = net.osmand.shared.binary.Abbreviations.INSTANCE;
		sb.append('\t').append(ofCopy ? k.isConjunction(w) : Abbreviations.isConjunction(w));
		sb.append('\t').append(hex(ofCopy ? k.replace(w) : Abbreviations.replace(w)));
		sb.append('\t').append(ofCopy ? k.isCommonSkipOtherCnt(w) : Abbreviations.isCommonSkipOtherCnt(w));
		sb.append('\t').append(ofCopy ? k.likelyPartOfBuilding(w, parts) : Abbreviations.likelyPartOfBuilding(w, parts));
		sb.append('\t').append(ofCopy ? k.likelyPartOfBuilding(w, null) : Abbreviations.likelyPartOfBuilding(w, null));
		sb.append('\t').append(ofCopy ? k.likelyPartOfRef(w, parts) : Abbreviations.likelyPartOfRef(w, parts));
		return sb.toString();
	}

	/** {@code K word letters letters3 isNumber2Letters startsWithDigit}. */
	private static String lettersLine(String w, boolean ofCopy) {
		KSearchAlgorithms k = KSearchAlgorithms.INSTANCE;
		return "K " + hex(w)
				+ "\t" + (ofCopy ? k.letters(w) : SearchAlgorithms.letters(w))
				+ "\t" + (ofCopy ? k.letters(w, 3) : SearchAlgorithms.letters(w, 3))
				+ "\t" + (ofCopy ? k.isNumber2Letters(w) : SearchAlgorithms.isNumber2Letters(w))
				+ "\t" + (ofCopy ? k.startsWithDigit(w) : SearchAlgorithms.startsWithDigit(w));
	}

	/** Each char as four hex digits, so that any string fits on a line. */
	static String hex(String s) {
		StringBuilder sb = new StringBuilder(s.length() * 4 + 1).append('x');
		for (int i = 0; i < s.length(); i++) {
			String h = Integer.toHexString(s.charAt(i));
			sb.append("0000", 0, 4 - h.length()).append(h);
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Integer> dictionary(Object cw, String name) {
		return (Map<String, Integer>) field(cw, name);
	}

	private static Object field(Object o, String name) {
		try {
			Field f = o.getClass().getDeclaredField(name);
			f.setAccessible(true);
			return f.get(o);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static Set<String> regionNames() throws IOException {
		Set<String> names = new LinkedHashSet<>();
		parseRegionNames(PlatformUtil.getOsmandRegions().getWorldRegion(), names);
		return names;
	}

	/** java's own {@code CommonWords.parseRegionNames}. */
	private static void parseRegionNames(WorldRegion region, Set<String> result) {
		try {
			Method m = CommonWords.class.getDeclaredMethod("parseRegionNames", WorldRegion.class, Set.class);
			m.setAccessible(true);
			m.invoke(null, region, result);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
