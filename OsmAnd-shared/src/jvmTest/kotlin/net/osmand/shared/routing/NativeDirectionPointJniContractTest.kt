package net.osmand.shared.routing

import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Pins the shape that the C++ router in `core-legacy` binds to at runtime.
 *
 * `java_wrap.cpp` calls `RoutingConfiguration.getNativeDirectionPoints()` and reads three fields off
 * every element of the array it gets back:
 *
 * ```
 * jclass_DirectionPoint = findGlobalClass(env, "net/osmand/shared/routing/NativeDirectionPoint");
 * jfield_DirectionPoint_tags = getFid(env, jclass_DirectionPoint, "tags", "[[Ljava/lang/String;");
 * ```
 *
 * The class name also appears inside the descriptor of that method, `()[Lnet/osmand/shared/routing/
 * NativeDirectionPoint;`, which `RoutingConfigurationDirectionPointsTest` in OsmAnd-java pins from the
 * other side. Neither is a compile error when it breaks; the direction points silently stop
 * reaching the native router.
 *
 * When a change here is intentional, `native/src/java_wrap.cpp` in OsmAnd-core-legacy has to change
 * with it, and that repository has to ship first, because the app consumes OsmAndCore as a prebuilt
 * snapshot.
 */
class NativeDirectionPointJniContractTest {

	private val boundFields = mapOf(
		"x31" to "I",
		"y31" to "I",
		"tags" to "[[Ljava/lang/String;"
	)

	@Test
	fun testClassNameMatchesTheJniLookup() {
		assertEquals(
			"net.osmand.shared.routing.NativeDirectionPoint",
			NativeDirectionPoint::class.java.name
		)
	}

	@Test
	fun testEveryBoundFieldKeepsItsNameAndDescriptor() {
		val fields = NativeDirectionPoint::class.java.declaredFields.associateBy { it.name }
		for ((name, descriptor) in boundFields) {
			val field = fields[name]
			assertNotNull(field, "$name is read by java_wrap.cpp and must keep its name")
			assertEquals(descriptor, descriptorOf(field.type), "$name changed type")
			kotlin.test.assertTrue(!Modifier.isStatic(field.modifiers), "$name must stay an instance field")
			kotlin.test.assertTrue(!Modifier.isPrivate(field.modifiers), "$name must stay a field, not a property")
		}
	}

	@Test
	fun testTagsArriveAsKeyValuePairs() {
		val point = NativeDirectionPoint(50.0, 10.0, linkedMapOf("osmand_dp" to "yes", "colour" to "red"))
		assertEquals(2, point.tags.size)
		assertEquals("osmand_dp", point.tags[0][0])
		assertEquals("yes", point.tags[0][1])
		assertEquals("colour", point.tags[1][0])
		assertEquals("red", point.tags[1][1])
	}

	private fun descriptorOf(type: Class<*>): String = when {
		type == Int::class.javaPrimitiveType -> "I"
		type.isArray -> "[" + descriptorOf(type.componentType)
		else -> "L" + type.name.replace('.', '/') + ";"
	}
}
