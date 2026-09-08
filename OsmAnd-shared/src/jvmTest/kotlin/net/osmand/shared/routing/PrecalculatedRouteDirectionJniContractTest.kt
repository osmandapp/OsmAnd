package net.osmand.shared.routing

import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins the shape that the C++ router in `core-legacy` binds to at runtime.
 *
 * `java_wrap.cpp` looks the class up by name and resolves eight fields, all of them private -
 * `GetFieldID` does not care about visibility:
 *
 * ```
 * jclass_PrecalculatedRouteDirection = findGlobalClass(env, "net/osmand/shared/routing/PrecalculatedRouteDirection");
 * jfield_PrecalculatedRouteDirection_tms = getFid(env, jclass_PrecalculatedRouteDirection, "tms", "[F");
 * ```
 *
 * `RoutingContext` also holds one in a `precalculatedRouteDirection` field the core resolves by the
 * descriptor `Lnet/osmand/shared/routing/PrecalculatedRouteDirection;`, so the package name is part
 * of the contract too.
 *
 * When a change here is intentional, `native/src/java_wrap.cpp` in OsmAnd-core-legacy has to change
 * with it, and that repository has to ship first, because the app consumes OsmAndCore as a prebuilt
 * snapshot.
 */
class PrecalculatedRouteDirectionJniContractTest {

	private val boundFields = mapOf(
		"tms" to "[F",
		"pointsX" to "[I",
		"pointsY" to "[I",
		"minSpeed" to "F",
		"maxSpeed" to "F",
		"followNext" to "Z",
		"startFinishTime" to "F",
		"endFinishTime" to "F"
	)

	@Test
	fun testClassNameMatchesTheJniLookup() {
		assertEquals(
			"net.osmand.shared.routing.PrecalculatedRouteDirection",
			PrecalculatedRouteDirection::class.java.name
		)
	}

	@Test
	fun testEveryBoundFieldKeepsItsNameAndDescriptor() {
		val fields = PrecalculatedRouteDirection::class.java.declaredFields.associateBy { it.name }
		for ((name, descriptor) in boundFields) {
			val field = fields[name]
			assertNotNull(field, "$name is read by java_wrap.cpp and must keep its name")
			assertEquals(descriptor, descriptorOf(field.type), "$name changed type")
			assertTrue(!Modifier.isStatic(field.modifiers), "$name must stay an instance field")
		}
	}

	@Test
	fun testBuildersStayStaticForJavaCallers() {
		// RoutePlannerFrontEnd and the app call PrecalculatedRouteDirection.build(...) unqualified
		val staticNames = PrecalculatedRouteDirection::class.java.declaredMethods
			.filter { Modifier.isStatic(it.modifiers) }
			.map { it.name }
			.toSet()
		assertTrue(staticNames.contains("build"), "build must stay static for Java callers")
	}

	private fun descriptorOf(type: Class<*>): String = when {
		type == Int::class.javaPrimitiveType -> "I"
		type == Float::class.javaPrimitiveType -> "F"
		type == Boolean::class.javaPrimitiveType -> "Z"
		type.isArray -> "[" + descriptorOf(type.componentType)
		else -> "L" + type.name.replace('.', '/') + ";"
	}
}
