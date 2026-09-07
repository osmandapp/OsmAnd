package net.osmand.shared.util.collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KTLongHashSetTest {

	@Test
	fun testAddContainsRemove() {
		val set = KTLongHashSet()
		assertTrue(set.isEmpty())
		assertFalse(5L in set)

		assertTrue(set.add(5L))
		assertFalse(set.add(5L))
		assertEquals(1, set.size)
		assertTrue(5L in set)

		assertFalse(set.remove(6L))
		assertTrue(set.remove(5L))
		assertEquals(0, set.size)
		assertFalse(5L in set)
	}

	@Test
	fun testExtremeKeys() {
		val set = KTLongHashSet()
		val keys = longArrayOf(0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE)
		for (key in keys) {
			assertTrue(set.add(key))
		}
		assertEquals(keys.size, set.size)
		for (key in keys) {
			assertTrue(key in set)
		}
	}

	@Test
	fun testGrowth() {
		val set = KTLongHashSet(4)
		val count = 10_000
		for (i in 0 until count) {
			set.add(i.toLong() shl 7)
		}
		assertEquals(count, set.size)
		for (i in 0 until count) {
			assertTrue((i.toLong() shl 7) in set)
		}
		assertFalse(1L in set)
	}

	@Test
	fun testAddAllAndToArray() {
		val set = KTLongHashSet()
		assertTrue(set.addAll(longArrayOf(1L, 2L, 3L, 2L)))
		assertEquals(3, set.size)
		assertFalse(set.addAll(longArrayOf(1L, 2L)))

		val other = KTLongHashSet()
		other.addAll(longArrayOf(3L, 4L))
		assertTrue(set.addAll(other))
		assertEquals(4, set.size)
		assertEquals(listOf(1L, 2L, 3L, 4L), set.toArray().sorted())
	}

	@Test
	fun testIteration() {
		val set = KTLongHashSet()
		for (i in 0 until 50) {
			set.add(i.toLong())
		}
		val byForEach = HashSet<Long>()
		set.forEach { byForEach.add(it) }
		assertEquals(50, byForEach.size)

		val byIterator = HashSet<Long>()
		val it = set.iterator()
		while (it.hasNext()) {
			byIterator.add(it.next())
		}
		assertEquals(byForEach, byIterator)
	}

	@Test
	fun testClear() {
		val set = KTLongHashSet()
		for (i in 0 until 100) {
			set.add(i.toLong())
		}
		set.clear()
		assertEquals(0, set.size)
		assertTrue(set.toArray().isEmpty())
		assertTrue(set.add(1L))
	}

	@Test
	fun testMatchesReferenceModelUnderRandomOps() {
		val set = KTLongHashSet(4)
		val model = HashSet<Long>()
		val random = XorShiftRandom(4242L)
		repeat(60_000) {
			val key = random.nextLong(1500) - 700
			when (random.nextInt(10)) {
				in 0..5 -> assertEquals(model.add(key), set.add(key), "add($key)")
				in 6..7 -> assertEquals(model.remove(key), set.remove(key), "remove($key)")
				else -> assertEquals(model.contains(key), key in set, "contains($key)")
			}
			assertEquals(model.size, set.size)
		}
		assertEquals(model.sorted(), set.toArray().sorted())
	}
}
