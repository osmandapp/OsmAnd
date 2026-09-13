package net.osmand.shared.util.collections

import kotlin.jvm.JvmOverloads

/**
 * Fixed semantics port of `java.util.BitSet`, limited to what [net.osmand.shared] needs.
 *
 * `GeneralRouter` represents a road's tag set as a bit mask over the universal rule table and then
 * evaluates rules with `or` / `and` / `intersects` / `nextSetBit`, so the semantics below follow
 * `java.util.BitSet` exactly, including:
 * - [length] is "highest set bit + 1", not the capacity;
 * - [equals] ignores trailing zero words, so sets of different capacity can be equal;
 * - reading a bit beyond the current capacity returns false instead of failing.
 *
 * Not thread safe.
 */
class KBitSet @JvmOverloads constructor(bitCapacity: Int = BITS_PER_WORD) {

	private var words: LongArray = LongArray(wordsFor(bitCapacity))

	fun get(index: Int): Boolean {
		requireIndex(index)
		val word = index shr WORD_SHIFT
		return word < words.size && (words[word] and (1L shl (index and BIT_MASK))) != 0L
	}

	fun set(index: Int) {
		requireIndex(index)
		val word = index shr WORD_SHIFT
		ensureWords(word + 1)
		words[word] = words[word] or (1L shl (index and BIT_MASK))
	}

	fun set(index: Int, value: Boolean) {
		if (value) set(index) else clear(index)
	}

	fun clear(index: Int) {
		requireIndex(index)
		val word = index shr WORD_SHIFT
		if (word < words.size) {
			words[word] = words[word] and (1L shl (index and BIT_MASK)).inv()
		}
	}

	/** Clears every bit, keeps the capacity. */
	fun clear() {
		words.fill(0L)
	}

	fun flip(index: Int) {
		requireIndex(index)
		val word = index shr WORD_SHIFT
		ensureWords(word + 1)
		words[word] = words[word] xor (1L shl (index and BIT_MASK))
	}

	fun or(other: KBitSet) {
		if (this === other) {
			return
		}
		ensureWords(other.words.size)
		for (i in other.words.indices) {
			words[i] = words[i] or other.words[i]
		}
	}

	fun and(other: KBitSet) {
		if (this === other) {
			return
		}
		for (i in words.indices) {
			words[i] = if (i < other.words.size) words[i] and other.words[i] else 0L
		}
	}

	fun andNot(other: KBitSet) {
		val last = minOf(words.size, other.words.size)
		for (i in 0 until last) {
			words[i] = words[i] and other.words[i].inv()
		}
	}

	fun xor(other: KBitSet) {
		ensureWords(other.words.size)
		for (i in other.words.indices) {
			words[i] = words[i] xor other.words[i]
		}
	}

	/** True when the two sets share at least one bit. */
	fun intersects(other: KBitSet): Boolean {
		val last = minOf(words.size, other.words.size)
		for (i in 0 until last) {
			if (words[i] and other.words[i] != 0L) {
				return true
			}
		}
		return false
	}

	fun isEmpty(): Boolean {
		for (word in words) {
			if (word != 0L) {
				return false
			}
		}
		return true
	}

	fun cardinality(): Int {
		var count = 0
		for (word in words) {
			count += word.countOneBits()
		}
		return count
	}

	/** Index of the highest set bit plus one, 0 for an empty set. */
	fun length(): Int {
		for (i in words.indices.reversed()) {
			val word = words[i]
			if (word != 0L) {
				return (i shl WORD_SHIFT) + BITS_PER_WORD - word.countLeadingZeroBits()
			}
		}
		return 0
	}

	/** Current capacity in bits, always a multiple of 64. */
	fun size(): Int = words.size shl WORD_SHIFT

	/** Index of the first set bit at or after [fromIndex], or -1. */
	fun nextSetBit(fromIndex: Int): Int {
		requireIndex(fromIndex)
		var word = fromIndex shr WORD_SHIFT
		if (word >= words.size) {
			return -1
		}
		var bits = words[word] and (-1L shl (fromIndex and BIT_MASK))
		while (true) {
			if (bits != 0L) {
				return (word shl WORD_SHIFT) + bits.countTrailingZeroBits()
			}
			word++
			if (word >= words.size) {
				return -1
			}
			bits = words[word]
		}
	}

	/** Index of the first clear bit at or after [fromIndex]. Always defined, the set grows on demand. */
	fun nextClearBit(fromIndex: Int): Int {
		requireIndex(fromIndex)
		var word = fromIndex shr WORD_SHIFT
		if (word >= words.size) {
			return fromIndex
		}
		var bits = words[word].inv() and (-1L shl (fromIndex and BIT_MASK))
		while (true) {
			if (bits != 0L) {
				return (word shl WORD_SHIFT) + bits.countTrailingZeroBits()
			}
			word++
			if (word >= words.size) {
				return word shl WORD_SHIFT
			}
			bits = words[word].inv()
		}
	}

	/** Indices of all set bits, ascending. */
	fun toIntArray(): IntArray {
		val result = IntArray(cardinality())
		var at = 0
		var bit = nextSetBit(0)
		while (bit >= 0) {
			result[at++] = bit
			bit = nextSetBit(bit + 1)
		}
		return result
	}

	fun copy(): KBitSet {
		val result = KBitSet(size())
		words.copyInto(result.words)
		return result
	}

	inline fun forEachSetBit(action: (index: Int) -> Unit) {
		var bit = nextSetBit(0)
		while (bit >= 0) {
			action(bit)
			bit = nextSetBit(bit + 1)
		}
	}

	private fun ensureWords(count: Int) {
		if (count > words.size) {
			var newSize = if (words.size == 0) 1 else words.size shl 1
			while (newSize < count) {
				newSize = newSize shl 1
			}
			words = words.copyOf(newSize)
		}
	}

	private fun requireIndex(index: Int) {
		if (index < 0) {
			throw IndexOutOfBoundsException("Negative bit index $index")
		}
	}

	/** Equal when the same bits are set, regardless of capacity. */
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is KBitSet) return false
		val common = minOf(words.size, other.words.size)
		for (i in 0 until common) {
			if (words[i] != other.words[i]) {
				return false
			}
		}
		val longer = if (words.size > other.words.size) words else other.words
		for (i in common until longer.size) {
			if (longer[i] != 0L) {
				return false
			}
		}
		return true
	}

	/** Matches `java.util.BitSet.hashCode()` and, like [equals], ignores trailing zero words. */
	override fun hashCode(): Int {
		var wordsInUse = words.size
		while (wordsInUse > 0 && words[wordsInUse - 1] == 0L) {
			wordsInUse--
		}
		var h = 1234L
		for (i in wordsInUse - 1 downTo 0) {
			h = h xor (words[i] * (i + 1))
		}
		return ((h shr 32) xor h).toInt()
	}

	override fun toString(): String {
		val builder = StringBuilder("{")
		var first = true
		var bit = nextSetBit(0)
		while (bit >= 0) {
			if (!first) {
				builder.append(", ")
			}
			first = false
			builder.append(bit)
			bit = nextSetBit(bit + 1)
		}
		return builder.append('}').toString()
	}

	companion object {
		const val BITS_PER_WORD = 64
		private const val WORD_SHIFT = 6
		private const val BIT_MASK = 63

		private fun wordsFor(bitCapacity: Int): Int {
			require(bitCapacity >= 0) { "Negative bit capacity $bitCapacity" }
			return if (bitCapacity == 0) 1 else (bitCapacity + BITS_PER_WORD - 1) shr WORD_SHIFT
		}
	}
}
