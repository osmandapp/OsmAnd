package net.osmand.shared.routing

import net.osmand.shared.util.KMapUtils
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouteDataObjectTest {

	/** A region with the rules a small test road needs, in a fixed order. */
	private fun region(vararg tags: Pair<String, String?>): RouteRegion {
		val region = RouteRegion()
		// id 0 is never a real rule, the readers count rules from 1
		region.initRouteEncodingRule(0, "", "")
		for ((i, tag) in tags.withIndex()) {
			region.initRouteEncodingRule(i + 1, tag.first, tag.second)
		}
		return region
	}

	private fun road(region: RouteRegion, vararg types: Int): RouteDataObject {
		val road = RouteDataObject(region)
		road.id = 64
		road.types = types.toList().toIntArray()
		// a short segment near the equator, roughly 1.5 km long
		road.pointsX = intArrayOf(KMapUtils.get31TileNumberX(30.0), KMapUtils.get31TileNumberX(30.01))
		road.pointsY = intArrayOf(KMapUtils.get31TileNumberY(50.0), KMapUtils.get31TileNumberY(50.0))
		return road
	}

	@Test
	fun testEncodingRulesAreFoundByTagAndValue() {
		val region = region("highway" to "primary", "oneway" to "yes")
		assertEquals(1, region.searchRouteEncodingRule("highway", "primary"))
		assertEquals(2, region.searchRouteEncodingRule("oneway", "yes"))
		assertEquals(-1, region.searchRouteEncodingRule("highway", "service"))
		assertEquals("primary", region.quickGetEncodingRule(1)?.getValue())
		assertEquals(3, region.quickGetEncodingRulesSize())
	}

	@Test
	fun testFindOrCreateRouteTypeAppends() {
		val region = region("highway" to "primary")
		assertEquals(1, region.findOrCreateRouteType("highway", "primary"))
		val created = region.findOrCreateRouteType("bridge", "yes")
		assertEquals(2, created)
		assertEquals(3, region.quickGetEncodingRulesSize())
		// the lookup cache is rebuilt after a rule is added
		assertEquals(2, region.searchRouteEncodingRule("bridge", "yes"))
	}

	@Test
	fun testSpecialRuleIdsAreRemembered() {
		val region = region("name" to null, "ref" to null, "highway" to "traffic_signals")
		assertEquals(1, region.getNameTypeRule())
		assertEquals(2, region.getRefTypeRule())
		assertEquals(3, region.trafficSignals)
	}

	@Test
	fun testHighwayAndOneway() {
		val region = region("highway" to "primary", "oneway" to "-1")
		val road = road(region, 1, 2)
		assertEquals("primary", road.getHighway())
		assertEquals(-1, road.getOneway())
		assertFalse(road.roundabout())
		assertFalse(road.tunnel())
	}

	@Test
	fun testRoundaboutImpliesOneway() {
		val region = region("junction" to "roundabout")
		val road = road(region, 1)
		assertTrue(road.roundabout())
		assertEquals(1, road.getOneway())
	}

	@Test
	fun testTunnel() {
		assertTrue(road(region("tunnel" to "yes"), 1).tunnel())
		assertTrue(road(region("layer" to "-1"), 1).tunnel())
		assertFalse(road(region("layer" to "1"), 1).tunnel())
	}

	@Test
	fun testMaximumSpeedFollowsDirection() {
		val region = region("highway" to "primary", "maxspeed:forward" to "50", "maxspeed:backward" to "30")
		val road = road(region, 1, 2, 3)
		assertTrue(abs(50 / 3.6f - road.getMaximumSpeed(true)) < 1e-4f)
		assertTrue(abs(30 / 3.6f - road.getMaximumSpeed(false)) < 1e-4f)
	}

	@Test
	fun testMaximumSpeedProfileWins() {
		val region = region("maxspeed" to "90", "maxspeed:hgv" to "60")
		val road = road(region, 1, 2)
		assertTrue(abs(90 / 3.6f - road.getMaximumSpeed(true)) < 1e-4f)
		assertTrue(abs(60 / 3.6f - road.getMaximumSpeed(true, RouteTypeRule.PROFILE_TRUCK)) < 1e-4f)
	}

	@Test
	fun testValueLookup() {
		val region = region("highway" to "primary", "surface" to "asphalt")
		val road = road(region, 1, 2)
		assertEquals("asphalt", road.getValue("surface"))
		assertNull(road.getValue("bridge"))
	}

	@Test
	fun testNames() {
		val region = region("name" to null, "ref" to null, "name:de" to null)
		val road = RouteDataObject(region, intArrayOf(1, 2, 3), arrayOf("Main street", "A1", "Hauptstrasse"))
		assertEquals("Main street", road.getName())
		assertEquals("Main street", road.getName(""))
		assertEquals("Hauptstrasse", road.getName("de"))
		// an unknown language falls back to the plain name
		assertEquals("Main street", road.getName("fr"))
		assertEquals("A1", road.getRef("", false, true))
		assertEquals(3, road.getNames()?.size)
	}

	@Test
	fun testPrivateAccess() {
		assertTrue(road(region("access" to "private"), 1).hasPrivateAccess(GeneralRouterProfile.CAR))
		assertTrue(road(region("motorcar" to "private"), 1).hasPrivateAccess(GeneralRouterProfile.CAR))
		assertFalse(road(region("motorcar" to "private"), 1).hasPrivateAccess(GeneralRouterProfile.BICYCLE))
		assertFalse(road(region("highway" to "primary"), 1).hasPrivateAccess(GeneralRouterProfile.CAR))
	}

	@Test
	fun testGeometry() {
		val road = road(region("highway" to "primary"), 1)
		assertEquals(2, road.getPointsLength())
		val d = road.distance(0, 1)
		// 0.01 degrees of longitude at 50N is about 715 m
		assertTrue(d > 600 && d < 800, "unexpected distance $d")
		// the road runs east, which is a quarter turn clockwise from north
		assertTrue(abs(road.directionRoute(0, true) - kotlin.math.PI / 2) < 0.05)
	}

	@Test
	fun testBearingVsRouteDirection() {
		val road = road(region("highway" to "primary"), 1)
		assertTrue(road.bearingVsRouteDirection(90f))
		assertFalse(road.bearingVsRouteDirection(270f))
		// a fix with no bearing reads as the forward direction
		assertTrue(road.bearingVsRouteDirection(null))
	}

	@Test
	fun testInsertPoint() {
		val road = road(region("highway" to "primary"), 1)
		val x = road.getPoint31XTile(0)
		val y = road.getPoint31YTile(0)
		road.insert(1, x + 10, y + 10)
		assertEquals(3, road.getPointsLength())
		assertEquals(x + 10, road.getPoint31XTile(1))
		assertEquals(y + 10, road.getPoint31YTile(1))
	}

	@Test
	fun testRestrictions() {
		val road = road(region("highway" to "primary"), 1)
		road.setRestriction(0, 512, 3, 0)
		road.setRestriction(1, 1024, 5, 77)
		assertEquals(2, road.getRestrictionLength())
		assertEquals(512, road.getRestrictionId(0))
		assertEquals(3, road.getRestrictionType(0))
		val info = road.getRestrictionInfo(1)
		assertEquals(1024, info.toWay)
		assertEquals(5, info.type)
	}

	@Test
	fun testPointTypesAndTrafficLights() {
		val region = region("highway" to "primary", "highway" to "traffic_signals")
		val road = road(region, 1)
		road.setPointTypes(1, intArrayOf(2))
		assertTrue(road.hasPointTypes())
		assertTrue(road.hasPointType(1, 2))
		assertTrue(road.hasTrafficLightAt(1))
		assertFalse(road.hasTrafficLightAt(0))
		road.removePointType(1, 2)
		assertFalse(road.hasPointType(1, 2))
	}

	@Test
	fun testCompareRoute() {
		val region = region("highway" to "primary")
		val a = road(region, 1)
		val b = road(region, 1)
		assertTrue(a.compareRoute(b))

		val c = road(region, 1)
		c.id = 128
		assertFalse(a.compareRoute(c))

		val d = RouteDataObject(a)
		assertTrue(a.compareRoute(d))
	}

	@Test
	fun testAdoptReencodesTypesAgainstAnotherRegion() {
		val source = region("highway" to "primary", "surface" to "asphalt")
		val road = road(source, 1, 2)

		// a target that already knows one of the tags under a different id
		val target = region("bridge" to "yes", "surface" to "asphalt")
		val adopted = target.adopt(road)

		assertTrue(adopted !== road)
		assertEquals("primary", adopted.getHighway())
		assertEquals("asphalt", adopted.getValue("surface"))
		// the tag it already had keeps its id, the new one is appended
		assertEquals(2, adopted.types!![1])
		assertEquals(road.id, adopted.id)

		// a road of the region itself is handed back untouched
		assertTrue(target.adopt(adopted) === adopted)
	}

	@Test
	fun testAdoptBorrowsRulesOfAnEmptyRegion() {
		val source = region("highway" to "primary")
		val road = road(source, 1)
		val empty = RouteRegion()
		assertTrue(empty.adopt(road) === road)
		assertEquals(source.quickGetEncodingRulesSize(), empty.quickGetEncodingRulesSize())
	}

	@Test
	fun testParseLength() {
		assertTrue(abs(3.5f - RouteDataObject.parseLength("3.5", 0f)) < 1e-4f)
		assertTrue(abs(3.5f - RouteDataObject.parseLength("3.5 m", 0f)) < 1e-4f)
		assertTrue(abs(3500f - RouteDataObject.parseLength("3.5 km", 0f)) < 1e-2f)
		// 14 feet 10 inches
		val feetAndInches = RouteDataObject.parseLength("14'10\"", 0f)
		assertTrue(abs(14 * 0.3048f + 10 * 0.0254f - feetAndInches) < 1e-3f, "was $feetAndInches")
		assertTrue(abs(7f - RouteDataObject.parseLength("no number", 7f)) < 1e-4f)
	}

	@Test
	fun testParseWeightInTon() {
		assertTrue(abs(7.5f - RouteDataObject.parseWeightInTon("7.5", 0f)) < 1e-4f)
		assertTrue(abs(7.5f - RouteDataObject.parseWeightInTon("7.5 t", 0f)) < 1e-4f)
		val lbs = RouteDataObject.parseWeightInTon("1000 lbs", 0f)
		assertTrue(abs(0.4535f - lbs) < 1e-3f, "was $lbs")
		assertTrue(abs(2f - RouteDataObject.parseWeightInTon("none", 2f)) < 1e-4f)
	}

	@Test
	fun testGetHighwayStatic() {
		val region = region("surface" to "asphalt", "highway" to "secondary")
		assertEquals("secondary", RouteDataObject.getHighway(intArrayOf(1, 2), region))
		assertNull(RouteDataObject.getHighway(intArrayOf(1), region))
	}
}
