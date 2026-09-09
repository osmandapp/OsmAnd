package net.osmand.shared.routing

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins what the C++ router in `core-legacy` binds to at runtime.
 *
 * `java_wrap.cpp` looks the class up by name, reads it off the routing context by descriptor, and
 * then asks the object for its `ordinal()`:
 *
 * ```
 * jclass_RouteCalculationMode = findGlobalClass(env, "net/osmand/shared/routing/RouteCalculationMode");
 * jfield_RoutingContext_calculationMode = getFid(env, jclass_RoutingContext, "calculationMode",
 *                                                "Lnet/osmand/shared/routing/RouteCalculationMode;");
 * c->calculationMode = (RouteCalculationMode) ienv->CallIntMethod(calculationMode, getOrdinalValueMethod);
 * ```
 *
 * So the ordinals are cast straight onto the C++ `enum class RouteCalculationMode { BASE, NORMAL,
 * COMPLEX }` in `native/src/routingContext.h`. Reordering the constants here silently routes in the
 * wrong mode: BASE would arrive as NORMAL, and nothing would fail to build.
 *
 * When a change here is intentional, `native/src/java_wrap.cpp` has to change with it, and that
 * repository has to ship first, because the app consumes OsmAndCore as a prebuilt snapshot.
 */
class RouteCalculationModeJniContractTest {

	@Test
	fun testClassNameMatchesTheJniLookup() {
		assertEquals("net.osmand.shared.routing.RouteCalculationMode", RouteCalculationMode::class.java.name)
	}

	@Test
	fun testOrdinalsMatchTheCppEnum() {
		assertEquals(
			listOf("BASE", "NORMAL", "COMPLEX"),
			RouteCalculationMode.entries.map { it.name },
			"the C++ side casts ordinal() onto its own enum, declared in this order"
		)
	}
}
