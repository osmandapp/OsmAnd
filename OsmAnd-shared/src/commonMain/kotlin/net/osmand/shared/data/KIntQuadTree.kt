package net.osmand.shared.data

import net.osmand.shared.util.collections.KTIntArrayList

/**
 * [KQuadTree] for `Int` items: the same nodes, the same insertion and the same query order, with
 * the items kept in a [KTIntArrayList] per node instead of a boxed list, and a query that takes
 * its box as four numbers and answers into a [KTIntArrayList].
 *
 * The route planner's heuristic asks [PrecalculatedRouteDirection][net.osmand.shared.routing.PrecalculatedRouteDirection]
 * for the nearest point of the previous route on every segment it settles; with the generic tree
 * that was a `KQuadRect` and a list of boxed `Int` per call, a tenth of the calculation on
 * Kotlin/Native. Because the traversal is the generic tree's, the candidates come out in the same
 * order, and the nearest of them is the same point java's `QuadTree<Integer>` finds.
 */
class KIntQuadTree(
	left: Double, top: Double, right: Double, bottom: Double,
	private val maxDepth: Int = 8,
	private val ratio: Float = 0.55f
) {

	private class Node(val left: Double, val top: Double, val right: Double, val bottom: Double) {
		var data: KTIntArrayList? = null
		val children: Array<Node?> = arrayOfNulls(4)
	}

	private val root = Node(left, top, right, bottom)

	/** Inserts [data] at a point; the coordinates are narrowed to float first, as `QuadTree.insert(T, float, float)` does. */
	fun insert(data: Int, x: Float, y: Float) {
		val v = x.toDouble()
		val w = y.toDouble()
		doInsertData(data, v, w, v, w, root, 0)
	}

	/** Fills [result] with every item whose node bounds intersect the box and returns it. */
	fun queryInBox(left: Double, top: Double, right: Double, bottom: Double, result: KTIntArrayList): KTIntArrayList {
		result.clear()
		queryNode(left, top, right, bottom, result, root)
		return result
	}

	private fun queryNode(left: Double, top: Double, right: Double, bottom: Double, result: KTIntArrayList, node: Node?) {
		if (node != null && intersects(left, top, right, bottom, node)) {
			node.data?.let { result.addAll(it) }
			for (child in node.children) {
				queryNode(left, top, right, bottom, result, child)
			}
		}
	}

	private fun doInsertData(data: Int, left: Double, top: Double, right: Double, bottom: Double, node: Node, currentDepth: Int) {
		val depth = currentDepth + 1
		if (depth < maxDepth) {
			val lx = node.left
			val ly = node.top
			val hx = node.right
			val hy = node.bottom
			val dx = hx - lx
			val dy = hy - ly
			val inv = 1 - ratio
			for (i in 0 until 4) {
				// the four quadrants of splitBox, in its order
				val cl = if (i == 0 || i == 2) lx else lx + dx * inv
				val ct = if (i == 0 || i == 1) ly else ly + dy * inv
				val cr = if (i == 0 || i == 2) lx + dx * ratio else hx
				val cb = if (i == 0 || i == 1) ly + dy * ratio else hy
				if (contains(cl, ct, cr, cb, left, top, right, bottom)) {
					var child = node.children[i]
					if (child == null) {
						child = Node(cl, ct, cr, cb)
						node.children[i] = child
					}
					doInsertData(data, left, top, right, bottom, child, depth)
					return
				}
			}
		}
		val list = node.data ?: KTIntArrayList().also { node.data = it }
		list.add(data)
	}

	/** `KQuadRect.contains`, on the quadrant (first four) and the item box (last four). */
	private fun contains(
		qLeft: Double, qTop: Double, qRight: Double, qBottom: Double,
		left: Double, top: Double, right: Double, bottom: Double
	): Boolean {
		return minOf(qLeft, qRight) <= minOf(left, right)
				&& maxOf(qLeft, qRight) >= maxOf(left, right)
				&& minOf(qTop, qBottom) <= minOf(top, bottom)
				&& maxOf(qTop, qBottom) >= maxOf(top, bottom)
	}

	/** `KQuadRect.intersects`, on the query box and the node bounds. */
	private fun intersects(left: Double, top: Double, right: Double, bottom: Double, node: Node): Boolean {
		return minOf(left, right) <= maxOf(node.left, node.right)
				&& maxOf(left, right) >= minOf(node.left, node.right)
				&& minOf(bottom, top) <= maxOf(node.bottom, node.top)
				&& maxOf(bottom, top) >= minOf(node.bottom, node.top)
	}
}
