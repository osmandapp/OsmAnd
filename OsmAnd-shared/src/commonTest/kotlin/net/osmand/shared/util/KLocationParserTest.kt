package net.osmand.shared.util

import net.osmand.shared.data.KLatLon
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * [KLocationParser] wherever it runs, on Kotlin/Native as well as the jvm: the phrases of
 * `LocationSearchTest` in OsmAnd-java that reach it, and Open Location Codes.
 *
 * [sameAsJava] asks the copy what `LocationParserCompatTest` in OsmAnd-java asked java and wrote to
 * `../OsmAnd-java/build/location-parser-java.txt`, or wherever `OSMAND_LOCATION_PARSER_JAVA_DUMP`
 * points; without it that check says so and passes. One difference is allowed: a UTM point beyond a
 * pole, which java clips to the pole. Its longitude is then what the rounding of the libm leaves of
 * a division by the cosine of about 90 degrees, and Kotlin/Native's libm is not java's.
 */
class KLocationParserTest {

	@Test
	fun basicCommaSearch() {
		parses("5.0,3.0", KLatLon(5.0, 3.0))
		parses("(5.0,3.0)", KLatLon(5.0, 3.0))
		parses("5.445,3.523", KLatLon(5.445, 3.523))
		parses("5:1:1,3:1", KLatLon((5 + 1 / 60f + 1 / 3600f).toDouble(), (3 + 1 / 60f).toDouble()))
	}

	@Test
	fun utmSearch() {
		parses("17N6734294749123", KLatLon(42.875017, -78.87659050764749))
		parses("17 N 673429 4749123", KLatLon(42.875017, -78.87659050764749))
		parses("36N 609752 5064037", KLatLon(45.721184, 34.410328))
		parses("35U 332274 5421365", KLatLon(48.922478, 24.71033))
	}

	@Test
	fun mgrsSearch() {
		// a float, as java's
		val l = assertNotNull(KLocationParser.parseLocation("18S UJ 23371 06519"))
		assertEquals(38.889801f.toDouble(), l.latitude)
		assertEquals((-77.036545f).toDouble(), l.longitude)
	}

	@Test
	fun basicSpaceSearch() {
		parses("5.0 3.0", KLatLon(5.0, 3.0))
		parses("-5.0 -3.0", KLatLon(-5.0, -3.0))
		parses("-45.5 3.0S", KLatLon(-45.5, -3.0))
		parses("45.5S 3.0 W", KLatLon(-45.5, -3.0))
		parses("5.445 3.523", KLatLon(5.445, 3.523))
		parses("5:1:1 3:1", KLatLon((5 + 1 / 60f + 1 / 3600f).toDouble(), (3 + 1 / 60f).toDouble()))
		parses("5'1'1 3'1", KLatLon((5 + 1 / 60f + 1 / 3600f).toDouble(), (3 + 1 / 60f).toDouble()))
		parses("Lat: 5.0 Lon: 3.0", KLatLon(5.0, 3.0))
		parses("0 n, 78 w", KLatLon(0.0, -78.0))
		parses("N 0 W 78", KLatLon(0.0, -78.0))
	}

	@Test
	fun advancedSpaceSearch() {
		parses("5 30 30 N 4 30 W", KLatLon(5.5 + 30 / 3600f, -4.5))
		parses("5 30  -4 30", KLatLon(5.5, -4.5))
		parses("S 5 30  4 30 W", KLatLon(-5.5, -4.5))
		parses("S5.4232  4.30W", KLatLon(-5.4232, -4.3))
		parses("5.4232, W4.30", KLatLon(5.4232, -4.3))
		parses("5.4232N, 45 30.5W", KLatLon(5.4232, -(45 + 30.5 / 60f)))
	}

	@Test
	fun arcgisSpaceSearch() {
		parses("43°S 79°23′13.7″W", KLatLon(-43.0, -(79 + 23 / 60f + 13.7 / 3600f)))
		parses("43°38′33.24″N 79°23′13.7″W", KLatLon(43 + 38 / 60f + 33.24 / 3600f, -(79 + 23 / 60f + 13.7 / 3600f)))
		parses("45° 30'30\"W 3.0", KLatLon(45 + 0.5 + 1 / 120f, -3.0))
	}

	@Test
	fun commaLatLonSearch() {
		parses("(33,95060 °S, 151,14453° E)", KLatLon(-33.95060, 151.14453))
		parses("33,95060, 151,14453", KLatLon(33.95060, 151.14453))
		parses("15,1235 S, 23,1244 W", KLatLon(-15.1235, -23.1244))
		parses("-15,1235, 23,1244", KLatLon(-15.1235, 23.1244))
	}

	@Test
	fun notPlaces() {
		assertNull(KLocationParser.parseLocation("5c Hazelmere road, nw6 6"))
		assertNull(KLocationParser.parseLocation("100 bridge street"))
		assertNull(KLocationParser.parseLocation(""))
	}

