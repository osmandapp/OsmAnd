package net.osmand.shared.compat;

import static net.osmand.shared.compat.CommonWordsCompatTest.hex;
import static net.osmand.shared.compat.SearchPhraseCompatTest.hexList;
import static org.junit.Assert.assertTrue;

import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.NameIndexReader;
import net.osmand.binary.NameIndexReader.NameIndexReaderMatcher;
import net.osmand.binary.NameIndexReader.PrefixNameValue;
import net.osmand.binary.OsmandOdb.AddressNameIndexDataAtom;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndex.OsmAndPoiNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndexDataAtom;
import net.osmand.data.Building;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.QuadRect;
import net.osmand.data.Street;
import net.osmand.search.core.HashSkipTileQuadTree.TileEntry;
import net.osmand.search.core.spatial.SpatialSearchContext.SpatialSearchStats;
import net.osmand.search.core.spatial.SpatialSearchToken;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtomXY;
import net.osmand.search.core.spatial.SpatialSearchTokenAccess;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import net.osmand.search.rules.SearchModLocaleRules;
import net.osmand.search.rules.SearchModLocaleRules.Form;
import net.osmand.search.rules.SearchModLocaleRules.WordRule;
import net.osmand.search.rules.SearchModRules;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.SearchAlgorithms;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import gnu.trove.list.array.TIntArrayList;

/**
 * The words of a query of the spatial search, {@code SpatialSearchToken}, and their atoms: java writes
 * its answers to {@code build/search-tokens-java.txt}, and {@code SpatialSearchTokenTest} of the
 * commonTest of OsmAnd-shared asks the copy the same on the jvm and on the iOS simulator. The copy
 * keeps the package-private part of the token internal, out of reach of this test.
 * <p>
 * Over the search maps of the tests: for each name index, the words of the queries of the search
 * case of the map, words made of keys of the index, whole, with the incomplete dot and cut short,
 * words of numbers, house numbers, abbreviations, poi categories and odd chars, and random words.
 * Each word is held to the keys of the index by the prefix matcher, with and without the rules of
 * the locale of the map, as a whole word and not; to names of the index, made of its keys and
 * suffixes, by {@code matchName} for every type of atom, which picks the forms of the rules the
 * owner of the name has, and by {@code matchSplitName}. Then atoms made of the atoms of the index,
 * with the same ids now and then, other counts, objects with the same osm id, rule spellings and
 * house numbers, are added to two words: what each add answered, the atoms the word keeps, its tile
 * trees, the atoms deleted and enlarged. The places of the atoms are read by {@code NameIndexAtomXY}
 * with the three settings that change them, enlarged and intersected. Last, the statistics as text
 * and every setting of the search with its default and in the presets.
 * <p>
 * Behind {@code OSMAND_SEARCH_COMPAT}, see {@link SearchCompat}:
 * <pre>
 * OSMAND_SEARCH_COMPAT=1 ./gradlew :OsmAnd-java:test --tests "*SpatialSearchTokenCompatTest" --rerun -i
 * </pre>
 */
public class SpatialSearchTokenCompatTest {

	private static final File JAVA_DUMP = new File("build/search-tokens-java.txt");
	private static final int KEYS = 100;
	private static final int NAMES = 100;
	private static final int KEY_WORDS = 6;
	private static final int RANDOM_WORDS = 3;
	private static final int ATOM_BLOCKS = 60;
	private static final int BLOCK_ATOMS = 5;
	private static final int XY_ATOMS = 150;
	private static final int TILE_PAIRS = 4000;
	private static final int BOXES = 4000;
	private static final int RHUMB_BOXES = 20000;
	private static final int POI_BBOXES = 100;

	/** The rules locales the words are matched with, besides the locale of the map. */
	static final String[] LOCALES = {"", "en_US", "de_DE", "fr", "uk_UA", "it_CH", "zz"};
	/** Every type of atom: street, building, poi, ref, boundary, postcode, city, village, category, unknown. */
	static final int[] TYPES = {4, -2, -1, -3, 0, 2, 1, 3, -5, 7};
	/** Numbers, house numbers, abbreviations, categories and odd chars, given to every eighth index. */
	static final String[] WORDS = {"4", "#4", "4th", "12", "12.", "2b", "2B", "28-ма", "28", "pa-21", "pa21", "pa",
			"st", "st.", "St.", "ave", "str", "straße", "strasse", "pl", "apt", "bis", "2bis", "a", "a.", "b", "1", "1.",
			"e", "n", "nw", "école", "ecole", "улица", "вулиця", "ул.", "#^restaurant", "#^", "#^top_index_brand",
			".", "-", "'", "l'", "o'brien", "a1", "m4", "i-95", "us-1", "rue", "via", "san", "de", "der", "1/2", "½",
			"k-12", "ß", "ǅ", "İstanbul", "ﬁ", "0", "007", "12a.", "ab.", "abc.", "x-", "-x", "4-", "4.5"};
	private static final String RANDOM_CHARS = "abcdefghijklmnoprstuvwyzéèüößçñąłжщї0123456789-.'# ";
	private static final double[] ENLARGE_DIMS = {0, 500, 999, 1000, 1001, 5000, 10000, 10001, 50000, 100000,
			100001, 300000, 300001, 1e7, -1, Double.NaN};
	private static final double[] ENLARGE_MULTS = {0.2, 1.0, 20.0};

