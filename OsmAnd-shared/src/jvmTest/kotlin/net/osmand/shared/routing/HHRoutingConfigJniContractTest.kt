package net.osmand.shared.routing

import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins the shape that the C++ router in `core-legacy` binds to at runtime.
 *
 * `java_wrap.cpp` resolves the class and fourteen of its fields **by string**, then reads them off
 * the object `NativeLibrary.nativeRouting` is handed:
 *
 * ```
 * jclass_HHRoutingConfig = findGlobalClass(env, "net/osmand/shared/routing/HHRoutingConfig");
 * jfield_HHRoutingConfig_MAX_COST = getFid(env, jclass_HHRoutingConfig, "MAX_COST", "D");
 * c->MAX_COST = ienv->GetDoubleField(jHHConf, jfield_HHRoutingConfig_MAX_COST);
 * ```
 *
 * Nothing in either build reads those strings. Rename a field, move the class to another package,
 * or widen `MAX_DEPTH` to a long, and every module still compiles - the first native route
 * calculation on a device is where it shows up. That is the whole reason this test exists.
 *
 * Visibility is not part of the contract: `GetFieldID` finds private fields, and Kotlin names the
 * backing field after the property either way. `@JvmField` on this class is for the java callers
 * that read and assign these as fields, which javac checks.
 *
 * When a change here is intentional, `native/src/java_wrap.cpp` has to change with it, and that
 * repository has to ship first, because the app consumes OsmAndCore as a prebuilt snapshot.
 */
class HHRoutingConfigJniContractTest {

	private val boundFields = mapOf(
		"HEURISTIC_COEFFICIENT" to "F",
		"DIJKSTRA_DIRECTION" to "F",
		"ROUTE_LAST_MILE" to "Z",
		"ROUTE_ALL_SEGMENTS" to "Z",
		"ROUTE_ALL_ALT_SEGMENTS" to "Z",
		"PRELOAD_SEGMENTS" to "Z",
		"CALC_ALTERNATIVES" to "Z",
		"MAX_COST" to "D",
		"MAX_DEPTH" to "I",
		"MAX_SETTLE_POINTS" to "I",
		"USE_CH" to "Z",
		"USE_CH_SHORTCUTS" to "Z",
		"USE_MIDPOINT" to "Z",
		"STRICT_BEST_GROUP_MAPS" to "Z"
	)

	@Test
	fun testClassNameMatchesTheJniLookup() {
		assertEquals("net.osmand.shared.routing.HHRoutingConfig", HHRoutingConfig::class.java.name)
	}

	@Test
	fun testEveryBoundFieldKeepsItsNameAndDescriptor() {
		val fields = HHRoutingConfig::class.java.declaredFields.associateBy { it.name }
		for ((name, descriptor) in boundFields) {
			val field = fields[name]
			assertNotNull(field, "$name is read by java_wrap.cpp and must keep its name")
			assertEquals(descriptor, descriptorOf(field.type), "$name changed type")
			assertTrue(!Modifier.isStatic(field.modifiers), "$name must stay an instance field")
		}
	}

	private fun descriptorOf(type: Class<*>): String = when {
		type == Int::class.javaPrimitiveType -> "I"
		type == Float::class.javaPrimitiveType -> "F"
		type == Double::class.javaPrimitiveType -> "D"
		type == Boolean::class.javaPrimitiveType -> "Z"
		type.isArray -> "[" + descriptorOf(type.componentType)
		else -> "L" + type.name.replace('.', '/') + ";"
	}
}
