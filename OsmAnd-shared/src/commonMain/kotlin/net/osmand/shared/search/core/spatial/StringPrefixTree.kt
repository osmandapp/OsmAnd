package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.NameIndexReader.NameIndexReaderMatcher
import net.osmand.shared.util.KSearchAlgorithms

/**
 * A Burst Radix Trie. Leaf buckets compress their common prefixes dynamically
 * and only burst into internal routing nodes when the bucket size strictly
 * exceeds COMPACT_COUNT (5).
 *
 * A copy of `StringPrefixTree` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS. Without `simpleGet`, `printTree` and `main`, which only the diagnostics of java call.
 */
class StringPrefixTree<T> {

	private val root = Node<T>("")

	/**
	 * Holds the string remainder relative to the parent node's prefix
	 */
	private class Entry<T>(var remainder: String, var value: T) {

		override fun toString(): String {
			if (remainder.isEmpty()) {
				return value.toString()
			}
			return "$remainder - $value"
		}
	}

	/**
	 * A Radix Node that functions either as a compact multi-entry leaf bucket or a
	 * structural internal routing node.
	 */
	private class Node<T>(var prefix: String) {
		// Leaf Node Data
		var entries: MutableList<Entry<T>> = ArrayList()

		// Internal Node Data
		var children: MutableList<Node<T>>? = null
	}

	/**
	 * Tokenizes and inserts a name into the tree.
	 */
	fun put(name: String, value: T) {
		val tokens = KSearchAlgorithms.splitAndNormalize(name, false)
		for (token in tokens) {
			insert(root, token, value)
		}
	}

	private fun insert(node: Node<T>, word: String, value: T) {
		val children = node.children
		if (children == null) {
			node.entries.add(Entry(word, value))
			// Burst ONLY when this specific bucket grows beyond the allowed count
			if (node.entries.size > COMPACT_COUNT) {
				burst(node)
			}
		} else {
			if (word.isEmpty()) {
				node.entries.add(Entry(word, value))
				return
			}
			for (child in children) {
				val commonLen = getCommonPrefixLength(child.prefix, word)
				if (commonLen > 0) {
					// Partial match detected
					if (commonLen < child.prefix.length) {
						val commonPrefix = child.prefix.substring(0, commonLen)
						val childRemaining = child.prefix.substring(commonLen)
						if (child.children == null) {
							// Push the chopped prefix down into the entry remainders and update the prefix.
							for (entry in child.entries) {
								entry.remainder = childRemaining + entry.remainder
							}
							child.prefix = commonPrefix
							// Insert the new word into this newly expanded leaf bucket
							insert(child, word.substring(commonLen), value)
							return
						} else {
							// If the child is already internal, execute a standard Radix split
							val splitNode = Node<T>(childRemaining)
							splitNode.children = child.children
							splitNode.entries = child.entries

							child.prefix = commonPrefix
							child.children = ArrayList<Node<T>>().also { it.add(splitNode) }
							insert(child, word.substring(commonLen), value)
							return
						}
					} else {
						// Full match of child prefix, route deeper down the tree
						insert(child, word.substring(commonLen), value)
						return
					}
				}
			}

			// No common prefix found with existing edges, add a clean new leaf branch
			val newChild = Node<T>(word)
			newChild.entries.add(Entry("", value))
			children.add(newChild)
		}
	}

	/**
	 * Converts a leaf into an internal node and redistributes its content.
	 */
	private fun burst(node: Node<T>) {
		val oldEntries = node.entries
		node.entries = ArrayList()
		node.children = ArrayList()
		for (entry in oldEntries) {
			insert(node, entry.remainder, entry.value)
		}
	}

	fun match(matcher: NameIndexReaderMatcher): List<T> {
		val l = ArrayList<T>()
		matchSearch(root, "", matcher, l)
		return l
	}

	private fun matchSearch(node: Node<T>, prefix: String, matcher: NameIndexReaderMatcher, result: MutableList<T>) {
		for (entry in node.entries) {
			if (matcher.matchKey(prefix + entry.remainder)) {
				result.add(entry.value)
			}
		}
		val children = node.children
		if (children != null) {
			for (child in children) {
				val p = prefix + child.prefix
				if (matcher.matchKey(p)) {
					matchSearch(child, p, matcher, result)
				}
			}
		}
	}

	private fun getCommonPrefixLength(s1: String, s2: String): Int {
		var i = 0
		val len1 = s1.length
		val len2 = s2.length
		while (i < len1 && i < len2) {
			val cp1 = codePointAt(s1, i)
			val cp2 = codePointAt(s2, i)
			if (cp1 != cp2) {
				break
			}
			i += if (cp1 > 0xFFFF) 2 else 1
		}
		return i
	}

	private fun codePointAt(value: String, index: Int): Int {
		val high = value[index]
		if (high.isHighSurrogate() && index + 1 < value.length) {
			val low = value[index + 1]
			if (low.isLowSurrogate()) {
				return 0x10000 + ((high.code - 0xD800) shl 10) + (low.code - 0xDC00)
			}
		}
		return high.code
	}

	companion object {
		private const val COMPACT_COUNT = 5
	}
}
