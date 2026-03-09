package net.osmand.plus.plugins.astronomy.search

import android.os.Bundle
import net.osmand.plus.plugins.astronomy.SkyObject
import net.osmand.plus.plugins.astronomy.SkyObject.Type
import java.time.ZonedDateTime
import java.util.Locale

internal enum class StarMapSearchSortMode {
	NAME_ASC,
	NAME_DESC,
	BRIGHTEST_FIRST,
	FAINTEST_FIRST,
	RISES_SOONEST,
	SETS_SOONEST
}

internal enum class StarMapSearchTypeFilter {
	SHOW_ALL,
	VISIBLE_NOW,
	VISIBLE_TONIGHT
}

internal enum class StarMapSearchCategoryFilter {
	ALL,
	SOLAR_SYSTEM,
	CONSTELLATIONS,
	STARS,
	NEBULAS,
	STAR_CLUSTERS,
	DEEP_SKY
}

internal enum class StarMapSearchQuickPresetType {
	NONE,
	WATCH_NOW,
	CATALOGS,
	CATEGORY_SOLAR_SYSTEM,
	CATEGORY_CONSTELLATIONS,
	CATEGORY_STARS,
	CATEGORY_NEBULAS,
	CATEGORY_STAR_CLUSTERS,
	CATEGORY_DEEP_SKY,
	MY_DATA_FAVORITES,
	MY_DATA_DAILY_PATH,
	MY_DATA_DIRECTIONS,
	CATALOG_WID
}

internal data class StarMapSearchEntry(
	val objectRef: SkyObject,
	val displayName: String,
	val magnitude: Float,
	val category: StarMapSearchCategoryFilter,
	val iconRes: Int,
	val iconColor: Int,
	val catalogWids: Set<String> = emptySet(),
	var nextRise: ZonedDateTime? = null,
	var nextSet: ZonedDateTime? = null,
	var isVisibleTonight: Boolean = false,
	var riseSetCalculated: Boolean = false,
	var visibleTonightCalculated: Boolean = false
)

