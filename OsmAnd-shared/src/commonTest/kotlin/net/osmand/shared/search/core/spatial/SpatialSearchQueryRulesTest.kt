package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.CityBlocks
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.search.core.spatial.SpatialSearchContext.SpatialSearchStats
import net.osmand.shared.search.rules.SearchModRules
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** `SpatialSearchQueryRulesTest` of OsmAnd-java, with the rules its tests read. */
class SpatialSearchQueryRulesTest {

	private val rulesDir = rulesDir()
	private val searchRules = SearchModRules.forSearch(rulesDir.toString())

	@AfterTest
	fun deleteRules() {
		FileSystem.SYSTEM.deleteRecursively(rulesDir)
	}

	private fun token(word: String) = SpatialSearchToken(searchRules, 2, word.lowercase(), word, 0)

	@Test
	fun placeAbbreviationRequiresEnglishStreet() {
		val pl = token("Pl")
		assertTrue(pl.matchName("Place", null, "en", SpatialSearchToken.STREET_TYPE))
		assertFalse(pl.matchName("Place", null, "en", SpatialSearchToken.POI_TYPE))
		assertFalse(pl.matchName("Place", null, "de", SpatialSearchToken.STREET_TYPE))
		assertTrue(pl.matchName("Pl", null, "de", SpatialSearchToken.POI_TYPE))
		val mount = token("Mt")
		assertTrue(mount.matchName("Mount", null, "en", CityBlocks.CITY_TOWN_TYPE.index))
		assertFalse(mount.matchName("Mount", null, "en", SpatialSearchToken.STREET_TYPE))
	}

	@Test
	fun prefixMatcherFollowsTheLocaleOfTheMap() {
		val stats = SpatialSearchStats()
		val parkway = token("Pkwy")
		assertTrue(parkway.getPrefixMatcher(stats, "en").matchKey("parkway"))
		assertFalse(parkway.getPrefixMatcher(stats, "de").matchKey("parkway"))
		// the first map does not change the matches of the second one
		assertTrue(parkway.getPrefixMatcher(stats, "en").matchKey("parkway"))
		val avenue = token("Ave")
		assertTrue(avenue.matchName("Esplanade", null, "en_US", SpatialSearchToken.STREET_TYPE))
		assertFalse(avenue.matchName("Esplanade", null, "en", SpatialSearchToken.STREET_TYPE))
		assertTrue(avenue.getPrefixMatcher(stats, "en_US").matchKey("esplanade"))
		assertFalse(avenue.getPrefixMatcher(stats, "en").matchKey("esplanade"))
	}

	@Test
	fun buildingSuffixFollowsTheLocaleOfTheMap() {
		assertFalse(token("Ter").likelyPartOfBuilding("en_US"))
		assertTrue(token("Ter").likelyPartOfBuilding("fr_FR"))
		val ter = token("Ter")
		assertTrue(ter.matchName("Terrace", null, "en_US", SpatialSearchToken.STREET_TYPE))
		// a POI category is of no map: any map the word was matched with
		assertFalse(ter.likelyPartOfBuilding())
		ter.matchName("Ter", null, "fr_FR", SpatialSearchToken.STREET_TYPE)
		assertTrue(ter.likelyPartOfBuilding())
	}

	companion object {
		/**
		 * The rules the tests of OsmAnd-java read, in a new directory: those it ships, next to its routing.xml, and over
		 * them those of its test resources, which come first on the class path of its tests.
		 */
		internal fun rulesDir(): Path {
			val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "spatial-search-rules-${Random.nextLong().toULong()}"
			FileSystem.SYSTEM.createDirectories(dir)
			val shipped = RoutingTestFixtures.routingXml().toPath().parent!!.parent!! / "search" / "rules"
			val test = RoutingTestFixtures.resourcesDir() / "net" / "osmand" / "search" / "rules"
			for (from in listOf(shipped, test)) {
				for (f in FileSystem.SYSTEM.listOrNull(from).orEmpty()) {
					if (f.name.startsWith("rules") && f.name.endsWith(".xml")) {
						FileSystem.SYSTEM.copy(f, dir / f.name)
					}
				}
			}
			return dir
		}
	}
}