	@Test
	fun openLocationCodes() {
		assertTrue(KLocationParser.isValidOLC("8FVC9G8F+6X"))
		assertFalse(KLocationParser.isValidOLC("8FVC9G8F6X"))
		val full = assertNotNull(KLocationParser.parseOpenLocationCode(" 8fvc9g8f+6x "))
		assertTrue(full.isFull())
		assertEquals("8FVC9G8F+6X", full.getCode()?.uppercase())
		// the center of the code's area
		val center = assertNotNull(full.getLatLon())
		assertEquals(47.3655625, center.latitude, 1e-9)
		assertEquals(8.5249375, center.longitude, 1e-9)

		val short = assertNotNull(KLocationParser.parseOpenLocationCode("9G8F+6X Zurich"))
		assertFalse(short.isFull())
		assertEquals("Zurich", short.getPlaceName())
		assertNull(short.getLatLon())
		val recovered = assertNotNull(short.recover(KLatLon(47.4, 8.6)))
		assertEquals(47.3655625, recovered.latitude, 1e-9)
		assertEquals(8.5249375, recovered.longitude, 1e-9)

		assertNull(KLocationParser.parseOpenLocationCode("not a code"))
	}

	@Test
	fun sameAsJava() {
		val lines = readJavaDump(
			"KLocationParserTest", "OSMAND_LOCATION_PARSER_JAVA_DUMP", "location-parser-java.txt",
			"LocationParserCompatTest"
		) ?: return
		var compared = 0
		var beyondPole = 0
		val different = ArrayList<String>()
		for (line in lines) {
			val phrase = dumpUnhex(line.substring(2, line.indexOf('\t')))
			val copy = when (line[0]) {
				'L' -> locationLine(phrase)
				'O' -> olcLine(phrase)
				'S' -> splitLine(phrase)
				else -> fail("unknown line $line")
			}
			if (copy != line) {
				if (line[0] == 'L' && atTheSamePole(line, copy)) {
					beyondPole++
				} else if (different.size < 20) {
					different.add("${line[0]} '$phrase'\njava $line\ncopy $copy")
				}
			}
			compared++
		}
		if (different.isNotEmpty()) {
			fail("different from java:\n" + different.joinToString("\n"))
		}
		assertTrue(compared > 100000, "lines compared: $compared")
		println("KLocationParserTest: $compared answers the same as java, but for the longitude of $beyondPole beyond a pole")
	}

	private fun parses(phrase: String, expected: KLatLon) {
		assertEquals(expected, KLocationParser.parseLocation(phrase), phrase)
	}

	/** Both places are at the same pole, java's latitude and the copy's 90 or both -90. */
	private fun atTheSamePole(java: String, copy: String): Boolean {
		val j = java.substringAfter('\t').split(",")
		val k = copy.substringAfter('\t').split(",")
		if (j.size != 2 || k.size != 2 || j[0] != k[0]) {
			return false
		}
		return abs(Double.fromBits(j[0].toULong(16).toLong())) == 90.0
	}

	private fun locationLine(phrase: String): String =
		"L " + dumpHex(phrase) + "\t" + outcome { place(KLocationParser.parseLocation(phrase)) }

	private fun olcLine(phrase: String): String {
		val sb = StringBuilder("O ").append(dumpHex(phrase))
		sb.append('\t').append(outcome { KLocationParser.isValidOLC(phrase).toString() })
		val code = KLocationParser.parseOpenLocationCode(phrase)
		sb.append('\t').append(
			if (code == null) "null" else dumpHex(code.getText()) + "," + hexOrNull(code.getCode()) +
					"," + code.isFull() + "," + hexOrNull(code.getPlaceName()) + "," + place(code.getLatLon())
		)
		if (code != null) {
			for (r in REFERENCES) {
				sb.append('\t').append(outcome { place(code.recover(r)) })
			}
		}
		return sb.toString()
	}

	private fun splitLine(phrase: String): String {
		val d = ArrayList<Double>()
		val all = ArrayList<Any>()
		val strings = ArrayList<String>()
		val partial = booleanArrayOf(false)
		val sb = StringBuilder("S ").append(dumpHex(phrase)).append('\t')
		sb.append(outcome {
			KLocationParser.splitObjects(phrase, d, all, strings, partial)
			""
		})
		for (v in d) {
			sb.append(dumpBits(v)).append(',')
		}
		sb.append('\t')
		for (o in all) {
			sb.append(if (o is Double) "d" + dumpBits(o) else "s" + dumpHex(o as String)).append(',')
		}
		sb.append('\t')
		for (s in strings) {
			sb.append(dumpHex(s)).append(',')
		}
		sb.append('\t').append(partial[0]).append('\t')
		sb.append(outcome { dumpBits(KLocationParser.parse1Coordinate(all, 0, all.size)) })
		return sb.toString()
	}

	private fun place(l: KLatLon?): String =
		if (l == null) "null" else dumpBits(l.latitude) + "," + dumpBits(l.longitude)

	private fun hexOrNull(s: String?): String = if (s == null) "null" else dumpHex(s)

	/** As `LocationParserCompatTest.outcome`: what [call] returns, or `!` and the kind of what it throws. */
	private fun outcome(call: () -> String): String =
		try {
			call()
		} catch (e: NullPointerException) {
			"!NullPointer"
		} catch (e: NumberFormatException) {
			"!NumberFormat"
		} catch (e: IndexOutOfBoundsException) {
			"!IndexOutOfBounds"
		} catch (e: IllegalArgumentException) {
			"!IllegalArgument"
		} catch (e: IllegalStateException) {
			"!IllegalState"
		} catch (e: RuntimeException) {
			"!" + e::class.simpleName
		}

	companion object {
		private val REFERENCES = listOf(KLatLon(47.3769, 8.5417), KLatLon(-33.9, 151.2), KLatLon(89.9, -179.9))
	}
}