	private Writer dump;
	private final Random random = new Random(26);
	// words of the rules with forms for some owners only, and the words of those forms
	private final List<String> scopedWords = new ArrayList<>();
	private final List<String> formNames = new ArrayList<>();
	private int tokens;
	private int keyMatches;
	private int nameMatches;
	private int scopedOnly;
	private int splits;
	private int atomsAdded;
	private int atomsReplaced;
	private int atomsRefused;
	private int osmDuplicates;
	private int xys;
	private int poiBboxes;

	@Test
	public void tokensAnswerTheSame() throws Exception {
		SearchCompat.assumeEnabled();
		JAVA_DUMP.getParentFile().mkdirs();
		File dir = SearchRulesCompatTest.rulesDir("search-tokens-rules");
		SearchModRules rules = SearchModRules.forSearch();
		scopedForms(rules);
		File part = new File(JAVA_DUMP.getPath() + ".part");
		List<File> files = TestObf.searchFiles();
		if (files.isEmpty()) {
			files = TestObf.files();
		}
		int indexes = 0;
		try (Writer w = Files.newBufferedWriter(part.toPath(), StandardCharsets.UTF_8)) {
			dump = w;
			write("D", hex(dir.getAbsolutePath()));
			write("L", hexList(Arrays.asList(LOCALES)));
			stats();
			settings();
			tilePlaces();
			boxes();
			rhumbBoxes();
			for (File file : files) {
				BinaryMapIndexReader r = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
				try {
					String mapLocale = rules.locales().forMap(region(file, r));
					write("F", hex(file.getPath()), hex(mapLocale));
					List<String[]> phraseWords = phraseWords(file);
					int count = r.getAddressIndexes().size() + r.getPoiIndexes().size();
					for (int i = 0; i < count; i++) {
						index(rules, r, i, indexes++, mapLocale, phraseWords);
					}
				} finally {
					r.close();
				}
			}
			dump = null;
		}
		Files.move(part.toPath(), JAVA_DUMP.toPath(), StandardCopyOption.REPLACE_EXISTING);
		System.out.printf("SpatialSearchTokenCompatTest: %d files, %d indexes, %d words: %d keys and %d names matched "
						+ "(%d by scoped forms only), %d split; %d atoms added, %d replaced, %d refused (%d by osm id); %d places, "
						+ "%d of poi atoms by their bbox%n",
				files.size(), indexes, tokens, keyMatches, nameMatches, scopedOnly, splits, atomsAdded, atomsReplaced,
				atomsRefused, osmDuplicates, xys, poiBboxes);
		assertTrue("words", tokens > 3000);
		assertTrue("keys matched", keyMatches > 10000);
		assertTrue("names matched by scoped forms only", scopedOnly > 50);
		assertTrue("names split", splits > 100);
		assertTrue("atoms replaced", atomsReplaced > 100);
		assertTrue("atoms refused by osm id", osmDuplicates > 100);
		assertTrue("places", xys > 10000);
		assertTrue("poi places by their bbox", poiBboxes > 100);
	}

	private void scopedForms(SearchModRules rules) {
		TreeSet<String> words = new TreeSet<>();
		TreeSet<String> forms = new TreeSet<>();
		for (String l : LOCALES) {
			for (WordRule rule : rules.rules(l).query()) {
				for (Form f : rule.forms()) {
					if (!SearchModLocaleRules.ANY_OBJECT.equals(f.object()) && !f.word().isEmpty()) {
						words.add(rule.word());
						forms.add(f.word());
					}
				}
			}
		}
		scopedWords.addAll(words);
		formNames.addAll(forms);
	}

	// as SpatialSearchFileCache names the region of a map
	private static String region(File file, BinaryMapIndexReader r) {
		String region = file.getName();
		for (AddressRegion a : r.getAddressIndexes()) {
			region = a.getName();
		}
		for (PoiRegion a : r.getPoiIndexes()) {
			region = r.getAddressIndexes().isEmpty() ? a.getName() : region;
		}
		return region;
	}

	/** The words of the queries of the search case of the map, as SpatialTextSearch.splitWords makes them. */
	private static List<String[]> phraseWords(File obf) throws IOException {
		List<String[]> res = new ArrayList<>();
		String name = obf.getName().substring(0, obf.getName().length() - ".obf".length());
		File json = new File("src/test/resources/search/" + name + ".json");
		if (!json.exists()) {
			return res;
		}
		JSONObject test = new JSONObject(Files.readString(json.toPath()));
		List<String> queries = new ArrayList<>();
		JSONArray phrases = test.optJSONArray("phrases");
		for (int p = 0; phrases != null && p < phrases.length(); p++) {
			queries.add(phrases.getString(p));
		}
		if (test.has("phrase")) {
			queries.add(test.getString("phrase"));
		}
		for (String query : queries) {
			List<String> owords = new ArrayList<>();
			List<String> words = SearchAlgorithms.splitAndNormalize(query, owords, false);
			List<String[]> phrase = new ArrayList<>();
			for (int k = 0; k < words.size(); k++) {
				if (!words.get(k).equals(SpatialSearchToken.DOT_INCOMPLETE_STRING)) {
					phrase.add(new String[] {words.get(k), owords.get(k), String.valueOf(phrase.size()), "0"});
				}
			}
			for (String[] t : phrase) {
				int number = numberOf(t[0]);
				for (String[] o : phrase) {
					if (number > 0 && o != t && SearchAlgorithms.letters(noDot(o[0])) > 0
							&& Algorithms.extractFirstIntegerNumber(noDot(o[0])) == number) {
						t[3] = "1";
					}
				}
			}
			res.addAll(phrase);
		}
		return res;
	}

