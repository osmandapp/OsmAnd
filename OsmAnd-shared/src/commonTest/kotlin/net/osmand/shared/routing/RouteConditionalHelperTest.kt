package net.osmand.shared.routing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouteConditionalHelperTest {

	private val helper = RouteConditionalHelper()

	/** A region with the rules given, with the conditional ones wired to the values they resolve to. */
	private fun region(vararg tags: Pair<String, String?>): RouteRegion {
		val region = RouteRegion()
		// id 0 is never a real rule, the readers count rules from 1
		region.initRouteEncodingRule(0, "", "")
		for ((i, tag) in tags.withIndex()) {
			region.initRouteEncodingRule(i + 1, tag.first, tag.second)
		}
		region.completeRouteEncodingRules()
		return region
	}

	private fun road(region: RouteRegion, vararg types: Int): RouteDataObject {
		val road = RouteDataObject(region)
		road.id = 64
		road.types = types.toList().toIntArray()
		return road
	}

	private fun tagsOf(road: RouteDataObject): List<Pair<String, String?>> =
		road.types!!.map { road.region!!.quickGetEncodingRule(it)!!.let { r -> r.getTag() to r.getValue() } }

	@Test
	fun testConditionalTagIsWrittenOntoTheRoadAsAPlainOne() {
		val region = region("highway" to "primary", "access:conditional" to "no @ (24/7)")
		assertTrue(region.quickGetEncodingRule(2)!!.conditional())

		val road = road(region, 1, 2)
		helper.processConditionalTags(road, 0L)

		// access=no, which completeRouteEncodingRules created for the condition, is on the road now
		assertTrue(tagsOf(road).contains("access" to "no"), tagsOf(road).toString())
	}

	@Test
	fun testAConditionNobodyCanReadChangesNothing() {
		val region = region("highway" to "primary", "access:conditional" to "no @ (whenever)")
		assertTrue(region.quickGetEncodingRule(2)!!.conditional())

		val road = road(region, 1, 2)
		val before = road.types!!.toList()
		helper.processConditionalTags(road, 0L)
		assertEquals(before, road.types!!.toList())
	}

	@Test
	fun testPointTypesGetTheirConditionalsResolvedToo() {
		val region = region("highway" to "primary", "access:conditional" to "no @ (24/7)")
		val road = road(region, 1)
		road.pointTypes = arrayOf(intArrayOf(2), null)

		helper.processConditionalTags(road, 0L)

		val resolved = region.searchRouteEncodingRule("access", "no")
		assertTrue(resolved > 0)
		// the point keeps the conditional rule and gains the one it resolved to
		assertEquals(listOf(2, resolved), road.pointTypes!![0]!!.toList())
		assertNull(road.pointTypes!![1])
	}

	@Test
	fun testAmbiguousMaxKeepsTheLargerOfTheTwoValues() {
		val region = region(
			"highway" to "primary",
			"maxspeed" to "50",
			"maxspeed:conditional" to "70 @ (22:00-06:00)"
		)
		val road = road(region, 1, 2, 3)

		helper.resolveAmbiguousConditionalTags(road, mapOf("maxspeed:conditional" to RouteConditionalHelper.RULE_INT_MAX))

		// the conditional 70 beats the plain 50, and replaces it in place
		assertTrue(tagsOf(road).contains("maxspeed" to "70"), tagsOf(road).toString())
		assertTrue(!tagsOf(road).contains("maxspeed" to "50"), tagsOf(road).toString())
	}

	@Test
	fun testAmbiguousMaxLeavesTheLargerPlainValueAlone() {
		val region = region(
			"highway" to "primary",
			"maxspeed" to "90",
			"maxspeed:conditional" to "70 @ (22:00-06:00)"
		)
		val road = road(region, 1, 2, 3)

		helper.resolveAmbiguousConditionalTags(road, mapOf("maxspeed:conditional" to RouteConditionalHelper.RULE_INT_MAX))

		assertTrue(tagsOf(road).contains("maxspeed" to "90"), tagsOf(road).toString())
	}

	@Test
	fun testAPlainRuleSetsTheValueItNames() {
		val region = region("highway" to "primary", "access:conditional" to "no @ (22:00-06:00)", "access" to "yes")
		val road = road(region, 1, 2)

		helper.resolveAmbiguousConditionalTags(road, mapOf("access:conditional" to "yes"))

		assertTrue(tagsOf(road).contains("access" to "yes"), tagsOf(road).toString())
	}

	@Test
	fun testTagsNobodyAskedAboutAreLeftAlone() {
		val region = region("highway" to "primary", "maxspeed:conditional" to "70 @ (22:00-06:00)")
		val road = road(region, 1, 2)
		val before = road.types!!.toList()

		helper.resolveAmbiguousConditionalTags(road, mapOf("access:conditional" to "yes"))

		assertEquals(before, road.types!!.toList())
	}

	@Test
	fun testUpdatingByAValueTheRegionDoesNotKnowChangesNothing() {
		val region = region("highway" to "primary")
		val road = road(region, 1)

		helper.updateTypesByTagValue(road, "surface", "gravel")

		assertEquals(listOf(1), road.types!!.toList())
	}

	@Test
	fun testUpdatingByRuleIdAppendsWhenTheTagIsNew() {
		val region = region("highway" to "primary", "surface" to "gravel")
		val road = road(region, 1)

		helper.updateTypesByTagRuleId(road, "surface", 2)

		assertEquals(listOf(1, 2), road.types!!.toList())
	}

	@Test
	fun testUpdatingByRuleIdReplacesWhenTheTagIsAlreadyThere() {
		val region = region("highway" to "primary", "surface" to "gravel", "surface" to "asphalt")
		val road = road(region, 1, 2)

		helper.updateTypesByTagRuleId(road, "surface", 3)

		assertEquals(listOf(1, 3), road.types!!.toList())
		assertNotNull(road.region)
	}
}
