package net.osmand.shared.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The float formatting of [StringBundle] used to be `java.text.DecimalFormat`, and the route gpx
 * files already in the wild were written with it. This pins the common implementation to the same
 * text by running both over the same values.
 */
class StringBundleFormatJvmTest {

	private fun reference(maxDigits: Int): DecimalFormat {
		val pattern = "#." + "#".repeat(maxDigits)
		return DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US))
	}

	private fun formatted(value: Float, maxDigits: Int): String? {
		val bundle = StringBundle()
		bundle.putFloat("v", value, maxDigits)
		return bundle.getString("v", null)
	}

	@Test
	fun testMatchesDecimalFormatOnFixedValues() {
		val values = floatArrayOf(
			0f, 0.5f, 1f, 3f, 12.5f, 12.345f, 12.344f, -0.5f, -3f, -12.345f,
			1234.5677f, 0.004f, 0.005f, 0.015f, 100f, 1e7f, 0.1f + 0.2f, 1f / 3f, 99.999f,
			// a negative that rounds to zero keeps its sign, and so does -0.0
			-0.0022246838f, -0.0f
		)
		for (maxDigits in 2..6) {
			val reference = reference(maxDigits)
			for (value in values) {
				assertEquals(reference.format(value), formatted(value, maxDigits), "$value at $maxDigits digits")
			}
		}
	}

	@Test
	fun testMatchesDecimalFormatOnRandomValues() {
		val random = Random(20260908L)
		for (maxDigits in 2..6) {
			val reference = reference(maxDigits)
			repeat(20_000) {
				// the shapes a route bundle carries: seconds, metres per second, distances
				val value = when (it % 4) {
					0 -> random.nextFloat() * 100
					1 -> random.nextFloat() * 100_000
					2 -> random.nextFloat()
					else -> (random.nextFloat() - 0.5f) * 2000
				}
				assertEquals(reference.format(value), formatted(value, maxDigits), "$value at $maxDigits digits")
			}
		}
	}

	@Test
	fun testOutsideTheFormattedRange() {
		// java had no formatter for these and printed the plain text of the number
		assertEquals("1.5", formatted(1.5f, 1))
		assertEquals("1.5", formatted(1.5f, 7))
		assertEquals("NaN", formatted(Float.NaN, 2))
		assertEquals("∞", formatted(Float.POSITIVE_INFINITY, 2))
		assertEquals("-∞", formatted(Float.NEGATIVE_INFINITY, 2))
	}
}
