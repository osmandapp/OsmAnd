package net.osmand.shared.util.collections;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

import org.junit.Test;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;

/**
 * Differential tests of the shared primitive collections against the originals they replace:
 * gnu.trove for the maps, sets and lists, java.util.BitSet for {@link KBitSet}.
 *
 * These live in OsmAnd-java because that is the only module where trove and OsmAnd-shared are both
 * on the classpath. They are cheap and run as part of the normal suite; the timing comparison is a
 * separate, disabled class, see {@link CollectionsTroveBenchmarkTest}.
 *
 * Every test drives both implementations through the same pseudo random operation stream and
 * asserts they stay in step, which is what makes a drop in replacement safe to swap in.
 */
public class TroveComparisonTest {

	private static final int OPERATIONS = 200_000;

	@Test
	public void testLongObjectMapMatchesTrove() {
		KTLongObjectMap<Integer> shared = new KTLongObjectMap<>();
		TLongObjectHashMap<Integer> trove = new TLongObjectHashMap<>();
		Random random = new Random(20260907L);

		for (int step = 0; step < OPERATIONS; step++) {
			long key = random.nextInt(4000) - 2000;
			int op = random.nextInt(10);
			if (op <= 5) {
				assertEquals("put(" + key + ")", trove.put(key, step), shared.put(key, step));
			} else if (op <= 7) {
				assertEquals("remove(" + key + ")", trove.remove(key), shared.remove(key));
			} else if (op == 8) {
				assertEquals("get(" + key + ")", trove.get(key), shared.get(key));
			} else {
				assertEquals("containsKey(" + key + ")", trove.containsKey(key), shared.containsKey(key));
			}
			assertEquals(trove.size(), shared.getSize());
		}

		long[] troveKeys = trove.keys();
		long[] sharedKeys = shared.keys();
		Arrays.sort(troveKeys);
		Arrays.sort(sharedKeys);
		assertArrayEquals(troveKeys, sharedKeys);
		for (long key : troveKeys) {
			assertEquals(trove.get(key), shared.get(key));
		}
	}

	@Test
	public void testLongHashSetMatchesTrove() {
		KTLongHashSet shared = new KTLongHashSet();
		TLongHashSet trove = new TLongHashSet();
		Random random = new Random(4242L);

		for (int step = 0; step < OPERATIONS; step++) {
			long key = random.nextInt(3000) - 1500;
			int op = random.nextInt(10);
			if (op <= 5) {
				assertEquals("add(" + key + ")", trove.add(key), shared.add(key));
			} else if (op <= 7) {
				assertEquals("remove(" + key + ")", trove.remove(key), shared.remove(key));
			} else {
				assertEquals("contains(" + key + ")", trove.contains(key), shared.contains(key));
			}
			assertEquals(trove.size(), shared.getSize());
		}

		long[] troveKeys = trove.toArray();
		long[] sharedKeys = shared.toArray();
		Arrays.sort(troveKeys);
		Arrays.sort(sharedKeys);
		assertArrayEquals(troveKeys, sharedKeys);
	}

	@Test
	public void testIntArrayListMatchesTrove() {
		KTIntArrayList shared = new KTIntArrayList();
		TIntArrayList trove = new TIntArrayList();
		Random random = new Random(777L);

		for (int step = 0; step < OPERATIONS; step++) {
			int op = random.nextInt(10);
			if (op <= 6) {
				shared.add(step);
				trove.add(step);
			} else if (op == 7 && !trove.isEmpty()) {
				int index = random.nextInt(trove.size());
				assertEquals(trove.removeAt(index), shared.removeAt(index));
			} else if (op == 8 && !trove.isEmpty()) {
				int index = random.nextInt(trove.size());
				trove.set(index, step);
				shared.set(index, step);
			} else if (!trove.isEmpty()) {
				int index = random.nextInt(trove.size());
				assertEquals(trove.get(index), shared.get(index));
			}
			assertEquals(trove.size(), shared.getSize());
		}
		assertArrayEquals(trove.toArray(), shared.toArray());
	}

	@Test
	public void testBitSetMatchesJavaBitSet() {
		KBitSet shared = new KBitSet(1);
		BitSet reference = new BitSet();
		Random random = new Random(31337L);

		for (int step = 0; step < OPERATIONS; step++) {
			int index = random.nextInt(4000);
			int op = random.nextInt(8);
			if (op <= 4) {
				shared.set(index);
				reference.set(index);
			} else if (op <= 6) {
				shared.clear(index);
				reference.clear(index);
			} else {
				assertEquals("get(" + index + ")", reference.get(index), shared.get(index));
			}
		}

		assertEquals(reference.cardinality(), shared.cardinality());
		assertEquals(reference.length(), shared.length());
		assertEquals(reference.isEmpty(), shared.isEmpty());
		for (int bit = reference.nextSetBit(0), mirrored = shared.nextSetBit(0);
				bit >= 0;
				bit = reference.nextSetBit(bit + 1), mirrored = shared.nextSetBit(mirrored + 1)) {
			assertEquals(bit, mirrored);
		}

		// binary operations and the capacity independent equality that GeneralRouter relies on
		KBitSet sharedOther = new KBitSet(1);
		BitSet referenceOther = new BitSet();
		for (int i = 0; i < 2000; i++) {
			int index = random.nextInt(4000);
			sharedOther.set(index);
			referenceOther.set(index);
		}
		assertEquals(referenceOther.intersects(reference), sharedOther.intersects(shared));

		KBitSet sharedAnd = shared.copy();
		sharedAnd.and(sharedOther);
		BitSet referenceAnd = (BitSet) reference.clone();
		referenceAnd.and(referenceOther);
		assertArrayEquals(referenceAnd.stream().toArray(), sharedAnd.toIntArray());

		KBitSet sharedOr = shared.copy();
		sharedOr.or(sharedOther);
		BitSet referenceOr = (BitSet) reference.clone();
		referenceOr.or(referenceOther);
		assertArrayEquals(referenceOr.stream().toArray(), sharedOr.toIntArray());

		KBitSet wide = new KBitSet(4096);
		KBitSet narrow = new KBitSet(1);
		wide.set(7);
		narrow.set(7);
		assertEquals(wide, narrow);
		assertEquals(wide.hashCode(), narrow.hashCode());
		BitSet referenceWide = new BitSet(4096);
		BitSet referenceNarrow = new BitSet(1);
		referenceWide.set(7);
		referenceNarrow.set(7);
		assertEquals(referenceWide.hashCode(), wide.hashCode());
		assertEquals(referenceNarrow.hashCode(), narrow.hashCode());
	}

	@Test
	public void testConstructorsAreUsableFromJava() {
		// @JvmOverloads must keep the no-arg form available for the Java routing code
		assertEquals(0, new KTLongObjectMap<String>().getSize());
		assertEquals(0, new KTLongHashSet().getSize());
		assertEquals(0, new KTIntArrayList().getSize());
		assertFalse(new KBitSet().get(0));
	}
}
