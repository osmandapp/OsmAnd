package net.osmand.shared.routing

import net.osmand.shared.util.KMapUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DirectionPointTest {

	@Test
	fun testTagKeysAreLowerCased() {
		// the router looks these up in the routing rules, which are lower case
		val point = DirectionPoint(50.0, 10.0)
		point.putTag("Apply_Direction_Angle", "90")
		assertEquals("90", point.getTag("apply_direction_angle"))
		assertNull(point.getTag("Apply_Direction_Angle"))
		assertEquals(mapOf("apply_direction_angle" to "90"), point.getTags())
	}

	@Test
	fun testPutTagAnswersTheValueItReplaced() {
		val point = DirectionPoint(50.0, 10.0)
		assertNull(point.putTag("osmand_dp", "yes"))
		assertEquals("yes", point.putTag("osmand_dp", "no"))
		assertEquals("no", point.getTag("osmand_dp"))
	}

	@Test
	fun testAngleIsNaNWhenTheTagIsMissing() {
		assertTrue(DirectionPoint(50.0, 10.0).getAngle().isNaN())
	}

	@Test
	fun testAngleIsRead() {
		val point = DirectionPoint(50.0, 10.0)
		point.putTag(DirectionPoint.ANGLE_TAG, "137.5")
		assertEquals(137.5, point.getAngle())

		point.putTag(DirectionPoint.ANGLE_TAG, "-45")
		assertEquals(-45.0, point.getAngle())
	}

	@Test
	fun testUnreadableAngleThrows() {
		val point = DirectionPoint(50.0, 10.0)
		point.putTag(DirectionPoint.ANGLE_TAG, "north")
		assertFailsWith<RuntimeException> { point.getAngle() }
	}

	@Test
	fun testCopyKeepsThePlaceAndTagsAndNothingElse() {
		val source = DirectionPoint(50.0, 10.0)
		source.putTag("osmand_dp", "yes")
		source.putTag(DirectionPoint.ANGLE_TAG, "90")
		// state a previous calculation left on it
		source.distance = 12.0
		source.connected = RouteDataObject(RouteRegion())
		source.connectedx = 5
		source.connectedy = 7
		source.types.add(3)

		val copy = DirectionPoint(source)
		assertEquals(50.0, copy.getLatitude())
		assertEquals(10.0, copy.getLongitude())
		assertEquals(source.getTags(), copy.getTags())
		assertNotSame(source.getTags(), copy.getTags())

		assertEquals(Double.MAX_VALUE, copy.distance)
		assertNull(copy.connected)
		assertEquals(0, copy.connectedx)
		assertEquals(0, copy.connectedy)
		assertEquals(0, copy.types.size())
	}

	@Test
	fun testCopiedTagsAreIndependent() {
		val source = DirectionPoint(50.0, 10.0)
		source.putTag("osmand_dp", "yes")
		val copy = DirectionPoint(source)
		copy.putTag("osmand_dp", "no")
		assertEquals("yes", source.getTag("osmand_dp"))
		assertEquals("no", copy.getTag("osmand_dp"))
	}

	@Test
	fun testTheTagsThatNameTheRouteTypes() {
		// the router splices a point carrying DirectionPoint.TAG into the road, and retypes the one
		// it left behind; the strings reach the map data, so they are fixed
		assertEquals("osmand_dp", DirectionPoint.TAG)
		assertEquals("osmand_add_point", DirectionPoint.CREATE_TYPE)
		assertEquals("osmand_delete_point", DirectionPoint.DELETE_TYPE)
		assertEquals("apply_direction_angle", DirectionPoint.ANGLE_TAG)
		assertEquals(45.0, DirectionPoint.MAX_ANGLE_DIFF)
	}

	@Test
	fun testNativePointTakesXFromLongitudeAndYFromLatitude() {
		val point = DirectionPoint(50.0, 10.0)
		val native = NativeDirectionPoint(point.getLatitude(), point.getLongitude(), point.getTags())
		assertEquals(10.0, KMapUtils.get31LongitudeX(native.x31), 1e-5)
		assertEquals(50.0, KMapUtils.get31LatitudeY(native.y31), 1e-5)
	}

	@Test
	fun testNativePointFlattensTagsInOrder() {
		val point = DirectionPoint(50.0, 10.0)
		point.putTag("osmand_dp", "yes")
		point.putTag(DirectionPoint.ANGLE_TAG, "90")

		val native = NativeDirectionPoint(point.getLatitude(), point.getLongitude(), point.getTags())
		assertEquals(2, native.tags.size)
		assertEquals(listOf("osmand_dp", "yes"), native.tags[0].toList())
		assertEquals(listOf("apply_direction_angle", "90"), native.tags[1].toList())
	}

	@Test
	fun testNativePointWithoutTags() {
		val native = NativeDirectionPoint(50.0, 10.0, emptyMap())
		assertEquals(0, native.tags.size)
	}
}
