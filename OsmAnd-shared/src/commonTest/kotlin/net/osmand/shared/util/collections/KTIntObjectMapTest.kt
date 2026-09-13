package net.osmand.shared.util.collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KTIntObjectMapTest {

	@Test
	fun testSizeIsReachableBothWays() {
		// Java code migrating off trove keeps calling size()
		val collection = KTIntObjectMap<Int>()
		assertEquals(0, collection.size())
		assertEquals(collection.size, collection.size())
	}

	@Test
	fun testPutGetRemove() {
		val map = KTIntObjectMap<String>()
		assertTrue(map.isEmpty())
		assertNull(map[1])

		assertNull(map.put(1, "one"))
		assertEquals("one", map.put(1, "ONE"))
		assertEquals("ONE", map[1])
		assertEquals(1, map.size)
		assertTrue(map.containsKey(1))

		assertNull(map.remove(2))
		assertEquals("ONE", map.remove(1))
		assertEquals(0, map.size)
		assertFalse(map.containsKey(1))
		assertNull(map[1])
	}

	@Test
	fun testNegativeAndExtremeKeys() {
		val map = KTIntObjectMap<String>()
		val keys = intArrayOf(0, -1, Int.MIN_VALUE, Int.MAX_VALUE, -42, 42)
		for (key in keys) {
			map.put(key, "v$key")
		}
		assertEquals(keys.size, map.size)
		for (key in keys) {
			assertEquals("v$key", map[key])
		}
	}

	@Test
	fun testGrowthKeepsEveryEntry() {
		val map = KTIntObjectMap<Int>(4)
		val count = 5000
		for (i in 0 until count) {
			map.put(i * 31, i)
		}
		assertEquals(count, map.size)
		for (i in 0 until count) {
			assertEquals(i, map[i * 31])
		}
		assertNull(map[1])
	}

	@Test
	fun testTombstonesAreReused() {
		val map = KTIntObjectMap<Int>(8)
		// churn far beyond the table size, tombstones must not fill the table up
		for (round in 0 until 200) {
			for (i in 0 until 50) {
				map.put(round * 50 + i, i)
			}
			for (i in 0 until 50) {
				map.remove(round * 50 + i)
			}
		}
		assertEquals(0, map.size)
		map.put(7, 7)
		assertEquals(7, map[7])
		assertEquals(1, map.size)
	}

	@Test
	fun testRemovedKeyDoesNotBreakProbeChain() {
		// keys chosen to be inserted, then removed from the middle of a chain
		val map = KTIntObjectMap<Int>(8)
		for (i in 0 until 12) {
			map.put(i, i)
		}
		map.remove(5)
		map.remove(6)
		for (i in 0 until 12) {
			if (i == 5 || i == 6) {
				assertNull(map[i], "key $i must be gone")
			} else {
				assertEquals(i, map[i], "key $i must still resolve")
			}
		}
	}

	@Test
	fun testClear() {
		val map = KTIntObjectMap<Int>()
		for (i in 0 until 100) {
			map.put(i, i)
		}
		map.clear()
		assertEquals(0, map.size)
		assertTrue(map.isEmpty())
		assertTrue(map.keys().isEmpty())
		assertTrue(map.values().isEmpty())
		map.put(1, 1)
		assertEquals(1, map[1])
	}

	@Test
	fun testPutIfAbsentAndGetOrPut() {
		val map = KTIntObjectMap<String>()
		assertEquals("a", map.putIfAbsent(1, "a"))
		assertEquals("a", map.putIfAbsent(1, "b"))
		assertEquals("a", map[1])

		var created = 0
		assertEquals("a", map.getOrPut(1) { created++; "z" })
		assertEquals(0, created)
		assertEquals("c", map.getOrPut(2) { created++; "c" })
		assertEquals(1, created)
		assertEquals("c", map[2])
		assertEquals(2, map.size)
	}

	@Test
	fun testKeysAndValues() {
		val map = KTIntObjectMap<Int>()
		for (i in 0 until 64) {
			map.put(i, i * 2)
		}
		map.remove(10)
		assertEquals(63, map.keys().size)
		assertEquals(63, map.values().size)
		assertEquals(map.values().sorted(), (0 until 64).filter { it != 10 }.map { it * 2 })
		assertEquals(map.keys().sorted(), (0 until 64).filter { it != 10 })
		assertEquals(map.values(), map.valueCollection())
	}

	@Test
	fun testIteration() {
		val map = KTIntObjectMap<Int>()
		for (i in 0 until 100) {
			map.put(i, i)
		}
		val seenByForEach = HashMap<Int, Int>()
		map.forEach { key, value -> seenByForEach[key] = value }
		assertEquals(100, seenByForEach.size)

		val seenByIterator = HashMap<Int, Int>()
		val it = map.iterator()
		while (it.hasNext()) {
			it.advance()
			seenByIterator[it.key()] = it.value()
		}
		assertEquals(seenByForEach, seenByIterator)

		var sum = 0
		map.forEachValue { sum += it }
		assertEquals((0 until 100).sum(), sum)
	}

	@Test
	fun testIterationFailsFastOnModification() {
		val map = KTIntObjectMap<Int>()
		for (i in 0 until 10) {
			map.put(i, i)
		}
		assertFailsWith<IllegalStateException> {
			map.forEach { key, _ -> map.put(key + 1000, 0) }
		}
		val map2 = KTIntObjectMap<Int>()
		for (i in 0 until 10) {
			map2.put(i, i)
		}
		assertFailsWith<IllegalStateException> {
			val it = map2.iterator()
			it.advance()
			map2.remove(9)
			it.advance()
		}
	}

	@Test
	fun testMatchesReferenceModelUnderRandomOps() {
		val map = KTIntObjectMap<Int>(4)
		val model = HashMap<Int, Int>()
		val random = XorShiftRandom(20260908L)
		repeat(60_000) { step ->
			// small key space, so collisions, overwrites and re-inserts all get exercised
			val key = random.nextInt(2000) - 1000
			when (random.nextInt(10)) {
				in 0..5 -> {
					assertEquals(model.put(key, step), map.put(key, step), "put($key)")
				}
				in 6..7 -> {
					assertEquals(model.remove(key), map.remove(key), "remove($key)")
				}
				8 -> {
					assertEquals(model[key], map[key], "get($key)")
				}
				else -> {
					assertEquals(model.containsKey(key), map.containsKey(key), "containsKey($key)")
				}
			}
			assertEquals(model.size, map.size)
		}
		assertEquals(model.size, map.size)
		for ((key, value) in model) {
			assertEquals(value, map[key])
		}
		val fromMap = HashMap<Int, Int>()
		map.forEach { key, value -> fromMap[key] = value }
		assertEquals(model, fromMap)
	}
}
