package net.osmand.plus.plugins.astronomy

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import net.osmand.plus.R
import net.osmand.plus.base.BaseFullScreenDialogFragment
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.astronomy.utils.AstroUtils
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.utils.UiUtilities
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Locale
import androidx.core.graphics.drawable.toDrawable

class StarMapSearchDialogFragment : BaseFullScreenDialogFragment() {

	private enum class ScreenMode {
		EXPLORE,
		FULL_SEARCH
	}

	private enum class SortMode {
		NAME_ASC,
		NAME_DESC,
		BRIGHTEST_FIRST,
		FAINTEST_FIRST,
		RISES_SOONEST,
		SETS_SOONEST
	}

	private enum class TypeFilter {
		SHOW_ALL,
		VISIBLE_NOW,
		VISIBLE_TONIGHT
	}

	enum class CategoryFilter {
		ALL,
		SOLAR_SYSTEM,
		CONSTELLATIONS,
		STARS,
		NEBULAS,
		STAR_CLUSTERS,
		DEEP_SKY
	}

	private enum class QuickPresetType {
		NONE,
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

	data class SearchEntry(
		val objectRef: SkyObject,
		val displayName: String,
		val magnitude: Float,
		val category: CategoryFilter,
		val iconRes: Int,
		val iconColor: Int,
		val catalogWid: String?,
		var nextRise: ZonedDateTime? = null,
		var nextSet: ZonedDateTime? = null,
		var isVisibleTonight: Boolean = false,
		var riseSetCalculated: Boolean = false,
		var visibleTonightCalculated: Boolean = false
	)

	private lateinit var searchAdapter: SearchAdapter
	private val preparedEntries = mutableListOf<SearchEntry>()
	private val visibleEntries = mutableListOf<SearchEntry>()

	private lateinit var searchRecycler: RecyclerView
	private lateinit var exploreContainer: View
	private lateinit var fullSearchContainer: View
	private lateinit var exploreSearchCard: MaterialCardView
	private lateinit var exploreCloseButton: View
	private lateinit var exploreSearchIcon: View
	private lateinit var fullSearchBackButton: View
	private lateinit var fullSearchClearButton: View
	private lateinit var fullSearchEditText: EditText
	private lateinit var sortButton: View
	private lateinit var filterButton: View
	private lateinit var sortIcon: ImageView
	private lateinit var sortText: TextView
	private lateinit var filterText: TextView
	private lateinit var emptyStateContainer: View
	private lateinit var emptyStateResetButton: View
	private lateinit var recentChipsContainer: LinearLayout
	private lateinit var recentChipsScroll: View
	private lateinit var categoriesContainer: LinearLayout
	private lateinit var myDataContainer: LinearLayout
	private lateinit var catalogsContainer: LinearLayout
	private lateinit var catalogsViewAllRow: View
	private lateinit var catalogsViewAllCount: TextView

	private var sortPopup: PopupWindow? = null
	private var filterPopup: PopupWindow? = null

	private var currentMode: ScreenMode = ScreenMode.EXPLORE
	private var currentSortMode: SortMode = SortMode.NAME_ASC
	private var currentTypeFilter: TypeFilter = TypeFilter.SHOW_ALL
	private var currentNakedEyeOnly = false
	private val selectedCategories = linkedSetOf(CategoryFilter.ALL)
	private var currentQuickPresetType: QuickPresetType = QuickPresetType.NONE
	private var currentQuickPresetCatalogWid: String? = null
	private var currentQuery: String = ""
	private val recentChips = mutableListOf<String>()
	private var observerForComputations = Observer(0.0, 0.0, 0.0)
	private var nowForComputations: ZonedDateTime = ZonedDateTime.now()
	private var duskForComputations: ZonedDateTime = ZonedDateTime.now()
	private var dawnForComputations: ZonedDateTime = ZonedDateTime.now().plusHours(12)

	private val dataProvider: AstroDataProvider by lazy {
		PluginsHelper.requirePlugin(AstronomyPlugin::class.java).dataProvider
	}

	private val astroSettings: AstronomyPluginSettings by lazy {
		PluginsHelper.requirePlugin(AstronomyPlugin::class.java).astroSettings
	}

	var onObjectSelected: ((SkyObject) -> Unit)? = null

	companion object {
		const val TAG = "StarMapSearchDialog"

		private const val KEY_MODE = "mode"
		private const val KEY_QUERY = "query"
		private const val KEY_SORT = "sort"
		private const val KEY_TYPE_FILTER = "type_filter"
		private const val KEY_NAKED_EYE = "naked_eye"
		private const val KEY_CATEGORIES = "categories"
		private const val KEY_QUICK_PRESET = "quick_preset"
		private const val KEY_QUICK_CATALOG = "quick_catalog"
		private const val KEY_RECENT_CHIPS = "recent_chips"
	}

	override fun getThemeUsageContext(): ThemeUsageContext = ThemeUsageContext.APP

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		restoreState(savedInstanceState)
	}