	private static String noDot(String w) {
		String aligned = SearchAlgorithms.alignChars(w);
		return aligned.endsWith(".") ? aligned.substring(0, aligned.length() - 1) : aligned;
	}

	// the mainNumber of a token of the word with the default MIN_CHARACTERS_INCOMPLETE
	private static int numberOf(String w) {
		String aligned = SearchAlgorithms.alignChars(w);
		boolean incomplete = aligned.endsWith(".");
		if (incomplete && w.length() <= 3) {
			return -1;
		}
		return SearchAlgorithms.letters(noDot(w)) == 0 ? Algorithms.extractFirstIntegerNumber(noDot(w)) : -1;
	}

	private void index(SearchModRules rules, BinaryMapIndexReader r, int i, int indexNo, String mapLocale,
			List<String[]> phraseWords) throws IOException {
		NameIndexReader reader = NameIndexCompatTest.javaReader(r, i);
		List<PrefixNameValue> blocks = new ArrayList<>();
		List<PrefixNameValue> all = r.readFullNameIndex(reader.setQuery("all", NameIndexCompatTest.javaAll()));
		if (all != null) {
			blocks.addAll(all);
		}
		blocks.sort(Comparator.comparingLong(p -> p.shift));
		boolean address = i < r.getAddressIndexes().size();
		write("I", String.valueOf(i), String.valueOf(blocks.size()));
		List<String> keys = new ArrayList<>();
		TreeSet<String> nameSet = new TreeSet<>();
		for (PrefixNameValue p : blocks) {
			keys.add(p.key);
			nameSet.add(p.key);
			for (String suffix : suffixes(p, address)) {
				nameSet.add(p.key + suffix);
			}
		}
		keys = sample(keys, KEYS);
		List<String> names = new ArrayList<>(nameSet);
		names = sample(names, NAMES);
		for (String[] w : phraseWords) {
			names.add(w[0]);
		}
		// every eighth index from the third: the words with forms for some owners only, in a locale of the rules
		boolean scoped = indexNo % 8 == 2;
		if (scoped) {
			names.addAll(formNames);
		}
		names.addAll(Arrays.asList("4th avenue", "28-ма лінія", "pa-21", "#^restaurant", "#^top_index_brand_x",
				"st. john", "12b", "2-я улица", "école", "o'brien"));
		write("K", hexList(keys));
		write("N", hexList(names));

		List<String[]> words = new ArrayList<>(phraseWords);
		for (String key : sample(keys, KEY_WORDS)) {
			words.add(new String[] {key, key, "0", "0"});
			words.add(new String[] {key + ".", key + ".", "1", "0"});
			if (key.length() > 2) {
				words.add(new String[] {key.substring(0, 2) + ".", key.substring(0, 2) + ".", "2", "0"});
			}
		}
		if (scoped) {
			for (String w : scopedWords) {
				words.add(new String[] {w, w, "1", "0"});
			}
		}
		if (indexNo % 8 == 0) {
			for (String w : WORDS) {
				words.add(new String[] {w, w.toUpperCase(), "0", w.equals("28") ? "1" : "0"});
			}
		}
		for (int k = 0; k < RANDOM_WORDS; k++) {
			StringBuilder sb = new StringBuilder();
			int len = 1 + random.nextInt(8);
			for (int c = 0; c < len; c++) {
				sb.append(RANDOM_CHARS.charAt(random.nextInt(RANDOM_CHARS.length())));
			}
			words.add(new String[] {sb.toString(), sb.toString(), String.valueOf(k), String.valueOf(k % 2)});
		}
		String[] nameLocales = scoped ? new String[] {mapLocale, "", LOCALES[(indexNo / 8) % LOCALES.length]}
				: new String[] {mapLocale, ""};
		for (String[] w : words) {
			token(rules, w, mapLocale, nameLocales, keys, names);
		}
		atoms(rules, blocks, address, mapLocale, keys.isEmpty() ? "x" : keys.get(0));
		places(blocks, address);
		if (address) {
			poiPlaces(blocks);
		}
	}

	private static List<String> suffixes(PrefixNameValue p, boolean address) throws IOException {
		return address ? p.getAddr().getSuffixesDictionaryList() : p.getPoi().getSuffixesDictionaryList();
	}

