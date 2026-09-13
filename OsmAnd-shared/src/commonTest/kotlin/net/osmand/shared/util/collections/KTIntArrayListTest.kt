package net.osmand.shared.util.collections

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KTIntArrayListTest {

	@Test
	fun testAddAndGet() {
		val list = KTIntArrayList()
		assertTrue(list.isEmpty())
		for (i in 0 until 100) {
			list.add(i)
		}
		assertEquals(100, list.size)
		for (i in 0 until 100) {
			assertEquals(i, list[i])
			assertEquals(i, list.getQuick(i))
		}
	}

	@Test
	fun testSizeIsReachableBothWays() {
		// Java code migrating off TIntArrayList keeps calling size()
		val list = KTIntArrayList(intArrayOf(1, 2, 3))
		assertEquals(3, list.size)
		assertEquals(list.size, list.size())
		list.add(4)
		assertEquals(4, list.size())
	}

	@Test
	fun testRemoveRange() {
		val list = KTIntArrayList(intArrayOf(1, 2, 3, 4, 5))
		list.remove(1, 2)
		assertContentEquals(intArrayOf(1, 4, 5), list.toArray())
		list.remove(2, 1)
		assertContentEquals(intArrayOf(1, 4), list.toArray())
		list.remove(0, 0)
		assertContentEquals(intArrayOf(1, 4), list.toArray())
		list.remove(0, 2)
		assertTrue(list.isEmpty())
		assertFailsWith<IllegalArgumentException> { list.remove(0, 1) }
	}

	@Test
	fun testFill() {
		val list = KTIntArrayList(intArrayOf(1, 2, 3))
		list.fill(7)
		assertContentEquals(intArrayOf(7, 7, 7), list.toArray())

		list.fill(1, 3, 9)
		assertContentEquals(intArrayOf(7, 9, 9), list.toArray())

		// filling past the end grows the list, like TIntArrayList.fill
		list.fill(3, 6, 4)
		assertContentEquals(intArrayOf(7, 9, 9, 4, 4, 4), list.toArray())
		assertFailsWith<IllegalArgumentException> { list.fill(3, 1, 0) }
	}

	@Test
	fun testAddAllArray() {
		val list = KTIntArrayList(intArrayOf(1))
		list.addAll(intArrayOf(2, 3))
		assertContentEquals(intArrayOf(1, 2, 3), list.toArray())
	}

	@Test
	fun testIterator() {
		val list = KTIntArrayList(intArrayOf(4, 8, 15))
		val seen = ArrayList<Int>()
		val it = list.iterator()
		while (it.hasNext()) {
			seen.add(it.next())
		}
		assertEquals(listOf(4, 8, 15), seen)
		assertFailsWith<NoSuchElementException> { it.next() }
		assertFalse(KTIntArrayList().iterator().hasNext())
	}

	@Test
	fun testBoundsChecked() {
		val list = KTIntArrayList()
		list.add(1)
		assertFailsWith<IndexOutOfBoundsException> { list[1] }
		assertFailsWith<IndexOutOfBoundsException> { list[-1] }
		assertFailsWith<IndexOutOfBoundsException> { list[0] = 5; list[3] = 5 }
	}

	@Test
	fun testAddArrayAndRange() {
		val list = KTIntArrayList()
		list.add(intArrayOf(1, 2, 3))
		list.add(intArrayOf(4, 5, 6, 7), 1, 2)
		assertContentEquals(intArrayOf(1, 2, 3, 5, 6), list.toArray())
		assertFailsWith<IllegalArgumentException> { list.add(intArrayOf(1), 0, 5) }
	}

	@Test
	fun testAddAll() {
		val a = KTIntArrayList(intArrayOf(1, 2))
		val b = KTIntArrayList(intArrayOf(3, 4))
		a.addAll(b)
		assertContentEquals(intArrayOf(1, 2, 3, 4), a.toArray())
		assertEquals(2, b.size)
	}

	@Test
	fun testInsertAndRemove() {
		val list = KTIntArrayList(intArrayOf(1, 2, 4))
		list.insert(2, 3)
		assertContentEquals(intArrayOf(1, 2, 3, 4), list.toArray())
		list.insert(4, 5)
		assertContentEquals(intArrayOf(1, 2, 3, 4, 5), list.toArray())
		assertEquals(3, list.removeAt(2))
		assertContentEquals(intArrayOf(1, 2, 4, 5), list.toArray())
		assertTrue(list.removeValue(5))
		assertFalse(list.removeValue(99))
		assertContentEquals(intArrayOf(1, 2, 4), list.toArray())
	}

	@Test
	fun testClearVsReset() {
		val list = KTIntArrayList(intArrayOf(1, 2, 3))
		val capacity = list.capacity()
		list.clear()
		assertEquals(0, list.size)
		assertEquals(capacity, list.capacity())
		list.add(9)
		assertEquals(9, list[0])

		val other = KTIntArrayList(intArrayOf(1, 2, 3))
		other.reset()
		assertEquals(0, other.size)
		other.add(7)
		assertContentEquals(intArrayOf(7), other.toArray())
	}

	@Test
	fun testSortKeepsPayloadOnly() {
		val list = KTIntArrayList(16)
		list.add(intArrayOf(5, 3, 9, 1))
		list.sort()
		assertContentEquals(intArrayOf(1, 3, 5, 9), list.toArray())
		assertEquals(4, list.size)
		// the unused tail must not have been pulled into the payload
		list.add(0)
		assertContentEquals(intArrayOf(1, 3, 5, 9, 0), list.toArray())
	}

	@Test
	fun testMiscOperations() {
		val list = KTIntArrayList(intArrayOf(4, 8, 15, 16, 23, 42))
		assertTrue(15 in list)
		assertFalse(7 in list)
		assertEquals(2, list.indexOf(15))
		assertEquals(-1, list.indexOf(7))
		assertContentEquals(intArrayOf(8, 15), list.toArray(1, 2))
		assertEquals(108L, list.sum())
		list.reverse()
		assertContentEquals(intArrayOf(42, 23, 16, 15, 8, 4), list.toArray())

		var sum = 0
		list.forEach { sum += it }
		assertEquals(108, sum)

		list.trimToSize()
		assertEquals(list.size, list.capacity())
	}

	@Test
	fun testEqualsAndHashCode() {
		val a = KTIntArrayList(intArrayOf(1, 2, 3))
		val b = KTIntArrayList(64)
		b.add(intArrayOf(1, 2, 3))
		assertEquals(a, b)
		assertEquals(a.hashCode(), b.hashCode())
		b.add(4)
		assertFalse(a == b)
		assertEquals("[1, 2, 3]", a.toString())
	}

	@Test
	fun testMatchesReferenceModelUnderRandomOps() {
		val list = KTIntArrayList(2)
		val model = ArrayList<Int>()
		val random = XorShiftRandom(777L)
		repeat(40_000) { step ->
			when (random.nextInt(10)) {
				in 0..6 -> {
					list.add(step)
					model.add(step)
				}
				7 -> if (model.isNotEmpty()) {
					val index = random.nextInt(model.size)
					assertEquals(model.removeAt(index), list.removeAt(index))
				}
				8 -> if (model.isNotEmpty()) {
					val index = random.nextInt(model.size)
					model[index] = step
					list[index] = step
				}
				else -> if (model.isNotEmpty()) {
					val index = random.nextInt(model.size)
					assertEquals(model[index], list[index])
				}
			}
			assertEquals(model.size, list.size)
		}
		assertContentEquals(model.toIntArray(), list.toArray())
	}
}
