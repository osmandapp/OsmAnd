package net.osmand.shared.util.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;

/**
 * Timing comparison of the shared primitive collections against gnu.trove and java.util.BitSet.
 *
 * <b>Disabled on purpose.</b> Measuring takes seconds and the timings are noise in a normal test
 * run, so this class is not part of the suite. It is a tool for when you change a collection, not a
 * regression gate: correctness against the same originals is covered by {@link TroveComparisonTest},
 * which does run.
 *
 * To measure, remove the {@code @Ignore} below, run it, and put the annotation back:
 * <pre>
 * ./gradlew :OsmAnd-java:test --tests "*CollectionsTroveBenchmarkTest" -i
 * </pre>
 *
 * The cross platform half of the same measurement, which is what tells us how Kotlin/Native
 * compares, is in
 * OsmAnd-shared/src/commonTest/kotlin/net/osmand/shared/util/collections/CollectionsBenchmarkTest.kt
 * and is disabled for the same reason.
 *
 * Nothing here asserts a timing. The assertions only check that both implementations computed the
 * same answer, so a slow machine can never turn the build red.
 */
@Ignore("benchmark, run manually")
public class CollectionsTroveBenchmarkTest {

	private static final int ENTRIES = 300_000;
	private static final int LOOKUPS = 600_000;
	private static final int LIST_ENTRIES = 3_000_000;
	private static final int RULE_EVALUATIONS = 300_000;
	private static final int WARMUP_ROUNDS = 3;
	private static final int MEASURED_ROUNDS = 5;

	@Test
	public void benchmarkLongObjectMap() {
		System.out.println();
		System.out.printf("### long -> object map, %d entries, %d lookups%n", ENTRIES, LOOKUPS);
		printHeader();

		for (KeyDistribution distribution : KeyDistribution.values()) {
			long[] keys = distribution.keys(ENTRIES);
			long[] probes = probeKeys(keys, LOOKUPS);
			Object value = new Object();
			long[] checksums = new long[3];

			double shared = measure(() -> {
				KTLongObjectMap<Object> map = new KTLongObjectMap<>(ENTRIES);
				for (long key : keys) {
					map.put(key, value);
				}
				long hits = 0;
				for (long probe : probes) {
					if (map.get(probe) != null) {
						hits++;
					}
				}
				long iterated = 0;
				for (long key : map.keys()) {
					iterated += key;
				}
				checksums[0] = hits * 31 + iterated;
			});

			double trove = measure(() -> {
				TLongObjectHashMap<Object> map = new TLongObjectHashMap<>(ENTRIES);
				for (long key : keys) {
					map.put(key, value);
				}
				long hits = 0;
				for (long probe : probes) {
					if (map.get(probe) != null) {
						hits++;
					}
				}
				long iterated = 0;
				for (long key : map.keys()) {
					iterated += key;
				}
				checksums[1] = hits * 31 + iterated;
			});

			double boxed = measure(() -> {
				HashMap<Long, Object> map = new HashMap<>(ENTRIES * 2);
				for (long key : keys) {
					map.put(key, value);
				}
				long hits = 0;
				for (long probe : probes) {
					if (map.get(probe) != null) {
						hits++;
					}
				}
				long iterated = 0;
				for (long key : map.keySet()) {
					iterated += key;
				}
				checksums[2] = hits * 31 + iterated;
			});

			assertEquals(checksums[1], checksums[0]);
			assertEquals(checksums[1], checksums[2]);
			printRow(distribution.label, "KTLongObjectMap", shared, "TLongObjectHashMap", trove, "HashMap", boxed);
		}
	}

	@Test
	public void benchmarkLongHashSet() {
		System.out.println();
		System.out.printf("### long set, %d entries, %d lookups%n", ENTRIES, LOOKUPS);
		printHeader();

		for (KeyDistribution distribution : KeyDistribution.values()) {
			long[] keys = distribution.keys(ENTRIES);
			long[] probes = probeKeys(keys, LOOKUPS);
			long[] checksums = new long[3];

			double shared = measure(() -> {
				KTLongHashSet set = new KTLongHashSet(ENTRIES);
				for (long key : keys) {
					set.add(key);
				}
				long hits = 0;
				for (long probe : probes) {
					if (set.contains(probe)) {
						hits++;
					}
				}
				checksums[0] = hits;
			});

			double trove = measure(() -> {
				TLongHashSet set = new TLongHashSet(ENTRIES);
				for (long key : keys) {
					set.add(key);
				}
				long hits = 0;
				for (long probe : probes) {
					if (set.contains(probe)) {
						hits++;
					}
				}
				checksums[1] = hits;
			});

			double boxed = measure(() -> {
				HashSet<Long> set = new HashSet<>(ENTRIES * 2);
				for (long key : keys) {
					set.add(key);
				}
				long hits = 0;
				for (long probe : probes) {
					if (set.contains(probe)) {
						hits++;
					}
				}
				checksums[2] = hits;
			});

			assertEquals(checksums[1], checksums[0]);
			assertEquals(checksums[1], checksums[2]);
			printRow(distribution.label, "KTLongHashSet", shared, "TLongHashSet", trove, "HashSet", boxed);
		}
	}

