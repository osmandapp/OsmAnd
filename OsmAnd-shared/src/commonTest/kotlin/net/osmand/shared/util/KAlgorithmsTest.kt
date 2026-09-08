package net.osmand.shared.util

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
}