	private void token(SearchModRules rules, String[] w, String mapLocale, String[] nameLocales, List<String> keys,
			List<String> names) throws IOException {
		int min = w[0].length() % 5 == 0 ? 0 : 2;
		SpatialSearchToken t = new SpatialSearchToken(rules, min, w[0], w[1], Integer.parseInt(w[2]));
		boolean numberNamedByOther = w[3].equals("1");
		SpatialSearchTokenAccess.setNumberNamedByOther(t, numberNamedByOther);
		boolean category = w[0].length() % 3 == 0;
		if (category) {
			SpatialSearchTokenAccess.addPoiCategoryMatch(t, 3);
		}
		tokens++;
		StringBuilder sb = new StringBuilder();
		sb.append(hex(SpatialSearchTokenAccess.word(t))).append('|').append(hex(SpatialSearchTokenAccess.originalWord(t)))
				.append('|').append(hex(SpatialSearchTokenAccess.wordAligned(t))).append('|')
				.append(hex(SpatialSearchTokenAccess.wordNoDot(t))).append('|').append(SpatialSearchTokenAccess.incomplete(t))
				.append('|').append(t.getMainNumber()).append('|').append(SpatialSearchTokenAccess.categoryMatchMode(t))
				.append('|').append(hexList(new ArrayList<>(SpatialSearchTokenAccess.bldWordSplit(t)))).append('|')
				.append(matcher(t.getMainCollator().getPart(), t.getMainCollator().getMode())).append('|')
				.append(SpatialSearchTokenAccess.noDotCollatorMain(t) == null ? "-"
						: matcher(SpatialSearchTokenAccess.noDotCollatorMain(t).getPart(),
								SpatialSearchTokenAccess.noDotCollatorMain(t).getMode())).append('|')
				.append(SpatialSearchTokenAccess.noHyphenCollatorMain(t) == null ? "-"
						: matcher(SpatialSearchTokenAccess.noHyphenCollatorMain(t).getPart(),
								SpatialSearchTokenAccess.noHyphenCollatorMain(t).getMode())).append('|')
				.append(t.likelyRef()).append('|').append(t.isOnlyFullMatch()).append('|').append(hex(t.toString()))
				.append('|').append(hex(t.getToken())).append('|').append(t.hasPoiCategoryKeys()).append('|')
				.append(t.likelyPartOfBuilding()).append('|');
		for (int type : TYPES) {
			sb.append(SpatialSearchTokenAccess.ruleOwner(t, type)).append(',');
		}
		sb.append('|');
		for (String l : LOCALES) {
			sb.append(t.likelyPartOfBuilding(l) ? '1' : '0');
		}
		sb.append(t.likelyPartOfBuilding(mapLocale) ? '1' : '0').append('|').append(t.likelyPartOfBuilding());
		write("T", hex(w[0]), hex(w[1]), String.valueOf(min), w[2], numberNamedByOther ? "1" : "0", category ? "1" : "0",
				sb.toString());

		SpatialSearchStats stats = new SpatialSearchStats();
		String[] matcherLocales = {null, mapLocale, "en_US"};
		// a broad word on a word of its own: the keys a word matched are cached, whatever broad was then
		SpatialSearchToken broadToken = new SpatialSearchToken(rules, min, w[0], w[1], Integer.parseInt(w[2]));
		SpatialSearchTokenAccess.setBroad(broadToken, true);
		for (int l = 0; l < matcherLocales.length; l++) {
			for (int broad = 0; broad < 2; broad++) {
				SpatialSearchToken b = broad == 1 ? broadToken : t;
				NameIndexReaderMatcher m = SpatialSearchTokenAccess.prefixMatcher(b, stats, matcherLocales[l]);
				StringBuilder bits = new StringBuilder();
				for (String key : keys) {
					boolean match = m.matchKey(key);
					keyMatches += match ? 1 : 0;
					bits.append(match ? '1' : '0');
				}
				write("M", String.valueOf(l), String.valueOf(broad), bits.toString(), String.valueOf(b.isOnlyFullMatch()));
			}
		}
		for (String locale : nameLocales) {
			StringBuilder bits = new StringBuilder();
			for (String name : names) {
				TIntArrayList poiTypes = poiTypes(name);
				boolean unscoped = SpatialSearchTokenAccess.matchName(t, name, poiTypes, locale, 7);
				for (int type : TYPES) {
					boolean match = SpatialSearchTokenAccess.matchName(t, name, poiTypes, locale, type);
					nameMatches += match ? 1 : 0;
					scopedOnly += match && !unscoped ? 1 : 0;
					bits.append(match ? '1' : '0');
				}
			}
			write("P", hex(locale), bits.toString());
		}
		StringBuilder split = new StringBuilder();
		for (String name : names) {
			String[] res;
			try {
				res = SpatialSearchTokenAccess.matchSplitName(t, name);
			} catch (IndexOutOfBoundsException e) {
				// a word with nothing left after alignChars ("'") and a name from a digit: matchName takes it first
				split.append("!;");
				continue;
			}
			if (res == null) {
				split.append('-');
			} else {
				splits++;
				split.append(hex(res[0])).append(',').append(hex(res[1]));
			}
			split.append(';');
		}
		write("Q", split.toString());
		write("U", String.valueOf(t.likelyPartOfBuilding()), String.valueOf(t.likelyPartOfBuilding(mapLocale)));
	}

	private static String matcher(String part, Object mode) {
		return hex(part) + ":" + mode;
	}

	/** The poi types given with a name: a category name is matched by them, other names only carry them. */
	static TIntArrayList poiTypes(String name) {
		if (name.length() % 4 == 1) {
			return null;
		}
		TIntArrayList res = new TIntArrayList();
		res.add(name.length() % 5);
		res.add(name.length() % 3 + 7);
		return res;
	}

