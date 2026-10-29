package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.NameIndexReader;
import net.osmand.binary.NameIndexReader.PrefixNameValue;
import net.osmand.binary.OsmandOdb.OsmAndAddressNameIndexData.AddressNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndex.OsmAndPoiNameIndexData;
import net.osmand.util.SearchAlgorithms;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Timing of the name index the spatial search reads, {@link net.osmand.shared.binary.NameIndexReader}
 * against {@link NameIndexReader}, both keeping the blocks raw as the spatial search does: every block
 * of every name index of a map, 20 words of the map on a fresh reader, the same words again on the
 * reader that cached their blocks, and the parse of every block.
 *
 * <b>Not part of a normal run</b>: it only does anything when {@code OSMAND_NAME_INDEX_BENCHMARK} is
 * set. The files are {@link TestObf#files()}, so {@code OSMAND_OBF_CORPUS} points it at real maps.
 * It asserts only that both readers found the same, so a slow machine never turns the build red;
 * what they find is compared in {@link NameIndexCompatTest}. The native half is
 * {@code NameIndexBenchmarkTest.kt} in OsmAnd-shared, over the same files and words.
 * <pre>
 * OSMAND_NAME_INDEX_BENCHMARK=1 OSMAND_TEST_HEAP=2g OSMAND_OBF_CORPUS=/maps/Andorra_europe.obf \
 *   ./gradlew :OsmAnd-java:test --tests "*NameIndexBenchmarkTest" --rerun -i
 * </pre>
 *
 * On four regional maps, best of five in ms; the native column is the cross platform half on the
 * release simulator binary:
 * <pre>
 * map                       whole: java  copy  native   20 words: java  copy  native   parse: java  copy  native
 * Andorra (10 MB)                  1.47  1.20    0.69             11.6  4.22    4.33           7.91  5.15    5.52
 * Limpopo (73 MB)                  1.64  1.21    1.67             7.63  3.23    4.46           6.29  5.44    10.4
 * Hamburg (153 MB)                 5.07  3.76    5.84             12.2  5.70    11.0           36.8  32.5    58.7
 * Noord-Holland (414 MB)           7.72  5.38    7.35             17.1  7.19    13.0           57.8  48.8    97.2
 * </pre>
 * The words are found 2.1 to 2.8 times faster by the copy on the jvm and 1.1 to 2.7 times on
 * Kotlin/Native than by java: most of a query is the walk over the keys, the second run over the
 * cached blocks takes about as long. Kotlin/Native parses a block 1.4 to 1.7 times slower than java
 * except in the smallest map.
 */
public class NameIndexBenchmarkTest {

	private static final int WARMUP_ROUNDS = 3;
	private static final int MEASURED_ROUNDS = 5;
	static final int QUERIES = 20;

	@Test
	public void readingNameIndexes() throws IOException {
		Assume.assumeTrue("set OSMAND_NAME_INDEX_BENCHMARK to run", System.getenv("OSMAND_NAME_INDEX_BENCHMARK") != null);
		System.out.println("### name index, best of " + MEASURED_ROUNDS + " in ms");
		System.out.println(String.format("%-40s %9s %9s %9s %9s %9s %9s %9s %9s", "map", "whole", "copy", "cold",
				"copy", "warm", "copy", "parse", "copy"));
		for (File file : TestObf.files()) {
			benchmark(file);
		}
	}

	private void benchmark(File file) throws IOException {
		BinaryMapIndexReader java = new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file);
		net.osmand.shared.binary.BinaryMapIndexReader copy = new net.osmand.shared.binary.BinaryMapIndexReader(file.getPath());
		List<String> queries = queries(java);
		int indexes = java.getAddressIndexes().size() + java.getPoiIndexes().size();
		long[] best = new long[8];
		Arrays.fill(best, Long.MAX_VALUE);
		long jfound = 0, kfound = 0, jatoms = 0, katoms = 0;
		for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
			long[] t = new long[8];
			jfound = 0;
			kfound = 0;
			jatoms = 0;
			katoms = 0;
			for (int i = 0; i < indexes; i++) {
				long started = System.nanoTime();
				NameIndexReader j = javaReader(java, i);
				java.readFullNameIndex(j);
				t[0] += System.nanoTime() - started;
				started = System.nanoTime();
				net.osmand.shared.binary.NameIndexReader k = copyReader(copy, i);
				copy.readFullNameIndex(k);
				t[1] += System.nanoTime() - started;

				NameIndexReader jq = javaReader(java, i);
				net.osmand.shared.binary.NameIndexReader kq = copyReader(copy, i);
				for (int pass = 0; pass < 2; pass++) {
					started = System.nanoTime();
					for (String q : queries) {
						List<PrefixNameValue> res = java.readFullNameIndex(jq.setQuery(q));
						jfound += res == null ? 0 : res.size();
					}
					t[2 + pass * 2] += System.nanoTime() - started;
					started = System.nanoTime();
					for (String q : queries) {
						List<net.osmand.shared.binary.NameIndexReader.PrefixNameValue> res = copy.readFullNameIndex(kq.setQuery(q));
						kfound += res == null ? 0 : res.size();
					}
					t[3 + pass * 2] += System.nanoTime() - started;
				}

				List<PrefixNameValue> jall = java.readFullNameIndex(j.setQuery("all", NameIndexCompatTest.javaAll()));
				List<net.osmand.shared.binary.NameIndexReader.PrefixNameValue> kall =
						copy.readFullNameIndex(k.setQuery("all", NameIndexCompatTest.copyAll()));
				started = System.nanoTime();
				if (jall != null) {
					for (PrefixNameValue p : jall) {
						AddressNameIndexData a = p.getAddr();
						jatoms += a != null ? a.getAtomCount() : p.getPoi().getAtomsCount();
					}
				}
				t[6] += System.nanoTime() - started;
				started = System.nanoTime();
				if (kall != null) {
					for (net.osmand.shared.binary.NameIndexReader.PrefixNameValue p : kall) {
						net.osmand.shared.binary.AddressNameIndexData a = p.getAddr();
						katoms += a != null ? a.getAtomCount() : p.getPoi().getAtomsCount();
					}
				}
				t[7] += System.nanoTime() - started;
			}
			if (round >= WARMUP_ROUNDS) {
				for (int n = 0; n < t.length; n++) {
					best[n] = Math.min(best[n], t[n]);
				}
			}
		}
		assertEquals("blocks found", jfound, kfound);
		assertEquals("atoms parsed", jatoms, katoms);
		StringBuilder line = new StringBuilder(String.format("%-40s", file.getName()));
		for (long b : best) {
			line.append(String.format(" %9.2f", b / 1e6));
		}
		System.out.println(line + "   blocks found " + jfound + ", atoms " + jatoms + ", words " + queries);
		java.close();
		copy.close();
	}

	private static NameIndexReader javaReader(BinaryMapIndexReader java, int i) {
		return NameIndexCompatTest.javaReader(java, i).setCacheRawBlocks(true);
	}

	private static net.osmand.shared.binary.NameIndexReader copyReader(net.osmand.shared.binary.BinaryMapIndexReader copy, int i) {
		return NameIndexCompatTest.copyReader(copy, i).setCacheRawBlocks(true);
	}

	/**
	 * {@link #QUERIES} words of the map, as {@code NameIndexBenchmarkTest.kt} picks them: the words of
	 * three letters or more of the names in its name indexes, or of the keys of a map from before the
	 * suffix dictionaries, in the order of the files, evenly spread.
	 */
	static List<String> queries(BinaryMapIndexReader java) throws IOException {
		Set<String> words = new LinkedHashSet<>();
		int indexes = java.getAddressIndexes().size() + java.getPoiIndexes().size();
		for (int i = 0; i < indexes; i++) {
			NameIndexReader j = javaReader(java, i);
			List<PrefixNameValue> all = java.readFullNameIndex(j.setQuery("all", NameIndexCompatTest.javaAll()));
			if (all == null) {
				continue;
			}
			all = new ArrayList<>(all);
			all.sort(Comparator.comparingLong(p -> p.shift));
			for (PrefixNameValue p : all) {
				AddressNameIndexData a = p.getAddr();
				OsmAndPoiNameIndexData d = p.getPoi();
				List<String> dictionary = a != null ? a.getSuffixesDictionaryList() : d.getSuffixesDictionaryList();
				if (dictionary.isEmpty() && p.key.length() >= 3 && p.key.indexOf(' ') == -1) {
					// a map from before the suffix dictionaries: the keys are the words
					words.add(p.key);
				}
				String suffix = null;
				for (String s : dictionary) {
					suffix = SearchAlgorithms.nameIndexDecodeDictionarySuffix(suffix, s);
					String name = p.key + suffix;
					if (name.length() >= 3 && name.indexOf(' ') == -1) {
						words.add(name);
					}
				}
			}
		}
		List<String> list = new ArrayList<>(words);
		List<String> res = new ArrayList<>();
		for (int n = 0; n < QUERIES && n < list.size(); n++) {
			res.add(list.get((int) ((long) n * list.size() / Math.min(QUERIES, list.size()))));
		}
		return res;
	}
}
