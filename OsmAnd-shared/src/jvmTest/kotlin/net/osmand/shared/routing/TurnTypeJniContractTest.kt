package net.osmand.shared.routing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the shape that the C++ router in `core-legacy` binds to at runtime.
 *
 * `java_wrap.cpp` looks the class up by name and resolves its constructor by JVM descriptor:
 *
 * ```
 * jclass_TurnType = findGlobalClass(env, "net/osmand/shared/routing/TurnType");
 * jmethod_TurnType_init = env->GetMethodID(jclass_TurnType, "<init>", "(IIFZ[IZZ)V");
 * ```
 *
 * Nothing in Kotlin or Java fails to compile when that contract breaks. Renaming the class, moving
 * its package, reordering the constructor parameters or giving any of them a default value (which
 * makes Kotlin emit a synthetic mask parameter instead) all produce a `NoSuchMethodError` on the
 * first native route calculation on a device, and nowhere earlier.
 *
 * When a change here is intentional, `native/src/java_wrap.cpp` in OsmAnd-core-legacy has to change
 * with it, and that repository has to ship first, because the app consumes OsmAndCore as a prebuilt
 * snapshot.
 */
class TurnTypeJniContractTest {

	@Test
	fun testClassNameMatchesTheJniLookup() {
		assertEquals("net.osmand.shared.routing.TurnType", TurnType::class.java.name)
	}

	@Test
	fun testConstructorDescriptorMatchesTheJniLookup() {
		val expected = "(IIFZ[IZZ)V"
		val descriptors = TurnType::class.java.declaredConstructors.map { descriptorOf(it.parameterTypes) }
		assertTrue(
			descriptors.contains(expected),
			"no constructor with descriptor $expected, found $descriptors"
		)
	}

	@Test
	fun testConstructorIsPublicAndProducesTheGivenValues() {
		val constructor = TurnType::class.java.getConstructor(
			Int::class.javaPrimitiveType,
			Int::class.javaPrimitiveType,
			Float::class.javaPrimitiveType,
			Boolean::class.javaPrimitiveType,
			IntArray::class.java,
			Boolean::class.javaPrimitiveType,
			Boolean::class.javaPrimitiveType
		)
		val lanes = intArrayOf(1, 2)
		val turn = constructor.newInstance(TurnType.TL, 3, 45f, true, lanes, true, false) as TurnType

		assertEquals(TurnType.TL, turn.value)
		assertEquals(3, turn.exitOut)
		assertEquals(45f, turn.turnAngle)
		assertTrue(turn.isSkipToSpeak)
		assertEquals(lanes, turn.lanes)
		assertTrue(turn.isPossibleLeftTurn)
		assertTrue(!turn.isPossibleRightTurn)
	}

	@Test
	fun testHelpersStayStaticForJavaCallers() {
		// @JvmStatic keeps the existing Java call sites (TurnType.valueOf(...)) compiling.
		// Without it they would have to say TurnType.Companion.valueOf(...) instead.
		val staticNames = TurnType::class.java.declaredMethods
			.filter { java.lang.reflect.Modifier.isStatic(it.modifiers) }
			.map { it.name }
			.toSet()
		for (name in listOf(
			"straight", "valueOf", "fromString", "getExitTurn", "lanesToString", "lanesFromString",
			"getPrimaryTurn", "getSecondaryTurn", "getTertiaryTurn", "setPrimaryTurnAndReset",
			"convertType", "orderFromLeftToRight"
		)) {
			assertTrue(staticNames.contains(name), "$name must stay static for Java callers")
		}
	}

	@Test
	fun testConstantsStayStaticFieldsForJavaCallers() {
		val fields = TurnType::class.java.declaredFields
			.filter { java.lang.reflect.Modifier.isStatic(it.modifiers) }
			.associateBy { it.name }
		for (name in listOf("C", "TL", "TSLL", "TSHL", "TR", "TSLR", "TSHR", "KL", "KR", "TU", "TRU", "OFFR", "RNDB", "RNLB")) {
			val field = fields[name]
			assertTrue(field != null, "TurnType.$name must stay a static field")
			assertEquals(Int::class.javaPrimitiveType, field.type, "TurnType.$name must stay an int")
		}
	}

	private fun descriptorOf(parameterTypes: Array<Class<*>>): String =
		parameterTypes.joinToString("", prefix = "(", postfix = ")V") { jvmName(it) }

	private fun jvmName(type: Class<*>): String = when {
		type == Int::class.javaPrimitiveType -> "I"
		type == Float::class.javaPrimitiveType -> "F"
		type == Boolean::class.javaPrimitiveType -> "Z"
		type.isArray -> "[" + jvmName(type.componentType)
		else -> "L" + type.name.replace('.', '/') + ";"
	}
}
