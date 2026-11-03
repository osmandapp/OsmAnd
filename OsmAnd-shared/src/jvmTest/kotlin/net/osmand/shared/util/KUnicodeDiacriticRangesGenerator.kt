package net.osmand.shared.util

import java.io.File
import java.text.Normalizer
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Writes `KUnicodeDiacriticRanges.kt`: the code points of the basic plane that stripping diacritics
 * can change, taken from `java.text.Normalizer` and `Character.getType`.
 *
 * Not a test: it is here because this is a source set with a jdk under it, and because a generated
 * file with no way to regenerate it is a file nobody can touch. `KUnicodeTest` is what checks the
 * result against the jdk, and it runs normally.
 *
 * To regenerate, from the repository root:
 * ```
 * ./gradlew :OsmAnd-shared:jvmTest --tests "*KUnicodeDiacriticRangesGenerator" -i
 * ```
 * with the `@Ignore` below removed, then put it back.
 */
@Ignore
class KUnicodeDiacriticRangesGenerator {

	@Test
	fun generate() {
		val ranges = mutableListOf<Int>()
		var start = -1
		for (code in 0..0xFFFF) {
			val affected = stripChangesIt(code)
			if (affected && start == -1) {
				start = code
			} else if (!affected && start != -1) {
				ranges.add(start)
				ranges.add(code - 1)
				start = -1
			}
		}
		if (start != -1) {
			ranges.add(start)
			ranges.add(0xFFFF)
		}

		val out = StringBuilder()
		out.append("package net.osmand.shared.util\n\n")
		out.append("/**\n")
		out.append(" * The code points of the basic plane that stripping diacritics can change: a non-spacing mark,\n")
		out.append(" * or a character that decomposing and recomposing does not give back. Inclusive `from, to`\n")
		out.append(" * pairs.\n")
		out.append(" *\n")
		out.append(" * Generated, do not edit: run KUnicodeDiacriticRangesGenerator to rebuild it.\n")
		out.append(" */\n")
		out.append("internal val K_UNICODE_DIACRITIC_RANGES = intArrayOf(")
		for (i in ranges.indices) {
			if (i % 8 == 0) {
				out.append("\n\t")
			}
			out.append("0x%04X".format(ranges[i]))
			if (i != ranges.size - 1) {
				out.append(",")
				if (i % 8 != 7) {
					out.append(" ")
				}
			}
		}
		out.append("\n)\n")

		val file = File("src/commonMain/kotlin/net/osmand/shared/util/KUnicodeDiacriticRanges.kt")
		file.writeText(out.toString())
		println("wrote ${file.absolutePath}: ${ranges.size / 2} ranges")
	}

	/**
	 * Whether `NFD, drop every non-spacing mark, NFC` gives back something other than [code] itself.
	 *
	 * The marks themselves go, and so does the mark inside a precomposed character. So does a
	 * character that is not its own canonical form even without a mark in it, such as the ohm sign
	 * or the greek question mark, which come back as omega and as a semicolon. A character that
	 * decomposes into base letters only, such as a hangul syllable, comes back unchanged.
	 */
	private fun stripChangesIt(code: Int): Boolean {
		val text = String(Character.toChars(code))
		val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
		val filtered = StringBuilder(decomposed.length)
		var i = 0
		while (i < decomposed.length) {
			val cp = decomposed.codePointAt(i)
			i += Character.charCount(cp)
			if (Character.getType(cp) != Character.NON_SPACING_MARK.toInt()) {
				filtered.appendCodePoint(cp)
			}
		}
		return Normalizer.normalize(filtered, Normalizer.Form.NFC) != text
	}
}
