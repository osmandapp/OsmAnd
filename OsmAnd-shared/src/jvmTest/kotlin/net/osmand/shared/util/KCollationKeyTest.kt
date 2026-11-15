package net.osmand.shared.util

import java.text.Collator
import java.text.RuleBasedCollator
import java.util.Locale
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exhaustive checks against the desktop-JVM-derived key rules, with significant/equivalent spaces
 * tailored to observations on Android API 37. This is not a full Android ICU oracle; the common
 * matcher regressions separately replay Android answers for all seven modes.
 */
class KCollationKeyTest {

	private val collator: Collator = RuleBasedCollator(
		(Collator.getInstance(Locale.US) as RuleBasedCollator).rules + " & z < ' '" +
				"\u00A0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u202F\u205F\u3000"
					.map { " = '$it'" }.joinToString("")
	).apply { strength = Collator.PRIMARY }

	@Test
	fun theSeparatorsAreTheCharactersTheCollatorSkips() {
		val empty = key("")
		var wrong = 0
		val firstWrong = StringBuilder()
		for (code in 0..0xFFFF) {
			if (Character.isSurrogate(code.toChar())) {
				continue
			}
			val ch = code.toChar()
			val collatorSkipsIt = key(ch.toString()) == empty
			if (collatorSkipsIt != KCollationKey.isSeparator(ch)) {
				wrong++
				if (wrong <= 10) {
					firstWrong.append("\n  U+%04X collator=%s ours=%s".format(code, collatorSkipsIt, KCollationKey.isSeparator(ch)))
				}
			}
		}
		assertEquals(0, wrong, "characters classified differently:$firstWrong")
	}

	/**
	 * Every character that survives the normalisation is its own primary class, apart from the
	 * equivalent spaces, ignored separators and the three that expand. If a jdk ever folds two of them together, the key stops
	 * being equivalent to the collator and this says so.
	 */
	@Test
	fun onlyEquivalentSpacesShareASurvivingPrimaryWeight() {
		val byKey = HashMap<String, Int>()
		val collisions = StringBuilder()
		var collisionCount = 0
		var survivors = 0
		for (code in 0..0xFFFF) {
			if (Character.isSurrogate(code.toChar())) {
				continue
			}
			val text = code.toChar().toString()
			if (text != aligned(text) || KCollationKey.isSeparator(code.toChar())) {
				continue
			}
			survivors++
			val previous = byKey.put(key(text), code)
			if (previous != null && !(collator.equals(text, " ") && collator.equals(previous.toChar().toString(), " "))) {
				collisionCount++
				if (collisionCount <= 10) {
					collisions.append("\n  U+%04X and U+%04X".format(previous, code))
				}
			}
		}
		assertTrue(survivors > 50000, "surviving characters: $survivors")
		assertEquals(0, collisionCount, "characters the collator folds together:$collisions")
	}

	@Test
	fun theExpansionsAreTheOnlyOnes() {
		assertEquals(key("ae"), key("æ"))
		assertEquals(key("oe"), key("œ"))
		assertEquals(key("th"), key("þ"))
		val twoLetters = HashMap<String, String>()
		val letters = "abcdefghijklmnopqrstuvwxyz0123456789"
		for (first in letters) {
			for (second in letters) {
				twoLetters.putIfAbsent(key("$first$second"), "$first$second")
			}
		}
		val unexpected = StringBuilder()
		for (code in 0..0xFFFF) {
			if (Character.isSurrogate(code.toChar())) {
				continue
			}
			val text = code.toChar().toString()
			if (text != aligned(text)) {
				continue
			}
			val expansion = twoLetters[key(text)] ?: continue
			if (code != 'æ'.code && code != 'œ'.code && code != 'þ'.code) {
				unexpected.append("\n  U+%04X expands to %s".format(code, expansion))
			}
		}
		assertEquals("", unexpected.toString(), "characters that expand and are not handled:$unexpected")
	}

	/**
	 * The collator is asked about the aligned text, because that is the only text the matcher ever
	 * hands it: `cmatches` lowercases and aligns both sides before the first comparison.
	 */
	@Test
	fun keyEqualityIsCollatorEqualityOnRandomStrings() {
		val alphabet = "abcdefgz0129 -.'\u00A0\u1680\u2009\u202F\u205F\u3000" + "æþœßéø" + "абйё" + "ασς" + "中あ"
		val random = Random(7)
		var differing = 0
		val firstDifferences = StringBuilder()
		for (i in 0 until 200_000) {
			val left = randomString(random, alphabet)
			val right = randomString(random, alphabet)
			val byCollator = collator.equals(aligned(left), aligned(right))
			val byKey = KCollationKey.primaryEquals(left, right)
			if (byCollator != byKey) {
				differing++
				if (differing <= 10) {
					firstDifferences.append("\n  '$left' vs '$right' collator=$byCollator key=$byKey")
				}
			}
		}
		assertEquals(0, differing, "pairs answered differently:$firstDifferences")
	}

	private fun randomString(random: Random, alphabet: String): String {
		val length = random.nextInt(4)
		val builder = StringBuilder(length)
		for (i in 0 until length) {
			builder.append(alphabet[random.nextInt(alphabet.length)])
		}
		return builder.toString()
	}

	private fun key(text: String): String = collator.getCollationKey(text).toByteArray().contentToString()

	/** What the matcher hands the collator: lowercased, and folded by `alignChars`. */
	private fun aligned(text: String): String = KCollationKey.lowercaseAndAlignChars(text)
}