	private void atoms(SearchModRules rules, List<PrefixNameValue> blocks, boolean address, String mapLocale,
			String firstKey) throws IOException {
		String[][] words = {{firstKey, "0"}, {"12", "1"}};
		for (String[] w : words) {
			SpatialSearchToken t = new SpatialSearchToken(rules, 2, w[0], w[0], 0);
			SpatialSearchTokenAccess.setNumberNamedByOther(t, w[1].equals("1"));
			write("W", hex(w[0]), w[1]);
			List<NameIndexAtom> added = new ArrayList<>();
			int k = 0;
			for (PrefixNameValue p : sample(blocks, ATOM_BLOCKS)) {
				List<String> suffixes = suffixes(p, address);
				int count = address ? p.getAddr().getAtomCount() : p.getPoi().getAtomsCount();
				for (int ai = 0; ai < count && ai < BLOCK_ATOMS; ai++) {
					k++;
					AddressNameIndexDataAtom a = address ? p.getAddr().getAtom(ai) : null;
					OsmAndPoiNameIndexDataAtom b = address ? null : p.getPoi().getAtoms(ai);
					String name = suffixes.isEmpty() ? p.key : p.key + suffixes.get(k % suffixes.size());
					int type = address ? a.getType() : (k % 9 == 0 ? SpatialSearchToken.POI_REF_TYPE : SpatialSearchToken.POI_TYPE);
					if (k % 11 == 0) {
						type = SpatialSearchToken.BUILDING_TYPE;
					}
					long id = (p.shift << 3) | (ai % 3);
					long pid = k % 4 == 0 ? p.shift : 0;
					int objKind = k % 6;
					long objId = 64 + (k % 10) * 2 + (k % 20 == 0 ? 1 : 0);
					boolean cityAsStreet = k % 7 == 0;
					int other = address ? (a.getOtherWordsCountCount() > 0 ? a.getOtherWordsCount(0) : k % 3)
							: (b.getOtherWordsCountCount() > 0 ? b.getOtherWordsCount(0) : k % 3);
					int otherFound = k % 4;
					int nearby = k % 5 * 100;
					int bld = k % 2 == 0 ? -1 : k % 4;
					boolean ruleSpelling = k % 5 == 0;
					int distinct = k % 3 - 1;
					int locale = k % (LOCALES.length + 1);
					int variant = k % 3;
					boolean bldObject = k % 8 == 0;
					String[] in = {hex(name), String.valueOf(type), String.valueOf(id), String.valueOf(pid),
							String.valueOf(objKind), String.valueOf(objId), cityAsStreet ? "1" : "0", String.valueOf(other),
							String.valueOf(otherFound), String.valueOf(nearby), String.valueOf(bld), ruleSpelling ? "1" : "0",
							String.valueOf(distinct), String.valueOf(locale), p.shift + ":" + ai + ":" + variant,
							bldObject ? "1" : "0"};
					NameIndexAtom atom = SpatialSearchTokenAccess.atom(name, type, id, pid, object(objKind, objId),
							cityAsStreet, other, otherFound, new NameIndexAtomXY(a, b, settings(variant)), nearby, bld,
							locale < LOCALES.length ? LOCALES[locale] : mapLocale, ruleSpelling, distinct, poiTypes(name));
					if (bldObject) {
						SpatialSearchTokenAccess.setBldObject(atom, object(0, objId + 1));
					}
					add(t, atom, "A", in);
					added.add(atom);
					if (k % 13 == 0) {
						NameIndexAtom category = SpatialSearchTokenAccess.categoryAtom(NameIndexReader.POI_CATEGORY_PREFIX + p.key,
								k % 40, k);
						add(t, category, "C", hex(NameIndexReader.POI_CATEGORY_PREFIX + p.key), String.valueOf(k % 40),
								String.valueOf(k));
					}
					if (k % 17 == 0) {
						// the same atom again
						add(t, added.get(added.size() / 2), "R", String.valueOf(added.size() / 2));
					}
					if (k % 19 == 0) {
						NameIndexAtom copy = SpatialSearchTokenAccess.copy(added.get(added.size() / 3));
						add(t, copy, "Y", String.valueOf(added.size() / 3), SpatialSearchTokenAccess.describe(copy));
						added.add(copy);
					}
				}
			}
			write("Z", summary(t));
		}
	}

	private void add(SpatialSearchToken t, NameIndexAtom atom, String kind, String... in) throws IOException {
		List<NameIndexAtom> atoms = SpatialSearchTokenAccess.atoms(t);
		int before = atoms.size();
		int osmIds = SpatialSearchTokenAccess.indexByOsmIdsSize(t);
		boolean res = SpatialSearchTokenAccess.addAtom(t, atom);
		if (!res) {
			atomsRefused++;
			osmDuplicates += atoms.size() == before && SpatialSearchTokenAccess.indexByOsmIdsSize(t) == osmIds
					&& atom.getObject() != null && !(atom.getObject() instanceof Street) && !atoms.contains(atom) ? 1 : 0;
		} else if (atoms.size() == before) {
			atomsReplaced += atoms.contains(atom) ? 1 : 0;
		} else {
			atomsAdded++;
		}
		String[] line = Arrays.copyOf(in, in.length + 1);
		line[in.length] = String.valueOf(res);
		write(kind, line);
	}

	private static MapObject object(int kind, long id) {
		MapObject obj;
		if (kind == 0) {
			obj = new Building();
		} else if (kind == 1) {
			obj = new Street(null);
		} else {
			return null;
		}
		obj.setId(id);
		obj.setName("o" + id);
		obj.setLocation(40 + id / 1000.0, -74 - id / 1000.0);
		return obj;
	}

