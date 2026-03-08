package net.osmand.plus.plugins.astronomy

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import net.osmand.plus.R
import net.osmand.plus.base.BaseFullScreenDialogFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.astronomy.utils.AstroUtils
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.utils.InsetsUtils.InsetSide
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.popup.PopUpMenu
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

class StarMapSearchDialogFragment : BaseFullScreenDialogFragment() {

	private enum class ScreenMode {
		EXPLORE,
		FULL_SEARCH
	}

	private enum class FullSearchMode {
		BROWSE,
		INPUT
	}

	private enum class InputPresentation {
		EXPLORE_BAR,
		STANDALONE
	}

	private enum class HideTarget {
		EXPLORE,
		BROWSE
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

	private lateinit var searchResultsPanel: View
	private lateinit var searchRecycler: RecyclerView
	private lateinit var exploreContainer: View
	private lateinit var fullSearchContainer: View
	private lateinit var fullSearchAppBar: AppBarLayout
	private lateinit var fullSearchResultsHost: FrameLayout
	private lateinit var exploreSearchInputResultsHost: FrameLayout
	private lateinit var fullSearchInputResultsHost: FrameLayout
	private lateinit var exploreSearchBar: SearchBar
	private lateinit var fullSearchAnchorBar: SearchBar
	private lateinit var fullSearchBrowseHeader: CollapsingToolbarLayout
	private lateinit var exploreSearchInputView: SearchView
	private lateinit var fullSearchInputView: SearchView
	private lateinit var fullSearchBrowseToolbar: MaterialToolbar
	private lateinit var exploreSearchEditText: EditText
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

	private var sortPopup: ListPopupWindow? = null
	private var filterPopup: ListPopupWindow? = null

	private var currentMode: ScreenMode = ScreenMode.EXPLORE
	private var currentFullSearchMode: FullSearchMode = FullSearchMode.INPUT
	private var currentInputPresentation: InputPresentation = InputPresentation.EXPLORE_BAR
	private var currentSortMode: SortMode = SortMode.NAME_ASC
	private var currentTypeFilter: TypeFilter = TypeFilter.SHOW_ALL
	private var currentNakedEyeOnly = false
	private val selectedCategories = linkedSetOf(CategoryFilter.ALL)
	private var currentQuickPresetType: QuickPresetType = QuickPresetType.NONE
	private var currentQuickPresetCatalogWid: String? = null
	private var currentQuery: String = ""
	private val recentChips = mutableListOf<String>()
	private val widToDisplayName = mutableMapOf<String, String>()
	private var observerForComputations = Observer(0.0, 0.0, 0.0)
	private var nowForComputations: ZonedDateTime = ZonedDateTime.now()
	private var duskForComputations: ZonedDateTime = ZonedDateTime.now()
	private var dawnForComputations: ZonedDateTime = ZonedDateTime.now().plusHours(12)
	private var suppressQueryDispatch = false
	private var pendingSearchHideTarget: HideTarget? = null
	private var previousSoftInputMode: Int? = null

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
		private const val KEY_FULL_SEARCH_MODE = "full_search_mode"
		private const val KEY_TYPE_FILTER = "type_filter"
		private const val KEY_NAKED_EYE = "naked_eye"
		private const val KEY_CATEGORIES = "categories"
		private const val KEY_QUICK_PRESET = "quick_preset"
		private const val KEY_QUICK_CATALOG = "quick_catalog"
		private const val KEY_RECENT_CHIPS = "recent_chips"
	}

	override fun getThemeId(): Int = if (nightMode) R.style.OsmandMaterialDarkTheme else R.style.OsmandMaterialLightTheme

	override fun getDialogThemeId(): Int = getThemeId()

