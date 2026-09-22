package net.osmand.shared.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KQuadTreeTest {

	private fun world() = KQuadRect(0.0, 0.0, 1000.0, 1000.0)

	private fun boxAt(x: Double, y: Double, size: Double = 1.0) =
		KQuadRect(x, y, x + size, y + size)

	@Test
	fun testInsertAndQuery() {
		val tree = KQuadTree<String>(world(), 8, 0.55f)
		tree.insert("a", boxAt(10.0, 10.0))
		tree.insert("b", boxAt(900.0, 900.0))
		tree.insert("c", boxAt(500.0, 500.0))

		val result = ArrayList<String>()
		tree.queryInBox(KQuadRect(0.0, 0.0, 100.0, 100.0), result)
		assertTrue(result.contains("a"))
		assertFalse(result.contains("b"))

		tree.queryInBox(KQuadRect(0.0, 0.0, 1000.0, 1000.0), result)
		assertEquals(3, result.size)
	}

	@Test
	fun testQueryReusesAndClearsTheResultList() {
		val tree = KQuadTree<String>(world())
		tree.insert("a", boxAt(10.0, 10.0))
		val result = ArrayList<String>()
		result.add("stale")
		tree.queryInBox(KQuadRect(0.0, 0.0, 100.0, 100.0), result)
		assertEquals(listOf("a"), result)
	}

	@Test
	fun testPointInsert() {
		val tree = KQuadTree<Int>(world())
		tree.insert(1, 100f, 100f)
		tree.insert(2, 800f, 800f)
		val result = ArrayList<Int>()
		tree.queryInBox(KQuadRect(90.0, 90.0, 110.0, 110.0), result)
		assertTrue(result.contains(1))
		assertFalse(result.contains(2))
	}

	@Test
	fun testEveryInsertedItemIsFoundByAFullQuery() {
		val tree = KQuadTree<Int>(world(), 6, 0.55f)
		val random = net.osmand.shared.util.collections.XorShiftRandom(9001L)
		val count = 2000
		for (i in 0 until count) {
			val x = random.nextInt(1000).toDouble()
			val y = random.nextInt(1000).toDouble()
			tree.insert(i, boxAt(x, y, 2.0))
		}
		val result = ArrayList<Int>()
		tree.queryInBox(world(), result)
		assertEquals(count, result.size)
		assertEquals(count, result.toSet().size)
	}

	@Test
	fun testQueryNeverMissesAnItemThatIntersects() {
		val tree = KQuadTree<Int>(world(), 8, 0.55f)
		val random = net.osmand.shared.util.collections.XorShiftRandom(13L)
		val boxes = ArrayList<KQuadRect>()
		for (i in 0 until 500) {
			val x = random.nextInt(980).toDouble()
			val y = random.nextInt(980).toDouble()
			val box = boxAt(x, y, 5.0)
			boxes.add(box)
			tree.insert(i, box)
		}
		repeat(50) {
			val qx = random.nextInt(900).toDouble()
			val qy = random.nextInt(900).toDouble()
			val query = KQuadRect(qx, qy, qx + 80.0, qy + 80.0)

			val expected = boxes.indices.filter { KQuadRect.intersects(query, boxes[it]) }.toSet()
			val actual = ArrayList<Int>()
			tree.queryInBox(query, actual)
			// the tree answers by node bounds, so it may over report but must never under report
			assertTrue(actual.toSet().containsAll(expected), "missed ${expected - actual.toSet()}")
		}
	}

	@Test
	fun testClear() {
		val tree = KQuadTree<Int>(world())
		for (i in 0 until 100) {
			tree.insert(i, boxAt(i.toDouble(), i.toDouble()))
		}
		tree.clear()
		val result = ArrayList<Int>()
		tree.queryInBox(world(), result)
		assertTrue(result.isEmpty())
	}

	@Test
	fun testCheckIntersection() {
		val tree = KQuadTree<KQuadRect>(world(), 8, 0.55f)
		val item = boxAt(500.0, 500.0, 10.0)
		tree.insert(item, item)

		val contains = KQuadTree.KQuadTreeItemInQuadRect<KQuadRect> { rect, value ->
			KQuadRect.intersects(rect, value)
		}
		assertTrue(tree.checkIntersection(KQuadRect(495.0, 495.0, 505.0, 505.0), -1, contains))
		assertFalse(tree.checkIntersection(KQuadRect(0.0, 0.0, 10.0, 10.0), -1, contains))

		// a hint depth beyond maxDepth degrades to the precise check instead of failing
		assertTrue(tree.checkIntersection(KQuadRect(495.0, 495.0, 505.0, 505.0), 99, contains))
	}
}
