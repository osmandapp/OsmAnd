package net.osmand.shared

import net.osmand.shared.util.KAlgorithms
import kotlin.test.Test
import kotlin.test.assertEquals

class KAlgorithmsTest {

	@Test
	fun testParseColor() {
		val colorStr1 = "#AAFF00E7"
		val color1 = 0xaaff00e7.toInt()
		val colorStr2 = "#22FF00E7"
		val color2 = 0x22ff00e7.toInt()
		val colorStr3 = "#FF00E7"
		val color3 = 0xffff00e7.toInt()
		val colorStr4 = "#00FF00E7"
		val color4 = 0x00ff00e7.toInt()

		assertEquals(colorStr1, KAlgorithms.colorToString(color1))
		assertEquals(colorStr2, KAlgorithms.colorToString(color2))
		assertEquals(colorStr3, KAlgorithms.colorToString(color3))
		assertEquals(colorStr4, KAlgorithms.colorToString(color4))
	}
}