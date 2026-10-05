package net.osmand.shared.routing

import java.util.Locale
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `RoutingParameter.getDefaultString` used `String.format(Locale.US, "%.1f", value)`, and the app
 * stores that text as the default of the preference the parameter lives in. It has to keep saying
 * the same thing, so this runs the common implementation and the java one over the same values.
 */
class GeneralRouterFormatJvmTest {

	private fun reference(value: Double): String = String.format(Locale.US, "%.1f", value)

	@Test
	fun testMatchesStringFormatOnRoutingXmlValues() {
		// the shapes routing.xml actually carries: tonnes, metres, factors
		val values = doubleArrayOf(
			0.0, 1.0, 2.5, 3.5, 5.0, 7.5, 10.0, 12.0, 0.1, 0.25, 1.05, 44.0, 100.0,
			-0.0, -1.0, -2.5, 0.04, 0.05, 0.06, 0.15, 2.675, 1.0 / 3.0
		)
		for (value in values) {
			assertEquals(reference(value), GeneralRouter.formatOneDecimal(value), "value $value")
		}
	}

	@Test
	fun testMatchesStringFormatOnRandomValues() {
		val random = Random(20260908L)
		repeat(100_000) {
			val value = (random.nextDouble() - 0.4) * Math.pow(10.0, random.nextInt(4).toDouble())
			assertEquals(reference(value), GeneralRouter.formatOneDecimal(value), "value $value")
		}
	}
}
