package net.osmand.shared.compat;

import static net.osmand.shared.compat.SearchPhraseCompatTest.unhex;
import static net.osmand.shared.compat.SearchPhraseCompatTest.unhexList;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.NameIndexReader;
import net.osmand.binary.NameIndexReader.NameIndexReaderMatcher;
import net.osmand.binary.NameIndexReader.PrefixNameValue;
import net.osmand.binary.OsmandOdb.AddressNameIndexDataAtom;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndexDataAtom;
import net.osmand.data.Building;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.search.core.spatial.SpatialSearchContext.SpatialSearchStats;
import net.osmand.search.core.spatial.SpatialSearchToken;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtomXY;
import net.osmand.search.core.spatial.SpatialSearchTokenAccess;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import net.osmand.search.rules.SearchModRules;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Timing of the words of a query of the spatial search, {@code SpatialSearchToken}, over what
 * {@link SpatialSearchTokenCompatTest} wrote to {@code build/search-tokens-java.txt}: the words made, each word held
 * to the keys of its name index by the prefix matcher and to its names by {@code matchName} for a street and a poi,
 * the names split by {@code matchSplitName}, the place of every atom of the indexes read, and the atoms added to
 * words. The copy keeps that part of the word internal, so the copy is timed by {@code SpatialSearchTokenBenchmarkTest}
 * of the commonTest of OsmAnd-shared, on the jvm and on the iOS simulator, over the same lines.
 *
 * <b>Not part of a normal run</b>: it only does anything when {@code OSMAND_SEARCH_TOKENS_BENCHMARK} is set, after
 * the compat test wrote its dump.
 * <pre>
 * OSMAND_SEARCH_TOKENS_BENCHMARK=1 ./gradlew :OsmAnd-java:test --tests "*SpatialSearchTokenBenchmarkTest" --rerun -i
 * </pre>
 */
public class SpatialSearchTokenBenchmarkTest {

	private static final int WARMUP_ROUNDS = 3;
	private static final int MEASURED_ROUNDS = 5;
	private static final int STREET = 4;
	private static final int POI = -1;

	private static class Index {
		String mapLocale;
		BinaryMapIndexReader reader;
		boolean address;
		final Map<Long, PrefixNameValue> blocks = new HashMap<>();
		List<String> keys;
		List<String> names;
		final List<String[]> words = new ArrayList<>();
		final List<List<String[]>> atoms = new ArrayList<>();
	}