	@Test
	public void benchmarkIntArrayList() {
		System.out.println();
		System.out.printf("### int list, %d appends plus a full scan%n", LIST_ENTRIES);
		printHeader();
		long[] checksums = new long[3];

		double shared = measure(() -> {
			KTIntArrayList list = new KTIntArrayList();
			for (int i = 0; i < LIST_ENTRIES; i++) {
				list.add(i);
			}
			long sum = 0;
			for (int i = 0; i < list.getSize(); i++) {
				sum += list.getQuick(i);
			}
			checksums[0] = sum;
		});

		double trove = measure(() -> {
			TIntArrayList list = new TIntArrayList();
			for (int i = 0; i < LIST_ENTRIES; i++) {
				list.add(i);
			}
			long sum = 0;
			for (int i = 0; i < list.size(); i++) {
				sum += list.getQuick(i);
			}
			checksums[1] = sum;
		});

		double boxed = measure(() -> {
			List<Integer> list = new ArrayList<>();
			for (int i = 0; i < LIST_ENTRIES; i++) {
				list.add(i);
			}
			long sum = 0;
			for (int i = 0; i < list.size(); i++) {
				sum += list.get(i);
			}
			checksums[2] = sum;
		});

		assertEquals(checksums[1], checksums[0]);
		assertEquals(checksums[1], checksums[2]);
		printRow("sequential", "KTIntArrayList", shared, "TIntArrayList", trove, "ArrayList", boxed);
	}

	@Test
	public void benchmarkBitSet() {
		// shaped like GeneralRouter rule evaluation: build a tag mask, intersect it with a road's
		// types, read the first matching bit
		System.out.println();
		System.out.printf("### bit mask evaluation, %d GeneralRouter style rule evaluations%n", RULE_EVALUATIONS);
		printHeader();

		int universe = 512;
		Random random = new Random(20260907L);
		KBitSet[] masks = new KBitSet[64];
		BitSet[] referenceMasks = new BitSet[64];
		for (int i = 0; i < masks.length; i++) {
			masks[i] = new KBitSet(universe);
			referenceMasks[i] = new BitSet(universe);
			for (int k = 0; k < 8; k++) {
				int bit = random.nextInt(universe);
				masks[i].set(bit);
				referenceMasks[i].set(bit);
			}
		}
		KBitSet roadTypes = new KBitSet(universe);
		BitSet referenceRoadTypes = new BitSet(universe);
		for (int k = 0; k < 24; k++) {
			int bit = random.nextInt(universe);
			roadTypes.set(bit);
			referenceRoadTypes.set(bit);
		}
		long[] checksums = new long[2];

		double shared = measure(() -> {
			long checksum = 0;
			KBitSet scratch = new KBitSet(universe);
			for (int i = 0; i < RULE_EVALUATIONS; i++) {
				KBitSet mask = masks[i & 63];
				if (mask.intersects(roadTypes)) {
					scratch.clear();
					scratch.or(mask);
					scratch.and(roadTypes);
					checksum += scratch.nextSetBit(0);
				}
			}
			checksums[0] = checksum;
		});

		double reference = measure(() -> {
			long checksum = 0;
			for (int i = 0; i < RULE_EVALUATIONS; i++) {
				BitSet mask = referenceMasks[i & 63];
				if (mask.intersects(referenceRoadTypes)) {
					BitSet findBit = new BitSet(mask.length());
					findBit.or(mask);
					findBit.and(referenceRoadTypes);
					checksum += findBit.nextSetBit(0);
				}
			}
			checksums[1] = checksum;
		});

		assertEquals(checksums[1], checksums[0]);
		assertTrue(checksums[0] != 0);
		printRow("GeneralRouter shape", "KBitSet", shared, "java.util.BitSet", reference, "-", Double.NaN);
	}

	// ------------------------------------------------------------------ harness

	private enum KeyDistribution {

		/** roadId << 6 | segment, exactly what routing uses as a map key. */
		ROUTING("routing ids"),

		/** Uniformly spread keys, the friendly case for any hash function. */
		RANDOM("random     ");

		final String label;

		KeyDistribution(String label) {
			this.label = label;
		}

		long[] keys(int count) {
			long[] keys = new long[count];
			Random random = new Random(20260907L);
			for (int i = 0; i < count; i++) {
				keys[i] = this == ROUTING
						? ((100_000_000L + i * 17L) << 6) | (i % 48)
						: random.nextLong();
			}
			return keys;
		}
	}

	/** Half of the probes hit an existing key, half miss. */
	private static long[] probeKeys(long[] keys, int count) {
		long[] probes = new long[count];
		Random random = new Random(1234L);
		for (int i = 0; i < count; i++) {
			probes[i] = (i & 1) == 0
					? keys[random.nextInt(keys.length)]
					: random.nextLong() | (1L << 62);
		}
		return probes;
	}

	private static double measure(Runnable block) {
		for (int i = 0; i < WARMUP_ROUNDS; i++) {
			block.run();
		}
		double best = Double.MAX_VALUE;
		for (int i = 0; i < MEASURED_ROUNDS; i++) {
			long start = System.nanoTime();
			block.run();
			double elapsed = (System.nanoTime() - start) / 1_000_000.0;
			if (elapsed < best) {
				best = elapsed;
			}
		}
		return best;
	}

	private static void printHeader() {
		System.out.printf("    best of %d rounds after %d warmup rounds%n", MEASURED_ROUNDS, WARMUP_ROUNDS);
	}

	private static void printRow(String label, String sharedName, double shared,
			String refName, double ref, String boxedName, double boxed) {
		StringBuilder line = new StringBuilder();
		line.append(String.format("  %-22s %-20s %8.2f ms", label, sharedName, shared));
		line.append(String.format("   vs %-20s %8.2f ms (%.2fx)", refName, ref, ref / shared));
		if (!Double.isNaN(boxed)) {
			line.append(String.format("   vs %-10s %8.2f ms (%.2fx)", boxedName, boxed, boxed / shared));
		}
		System.out.println(line);
	}
}
