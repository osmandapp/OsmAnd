package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;

import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.NameIndexReader;
import net.osmand.binary.NameIndexReader.NameIndexReaderBytes;
import net.osmand.binary.NameIndexReader.NameIndexReaderMatcher;
import net.osmand.binary.NameIndexReader.PrefixNameValue;
import net.osmand.binary.NameIndexReader.ValueFreq;
import net.osmand.binary.OsmandOdb.AddressNameIndexDataAtom;
import net.osmand.binary.OsmandOdb.CommonIndexedStats;
import net.osmand.binary.OsmandOdb.OsmAndAddressNameIndexData.AddressNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndex.OsmAndPoiNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndexDataAtom;
import net.osmand.shared.util.KSearchAlgorithms;
import net.osmand.util.SearchAlgorithms;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The name index the spatial search reads: {@link net.osmand.shared.binary.NameIndexReader} on the
 * copy of the reader against {@link NameIndexReader} on java's, over the same files.
 *
 * Each address and poi name index is read twice over, with the blocks parsed and with them kept as
 * raw bytes, as the spatial search keeps them. First whole: every key of the string table and every
 * block, compared field by field down to each atom, with the decoded bbox of each atom, the common
 * words of the index, the sizes the cache counts and the bytes the reader counts. Then a fresh
 * reader is asked a run of queries made from the names of the index, whole, cut short, with the
 * incomplete dot, before a hyphen, and names it does not hold: the blocks each query returns and
 * the bytes it read and skipped are compared after every query, while the cache of the reader
 * fills up and is cleared and collected on the way. At the end the run reads the whole index on top
 * of the cached blocks, which java refuses with an exception, and goes on after resetting the read
 * limits. The matcher of the keys is held against java's over keys and queries of the index, and
 * the marker words over all the words of the index.
 *
 * Most test maps were built before the name index got its suffix dictionaries and common words;
 * the search maps of the last year have them. {@code OSMAND_OBF_CORPUS} runs the test over real
 * maps instead, all of which have them:
 * <pre>
 * OSMAND_SEARCH_COMPAT=1 OSMAND_TEST_HEAP=1g OSMAND_OBF_CORPUS=/maps/Andorra_europe.obf \
 *   ./gradlew :OsmAnd-java:test --tests "*NameIndexCompatTest"
 * </pre>
 *
 * <b>Not compared.</b> The order of the blocks a query returns, and of the blocks
 * {@code getMatchedPrefixes} lists: java takes it from the hash order of a trove set, the copy from
 * the hash order of its own set, so both are sorted by their offset here. {@code toString} of a
 * block: java lists the frequencies of its suffixes, which serve only the inspection tools.
 */
@RunWith(Parameterized.class)
public class NameIndexCompatTest {

	private static final int QUERY_KEYS = 10;

	/** The markers, spelled in other cases, cut and run on, and with the letters case folding treats apart. */
	private static final List<String> MARKER_WORDS = Arrays.asList(NameIndexReader.CITY_AS_STREET_COMMON,
			"CityAsStreetCommon", NameIndexReader.RULE_SPELLING_COMMON, "SPELLVARIANTCOMMON",
			NameIndexReader.ALT_NAME_COMMON_PREFIX, "altnamecommon0", "altnamecommon8", "altnamecommon10",
			"ALTNAMECOMMON3", "xaltnamecommon3", "", "c\u0130tyasstreetcommon", "cıtyasstreetcommon",
			"\u017fpellvariantcommon", "spellvariantcommon ", "altnamecommon\u0663", "ALTNAMECOMMON7");
	private static final int MATCHER_KEYS = 300;

	private static int indexes;
	private static int newFormatIndexes;
	private static int prefixesCompared;
	private static int atomsCompared;
	private static int bboxesCompared;
	private static int queriesCompared;
	private static int matchesCompared;
	private static int casedMatchesCompared;
	private static int refusalsCompared;

	private static final File JAVA_DUMP = new File("build/name-index-java.txt");
	private static Writer dump;
	private static int filesToDump;
	private static int filesDumped;

	private final File file;
	private final BinaryMapIndexReader java;
	private final net.osmand.shared.binary.BinaryMapIndexReader copy;

	public NameIndexCompatTest(String name, File file) throws IOException {
		this.file = file;
		this.java = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
		this.copy = new net.osmand.shared.binary.BinaryMapIndexReader(file.getPath());
	}

	@After
	public void close() throws IOException {
		java.close();
		copy.close();
	}

	@BeforeClass
	public static void enabled() throws IOException {
		SearchCompat.assumeEnabled();
		JAVA_DUMP.getParentFile().mkdirs();
		dump = Files.newBufferedWriter(new File(JAVA_DUMP.getPath() + ".part").toPath(), StandardCharsets.UTF_8);
		for (String w : MARKER_WORDS) {
			write("X\t" + CommonWordsCompatTest.hex(w) + "\t" + NameIndexReader.altNameVariant(w) + "\t"
					+ NameIndexReader.isIndexMarker(w));
		}
	}

