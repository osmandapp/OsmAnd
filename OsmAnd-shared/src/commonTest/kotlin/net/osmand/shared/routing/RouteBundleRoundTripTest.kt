package net.osmand.shared.routing

import net.osmand.shared.data.KLocation
import net.osmand.shared.util.KMapUtils
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Route gpx files are written and read through [RouteSegmentResult.writeToBundle] and
 * [RouteSegmentResult.readFromBundle], and nothing else covers that pair. This drives a segment
 * through both and checks it comes back the same, including the text of the numbers, which is what
 * ends up in the file.
 */
class RouteBundleRoundTripTest {

	private val points = arrayOf(
		doubleArrayOf(50.0, 30.0),
		doubleArrayOf(50.001, 30.001),
		doubleArrayOf(50.002, 30.003),
		doubleArrayOf(50.0025, 30.005)
	)

	private fun sourceRegion(): RouteRegion {
		val region = RouteRegion()
		region.initRouteEncodingRule(0, "", "")
		region.initRouteEncodingRule(1, "highway", "primary")
		region.initRouteEncodingRule(2, "oneway", "yes")
		region.initRouteEncodingRule(3, "name", null)
		region.initRouteEncodingRule(4, "ref", null)
		return region
	}

	private fun road(region: RouteRegion): RouteDataObject {
		val road = RouteDataObject(region, intArrayOf(3, 4), arrayOf("Main street", "A1"))
		road.id = 12345L shl 6
		road.types = intArrayOf(1, 2)
		road.pointsX = IntArray(points.size) { KMapUtils.get31TileNumberX(points[it][1]) }
		road.pointsY = IntArray(points.size) { KMapUtils.get31TileNumberY(points[it][0]) }
		return road
	}

	private fun locations(): MutableList<KLocation> {
		val locations = ArrayList<KLocation>()
		for (point in points) {
			val location = KLocation("test", point[0], point[1])
			location.altitude = 100 + point[0]
			locations.add(location)
		}
		return locations
	}

	/** Rebuilds a region from the rules a route collected, the way RouteImporter does. */
	private fun targetRegion(resources: RouteDataResources): RouteRegion {
		val target = RouteRegion()
		var ruleId = 0
		for (rule in resources.getRules().keys) {
			target.initRouteEncodingRule(ruleId++, rule.getTag(), rule.getValue())
		}
		return target
	}

	@Test
	fun testSegmentSurvivesTheRoundTrip() {
		val region = sourceRegion()
		val road = road(region)

		val written = RouteSegmentResult(road, 0, points.size - 1)
		written.setSegmentTime(63.456f)
		written.setSegmentSpeed(13.888889f)
		written.setTurnType(TurnType.valueOf(TurnType.TL, false))

		val writeResources = RouteDataResources(locations())
		written.collectTypes(writeResources)
		written.collectNames(writeResources)
		val bundle = RouteDataBundle(writeResources)
		written.writeToBundle(bundle)

		// the numbers are written the way DecimalFormat used to write them
		assertEquals("4", bundle.getString("length", null))
		assertEquals("63.46", bundle.getString("segmentTime", null))
		assertEquals("13.89", bundle.getString("speed", null))
		assertEquals("12345", bundle.getString("id", null))
		assertNotNull(bundle.getString("turnType", null))

		val readResources = RouteDataResources(locations())
		val read = RouteSegmentResult(RouteDataObject(targetRegion(writeResources)), false)
		read.readFromBundle(RouteDataBundle(readResources, bundle))
		read.fillNames(readResources)

		assertEquals(0, read.getStartPointIndex())
		assertEquals(points.size - 1, read.getEndPointIndex())
		assertEquals(63.46f, read.getSegmentTime())
		assertEquals(13.89f, read.getSegmentSpeed())
		assertEquals(TurnType.TL, read.getTurnType()?.value)
		assertEquals(12345L shl 6, read.getObject().id)

		val readRoad = read.getObject()
		assertEquals(points.size, readRoad.getPointsLength())
		assertContentEquals(road.pointsX, readRoad.pointsX)
		assertContentEquals(road.pointsY, readRoad.pointsY)
		assertEquals("primary", readRoad.getHighway())
		assertEquals(1, readRoad.getOneway())
		assertEquals("Main street", readRoad.getName())
		assertEquals("A1", readRoad.getRef("", false, true))
	}

	@Test
	fun testPointTypesWithEmptyRowsSurviveTheRoundTrip() {
		val region = sourceRegion()
		region.initRouteEncodingRule(5, "highway", "traffic_signals")
		val road = road(region)
		// only the third point carries a type, the others have none
		road.setPointTypes(2, intArrayOf(5))

		val written = RouteSegmentResult(road, 0, points.size - 1)
		val resources = RouteDataResources(locations())
		written.collectTypes(resources)
		written.collectNames(resources)
		val bundle = RouteDataBundle(resources)
		written.writeToBundle(bundle)

		val target = targetRegion(resources)
		val read = RouteSegmentResult(RouteDataObject(target), false)
		read.readFromBundle(RouteDataBundle(RouteDataResources(locations()), bundle))

		val pointTypes = read.getObject().pointTypes
		assertNotNull(pointTypes)
		// a point that carries no types is written as an empty row and reads back as none
		assertNull(pointTypes[0])
		assertNull(pointTypes[1])
		assertContentEquals(
			intArrayOf(target.searchRouteEncodingRule("highway", "traffic_signals")), pointTypes[2]
		)
		assertTrue(read.getObject().hasTrafficLightAt(2))
	}

	@Test
	fun testRulesAreCollectedOnce() {
		val region = sourceRegion()

		val once = RouteDataResources()
		collect(once, region, 1)
		// highway and oneway, plus a bare and a valued rule for each of name and ref
		assertEquals(6, once.getRules().size)

		val thrice = RouteDataResources()
		collect(thrice, region, 3)
		// segments that repeat a tag reuse its rule rather than adding another
		assertEquals(once.getRules().size, thrice.getRules().size)
	}

	private fun collect(resources: RouteDataResources, region: RouteRegion, segments: Int) {
		repeat(segments) {
			val segment = RouteSegmentResult(road(region), 0, points.size - 1)
			segment.collectTypes(resources)
			segment.collectNames(resources)
		}
	}

	@Test
	fun testSegmentStartIndexAdvancesAcrossSegments() {
		val resources = RouteDataResources(locations())
		assertEquals(0, resources.getCurrentSegmentStartLocationIndex())
		resources.updateNextSegmentStartLocation(3)
		// consecutive segments share their meeting point
		assertEquals(2, resources.getCurrentSegmentStartLocationIndex())
		assertEquals(points[2][0], resources.getCurrentSegmentLocation(0).latitude)
	}
}
