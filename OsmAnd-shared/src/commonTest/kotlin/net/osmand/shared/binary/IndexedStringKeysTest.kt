package net.osmand.shared.binary

import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.data.MapObject
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.search.core.SearchPhraseTest
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The keys at the top of the name tables a reader keeps between searches: searches of streets and
 * pois by name on one reader, one after another, find what each finds on a reader of its own, which
 * reads every key afresh. Over the maps of the search tests, which `SearchPhraseCompatTest` unpacks
 * into `OsmAnd-java/build/search-obf`, and the real maps `OSMAND_NAME_SEARCH_MAPS` names, separated
 * by `:`, whose tables are larger.
 */
class IndexedStringKeysTest {

	@Test
	fun keptKeysFindTheSame() {
		val dir = RoutingTestFixtures.resourcesDir().parent!!.parent!!.parent!! / "build" / "search-obf"
		if (!FileSystem.SYSTEM.exists(dir)) {
			println("IndexedStringKeysTest: no maps at $dir, run SearchPhraseCompatTest in OsmAnd-java first")
			return
		}
		SearchPhraseTest.setUp
		val files = FileSystem.SYSTEM.list(dir).filter { it.name.endsWith(".obf") }.sorted() +
				(testEnvironment("OSMAND_NAME_SEARCH_MAPS")?.split(":")?.map { it.toPath() } ?: emptyList())
		var searches = 0
		var found = 0
		for (file in files) {
			val kept = BinaryMapIndexReader(file.toString())
			try {
				for (word in words(kept)) {
					for (mode in MODES) {
						val streets = streets(kept, word, mode)
						val pois = pois(kept, word, mode)
						assertEquals(fresh(file) { streets(it, word, mode) }, streets, "${file.name} streets '$word' $mode")
						assertEquals(fresh(file) { pois(it, word, mode) }, pois, "${file.name} pois '$word' $mode")
						searches += 2
						found += (if (streets.isEmpty()) 0 else 1) + (if (pois.isEmpty()) 0 else 1)
					}
				}
			} finally {
				kept.close()
			}
		}
		assertTrue(searches > 1000 && found > 500, "$searches searches, $found found something")
		println("IndexedStringKeysTest: $searches searches the same on a reader kept and on fresh ones, $found of them found something")
	}

	private companion object {
		/** How many streets of a settlement give a word to search by. */
		const val WORDS = 20

		val MODES = listOf(KStringMatcherMode.CHECK_EQUALS_FROM_SPACE, KStringMatcherMode.CHECK_STARTS_FROM_SPACE)

		/**
		 * The longest word of the names of the first streets of the first towns and villages, and its
		 * first three letters.
		 */
		fun words(reader: BinaryMapIndexReader): List<String> {
			val words = LinkedHashSet<String>()
			val cities = reader.getCities(null, CityBlocks.CITY_TOWN_TYPE).take(2) + reader.getCities(null, CityBlocks.VILLAGES_TYPE).take(2)
			for (city in cities) {
				reader.preloadStreets(city, null)
				for (street in city.getStreets().take(WORDS)) {
					val word = street.getName()?.lowercase()?.split(" ")?.maxByOrNull { it.length } ?: continue
					words.add(word)
					words.add(word.take(3))
				}
			}
			return words.toList()
		}

		fun <T> fresh(file: Path, search: (BinaryMapIndexReader) -> T): T {
			val reader = BinaryMapIndexReader(file.toString())
			try {
				return search(reader)
			} finally {
				reader.close()
			}
		}

		fun streets(reader: BinaryMapIndexReader, word: String, mode: KStringMatcherMode): List<String> =
			reader.searchAddressDataByName(SearchRequest.buildAddressByNameRequest<MapObject>(null, word, mode))
				.map { it::class.simpleName + ":" + it.getId() + ":" + it.getName() }

		fun pois(reader: BinaryMapIndexReader, word: String, mode: KStringMatcherMode): List<String> {
			val req = SearchRequest.buildSearchPoiRequest(0, 0, word, 0, Int.MAX_VALUE, 0, Int.MAX_VALUE)
			req.matcherMode = mode
			return reader.searchPoiByName(req).map { "A" + it.getId() + ":" + it.getName() }
		}
	}
}