	/** The atoms the word keeps, its tile trees, then the atoms deleted and enlarged. */
	private static String summary(SpatialSearchToken t) {
		List<NameIndexAtom> atoms = SpatialSearchTokenAccess.atoms(t);
		StringBuilder sb = new StringBuilder();
		for (NameIndexAtom a : atoms) {
			sb.append(SpatialSearchTokenAccess.describe(a)).append('/').append(atomAnswers(a)).append(';');
		}
		for (int i = 0; i < atoms.size(); i++) {
			NameIndexAtom a = atoms.get(i);
			sb.append(SpatialSearchTokenAccess.atomToken(t, a) == a ? '=' : '+');
			if (i % 3 == 0) {
				t.addPartialCommonAtom(a, List.of(t), i % 2 == 0);
			}
			if (i % 5 == 0) {
				t.addPartialOtherAtom(a, List.of(), i % 4 == 0);
			}
		}
		sb.append('|').append(partials(t.getPartialExactMatch())).append('|').append(partials(t.getPartialMatch()));
		t.clearPartialAtoms();
		sb.append('|').append(t.getPartialExactMatch().size()).append(t.getPartialMatch().size());
		sb.append('|').append(SpatialSearchTokenAccess.indexSize(t)).append('|')
				.append(SpatialSearchTokenAccess.indexByOsmIdsSize(t)).append('|')
				.append(new TreeSet<>(SpatialSearchTokenAccess.poiCategoryKeysToAutocomplete(t))).append('|')
				.append(new TreeSet<>(SpatialSearchTokenAccess.poiCategoryIds(t))).append('|').append(t.hasPoiCategoryKeys())
				.append('|').append(trees(t)).append('|');
		for (int i = 0; i < atoms.size(); i += 4) {
			SpatialSearchTokenAccess.removeAtom(t, atoms.get(i));
		}
		int[] deleted = SpatialSearchTokenAccess.deletedAtoms(t);
		Arrays.sort(deleted);
		sb.append(Arrays.toString(deleted)).append('|');
		for (NameIndexAtom a : atoms) {
			if (a.isGeoArea() || SpatialSearchTokenAccess.bbox31(SpatialSearchTokenAccess.coords(a)) == null) {
				SpatialSearchTokenAccess.enlargeBbox(t, a, a.isGeoArea() ? 0.5 : 1.5);
				sb.append(xy(SpatialSearchTokenAccess.coords(a))).append(';');
			}
		}
		sb.append('|').append(trees(t));
		return sb.toString();
	}

	private static String partials(List<SpatialSearchToken.PartialMatch> list) {
		StringBuilder sb = new StringBuilder();
		for (SpatialSearchToken.PartialMatch p : list) {
			sb.append(p.atom().getName()).append(':').append(p.other().size()).append(':').append(p.nonNumericMatch()).append(';');
		}
		return sb.toString();
	}

	private static String atomAnswers(NameIndexAtom a) {
		LatLon l = a.getResultLocation();
		return a.isCityStreetName() + "," + a.isStreetBuilding() + "," + a.isPoiCategory() + "," + a.isPostcode() + ","
				+ a.isBoundary() + "," + a.isGeoArea() + "," + a.isCityVillage() + "," + a.isCity() + "," + a.isStreet() + ","
				+ a.atomicObject() + "," + a.isBuilding() + "," + a.isPOI() + "," + a.isPOIRef() + "," + a.typeStr() + ","
				+ hex(a.toString()) + "," + (l == null ? "null" : bits(l.getLatitude()) + ":" + bits(l.getLongitude())) + ","
				+ hex(a.getName()) + "," + (a.getObject() == null ? "null" : a.getObject().getId()) + ","
				+ (a.getBuilding() == null ? "null" : a.getBuilding().getId());
	}

	/** Each atom by the tiles of its place in the quad tree, the entries of the skip tree, the atoms of their bboxes. */
	private static String trees(SpatialSearchToken t) {
		StringBuilder sb = new StringBuilder();
		for (NameIndexAtom a : SpatialSearchTokenAccess.atoms(t)) {
			NameIndexAtomXY c = SpatialSearchTokenAccess.coords(a);
			SpatialSearchTokenAccess.quadTree(t).forEachMatch(SpatialSearchTokenAccess.bboxTileZoom(c),
					SpatialSearchTokenAccess.bboxTileId(c), list -> sb.append(list).append(' '));
			sb.append(';');
		}
		for (TileEntry<Integer> e : SpatialSearchTokenAccess.quadTreeSkip(t).getTileEntries()) {
			sb.append(e.objId).append(':').append(e.obj).append(':').append(Arrays.toString(e.bbox31)).append(':')
					.append(e.z).append(':').append(e.tileId).append(';');
		}
		return sb.toString();
	}

	private static SpatialTextSearchSettings settings(int variant) {
		SpatialTextSearchSettings s = SpatialTextSearchSettings.defaultSettings();
		if (variant == 1) {
			s.DEV_USE_SKIP_HASH_TREE = false;
		} else if (variant == 2) {
			s.USE_POI_BBOX = true;
		}
		return s;
	}