	override fun createDialog(savedInstanceState: Bundle?): Dialog {
		return object : Dialog(requireContext(), theme) {
			override fun onBackPressed() {
				if (!handleBackPressedInternal()) {
					super.onBackPressed()
				}
			}
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putString(KEY_MODE, currentMode.name)
		outState.putString(KEY_QUERY, currentQuery)
		outState.putString(KEY_SORT, currentSortMode.name)
		outState.putString(KEY_TYPE_FILTER, currentTypeFilter.name)
		outState.putBoolean(KEY_NAKED_EYE, currentNakedEyeOnly)
		outState.putStringArrayList(KEY_CATEGORIES, ArrayList(selectedCategories.map { it.name }))
		outState.putString(KEY_QUICK_PRESET, currentQuickPresetType.name)
		outState.putString(KEY_QUICK_CATALOG, currentQuickPresetCatalogWid)
		outState.putStringArrayList(KEY_RECENT_CHIPS, ArrayList(recentChips))
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		return themedInflater.inflate(R.layout.dialog_star_map_search, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		bindViews(view)
		refreshPreparedEntries()
		setupSearchRecycler()
		setupExploreContent()
		setupListeners()
		renderRecentChips()
		applyMode(currentMode, requestKeyboard = currentMode == ScreenMode.FULL_SEARCH)
		applyFiltersAndSort(scrollToTop = false)
	}

	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (hidden) {
			dialog?.hide()
			return
		}
		dialog?.show()
		refreshPreparedEntries()
		setupMyDataRows()
		setupCatalogRows()
		applyFiltersAndSort(scrollToTop = false)
	}

	override fun onDestroyView() {
		dismissPopups()
		super.onDestroyView()
	}

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = super.getInsetTargets()
		collection.add(InsetTarget.createScrollable(R.id.search_results))
		collection.add(InsetTarget.createScrollable(R.id.explore_container))
		return collection
	}

	private fun bindViews(root: View) {
		exploreContainer = root.findViewById(R.id.explore_container)
		fullSearchContainer = root.findViewById(R.id.full_search_container)
		exploreSearchCard = root.findViewById(R.id.explore_search_card)
		exploreCloseButton = root.findViewById(R.id.explore_close_button)
		exploreSearchIcon = root.findViewById(R.id.explore_search_icon)
		fullSearchBackButton = root.findViewById(R.id.full_search_back_button)
		fullSearchClearButton = root.findViewById(R.id.full_search_clear_button)
		fullSearchEditText = root.findViewById(R.id.full_search_edit_text)
		sortButton = root.findViewById(R.id.sort_button)
		filterButton = root.findViewById(R.id.filter_button)
		sortIcon = root.findViewById(R.id.sort_icon)
		sortText = root.findViewById(R.id.sort_text)
		filterText = root.findViewById(R.id.filter_text)
		emptyStateContainer = root.findViewById(R.id.empty_state_container)
		emptyStateResetButton = root.findViewById(R.id.empty_state_reset_button)
		recentChipsContainer = root.findViewById(R.id.recent_chips_container)
		recentChipsScroll = root.findViewById(R.id.recent_chips_scroll)
		searchRecycler = root.findViewById(R.id.search_results)
		categoriesContainer = root.findViewById(R.id.categories_rows_container)
		myDataContainer = root.findViewById(R.id.my_data_rows_container)
		catalogsContainer = root.findViewById(R.id.catalogs_rows_container)
		catalogsViewAllRow = root.findViewById(R.id.catalogs_view_all_row)
		catalogsViewAllCount = root.findViewById(R.id.catalogs_view_all_count)
	}