	@Test
	public void tokens() throws IOException {
		Assume.assumeTrue("set OSMAND_SEARCH_TOKENS_BENCHMARK to run", System.getenv("OSMAND_SEARCH_TOKENS_BENCHMARK") != null);
		File dump = new File("build/search-tokens-java.txt");
		Assume.assumeTrue("run SpatialSearchTokenCompatTest first", dump.exists());
		SearchModRules rules = SearchModRules.forSearch();
		List<Index> indexes = new ArrayList<>();
		List<BinaryMapIndexReader> readers = new ArrayList<>();
		BinaryMapIndexReader reader = null;
		String mapLocale = "";
		Index index = null;
		for (String line : Files.readAllLines(dump.toPath(), StandardCharsets.UTF_8)) {
			String[] f = line.split("\t", -1);
			switch (f[0]) {
				case "F" -> {
					File file = new File(unhex(f[1]));
					reader = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
					readers.add(reader);
					mapLocale = unhex(f[2]);
				}
				case "I" -> {
					index = new Index();
					index.mapLocale = mapLocale;
					index.reader = reader;
					int i = Integer.parseInt(f[1]);
					index.address = i < reader.getAddressIndexes().size();
					NameIndexReader nameReader = NameIndexCompatTest.javaReader(reader, i);
					List<PrefixNameValue> all = reader.readFullNameIndex(nameReader.setQuery("all", NameIndexCompatTest.javaAll()));
					if (all != null) {
						for (PrefixNameValue p : all) {
							index.blocks.put(p.shift, p);
						}
					}
					indexes.add(index);
				}
				case "K" -> index.keys = unhexList(f[1]);
				case "N" -> index.names = unhexList(f[1]);
				case "T" -> index.words.add(new String[] {unhex(f[1]), unhex(f[2]), f[3], f[4]});
				case "W" -> index.atoms.add(new ArrayList<>());
				case "A" -> index.atoms.get(index.atoms.size() - 1).add(f);
				default -> {
				}
			}
		}
		SpatialTextSearchSettings settings = SpatialTextSearchSettings.defaultSettings();
		long[] best = new long[6];
		Arrays.fill(best, Long.MAX_VALUE);
		long[] found = new long[6];
		int words = 0;
		int keys = 0;
		int names = 0;
		int places = 0;
		int atoms = 0;
		for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
			long[] t = new long[6];
			Arrays.fill(found, 0);
			words = keys = names = places = atoms = 0;
			List<List<SpatialSearchToken>> tokens = new ArrayList<>();
			long started = System.nanoTime();
			for (Index x : indexes) {
				List<SpatialSearchToken> list = new ArrayList<>();
				for (String[] w : x.words) {
					SpatialSearchToken token = new SpatialSearchToken(rules, Integer.parseInt(w[2]), w[0], w[1],
							Integer.parseInt(w[3]));
					found[0] += token.likelyPartOfBuilding(x.mapLocale) ? 1 : 0;
					list.add(token);
				}
				words += list.size();
				tokens.add(list);
			}
			t[0] = System.nanoTime() - started;

			SpatialSearchStats stats = new SpatialSearchStats();
			started = System.nanoTime();
			for (int n = 0; n < indexes.size(); n++) {
				Index x = indexes.get(n);
				for (SpatialSearchToken token : tokens.get(n)) {
					NameIndexReaderMatcher m = SpatialSearchTokenAccess.prefixMatcher(token, stats, x.mapLocale);
					for (String key : x.keys) {
						found[1] += m.matchKey(key) ? 1 : 0;
					}
					keys += x.keys.size();
				}
			}
			t[1] = System.nanoTime() - started;

			started = System.nanoTime();
			for (int n = 0; n < indexes.size(); n++) {
				Index x = indexes.get(n);
				for (SpatialSearchToken token : tokens.get(n)) {
					for (String name : x.names) {
						found[2] += SpatialSearchTokenAccess.matchName(token, name, null, x.mapLocale, STREET) ? 1 : 0;
						found[2] += SpatialSearchTokenAccess.matchName(token, name, null, x.mapLocale, POI) ? 1 : 0;
					}
					names += 2 * x.names.size();
				}
			}
			t[2] = System.nanoTime() - started;

			started = System.nanoTime();
			for (int n = 0; n < indexes.size(); n++) {
				Index x = indexes.get(n);
				for (SpatialSearchToken token : tokens.get(n)) {
					for (String name : x.names) {
						try {
							found[3] += SpatialSearchTokenAccess.matchSplitName(token, name) == null ? 0 : 1;
						} catch (IndexOutOfBoundsException e) {
							found[3] += 2;
						}
					}
				}
			}
			t[3] = System.nanoTime() - started;

			started = System.nanoTime();
			for (Index x : indexes) {
				for (PrefixNameValue p : x.blocks.values()) {
					int count = x.address ? p.getAddr().getAtomCount() : p.getPoi().getAtomsCount();
					for (int ai = 0; ai < count; ai++) {
						NameIndexAtomXY xy = x.address ? new NameIndexAtomXY(p.getAddr().getAtom(ai), null, settings)
								: new NameIndexAtomXY(null, p.getPoi().getAtoms(ai), settings);
						found[4] += SpatialSearchTokenAccess.bboxTileZoom(xy);
						places++;
					}
				}
			}
			t[4] = System.nanoTime() - started;

			List<List<NameIndexAtom>> built = new ArrayList<>();
			for (Index x : indexes) {
				for (List<String[]> specs : x.atoms) {
					List<NameIndexAtom> list = new ArrayList<>();
					for (String[] f : specs) {
						list.add(atom(x, f, settings));
					}
					built.add(list);
				}
			}
			started = System.nanoTime();
			for (List<NameIndexAtom> list : built) {
				SpatialSearchToken token = new SpatialSearchToken(rules, 2, "main", "main", 0);
				for (NameIndexAtom a : list) {
					found[5] += SpatialSearchTokenAccess.addAtom(token, a) ? 1 : 0;
				}
				atoms += list.size();
			}
			t[5] = System.nanoTime() - started;
			if (round >= WARMUP_ROUNDS) {
				for (int n = 0; n < t.length; n++) {
					best[n] = Math.min(best[n], t[n]);
				}
			}
		}
		for (BinaryMapIndexReader r : readers) {
			r.close();
		}
		System.out.println("### spatial search words on java, best of " + MEASURED_ROUNDS + " in ms");
		String[] titles = {"words made " + words, "keys matched " + keys, "names matched " + names, "names split " + names / 2,
				"places read " + places, "atoms added " + atoms};
		for (int n = 0; n < titles.length; n++) {
			System.out.println(String.format("%-28s %9.2f   found %d", titles[n], best[n] / 1e6, found[n]));
		}
	}

	// the atom of a line A of the dump, as SpatialSearchTokenCompatTest made it
	private static NameIndexAtom atom(Index x, String[] f, SpatialTextSearchSettings settings) throws IOException {
		String[] place = f[15].split(":");
		PrefixNameValue p = x.blocks.get(Long.parseLong(place[0]));
		int ai = Integer.parseInt(place[1]);
		AddressNameIndexDataAtom a = x.address ? p.getAddr().getAtom(ai) : null;
		OsmAndPoiNameIndexDataAtom b = x.address ? null : p.getPoi().getAtoms(ai);
		long objId = Long.parseLong(f[6]);
		MapObject obj = null;
		if (f[5].equals("0") || f[5].equals("1")) {
			obj = f[5].equals("0") ? new Building() : new Street(null);
			obj.setId(objId);
		}
		int locale = Integer.parseInt(f[14]);
		return SpatialSearchTokenAccess.atom(unhex(f[1]), Integer.parseInt(f[2]), Long.parseLong(f[3]),
				Long.parseLong(f[4]), obj, f[7].equals("1"), Integer.parseInt(f[8]), Integer.parseInt(f[9]),
				new NameIndexAtomXY(a, b, settings), Integer.parseInt(f[10]), Integer.parseInt(f[11]),
				locale < SpatialSearchTokenCompatTest.LOCALES.length ? SpatialSearchTokenCompatTest.LOCALES[locale] : x.mapLocale,
				f[12].equals("1"), Integer.parseInt(f[13]), null);
	}
}
