package net.osmand.shared.util.collections

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KBitSetTest {

	private fun bitsOf(vararg indices: Int): KBitSet {
		val set = KBitSet()
		for (index in indices) {
			set.set(index)
		}
		return set
	}

	@Test
	fun testSetGetClear() {
		val bits = KBitSet()
		assertTrue(bits.isEmpty())
		assertFalse(bits.get(0))
		// reading past the capacity must answer false, not fail
		assertFalse(bits.get(100_000))

		bits.set(5)
		assertTrue(bits.get(5))
		assertFalse(bits.get(4))
		assertEquals(1, bits.cardinality())

		bits.set(5, false)
		assertFalse(bits.get(5))
		bits.set(5, true)
		assertTrue(bits.get(5))
		bits.clear(5)
		assertFalse(bits.get(5))
		assertTrue(bits.isEmpty())
	}

	@Test
	fun testGrowsOnDemandAcrossWords() {
		val bits = KBitSet(1)
		val indices = intArrayOf(0, 1, 63, 64, 65, 127, 128, 4095)
		for (index in indices) {
			bits.set(index)
		}
		assertEquals(indices.size, bits.cardinality())
		for (index in indices) {
			assertTrue(bits.get(index), "bit $index")
		}
		assertFalse(bits.get(62))
		assertEquals(4096, bits.length())
		assertContentEquals(indices, bits.toIntArray())
	}

	@Test
	fun testLengthAndSize() {
		val bits = KBitSet()
		assertEquals(0, bits.length())
		bits.set(0)
		assertEquals(1, bits.length())
		bits.set(70)
		assertEquals(71, bits.length())
		bits.clear(70)
		assertEquals(1, bits.length())
		assertTrue(bits.size() >= 128)
		assertEquals(0, bits.size() % KBitSet.BITS_PER_WORD)
	}

	@Test
	fun testNextSetBitAndNextClearBit() {
		val bits = bitsOf(3, 64, 65, 200)
		assertEquals(3, bits.nextSetBit(0))
		assertEquals(3, bits.nextSetBit(3))
		assertEquals(64, bits.nextSetBit(4))
		assertEquals(65, bits.nextSetBit(65))
		assertEquals(200, bits.nextSetBit(66))
		assertEquals(-1, bits.nextSetBit(201))
		assertEquals(-1, bits.nextSetBit(100_000))

		assertEquals(0, bits.nextClearBit(0))
		assertEquals(4, bits.nextClearBit(3))
		assertEquals(66, bits.nextClearBit(64))
	}

	@Test
	fun testBinaryOperations() {
		val a = bitsOf(1, 2, 3, 100)
		val b = bitsOf(2, 3, 4)

		assertTrue(a.intersects(b))
		assertFalse(bitsOf(1).intersects(bitsOf(2)))
		// intersection must be found even when only the longer set has the bit range
		assertTrue(bitsOf(200).intersects(bitsOf(200)))
		assertFalse(bitsOf(200).intersects(bitsOf(1)))

		val or = a.copy()
		or.or(b)
		assertContentEquals(intArrayOf(1, 2, 3, 4, 100), or.toIntArray())

		val and = a.copy()
		and.and(b)
		assertContentEquals(intArrayOf(2, 3), and.toIntArray())

		val andNot = a.copy()
		andNot.andNot(b)
		assertContentEquals(intArrayOf(1, 100), andNot.toIntArray())

		val xor = a.copy()
		xor.xor(b)
		assertContentEquals(intArrayOf(1, 4, 100), xor.toIntArray())

		val self = a.copy()
		self.and(self)
		assertContentEquals(a.toIntArray(), self.toIntArray())
		self.or(self)
		assertContentEquals(a.toIntArray(), self.toIntArray())
	}

	@Test
	fun testAndClearsBitsBeyondTheOtherSet() {
		val a = bitsOf(1, 500)
		val b = bitsOf(1)
		a.and(b)
		assertContentEquals(intArrayOf(1), a.toIntArray())
	}

	@Test
	fun testEqualsIgnoresCapacity() {
		val small = KBitSet(8)
		val large = KBitSet(4096)
		small.set(3)
		large.set(3)
		assertEquals(small, large)
		assertEquals(small.hashCode(), large.hashCode())

		large.set(1000)
		assertFalse(small == large)
		large.clear(1000)
		assertEquals(small, large)

		assertEquals(KBitSet(8), KBitSet(4096))
		assertEquals(KBitSet(8).hashCode(), KBitSet(4096).hashCode())
	}

	@Test
	fun testGeneralRouterEvaluationShape() {
		// mirrors GeneralRouter: mask over the universal rule table, intersected with a road's types
		val tagRuleMask = bitsOf(4, 9, 17)
		val roadTypes = bitsOf(2, 9, 30)

		assertTrue(tagRuleMask.intersects(roadTypes))
		val findBit = KBitSet(tagRuleMask.length())
		findBit.or(tagRuleMask)
		findBit.and(roadTypes)
		assertEquals(9, findBit.nextSetBit(0))

		// checkAllTypesShouldBePresent: filterTypes must be a subset of types
		val filterTypes = bitsOf(2, 9)
		val eval = KBitSet()
		eval.or(filterTypes)
		eval.and(roadTypes)
		assertEquals(filterTypes, eval)

		val missing = bitsOf(2, 9, 11)
		val eval2 = KBitSet()
		eval2.or(missing)
		eval2.and(roadTypes)
		assertFalse(missing == eval2)
	}

	@Test
	fun testNegativeIndexRejected() {
		val bits = KBitSet()
		assertFailsWith<IndexOutOfBoundsException> { bits.set(-1) }
		assertFailsWith<IndexOutOfBoundsException> { bits.get(-1) }
		assertFailsWith<IndexOutOfBoundsException> { bits.nextSetBit(-1) }
	}

	@Test
	fun testMatchesReferenceModelUnderRandomOps() {
		val bits = KBitSet(1)
		val model = HashSet<Int>()
		val random = XorShiftRandom(31337L)
		repeat(50_000) {
			val index = random.nextInt(3000)
			when (random.nextInt(8)) {
				in 0..4 -> {
					bits.set(index)
					model.add(index)
				}
				in 5..6 -> {
					bits.clear(index)
					model.remove(index)
				}
				else -> assertEquals(model.contains(index), bits.get(index))
			}
		}
		assertEquals(model.size, bits.cardinality())
		assertContentEquals(model.sorted().toIntArray(), bits.toIntArray())
		assertEquals((model.maxOrNull() ?: -1) + 1, bits.length())

		var visited = 0
		bits.forEachSetBit { index ->
			assertTrue(index in model)
			visited++
		}
		assertEquals(model.size, visited)
	}
}
