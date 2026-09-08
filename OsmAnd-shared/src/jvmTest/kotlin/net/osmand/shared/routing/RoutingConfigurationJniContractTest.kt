package net.osmand.shared.routing

import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins the shape that the C++ router in `core-legacy` binds to at runtime.
 *
 * `java_wrap.cpp` looks the class up by name, resolves twelve fields by name and descriptor, and
 * calls one method:
 *
 * ```
 * jclass_RoutingConfiguration = findGlobalClass(env, "net/osmand/shared/routing/RoutingConfiguration");
 * jfield_RoutingConfiguration_router = getFid(env, jclass_RoutingConfiguration, "router", "Lnet/osmand/shared/routing/GeneralRouter;");
 * jmethod_RoutingConfiguration_getDirectionPoints = env->GetMethodID(jclass_RoutingConfiguration,
 *         "getNativeDirectionPoints", "()[Lnet/osmand/shared/routing/NativeDirectionPoint;");
 * ```
 *
 * `RoutingContext` also holds the configuration in a `config` field the core resolves by the
 * descriptor `Lnet/osmand/shared/routing/RoutingConfiguration;`, so the package name is part of the
 * contract too; `RoutingConfigurationDefaultTest` in OsmAnd-java pins that side.
 *
 * When a change here is intentional, `native/src/java_wrap.cpp` in OsmAnd-core-legacy has to change
 * with it, and that repository has to ship first, because the app consumes OsmAndCore as a prebuilt
 * snapshot.
 */
class RoutingConfigurationJniContractTest {

	private val boundFields = mapOf(
		"nativeMemoryLimitation" to "J",
		"heuristicCoefficient" to "F",
		"minPointApproximation" to "F",
		"minStepApproximation" to "F",
		"maxStepApproximation" to "F",
		"smoothenPointsNoRoute" to "F",
		"penaltyForReverseDirection" to "D",
		"ZOOM_TO_LOAD_TILES" to "I",
		"planRoadDirection" to "I",
		"routeCalculationTime" to "J",
		"routerName" to "Ljava/lang/String;",
		"router" to "Lnet/osmand/shared/routing/GeneralRouter;"
	)

	@Test
	fun testClassNameMatchesTheJniLookup() {
		assertEquals(
			"net.osmand.shared.routing.RoutingConfiguration",
			RoutingConfiguration::class.java.name
		)
	}

	@Test
	fun testEveryBoundFieldKeepsItsNameAndDescriptor() {
		val fields = RoutingConfiguration::class.java.declaredFields.associateBy { it.name }
		for ((name, descriptor) in boundFields) {
			val field = fields[name]
			assertNotNull(field, "$name is read by java_wrap.cpp and must keep its name")
			assertEquals(descriptor, descriptorOf(field.type), "$name changed type")
			assertTrue(!Modifier.isStatic(field.modifiers), "$name must stay an instance field")
			assertTrue(!Modifier.isPrivate(field.modifiers), "$name must stay a field, not a property")
		}
	}

	@Test
	fun testNativeDirectionPointsKeepsItsDescriptor() {
		val method = RoutingConfiguration::class.java.getMethod("getNativeDirectionPoints")
		assertEquals("()[Lnet/osmand/shared/routing/NativeDirectionPoint;", descriptorOf(method))
	}

	@Test
	fun testHelpersStayStaticForJavaCallers() {
		// TestRouting and the app call these unqualified
		val staticNames = RoutingConfiguration::class.java.declaredMethods
			.filter { Modifier.isStatic(it.modifiers) }
			.map { it.name }
			.toSet()
		for (name in listOf("getDefault", "parseDefault", "parseFromFile", "parseFromSource",
				"parseSilentInt", "parseSilentFloat")) {
			assertTrue(staticNames.contains(name), "$name must stay static for Java callers")
		}
	}

	@Test
	fun testParseFromFileTakesAPathSoJavaNeedsNoOkio() {
		// okio is an implementation dependency of this module, so its Path is not on the classpath
		// of OsmAnd-java; a KFile-only overload would not resolve there
		RoutingConfiguration::class.java.getMethod(
			"parseFromFile", String::class.java, String::class.java, RoutingConfiguration.Builder::class.java
		)
	}

	private fun descriptorOf(method: java.lang.reflect.Method): String =
		method.parameterTypes.joinToString("", prefix = "(", postfix = ")") { descriptorOf(it) } +
				descriptorOf(method.returnType)

	private fun descriptorOf(type: Class<*>): String = when {
		type == Int::class.javaPrimitiveType -> "I"
		type == Long::class.javaPrimitiveType -> "J"
		type == Float::class.javaPrimitiveType -> "F"
		type == Double::class.javaPrimitiveType -> "D"
		type == Boolean::class.javaPrimitiveType -> "Z"
		type.isArray -> "[" + descriptorOf(type.componentType)
		else -> "L" + type.name.replace('.', '/') + ";"
	}
}