	@AfterClass
	public static void namesWereCompared() throws IOException {
		if (!SearchCompat.enabled()) {
			return;
		}
		if (dump == null) {
			// enabled() failed to open it
			return;
		}
		dump.close();
		dump = null;
		File written = new File(JAVA_DUMP.getPath() + ".part");
		// moved in place only when every file was written: a dump cut short would pass for a whole one
		if (filesDumped == filesToDump) {
			Files.move(written.toPath(), JAVA_DUMP.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} else {
			System.out.println("NameIndexCompatTest: " + filesDumped + " of " + filesToDump + " files written, "
					+ JAVA_DUMP + " is left as it was");
		}
		System.out.println("NameIndexCompatTest: " + indexes + " name indexes (" + newFormatIndexes
				+ " with a suffix dictionary), " + prefixesCompared + " blocks, " + atomsCompared + " atoms, "
				+ bboxesCompared + " bboxes, " + queriesCompared + " queries, " + matchesCompared
				+ " key matches (" + casedMatchesCompared + " with the query capitalised), " + refusalsCompared
				+ " refused reads compared");
		String corpus = System.getenv("OSMAND_OBF_CORPUS");
		if (corpus != null && !corpus.isEmpty()) {
			// the counts below are those of the test maps
			return;
		}
		assertTrue("indexes: " + indexes, indexes > 100);
		assertTrue("indexes with a suffix dictionary: " + newFormatIndexes, newFormatIndexes > 10);
		assertTrue("blocks: " + prefixesCompared, prefixesCompared > 100_000);
		assertTrue("atoms: " + atomsCompared, atomsCompared > 100_000);
		assertTrue("bboxes: " + bboxesCompared, bboxesCompared > 1000);
		assertTrue("queries: " + queriesCompared, queriesCompared > 1000);
		assertTrue("key matches: " + matchesCompared, matchesCompared > 100_000);
		assertTrue("refused reads: " + refusalsCompared, refusalsCompared > 100);
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static List<Object[]> data() throws IOException {
		List<Object[]> data = new ArrayList<>();
		if (!SearchCompat.enabled()) {
			// runs before enabled(): the search maps are not unpacked for nothing
			return data;
		}
		List<File> files = new ArrayList<>(TestObf.files());
		files.addAll(TestObf.searchFiles());
		for (File file : files) {
			data.add(new Object[] {file.getName(), file});
		}
		filesToDump = data.size();
		return data;
	}

	/** The number of name indexes of the file, the same on both sides. */
	private int indexCount() {
		assertEquals("address sections", java.getAddressIndexes().size(), copy.getAddressIndexes().size());
		assertEquals("poi sections", java.getPoiIndexes().size(), copy.getPoiIndexes().size());
		return java.getAddressIndexes().size() + java.getPoiIndexes().size();
	}

	private NameIndexReader javaReader(int i) {
		return javaReader(java, i);
	}

	private net.osmand.shared.binary.NameIndexReader copyReader(int i) {
		return copyReader(copy, i);
	}

	/** Index {@code i} of the file: the address sections first, then the poi sections. */
	static NameIndexReader javaReader(BinaryMapIndexReader java, int i) {
		int addresses = java.getAddressIndexes().size();
		if (i < addresses) {
			return new NameIndexReader(java.getAddressIndexes().get(i));
		}
		return new NameIndexReader(java.getPoiIndexes().get(i - addresses));
	}

	static net.osmand.shared.binary.NameIndexReader copyReader(net.osmand.shared.binary.BinaryMapIndexReader copy, int i) {
		int addresses = copy.getAddressIndexes().size();
		if (i < addresses) {
			return new net.osmand.shared.binary.NameIndexReader(copy.getAddressIndexes().get(i));
		}
		return new net.osmand.shared.binary.NameIndexReader(copy.getPoiIndexes().get(i - addresses));
	}

	static NameIndexReaderMatcher javaAll() {
		return new NameIndexReaderMatcher("") {
			@Override
			public boolean matchKey(String key) {
				return true;
			}
		};
	}

	static net.osmand.shared.binary.NameIndexReader.NameIndexReaderMatcher copyAll() {
		return new net.osmand.shared.binary.NameIndexReader.NameIndexReaderMatcher("") {
			@Override
			public boolean matchKey(String key) {
				return true;
			}
		};
	}

	/** Every block of every index, read whole, then listed by a query that takes every key. */
	@Test
	public void wholeIndexesAreTheSame() throws IOException {
		int count = indexCount();
		for (int i = 0; i < count; i++) {
			for (boolean raw : new boolean[] {false, true}) {
				String m = "index " + i + (raw ? " raw" : "");
				NameIndexReader j = javaReader(i).setCacheRawBlocks(raw);
				net.osmand.shared.binary.NameIndexReader k = copyReader(i).setCacheRawBlocks(raw);
				assertNull(m + " java reads all", java.readFullNameIndex(j));
				assertNull(m + " copy reads all", copy.readFullNameIndex(k));
				sameBytes(m, j, k);
				sameCommonWords(m, j, k);

				List<PrefixNameValue> jall = java.readFullNameIndex(j.setQuery("all", javaAll()));
				List<net.osmand.shared.binary.NameIndexReader.PrefixNameValue> kall =
						copy.readFullNameIndex(k.setQuery("all", copyAll()));
				samePrefixes(m + " all", jall, kall);
				samePrefixes(m + " matched", j.getMatchedPrefixes("all"), k.getMatchedPrefixes("all"));
				assertNull(m + " unknown query", k.getMatchedPrefixes("none"));
				sameBytes(m + " all", j, k);
				if (jall == null) {
					// the list is made at the first block: an index without blocks has none
					continue;
				}
				if (!raw && j.poiRegion != null) {
					sameOrder(m, jall, kall);
				}
				if (!raw) {
					indexes++;
					prefixesCompared += jall.size();
					for (PrefixNameValue p : jall) {
						AddressNameIndexData a = p.getAddr();
						OsmAndPoiNameIndexData d = p.getPoi();
						if ((a != null ? a.getSuffixesDictionaryCount() : d.getSuffixesDictionaryCount()) > 0) {
							newFormatIndexes++;
							break;
						}
					}
				}
			}
		}
	}

	/** A run of queries made from the names of the index, on one reader that caches the blocks. */
	@Test
	public void queriesAreTheSame() throws IOException {
		int count = indexCount();
		for (int i = 0; i < count; i++) {
			List<String> queries = queries(i);
			for (boolean raw : new boolean[] {false, true}) {
				String m = "index " + i + (raw ? " raw" : "");
				NameIndexReader j = javaReader(i).setCacheRawBlocks(raw);
				net.osmand.shared.binary.NameIndexReader k = copyReader(i).setCacheRawBlocks(raw);
				for (int q = 0; q < queries.size(); q++) {
					String query = queries.get(q);
					String mq = m + " query " + q + " '" + query + "'";
					if (q % 3 == 0) {
						j.resetBytesStat();
						k.resetBytesStat();
					}
					samePrefixes(mq, java.readFullNameIndex(j.setQuery(query)), copy.readFullNameIndex(k.setQuery(query)), false);
					samePrefixes(mq + " matched", j.getMatchedPrefixes(query), k.getMatchedPrefixes(query), false);
					sameBytes(mq, j, k);
					queriesCompared++;
					if (q % 7 == 6) {
						j.clearPrefixes();
						k.clearPrefixes();
					} else if (q % 5 == 4) {
						j.gcPrefixes(20);
						k.gcPrefixes(20);
					}
					sameBytes(mq + " collected", j, k);
				}
				// the whole index over the cached blocks: java refuses to load a block twice
				j.clearQuery();
				k.clearQuery();
				String jout = outcome(() -> java.readFullNameIndex(j));
				String kout = outcome(() -> copy.readFullNameIndex(k));
				assertEquals(m + " whole index over the cache", jout, kout);
				if (jout.startsWith("threw")) {
					refusalsCompared++;
				}
				java.resetReadLimits();
				copy.resetReadLimits();
				j.clearPrefixes();
				k.clearPrefixes();
				String query = queries.get(0);
				samePrefixes(m + " after the limits are reset", java.readFullNameIndex(j.setQuery(query)),
						copy.readFullNameIndex(k.setQuery(query)));
			}
		}
	}

	/**
	 * The matcher of the keys over keys and queries of the index, and over the queries capitalised:
	 * the copy holds those to java with the query lowercased, see {@link #copyFoldsCaseItself}.
	 */
	@Test
	public void keysMatchTheSame() throws IOException {
		int count = indexCount();
		for (int i = 0; i < count; i++) {
			List<String> keys = sample(keys(i), MATCHER_KEYS);
			List<String> casedKeys = sample(keys, MATCHER_KEYS / 3);
			List<String> queries = queries(i);
			for (String query : queries) {
				NameIndexReaderMatcher jm = new NameIndexReaderMatcher(query);
				net.osmand.shared.binary.NameIndexReader.NameIndexReaderMatcher km =
						new net.osmand.shared.binary.NameIndexReader.NameIndexReaderMatcher(query);
				for (String key : keys) {
					assertEquals("index " + i + " query '" + query + "' key '" + key + "'", jm.matchKey(key), km.matchKey(key));
					matchesCompared++;
				}
				copyFoldsCaseItself(i, query.toUpperCase(Locale.ROOT), casedKeys);
				copyFoldsCaseItself(i, query.substring(0, 1).toUpperCase(Locale.ROOT) + query.substring(1), casedKeys);
			}
		}
	}

	/**
	 * <b>The copy folds the case of the query itself.</b> Java walks the keys with the static
	 * {@code CollatorStringMatcher.cmatches}, which lowercases the full name but not the part, and its
	 * collator does not fold the case of most scripts: "Кре." does not reach the key "крещ" when it is
	 * the part, which the incomplete dot makes it. The copy lowercases on every path, as
	 * {@code AddressReaderCompatTest} describes for the address search. The spatial search hands the
	 * matcher lowercased words ({@code SearchAlgorithms.normalizeToken}), so both answer the same
	 * there; here the copy with the query as typed is held to java with it lowercased.
	 */
	private static void copyFoldsCaseItself(int i, String typed, List<String> keys) {
		NameIndexReaderMatcher jm = new NameIndexReaderMatcher(typed.toLowerCase(Locale.ROOT));
		net.osmand.shared.binary.NameIndexReader.NameIndexReaderMatcher km =
				new net.osmand.shared.binary.NameIndexReader.NameIndexReaderMatcher(typed);
		for (String key : keys) {
			assertEquals("index " + i + " query '" + typed + "' key '" + key + "'", jm.matchKey(key), km.matchKey(key));
			casedMatchesCompared++;
		}
	}

	/** The marker words over the keys and the names of the index, and over each marker. */
	@Test
	public void markersAreTheSame() throws IOException {
		Set<String> words = new LinkedHashSet<>(MARKER_WORDS);
		for (int v = 1; v <= NameIndexReader.ALT_NAME_VARIANTS; v++) {
			assertEquals(NameIndexReader.altNameMarker(v), net.osmand.shared.binary.NameIndexReader.Companion.altNameMarker(v));
			words.add(NameIndexReader.altNameMarker(v));
		}
		int count = indexCount();
		for (int i = 0; i < count; i++) {
			words.addAll(names(i));
		}
		for (String w : words) {
			assertEquals("altNameVariant '" + w + "'", NameIndexReader.altNameVariant(w),
					net.osmand.shared.binary.NameIndexReader.Companion.altNameVariant(w));
			assertEquals("isIndexMarker '" + w + "'", NameIndexReader.isIndexMarker(w),
					net.osmand.shared.binary.NameIndexReader.Companion.isIndexMarker(w));
		}
		assertEquals(NameIndexReader.altNameVariant(null), net.osmand.shared.binary.NameIndexReader.Companion.altNameVariant(null));
		assertEquals(NameIndexReader.isIndexMarker(null), net.osmand.shared.binary.NameIndexReader.Companion.isIndexMarker(null));
		assertEquals(NameIndexReader.POI_CATEGORY_PREFIX, net.osmand.shared.binary.NameIndexReader.POI_CATEGORY_PREFIX);
	}

	private interface Read {
		Object get() throws Exception;
	}

	private static String outcome(Read read) {
		try {
			Object value = read.get();
			return value == null ? "null" : "a list of " + ((List<?>) value).size();
		} catch (Throwable t) {
			return "threw " + t.getClass().getName();
		}
	}

	/** Every block of the index, in the order of the file. */
	private List<PrefixNameValue> blocks(NameIndexReader j) throws IOException {
		List<PrefixNameValue> all = java.readFullNameIndex(j.setQuery("all", javaAll()));
		List<PrefixNameValue> res = all == null ? new ArrayList<>() : new ArrayList<>(all);
		res.sort(Comparator.comparingLong(p -> p.shift));
		return res;
	}

	/** The keys of the index, in the order of the file. */
	private List<String> keys(int i) throws IOException {
		List<PrefixNameValue> all = blocks(javaReader(i));
		List<String> keys = new ArrayList<>();
		for (PrefixNameValue p : all) {
			keys.add(p.key);
		}
		return keys;
	}

	/** The keys, the names their suffixes make and the common words of the index. */
	private List<String> names(int i) throws IOException {
		NameIndexReader j = javaReader(i);
		List<PrefixNameValue> all = blocks(j);
		Set<String> names = new LinkedHashSet<>();
		for (PrefixNameValue p : all) {
			names.add(p.key);
			AddressNameIndexData a = p.getAddr();
			OsmAndPoiNameIndexData d = p.getPoi();
			String suffix = null;
			for (String s : a != null ? a.getSuffixesDictionaryList() : d.getSuffixesDictionaryList()) {
				suffix = SearchAlgorithms.nameIndexDecodeDictionarySuffix(suffix, s);
				names.add(p.key + suffix);
				for (String w : (p.key + suffix).split(" ")) {
					names.add(w);
				}
			}
			for (String s : a != null ? atomExtras(a) : poiExtras(d)) {
				for (String w : s.split(" ")) {
					names.add(w);
				}
			}
		}
		Map<String, ValueFreq> common = j.getCommonWordsStats();
		if (common != null) {
			names.addAll(common.keySet());
		}
		return new ArrayList<>(names);
	}

	private static List<String> atomExtras(AddressNameIndexData a) {
		List<String> res = new ArrayList<>();
		for (AddressNameIndexDataAtom atom : a.getAtomList()) {
			res.addAll(atom.getExtraSuffixList());
		}
		return res;
	}

	private static List<String> poiExtras(OsmAndPoiNameIndexData d) {
		List<String> res = new ArrayList<>();
		for (OsmAndPoiNameIndexDataAtom atom : d.getAtomsList()) {
			res.addAll(atom.getExtraSuffixList());
		}
		return res;
	}

	/**
	 * Queries from evenly spread keys of the index: the key, its first letters, the key with the
	 * incomplete dot, the part before a hyphen, a whole name it starts, and names it does not hold.
	 */
	private List<String> queries(int i) throws IOException {
		List<String> keys = keys(i);
		List<String> names = names(i);
		Set<String> queries = new LinkedHashSet<>();
		for (String key : sample(keys, QUERY_KEYS)) {
			queries.add(key);
			for (int n = 1; n <= 3 && n < key.length(); n++) {
				queries.add(key.substring(0, n));
			}
			queries.add(key + ".");
			if (key.length() > 1) {
				queries.add(key.substring(0, key.length() - 1) + ".");
			}
			int hyphen = key.indexOf('-');
			if (hyphen > 0) {
				queries.add(key.substring(0, hyphen));
				queries.add(key.replace("-", ""));
			}
		}
		for (String name : sample(names, QUERY_KEYS)) {
			if (!name.isEmpty()) {
				queries.add(name);
			}
		}
		queries.add("zzqx");
		queries.add("12");
		queries.add(".");
		List<String> res = new ArrayList<>();
		for (String q : queries) {
			if (q != null && !q.isEmpty()) {
				res.add(q);
			}
		}
		return res;
	}

	private static List<String> sample(List<String> values, int count) {
		if (values.size() <= count) {
			return values;
		}
		List<String> res = new ArrayList<>();
		for (int n = 0; n < count; n++) {
			res.add(values.get((int) ((long) n * values.size() / count)));
		}
		return res;
	}

	private static void sameBytes(String m, NameIndexReader j, net.osmand.shared.binary.NameIndexReader k) {
		NameIndexReaderBytes jb = j.getBytesStat();
		net.osmand.shared.binary.NameIndexReader.NameIndexReaderBytes kb = k.getBytesStat();
		assertEquals(m + " readTableBytes", jb.readTableBytes, kb.getReadTableBytes());
		assertEquals(m + " skipTableBytes", jb.skipTableBytes, kb.getSkipTableBytes());
		assertEquals(m + " readAtomBytes", jb.readAtomBytes, kb.getReadAtomBytes());
		assertEquals(m + " skipAtomBytes", jb.skipAtomBytes, kb.getSkipAtomBytes());
		assertEquals(m + " cachedBytes", j.getCachedBytes(), k.getCachedBytes());
		assertEquals(m + " readAll", j.readAll(), k.readAll());
	}

	private static void sameCommonWords(String m, NameIndexReader j, net.osmand.shared.binary.NameIndexReader k) {
		CommonIndexedStats js = j.getCommonStats();
		net.osmand.shared.binary.CommonIndexedStats ks = k.getCommonStats();
		assertEquals(m + " common stats", js == null, ks == null);
		if (js == null) {
			assertNull(m + " common words", k.getCommonWordsStats());
			return;
		}
		assertEquals(m + " common values", js.getValueList(), ks.getValueList());
		assertEquals(m + " common matched", js.getMatchedList(), ks.getMatchedList());
		assertEquals(m + " common nonindexed", js.getNonindexedList(), ks.getNonindexedList());
		for (int n = 0; n < js.getValueCount(); n++) {
			assertEquals(m + " common word " + n, j.getCommonIndexed(n), k.getCommonIndexed(n));
		}
		Map<String, ValueFreq> jw = j.getCommonWordsStats();
		Map<String, net.osmand.shared.binary.NameIndexReader.ValueFreq> kw = k.getCommonWordsStats();
		assertEquals(m + " common words", jw.keySet(), kw.keySet());
		for (Map.Entry<String, ValueFreq> e : jw.entrySet()) {
			net.osmand.shared.binary.NameIndexReader.ValueFreq kv = kw.get(e.getKey());
			assertEquals(m + " " + e.getKey() + " value", e.getValue().value, kv.getValue());
			assertEquals(m + " " + e.getKey() + " freq", e.getValue().freq, kv.getFreq());
			assertEquals(m + " " + e.getKey() + " extra", e.getValue().extra, kv.getExtra());
		}
	}

	private void samePrefixes(String m, List<PrefixNameValue> jl,
			List<net.osmand.shared.binary.NameIndexReader.PrefixNameValue> kl) {
		samePrefixes(m, jl, kl, true);
	}

	/**
	 * The same blocks, by offset: their keys, and what each holds, field by field when {@code deep}, or
	 * else only its bytes or its size: the blocks of a query are blocks the whole read compared.
	 */
	private void samePrefixes(String m, List<PrefixNameValue> jl,
			List<net.osmand.shared.binary.NameIndexReader.PrefixNameValue> kl, boolean deep) {
		assertEquals(m + " null", jl == null, kl == null);
		if (jl == null) {
			return;
		}
		assertEquals(m + " size", jl.size(), kl.size());
		List<PrefixNameValue> js = new ArrayList<>(jl);
		List<net.osmand.shared.binary.NameIndexReader.PrefixNameValue> ks = new ArrayList<>(kl);
		js.sort(Comparator.comparingLong(p -> p.shift));
		ks.sort(Comparator.comparingLong(p -> p.shift));
		for (int n = 0; n < js.size(); n++) {
			PrefixNameValue j = js.get(n);
			net.osmand.shared.binary.NameIndexReader.PrefixNameValue k = ks.get(n);
			String mp = m + " block " + n + " '" + j.key + "'";
			assertEquals(mp + " key", j.key, k.key);
			assertEquals(mp + " shift", j.shift, k.shift);
			assertEquals(mp + " poi", j.poi != null, k.poi != null);
			assertEquals(mp + " addr", j.addr != null, k.addr != null);
			assertEquals(mp + " raw", j.data != null, k.data != null);
			if (j.data != null) {
				assertArrayEquals(mp + " data", j.data, k.data);
			}
			if (!deep) {
				if (j.addr != null) {
					assertEquals(mp + " serialized size", j.addr.getSerializedSize(), k.addr.getSerializedSize());
				}
				if (j.poi != null) {
					assertEquals(mp + " serialized size", j.poi.getSerializedSize(), k.poi.getSerializedSize());
				}
				continue;
			}
			try {
				AddressNameIndexData ja = j.getAddr();
				net.osmand.shared.binary.AddressNameIndexData ka = k.getAddr();
				assertEquals(mp + " getAddr", ja == null, ka == null);
				if (ja != null) {
					sameAddress(mp, ja, ka);
				}
				OsmAndPoiNameIndexData jp = j.getPoi();
				net.osmand.shared.binary.OsmAndPoiNameIndexData kp = k.getPoi();
				assertEquals(mp + " getPoi", jp == null, kp == null);
				if (jp != null) {
					samePoi(mp, jp, kp);
				}
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private static void sameOrder(String m, List<PrefixNameValue> jl,
			List<net.osmand.shared.binary.NameIndexReader.PrefixNameValue> kl) {
		List<PrefixNameValue> js = new ArrayList<>(jl);
		List<net.osmand.shared.binary.NameIndexReader.PrefixNameValue> ks = new ArrayList<>(kl);
		js.sort(Comparator.comparingLong(p -> p.shift));
		ks.sort(Comparator.comparingLong(p -> p.shift));
		js.sort(null);
		ks.sort(null);
		for (int n = 0; n < js.size(); n++) {
			assertEquals(m + " order " + n, js.get(n).key, ks.get(n).key);
		}
	}

	private void sameAddress(String m, AddressNameIndexData j, net.osmand.shared.binary.AddressNameIndexData k) {
		assertEquals(m + " serialized size", j.getSerializedSize(), k.getSerializedSize());
		assertEquals(m + " hasAtomsLength", j.hasAtomsLength(), k.hasAtomsLength());
		assertEquals(m + " atomsLength", j.getAtomsLength(), k.getAtomsLength());
		assertEquals(m + " suffixes", j.getSuffixesDictionaryList(), k.getSuffixesDictionaryList());
		assertEquals(m + " common suffixes", j.getSuffixesCommonDictionaryList(), k.getSuffixesCommonDictionaryList());
		assertEquals(m + " atoms", j.getAtomCount(), k.getAtomCount());
		for (int n = 0; n < j.getAtomCount(); n++) {
			AddressNameIndexDataAtom ja = j.getAtom(n);
			net.osmand.shared.binary.AddressNameIndexDataAtom ka = k.getAtom(n);
			String ma = m + " atom " + n;
			assertEquals(ma + " type", ja.getType(), ka.getType());
			assertEquals(ma + " bits", ja.getSuffixesBitsetIndexList(), ka.getSuffixesBitsetIndexList());
			assertEquals(ma + " extra", ja.getExtraSuffixList(), ka.getExtraSuffixList());
			assertEquals(ma + " other", ja.getOtherWordsCountList(), ka.getOtherWordsCountList());
			assertEquals(ma + " hasBbox", ja.hasBbox(), ka.hasBbox());
			assertArrayEquals(ma + " bbox", ja.getBbox().toByteArray(), ka.getBbox());
			assertEquals(ma + " hasEnclosing", ja.hasEnclosingObjects(), ka.hasEnclosingObjects());
			assertEquals(ma + " enclosing", ja.getEnclosingObjects(), ka.getEnclosingObjects());
			assertEquals(ma + " shiftToIndex", ja.getShiftToIndexList(), ka.getShiftToIndexList());
			assertEquals(ma + " shiftToCityIndex", ja.getShiftToCityIndexList(), ka.getShiftToCityIndexList());
			assertEquals(ma + " xy16", ja.getXy16List(), ka.getXy16List());
			if (ja.getXy16Count() > 0) {
				int xy16 = ja.getXy16(0);
				sameBbox(ma, ja.hasBbox() ? ja.getBbox() : null, ka.hasBbox() ? ka.getBbox() : null, xy16 >>> 16,
						xy16 & ((1 << 16) - 1));
			}
			atomsCompared++;
		}
	}

	private void samePoi(String m, OsmAndPoiNameIndexData j, net.osmand.shared.binary.OsmAndPoiNameIndexData k) {
		assertEquals(m + " serialized size", j.getSerializedSize(), k.getSerializedSize());
		assertEquals(m + " hasAtomsLength", j.hasAtomsLength(), k.hasAtomsLength());
		assertEquals(m + " atomsLength", j.getAtomsLength(), k.getAtomsLength());
		assertEquals(m + " suffixes", j.getSuffixesDictionaryList(), k.getSuffixesDictionaryList());
		assertEquals(m + " common suffixes", j.getSuffixesCommonDictionaryList(), k.getSuffixesCommonDictionaryList());
		assertEquals(m + " atoms", j.getAtomsCount(), k.getAtomsCount());
		for (int n = 0; n < j.getAtomsCount(); n++) {
			OsmAndPoiNameIndexDataAtom ja = j.getAtoms(n);
			net.osmand.shared.binary.OsmAndPoiNameIndexDataAtom ka = k.getAtoms(n);
			String ma = m + " atom " + n;
			assertEquals(ma + " hasZoom", ja.hasZoom(), ka.hasZoom());
			assertEquals(ma + " zoom", ja.getZoom(), ka.getZoom());
			assertEquals(ma + " hasX", ja.hasX(), ka.hasX());
			assertEquals(ma + " x", ja.getX(), ka.getX());
			assertEquals(ma + " hasY", ja.hasY(), ka.hasY());
			assertEquals(ma + " y", ja.getY(), ka.getY());
			assertEquals(ma + " bits", ja.getSuffixesBitsetIndexList(), ka.getSuffixesBitsetIndexList());
			assertEquals(ma + " extra", ja.getExtraSuffixList(), ka.getExtraSuffixList());
			assertEquals(ma + " other", ja.getOtherWordsCountList(), ka.getOtherWordsCountList());
			assertEquals(ma + " poiIndInBlock", ja.getPoiIndInBlockList(), ka.getPoiIndInBlockList());
			assertEquals(ma + " hasBbox", ja.hasBbox(), ka.hasBbox());
			assertArrayEquals(ma + " bbox", ja.getBbox().toByteArray(), ka.getBbox());
			assertEquals(ma + " categories", ja.getPoiCategoriesList(), ka.getPoiCategoriesList());
			assertEquals(ma + " elo", ja.getEloRatingList(), ka.getEloRatingList());
			assertEquals(ma + " hasShiftTo", ja.hasShiftTo(), ka.hasShiftTo());
			assertEquals(ma + " shiftTo", ja.getShiftTo(), ka.getShiftTo());
			assertEquals(ma + " z8", ja.getShiftToNextZ8Tile(), ka.getShiftToNextZ8Tile());
			assertEquals(ma + " z10", ja.getShiftToNextZ10Tile(), ka.getShiftToNextZ10Tile());
			assertEquals(ma + " z12", ja.getShiftToNextZ12Tile(), ka.getShiftToNextZ12Tile());
			sameBbox(ma, ja.hasBbox() ? ja.getBbox() : null, ka.hasBbox() ? ka.getBbox() : null, ja.getX(), ja.getY());
			atomsCompared++;
		}
	}

	private void sameBbox(String m, ByteString j, byte[] k, int x16, int y16) {
		int[] jb = SearchAlgorithms.decodeBboxForNameAtomsBytes(j, x16, y16);
		int[] kb = KSearchAlgorithms.INSTANCE.decodeBboxForNameAtomsBytes(k, x16, y16);
		assertArrayEquals(m + " decoded bbox", jb, kb);
		if (jb != null) {
			bboxesCompared++;
		}
	}

	/**
	 * What the run of {@link #wholeIndexesAreTheSame}, {@link #queriesAreTheSame}, {@link #keysMatchTheSame}
	 * and {@link #markersAreTheSame} gets from java, for {@code NameIndexReaderTest} in OsmAnd-shared to
	 * ask the copy wherever it runs: the bytes counted after each read, each block by its key, offset
	 * and a hash of what it holds, the blocks of each query on the raw blocks by a hash of their
	 * offsets, and the answers of the matcher and of the markers.
	 */
	@Test
	public void javaDump() throws IOException {
		write("F\t" + CommonWordsCompatTest.hex(file.getPath()));
		int count = indexCount();
		for (int i = 0; i < count; i++) {
			List<String> queries = queries(i);
			for (boolean raw : new boolean[] {false, true}) {
				write("I\t" + i + "\t" + (raw ? 1 : 0));
				NameIndexReader j = javaReader(i).setCacheRawBlocks(raw);
				write("A\t" + dumpOutcome(() -> java.readFullNameIndex(j)) + "\t" + bytes(j));
				write("C\t" + fnv(commonWords(j)));
				List<PrefixNameValue> all = java.readFullNameIndex(j.setQuery("all", javaAll()));
				for (PrefixNameValue p : sorted(all)) {
					write("B\t" + CommonWordsCompatTest.hex(p.key) + "\t" + p.shift + "\t" + fnv(describe(p)));
				}
				write("L\t" + shifts(all) + "\t" + shifts(j.getMatchedPrefixes("all")) + "\t" + bytes(j));
				if (!raw) {
					// the queries run on the raw blocks only, as the spatial search reads them
					continue;
				}
				NameIndexReader q = javaReader(i).setCacheRawBlocks(raw);
				for (int n = 0; n < queries.size(); n++) {
					String query = queries.get(n);
					if (n % 3 == 0) {
						q.resetBytesStat();
					}
					List<PrefixNameValue> res = java.readFullNameIndex(q.setQuery(query));
					write("Q\t" + CommonWordsCompatTest.hex(query) + "\t" + (n % 3 == 0 ? 1 : 0) + "\t" + shifts(res) + "\t"
							+ shifts(q.getMatchedPrefixes(query)) + "\t" + bytes(q));
					String action = n % 7 == 6 ? "clear" : n % 5 == 4 ? "gc" : "none";
					if (action.equals("clear")) {
						q.clearPrefixes();
					} else if (action.equals("gc")) {
						q.gcPrefixes(20);
					}
					write("G\t" + action + "\t" + bytes(q));
				}
				q.clearQuery();
				write("R\t" + dumpOutcome(() -> java.readFullNameIndex(q)));
				java.resetReadLimits();
				q.clearPrefixes();
				String query = queries.get(0);
				List<PrefixNameValue> res = java.readFullNameIndex(q.setQuery(query));
				write("Q\t" + CommonWordsCompatTest.hex(query) + "\t0\t" + shifts(res) + "\t"
						+ shifts(q.getMatchedPrefixes(query)) + "\t" + bytes(q));
			}
			List<String> keys = sample(keys(i), MATCHER_KEYS);
			StringBuilder line = new StringBuilder("K");
			for (String key : keys) {
				line.append('\t').append(CommonWordsCompatTest.hex(key));
			}
			write(line.toString());
			for (String query : queries) {
				NameIndexReaderMatcher jm = new NameIndexReaderMatcher(query);
				StringBuilder bits = new StringBuilder();
				for (String key : keys) {
					bits.append(jm.matchKey(key) ? '1' : '0');
				}
				write("M\t" + CommonWordsCompatTest.hex(query) + "\t" + bits);
			}
			Set<String> words = new TreeSet<>(names(i));
			words.remove("");
			StringBuilder answers = new StringBuilder();
			for (String w : words) {
				answers.append(NameIndexReader.altNameVariant(w)).append(NameIndexReader.isIndexMarker(w) ? "m" : "").append(';');
			}
			write("W\t" + words.size() + "\t" + fnv(answers.toString()));
		}
		filesDumped++;
	}

	private static void write(String line) throws IOException {
		dump.write(line);
		dump.write('\n');
	}

	private static String dumpOutcome(Read read) {
		String outcome = outcome(read);
		return outcome.startsWith("threw") ? "threw" : outcome;
	}

	/** FNV-1a over the chars, which {@code NameIndexReaderTest} computes the same. */
	public static long fnv(String s) {
		long h = 0xcbf29ce484222325L;
		for (int i = 0; i < s.length(); i++) {
			h ^= s.charAt(i);
			h *= 0x100000001b3L;
		}
		return h;
	}

	private static String bytes(NameIndexReader j) {
		NameIndexReaderBytes b = j.getBytesStat();
		return b.readTableBytes + "\t" + b.skipTableBytes + "\t" + b.readAtomBytes + "\t" + b.skipAtomBytes + "\t"
				+ j.getCachedBytes() + "\t" + j.readAll();
	}

	private static List<PrefixNameValue> sorted(List<PrefixNameValue> list) {
		List<PrefixNameValue> res = list == null ? new ArrayList<>() : new ArrayList<>(list);
		res.sort(Comparator.comparingLong(p -> p.shift));
		return res;
	}

	/** The number of blocks and a hash of their offsets in order, or null. */
	private static String shifts(List<PrefixNameValue> list) {
		if (list == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		for (PrefixNameValue p : sorted(list)) {
			sb.append(p.shift).append(',');
		}
		return list.size() + ":" + fnv(sb.toString());
	}

	private static String commonWords(NameIndexReader j) {
		CommonIndexedStats cs = j.getCommonStats();
		if (cs == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		strings(sb, cs.getValueList());
		sb.append('|').append(cs.getMatchedList()).append('|').append(cs.getNonindexedList()).append('|');
		for (int n = 0; n < cs.getValueCount(); n++) {
			sb.append(CommonWordsCompatTest.hex(j.getCommonIndexed(n))).append(',');
		}
		for (Map.Entry<String, ValueFreq> e : new java.util.TreeMap<>(j.getCommonWordsStats()).entrySet()) {
			sb.append('|').append(CommonWordsCompatTest.hex(e.getKey())).append(',').append(e.getValue().freq)
					.append(',').append(e.getValue().extra);
		}
		return sb.toString();
	}

	private static void strings(StringBuilder sb, List<String> list) {
		for (String v : list) {
			sb.append(CommonWordsCompatTest.hex(v)).append(',');
		}
	}

	private static String bytesHex(byte[] b) {
		StringBuilder sb = new StringBuilder();
		for (byte v : b) {
			sb.append(Character.forDigit((v >> 4) & 15, 16)).append(Character.forDigit(v & 15, 16));
		}
		return sb.toString();
	}

	/** All a block holds, as {@code NameIndexReaderTest.describe} writes it for the copy. */
	static String describe(PrefixNameValue p) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(p.poi != null ? 'p' : '-').append(p.addr != null ? 'a' : '-').append(p.data != null ? 'r' : '-');
		if (p.data != null) {
			sb.append(bytesHex(p.data));
		}
		AddressNameIndexData a = p.getAddr();
		if (a != null) {
			sb.append("|A").append(a.getSerializedSize()).append('|').append(a.hasAtomsLength()).append('|')
					.append(a.getAtomsLength()).append('|');
			strings(sb, a.getSuffixesDictionaryList());
			sb.append('|').append(a.getSuffixesCommonDictionaryList());
			for (AddressNameIndexDataAtom t : a.getAtomList()) {
				sb.append("|a").append(t.getType()).append(';').append(t.getSuffixesBitsetIndexList()).append(';');
				strings(sb, t.getExtraSuffixList());
				sb.append(';').append(t.getOtherWordsCountList()).append(';').append(t.hasBbox()).append(';')
						.append(bytesHex(t.getBbox().toByteArray())).append(';').append(t.hasEnclosingObjects()).append(';')
						.append(t.getEnclosingObjects()).append(';').append(t.getShiftToIndexList()).append(';')
						.append(t.getShiftToCityIndexList()).append(';').append(t.getXy16List());
				if (t.getXy16Count() > 0) {
					int xy16 = t.getXy16(0);
					sb.append(';').append(Arrays.toString(SearchAlgorithms.decodeBboxForNameAtomsBytes(
							t.hasBbox() ? t.getBbox() : null, xy16 >>> 16, xy16 & ((1 << 16) - 1))));
				}
			}
		}
		OsmAndPoiNameIndexData d = p.getPoi();
		if (d != null) {
			sb.append("|P").append(d.getSerializedSize()).append('|').append(d.hasAtomsLength()).append('|')
					.append(d.getAtomsLength()).append('|');
			strings(sb, d.getSuffixesDictionaryList());
			sb.append('|').append(d.getSuffixesCommonDictionaryList());
			for (OsmAndPoiNameIndexDataAtom t : d.getAtomsList()) {
				sb.append("|p").append(t.hasZoom()).append(';').append(t.getZoom()).append(';').append(t.hasX()).append(';')
						.append(t.getX()).append(';').append(t.hasY()).append(';').append(t.getY()).append(';')
						.append(t.getSuffixesBitsetIndexList()).append(';');
				strings(sb, t.getExtraSuffixList());
				sb.append(';').append(t.getOtherWordsCountList()).append(';').append(t.getPoiIndInBlockList()).append(';')
						.append(t.hasBbox()).append(';').append(bytesHex(t.getBbox().toByteArray())).append(';')
						.append(t.getPoiCategoriesList()).append(';').append(t.getEloRatingList()).append(';')
						.append(t.hasShiftTo()).append(';').append(t.getShiftTo()).append(';').append(t.getShiftToNextZ8Tile())
						.append(';').append(t.getShiftToNextZ10Tile()).append(';').append(t.getShiftToNextZ12Tile()).append(';')
						.append(Arrays.toString(SearchAlgorithms.decodeBboxForNameAtomsBytes(t.hasBbox() ? t.getBbox() : null,
								t.getX(), t.getY())));
			}
		}
		return sb.toString();
	}
}
