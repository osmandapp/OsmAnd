package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.search.rules.SearchModDictionary;
import net.osmand.search.rules.SearchModLocaleRules;
import net.osmand.search.rules.SearchModLocales;
import net.osmand.search.rules.SearchModRules;
import net.osmand.search.rules.SearchModRules.SearchModRuleOwner;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * The shared copy of the search rules, {@link net.osmand.shared.search.rules.SearchModRules}, against
 * {@link SearchModRules#forSearch()}: the rules files of OsmAnd-java, every locale they have and their variants, every
 * word of their rules and its variants; and random rules files, with layers that replace and disable rules, mirror
 * pairs, several forms of a word and the sections only the OBF writer reads, through java's parser and the copy's.
 * Asked the same, the copy answers what java answers, in java's order.
 * <p>
 * The questions and a hash of java's answers go to {@code build/search-rules-java.txt}, which
 * {@code SearchModRulesTest} in OsmAnd-shared asks the copy on the jvm and on Kotlin/Native.
 * <p>
 * Behind {@code OSMAND_SEARCH_COMPAT}, see {@link SearchCompat}:
 * <pre>
 * OSMAND_SEARCH_COMPAT=1 ./gradlew :OsmAnd-java:test --tests "*SearchRulesCompatTest" --rerun -i
 * </pre>
 */
public class SearchRulesCompatTest {

	private static final File JAVA_DUMP = new File("build/search-rules-java.txt");
	private static final int RANDOM_CASES = 400;

	static final String[] RANDOM_FROM = {"st", "St", "dr", "pl", "e", "s", "ave", "av", "blvd", "1st", "ул", "пр",
			"str", "STR", "sta", "l'", "d'", "o", "bis", "apt", "de", "la", "und", "и", "ß", "école"};
	static final String[] RANDOM_TO = {"Street", "Saint", "Saint Louis", "  Place  ", "Drive", "Doctor", "Avenue",
			"Boulevard", "First", "улица", "проспект", "Straße", "Santa", "école", "Jirón", "North-West", "St.", "",
			" ", "Mount\tEverest", "x", "A&B", "\u00a0Place"};
	/** Blank for Kotlin's trim(), not for java's: a rare form, as java cannot read a file with it. */
	static final String[] RANDOM_BLANK_TO = {"\u00a0", "\u2003 "};
	static final String[] RANDOM_OBJECTS = {null, null, null, "", "*", "street", "poi", "locality,poi", "street, poi",
			" street ", "building", "street,", "nope", "STREET", "boundary", "postcode", "*,street", "street\u00a0"};
	static final String[] RANDOM_ENABLED = {null, null, null, null, null, "true", "false", "false", "FALSE"};
	static final String[] RANDOM_LOCALES = {"en-us", "de_DE", "fr", "zh-hant-tw", " it_CH ", "", "sr_latn_rs"};
	static final String[] RANDOM_PREFIXES = {"Us", "us_texas", "Germany", "switzerland_ticino", "Switzerland", "fr"};
	/** The locale of the random rules: the base and three layers, the files named as {@code SearchModRules} reads them. */
	static final String RANDOM_LOCALE = "xx_YY_ZZ";
	static final String[] RANDOM_FILES = {"rules.xml", "rules_xx.xml", "rules_xx_YY.xml", "rules_xx_YY_ZZ.xml"};

	private static File dir;
	private static SearchModRules java;
	private static net.osmand.shared.search.rules.SearchModRules copy;
	private static List<String> locales;
	private static List<String> words;
	private static List<Set<String>> splits;
	private static List<String> randomWords;
	private static List<Set<String>> randomSplits;
	private static Writer dump;
	private static int compared;

	@BeforeClass
	public static void setUp() throws IOException {
		SearchCompat.assumeEnabled();
		JAVA_DUMP.getParentFile().mkdirs();
		dir = rulesDir("search-rules-java");
		java = SearchModRules.forSearch();
		copy = net.osmand.shared.search.rules.SearchModRules.Companion.forSearch(dir.getPath());
		locales = locales(dir);
		words = words(java, locales);
		splits = new ArrayList<>();
		for (String w : words) {
			splits.add(split(w));
		}
		Set<String> random = new LinkedHashSet<>();
		for (String w : RANDOM_FROM) {
			random.addAll(variants(w));
		}
		for (String w : RANDOM_TO) {
			for (String part : w.trim().split("[\\s-]+")) {
				random.addAll(variants(part));
			}
		}
		randomWords = new ArrayList<>(random);
		randomSplits = new ArrayList<>();
		for (String w : randomWords) {
			randomSplits.add(split(w));
		}
		dump = Files.newBufferedWriter(new File(JAVA_DUMP.getPath() + ".part").toPath(), StandardCharsets.UTF_8);
		dump.write("D\t" + CommonWordsCompatTest.hex(dir.getAbsolutePath()) + "\n");
		for (String l : locales) {
			dump.write("L\t" + hexOrNull(l) + "\n");
		}
		for (int i = 0; i < words.size(); i++) {
			dump.write("W\t" + CommonWordsCompatTest.hex(words.get(i)) + "\t"
					+ SearchPhraseCompatTest.hexList(new ArrayList<>(splits.get(i))) + "\n");
		}
		for (int i = 0; i < randomWords.size(); i++) {
			dump.write("V\t" + CommonWordsCompatTest.hex(randomWords.get(i)) + "\t"
					+ SearchPhraseCompatTest.hexList(new ArrayList<>(randomSplits.get(i))) + "\n");
		}
	}

	@AfterClass
	public static void written() throws IOException {
		if (!SearchCompat.enabled() || dump == null) {
			return;
		}
		dump.close();
		dump = null;
		Files.move(new File(JAVA_DUMP.getPath() + ".part").toPath(), JAVA_DUMP.toPath(), StandardCopyOption.REPLACE_EXISTING);
		System.out.println("SearchRulesCompatTest: " + locales.size() + " locales, " + words.size() + " words, "
				+ RANDOM_CASES + " random rules, " + compared + " answers compared");
	}

	/**
	 * The rules files java reads here, in a directory for the copy: each the one its name finds first on the class
	 * path, as {@code SearchModRules} finds it. In the tests of OsmAnd-java that is the test layer
	 * {@code rules_en_US.xml} of {@code resources/test-resources/abbr} over the one of the jar.
	 */
	static File rulesDir(String dirName) throws IOException {
		File dir = new File(JAVA_DUMP.getParentFile(), dirName);
		deleteTree(dir);
		dir.mkdirs();
		Set<String> names = new TreeSet<>();
		Enumeration<URL> dirs = SearchModRules.class.getClassLoader().getResources("net/osmand/search/rules");
		while (dirs.hasMoreElements()) {
			String[] list = new File(dirs.nextElement().getPath()).list();
			if (list != null) {
				names.addAll(Arrays.asList(list));
			}
		}
		for (String name : names) {
			if (name.startsWith("rules") && name.endsWith(".xml")) {
				try (InputStream in = SearchModRules.class.getResourceAsStream("/net/osmand/search/rules/" + name)) {
					Files.copy(in, new File(dir, name).toPath());
				}
			}
		}
		return dir;
	}

	@Test
	public void everyLocaleAnswersTheSame() throws IOException {
		int rules = 0;
		for (String locale : locales) {
			String j = answers(() -> describe(java.rules(locale), java.dictionary(locale), words, splits));
			String k = answers(() -> describe(copy.rules(locale), copy.dictionary(locale), words, splits));
			same("locale " + locale, j, k);
			expect("locale " + hexOrNull(locale), j);
			rules += j.length();
		}
		assertTrue("rules described: " + rules, rules > 100_000);
	}

	@Test
	public void mapsAndLocaleNamesAreTheSame() throws Exception {
		Field field = SearchModLocales.class.getDeclaredField("localeByPrefix");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<String, String> byPrefix = (Map<String, String>) field.get(java.locales());
		assertTrue("map prefixes: " + byPrefix.size(), byPrefix.size() > 100);
		List<String> names = new ArrayList<>();
		for (String prefix : byPrefix.keySet()) {
			String capital = Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1);
			names.add(prefix);
			names.add(capital + "_europe_2.obf");
			names.add("/sdcard/osmand/" + prefix.toUpperCase(Locale.ROOT) + "_asia.obf");
			names.add("C:\\maps\\" + capital + ".obf");
			names.add(prefix + "x_y");
			names.add(prefix + "_");
		}
		names.addAll(Arrays.asList("World_basemap_2.obf", "", null, "_", "Us_new-york_northamerica_2.obf",
				"Switzerland_ticino_europe_2.obf", "İstanbul_turkey_asia", "Us_İ", "/", "us/", "\\Germany"));
		StringBuilder j = new StringBuilder();
		StringBuilder k = new StringBuilder();
		for (String name : names) {
			dump.write("M\t" + hexOrNull(name) + "\n");
			j.append(java.locales().forMap(name)).append('|').append(java.locales().mapPrefix(name)).append('\n');
			k.append(copy.locales().forMap(name)).append('|').append(copy.locales().mapPrefix(name)).append('\n');
		}
		same("maps", j.toString(), k.toString());
		expect("maps", j.toString());

		j.setLength(0);
		k.setLength(0);
		for (String locale : locales) {
			j.append(answers(() -> java.locales().normalize(locale))).append('|')
					.append(answers(() -> java.dictionary(locale).locale())).append('\n');
			k.append(answers(() -> copy.locales().normalize(locale))).append('|')
					.append(answers(() -> copy.dictionary(locale).locale())).append('\n');
		}
		same("locale names", j.toString(), k.toString());
		expect("locale names", j.toString());
	}

	@Test
	public void refsAreTheSame() throws IOException {
		StringBuilder j = new StringBuilder();
		StringBuilder k = new StringBuilder();
		for (int i = 0; i < words.size(); i++) {
			j.append(java.likelyPartOfRef(words.get(i), splits.get(i)) ? '1' : '0');
			k.append(copy.likelyPartOfRef(words.get(i), splits.get(i)) ? '1' : '0');
		}
		same("refs", j.toString(), k.toString());
		expect("refs", j.toString());
	}

	@Test
	public void withoutRulesTheSame() throws Exception {
		Constructor<SearchModRules> c = SearchModRules.class.getDeclaredConstructor(String.class, boolean.class);
		c.setAccessible(true);
		SearchModRules none = c.newInstance("/net/osmand/search/no-such-dir/", false);
		net.osmand.shared.search.rules.SearchModRules noneCopy =
				net.osmand.shared.search.rules.SearchModRules.Companion.forSearch(new File(dir, "no-such-dir").getPath());
		StringBuilder j = new StringBuilder();
		StringBuilder k = new StringBuilder();
		for (String locale : locales) {
			j.append(answers(() -> describe(none.rules(locale), none.dictionary(locale), words, splits)))
					.append(none.locales().forMap("Germany_europe")).append('\n');
			k.append(answers(() -> describe(noneCopy.rules(locale), noneCopy.dictionary(locale), words, splits)))
					.append(noneCopy.locales().forMap("Germany_europe")).append('\n');
		}
		same("no rules", j.toString(), k.toString());
		expect("no rules", j.toString());
	}

	/**
	 * Random rules files, put on the class path of the test, through java's {@code SearchModRules} and through the
	 * copy's over the same files: the rules of {@link #RANDOM_LOCALE}, read from the base and up to three layers.
	 */
	@Test
	public void randomRulesAnswerTheSame() throws Exception {
		Constructor<SearchModRules> javaRules = SearchModRules.class.getDeclaredConstructor(String.class, boolean.class);
		javaRules.setAccessible(true);
		// a directory of the class path of the test: java finds the files there as resources
		File classes = new File(SearchRulesCompatTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		String resources = "/net/osmand/search/rules-random/";
		File root = new File(classes, resources.substring(1));
		Random random = new Random(21102026);
		int broken = 0;
		int disabled = 0;
		int withRules = 0;
		deleteTree(root);
		try {
			for (int n = 0; n < RANDOM_CASES; n++) {
				List<String> files = new ArrayList<>();
				int layers = random.nextInt(4);
				File caseDir = new File(root, String.valueOf(n));
				caseDir.mkdirs();
				for (int i = 0; i <= layers; i++) {
					files.add(randomFile(random, i == 0));
					Files.write(new File(caseDir, RANDOM_FILES[i]).toPath(), files.get(i).getBytes(StandardCharsets.UTF_8));
					dump.write("F\t" + n + "\t" + RANDOM_FILES[i] + "\t" + CommonWordsCompatTest.hex(files.get(i)) + "\n");
					broken += javaParses(RANDOM_FILES[i], files.get(i)) ? 0 : 1;
				}
				SearchModRules j = javaRules.newInstance(resources + n + "/", false);
				net.osmand.shared.search.rules.SearchModRules k =
						net.osmand.shared.search.rules.SearchModRules.Companion.forSearch(caseDir.getPath());
				// the map locales of the prefixes, a line, then the rules
				String ja = answers(() -> forMaps(j) + "\n"
						+ describe(j.rules(RANDOM_LOCALE), j.dictionary(RANDOM_LOCALE), randomWords, randomSplits));
				String ka = answers(() -> forMaps(k) + "\n"
						+ describe(k.rules(RANDOM_LOCALE), k.dictionary(RANDOM_LOCALE), randomWords, randomSplits));
				same("random " + n + "\n" + String.join("\n----\n", files), ja, ka);
				expect("random " + n, ja);
				disabled += String.join("", files).contains("enabled=\"false\"") ? 1 : 0;
				withRules += answers(() -> String.valueOf(j.rules(RANDOM_LOCALE).query().size())).equals("0") ? 0 : 1;
			}
		} finally {
			deleteTree(root);
		}
		// java found the files on the class path: most cases have query rules
		assertTrue("with query rules " + withRules, withRules > RANDOM_CASES / 2);
		// the random files reach a broken file and a disabled rule often enough
		assertTrue("broken files " + broken, broken > 10 && broken < RANDOM_CASES / 2);
		assertTrue("disabled in " + disabled, disabled > RANDOM_CASES / 4);
	}

	private static String forMaps(SearchModRules rules) {
		StringBuilder sb = new StringBuilder();
		for (String prefix : RANDOM_PREFIXES) {
			sb.append(rules.locales().forMap(prefix + "_europe")).append(' ');
		}
		return sb.toString();
	}

	private static String forMaps(net.osmand.shared.search.rules.SearchModRules rules) {
		StringBuilder sb = new StringBuilder();
		for (String prefix : RANDOM_PREFIXES) {
			sb.append(rules.locales().forMap(prefix + "_europe")).append(' ');
		}
		return sb.toString();
	}

	/** Whether java's parser reads a file, the way {@code SearchModRules.forSearch()} parses it. */
	private static boolean javaParses(String file, String text) throws Exception {
		Class<?> parserClass = Class.forName("net.osmand.search.rules.SearchModRulesParser");
		Constructor<?> parserOf = parserClass.getDeclaredConstructor(String.class, SearchModLocales.class, boolean.class);
		parserOf.setAccessible(true);
		Method parse = parserClass.getDeclaredMethod("parse", InputStream.class);
		parse.setAccessible(true);
		Constructor<SearchModLocales> localesOf = SearchModLocales.class.getDeclaredConstructor();
		localesOf.setAccessible(true);
		try {
			parse.invoke(parserOf.newInstance(file, localesOf.newInstance(), false),
					new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
			return true;
		} catch (InvocationTargetException e) {
			return false;
		}
	}

	static void deleteTree(File f) {
		File[] children = f.listFiles();
		if (children != null) {
			for (File c : children) {
				deleteTree(c);
			}
		}
		f.delete();
	}

	private interface Answer {
		String get() throws Exception;
	}

	private static String answers(Answer answer) {
		try {
			return answer.get();
		} catch (Exception e) {
			return "throws";
		}
	}

	private static void same(String what, String j, String k) {
		compared++;
		if (!j.equals(k)) {
			String[] jl = j.split("\n", -1);
			String[] kl = k.split("\n", -1);
			for (int i = 0; i < Math.min(jl.length, kl.length); i++) {
				assertEquals(what + ", line " + i, jl[i], kl[i]);
			}
			assertEquals(what, j, k);
		}
	}

	private static void expect(String name, String java) throws IOException {
		dump.write("E\t" + name + "\t" + java.length() + "\t" + NameIndexCompatTest.fnv(java) + "\n");
	}

	private static String hexOrNull(String s) {
		return s == null ? "null" : CommonWordsCompatTest.hex(s);
	}

	/** Every locale of a rules file, "" and null, and their spellings: case, hyphens, spaces, scripts, nonsense. */
	static List<String> locales(File dir) {
		Set<String> result = new LinkedHashSet<>(Arrays.asList(null, "", " ", "en-us", "EN_us", "zh-hant-tw",
				"zh_HANT", "sr-Latn-RS", " de ", "de\u00a0", "\u0001de", "de_DE_x", "xx", "xx_YY", "en__US", "_en",
				"_", "-", "en_", "de-AT-1996", "nb_NO", "pt-br"));
		String[] names = dir.list();
		Arrays.sort(names);
		for (String name : names) {
			if (name.startsWith("rules_") && name.endsWith(".xml")) {
				String locale = name.substring("rules_".length(), name.length() - ".xml".length());
				result.add(locale);
				result.add(locale.toLowerCase(Locale.ROOT).replace('_', '-'));
			}
		}
		return new ArrayList<>(result);
	}

	/** Every word of the rules of the locales and its variants, and words of house numbers and refs. */
	static List<String> words(SearchModRules rules, List<String> locales) {
		Set<String> words = new TreeSet<>(Arrays.asList("12", "2b", "2bis", "a1", "A1", "a", "", "18-20", "#3",
				"Straße", "STRASSE", "école", "l'", "o'", "d`", "İstanbul", "ΣΑΣ", "ß", "ǅ", "x\u0301", "ab12",
				"12ab", "1/2", "k2", "apt", "bis", "ter", "д", "корп", "st.", "dr.", "e"));
		for (String locale : locales) {
			SearchModLocaleRules r;
			try {
				r = rules.rules(locale);
			} catch (RuntimeException e) {
				continue;
			}
			for (SearchModLocaleRules.WordRule rule : r.query()) {
				words.add(rule.word());
				for (SearchModLocaleRules.Form f : rule.forms()) {
					words.add(f.word());
				}
			}
			words.addAll(r.formWords());
			words.addAll(r.buildings());
			words.addAll(r.ignorables());
			words.addAll(r.skipPenalty().keySet());
		}
		Set<String> result = new LinkedHashSet<>();
		for (String w : words) {
			result.addAll(variants(w));
		}
		return new ArrayList<>(result);
	}

	private static List<String> variants(String w) {
		List<String> v = new ArrayList<>();
		v.add(w);
		v.add(w.toUpperCase(Locale.ROOT));
		if (!w.isEmpty()) {
			v.add(Character.toUpperCase(w.charAt(0)) + w.substring(1));
		}
		v.add(w + ".");
		return v;
	}

	/** The runs of digits and of letters of a word, as a house number splits: "2bis" is 2 and bis. */
	static Set<String> split(String w) {
		Set<String> parts = new LinkedHashSet<>();
		int start = 0;
		for (int i = 1; i <= w.length(); i++) {
			if (i == w.length() || Character.isDigit(w.charAt(i)) != Character.isDigit(w.charAt(i - 1))
					|| !Character.isLetterOrDigit(w.charAt(i))) {
				String part = w.substring(start, i);
				if (!part.isEmpty() && Character.isLetterOrDigit(part.charAt(0))) {
					parts.add(part);
				}
				start = i < w.length() && !Character.isLetterOrDigit(w.charAt(i)) ? i + 1 : i;
			}
		}
		return parts;
	}

	private static String describe(SearchModLocaleRules rules, SearchModDictionary dictionary, List<String> words,
			List<Set<String>> splits) {
		StringBuilder sb = new StringBuilder();
		for (SearchModLocaleRules.WordRule rule : rules.query()) {
			sb.append("q ").append(rule.word()).append(" =");
			for (SearchModLocaleRules.Form f : rule.forms()) {
				sb.append(' ').append(f.object()).append(':').append(f.word());
			}
			sb.append('\n');
		}
		sb.append("fw ").append(String.join(" ", rules.formWords())).append('\n');
		for (String w : rules.formWords()) {
			sb.append("f ").append(w).append(" =");
			for (SearchModLocaleRules.Form f : rules.forms(w)) {
				sb.append(' ').append(f.object()).append(':').append(f.word());
			}
			sb.append('\n');
		}
		for (String w : words) {
			if (!rules.forms(w).isEmpty()) {
				sb.append("fv ").append(w).append(' ').append(rules.forms(w).size()).append('\n');
			}
		}
		sb.append("b ").append(String.join(" ", rules.buildings())).append('\n');
		sb.append("i ").append(String.join(" ", rules.ignorables())).append('\n');
		rules.skipPenalty().forEach((w, list) -> sb.append("sp ").append(w).append(" = ").append(list).append('\n'));
		sb.append("d ").append(dictionary.locale()).append(" | ").append(dictionary).append('\n');
		for (int i = 0; i < words.size(); i++) {
			String w = words.get(i);
			List<SearchModDictionary.QueryForm> forms = dictionary.getQueryForms(w);
			if (!forms.isEmpty()) {
				sb.append("qf ").append(w).append(" =");
				for (SearchModDictionary.QueryForm f : forms) {
					sb.append(' ').append(f.object()).append(':').append(f.word()).append(':').append(f.reverse())
							.append(':').append(f.isUnscoped()).append(':');
					for (SearchModRuleOwner o : f.owners()) {
						sb.append(o.tag).append(',');
					}
					for (SearchModRuleOwner o : SearchModRuleOwner.values()) {
						sb.append(f.appliesTo(o) ? '1' : '0');
					}
				}
				sb.append('\n');
			}
			boolean building = dictionary.likelyPartOfBuilding(w, null);
			boolean split = dictionary.likelyPartOfBuilding(w, splits.get(i));
			StringBuilder skip = new StringBuilder();
			for (SearchModRuleOwner o : SearchModRuleOwner.values()) {
				skip.append(dictionary.isCommonSkipOtherCnt(w, o) ? '1' : '0');
				skip.append(dictionary.isCommonSkipOtherCnt(w.toLowerCase(Locale.ROOT), o) ? '1' : '0');
			}
			if (building || split || skip.indexOf("1") >= 0) {
				sb.append("w ").append(w).append(' ').append(building ? 1 : 0).append(split ? 1 : 0).append(' ')
						.append(skip).append('\n');
			}
		}
		return sb.toString();
	}

	private static String describe(net.osmand.shared.search.rules.SearchModLocaleRules rules,
			net.osmand.shared.search.rules.SearchModDictionary dictionary, List<String> words, List<Set<String>> splits) {
		StringBuilder sb = new StringBuilder();
		for (net.osmand.shared.search.rules.SearchModLocaleRules.WordRule rule : rules.query()) {
			sb.append("q ").append(rule.getWord()).append(" =");
			for (net.osmand.shared.search.rules.SearchModLocaleRules.Form f : rule.getForms()) {
				sb.append(' ').append(f.getObject()).append(':').append(f.getWord());
			}
			sb.append('\n');
		}
		sb.append("fw ").append(String.join(" ", rules.formWords())).append('\n');
		for (String w : rules.formWords()) {
			sb.append("f ").append(w).append(" =");
			for (net.osmand.shared.search.rules.SearchModLocaleRules.Form f : rules.forms(w)) {
				sb.append(' ').append(f.getObject()).append(':').append(f.getWord());
			}
			sb.append('\n');
		}
		for (String w : words) {
			if (!rules.forms(w).isEmpty()) {
				sb.append("fv ").append(w).append(' ').append(rules.forms(w).size()).append('\n');
			}
		}
		sb.append("b ").append(String.join(" ", rules.buildings())).append('\n');
		sb.append("i ").append(String.join(" ", rules.ignorables())).append('\n');
		rules.skipPenalty().forEach((w, list) -> sb.append("sp ").append(w).append(" = ").append(list).append('\n'));
		sb.append("d ").append(dictionary.locale()).append(" | ").append(dictionary).append('\n');
		net.osmand.shared.search.rules.SearchModRules.SearchModRuleOwner[] owners =
				net.osmand.shared.search.rules.SearchModRules.SearchModRuleOwner.values();
		for (int i = 0; i < words.size(); i++) {
			String w = words.get(i);
			List<net.osmand.shared.search.rules.SearchModDictionary.QueryForm> forms = dictionary.getQueryForms(w);
			if (!forms.isEmpty()) {
				sb.append("qf ").append(w).append(" =");
				for (net.osmand.shared.search.rules.SearchModDictionary.QueryForm f : forms) {
					sb.append(' ').append(f.getObject()).append(':').append(f.getWord()).append(':')
							.append(f.getReverse()).append(':').append(f.isUnscoped()).append(':');
					for (net.osmand.shared.search.rules.SearchModRules.SearchModRuleOwner o : f.owners()) {
						sb.append(o.getTag()).append(',');
					}
					for (net.osmand.shared.search.rules.SearchModRules.SearchModRuleOwner o : owners) {
						sb.append(f.appliesTo(o) ? '1' : '0');
					}
				}
				sb.append('\n');
			}
			boolean building = dictionary.likelyPartOfBuilding(w, null);
			boolean split = dictionary.likelyPartOfBuilding(w, splits.get(i));
			StringBuilder skip = new StringBuilder();
			for (net.osmand.shared.search.rules.SearchModRules.SearchModRuleOwner o : owners) {
				skip.append(dictionary.isCommonSkipOtherCnt(w, o) ? '1' : '0');
				skip.append(dictionary.isCommonSkipOtherCnt(w.toLowerCase(Locale.ROOT), o) ? '1' : '0');
			}
			if (building || split || skip.indexOf("1") >= 0) {
				sb.append("w ").append(w).append(' ').append(building ? 1 : 0).append(split ? 1 : 0).append(' ')
						.append(skip).append('\n');
			}
		}
		return sb.toString();
	}

	/** A random rules file: the base has {@code <locales>}, mostly. */
	static String randomFile(Random random, boolean base) {
		StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		int version = random.nextInt(40);
		sb.append("<rules").append(version == 0 ? "" : " version=\"" + (version == 1 ? "4" : "5") + "\"").append(">\n");
		if (base && random.nextInt(20) != 0) {
			sb.append("  <locales>\n");
			if (random.nextBoolean()) {
				sb.append("    <group id=\"g\" languages=\"en de\" locales=\"de_AT\"/>\n");
			}
			int maps = random.nextInt(4);
			for (int i = 0; i < maps; i++) {
				sb.append("    <map locale=\"").append(pick(random, RANDOM_LOCALES)).append("\" prefixes=\"");
				int prefixes = 1 + random.nextInt(3);
				for (int p = 0; p < prefixes; p++) {
					sb.append(p > 0 ? (random.nextBoolean() ? " " : "\t\n ") : "").append(pick(random, RANDOM_PREFIXES));
				}
				sb.append('"').append(random.nextBoolean() ? " group=\"g\" translit=\"zh\"" : "").append("/>\n");
			}
			sb.append("  </locales>\n");
		}
		int sections = random.nextInt(5);
		for (int s = 0; s < sections; s++) {
			switch (random.nextInt(5)) {
				case 0:
					// mirror pairs, directly under <rules>
					sb.append("  <rule from=\"").append(esc(pick(random, RANDOM_FROM))).append('"');
					sb.append(" to=\"").append(esc(pickTo(random))).append('"');
					attribute(sb, "object", pick(random, RANDOM_OBJECTS));
					attribute(sb, "enabled", pick(random, RANDOM_ENABLED));
					sb.append("/>\n");
					break;
				case 1:
					sb.append("  <index>\n    <rule object=\"street\" from=\"(unclosed\" to=\"x\"/>\n")
							.append("    <unglue glue=\".\" script=\"NoSuchScript\"/>\n    <class1>a b</class1>\n")
							.append("    <class9>z</class9>\n  </index>\n");
					break;
				case 2:
					sb.append("  <poi>\n    <abbr short=\" a \" full=\"b\"/>\n    <service word=\"DMV\" tags=\"a=b\"/>\n")
							.append("    <brand name=\"x\"/>\n  </poi>\n");
					break;
				default:
					sb.append("  <query>\n");
					int rules = 1 + random.nextInt(8);
					for (int r = 0; r < rules; r++) {
						randomQueryRule(random, sb);
					}
					sb.append("  </query>\n");
			}
		}
		int end = random.nextInt(40);
		if (end == 0) {
			// cut anywhere: a broken file, or a whole one without its end
			return sb.substring(0, random.nextInt(sb.length()));
		}
		return end == 1 ? sb.toString() : sb.append("</rules>\n").toString();
	}

	private static void randomQueryRule(Random random, StringBuilder sb) {
		int kind = random.nextInt(10);
		if (kind < 2) {
			sb.append("    <skipPenalty");
			attribute(sb, "object", pick(random, RANDOM_OBJECTS));
			attribute(sb, "enabled", pick(random, RANDOM_ENABLED));
			sb.append('>');
			int n = random.nextInt(4);
			for (int i = 0; i < n; i++) {
				sb.append(random.nextBoolean() ? " " : "\n\t").append(esc(pick(random, random.nextBoolean() ? RANDOM_FROM
						: RANDOM_TO)));
			}
			sb.append("</skipPenalty>\n");
			return;
		}
		sb.append("    <rule from=\"").append(esc(pick(random, RANDOM_FROM))).append('"');
		if (kind < 6) {
			String to = random.nextInt(8) == 0 ? null : pickTo(random);
			attribute(sb, "to", to);
			attribute(sb, "object", random.nextInt(6) == 0 ? "building" : pick(random, RANDOM_OBJECTS));
			attribute(sb, "enabled", pick(random, RANDOM_ENABLED));
			sb.append("/>\n");
			return;
		}
		if (random.nextInt(4) == 0) {
			attribute(sb, "to", pickTo(random));
		}
		attribute(sb, "object", pick(random, RANDOM_OBJECTS));
		attribute(sb, "enabled", pick(random, RANDOM_ENABLED));
		sb.append(">\n");
		int forms = random.nextInt(4);
		for (int i = 0; i < forms; i++) {
			if (random.nextInt(5) == 0) {
				sb.append("      <!-- a comment -->\n");
			}
			sb.append("      <to");
			attribute(sb, "object", pick(random, RANDOM_OBJECTS));
			sb.append('>').append(esc(pickTo(random))).append("</to>\n");
		}
		sb.append("    </rule>\n");
	}

	private static void attribute(StringBuilder sb, String name, String value) {
		if (value != null) {
			sb.append(' ').append(name).append("=\"").append(esc(value)).append('"');
		}
	}

	private static String esc(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;").replace("\t", "&#9;");
	}

	private static String pickTo(Random random) {
		return random.nextInt(40) == 0 ? pick(random, RANDOM_BLANK_TO) : pick(random, RANDOM_TO);
	}

	private static String pick(Random random, String[] values) {
		return values[random.nextInt(values.length)];
	}
}
