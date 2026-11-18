package net.osmand.shared.search

import net.osmand.shared.data.KLatLon
import net.osmand.shared.search.SearchUICore.SearchResultCollection
import net.osmand.shared.search.core.SearchPhrase
import net.osmand.shared.search.core.SearchResult
import net.osmand.shared.search.core.SearchSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class SpatialResultsSortTest {
	private fun collection(sort: SearchSettings.SortType): SearchResultCollection {
		val settings = SearchSettings(emptyList()).setOriginalLocation(KLatLon(49.84, 24.03)).apply { setSortType(sort) }
		val phrase = SearchPhrase.emptyPhrase(settings)
		val rows = listOf(row(phrase, "far", 49.87), row(phrase, "near", 49.841),
			row(phrase, "middle", 49.85), row(phrase, "next level", 49.8405, 1))
		return SearchResultCollection(phrase, true, 0).addSearchResults(rows, false, false)
	}

	private fun row(phrase: SearchPhrase, name: String, latitude: Double?, level: Int = 0) = SearchResult(phrase).apply {
		localeName = name
		location = latitude?.let { KLatLon(it, 24.03) }
		spatialSearchVisibleLevel = level
	}

	private fun names(collection: SearchResultCollection) = collection.getVisibleSpatialSearchResults().map { it.localeName }

	@Test fun nearestSortsOnlyVisibleRowsAndKeepsEngineOrderInStorage() {
		val results = collection(SearchSettings.SortType.ONLY_BY_DISTANCE)
		assertEquals(listOf("near", "middle", "far"), names(results))
		assertEquals(listOf("far", "near", "middle", "next level"), results.getCurrentSearchResults().map { it.localeName })
		results.showMoreSpatialSearchResults()
		assertEquals(listOf("next level", "near", "middle", "far"), names(results))
	}

	@Test fun relevanceKeepsEngineOrder() {
		assertEquals(listOf("far", "near", "middle"), names(collection(SearchSettings.SortType.BY_RELEVANCE)))
	}

	@Test fun nearestKeepsCategoryWithoutLocationFirst() {
		val results = collection(SearchSettings.SortType.ONLY_BY_DISTANCE)
		results.addSearchResults(listOf(row(results.getPhrase(), "category", null)), false, false)
		assertEquals(listOf("category", "near", "middle", "far"), names(results))
	}
}
