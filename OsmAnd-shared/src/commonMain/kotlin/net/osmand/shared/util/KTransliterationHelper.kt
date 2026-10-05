package net.osmand.shared.util

import kotlin.jvm.JvmStatic

/**
 * Romanisation of map names, used wherever a latin rendering of a local name is asked for.
 */
object KTransliterationHelper {

	private var japanese = false

	@JvmStatic
	fun isJapanese(): Boolean = japanese

	/** Set once per loaded map set, from whether any of the maps covers Japan. */
	@JvmStatic
	fun setJapanese(japanese: Boolean) {
		KTransliterationHelper.japanese = japanese
	}

	@JvmStatic
	fun transliterate(text: String): String {
		// japanese is left as it is: romanising it properly needs a tokenizer, and a character by
		// character transliteration reads worse than the original
		return if (japanese) text else toLatin(text)
	}
}

/**
 * Character by character romanisation from junidecode's tables, in common code.
 *
 * It used to be an `expect`/`actual`: junidecode on the jvm and android, `NSStringTransformToLatin`
 * on iOS. Those are different engines and they disagree - over 22121 amenity names off three
 * regional maps they produced the same string for 13694 of them - so the same map could be searched
 * by a romanised name on android and not on iOS. Sharing the tables removes that, and on
 * Kotlin/Native it is also faster: 0.09 us a name against 2.3 us for the Foundation call it
 * replaces, which cannot cache the transliterator it builds.
 *
 * The walk is junidecode's own, quirks included, because the point is to produce what android has
 * always produced: the code point is read at each index but the index advances by one, so both
 * halves of a surrogate pair are looked up - the pair once as a whole and then its low half again -
 * and the block index is masked to a byte, which folds anything outside the basic plane back onto a
 * low block.
 */
internal fun toLatin(text: String): String {
	var out: StringBuilder? = null
	var i = 0
	while (i < text.length) {
		val c = text[i]
		if (c.code in IDENTITY_FIRST..IDENTITY_LAST) {
			// printable ascii, where the table maps every character to itself
			out?.append(c)
			i++
			continue
		}
		if (out == null) {
			out = StringBuilder(text.length + 8).append(text, 0, i)
		}
		out.append(romanisationOf(codePointAt(text, i)))
		i++
	}
	return out?.toString() ?: text
}

/** The range the table leaves alone, which lets a name of plain ascii be returned as it came. */
internal const val IDENTITY_FIRST = 0x20
internal const val IDENTITY_LAST = 0x7e

/** `String.codePointAt`, which common code does not have. */
private fun codePointAt(text: String, index: Int): Int {
	val c = text[index]
	if (c.isHighSurrogate() && index + 1 < text.length) {
		val next = text[index + 1]
		if (next.isLowSurrogate()) {
			return 0x10000 + ((c.code - 0xD800) shl 10) + (next.code - 0xDC00)
		}
	}
	return c.code
}

/**
 * Blocks are stored joined and split on first use.
 *
 * Two threads can split the same block at once; they compute the same array and either will do, so
 * this is left without a lock rather than paying for one on every name.
 */
private val splitBlocks = arrayOfNulls<Array<String>>(256)

internal fun romanisationOf(codePoint: Int): String {
	// junidecode masks the block index, so a code point outside the basic plane wraps onto a low one
	val high = (codePoint shr 8) and 0xff
	val low = codePoint and 0xff
	val cached = splitBlocks[high]
	if (cached != null) {
		return cached[low]
	}
	val joined = KTransliterationTables.block(high) ?: return ""
	val entries = joined.split(KTransliterationTables.SEPARATOR).toTypedArray()
	splitBlocks[high] = entries
	return entries[low]
}
