package net.osmand.shared.routing

import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Lives in jvmTest rather than commonTest: the parsing goes through [net.osmand.shared.xml.XmlPullParser],
 * whose iOS implementation is an API the host app injects at startup, so nothing XML can be parsed
 * from a Kotlin/Native test binary. Everything under test here is common code either way.
 */
class RoutingConfigurationTest {

	private val xml = """
		<?xml version="1.0" encoding="UTF-8"?>
		<osmand_routing_config defaultProfile="car">
			<attribute name="zoomToLoadTiles" value="15"/>
			<attribute name="planRoadDirection" value="1"/>
			<routingProfile name="car" baseProfile="car" maxSpeed="30" derivedProfiles="truck">
				<attribute name="heuristicCoefficient" value="2"/>
				<attribute name="minPointApproximation" value="35"/>
				<parameter id="short_way" group="driving_style" name="Short" description="Shortest"
						type="boolean" default="true"/>
				<parameter id="weight" name="Weight" description="Vehicle weight" profiles="car,truck"
						type="numeric" values="3.5, 7.5, 11.5" valueDescriptions="light,medium,heavy" default="7.5"/>
				<way attribute="access">
					<select value="-1" t="highway" v="motorway"/>
					<select value="1"/>
				</way>
				<way attribute="speed">
					<select value="20" t="highway" v="primary"/>
				</way>
			</routingProfile>
			<routingProfile name="bicycle" baseProfile="bicycle">
				<attribute name="memoryLimitInMB" value="7"/>
			</routingProfile>
		</osmand_routing_config>
	""".trimIndent()

	private fun parse(filename: String? = null): RoutingConfiguration.Builder =
		RoutingConfiguration.parseFromSource(Buffer().writeUtf8(xml), filename, RoutingConfiguration.Builder())

	private fun limits() = RoutingConfiguration.RoutingMemoryLimits(0, 0)

	@Test
	fun testProfilesAreReadWithTheirBaseProfile() {
		val builder = parse()
		assertEquals("car", builder.getDefaultRouter())
		assertEquals(setOf("car", "bicycle"), builder.getAllRouters().keys)
		assertEquals(GeneralRouterProfile.CAR, builder.getRouter("car")!!.getProfile())
		assertEquals(GeneralRouterProfile.BICYCLE, builder.getRouter("bicycle")!!.getProfile())
	}

	@Test
	fun testUnknownBaseProfileFallsBackToCar() {
		val source = Buffer().writeUtf8(
			"""<osmand_routing_config defaultProfile="x">
				<routingProfile name="x" baseProfile="hovercraft"/>
			</osmand_routing_config>"""
		)
		val builder = RoutingConfiguration.parseFromSource(source)
		assertEquals(GeneralRouterProfile.CAR, builder.getRouter("x")!!.getProfile())
	}

	@Test
	fun testProfileAttributesReachTheConfiguration() {
		val config = parse().build("car", limits())
		assertEquals(2f, config.heuristicCoefficient)
		assertEquals(35f, config.minPointApproximation)
	}

	@Test
	fun testGlobalAttributesAreUsedWhenTheProfileIsSilent() {
		val config = parse().build("car", limits())
		assertEquals(15, config.ZOOM_TO_LOAD_TILES)
		assertEquals(1, config.planRoadDirection)
		// nothing declares it, so the field keeps its own default
		assertEquals(20000f, config.recalculateDistance)
	}

	@Test
	fun testMemoryLimitComesFromTheProfileOrTheCaller() {
		assertEquals(7L shl 20, parse().build("bicycle", limits()).memoryLimitation)
		assertEquals(
			64L shl 20,
			parse().build("car", RoutingConfiguration.RoutingMemoryLimits(64, 0)).memoryLimitation
		)
		// no profile limit and no caller limit falls back to the constant
		assertEquals(
			RoutingConfiguration.DEFAULT_MEMORY_LIMIT.toLong() shl 20,
			parse().build("car", limits()).memoryLimitation
		)
	}

	@Test
	fun testUnknownProfileFallsBackToTheDefaultOne() {
		val config = parse().build("hovercraft", limits())
		assertEquals("car", config.routerName)
	}

	@Test
	fun testDerivedProfileRoutesOnItsParentWithAFlag() {
		val builder = parse()
		val params = LinkedHashMap<String, String>()
		val config = builder.build("truck", null, limits(), params)
		assertEquals("car", config.routerName)
		assertEquals("true", params["profile_truck"])
	}