internal data class StarMapSearchStateSnapshot(
	val query: String,
	val sortMode: StarMapSearchSortMode,
	val typeFilter: StarMapSearchTypeFilter,
	val nakedEyeOnly: Boolean,
	val quickPresetType: StarMapSearchQuickPresetType,
	val quickPresetCatalogWid: String?,
	val selectedCategories: Set<StarMapSearchCategoryFilter>
) {

	fun filterAndSort(
		preparedEntries: List<StarMapSearchEntry>,
		visibleTonightProvider: (StarMapSearchEntry) -> Boolean,
		riseSortValueProvider: (StarMapSearchEntry) -> Long,
		setSortValueProvider: (StarMapSearchEntry) -> Long
	): List<StarMapSearchEntry> {
		val filteredEntries = mutableListOf<StarMapSearchEntry>()
		val queryLower = query.lowercase(Locale.getDefault())
		val specificCategories = selectedCategories.filter { it != StarMapSearchCategoryFilter.ALL }.toSet()

		for (entry in preparedEntries) {
			if (!matchesQuickPreset(entry)) continue
			if (queryLower.isNotEmpty() && !matchesQuery(entry, queryLower)) continue
			if (!matchesTypeFilter(entry, visibleTonightProvider)) continue
			if (nakedEyeOnly && entry.magnitude > 6.0f) continue
			if (specificCategories.isNotEmpty() && entry.category !in specificCategories) continue
			filteredEntries.add(entry)
		}

		filteredEntries.sortWith(createComparator(riseSortValueProvider, setSortValueProvider))
		return filteredEntries
	}

	private fun matchesQuickPreset(entry: StarMapSearchEntry): Boolean {
		return when (quickPresetType) {
			StarMapSearchQuickPresetType.NONE -> true
			StarMapSearchQuickPresetType.WATCH_NOW -> true
			StarMapSearchQuickPresetType.CATALOGS -> false
			StarMapSearchQuickPresetType.CATEGORY_SOLAR_SYSTEM -> entry.category == StarMapSearchCategoryFilter.SOLAR_SYSTEM
			StarMapSearchQuickPresetType.CATEGORY_CONSTELLATIONS -> entry.category == StarMapSearchCategoryFilter.CONSTELLATIONS
			StarMapSearchQuickPresetType.CATEGORY_STARS -> entry.category == StarMapSearchCategoryFilter.STARS
			StarMapSearchQuickPresetType.CATEGORY_NEBULAS -> entry.category == StarMapSearchCategoryFilter.NEBULAS
			StarMapSearchQuickPresetType.CATEGORY_STAR_CLUSTERS -> entry.category == StarMapSearchCategoryFilter.STAR_CLUSTERS
			StarMapSearchQuickPresetType.CATEGORY_DEEP_SKY -> entry.category == StarMapSearchCategoryFilter.DEEP_SKY
			StarMapSearchQuickPresetType.MY_DATA_FAVORITES -> entry.objectRef.isFavorite
			StarMapSearchQuickPresetType.MY_DATA_DAILY_PATH -> entry.objectRef.showCelestialPath
			StarMapSearchQuickPresetType.MY_DATA_DIRECTIONS -> entry.objectRef.showDirection
			StarMapSearchQuickPresetType.CATALOG_WID -> !quickPresetCatalogWid.isNullOrEmpty() && quickPresetCatalogWid in entry.catalogWids
		}
	}

	private fun matchesQuery(entry: StarMapSearchEntry, queryLower: String): Boolean {
		val localized = entry.objectRef.localizedName.orEmpty().lowercase(Locale.getDefault())
		val original = entry.objectRef.name.lowercase(Locale.getDefault())
		return localized.contains(queryLower) || original.contains(queryLower)
	}

	private fun matchesTypeFilter(
		entry: StarMapSearchEntry,
		visibleTonightProvider: (StarMapSearchEntry) -> Boolean
	): Boolean {
		return when (typeFilter) {
			StarMapSearchTypeFilter.SHOW_ALL -> true
			StarMapSearchTypeFilter.VISIBLE_NOW -> entry.objectRef.altitude > 0
			StarMapSearchTypeFilter.VISIBLE_TONIGHT -> visibleTonightProvider(entry)
		}
	}

	private fun createComparator(
		riseSortValueProvider: (StarMapSearchEntry) -> Long,
		setSortValueProvider: (StarMapSearchEntry) -> Long
	): Comparator<StarMapSearchEntry> {
		return when (sortMode) {
			StarMapSearchSortMode.NAME_ASC -> compareBy { it.displayName.lowercase(Locale.getDefault()) }
			StarMapSearchSortMode.NAME_DESC -> compareByDescending { it.displayName.lowercase(Locale.getDefault()) }
			StarMapSearchSortMode.BRIGHTEST_FIRST -> compareBy { it.magnitude }
			StarMapSearchSortMode.FAINTEST_FIRST -> compareByDescending { it.magnitude }
			StarMapSearchSortMode.RISES_SOONEST -> compareBy(
				{ riseSortValueProvider(it) },
				{ it.displayName.lowercase(Locale.getDefault()) }
			)
			StarMapSearchSortMode.SETS_SOONEST -> compareBy(
				{ setSortValueProvider(it) },
				{ it.displayName.lowercase(Locale.getDefault()) }
			)
		}
	}
}

internal class StarMapSearchState(savedInstanceState: Bundle? = null) {

	companion object {
		private const val KEY_QUERY = "query"
		private const val KEY_SORT = "sort"
		private const val KEY_TYPE_FILTER = "type_filter"
		private const val KEY_NAKED_EYE = "naked_eye"
		private const val KEY_CATEGORIES = "categories"
		private const val KEY_QUICK_PRESET = "quick_preset"
		private const val KEY_QUICK_CATALOG = "quick_catalog"
		private const val KEY_RECENT_CHIPS = "recent_chips"
		private const val MAX_RECENT_CHIPS = 8
	}

	var query: String = ""
	var sortMode: StarMapSearchSortMode = StarMapSearchSortMode.NAME_ASC
	var typeFilter: StarMapSearchTypeFilter = StarMapSearchTypeFilter.SHOW_ALL
	var nakedEyeOnly: Boolean = false
	var quickPresetType: StarMapSearchQuickPresetType = StarMapSearchQuickPresetType.NONE
	var quickPresetCatalogWid: String? = null
	val selectedCategories = linkedSetOf(StarMapSearchCategoryFilter.ALL)
	val recentChips = mutableListOf<String>()

	init {
		restore(savedInstanceState)
	}

