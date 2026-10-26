package net.osmand.plus.mapcontextmenu.editors.icon

import net.osmand.CollatorStringMatcher
import net.osmand.CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE
import net.osmand.OnResultCallback
import net.osmand.PlatformUtil
import net.osmand.osm.PoiType
import net.osmand.plus.OsmandApplication
import net.osmand.plus.mapcontextmenu.editors.icon.EditorIconController.LAST_USED_KEY
import net.osmand.plus.mapcontextmenu.editors.icon.EditorIconController.ORIGINAL_KEY
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconSearchResult
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconsCategory
import net.osmand.plus.utils.AndroidUtils
import net.osmand.search.SearchUICore.SearchResultComparator
import net.osmand.search.SearchUICore.SearchResultMatcher
import net.osmand.search.core.SearchCoreFactory.SearchAmenityTypesAPI
import net.osmand.search.core.SearchPhrase
import net.osmand.search.core.SearchSettings
import net.osmand.util.Algorithms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext

class EditorIconSearcher(
	private val app: OsmandApplication,
	private val controller: EditorIconController
) {

	private val poiTypesSearch = SearchAmenityTypesAPI(app.poiTypes)
	private val searchDispatcher = Dispatchers.Default.limitedParallelism(1)
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
	private var searchJob: Job? = null

	fun search(query: String, callback: OnResultCallback<List<IconSearchResult>>) {
		cancel()
		val icons = collectSelectableIcons()
		searchJob = scope.launch {
			val results = withContext(searchDispatcher) { searchIcons(query, icons) }
			callback.onResult(results)
		}
	}

	fun cancel() {
		searchJob?.cancel()
		searchJob = null
	}

	private fun collectSelectableIcons(): Map<String, SelectableIcon> {
		val icons = LinkedHashMap<String, SelectableIcon>()
		for (category in controller.getCategories()) {
			for (iconKey in category.iconKeys) {
				val known = icons[iconKey]
				if (known == null || known.category.key in FALLBACK_CATEGORY_KEYS) {
					icons[iconKey] = SelectableIcon(category, controller.isPoiIcon(iconKey))
				}
			}
		}
		return icons
	}

	private suspend fun searchIcons(query: String, icons: Map<String, SelectableIcon>): List<IconSearchResult> {
		val results = LinkedHashMap<String, IconSearchResult>()
		for (poiType in searchPoiTypes(query)) {
			EditorIconUtils.retrieveIconKey(poiType) { iconKey ->
				if (iconKey in icons) {
					results.putIfAbsent(iconKey, IconSearchResult(iconKey, poiType))
				}
			}
		}
		coroutineContext.ensureActive()
		val nameMatcher = CollatorStringMatcher(query, CHECK_STARTS_FROM_SPACE)
		for ((iconKey, icon) in icons) {
			if (icon.isPoiIcon || !matchesIconKey(nameMatcher, iconKey)) {
				continue
			}
			val iconId = AndroidUtils.getDrawableId(app, iconKey, 0)
			if (iconId != 0) {
				results.putIfAbsent(iconKey, IconSearchResult(iconKey, getIconName(iconKey), iconId,
					icon.category.key, icon.category.translation))
			}
		}
		return results.values.toList()
	}

	private fun searchPoiTypes(query: String): List<PoiType> {
		// Own settings without maps: the shared search core is spatial and needs a location
		val settings = SearchSettings(emptyList())
		val phrase = SearchPhrase.emptyPhrase(settings).generateNewPhrase(query, settings)
		val matcher = SearchResultMatcher(null, phrase, 0, AtomicInteger(), -1)
		try {
			poiTypesSearch.search(phrase, matcher)
		} catch (e: IOException) {
			LOG.error("Failed to search POI types", e)
		}
		return matcher.requestResults
			.sortedWith(SearchResultComparator(phrase))
			.mapNotNull { (it.`object` as? PoiType)?.takeUnless(PoiType::isAdditional) }
	}

	private class SelectableIcon(val category: IconsCategory, val isPoiIcon: Boolean)

	companion object {
		private val LOG = PlatformUtil.getLog(EditorIconSearcher::class.java)
		private val ICON_KEY_PREFIX = Regex("^(special_|activities_|ic_action_)")
		private val FALLBACK_CATEGORY_KEYS = setOf(LAST_USED_KEY, ORIGINAL_KEY)

		private fun matchesIconKey(matcher: CollatorStringMatcher, iconKey: String): Boolean =
			matcher.matches(iconKey) || matcher.matches(iconKey.replace('_', ' '))

		private fun getIconName(iconKey: String): String =
			Algorithms.capitalizeFirstLetter(iconKey.replace(ICON_KEY_PREFIX, "").replace('_', ' '))
	}
}
