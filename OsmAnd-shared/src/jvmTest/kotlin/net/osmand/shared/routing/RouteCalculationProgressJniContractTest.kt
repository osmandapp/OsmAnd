package net.osmand.shared.routing

import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins the shape that the C++ router in `core-legacy` binds to at runtime.
 *
 * `java_wrap.cpp` looks the class up by name and then resolves thirty fields by name and JVM
 * descriptor, five of which are private - `GetFieldID` does not care about visibility:
 *
 * ```
 * jclass_RouteCalculationProgress = findGlobalClass(env, "net/osmand/shared/routing/RouteCalculationProgress");
 * jfield_RouteCalculationProgress_segmentNotFound = getFid(env, jclass_RouteCalculationProgress, "segmentNotFound", "I");
 * ```
 *
 * The native side both reads and writes them, so a rename or a widened type is not a compile error
 * anywhere; it surfaces as a crash on the first native route calculation on a device. `RoutingContext`
 * additionally holds a `calculationProgress` field that the native side resolves by the descriptor
 * `Lnet/osmand/shared/routing/RouteCalculationProgress;`, so this class cannot change package either.
 *
 * When a change here is intentional, `native/src/java_wrap.cpp` in OsmAnd-core-legacy has to change
 * with it, and that repository has to ship first, because the app consumes OsmAndCore as a prebuilt
 * snapshot.
 */
class RouteCalculationProgressJniContractTest {

	/** Every field `java_wrap.cpp` resolves, with the descriptor it asks for. */
	private val boundFields = mapOf(
		"approximatedDistance" to "F",
		"directQueueSize" to "I",
		"directSegmentQueueSize" to "I",
		"distanceFromBegin" to "F",
		"distanceFromEnd" to "F",
		"distinctLoadedTiles" to "I",
		"fastRoutingStatusOrdinal" to "I",
		"hhCurrentStepProgress" to "D",
		"hhIterationStep" to "I",
		"hhTargetsDone" to "I",
		"hhTargetsTotal" to "I",
		"isCancelled" to "Z",
		"iteration" to "I",
		"loadedPrevUnloadedTiles" to "I",
		"loadedTiles" to "I",
		"oppositeQueueSize" to "I",
		"reverseSegmentQueueSize" to "I",
		"routingCalculatedTime" to "F",
		"segmentNotFound" to "I",
		"timeNanoToCalcDeviation" to "J",
		"timeToCalculate" to "J",
		"timeToFindInitialSegments" to "J",
		"timeToLoad" to "J",
		"timeToLoadHeaders" to "J",
		"totalApproximateDistance" to "F",
		"totalEstimatedDistance" to "F",
		"unloadedTiles" to "I",
		"visitedDirectSegments" to "I",
		"visitedOppositeSegments" to "I",
		"visitedSegments" to "I"
	)

	@Test
	fun testClassNameMatchesTheJniLookup() {
		assertEquals(
			"net.osmand.shared.routing.RouteCalculationProgress",
			RouteCalculationProgress::class.java.name
		)
	}

	@Test
	fun testEveryBoundFieldKeepsItsNameAndDescriptor() {
		val fields = RouteCalculationProgress::class.java.declaredFields.associateBy { it.name }
		for ((name, descriptor) in boundFields) {
			val field = fields[name]
			assertNotNull(field, "$name is read or written by java_wrap.cpp and must keep its name")
			assertEquals(descriptor, descriptorOf(field.type), "$name changed type")
			assertTrue(!Modifier.isStatic(field.modifiers), "$name must stay an instance field")
			assertTrue(!Modifier.isFinal(field.modifiers), "$name is written from native code")
		}
	}

	@Test
	fun testTheNativeSideCanStillReachTheProgressWithoutAccessors() {
		// @JvmField on the public ones, plain private fields for the rest: either way the native
		// side gets a field, not a getter. A property with an accessor would resolve to nothing.
		val progress = RouteCalculationProgress()
		val field = RouteCalculationProgress::class.java.getDeclaredField("fastRoutingStatusOrdinal")
		field.isAccessible = true
		field.setInt(progress, FastRoutingState.Status.FAILED_WITH_MISSING_MAPS.ordinal)
		assertEquals(FastRoutingState.Status.FAILED_WITH_MISSING_MAPS, progress.getFastRoutingStatus())
		assertTrue(progress.isSlowRoutingActive())
	}

	@Test
	fun testFastRoutingHelpersStayStaticForJavaCallers() {
		// NavigationSession calls FastRoutingState.isSuccessStatus(...) unqualified
		val staticNames = FastRoutingState::class.java.declaredMethods
			.filter { Modifier.isStatic(it.modifiers) }
			.map { it.name }
			.toSet()
		for (name in listOf("isSuccessStatus", "isCancelledStatus", "isFailedStatus")) {
			assertTrue(staticNames.contains(name), "$name must stay static for Java callers")
		}
	}

	@Test
	fun testCaptureStaysStaticForJavaCallers() {
		val staticNames = RouteCalculationProgress::class.java.declaredMethods
			.filter { Modifier.isStatic(it.modifiers) }
			.map { it.name }
			.toSet()
		assertTrue(staticNames.contains("capture"), "capture must stay static for Java callers")
	}

	private fun descriptorOf(type: Class<*>): String = when (type) {
		Int::class.javaPrimitiveType -> "I"
		Long::class.javaPrimitiveType -> "J"
		Float::class.javaPrimitiveType -> "F"
		Double::class.javaPrimitiveType -> "D"
		Boolean::class.javaPrimitiveType -> "Z"
		else -> "L" + type.name.replace('.', '/') + ";"
	}
}