	@Test
	fun testBooleanAndNumericParametersAreRegistered() {
		val router = parse().getRouter("car")!!
		val shortWay = router.getParameters()["short_way"]
		assertNotNull(shortWay)
		assertEquals(GeneralRouter.RoutingParameterType.BOOLEAN, shortWay.type)
		assertEquals("driving_style", shortWay.group)
		assertTrue(shortWay.defaultBoolean)
		assertNull(shortWay.profiles)

		val weight = router.getParameters()["weight"]
		assertNotNull(weight)
		assertEquals(GeneralRouter.RoutingParameterType.NUMERIC, weight.type)
		assertEquals(7.5, weight.defaultNumeric)
		assertEquals(listOf("car", "truck"), weight.profiles!!.toList())
		// the values are trimmed, the descriptions are not
		assertEquals(listOf(3.5, 7.5, 11.5), weight.possibleValues!!.toList())
		assertEquals(listOf("light", "medium", "heavy"), weight.possibleValueDescriptions!!.toList())
	}

	@Test
	fun testRulesLandInTheAttributeTheyWereDeclaredUnder() {
		val router = parse().getRouter("car")!!
		assertEquals(2, router.getObjContext(GeneralRouter.RouteDataObjectAttribute.ACCESS).getRules().size)
		assertEquals(1, router.getObjContext(GeneralRouter.RouteDataObjectAttribute.ROAD_SPEED).getRules().size)
		assertEquals(0, router.getObjContext(GeneralRouter.RouteDataObjectAttribute.ONEWAY).getRules().size)
	}

	@Test
	fun testAFilenameNamesTheProfilesItBrought() {
		val builder = parse("bike.xml")
		assertEquals(setOf("bike.xml/car", "bike.xml/bicycle"), builder.getAllRouters().keys)
		assertEquals("bike.xml/car", builder.getRoutingProfileKeyByFileName("bike.xml"))
		assertNull(builder.getRoutingProfileKeyByFileName("other.xml"))
		assertNull(builder.getRoutingProfileKeyByFileName(null))
	}

	@Test
	fun testImpassableRoadsReachTheRouterOfEveryBuild() {
		val builder = parse()
		builder.addImpassableRoad(42L)
		assertEquals(setOf(42L), builder.getImpassableRoadLocations())

		val config = builder.build("car", limits())
		assertTrue(config.router.getImpassableRoadIds().contains(42L))

		builder.clearImpassableRoadLocations()
		assertTrue(builder.getImpassableRoadLocations().isEmpty())
		// the configuration that was already built keeps the set it was given
		assertTrue(config.router.getImpassableRoadIds().contains(42L))
	}

	@Test
	fun testEachBuildProducesItsOwnConfiguration() {
		val builder = parse()
		val first = builder.build("car", limits())
		val second = builder.build("car", limits())
		assertNotSame(first, second)
		first.showMinorTurns = true
		assertFalse(second.showMinorTurns)
	}

	@Test
	fun testRouterNameIsRecordedInTheAttributes() {
		val config = parse().build("bicycle", limits())
		assertEquals("bicycle", config.routerName)
		assertEquals("bicycle", config.attributes["routerName"])
		assertEquals("15", config.attributes["zoomToLoadTiles"])
	}

	@Test
	fun testDirectionIsCarriedIntoTheConfiguration() {
		val config = parse().build("car", 1.25, limits(), LinkedHashMap())
		assertEquals(1.25, config.initialDirection)
		assertNull(config.targetDirection)
	}

	@Test
	fun testParseSilentHelpersKeepTheDefaultOnEmptyInput() {
		assertEquals(7, RoutingConfiguration.parseSilentInt(null, 7))
		assertEquals(7, RoutingConfiguration.parseSilentInt("", 7))
		assertEquals(3, RoutingConfiguration.parseSilentInt("3", 7))
		assertEquals(7f, RoutingConfiguration.parseSilentFloat(null, 7f))
		assertEquals(7f, RoutingConfiguration.parseSilentFloat("", 7f))
		assertEquals(2.5f, RoutingConfiguration.parseSilentFloat("2.5", 7f))
	}

	@Test
	fun testConfigurationWithoutDirectionPoints() {
		val config = parse().build("car", limits())
		assertNull(config.getDirectionPoints())
		assertEquals(0, config.getNativeDirectionPoints().size)
	}
}
