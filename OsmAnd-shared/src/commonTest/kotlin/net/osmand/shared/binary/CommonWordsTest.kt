package net.osmand.shared.binary

import net.osmand.shared.map.OsmandRegions
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.dumpHex
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.readJavaDump
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * [CommonWords] and [Abbreviations] wherever they run, on Kotlin/Native as well as the jvm, with the
 * words of the regions of `regions.ocbf`: the one OsmAnd-java downloads into its resources, or
 * wherever `OSMAND_REGIONS_OCBF` points.
 *
 * [sameAsJava] asks the copy what `CommonWordsCompatTest` in OsmAnd-java asked java and wrote to
 * `../OsmAnd-java/build/common-words-java.txt`, or wherever `OSMAND_COMMON_WORDS_JAVA_DUMP` points;
 * without it that check says so and passes. It also compares the letters and digits of every
 * character of the basic plane to java's. Kotlin/Native knows a later Unicode than java 17, so a
 * character java has no category for may be a letter here; any other difference fails.
 */
class CommonWordsTest {

	@Test
	fun streetWordsAreCommon() {
		assertTrue(words.isCommon("street"))
		assertTrue(words.isCommon("улица"))
		assertTrue(words.isCommon("strasse"))
		assertFalse(words.isCommon("zurich"))
		// a house number ranks as the placeholder all of them share
		assertEquals(words.getCommon("18"), words.getCommon("18b"))
		assertNotEquals(-1, words.getCommon("18"))
		assertEquals(-1, words.getCommonGeocoding("18"))
		// higher is searched first: a frequent word before a common one, an unknown one before both
		assertTrue(words.getCommonSearch("henrique") > words.getCommonSearch("street"))
		assertEquals(-1, words.getCommonSearch("zzzzqqqq"))
	}

	@Test
	fun abbreviations() {
		assertEquals("Street", Abbreviations.replace("St"))
		assertEquals("Main", Abbreviations.replace("Main"))
		assertTrue(Abbreviations.isConjunction("und"))
		assertTrue(Abbreviations.isCommonSkipOtherCnt("northwest"))
		assertEquals("Street Saint", Abbreviations.getSearchabbreviations()["st"])
		assertTrue(Abbreviations.likelyPartOfBuilding("2bis", setOf("2", "bis")))
		assertFalse(Abbreviations.likelyPartOfBuilding("main", null))
		assertTrue(Abbreviations.likelyPartOfRef("a1", setOf("a", "1")))
	}

	@Test
	fun sameAsJava() {
		val lines = readJavaDump(
			"CommonWordsTest", "OSMAND_COMMON_WORDS_JAVA_DUMP", "common-words-java.txt", "CommonWordsCompatTest"
		) ?: return
		var compared = 0
		var newerUnicode = 0
		val different = ArrayList<String>()
		for (line in lines) {
			when (line[0]) {
				'W', 'A', 'K' -> {
					val copy = when (line[0]) {
						'W' -> wordLine(line)
						'A' -> abbreviationLine(line)
						else -> lettersLine(line)
					}
					if (copy != line && different.size < 20) {
						different.add("java $line\ncopy $copy")
					}
					compared++
				}
				'C' -> {
					val flags = line.substring(2)
					assertEquals(0x10000, flags.length, "characters")
					for (c in 0..0xFFFF) {
						val java = flags[c].digitToInt(16)
						val ch = c.toChar()
						val copy = (if (ch.isLetter()) 1 else 0) or (if (ch.isDigit()) 2 else 0)
						if (copy != (java and 3)) {
							if ((java and 4) == 0) {
								newerUnicode++
							} else if (different.size < 20) {
								different.add("U+${c.toString(16)}: java ${java and 3}, copy $copy")
							}
						}
					}
				}
			}
		}
		if (different.isNotEmpty()) {
			fail("different from java:\n" + different.joinToString("\n"))
		}
		assertTrue(compared > 100000, "lines compared: $compared")
		println(
			"CommonWordsTest: $compared answers the same as java; $newerUnicode characters " +
					"java 17 has no category for are letters or digits here"
		)
	}

	private fun wordLine(line: String): String {
		val w = dumpUnhex(line.substring(2, line.indexOf('\t')))
		return "W " + dumpHex(w) + "\t" + words.getCommon(w) + "\t" + words.getCommonSearch(w) + "\t" +
				words.getCommonGeocoding(w) + "\t" + words.getFrequentlyUsed(w) + "\t" + words.isCommon(w)
	}

	private fun abbreviationLine(line: String): String {
		val fields = line.substring(2).split("\t")
		val w = dumpUnhex(fields[0])
		val parts = LinkedHashSet<String>()
		if (fields[1].isNotEmpty()) {
			fields[1].split(",").mapTo(parts) { dumpUnhex(it) }
		}
		return "A " + fields[0] + "\t" + fields[1] +
				"\t" + Abbreviations.isConjunction(w) +
				"\t" + dumpHex(Abbreviations.replace(w)) +
				"\t" + Abbreviations.isCommonSkipOtherCnt(w) +
				"\t" + Abbreviations.likelyPartOfBuilding(w, parts) +
				"\t" + Abbreviations.likelyPartOfBuilding(w, null) +
				"\t" + Abbreviations.likelyPartOfRef(w, parts)
	}

	private fun lettersLine(line: String): String {
		val w = dumpUnhex(line.substring(2, line.indexOf('\t')))
		return "K " + dumpHex(w) +
				"\t" + KSearchAlgorithms.letters(w) +
				"\t" + KSearchAlgorithms.letters(w, 3) +
				"\t" + KSearchAlgorithms.isNumber2Letters(w) +
				"\t" + KSearchAlgorithms.startsWithDigit(w)
	}

	companion object {
		private val words: CommonWords by lazy {
			val path = testEnvironment("OSMAND_REGIONS_OCBF") ?: "../OsmAnd-java/src/main/resources/net/osmand/map/regions.ocbf"
			if (!FileSystem.SYSTEM.exists(path.toPath())) {
				fail("regions.ocbf not found at $path; :OsmAnd-java:processResources downloads it, or set OSMAND_REGIONS_OCBF")
			}
			CommonWords.osmandRegions = OsmandRegions(path)
			CommonWords.getInstance()
		}
	}
}
