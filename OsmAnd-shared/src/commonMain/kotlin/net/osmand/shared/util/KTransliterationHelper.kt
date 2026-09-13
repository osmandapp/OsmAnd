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

/** Character by character romanisation, whatever the platform offers for it. */
internal expect fun toLatin(text: String): String
