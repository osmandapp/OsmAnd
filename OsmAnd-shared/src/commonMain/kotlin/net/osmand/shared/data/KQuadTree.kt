package net.osmand.shared.data

import kotlin.jvm.JvmOverloads

/**
 * Bounded quad tree over [KQuadRect], port of `net.osmand.data.QuadTree`.
 *
 * Children boxes overlap by design: [ratio] above 0.5 makes each quadrant slightly larger than a
 * quarter, so items straddling the middle still descend instead of piling up in the parent node.
 * Items that fit no single child stay in the node they reached.
 */
class KQuadTree<T> @JvmOverloads constructor(
	bounds: KQuadRect,
	depth: Int = 8,
	ratio: Float = 0.55f
) {

	private class Node<T>(bounds: KQuadRect) {
		var data: MutableList<T>? = null
		val children: Array<Node<T>?> = arrayOfNulls(4)
		val bounds: KQuadRect = KQuadRect(bounds.left, bounds.top, bounds.right, bounds.bottom)
	}

	private val ratio: Float = ratio
	private val maxDepth: Int = depth
	private val root: Node<T> = Node(bounds)

	fun insert(data: T, box: KQuadRect) {
		doInsertData(data, box, root, 0)
	}

	fun insert(data: T, x: Float, y: Float) {
		val v = x.toDouble()
		val w = y.toDouble()
		insert(data, KQuadRect(v, w, v, w))
	}

	/** Fills [result] with every item whose node bounds intersect [box] and returns it. */
	fun queryInBox(box: KQuadRect, result: MutableList<T>): MutableList<T> {
		result.clear()
		queryNode(box, result, root)
		return result
	}

	fun clear() {
		clear(root)
	}

	/**
	 * True when [box] intersects the tree.
	 *
	 * With [hintDepth] of -1 every candidate item is verified through [contains]; with a depth of 0
	 * or more the check stops at that level and reports a bounds intersection without looking at
	 * items, which is the cheap "is anything around here" mode.
	 */
	fun checkIntersection(box: KQuadRect, hintDepth: Int, contains: KQuadTreeItemInQuadRect<T>): Boolean {
		val depth = if (hintDepth != -1 && hintDepth > maxDepth) -1 else hintDepth
		return checkIntersectionRecursive(box, root, 0, depth, contains)
	}

	fun interface KQuadTreeItemInQuadRect<T> {
		fun contains(rect: KQuadRect, item: T): Boolean
	}

	private fun clear(node: Node<T>?) {
		if (node != null) {
			node.data?.clear()
			for (child in node.children) {
				clear(child)
			}
		}
	}

	private fun queryNode(box: KQuadRect, result: MutableList<T>, node: Node<T>?) {
		if (node != null && KQuadRect.intersects(box, node.bounds)) {
			node.data?.let { result.addAll(it) }
			for (child in node.children) {
				queryNode(box, result, child)
			}
		}
	}

	private fun doInsertData(data: T, box: KQuadRect, node: Node<T>, currentDepth: Int) {
		val depth = currentDepth + 1
		if (depth < maxDepth) {
			val ext = splitBox(node.bounds)
			for (i in 0 until 4) {
				if (ext[i].contains(box)) {
					var child = node.children[i]
					if (child == null) {
						child = Node(ext[i])
						node.children[i] = child
					}
					doInsertData(data, box, child, depth)
					return
				}
			}
		}
		val list = node.data ?: ArrayList<T>().also { node.data = it }
		list.add(data)
	}

	private fun splitBox(extent: KQuadRect): Array<KQuadRect> {
		val lx = extent.left
		val ly = extent.top
		val hx = extent.right
		val hy = extent.bottom
		val dx = hx - lx
		val dy = hy - ly
		val inv = 1 - ratio
		return arrayOf(
			KQuadRect(lx, ly, lx + dx * ratio, ly + dy * ratio),
			KQuadRect(lx + dx * inv, ly, hx, ly + dy * ratio),
			KQuadRect(lx, ly + dy * inv, lx + dx * ratio, hy),
			KQuadRect(lx + dx * inv, ly + dy * inv, hx, hy)
		)
	}

	private fun checkIntersectionRecursive(
		box: KQuadRect,
		node: Node<T>?,
		currentDepth: Int,
		targetDepth: Int,
		contains: KQuadTreeItemInQuadRect<T>
	): Boolean {
		if (node == null || !KQuadRect.intersects(box, node.bounds)) {
			return false
		}
		if (targetDepth != -1) {
			if (currentDepth == targetDepth) {
				return true
			}
			if (currentDepth > targetDepth) {
				return false
			}
		}
		node.data?.let { items ->
			for (item in items) {
				if (contains.contains(box, item)) {
					return true
				}
			}
		}
		for (child in node.children) {
			if (checkIntersectionRecursive(box, child, currentDepth + 1, targetDepth, contains)) {
				return true
			}
		}
		return false
	}
}