	fun save(outState: Bundle) {
		outState.putString(KEY_QUERY, query)
		outState.putString(KEY_SORT, sortMode.name)
		outState.putString(KEY_TYPE_FILTER, typeFilter.name)
		outState.putBoolean(KEY_NAKED_EYE, nakedEyeOnly)
		outState.putStringArrayList(KEY_CATEGORIES, ArrayList(selectedCategories.map { it.name }))
		outState.putString(KEY_QUICK_PRESET, quickPresetType.name)
		outState.putString(KEY_QUICK_CATALOG, quickPresetCatalogWid)
		outState.putStringArrayList(KEY_RECENT_CHIPS, ArrayList(recentChips))
	}

	fun restore(savedInstanceState: Bundle?) {
		if (savedInstanceState == null) {
			return
		}
		query = savedInstanceState.getString(KEY_QUERY).orEmpty()
		sortMode = savedInstanceState.getString(KEY_SORT)?.let {
			runCatching { StarMapSearchSortMode.valueOf(it) }.getOrNull()
		} ?: StarMapSearchSortMode.NAME_ASC
		typeFilter = savedInstanceState.getString(KEY_TYPE_FILTER)?.let {
			runCatching { StarMapSearchTypeFilter.valueOf(it) }.getOrNull()
		} ?: StarMapSearchTypeFilter.SHOW_ALL
		nakedEyeOnly = savedInstanceState.getBoolean(KEY_NAKED_EYE, false)
		quickPresetType = savedInstanceState.getString(KEY_QUICK_PRESET)?.let {
			runCatching { StarMapSearchQuickPresetType.valueOf(it) }.getOrNull()
		} ?: StarMapSearchQuickPresetType.NONE
		quickPresetCatalogWid = savedInstanceState.getString(KEY_QUICK_CATALOG)

		selectedCategories.clear()
		savedInstanceState.getStringArrayList(KEY_CATEGORIES)
			?.mapNotNull { runCatching { StarMapSearchCategoryFilter.valueOf(it) }.getOrNull() }
			?.forEach { selectedCategories.add(it) }
		if (selectedCategories.isEmpty()) {
			selectedCategories.add(StarMapSearchCategoryFilter.ALL)
		}

		recentChips.clear()
		recentChips.addAll(savedInstanceState.getStringArrayList(KEY_RECENT_CHIPS).orEmpty())
	}

	fun selectQuickPreset(quickPresetType: StarMapSearchQuickPresetType, catalogWid: String?) {
		this.quickPresetType = quickPresetType
		quickPresetCatalogWid = catalogWid
		query = ""
	}

	fun prepareForExploreEntry(quickPresetType: StarMapSearchQuickPresetType, catalogWid: String?) {
		query = ""
		sortMode = if (quickPresetType == StarMapSearchQuickPresetType.WATCH_NOW) {
			StarMapSearchSortMode.BRIGHTEST_FIRST
		} else {
			StarMapSearchSortMode.NAME_ASC
		}
		typeFilter = if (quickPresetType == StarMapSearchQuickPresetType.WATCH_NOW) {
			StarMapSearchTypeFilter.VISIBLE_TONIGHT
		} else {
			StarMapSearchTypeFilter.SHOW_ALL
		}
		nakedEyeOnly = false
		this.quickPresetType = quickPresetType
		quickPresetCatalogWid = catalogWid
		selectedCategories.clear()
		selectedCategories.add(categoryPreset() ?: StarMapSearchCategoryFilter.ALL)
	}

	fun shouldOpenInBrowseMode(): Boolean {
		return quickPresetType == StarMapSearchQuickPresetType.WATCH_NOW ||
			quickPresetType == StarMapSearchQuickPresetType.CATALOGS ||
			isCategoryPreset() ||
			quickPresetType == StarMapSearchQuickPresetType.CATALOG_WID ||
			quickPresetType == StarMapSearchQuickPresetType.MY_DATA_FAVORITES ||
			quickPresetType == StarMapSearchQuickPresetType.MY_DATA_DAILY_PATH ||
			quickPresetType == StarMapSearchQuickPresetType.MY_DATA_DIRECTIONS
	}

	fun hasBrowseContext(): Boolean = quickPresetType != StarMapSearchQuickPresetType.NONE

	fun isCategoryPreset(): Boolean = categoryPreset() != null