	/** The place of every atom of the index read with the settings that change it, enlarged and intersected. */
	private void places(List<PrefixNameValue> blocks, boolean address) throws IOException {
		int n = 0;
		NameIndexAtomXY prev = new NameIndexAtomXY(null, null, null);
		for (PrefixNameValue p : blocks) {
			int count = address ? p.getAddr().getAtomCount() : p.getPoi().getAtomsCount();
			for (int ai = 0; ai < count && n < XY_ATOMS; ai++, n++) {
				AddressNameIndexDataAtom a = address ? p.getAddr().getAtom(ai) : null;
				OsmAndPoiNameIndexDataAtom b = address ? null : p.getPoi().getAtoms(ai);
				for (int variant = 0; variant < 3; variant++) {
					NameIndexAtomXY xy = new NameIndexAtomXY(a, b, settings(variant));
					xys++;
					StringBuilder sb = new StringBuilder(xy(xy));
					sb.append('|').append(xy.tileIdString()).append('|').append(bits(xy.dimensionInM())).append('|')
							.append(xy.intersects(prev)).append(prev.intersects(xy)).append(xy.contains(prev))
							.append(prev.contains(xy)).append('|');
					int[] box = SpatialSearchTokenAccess.bbox31(prev);
					if (box != null) {
						sb.append(xy.intersects(box));
					}
					for (double mult : ENLARGE_MULTS) {
						NameIndexAtomXY e = new NameIndexAtomXY(a, b, settings(variant));
						e.enlargeBbox31(mult);
						sb.append('|').append(xy(e)).append(e.intersects(xy)).append(e.contains(xy)).append(xy.contains(e));
					}
					write("X", String.valueOf(p.shift), String.valueOf(ai), String.valueOf(variant), sb.toString());
					prev = xy;
				}
			}
		}
	}

	/**
	 * Places known only by their tile against each other: the same tile, its parents and children, other tiles and the
	 * same number at another zoom.
	 */
	private void tilePlaces() throws IOException {
		Random r = new Random(15);
		StringBuilder sb = new StringBuilder();
		for (int n = 0; n < TILE_PAIRS; n++) {
			int z1 = 10 + r.nextInt(7);
			int z2 = 10 + r.nextInt(7);
			long id1 = r.nextInt(64) == 0 ? r.nextInt(16) : (long) (r.nextDouble() * (1L << (2 * z1)));
			long id2 = switch (n % 4) {
				case 0 -> id1;
				case 1 -> z2 <= z1 ? id1 >> (2 * (z1 - z2)) : (id1 << (2 * (z2 - z1))) | r.nextInt(4);
				case 2 -> (long) (r.nextDouble() * (1L << (2 * z2)));
				default -> id1 ^ 1;
			};
			NameIndexAtomXY a = SpatialSearchTokenAccess.tilePlace(z1, id1);
			NameIndexAtomXY b = SpatialSearchTokenAccess.tilePlace(z2, id2);
			sb.setLength(0);
			sb.append(a.intersects(b)).append(b.intersects(a)).append(a.contains(b)).append(b.contains(a));
			write("V", String.valueOf(z1), String.valueOf(id1), String.valueOf(z2), String.valueOf(id2), sb.toString());
		}
	}

	/** Bboxes enlarged and measured, upside down and inside out as well. */
	private void boxes() throws IOException {
		Random r = new Random(31);
		for (int n = 0; n < BOXES; n++) {
			int[] box = new int[4];
			for (int i = 0; i < 4; i++) {
				box[i] = r.nextInt(1 << 30) + (1 << 29);
			}
			if (n % 3 == 0) {
				// a box the right way round; a random one is the other way round on an axis half the time
				Arrays.sort(box);
				box = new int[] {box[0], box[1], box[3], box[2]};
			}
			double mult = ENLARGE_MULTS[n % ENLARGE_MULTS.length] * (n % 5 == 0 ? -1 : 1);
			int[] enlarged = box.clone();
			NameIndexAtomXY.enlargeBbox(mult, enlarged);
			write("O", Arrays.toString(box), String.valueOf(mult), Arrays.toString(enlarged),
					bits(NameIndexAtomXY.distanceInM(box)), bits(NameIndexAtomXY.distanceInM(box[2], box[3], box[0], box[1])));
		}
	}

	/** The bbox of a radius around a tile, MapUtils.calc31BboxRhumb, over the world and the radii of the settings. */
	private void rhumbBoxes() throws IOException {
		Random r = new Random(47);
		int[] radii = {50, 300, 1000, 1, 20_000};
		for (int n = 0; n < RHUMB_BOXES; n++) {
			int radius = n % 7 == 0 ? r.nextInt(100_000) : radii[n % radii.length];
			int z = n % 9 == 0 ? r.nextInt(17) : 16;
			int x = r.nextInt(1 << z);
			int y = r.nextInt(1 << z);
			write("H", String.valueOf(radius), String.valueOf(x), String.valueOf(y), String.valueOf(z),
					Arrays.toString(MapUtils.calc31BboxRhumb(radius, x, y, z)));
		}
	}

