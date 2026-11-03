package net.osmand.shared.util.collections

/**
 * A binary heap ordered by a comparator: the least element is at the head.
 *
 * The sift up and sift down are `java.util.PriorityQueue`'s, step for step, so that two elements
 * the comparator holds equal come out in the same order as they would on the jvm. The route
 * planner polls its open set from this, and where two segments cost the same the order they are
 * taken in decides which of two equal routes is found; keeping java's order is what lets the
 * shared planner be compared with the java one segment by segment.
 */
class KPriorityQueue<T : Any>(initialCapacity: Int, private val comparator: Comparator<in T>) {

	private var queue = arrayOfNulls<Any>(if (initialCapacity < 1) 1 else initialCapacity)

	var size: Int = 0
		private set

	fun size(): Int = size

	fun isEmpty(): Boolean = size == 0

	fun isNotEmpty(): Boolean = size != 0

	fun add(e: T): Boolean = offer(e)

	fun offer(e: T): Boolean {
		val i = size
		if (i >= queue.size) {
			grow(i + 1)
		}
		siftUp(i, e)
		size = i + 1
		return true
	}

	@Suppress("UNCHECKED_CAST")
	fun peek(): T? = if (size == 0) null else queue[0] as T

	@Suppress("UNCHECKED_CAST")
	fun poll(): T? {
		val es = queue
		val result = es[0] as T? ?: return null
		val n = --size
		val x = es[n] as T
		es[n] = null
		if (n > 0) {
			siftDown(0, x, n)
		}
		return result
	}

	fun clear() {
		for (i in 0 until size) {
			queue[i] = null
		}
		size = 0
	}

	private fun grow(minCapacity: Int) {
		val oldCapacity = queue.size
		// Double size if small; else grow by 50%
		val newCapacity = oldCapacity + (if (oldCapacity < 64) oldCapacity + 2 else oldCapacity shr 1)
		queue = queue.copyOf(if (newCapacity < minCapacity) minCapacity else newCapacity)
	}

	@Suppress("UNCHECKED_CAST")
	private fun siftUp(index: Int, x: T) {
		var k = index
		val es = queue
		while (k > 0) {
			val parent = (k - 1) ushr 1
			val e = es[parent] as T
			if (comparator.compare(x, e) >= 0) {
				break
			}
			es[k] = e
			k = parent
		}
		es[k] = x
	}

	@Suppress("UNCHECKED_CAST")
	private fun siftDown(index: Int, x: T, n: Int) {
		var k = index
		val es = queue
		val half = n ushr 1
		while (k < half) {
			var child = (k shl 1) + 1
			var c = es[child] as T
			val right = child + 1
			if (right < n && comparator.compare(c, es[right] as T) > 0) {
				child = right
				c = es[child] as T
			}
			if (comparator.compare(x, c) <= 0) {
				break
			}
			es[k] = c
			k = child
		}
		es[k] = x
	}
}