	private fun setupSearchRecycler() {
		searchAdapter = SearchAdapter()
		searchRecycler.layoutManager = LinearLayoutManager(context)
		searchRecycler.adapter = searchAdapter
		searchRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
				if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
					AndroidUtils.hideSoftKeyboard(requireActivity(), fullSearchEditText)
				}
			}
		})
	}

	private fun setupExploreContent() {
		setupCategoryRows()
		setupMyDataRows()
		setupCatalogRows()
	}

	private fun setupListeners() {
		exploreCloseButton.setOnClickListener { dismiss() }
		exploreSearchCard.setOnClickListener { openFullSearch(QuickPresetType.NONE, null) }
		exploreSearchIcon.setOnClickListener { openFullSearch(QuickPresetType.NONE, null) }
		fullSearchBackButton.setOnClickListener { applyMode(ScreenMode.EXPLORE, requestKeyboard = false) }
		fullSearchClearButton.setOnClickListener {
			if (currentQuery.isNotEmpty()) {
				fullSearchEditText.setText("")
			}
		}
		emptyStateResetButton.setOnClickListener { resetAllSearchParams() }
		sortButton.setOnClickListener { showSortPopup(sortButton) }
		filterButton.setOnClickListener { showFilterPopup(filterButton) }

		fullSearchEditText.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
			override fun afterTextChanged(s: Editable?) {
				val newQuery = s?.toString().orEmpty()
				if (newQuery != currentQuery) {
					currentQuery = newQuery
					applyFiltersAndSort(scrollToTop = true)
				}
			}
		})
	}

	private fun restoreState(savedInstanceState: Bundle?) {
		if (savedInstanceState == null) {
			return
		}
		currentMode = savedInstanceState.getString(KEY_MODE)?.let { runCatching { ScreenMode.valueOf(it) }.getOrNull() }
			?: ScreenMode.EXPLORE
		currentQuery = savedInstanceState.getString(KEY_QUERY).orEmpty()
		currentSortMode = savedInstanceState.getString(KEY_SORT)?.let { runCatching { SortMode.valueOf(it) }.getOrNull() }
			?: SortMode.NAME_ASC
		currentTypeFilter = savedInstanceState.getString(KEY_TYPE_FILTER)?.let { runCatching { TypeFilter.valueOf(it) }.getOrNull() }
			?: TypeFilter.SHOW_ALL
		currentNakedEyeOnly = savedInstanceState.getBoolean(KEY_NAKED_EYE, false)

		selectedCategories.clear()
		savedInstanceState.getStringArrayList(KEY_CATEGORIES)
			?.mapNotNull { runCatching { CategoryFilter.valueOf(it) }.getOrNull() }
			?.forEach { selectedCategories.add(it) }
		if (selectedCategories.isEmpty()) {
			selectedCategories.add(CategoryFilter.ALL)
		}

		currentQuickPresetType = savedInstanceState.getString(KEY_QUICK_PRESET)?.let {
			runCatching { QuickPresetType.valueOf(it) }.getOrNull()
		} ?: QuickPresetType.NONE
		currentQuickPresetCatalogWid = savedInstanceState.getString(KEY_QUICK_CATALOG)

		recentChips.clear()
		recentChips.addAll(savedInstanceState.getStringArrayList(KEY_RECENT_CHIPS).orEmpty())
	}

	private fun setupCategoryRows() {
		categoriesContainer.removeAllViews()
		addExploreRow(
			container = categoriesContainer,
			iconRes = R.drawable.ic_action_planet_outlined,
			title = getString(R.string.astro_solar_system),
			subtitle = null,
			count = null
		) {
			openFullSearch(QuickPresetType.CATEGORY_SOLAR_SYSTEM, null)
		}
		addExploreRow(
			container = categoriesContainer,
			iconRes = R.drawable.ic_action_constellations,
			title = getString(R.string.astro_constellations),
			subtitle = null,
			count = null
		) {
			openFullSearch(QuickPresetType.CATEGORY_CONSTELLATIONS, null)
		}
		addExploreRow(
			container = categoriesContainer,
			iconRes = R.drawable.ic_action_stars,
			title = getString(R.string.astro_stars),
			subtitle = null,
			count = null
		) {
			openFullSearch(QuickPresetType.CATEGORY_STARS, null)
		}
		addExploreRow(
			container = categoriesContainer,
			iconRes = R.drawable.ic_action_nebulas,
			title = getString(R.string.astro_nebulas),
			subtitle = null,
			count = null
		) {
			openFullSearch(QuickPresetType.CATEGORY_NEBULAS, null)
		}
		addExploreRow(
			container = categoriesContainer,
			iconRes = R.drawable.ic_action_star_clusters,
			title = getString(R.string.astro_star_clusters),
			subtitle = null,
			count = null
		) {
			openFullSearch(QuickPresetType.CATEGORY_STAR_CLUSTERS, null)
		}
		addExploreRow(
			container = categoriesContainer,
			iconRes = R.drawable.ic_action_galaxy,
			title = getString(R.string.astro_deep_sky),
			subtitle = getString(R.string.astro_explore_deep_sky_subtitle),
			count = null,
			showDivider = false
		) {
			openFullSearch(QuickPresetType.CATEGORY_DEEP_SKY, null)
		}
	}

	private fun setupMyDataRows() {
		myDataContainer.removeAllViews()
		val config = astroSettings.getStarMapConfig()
		addExploreRow(
			container = myDataContainer,
			iconRes = R.drawable.ic_action_bookmark_filled,
			title = getString(R.string.favorites_item),
			subtitle = null,
			count = config.favorites.size,
			showDivider = true
		) {
			openFullSearch(QuickPresetType.MY_DATA_FAVORITES, null)
		}
		addExploreRow(
			container = myDataContainer,
			iconRes = R.drawable.ic_action_target_path_on,
			title = getString(R.string.astro_daily_path),
			subtitle = null,
			count = config.celestialPaths.size,
			showDivider = true
		) {
			openFullSearch(QuickPresetType.MY_DATA_DAILY_PATH, null)
		}
		addExploreRow(
			container = myDataContainer,
			iconRes = R.drawable.ic_action_target_direction_on,
			title = getString(R.string.astro_directions),
			subtitle = null,
			count = config.directions.size,
			showDivider = false
		) {
			openFullSearch(QuickPresetType.MY_DATA_DIRECTIONS, null)
		}
	}

	private fun setupCatalogRows() {
		catalogsContainer.removeAllViews()
		val catalogs = dataProvider.getCatalogs(requireContext())
		catalogs.take(3).forEachIndexed { index, catalog ->
			addExploreRow(
				container = catalogsContainer,
				iconRes = R.drawable.ic_action_book_info,
				title = catalog.name,
				subtitle = null,
				count = null,
				showDivider = index != 2
			) {
				openFullSearch(QuickPresetType.CATALOG_WID, catalog.wid)
			}
		}
		catalogsViewAllCount.text = catalogs.size.toString()
		catalogsViewAllRow.setOnClickListener {
			openFullSearch(QuickPresetType.NONE, null)
		}
	}

	private fun addExploreRow(
		container: LinearLayout,
		iconRes: Int,
		title: String,
		subtitle: String?,
		count: Int?,
		showDivider: Boolean = true,
		onClick: (() -> Unit)?
	) {
		val row = themedInflater.inflate(R.layout.item_astro_explore_row, container, false)
		val icon = row.findViewById<ImageView>(R.id.row_icon)
		val titleView = row.findViewById<TextView>(R.id.row_title)
		val subtitleView = row.findViewById<TextView>(R.id.row_subtitle)
		val countView = row.findViewById<TextView>(R.id.row_count)
		val divider = row.findViewById<View>(R.id.row_divider)

		icon.setImageDrawable(app.uiUtilities.getIcon(iconRes, ColorUtilities.getActiveIconColorId(nightMode)))
		titleView.text = title
		subtitleView.text = subtitle
		subtitleView.isVisible = !subtitle.isNullOrEmpty()

		if (count != null) {
			countView.isVisible = true
			countView.text = count.toString()
		} else {
			countView.isVisible = false
		}
		divider.isVisible = showDivider

		if (onClick != null) {
			row.setOnClickListener { onClick.invoke() }
		} else {
			row.setOnClickListener(null)
			row.isClickable = false
			row.isFocusable = false
		}
		container.addView(row)
	}

	private fun openFullSearch(quickPresetType: QuickPresetType, catalogWid: String?) {
		currentQuickPresetType = quickPresetType
		currentQuickPresetCatalogWid = catalogWid
		currentQuery = ""
		applyMode(ScreenMode.FULL_SEARCH, requestKeyboard = true)
		fullSearchEditText.setText("")
		applyFiltersAndSort(scrollToTop = true)
	}

	private fun applyMode(mode: ScreenMode, requestKeyboard: Boolean) {
		currentMode = mode
		exploreContainer.isVisible = mode == ScreenMode.EXPLORE
		fullSearchContainer.isVisible = mode == ScreenMode.FULL_SEARCH
		if (mode == ScreenMode.FULL_SEARCH) {
			if (fullSearchEditText.text.toString() != currentQuery) {
				fullSearchEditText.setText(currentQuery)
				fullSearchEditText.setSelection(fullSearchEditText.length())
			}
			if (requestKeyboard) {
				fullSearchEditText.requestFocus()
				AndroidUtils.showSoftKeyboard(requireActivity(), fullSearchEditText)
			}
		} else {
			AndroidUtils.hideSoftKeyboard(requireActivity(), fullSearchEditText)
			dismissPopups()
		}
		updateSortControls()
		updateFilterControls()
		updateEmptyStateVisibility()
	}

	private fun handleBackPressedInternal(): Boolean {
		if (!::fullSearchContainer.isInitialized) {
			return false
		}
		if (sortPopup?.isShowing == true) {
			dismissSortPopup()
			return true
		}
		if (filterPopup?.isShowing == true) {
			dismissFilterPopup()
			return true
		}
		return if (currentMode == ScreenMode.FULL_SEARCH) {
			applyMode(ScreenMode.EXPLORE, requestKeyboard = false)
			true
		} else {
			false
		}
	}

	private fun refreshPreparedEntries() {
		preparedEntries.clear()
		val parent = parentFragment as? StarMapFragment
		val objects = parent?.getSearchableObjects().orEmpty()

		val observer = parent?.starView?.observer ?: Observer(0.0, 0.0, 0.0)
		val currentCalendar = parent?.viewModel?.currentCalendar?.value ?: Calendar.getInstance()
		val zoneId = ZoneId.systemDefault()
		val now = currentCalendar.toInstant().atZone(zoneId)
		observerForComputations = observer
		nowForComputations = now

		val dayStart = now.toLocalDate().atStartOfDay(zoneId)
		val dayEnd = dayStart.plusDays(1)
		val twilight = AstroUtils.computeTwilight(dayStart, dayEnd, observer, zoneId)
		val dusk = twilight.civilDusk ?: dayStart.withHour(18)
		val dawnRaw = twilight.civilDawn
		val dawn = when {
			dawnRaw == null -> dusk.plusHours(12)
			dawnRaw.isAfter(dusk) -> dawnRaw
			else -> dawnRaw.plusDays(1)
		}
		duskForComputations = dusk
		dawnForComputations = dawn

		for (obj in objects) {
			preparedEntries.add(
				SearchEntry(
					objectRef = obj,
					displayName = obj.localizedName ?: obj.name,
					magnitude = obj.magnitude,
					category = mapCategory(obj),
					iconRes = AstroUtils.getObjectTypeIcon(obj.type),
					iconColor = if (obj.type.isSunSystem()) obj.color else ColorUtilities.getPrimaryIconColor(requireContext(), nightMode),
					catalogWid = obj.catalog?.wid
				)
			)
		}
	}

	private fun mapCategory(obj: SkyObject): CategoryFilter {
		return when {
			obj.type.isSunSystem() -> CategoryFilter.SOLAR_SYSTEM
			obj.type == SkyObject.Type.CONSTELLATION -> CategoryFilter.CONSTELLATIONS
			obj.type == SkyObject.Type.STAR -> CategoryFilter.STARS
			obj.type == SkyObject.Type.NEBULA -> CategoryFilter.NEBULAS
			obj.type == SkyObject.Type.OPEN_CLUSTER || obj.type == SkyObject.Type.GLOBULAR_CLUSTER -> CategoryFilter.STAR_CLUSTERS
			else -> CategoryFilter.DEEP_SKY
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	private fun applyFiltersAndSort(scrollToTop: Boolean) {
		visibleEntries.clear()

		val queryLower = currentQuery.lowercase(Locale.getDefault())
		val specificCategories = selectedCategories.filter { it != CategoryFilter.ALL }.toSet()

		for (entry in preparedEntries) {
			if (!matchesQuickPreset(entry)) continue
			if (queryLower.isNotEmpty() && !matchesQuery(entry, queryLower)) continue
			if (!matchesTypeFilter(entry)) continue
			if (currentNakedEyeOnly && entry.magnitude > 6.0f) continue
			if (specificCategories.isNotEmpty() && entry.category !in specificCategories) continue
			visibleEntries.add(entry)
		}

		visibleEntries.sortWith(createComparator())
		if (::searchAdapter.isInitialized) {
			searchAdapter.notifyDataSetChanged()
		}
		if (scrollToTop && ::searchRecycler.isInitialized && visibleEntries.isNotEmpty()) {
			searchRecycler.scrollToPosition(0)
		}
		updateSortControls()
		updateFilterControls()
		updateEmptyStateVisibility()
	}

	private fun matchesQuickPreset(entry: SearchEntry): Boolean {
		return when (currentQuickPresetType) {
			QuickPresetType.NONE -> true
			QuickPresetType.CATEGORY_SOLAR_SYSTEM -> entry.category == CategoryFilter.SOLAR_SYSTEM
			QuickPresetType.CATEGORY_CONSTELLATIONS -> entry.category == CategoryFilter.CONSTELLATIONS
			QuickPresetType.CATEGORY_STARS -> entry.category == CategoryFilter.STARS
			QuickPresetType.CATEGORY_NEBULAS -> entry.category == CategoryFilter.NEBULAS
			QuickPresetType.CATEGORY_STAR_CLUSTERS -> entry.category == CategoryFilter.STAR_CLUSTERS
			QuickPresetType.CATEGORY_DEEP_SKY -> entry.category == CategoryFilter.DEEP_SKY
			QuickPresetType.MY_DATA_FAVORITES -> entry.objectRef.isFavorite
			QuickPresetType.MY_DATA_DAILY_PATH -> entry.objectRef.showCelestialPath
			QuickPresetType.MY_DATA_DIRECTIONS -> entry.objectRef.showDirection
			QuickPresetType.CATALOG_WID -> !currentQuickPresetCatalogWid.isNullOrEmpty() && entry.catalogWid == currentQuickPresetCatalogWid
		}
	}

	private fun matchesQuery(entry: SearchEntry, queryLower: String): Boolean {
		val localized = entry.objectRef.localizedName.orEmpty().lowercase(Locale.getDefault())
		val original = entry.objectRef.name.lowercase(Locale.getDefault())
		return localized.contains(queryLower) || original.contains(queryLower)
	}

	private fun matchesTypeFilter(entry: SearchEntry): Boolean {
		return when (currentTypeFilter) {
			TypeFilter.SHOW_ALL -> true
			TypeFilter.VISIBLE_NOW -> entry.objectRef.altitude > 0
			TypeFilter.VISIBLE_TONIGHT -> getVisibleTonight(entry)
		}
	}

	private fun createComparator(): Comparator<SearchEntry> {
		return when (currentSortMode) {
			SortMode.NAME_ASC -> compareBy { it.displayName.lowercase(Locale.getDefault()) }
			SortMode.NAME_DESC -> compareByDescending { it.displayName.lowercase(Locale.getDefault()) }
			SortMode.BRIGHTEST_FIRST -> compareBy<SearchEntry> { it.magnitude }
			SortMode.FAINTEST_FIRST -> compareByDescending<SearchEntry> { it.magnitude }
			SortMode.RISES_SOONEST -> compareBy<SearchEntry>({ getRiseSortValue(it) }, { it.displayName.lowercase(Locale.getDefault()) })
			SortMode.SETS_SOONEST -> compareBy<SearchEntry>({ getSetSortValue(it) }, { it.displayName.lowercase(Locale.getDefault()) })
		}
	}

	private fun ensureRiseSet(entry: SearchEntry) {
		if (entry.riseSetCalculated) {
			return
		}
		val (rise, set) = AstroUtils.nextRiseSet(entry.objectRef, nowForComputations, observerForComputations)
		entry.nextRise = rise
		entry.nextSet = set
		entry.riseSetCalculated = true
	}

	private fun getVisibleTonight(entry: SearchEntry): Boolean {
		if (entry.visibleTonightCalculated) {
			return entry.isVisibleTonight
		}
		val (nightRise, nightSet) = AstroUtils.nextRiseSet(
			entry.objectRef,
			duskForComputations,
			observerForComputations,
			duskForComputations,
			dawnForComputations
		)
		val visibleAtDusk = AstroUtils.altitude(entry.objectRef, duskForComputations, observerForComputations) > 0
		entry.isVisibleTonight = visibleAtDusk || nightRise != null || nightSet != null
		entry.visibleTonightCalculated = true
		return entry.isVisibleTonight
	}

	private fun getRiseSortValue(entry: SearchEntry): Long {
		ensureRiseSet(entry)
		return entry.nextRise?.toInstant()?.toEpochMilli() ?: Long.MAX_VALUE
	}

	private fun getSetSortValue(entry: SearchEntry): Long {
		ensureRiseSet(entry)
		return entry.nextSet?.toInstant()?.toEpochMilli() ?: Long.MAX_VALUE
	}

	private fun updateSortControls() {
		if (!::sortText.isInitialized || !::sortIcon.isInitialized) return
		val (iconRes, textRes) = when (currentSortMode) {
			SortMode.NAME_ASC -> R.drawable.ic_action_sort_by_name_ascending to R.string.sort_name_ascending
			SortMode.NAME_DESC -> R.drawable.ic_action_sort_by_name_descending to R.string.sort_name_descending
			SortMode.BRIGHTEST_FIRST -> R.drawable.ic_action_sort_short_to_long to R.string.astro_sort_brightest_first
			SortMode.FAINTEST_FIRST -> R.drawable.ic_action_sort_long_to_short to R.string.astro_sort_faintest_first
			SortMode.RISES_SOONEST -> R.drawable.ic_action_sort_date_1 to R.string.astro_sort_rises_soonest
			SortMode.SETS_SOONEST -> R.drawable.ic_action_sort_date_31 to R.string.astro_sort_sets_soonest
		}
		sortText.setText(textRes)
		sortIcon.setImageDrawable(app.uiUtilities.getIcon(iconRes, ColorUtilities.getActiveIconColorId(nightMode)))
	}

	private fun updateFilterControls() {
		if (!::filterText.isInitialized) return
		val filterCount = calculateFilterCount()
		filterText.text = getString(R.string.filter_tracks_count, filterCount)
	}

	private fun calculateFilterCount(): Int {
		var count = 0
		if (currentTypeFilter != TypeFilter.SHOW_ALL) count++
		if (currentNakedEyeOnly) count++
		val specificCategories = selectedCategories.filter { it != CategoryFilter.ALL }
		if (specificCategories.isNotEmpty()) count++
		return count
	}

	private fun updateEmptyStateVisibility() {
		if (!::emptyStateContainer.isInitialized) return
		emptyStateContainer.isVisible = currentMode == ScreenMode.FULL_SEARCH && visibleEntries.isEmpty()
	}

	private fun resetAllSearchParams() {
		currentQuery = ""
		currentSortMode = SortMode.NAME_ASC
		currentTypeFilter = TypeFilter.SHOW_ALL
		currentNakedEyeOnly = false
		selectedCategories.clear()
		selectedCategories.add(CategoryFilter.ALL)
		currentQuickPresetType = QuickPresetType.NONE
		currentQuickPresetCatalogWid = null
		if (::fullSearchEditText.isInitialized) {
			fullSearchEditText.setText("")
		}
		applyFiltersAndSort(scrollToTop = true)
	}

	private fun addRecentChip(label: String) {
		val normalized = label.trim()
		if (normalized.isEmpty()) return
		recentChips.removeAll { it.equals(normalized, ignoreCase = true) }
		recentChips.add(0, normalized)
		while (recentChips.size > 8) {
			recentChips.removeAt(recentChips.lastIndex)
		}
		renderRecentChips()
	}

	private fun renderRecentChips() {
		if (!::recentChipsContainer.isInitialized) return
		recentChipsContainer.removeAllViews()
		recentChipsScroll.isVisible = recentChips.isNotEmpty()
		if (recentChips.isEmpty()) return

		recentChips.forEach { chipTitle ->
			val chipCard = MaterialCardView(requireContext()).apply {
				radius = resources.getDimension(R.dimen.content_padding)
				cardElevation = 0f
				setCardBackgroundColor(
					ColorUtilities.getColorWithAlpha(
						ColorUtilities.getActiveColor(app, nightMode),
						0.12f
					)
				)
			}
			val chipText = TextView(requireContext()).apply {
				text = chipTitle
				setTextColor(ColorUtilities.getActiveColor(app, nightMode))
				setPadding(
					resources.getDimensionPixelSize(R.dimen.content_padding),
					resources.getDimensionPixelSize(R.dimen.content_padding_small),
					resources.getDimensionPixelSize(R.dimen.content_padding),
					resources.getDimensionPixelSize(R.dimen.content_padding_small)
				)
				textSize = 14f
			}
			chipCard.addView(chipText)
			chipCard.setOnClickListener {
				currentQuickPresetType = QuickPresetType.NONE
				currentQuickPresetCatalogWid = null
				currentQuery = chipTitle
				applyMode(ScreenMode.FULL_SEARCH, requestKeyboard = true)
				fullSearchEditText.setText(chipTitle)
				fullSearchEditText.setSelection(chipTitle.length)
				applyFiltersAndSort(scrollToTop = true)
			}
			val lp = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			)
			lp.marginEnd = resources.getDimensionPixelSize(R.dimen.content_padding_small)
			recentChipsContainer.addView(chipCard, lp)
		}
	}

	private fun showSortPopup(anchor: View) {
		dismissSortPopup()
		val popupView = themedInflater.inflate(R.layout.popup_star_search_sort, null)
		val activeColor = ColorUtilities.getActiveColor(app, nightMode)

		val options = listOf(
			Triple(R.id.row_sort_name_asc, R.id.radio_sort_name_asc, SortMode.NAME_ASC),
			Triple(R.id.row_sort_name_desc, R.id.radio_sort_name_desc, SortMode.NAME_DESC),
			Triple(R.id.row_sort_brightest, R.id.radio_sort_brightest, SortMode.BRIGHTEST_FIRST),
			Triple(R.id.row_sort_faintest, R.id.radio_sort_faintest, SortMode.FAINTEST_FIRST),
			Triple(R.id.row_sort_rises, R.id.radio_sort_rises, SortMode.RISES_SOONEST),
			Triple(R.id.row_sort_sets, R.id.radio_sort_sets, SortMode.SETS_SOONEST)
		)

		options.forEach { (_, radioId, mode) ->
			val radio = popupView.findViewById<RadioButton>(radioId)
			UiUtilities.setupCompoundButton(nightMode, activeColor, radio)
			radio.isChecked = currentSortMode == mode
		}

		options.forEach { (rowId, _, mode) ->
			popupView.findViewById<View>(rowId).setOnClickListener {
				currentSortMode = mode
				updateSortControls()
				applyFiltersAndSort(scrollToTop = true)
				dismissSortPopup()
			}
		}

		sortPopup = createPopup(popupView)
		sortPopup?.showAsDropDown(anchor)
	}

	private fun showFilterPopup(anchor: View) {
		dismissFilterPopup()
		val popupView = themedInflater.inflate(R.layout.popup_star_search_filter, null)
		val activeColor = ColorUtilities.getActiveColor(app, nightMode)

		val typeOptions = listOf(
			Triple(R.id.row_filter_type_all, R.id.radio_filter_type_all, TypeFilter.SHOW_ALL),
			Triple(R.id.row_filter_type_now, R.id.radio_filter_type_now, TypeFilter.VISIBLE_NOW),
			Triple(R.id.row_filter_type_tonight, R.id.radio_filter_type_tonight, TypeFilter.VISIBLE_TONIGHT)
		)
		typeOptions.forEach { (_, radioId, type) ->
			val radio = popupView.findViewById<RadioButton>(radioId)
			UiUtilities.setupCompoundButton(nightMode, activeColor, radio)
			radio.isChecked = currentTypeFilter == type
		}
		typeOptions.forEach { (rowId, _, type) ->
			popupView.findViewById<View>(rowId).setOnClickListener {
				currentTypeFilter = type
				applyFiltersAndSort(scrollToTop = true)
				showFilterPopup(anchor)
			}
		}

		val nakedEyeCheck = popupView.findViewById<CheckBox>(R.id.checkbox_filter_naked_eye)
		UiUtilities.setupCompoundButton(nightMode, activeColor, nakedEyeCheck)
		nakedEyeCheck.isChecked = currentNakedEyeOnly
		popupView.findViewById<View>(R.id.row_filter_naked_eye).setOnClickListener {
			currentNakedEyeOnly = !currentNakedEyeOnly
			applyFiltersAndSort(scrollToTop = true)
			showFilterPopup(anchor)
		}

		setupCategoryCheckbox(
			popupView,
			R.id.row_filter_category_all,
			R.id.checkbox_filter_category_all,
			CategoryFilter.ALL,
			activeColor,
			anchor
		)
		setupCategoryCheckbox(
			popupView,
			R.id.row_filter_category_solar,
			R.id.checkbox_filter_category_solar,
			CategoryFilter.SOLAR_SYSTEM,
			activeColor,
			anchor
		)
		setupCategoryCheckbox(
			popupView,
			R.id.row_filter_category_constellations,
			R.id.checkbox_filter_category_constellations,
			CategoryFilter.CONSTELLATIONS,
			activeColor,
			anchor
		)
		setupCategoryCheckbox(
			popupView,
			R.id.row_filter_category_stars,
			R.id.checkbox_filter_category_stars,
			CategoryFilter.STARS,
			activeColor,
			anchor
		)
		setupCategoryCheckbox(
			popupView,
			R.id.row_filter_category_nebulas,
			R.id.checkbox_filter_category_nebulas,
			CategoryFilter.NEBULAS,
			activeColor,
			anchor
		)
		setupCategoryCheckbox(
			popupView,
			R.id.row_filter_category_clusters,
			R.id.checkbox_filter_category_clusters,
			CategoryFilter.STAR_CLUSTERS,
			activeColor,
			anchor
		)
		setupCategoryCheckbox(
			popupView,
			R.id.row_filter_category_deep_sky,
			R.id.checkbox_filter_category_deep_sky,
			CategoryFilter.DEEP_SKY,
			activeColor,
			anchor
		)

		filterPopup = createPopup(popupView)
		filterPopup?.showAsDropDown(anchor)
	}

	private fun setupCategoryCheckbox(
		popupView: View,
		rowId: Int,
		checkId: Int,
		categoryFilter: CategoryFilter,
		activeColor: Int,
		anchor: View
	) {
		val checkBox = popupView.findViewById<CheckBox>(checkId)
		UiUtilities.setupCompoundButton(nightMode, activeColor, checkBox)
		checkBox.isChecked = selectedCategories.contains(categoryFilter)

		popupView.findViewById<View>(rowId).setOnClickListener {
			if (selectedCategories.contains(categoryFilter)) {
				selectedCategories.remove(categoryFilter)
			} else {
				selectedCategories.add(categoryFilter)
			}
			if (selectedCategories.isEmpty()) {
				selectedCategories.add(CategoryFilter.ALL)
			}
			applyFiltersAndSort(scrollToTop = true)
			showFilterPopup(anchor)
		}
	}

	private fun createPopup(contentView: View): PopupWindow {
		val popup = PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
		popup.isOutsideTouchable = true
		popup.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
		popup.elevation = resources.getDimension(R.dimen.content_padding_small)
		contentView.setOnTouchListener { _, event ->
			if (event.action == MotionEvent.ACTION_OUTSIDE) {
				popup.dismiss()
				true
			} else {
				false
			}
		}
		return popup
	}

	private fun dismissSortPopup() {
		sortPopup?.dismiss()
		sortPopup = null
	}

	private fun dismissFilterPopup() {
		filterPopup?.dismiss()
		filterPopup = null
	}

	private fun dismissPopups() {
		dismissSortPopup()
		dismissFilterPopup()
	}

	inner class SearchAdapter : RecyclerView.Adapter<SearchViewHolder>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
			val view = LayoutInflater.from(parent.context).inflate(R.layout.item_star_search, parent, false)
			return SearchViewHolder(view)
		}

		override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
			holder.bind(visibleEntries[position])
		}

		override fun getItemCount(): Int = visibleEntries.size
	}

	inner class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		private val nameText = view.findViewById<TextView>(R.id.object_name)
		private val infoText = view.findViewById<TextView>(R.id.object_info)
		private val iconView = view.findViewById<ImageView>(R.id.object_icon)

		fun bind(entry: SearchEntry) {
			val obj = entry.objectRef
			nameText.text = entry.displayName
			infoText.text = buildSubtitle(entry)

			iconView.setImageDrawable(app.uiUtilities.getIcon(entry.iconRes, ColorUtilities.getDefaultIconColorId(nightMode)))
			iconView.setColorFilter(entry.iconColor)

			itemView.setOnClickListener {
				addRecentChip(entry.displayName)
				onObjectSelected?.invoke(obj)
				parentFragmentManager.beginTransaction()
					.hide(this@StarMapSearchDialogFragment)
					.addToBackStack(TAG)
					.commit()
			}
		}

		private fun buildSubtitle(entry: SearchEntry): String {
			val typeName = AstroUtils.getObjectTypeName(itemView.context, entry.objectRef.type)
			val magText = String.format(Locale.getDefault(), itemView.context.getString(R.string.astro_search_magnitude_short), entry.magnitude)
			val eventText = resolveEventText(entry)
			return "$typeName • $magText • $eventText"
		}

		private fun resolveEventText(entry: SearchEntry): String {
			ensureRiseSet(entry)
			val rise = entry.nextRise
			val set = entry.nextSet
			val hasRise = rise != null
			val hasSet = set != null
			if (hasRise && hasSet) {
				return if (rise.isBefore(set)) {
					itemView.context.getString(
						R.string.astro_search_rises_at,
						AstroUtils.formatLocalTime(Time.fromMillisecondsSince1970(rise.toInstant().toEpochMilli()))
					)
				} else {
					itemView.context.getString(
						R.string.astro_search_sets_at,
						AstroUtils.formatLocalTime(Time.fromMillisecondsSince1970(set.toInstant().toEpochMilli()))
					)
				}
			}
			if (hasRise) {
				return itemView.context.getString(
					R.string.astro_search_rises_at,
					AstroUtils.formatLocalTime(Time.fromMillisecondsSince1970(rise.toInstant().toEpochMilli()))
				)
			}
			if (hasSet) {
				return itemView.context.getString(
					R.string.astro_search_sets_at,
					AstroUtils.formatLocalTime(Time.fromMillisecondsSince1970(set.toInstant().toEpochMilli()))
				)
			}
			return if (entry.objectRef.altitude > 0) {
				itemView.context.getString(R.string.astro_search_always_up)
			} else {
				itemView.context.getString(R.string.astro_search_never_rises)
			}
		}
	}
}
