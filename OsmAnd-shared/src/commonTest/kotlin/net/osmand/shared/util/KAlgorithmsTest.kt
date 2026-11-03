package net.osmand.shared.util

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KAlgorithmsTest {

	@Test
	fun testIsDigit() {
		assertTrue(KAlgorithms.isDigit('0'))
		assertTrue(KAlgorithms.isDigit('9'))
		assertFalse(KAlgorithms.isDigit('.'))
		assertFalse(KAlgorithms.isDigit('a'))
		// only ascii digits count, the same as the java original
		assertFalse(KAlgorithms.isDigit('٣'))
	}

	@Test
	fun testFindFirstNumberEndIndex() {
		assertEquals(2, KAlgorithms.findFirstNumberEndIndex("50"))
		assertEquals(2, KAlgorithms.findFirstNumberEndIndex("50 km/h"))
		assertEquals(4, KAlgorithms.findFirstNumberEndIndex("12.5 t"))
		assertEquals(2, KAlgorithms.findFirstNumberEndIndex("-5"))
		assertEquals(4, KAlgorithms.findFirstNumberEndIndex("-3.5m"))
	}

	@Test
	fun testFindFirstNumberEndIndexWithoutNumber() {
		assertEquals(-1, KAlgorithms.findFirstNumberEndIndex(""))
		assertEquals(-1, KAlgorithms.findFirstNumberEndIndex("none"))
		assertEquals(-1, KAlgorithms.findFirstNumberEndIndex("-"))
		// a value that opens with a dot carries no number
		assertEquals(-1, KAlgorithms.findFirstNumberEndIndex(".5"))
	}

	@Test
	fun testFindFirstNumberEndIndexDropsTrailingDot() {
		// "40." is corrected to "40"
		assertEquals(2, KAlgorithms.findFirstNumberEndIndex("40."))
		assertEquals(2, KAlgorithms.findFirstNumberEndIndex("40. t"))
		// a second dot makes the whole value unusable
		assertEquals(-1, KAlgorithms.findFirstNumberEndIndex("40.5.1"))
	}

	@Test
	fun testSortByFileVersionsKeepsTheOrderOfCompareFileVersions() {
		val names = listOf(
			"Germany_berlin_europe_2.obf", "germany_berlin_europe.obf", "Germany_berlin_europe_26_09_27.obf",
			"World_basemap_2.obf", "World_basemap.obf", "World_basemap_mini_2.obf", "Europe_wikivoyage.travel.obf",
			"Europe_wikivoyage_2.obf", "Ukraine_transcarpathia_europe_2.obf", "Ukraine_transcarpathia_europe_25_12_01.obf",
			"Us_texas_northamerica_2.obf", "a.obf", "A.obf", "b", ""
		)
		// names of one key, whose order only a stable sort keeps
		assertTrue(names.groupBy { KAlgorithms.simplifyFileName(it) }.values.count { it.size > 1 } >= 4)
		val random = Random(7)
		repeat(50) {
			val shuffled = names.shuffled(random)
			val expected = ArrayList(shuffled).apply { sortWith(KAlgorithms::compareFileVersions) }
			assertEquals(expected, KAlgorithms.sortByFileVersions(shuffled))
		}
	}
}