	fun categoryPreset(): StarMapSearchCategoryFilter? {
		return when (quickPresetType) {
			StarMapSearchQuickPresetType.CATEGORY_SOLAR_SYSTEM -> StarMapSearchCategoryFilter.SOLAR_SYSTEM
			StarMapSearchQuickPresetType.CATEGORY_CONSTELLATIONS -> StarMapSearchCategoryFilter.CONSTELLATIONS
			StarMapSearchQuickPresetType.CATEGORY_STARS -> StarMapSearchCategoryFilter.STARS
			StarMapSearchQuickPresetType.CATEGORY_NEBULAS -> StarMapSearchCategoryFilter.NEBULAS
			StarMapSearchQuickPresetType.CATEGORY_STAR_CLUSTERS -> StarMapSearchCategoryFilter.STAR_CLUSTERS
			StarMapSearchQuickPresetType.CATEGORY_DEEP_SKY -> StarMapSearchCategoryFilter.DEEP_SKY
			else -> null
		}
	}

	fun snapshot(): StarMapSearchStateSnapshot {
		return StarMapSearchStateSnapshot(
			query = query,
			sortMode = sortMode,
			typeFilter = typeFilter,
			nakedEyeOnly = nakedEyeOnly,
			quickPresetType = quickPresetType,
			quickPresetCatalogWid = quickPresetCatalogWid,
			selectedCategories = selectedCategories.toSet()
		)
	}

	fun filterAndSort(
		preparedEntries: List<StarMapSearchEntry>,
		visibleTonightProvider: (StarMapSearchEntry) -> Boolean,
		riseSortValueProvider: (StarMapSearchEntry) -> Long,
		setSortValueProvider: (StarMapSearchEntry) -> Long
	): List<StarMapSearchEntry> {
		return snapshot().filterAndSort(
			preparedEntries = preparedEntries,
			visibleTonightProvider = visibleTonightProvider,
			riseSortValueProvider = riseSortValueProvider,
			setSortValueProvider = setSortValueProvider
		)
	}

	fun calculateFilterCount(): Int {
		var count = 0
		if (
			quickPresetType != StarMapSearchQuickPresetType.NONE &&
			quickPresetType != StarMapSearchQuickPresetType.CATALOGS &&
			quickPresetType != StarMapSearchQuickPresetType.WATCH_NOW &&
			!isCategoryPreset()
		) {
			count++
		}
		if (typeFilter != StarMapSearchTypeFilter.SHOW_ALL) count++
		if (nakedEyeOnly) count++
		if (selectedCategories.any { it != StarMapSearchCategoryFilter.ALL }) count++
		return count
	}

	fun reset() {
		query = ""
		sortMode = StarMapSearchSortMode.NAME_ASC
		typeFilter = StarMapSearchTypeFilter.SHOW_ALL
		nakedEyeOnly = false
		quickPresetType = StarMapSearchQuickPresetType.NONE
		quickPresetCatalogWid = null
		selectedCategories.clear()
		selectedCategories.add(StarMapSearchCategoryFilter.ALL)
	}

	fun addRecentChip(label: String) {
		val normalized = label.trim()
		if (normalized.isEmpty()) {
			return
		}
		recentChips.removeAll { it.equals(normalized, ignoreCase = true) }
		recentChips.add(0, normalized)
		while (recentChips.size > MAX_RECENT_CHIPS) {
			recentChips.removeAt(recentChips.lastIndex)
		}
	}

	fun toggleCategoryFilter(categoryFilter: StarMapSearchCategoryFilter) {
		if (categoryFilter == StarMapSearchCategoryFilter.ALL) {
			selectedCategories.clear()
			selectedCategories.add(StarMapSearchCategoryFilter.ALL)
			return
		}

		selectedCategories.remove(StarMapSearchCategoryFilter.ALL)
		if (selectedCategories.contains(categoryFilter)) {
			selectedCategories.remove(categoryFilter)
		} else {
			selectedCategories.add(categoryFilter)
		}
		if (selectedCategories.isEmpty()) {
			selectedCategories.add(StarMapSearchCategoryFilter.ALL)
		}
	}
}

internal fun mapStarMapSearchCategory(obj: SkyObject): StarMapSearchCategoryFilter {
	return when (obj.type) {
		Type.SUN,
		Type.MOON,
		Type.PLANET -> StarMapSearchCategoryFilter.SOLAR_SYSTEM
		Type.CONSTELLATION -> StarMapSearchCategoryFilter.CONSTELLATIONS
		Type.STAR -> StarMapSearchCategoryFilter.STARS
		Type.NEBULA -> StarMapSearchCategoryFilter.NEBULAS
		Type.OPEN_CLUSTER,
		Type.GLOBULAR_CLUSTER -> StarMapSearchCategoryFilter.STAR_CLUSTERS
		else -> StarMapSearchCategoryFilter.DEEP_SKY
	}
}