	/** Poi atoms with the bboxes of the address atoms: the maps of the tests have none, and USE_POI_BBOX reads them. */
	private void poiPlaces(List<PrefixNameValue> blocks) throws IOException {
		OsmAndPoiNameIndexData.Builder block = OsmAndPoiNameIndexData.newBuilder();
		for (PrefixNameValue p : blocks) {
			for (AddressNameIndexDataAtom a : p.getAddr().getAtomList()) {
				if (a.hasBbox() && a.getXy16Count() > 0 && block.getAtomsCount() < POI_BBOXES) {
					block.addAtoms(OsmAndPoiNameIndexDataAtom.newBuilder().setX(a.getXy16(0) >>> 16)
							.setY(a.getXy16(0) & 0xffff).setBbox(a.getBbox()).buildPartial());
				}
			}
		}
		if (block.getAtomsCount() == 0) {
			return;
		}
		OsmAndPoiNameIndexData data = block.buildPartial();
		StringBuilder sb = new StringBuilder();
		for (OsmAndPoiNameIndexDataAtom b : data.getAtomsList()) {
			for (int variant = 0; variant < 3; variant++) {
				NameIndexAtomXY xy = new NameIndexAtomXY(null, b, settings(variant));
				if (variant == 2 && SpatialSearchTokenAccess.bbox31(xy) != null) {
					poiBboxes++;
				}
				sb.append(xy(xy)).append('|').append(bits(xy.dimensionInM())).append(';');
			}
		}
		write("B", hexBytes(data.toByteArray()), sb.toString());
	}

	private static String hexBytes(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
		}
		return sb.toString();
	}

	private static String xy(NameIndexAtomXY xy) {
		return Arrays.toString(SpatialSearchTokenAccess.bbox31(xy)) + ":" + SpatialSearchTokenAccess.bboxTileZoom(xy) + ":"
				+ SpatialSearchTokenAccess.bboxTileId(xy) + ":" + SpatialSearchTokenAccess.x16(xy) + ":"
				+ SpatialSearchTokenAccess.y16(xy);
	}

	/** The statistics as text, filled with the same numbers on both sides. */
	private void stats() throws IOException {
		SpatialSearchStats empty = new SpatialSearchStats();
		empty.doTiming = false;
		empty.sub1MatchTime.start();
		empty.sub1MatchTime.finish();
		write("S", "0", hex(empty.toString()), String.valueOf(empty.sub1PartMatchTime.intervals),
				String.valueOf(empty.sub1MatchTime.time));
		SpatialSearchStats s = new SpatialSearchStats();
		s.requestTime.time = 123_456_789L;
		s.step1Atoms.time = 45_678_901L;
		s.tokenObjs = 1_234_567;
		s.sub1FileAtomsTime.time = 999_950L;
		s.sub1FileAtomsTime.intervals = 42;
		s.sub1MatchTime.time = 50_000L;
		s.sub1MatchTime.intervals = 12_345;
		s.sub1PoiNameBoundaryTime.time = 7_049_999L;
		s.step2Compute.time = 1_000_000_000_000L;
		s.maxCombinations = 999;
		s.sub2LoadObjectsBldTime.time = 1L;
		s.sub2ReadObjTime.time = 150_000L;
		s.readTableBytes = 5_000_000_000L;
		s.readAtomsBytes = 1023;
		s.readObjsBytes = 1;
		write("S", "1", hex(s.toString()), String.valueOf(s.sub1PartMatchTime.intervals), String.valueOf(s.sub1MatchTime.time));
	}

	/** Every public setting with its default and in the presets, by name. */
	private void settings() throws Exception {
		QuadRect r = new QuadRect(-74.1, 40.9, -73.8, 40.6);
		SpatialTextSearchSettings[] presets = {SpatialTextSearchSettings.defaultSettings(),
				SpatialTextSearchSettings.searchPoiByCategorySettings(15, r),
				SpatialTextSearchSettings.searchPoiByCategorySettings(12, null),
				SpatialTextSearchSettings.searchPoiCategoriesSettings(14, r),
				SpatialTextSearchSettings.searchPoiCategoriesSettings(14, null),
				SpatialTextSearchSettings.suggestionSettings()};
		List<Field> fields = new ArrayList<>();
		for (Field f : SpatialTextSearchSettings.class.getFields()) {
			if (!Modifier.isStatic(f.getModifiers())) {
				fields.add(f);
			}
		}
		fields.sort(Comparator.comparing(Field::getName));
		for (int p = 0; p < presets.length; p++) {
			StringBuilder sb = new StringBuilder();
			for (Field f : fields) {
				Object v = f.get(presets[p]);
				sb.append(f.getName()).append('=').append(v instanceof int[] ? Arrays.toString((int[]) v) : String.valueOf(v))
						.append(';');
			}
			write("G", String.valueOf(p), sb.toString());
		}
		SpatialTextSearchSettings s = SpatialTextSearchSettings.defaultSettings();
		StringBuilder sb = new StringBuilder();
		for (double dim : ENLARGE_DIMS) {
			sb.append(bits(s.evalEnlargeBoundary(s.ENLARGE_BOUNDARIES, dim))).append(',');
		}
		write("E", sb.toString());
	}

	public static String bits(double v) {
		return Long.toHexString(Double.doubleToRawLongBits(v));
	}

	private static <T> List<T> sample(List<T> values, int count) {
		if (values.size() <= count) {
			return values;
		}
		List<T> res = new ArrayList<>();
		for (int n = 0; n < count; n++) {
			res.add(values.get((int) ((long) n * values.size() / count)));
		}
		return res;
	}

	private void write(String kind, String... fields) throws IOException {
		dump.write(kind);
		for (String f : fields) {
			dump.write('\t');
			dump.write(f);
		}
		dump.write('\n');
	}
}