	override fun getStatusBarColorId(): Int = android.R.color.transparent

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
		outState.putString(KEY_FULL_SEARCH_MODE, currentFullSearchMode.name)
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
		AndroidUiHelper.setStatusBarContentColor(view, nightMode)
		bindViews(view)
		refreshPreparedEntries()
		setupSearchRecycler()
		setupExploreContent()
		setupListeners()
		applySearchSoftInputMode()
		renderRecentChips()
		applyMode(
			currentMode,
			requestKeyboard = currentMode == ScreenMode.FULL_SEARCH && currentFullSearchMode == FullSearchMode.INPUT
		)
		applyFiltersAndSort(scrollToTop = false)
	}

	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (hidden) {
			restoreSearchSoftInputMode()
			dialog?.hide()
			return
		}
		applySearchSoftInputMode()
		dialog?.show()
		view?.let { AndroidUiHelper.setStatusBarContentColor(it, nightMode) }
		refreshPreparedEntries()
		setupMyDataRows()
		setupCatalogRows()
		applyFiltersAndSort(scrollToTop = false)
	}

	override fun onDestroyView() {
		dismissPopups()
		restoreSearchSoftInputMode()
		super.onDestroyView()
	}

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = super.getInsetTargets()
		collection.removeType(InsetTarget.Type.ROOT_INSET)
		collection.add(InsetTarget.createScrollable(R.id.search_results))
		collection.add(InsetTarget.createScrollable(R.id.explore_container))
		collection.add(
			InsetTarget.createCustomBuilder(R.id.explore_container)
				.portraitSides(InsetSide.TOP)
				.landscapeSides(InsetSide.TOP, InsetSide.LEFT, InsetSide.RIGHT)
				.applyPadding(true)
				.build()
		)
		collection.replace(InsetTarget.createCollapsingAppBar(R.id.full_search_app_bar))
		return collection
	}

	private fun bindViews(root: View) {
		exploreContainer = root.findViewById(R.id.explore_container)
		fullSearchContainer = root.findViewById(R.id.full_search_container)
		fullSearchAppBar = root.findViewById(R.id.full_search_app_bar)
		fullSearchResultsHost = root.findViewById(R.id.full_search_results_host)
		exploreSearchInputResultsHost = root.findViewById(R.id.explore_search_input_results_host)
		fullSearchInputResultsHost = root.findViewById(R.id.full_search_input_results_host)
		exploreSearchBar = root.findViewById(R.id.explore_search_bar)
		fullSearchAnchorBar = root.findViewById(R.id.full_search_anchor_bar)
		fullSearchBrowseHeader = root.findViewById(R.id.full_search_browse_header)
		exploreSearchInputView = root.findViewById(R.id.explore_search_input_view)
		exploreSearchInputView.setVisible(false)
		fullSearchInputView = root.findViewById(R.id.full_search_input_view)
		fullSearchInputView.setVisible(false)
		fullSearchBrowseToolbar = root.findViewById(R.id.full_search_browse_toolbar)
		exploreSearchEditText = exploreSearchInputView.editText
		fullSearchEditText = fullSearchInputView.editText
		recentChipsContainer = root.findViewById(R.id.recent_chips_container)
		recentChipsScroll = root.findViewById(R.id.recent_chips_scroll)
		categoriesContainer = root.findViewById(R.id.categories_rows_container)
		myDataContainer = root.findViewById(R.id.my_data_rows_container)
		catalogsContainer = root.findViewById(R.id.catalogs_rows_container)
		catalogsViewAllRow = root.findViewById(R.id.catalogs_view_all_row)
		catalogsViewAllCount = root.findViewById(R.id.catalogs_view_all_count)

		searchResultsPanel = themedInflater.inflate(R.layout.view_star_search_results_panel, fullSearchResultsHost, false)
		sortButton = searchResultsPanel.findViewById(R.id.sort_button)
		filterButton = searchResultsPanel.findViewById(R.id.filter_button)
		sortIcon = searchResultsPanel.findViewById(R.id.sort_icon)
		sortText = searchResultsPanel.findViewById(R.id.sort_text)
		filterText = searchResultsPanel.findViewById(R.id.filter_text)
		emptyStateContainer = searchResultsPanel.findViewById(R.id.empty_state_container)
		emptyStateResetButton = searchResultsPanel.findViewById(R.id.empty_state_reset_button)
		searchRecycler = searchResultsPanel.findViewById(R.id.search_results)
		attachSearchResultsPanel(fullSearchResultsHost)
	}

	private fun setupSearchRecycler() {
		searchAdapter = SearchAdapter()
		searchRecycler.layoutManager = LinearLayoutManager(context)
		searchRecycler.adapter = searchAdapter
		searchRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
				if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
					AndroidUtils.hideSoftKeyboard(requireActivity(), getActiveSearchEditText())
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
		bindExploreSearchBarListeners()
		fullSearchBrowseToolbar.setNavigationOnClickListener { applyMode(ScreenMode.EXPLORE, requestKeyboard = false) }
		fullSearchBrowseToolbar.setOnMenuItemClickListener { item ->
			if (item.itemId == R.id.action_search) {
				switchToInputMode()
				true
			} else {
				false
			}
		}
		exploreSearchInputView.addTransitionListener { _, _, newState ->
			when (newState) {
				SearchView.TransitionState.SHOWN -> handleSearchViewShown(InputPresentation.EXPLORE_BAR)
				SearchView.TransitionState.HIDDEN -> handleSearchViewHidden(InputPresentation.EXPLORE_BAR)
				else -> Unit
			}
		}
		fullSearchInputView.addTransitionListener { _, _, newState ->
			when (newState) {
				SearchView.TransitionState.SHOWN -> handleSearchViewShown(InputPresentation.STANDALONE)
				SearchView.TransitionState.HIDDEN -> handleSearchViewHidden(InputPresentation.STANDALONE)
				else -> Unit
			}
		}
		emptyStateResetButton.setOnClickListener { resetAllSearchParams() }
		sortButton.setOnClickListener { showSortPopup(sortButton) }
		filterButton.setOnClickListener { showFilterPopup(filterButton) }

		val textWatcher = object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
			override fun afterTextChanged(s: Editable?) {
				if (suppressQueryDispatch) {
					return
				}
				val newQuery = s?.toString().orEmpty()
				if (newQuery != currentQuery) {
					currentQuery = newQuery
					applyFiltersAndSort(scrollToTop = true)
				}
			}
		}
		exploreSearchEditText.addTextChangedListener(textWatcher)
		fullSearchEditText.addTextChangedListener(textWatcher)
	}

	private fun updateBrowseToolbarAppearance() {
		val iconColor = ColorUtilities.getDefaultIconColor(requireContext(), nightMode)
		exploreSearchBar.navigationIcon?.setTint(iconColor)
		exploreSearchBar.menu.findItem(R.id.action_search)?.icon?.setTint(iconColor)
		fullSearchAnchorBar.navigationIcon?.setTint(iconColor)
		fullSearchBrowseToolbar.navigationIcon?.setTint(iconColor)
		fullSearchBrowseToolbar.menu.findItem(R.id.action_search)?.icon?.setTint(iconColor)
	}

	private fun attachSearchResultsPanel(host: FrameLayout) {
		(searchResultsPanel.parent as? ViewGroup)?.removeView(searchResultsPanel)
		host.addView(
			searchResultsPanel,
			FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
		)
	}

	private fun getSearchView(presentation: InputPresentation): SearchView {
		return if (presentation == InputPresentation.EXPLORE_BAR) {
			exploreSearchInputView
		} else {
			fullSearchInputView
		}
	}

	private fun getSearchResultsHost(presentation: InputPresentation): FrameLayout {
		return if (presentation == InputPresentation.EXPLORE_BAR) {
			exploreSearchInputResultsHost
		} else {
			fullSearchInputResultsHost
		}
	}

	private fun getSearchEditText(presentation: InputPresentation): EditText {
		return if (presentation == InputPresentation.EXPLORE_BAR) {
			exploreSearchEditText
		} else {
			fullSearchEditText
		}
	}

	private fun getActiveSearchView(): SearchView? {
		return when {
			::exploreSearchInputView.isInitialized && exploreSearchInputView.isShowing -> exploreSearchInputView
			::fullSearchInputView.isInitialized && fullSearchInputView.isShowing -> fullSearchInputView
			else -> null
		}
	}

	private fun getActiveSearchEditText(): EditText {
		return getSearchEditText(currentInputPresentation)
	}

	private fun applySearchSoftInputMode() {
		val window = activity?.window ?: return
		if (previousSoftInputMode == null) {
			previousSoftInputMode = window.attributes.softInputMode
		}
		window.setSoftInputMode(SOFT_INPUT_ADJUST_NOTHING)
		exploreSearchInputView.updateSoftInputMode()
		fullSearchInputView.updateSoftInputMode()
	}

	private fun restoreSearchSoftInputMode() {
		val window = activity?.window ?: return
		val softInputMode = previousSoftInputMode ?: return
		window.setSoftInputMode(softInputMode)
		previousSoftInputMode = null
	}

	private fun restoreState(savedInstanceState: Bundle?) {
		if (savedInstanceState == null) {
			return
		}
		currentMode = savedInstanceState.getString(KEY_MODE)?.let { runCatching { ScreenMode.valueOf(it) }.getOrNull() }
			?: ScreenMode.EXPLORE
		currentFullSearchMode = savedInstanceState.getString(KEY_FULL_SEARCH_MODE)?.let {
			runCatching { FullSearchMode.valueOf(it) }.getOrNull()
		} ?: FullSearchMode.INPUT
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

	private fun bindExploreSearchBarListeners() {
		exploreSearchBar.setNavigationOnClickListener { dismiss() }
		exploreSearchBar.setOnClickListener { openFullSearch(QuickPresetType.NONE, null) }
		exploreSearchBar.setOnMenuItemClickListener { item ->
			if (item.itemId == R.id.action_search) {
				openFullSearch(QuickPresetType.NONE, null)
				true
			} else {
				false
			}
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
		currentFullSearchMode = if (isCategoryPreset() || quickPresetType == QuickPresetType.CATALOG_WID ||
			quickPresetType == QuickPresetType.MY_DATA_FAVORITES ||
			quickPresetType == QuickPresetType.MY_DATA_DAILY_PATH ||
			quickPresetType == QuickPresetType.MY_DATA_DIRECTIONS) {
			FullSearchMode.BROWSE
		} else {
			FullSearchMode.INPUT
		}
		applyMode(ScreenMode.FULL_SEARCH, requestKeyboard = currentFullSearchMode == FullSearchMode.INPUT)
		applyFiltersAndSort(scrollToTop = true)
	}

	private fun applyMode(mode: ScreenMode, requestKeyboard: Boolean) {
		updateBrowseToolbarAppearance()
		if (mode == ScreenMode.EXPLORE) {
			showExploreMode()
		} else if (currentFullSearchMode == FullSearchMode.BROWSE) {
			showBrowseMode()
		} else {
			val presentation = if (hasBrowseContext()) {
				InputPresentation.STANDALONE
			} else {
				InputPresentation.EXPLORE_BAR
			}
			showInputMode(presentation, requestKeyboard)
		}
		updateSortControls()
		updateFilterControls()
		updateEmptyStateVisibility()
	}

	private fun renderBrowseHeader() {
		fullSearchBrowseHeader.title = getBrowseTitle()
	}

	private fun showExploreMode() {
		currentMode = ScreenMode.EXPLORE
		currentFullSearchMode = FullSearchMode.INPUT
		dismissPopups()
		getActiveSearchView()?.let { searchView ->
			pendingSearchHideTarget = HideTarget.EXPLORE
			searchView.hide()
			return
		}
		attachSearchResultsPanel(fullSearchResultsHost)
		fullSearchAnchorBar.isVisible = false
		fullSearchBrowseHeader.isVisible = true
		exploreSearchBar.setText("")
		exploreContainer.isVisible = true
		fullSearchContainer.isVisible = false
		AndroidUtils.hideSoftKeyboard(requireActivity(), getActiveSearchEditText())
	}

	private fun showBrowseMode() {
		currentMode = ScreenMode.FULL_SEARCH
		currentFullSearchMode = FullSearchMode.BROWSE
		dismissPopups()
		getActiveSearchView()?.let { searchView ->
			pendingSearchHideTarget = HideTarget.BROWSE
			searchView.hide()
			return
		}
		attachSearchResultsPanel(fullSearchResultsHost)
		fullSearchAnchorBar.isVisible = false
		renderBrowseHeader()
		updateInfoCard()
		fullSearchBrowseHeader.isVisible = true
		exploreContainer.isVisible = false
		fullSearchContainer.isVisible = true
		fullSearchAppBar.setExpanded(true, false)
		AndroidUtils.hideSoftKeyboard(requireActivity(), getActiveSearchEditText())
	}

	private fun switchToInputMode() {
		showInputMode(InputPresentation.STANDALONE, requestKeyboard = true)
	}

	private fun showInputMode(presentation: InputPresentation, requestKeyboard: Boolean) {
		currentMode = ScreenMode.FULL_SEARCH
		currentFullSearchMode = FullSearchMode.INPUT
		currentInputPresentation = presentation
		pendingSearchHideTarget = null
		dismissPopups()
		attachSearchResultsPanel(getSearchResultsHost(presentation))
		configureSearchView(presentation, requestKeyboard)
		syncSearchQuery()
		updateInfoCard()
		if (presentation == InputPresentation.EXPLORE_BAR) {
			fullSearchInputView.setVisible(false)
			fullSearchAnchorBar.isVisible = false
		} else {
			exploreSearchInputView.setVisible(false)
			fullSearchAnchorBar.isVisible = true
		}
		fullSearchBrowseHeader.isVisible = presentation == InputPresentation.STANDALONE
		exploreContainer.isVisible = presentation == InputPresentation.EXPLORE_BAR
		fullSearchContainer.isVisible = presentation == InputPresentation.STANDALONE
		val searchView = getSearchView(presentation)
		if (!searchView.isShowing) {
			searchView.show()
		} else {
			handleSearchViewShown(presentation)
		}
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
			if (currentFullSearchMode == FullSearchMode.INPUT) {
				pendingSearchHideTarget = if (hasBrowseContext()) HideTarget.BROWSE else HideTarget.EXPLORE
				getSearchView(currentInputPresentation).hide()
			} else {
				showExploreMode()
			}
			true
		} else {
			false
		}
	}

	private fun configureSearchView(presentation: InputPresentation, requestKeyboard: Boolean) {
		val searchView = getSearchView(presentation)
		searchView.setupWithSearchBar(
			if (presentation == InputPresentation.EXPLORE_BAR) exploreSearchBar else fullSearchAnchorBar
		)
		searchView.setMenuItemsAnimated(false)
		searchView.toolbar.menu.clear()
		searchView.inflateMenu(R.menu.menu_star_search_input)
		val iconColor = ColorUtilities.getDefaultIconColor(requireContext(), nightMode)
		searchView.toolbar.navigationIcon?.mutate()?.setTint(iconColor)
		searchView.toolbar.menu.findItem(R.id.action_close_search)?.icon?.mutate()?.setTint(iconColor)
		searchView.setOnMenuItemClickListener { item ->
			if (item.itemId == R.id.action_close_search) {
				pendingSearchHideTarget = if (hasBrowseContext()) HideTarget.BROWSE else HideTarget.EXPLORE
				searchView.hide()
				true
			} else {
				false
			}
		}
		if (presentation == InputPresentation.EXPLORE_BAR) {
			bindExploreSearchBarListeners()
		} else {
			fullSearchAnchorBar.setText(currentQuery)
		}
		searchView.setAutoShowKeyboard(requestKeyboard)
		searchView.updateSoftInputMode()
	}

	private fun handleSearchViewShown(presentation: InputPresentation) {
		attachSearchResultsPanel(getSearchResultsHost(presentation))
		if (presentation == InputPresentation.EXPLORE_BAR) {
			exploreContainer.isVisible = false
			fullSearchAnchorBar.isVisible = false
			fullSearchContainer.isVisible = false
		} else {
			fullSearchAnchorBar.isVisible = true
			fullSearchContainer.isVisible = true
		}
		val editText = getSearchEditText(presentation)
		if (editText.text?.length ?: 0 > 0) {
			editText.setSelection(editText.length())
		}
	}

	private fun handleSearchViewHidden(presentation: InputPresentation) {
		if (presentation != currentInputPresentation) {
			return
		}
		attachSearchResultsPanel(fullSearchResultsHost)
		AndroidUtils.hideSoftKeyboard(requireActivity(), getSearchEditText(presentation))
		when (pendingSearchHideTarget ?: if (hasBrowseContext()) HideTarget.BROWSE else HideTarget.EXPLORE) {
			HideTarget.BROWSE -> {
				currentMode = ScreenMode.FULL_SEARCH
				currentFullSearchMode = FullSearchMode.BROWSE
				fullSearchAnchorBar.isVisible = false
				renderBrowseHeader()
				updateInfoCard()
				fullSearchBrowseHeader.isVisible = true
				exploreContainer.isVisible = false
				fullSearchContainer.isVisible = true
				fullSearchAppBar.setExpanded(true, false)
			}
			HideTarget.EXPLORE -> {
				currentMode = ScreenMode.EXPLORE
				currentFullSearchMode = FullSearchMode.INPUT
				fullSearchAnchorBar.isVisible = false
				fullSearchBrowseHeader.isVisible = true
				exploreSearchBar.setText("")
				exploreContainer.isVisible = true
				fullSearchContainer.isVisible = false
			}
		}
		pendingSearchHideTarget = null
		updateSortControls()
		updateFilterControls()
		updateEmptyStateVisibility()
	}

	private fun syncSearchQuery() {
		suppressQueryDispatch = true
		if (exploreSearchEditText.text.toString() != currentQuery) {
			exploreSearchEditText.setText(currentQuery)
		}
		if (fullSearchEditText.text.toString() != currentQuery) {
			fullSearchEditText.setText(currentQuery)
		}
		fullSearchAnchorBar.setText(currentQuery)
		exploreSearchInputView.setText(currentQuery)
		fullSearchInputView.setText(currentQuery)
		val editText = getSearchEditText(currentInputPresentation)
		if ((editText.text?.length ?: 0) > 0) {
			editText.setSelection(editText.length())
		}
		suppressQueryDispatch = false
	}

	private fun refreshPreparedEntries() {
		preparedEntries.clear()
		widToDisplayName.clear()
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
			if (obj.wid.isNotEmpty()) {
				widToDisplayName[obj.wid] = obj.localizedName ?: obj.name
			}
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

	private fun isCategoryPreset(): Boolean {
		return categoryPresetFromQuickPreset() != null
	}

	private fun hasBrowseContext(): Boolean {
		return currentQuickPresetType != QuickPresetType.NONE
	}

	private fun categoryPresetFromQuickPreset(): CategoryFilter? {
		return when (currentQuickPresetType) {
			QuickPresetType.CATEGORY_SOLAR_SYSTEM -> CategoryFilter.SOLAR_SYSTEM
			QuickPresetType.CATEGORY_CONSTELLATIONS -> CategoryFilter.CONSTELLATIONS
			QuickPresetType.CATEGORY_STARS -> CategoryFilter.STARS
			QuickPresetType.CATEGORY_NEBULAS -> CategoryFilter.NEBULAS
			QuickPresetType.CATEGORY_STAR_CLUSTERS -> CategoryFilter.STAR_CLUSTERS
			QuickPresetType.CATEGORY_DEEP_SKY -> CategoryFilter.DEEP_SKY
			else -> null
		}
	}

	private fun shouldShowInfoHeader(): Boolean {
		return currentMode == ScreenMode.FULL_SEARCH &&
				currentFullSearchMode == FullSearchMode.BROWSE &&
				categoryPresetFromQuickPreset() != null
	}

	private fun getCategoryIconRes(category: CategoryFilter): Int {
		return when (category) {
			CategoryFilter.SOLAR_SYSTEM -> R.drawable.ic_action_planet_outlined
			CategoryFilter.CONSTELLATIONS -> R.drawable.ic_action_constellations
			CategoryFilter.STARS -> R.drawable.ic_action_stars
			CategoryFilter.NEBULAS -> R.drawable.ic_action_nebulas
			CategoryFilter.STAR_CLUSTERS -> R.drawable.ic_action_star_clusters
			CategoryFilter.DEEP_SKY -> R.drawable.ic_action_galaxy
			CategoryFilter.ALL -> R.drawable.ic_action_search_dark
		}
	}

	private fun getCategoryInfoTextRes(category: CategoryFilter): Int {
		return when (category) {
			CategoryFilter.SOLAR_SYSTEM -> R.string.astro_search_info_solar_system
			CategoryFilter.CONSTELLATIONS -> R.string.astro_search_info_constellations
			CategoryFilter.STARS -> R.string.astro_search_info_stars
			CategoryFilter.NEBULAS -> R.string.astro_search_info_nebulas
			CategoryFilter.STAR_CLUSTERS -> R.string.astro_search_info_star_clusters
			CategoryFilter.DEEP_SKY -> R.string.astro_search_info_deep_sky
			CategoryFilter.ALL -> R.string.astro_search_info_solar_system
		}
	}

	private fun getBrowseTitle(): String {
		return when (currentQuickPresetType) {
			QuickPresetType.CATEGORY_SOLAR_SYSTEM -> getString(R.string.astro_solar_system)
			QuickPresetType.CATEGORY_CONSTELLATIONS -> getString(R.string.astro_constellations)
			QuickPresetType.CATEGORY_STARS -> getString(R.string.astro_stars)
			QuickPresetType.CATEGORY_NEBULAS -> getString(R.string.astro_nebulas)
			QuickPresetType.CATEGORY_STAR_CLUSTERS -> getString(R.string.astro_star_clusters)
			QuickPresetType.CATEGORY_DEEP_SKY -> getString(R.string.astro_deep_sky)
			QuickPresetType.MY_DATA_FAVORITES -> getString(R.string.favorites_item)
			QuickPresetType.MY_DATA_DAILY_PATH -> getString(R.string.astro_daily_path)
			QuickPresetType.MY_DATA_DIRECTIONS -> getString(R.string.astro_directions)
			QuickPresetType.CATALOG_WID -> {
				val catalog = dataProvider.getCatalogs(requireContext()).firstOrNull { it.wid == currentQuickPresetCatalogWid }
				catalog?.name ?: getString(R.string.shared_string_search)
			}
			QuickPresetType.NONE -> getString(R.string.shared_string_search)
		}
	}

	private fun updateInfoCard() {
		if (::searchAdapter.isInitialized) {
			searchAdapter.notifyDataSetChanged()
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
		if (scrollToTop && currentMode == ScreenMode.FULL_SEARCH && currentFullSearchMode == FullSearchMode.BROWSE) {
			fullSearchAppBar.setExpanded(true, false)
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
		if (currentQuickPresetType != QuickPresetType.NONE) count++
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
		currentFullSearchMode = FullSearchMode.INPUT
		selectedCategories.clear()
		selectedCategories.add(CategoryFilter.ALL)
		currentQuickPresetType = QuickPresetType.NONE
		currentQuickPresetCatalogWid = null
		syncSearchQuery()
		if (currentMode == ScreenMode.FULL_SEARCH) {
			showInputMode(InputPresentation.STANDALONE, requestKeyboard = false)
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
				currentFullSearchMode = FullSearchMode.INPUT
				currentQuery = chipTitle
				showInputMode(InputPresentation.EXPLORE_BAR, requestKeyboard = true)
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
		val activeColor = ColorUtilities.getActiveColor(app, nightMode)
		val secondaryTextColor = ColorUtilities.getSecondaryTextColor(requireContext(), nightMode)

		val items = listOf(
			createPopupHeaderItem(getString(R.string.sort_by), secondaryTextColor),
			createRadioPopupItem(getString(R.string.sort_name_ascending), currentSortMode == SortMode.NAME_ASC, activeColor) {
				currentSortMode = SortMode.NAME_ASC
				updateSortControls()
				applyFiltersAndSort(scrollToTop = true)
			},
			createRadioPopupItem(getString(R.string.sort_name_descending), currentSortMode == SortMode.NAME_DESC, activeColor) {
				currentSortMode = SortMode.NAME_DESC
				updateSortControls()
				applyFiltersAndSort(scrollToTop = true)
			},
			createRadioPopupItem(
				getString(R.string.astro_sort_brightest_first),
				currentSortMode == SortMode.BRIGHTEST_FIRST,
				activeColor,
				showTopDivider = true
			) {
				currentSortMode = SortMode.BRIGHTEST_FIRST
				updateSortControls()
				applyFiltersAndSort(scrollToTop = true)
			},
			createRadioPopupItem(getString(R.string.astro_sort_faintest_first), currentSortMode == SortMode.FAINTEST_FIRST, activeColor) {
				currentSortMode = SortMode.FAINTEST_FIRST
				updateSortControls()
				applyFiltersAndSort(scrollToTop = true)
			},
			createRadioPopupItem(
				getString(R.string.astro_sort_rises_soonest),
				currentSortMode == SortMode.RISES_SOONEST,
				activeColor,
				showTopDivider = true
			) {
				currentSortMode = SortMode.RISES_SOONEST
				updateSortControls()
				applyFiltersAndSort(scrollToTop = true)
			},
			createRadioPopupItem(getString(R.string.astro_sort_sets_soonest), currentSortMode == SortMode.SETS_SOONEST, activeColor) {
				currentSortMode = SortMode.SETS_SOONEST
				updateSortControls()
				applyFiltersAndSort(scrollToTop = true)
			}
		)
		sortPopup = PopUpMenu.showAndGet(
			createPopupDisplayData(
				anchor = anchor,
				items = items,
				alignEnd = false,
				limitHeight = false,
				layoutId = R.layout.popup_star_search_sort_menu_item
			)
		)
	}

	private fun showFilterPopup(anchor: View) {
		dismissFilterPopup()
		val activeColor = ColorUtilities.getActiveColor(app, nightMode)
		val secondaryTextColor = ColorUtilities.getSecondaryTextColor(requireContext(), nightMode)
		val items = listOf(
			createPopupHeaderItem(getString(R.string.shared_string_type), secondaryTextColor),
			createRadioPopupItem(getString(R.string.astro_filter_show_all), currentTypeFilter == TypeFilter.SHOW_ALL, activeColor,) {
				currentTypeFilter = TypeFilter.SHOW_ALL
				applyFiltersAndSort(scrollToTop = true)
			},
			createRadioPopupItem(getString(R.string.astro_filter_visible_now), currentTypeFilter == TypeFilter.VISIBLE_NOW, activeColor) {
				currentTypeFilter = TypeFilter.VISIBLE_NOW
				applyFiltersAndSort(scrollToTop = true)
			},
			createRadioPopupItem(getString(R.string.astro_filter_visible_tonight), currentTypeFilter == TypeFilter.VISIBLE_TONIGHT, activeColor) {
				currentTypeFilter = TypeFilter.VISIBLE_TONIGHT
				applyFiltersAndSort(scrollToTop = true)
			},
			createCheckPopupItem(getString(R.string.astro_filter_naked_eye), currentNakedEyeOnly, activeColor, showTopDivider = true) {
				currentNakedEyeOnly = !currentNakedEyeOnly
				applyFiltersAndSort(scrollToTop = true)
			},
			createPopupHeaderItem(getString(R.string.favourites_edit_dialog_category), secondaryTextColor, showTopDivider = true),
			createCheckPopupItem(getString(R.string.shared_string_all), selectedCategories.contains(CategoryFilter.ALL), activeColor) {
				toggleCategoryFilter(CategoryFilter.ALL)
				applyFiltersAndSort(scrollToTop = true)
			},
			createCheckPopupItem(getString(R.string.astro_solar_system), selectedCategories.contains(CategoryFilter.SOLAR_SYSTEM), activeColor) {
				toggleCategoryFilter(CategoryFilter.SOLAR_SYSTEM)
				applyFiltersAndSort(scrollToTop = true)
			},
			createCheckPopupItem(getString(R.string.astro_constellations), selectedCategories.contains(CategoryFilter.CONSTELLATIONS), activeColor) {
				toggleCategoryFilter(CategoryFilter.CONSTELLATIONS)
				applyFiltersAndSort(scrollToTop = true)
			},
			createCheckPopupItem(getString(R.string.astro_stars), selectedCategories.contains(CategoryFilter.STARS), activeColor) {
				toggleCategoryFilter(CategoryFilter.STARS)
				applyFiltersAndSort(scrollToTop = true)
			},
			createCheckPopupItem(getString(R.string.astro_nebulas), selectedCategories.contains(CategoryFilter.NEBULAS), activeColor) {
				toggleCategoryFilter(CategoryFilter.NEBULAS)
				applyFiltersAndSort(scrollToTop = true)
			},
			createCheckPopupItem(getString(R.string.astro_star_clusters), selectedCategories.contains(CategoryFilter.STAR_CLUSTERS), activeColor) {
				toggleCategoryFilter(CategoryFilter.STAR_CLUSTERS)
				applyFiltersAndSort(scrollToTop = true)
			},
			createCheckPopupItem(getString(R.string.astro_deep_sky), selectedCategories.contains(CategoryFilter.DEEP_SKY), activeColor) {
				toggleCategoryFilter(CategoryFilter.DEEP_SKY)
				applyFiltersAndSort(scrollToTop = true)
			}
		)
		filterPopup = PopUpMenu.showAndGet(createPopupDisplayData(anchor, items, alignEnd = true, limitHeight = true))
	}

	private fun toggleCategoryFilter(categoryFilter: CategoryFilter) {
		if (selectedCategories.contains(categoryFilter)) {
			selectedCategories.remove(categoryFilter)
		} else {
			selectedCategories.add(categoryFilter)
		}
		if (selectedCategories.isEmpty()) {
			selectedCategories.add(CategoryFilter.ALL)
		}
	}

	private fun createPopupDisplayData(
		anchor: View,
		items: List<PopUpMenuItem>,
		alignEnd: Boolean,
		limitHeight: Boolean,
		layoutId: Int = R.layout.popup_star_search_menu_item
	): PopUpMenuDisplayData {
		val contentPaddingHalf = resources.getDimensionPixelSize(R.dimen.content_padding_half)
		return PopUpMenuDisplayData().apply {
			anchorView = anchor
			this.layoutId = layoutId
			nightMode = this@StarMapSearchDialogFragment.nightMode
			widthMode = PopUpMenuWidthMode.STANDARD
			showCompound = true
			this.limitHeight = limitHeight
			dropDownGravity = if (alignEnd) Gravity.END or Gravity.BOTTOM else Gravity.START or Gravity.BOTTOM
			horizontalOffset = if (alignEnd) -contentPaddingHalf else contentPaddingHalf
			verticalOffset = -anchor.height + contentPaddingHalf
			menuItems = items
		}
	}

	private fun createPopupHeaderItem(
		title: CharSequence,
		titleColor: Int,
		showTopDivider: Boolean = false
	): PopUpMenuItem {
		return PopUpMenuItem.Builder(requireContext())
			.setTitle(title)
			.setTitleColor(titleColor)
			.setTitleBold(true)
			.setDismissOnClick(true)
			.showTopDivider(showTopDivider)
			.create()
	}

	private fun createRadioPopupItem(
		title: CharSequence,
		selected: Boolean,
		activeColor: Int,
		showTopDivider: Boolean = false,
		dismissOnClick: Boolean = true,
		onClick: () -> Unit
	): PopUpMenuItem {
		return PopUpMenuItem.Builder(requireContext())
			.setTitle(title)
			.showCompoundBtn(activeColor, PopUpMenuItem.CompoundButtonType.RADIO)
			.setSelected(selected)
			.showTopDivider(showTopDivider)
			.setDismissOnClick(dismissOnClick)
			.setOnClickListener { onClick() }
			.create()
	}

	private fun createCheckPopupItem(
		title: CharSequence,
		selected: Boolean,
		activeColor: Int,
		showTopDivider: Boolean = false,
		dismissOnClick: Boolean = true,
		onClick: () -> Unit
	): PopUpMenuItem {
		return PopUpMenuItem.Builder(requireContext())
			.setTitle(title)
			.showCompoundBtn(activeColor, PopUpMenuItem.CompoundButtonType.CHECKBOX)
			.setSelected(selected)
			.showTopDivider(showTopDivider)
			.setDismissOnClick(dismissOnClick)
			.setOnClickListener { onClick() }
			.create()
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
		private val viewTypeInfo = 0
		private val viewTypeItem = 1

		override fun getItemViewType(position: Int): Int {
			return if (shouldShowInfoHeader() && position == 0) viewTypeInfo else viewTypeItem
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
			val layoutId = if (viewType == viewTypeInfo) {
				R.layout.item_star_search_info
			} else {
				R.layout.item_star_search
			}
			val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
			return SearchViewHolder(view)
		}

		override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
			if (getItemViewType(position) == viewTypeInfo) {
				holder.bindInfo()
			} else {
				holder.bindResult(getEntryForPosition(position))
			}
		}

		override fun getItemCount(): Int = visibleEntries.size + if (shouldShowInfoHeader()) 1 else 0

		private fun getEntryForPosition(position: Int): SearchEntry {
			val entryIndex = if (shouldShowInfoHeader()) position - 1 else position
			return visibleEntries[entryIndex]
		}
	}

	inner class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		private val nameText: TextView? = view.findViewById(R.id.object_name)
		private val infoText: TextView? = view.findViewById(R.id.object_info)
		private val iconView: ImageView? = view.findViewById(R.id.object_icon)
		private val headerInfoIcon: ImageView? = view.findViewById(R.id.info_icon)
		private val headerInfoText: TextView? = view.findViewById(R.id.info_text)

		fun bindInfo() {
			val presetCategory = categoryPresetFromQuickPreset() ?: return
			headerInfoIcon?.setImageDrawable(
				app.uiUtilities.getIcon(
					getCategoryIconRes(presetCategory),
					ColorUtilities.getActiveIconColorId(nightMode)
				)
			)
			headerInfoText?.setText(getCategoryInfoTextRes(presetCategory))
			itemView.setOnClickListener(null)
		}

		fun bindResult(entry: SearchEntry) {
			val obj = entry.objectRef
			nameText?.text = entry.displayName
			infoText?.text = buildSubtitle(entry)

			val forcedCategory = categoryPresetFromQuickPreset()
			if (forcedCategory != null) {
				iconView?.setImageDrawable(
					app.uiUtilities.getIcon(
						getCategoryIconRes(forcedCategory),
						ColorUtilities.getDefaultIconColorId(nightMode)
					)
				)
				iconView?.clearColorFilter()
			} else {
				iconView?.setImageDrawable(app.uiUtilities.getIcon(entry.iconRes, ColorUtilities.getDefaultIconColorId(nightMode)))
				iconView?.setColorFilter(entry.iconColor)
			}

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
			val descriptorText = buildDescriptor(entry)
			val magText = String.format(
				Locale.getDefault(),
				itemView.context.getString(R.string.astro_search_magnitude_short),
				entry.magnitude
			)
			if (entry.objectRef.type == SkyObject.Type.CONSTELLATION) {
				return "$descriptorText • $magText"
			}
			val eventText = resolveEventText(entry)
			return "$descriptorText • $magText • $eventText"
		}

		private fun buildDescriptor(entry: SearchEntry): String {
			val obj = entry.objectRef
			val parentName = resolveParentName(obj)
			val presetCategory = categoryPresetFromQuickPreset()
			return when (presetCategory) {
				CategoryFilter.CONSTELLATIONS -> {
					val code = obj.catalogId?.trim().orEmpty()
					if (code.isNotEmpty()) {
						itemView.context.getString(
							R.string.astro_search_constellation_with_code,
							code.uppercase(Locale.getDefault())
						)
					} else {
						itemView.context.getString(R.string.astro_search_type_constellation)
					}
				}
				CategoryFilter.STARS -> {
					if (parentName.isNullOrEmpty()) {
						itemView.context.getString(R.string.astro_search_type_star)
					} else {
						itemView.context.getString(R.string.astro_search_in_location, parentName)
					}
				}
				CategoryFilter.NEBULAS,
				CategoryFilter.STAR_CLUSTERS,
				CategoryFilter.DEEP_SKY -> {
					val typeLabel = getSingularTypeLabel(obj.type)
					if (parentName.isNullOrEmpty()) {
						typeLabel
					} else {
						itemView.context.getString(R.string.astro_search_type_in_location, typeLabel, parentName)
					}
				}
				else -> getSingularTypeLabel(obj.type)
			}
		}

		private fun getSingularTypeLabel(type: SkyObject.Type): String {
			return when (type) {
				SkyObject.Type.SUN -> itemView.context.getString(R.string.astro_name_sun)
				SkyObject.Type.MOON -> itemView.context.getString(R.string.astro_name_moon)
				SkyObject.Type.PLANET -> itemView.context.getString(R.string.astro_search_type_planet)
				SkyObject.Type.STAR -> itemView.context.getString(R.string.astro_search_type_star)
				SkyObject.Type.GALAXY -> itemView.context.getString(R.string.astro_search_type_galaxy)
				SkyObject.Type.NEBULA -> itemView.context.getString(R.string.astro_search_type_nebula)
				SkyObject.Type.BLACK_HOLE -> itemView.context.getString(R.string.astro_search_type_black_hole)
				SkyObject.Type.OPEN_CLUSTER -> itemView.context.getString(R.string.astro_search_type_open_cluster)
				SkyObject.Type.GLOBULAR_CLUSTER -> itemView.context.getString(R.string.astro_search_type_globular_cluster)
				SkyObject.Type.GALAXY_CLUSTER -> itemView.context.getString(R.string.astro_search_type_galaxy_cluster)
				SkyObject.Type.CONSTELLATION -> itemView.context.getString(R.string.astro_search_type_constellation)
			}
		}

		private fun resolveParentName(obj: SkyObject): String? {
			val centerWid = obj.centerWId?.trim().orEmpty()
			if (centerWid.isEmpty()) {
				return null
			}
			val mapped = widToDisplayName[centerWid]
			if (!mapped.isNullOrEmpty()) {
				return mapped
			}
			return centerWid.replace('_', ' ').ifEmpty { null }
		}

		private fun resolveEventText(entry: SearchEntry): String {
			ensureRiseSet(entry)
			val rise = entry.nextRise
			val set = entry.nextSet
			if (rise != null && set != null) {
				return if (rise.isBefore(set)) {
					formatEvent(rise, isRise = true)
				} else {
					formatEvent(set, isRise = false)
				}
			}
			if (rise != null) {
				return formatEvent(rise, isRise = true)
			}
			if (set != null) {
				return formatEvent(set, isRise = false)
			}
			return if (entry.objectRef.altitude > 0) {
				itemView.context.getString(R.string.astro_search_always_up)
			} else {
				itemView.context.getString(R.string.astro_search_never_rises)
			}
		}

		private fun formatEvent(time: ZonedDateTime, isRise: Boolean): String {
			val formattedTime = AstroUtils.formatLocalTime(
				Time.fromMillisecondsSince1970(time.toInstant().toEpochMilli())
			)
			val daysBetween = ChronoUnit.DAYS.between(nowForComputations.toLocalDate(), time.toLocalDate())
			return if (daysBetween == 1L) {
				val tomorrow = itemView.context.getString(R.string.tomorrow)
				if (isRise) {
					itemView.context.getString(R.string.astro_search_rises_tomorrow, tomorrow, formattedTime)
				} else {
					itemView.context.getString(R.string.astro_search_sets_tomorrow, tomorrow, formattedTime)
				}
			} else {
				if (isRise) {
					itemView.context.getString(R.string.astro_search_rises_at, formattedTime)
				} else {
					itemView.context.getString(R.string.astro_search_sets_at, formattedTime)
				}
			}
		}
	}
	}
