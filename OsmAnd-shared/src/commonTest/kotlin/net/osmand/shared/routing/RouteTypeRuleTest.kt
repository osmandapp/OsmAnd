package net.osmand.shared.routing

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouteTypeRuleTest {

	private fun assertSpeed(expected: Float, actual: Float) {
		assertTrue(abs(expected - actual) < 1e-4f, "expected $expected but was $actual")
	}

	@Test
	fun testOneway() {
		assertEquals(1, RouteTypeRule("oneway", "yes").onewayDirection())
		assertEquals(1, RouteTypeRule("oneway", "1").onewayDirection())
		assertEquals(-1, RouteTypeRule("oneway", "-1").onewayDirection())
		assertEquals(-1, RouteTypeRule("oneway", "reverse").onewayDirection())
		assertEquals(0, RouteTypeRule("oneway", "no").onewayDirection())
		// the tag is matched case insensitively
		assertEquals(1, RouteTypeRule("OneWay", "yes").onewayDirection())
		// a rule of another type never reports a direction
		assertEquals(0, RouteTypeRule("highway", "primary").onewayDirection())
	}

	@Test
	fun testBooleanValuesAreNormalized() {
		// "true"/"false" come from a few third party sources, osm itself uses yes/no
		assertEquals("yes", RouteTypeRule("oneway", "true").getValue())
		assertEquals("no", RouteTypeRule("oneway", "false").getValue())
		assertEquals(1, RouteTypeRule("oneway", "true").onewayDirection())
		assertEquals(0, RouteTypeRule("oneway", "false").onewayDirection())
	}

	@Test
	fun testHighwayAndRoundabout() {
		val primary = RouteTypeRule("highway", "primary")
		assertEquals("primary", primary.highwayRoad())
		assertFalse(primary.roundabout())

		assertTrue(RouteTypeRule("junction", "roundabout").roundabout())
		assertTrue(RouteTypeRule("roundabout", "any").roundabout())
		assertNull(RouteTypeRule("junction", "roundabout").highwayRoad())
		// a highway without a value is not a highway type rule
		assertNull(RouteTypeRule("highway", null).highwayRoad())
	}

	@Test
	fun testTrafficSignalsAndRailwayCrossing() {
		assertEquals(RouteTypeRule.TRAFFIC_SIGNALS, RouteTypeRule("highway", "traffic_signals").getType())
		assertEquals(RouteTypeRule.RAILWAY_CROSSING, RouteTypeRule("railway", "crossing").getType())
		assertEquals(RouteTypeRule.RAILWAY_CROSSING, RouteTypeRule("railway", "level_crossing").getType())
		assertNotEquals(RouteTypeRule.RAILWAY_CROSSING, RouteTypeRule("railway", "rail").getType())
	}

	@Test
	fun testMaxSpeed() {
		// values are stored in m/s
		assertSpeed(50 / 3.6f, RouteTypeRule("maxspeed", "50").maxSpeed(RouteTypeRule.PROFILE_NONE))
		assertSpeed(30 / 3.6f * 1.6f, RouteTypeRule("maxspeed", "30 mph").maxSpeed(RouteTypeRule.PROFILE_NONE))
		assertSpeed(RouteDataUtils.NONE_MAX_SPEED, RouteTypeRule("maxspeed", "none").maxSpeed(RouteTypeRule.PROFILE_NONE))
		// an unparseable value keeps the default passed to parseSpeed, which is 0
		assertSpeed(0f, RouteTypeRule("maxspeed", "walk").maxSpeed(RouteTypeRule.PROFILE_NONE))
		// asking for the wrong profile reports no limit
		assertSpeed(-1f, RouteTypeRule("maxspeed", "50").maxSpeed(RouteTypeRule.PROFILE_TRUCK))
	}

	@Test
	fun testMaxSpeedProfiles() {
		val hgv = RouteTypeRule("maxspeed:hgv", "60")
		assertSpeed(60 / 3.6f, hgv.maxSpeed(RouteTypeRule.PROFILE_TRUCK))
		assertSpeed(-1f, hgv.maxSpeed(RouteTypeRule.PROFILE_NONE))

		val motorcar = RouteTypeRule("maxspeed:motorcar", "90")
		assertSpeed(90 / 3.6f, motorcar.maxSpeed(RouteTypeRule.PROFILE_CAR))
		assertSpeed(-1f, motorcar.maxSpeed(RouteTypeRule.PROFILE_NONE))
	}

	@Test
	fun testMaxSpeedDirection() {
		val forward = RouteTypeRule("maxspeed:forward", "70")
		assertEquals(1, forward.isForward())
		assertSpeed(70 / 3.6f, forward.maxSpeed(RouteTypeRule.PROFILE_NONE))

		val backward = RouteTypeRule("maxspeed:backward", "70")
		assertEquals(-1, backward.isForward())
		assertSpeed(70 / 3.6f, backward.maxSpeed(RouteTypeRule.PROFILE_NONE))

		assertEquals(0, RouteTypeRule("maxspeed", "70").isForward())
	}

	@Test
	fun testLanes() {
		assertEquals(3, RouteTypeRule("lanes", "3").lanes())
		// osm sometimes carries a suffix, the leading digits still count
		assertEquals(2, RouteTypeRule("lanes", "2 lanes").lanes())
		assertEquals(-1, RouteTypeRule("lanes", "unknown").lanes())
		assertEquals(-1, RouteTypeRule("highway", "primary").lanes())
	}

	@Test
	fun testConditional() {
		val rule = RouteTypeRule("maxspeed:conditional", "30 @ (Mo-Fr 07:00-19:00); 50 @ (Sa-Su)")
		assertTrue(rule.conditional())
		assertEquals("maxspeed", rule.getNonConditionalTag())
		// a conditional rule is never used directly, so it stays untyped
		assertEquals(0, rule.getType())

		val conditions = rule.getConditions()
		assertEquals(2, conditions?.size)
		assertEquals("30", conditions?.get(0)?.value)
		assertEquals("50", conditions?.get(1)?.value)
	}

	@Test
	fun testConditionalValueResolvesToRuleId() {
		val rule = RouteTypeRule("access:conditional", "no @ (24/7)")
		rule.getConditions()!![0].ruleId = 42
		assertEquals(42, rule.conditionalValue(0L))

		// an unconditional rule has nothing to resolve
		assertFalse(RouteTypeRule("access", "no").conditional())
		assertEquals(0, RouteTypeRule("access", "no").conditionalValue(0L))
	}

	@Test
	fun testMaxIntegerConditionalValue() {
		val rule = RouteTypeRule("maxspeed:conditional", "30 @ (Mo-Fr 07:00-19:00); 50 @ (Sa-Su)")
		assertEquals(50, rule.getMaxIntegerConditionalValue())

		// non numeric values are skipped, and a rule with none at all reports nothing
		val weight = RouteTypeRule("maxweight:conditional", "delivery @ (Mo-Fr 07:00-19:00)")
		assertNull(weight.getMaxIntegerConditionalValue())
		assertNull(RouteTypeRule("highway", "primary").getMaxIntegerConditionalValue())
	}

	@Test
	fun testNonConditionalTagOfPlainTag() {
		assertEquals("maxspeed", RouteTypeRule("maxspeed", "50").getNonConditionalTag())
	}

	@Test
	fun testEqualsAndHashCode() {
		// rules are used as map keys when a route is exported
		val a = RouteTypeRule("highway", "primary")
		val b = RouteTypeRule("highway", "primary")
		assertEquals(a, b)
		assertEquals(a.hashCode(), b.hashCode())

		assertNotEquals(a, RouteTypeRule("highway", "secondary"))
		assertNotEquals(a, RouteTypeRule("route", "primary"))
		assertNotEquals<Any?>(a, "highway=primary")

		val noValue = RouteTypeRule("name", null)
		assertEquals(noValue, RouteTypeRule("name", null))
		assertNotEquals(noValue, RouteTypeRule("name", "Main street"))
	}

	@Test
	fun testToString() {
		assertEquals("highway=primary", RouteTypeRule("highway", "primary").toString())
		assertEquals("name=null", RouteTypeRule("name", null).toString())
	}

	@Test
	fun testParseSpeed() {
		assertSpeed(RouteDataUtils.NONE_MAX_SPEED, RouteDataUtils.parseSpeed("none", 1f))
		assertSpeed(50 / 3.6f, RouteDataUtils.parseSpeed("50", 1f))
		assertSpeed(50 / 3.6f, RouteDataUtils.parseSpeed("50 km/h", 1f))
		assertSpeed(50 / 3.6f * 1.6f, RouteDataUtils.parseSpeed("50mph", 1f))
		assertSpeed(7.5f, RouteDataUtils.parseSpeed("no number", 7.5f))
	}
}
