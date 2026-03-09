package net.osmand.plus.plugins.astronomy.search

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as MaterialR
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.google.android.material.tabs.TabLayout
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.osmand.plus.R
import net.osmand.plus.base.BaseFullScreenDialogFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.astronomy.AstroDataProvider
import net.osmand.plus.plugins.astronomy.AstronomyPlugin
import net.osmand.plus.plugins.astronomy.AstronomyPluginSettings
import net.osmand.plus.plugins.astronomy.SkyObject
import net.osmand.plus.plugins.astronomy.StarMapFragment
import net.osmand.plus.plugins.astronomy.utils.AstroUtils
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.utils.InsetsUtils
import net.osmand.plus.widgets.popup.PopUpMenu
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

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

	private data class RiseSetCacheEntry(
		val nextRise: ZonedDateTime?,
		val nextSet: ZonedDateTime?
	)

	private data class CatalogsBackState(
		val query: String,
		val sortMode: StarMapSearchSortMode
	)

	private lateinit var searchAdapter: StarMapSearchResultsAdapter
	private lateinit var catalogsAdapter: StarMapCatalogsAdapter
	private lateinit var searchState: StarMapSearchState
	private val preparedEntries = mutableListOf<StarMapSearchEntry>()
	private val visibleEntries = mutableListOf<StarMapSearchEntry>()
	private val preparedCatalogEntries = mutableListOf<StarMapCatalogEntry>()
	private val visibleCatalogEntries = mutableListOf<StarMapCatalogEntry>()
	private val riseSetCache = ConcurrentHashMap<String, RiseSetCacheEntry>()
	private val visibleTonightCache = ConcurrentHashMap<String, Boolean>()

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
	private lateinit var myDataTabs: TabLayout
	private lateinit var exploreSearchEditText: EditText
	private lateinit var fullSearchEditText: EditText
	private lateinit var sortButton: View
	private lateinit var filterButton: View
	private lateinit var sortIcon: ImageView
	private lateinit var sortText: TextView
	private lateinit var filterText: TextView
	private lateinit var sortProgress: View
	private lateinit var emptyStateContainer: View
	private lateinit var emptyStateIcon: ImageView
	private lateinit var emptyStateTitle: TextView
	private lateinit var emptyStateDescription: TextView
	private lateinit var emptyStateResetButton: View
	private lateinit var recentChipsContainer: LinearLayout
	private lateinit var recentChipsScroll: View
	private lateinit var watchNowRow: View
	private lateinit var categoriesContainer: LinearLayout
	private lateinit var myDataContainer: LinearLayout
	private lateinit var catalogsContainer: LinearLayout
	private lateinit var catalogsViewAllRow: View
	private lateinit var catalogsViewAllCount: TextView

	private var sortPopup: ListPopupWindow? = null
	private var filterPopup: ListPopupWindow? = null
	private var filterAndSortJob: Job? = null
	private var filterAndSortRequestId = 0

	private var currentMode: ScreenMode = ScreenMode.EXPLORE
	private var currentFullSearchMode: FullSearchMode = FullSearchMode.INPUT
	private var currentInputPresentation: InputPresentation = InputPresentation.EXPLORE_BAR
	private var wasInfoHeaderVisible = false
	private var defaultBrowseHeaderHeight = 0
	private val widToDisplayName = mutableMapOf<String, String>()
	private var observerForComputations = Observer(0.0, 0.0, 0.0)
	private var nowForComputations: ZonedDateTime = ZonedDateTime.now()
	private var duskForComputations: ZonedDateTime = ZonedDateTime.now()
	private var dawnForComputations: ZonedDateTime = ZonedDateTime.now().plusHours(12)
	private var suppressQueryDispatch = false
	private var pendingSearchHideTarget: HideTarget? = null
	private var previousSoftInputMode: Int? = null
	private var catalogsBackState: CatalogsBackState? = null

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
		private const val KEY_FULL_SEARCH_MODE = "full_search_mode"
		private const val KEY_CATALOGS_BACK_QUERY = "catalogs_back_query"
		private const val KEY_CATALOGS_BACK_SORT = "catalogs_back_sort"
		private const val RISE_ARROW = "↗"
		private const val SET_ARROW = "↘"
		private const val UP_ARROW = "↑"
		private const val DOWN_ARROW = "↓"
	}

	override fun getThemeId(): Int = if (nightMode) R.style.OsmandMaterialDarkTheme else R.style.OsmandMaterialLightTheme

	override fun getDialogThemeId(): Int = getThemeId()

	override fun getStatusBarColorId(): Int = android.R.color.transparent

	override fun getThemeUsageContext(): ThemeUsageContext = ThemeUsageContext.APP

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		searchState = StarMapSearchState(savedInstanceState)
		restoreUiState(savedInstanceState)
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
		catalogsBackState?.let {
			outState.putString(KEY_CATALOGS_BACK_QUERY, it.query)
			outState.putString(KEY_CATALOGS_BACK_SORT, it.sortMode.name)
		}
		searchState.save(outState)
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
		filterAndSortJob?.cancel()
		filterAndSortJob = null
		if (::sortProgress.isInitialized) {
			sortProgress.isVisible = false
		}
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
				.portraitSides(InsetsUtils.InsetSide.TOP)
				.landscapeSides(InsetsUtils.InsetSide.TOP, InsetsUtils.InsetSide.LEFT, InsetsUtils.InsetSide.RIGHT)
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
		defaultBrowseHeaderHeight = fullSearchBrowseHeader.layoutParams.height
		myDataTabs = root.findViewById(R.id.my_data_tabs)
		exploreSearchInputView = root.findViewById(R.id.explore_search_input_view)
		exploreSearchInputView.setVisible(false)
		fullSearchInputView = root.findViewById(R.id.full_search_input_view)
		fullSearchInputView.setVisible(false)
		fullSearchBrowseToolbar = root.findViewById(R.id.full_search_browse_toolbar)
		exploreSearchEditText = exploreSearchInputView.editText
		fullSearchEditText = fullSearchInputView.editText
		recentChipsContainer = root.findViewById(R.id.recent_chips_container)
		recentChipsScroll = root.findViewById(R.id.recent_chips_scroll)
		watchNowRow = root.findViewById(R.id.watch_now_row)
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
		sortProgress = searchResultsPanel.findViewById(R.id.sort_progress)
		emptyStateContainer = searchResultsPanel.findViewById(R.id.empty_state_container)
		emptyStateIcon = searchResultsPanel.findViewById(R.id.empty_state_icon)
		emptyStateTitle = searchResultsPanel.findViewById(R.id.empty_state_title)
		emptyStateDescription = searchResultsPanel.findViewById(R.id.empty_state_description)
		emptyStateResetButton = searchResultsPanel.findViewById(R.id.empty_state_reset_button)
		searchRecycler = searchResultsPanel.findViewById(R.id.search_results)
		attachSearchResultsPanel(fullSearchResultsHost)
	}

	private fun setupSearchRecycler() {
		searchAdapter = StarMapSearchResultsAdapter(
			uiUtilities = app.uiUtilities,
			nightMode = nightMode,
			visibleEntries = visibleEntries,
			widToDisplayName = widToDisplayName,
			shouldShowInfoHeader = ::shouldShowInfoHeader,
			categoryPresetProvider = searchState::categoryPreset,
			eventTextProvider = ::resolveEventText,
			onEntrySelected = ::onSearchEntrySelected
		)
		catalogsAdapter = StarMapCatalogsAdapter(
			uiUtilities = app.uiUtilities,
			nightMode = nightMode,
			visibleEntries = visibleCatalogEntries,
			onCatalogSelected = ::onCatalogSelected
		)
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
		setupWatchNowRow()
		setupCategoryRows()
		setupMyDataRows()
		setupCatalogRows()
	}

	private fun setupWatchNowRow() {
		watchNowRow.setOnClickListener {
			openFullSearch(StarMapSearchQuickPresetType.WATCH_NOW, null)
		}
	}

	private fun setupListeners() {
		setupMyDataTabs()
		bindExploreSearchBarListeners()
		fullSearchBrowseToolbar.setNavigationOnClickListener { handleBrowseBackNavigation() }
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
		emptyStateResetButton.setOnClickListener { handleEmptyStateAction() }
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
				if (newQuery != searchState.query) {
					searchState.query = newQuery
					applyFiltersAndSort(scrollToTop = true)
				}
			}
		}
		exploreSearchEditText.addTextChangedListener(textWatcher)
		fullSearchEditText.addTextChangedListener(textWatcher)
	}

	private fun setupMyDataTabs() {
		myDataTabs.removeAllTabs()
		myDataTabs.addTab(myDataTabs.newTab().setText(R.string.favorites_item))
		myDataTabs.addTab(myDataTabs.newTab().setText(R.string.astro_daily_path))
		myDataTabs.addTab(myDataTabs.newTab().setText(R.string.astro_directions))
		myDataTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab) {
				val preset = when (tab.position) {
					0 -> StarMapSearchQuickPresetType.MY_DATA_FAVORITES
					1 -> StarMapSearchQuickPresetType.MY_DATA_DAILY_PATH
					else -> StarMapSearchQuickPresetType.MY_DATA_DIRECTIONS
				}
				if (searchState.quickPresetType == preset) {
					return
				}
				searchState.selectQuickPreset(preset, null)
				currentFullSearchMode = FullSearchMode.BROWSE
				applyMode(ScreenMode.FULL_SEARCH, requestKeyboard = false)
				applyFiltersAndSort(scrollToTop = true)
			}

			override fun onTabUnselected(tab: TabLayout.Tab) = Unit

			override fun onTabReselected(tab: TabLayout.Tab) = Unit
		})
	}

	private fun updateBrowseToolbarAppearance() {
		val iconColor = ColorUtilities.getDefaultIconColor(requireContext(), nightMode)
		exploreSearchBar.navigationIcon?.setTint(iconColor)
		exploreSearchBar.menu.findItem(R.id.action_search)?.icon?.setTint(iconColor)
		fullSearchAnchorBar.navigationIcon?.setTint(iconColor)
		fullSearchBrowseToolbar.navigationIcon?.setTint(iconColor)
		fullSearchBrowseToolbar.menu.findItem(R.id.action_search)?.icon?.setTint(iconColor)
		fullSearchBrowseToolbar.menu.findItem(R.id.action_search)?.isVisible = !isMyDataMode()
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
		window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
		exploreSearchInputView.updateSoftInputMode()
		fullSearchInputView.updateSoftInputMode()
	}

	private fun restoreSearchSoftInputMode() {
		val window = activity?.window ?: return
		val softInputMode = previousSoftInputMode ?: return
		window.setSoftInputMode(softInputMode)
		previousSoftInputMode = null
	}

	private fun restoreUiState(savedInstanceState: Bundle?) {
		if (savedInstanceState == null) {
			return
		}
		currentMode = savedInstanceState.getString(KEY_MODE)?.let { runCatching { ScreenMode.valueOf(it) }.getOrNull() }
			?: ScreenMode.EXPLORE
		currentFullSearchMode = savedInstanceState.getString(KEY_FULL_SEARCH_MODE)?.let {
			runCatching { FullSearchMode.valueOf(it) }.getOrNull()
		} ?: FullSearchMode.INPUT
		val catalogsBackSort = savedInstanceState.getString(KEY_CATALOGS_BACK_SORT)?.let {
			runCatching { StarMapSearchSortMode.valueOf(it) }.getOrNull()
		}
		val catalogsBackQuery = savedInstanceState.getString(KEY_CATALOGS_BACK_QUERY)
		catalogsBackState = if (catalogsBackSort != null && catalogsBackQuery != null) {
			CatalogsBackState(catalogsBackQuery, catalogsBackSort)
		} else {
			null
		}
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
			openFullSearch(StarMapSearchQuickPresetType.CATEGORY_SOLAR_SYSTEM, null)
		}
		addExploreRow(
			container = categoriesContainer,
			iconRes = R.drawable.ic_action_constellations,
			title = getString(R.string.astro_constellations),
			subtitle = null,
			count = null
		) {
			openFullSearch(StarMapSearchQuickPresetType.CATEGORY_CONSTELLATIONS, null)
		}
		addExploreRow(
			container = categoriesContainer,
			iconRes = R.drawable.ic_action_stars,
			title = getString(R.string.astro_stars),
			subtitle = null,
			count = null
		) {
			openFullSearch(StarMapSearchQuickPresetType.CATEGORY_STARS, null)
		}
		addExploreRow(
			container = categoriesContainer,
			iconRes = R.drawable.ic_action_nebulas,
			title = getString(R.string.astro_nebulas),
			subtitle = null,
			count = null
		) {
			openFullSearch(StarMapSearchQuickPresetType.CATEGORY_NEBULAS, null)
		}
		addExploreRow(
			container = categoriesContainer,
			iconRes = R.drawable.ic_action_star_clusters,
			title = getString(R.string.astro_star_clusters),
			subtitle = null,
			count = null
		) {
			openFullSearch(StarMapSearchQuickPresetType.CATEGORY_STAR_CLUSTERS, null)
		}
		addExploreRow(
			container = categoriesContainer,
			iconRes = R.drawable.ic_action_galaxy,
			title = getString(R.string.astro_deep_sky),
			subtitle = getString(R.string.astro_explore_deep_sky_subtitle),
			count = null,
			showDivider = false
		) {
			openFullSearch(StarMapSearchQuickPresetType.CATEGORY_DEEP_SKY, null)
		}
	}

	private fun bindExploreSearchBarListeners() {
		exploreSearchBar.setNavigationOnClickListener { dismiss() }
		exploreSearchBar.setOnClickListener { openFullSearch(StarMapSearchQuickPresetType.NONE, null) }
		exploreSearchBar.setOnMenuItemClickListener { item ->
			if (item.itemId == R.id.action_search) {
				openFullSearch(StarMapSearchQuickPresetType.NONE, null)
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
			openFullSearch(StarMapSearchQuickPresetType.MY_DATA_FAVORITES, null)
		}
		addExploreRow(
			container = myDataContainer,
			iconRes = R.drawable.ic_action_target_path_on,
			title = getString(R.string.astro_daily_path),
			subtitle = null,
			count = config.celestialPaths.size,
			showDivider = true
		) {
			openFullSearch(StarMapSearchQuickPresetType.MY_DATA_DAILY_PATH, null)
		}
		addExploreRow(
			container = myDataContainer,
			iconRes = R.drawable.ic_action_target_direction_on,
			title = getString(R.string.astro_directions),
			subtitle = null,
			count = config.directions.size,
			showDivider = false
		) {
			openFullSearch(StarMapSearchQuickPresetType.MY_DATA_DIRECTIONS, null)
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
				showDivider = index != minOf(2, catalogs.lastIndex)
			) {
				clearCatalogsBackState()
				openFullSearch(StarMapSearchQuickPresetType.CATALOG_WID, catalog.wid)
			}
		}
		catalogsViewAllCount.text = catalogs.size.toString()
		catalogsViewAllRow.setOnClickListener {
			openFullSearch(StarMapSearchQuickPresetType.CATALOGS, null)
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

	private fun openFullSearch(quickPresetType: StarMapSearchQuickPresetType, catalogWid: String?) {
		searchState.prepareForExploreEntry(quickPresetType, catalogWid)
		currentFullSearchMode = if (searchState.shouldOpenInBrowseMode()) {
			FullSearchMode.BROWSE
		} else {
			FullSearchMode.INPUT
		}
		applyMode(ScreenMode.FULL_SEARCH, requestKeyboard = currentFullSearchMode == FullSearchMode.INPUT)
		applyFiltersAndSort(scrollToTop = true)
	}

	private fun applyMode(mode: ScreenMode, requestKeyboard: Boolean) {
		updateBrowseToolbarAppearance()
		updateBrowseHeaderLayout()
		if (mode == ScreenMode.EXPLORE) {
			showExploreMode()
		} else if (currentFullSearchMode == FullSearchMode.BROWSE) {
			showBrowseMode()
		} else {
			val presentation = if (searchState.hasBrowseContext()) {
				InputPresentation.STANDALONE
			} else {
				InputPresentation.EXPLORE_BAR
			}
			showInputMode(presentation, requestKeyboard)
		}
		updateMyDataTabs()
		updateSortControls()
		updateFilterControls()
		updateEmptyStateContent()
		updateEmptyStateVisibility()
	}

	private fun renderBrowseHeader() {
		val title = getBrowseTitle()
		if (isMyDataMode()) {
			fullSearchBrowseHeader.title = ""
			fullSearchBrowseToolbar.title = title
		} else {
			fullSearchBrowseToolbar.title = ""
			fullSearchBrowseHeader.title = title
		}
	}

	private fun updateBrowseHeaderLayout() {
		if (!::fullSearchBrowseHeader.isInitialized || !::fullSearchBrowseToolbar.isInitialized) {
			return
		}
		val isCompactHeader = isMyDataMode()
		fullSearchBrowseHeader.isTitleEnabled = !isCompactHeader
		val targetHeight = if (isCompactHeader) {
			resources.getDimensionPixelSize(R.dimen.toolbar_height)
		} else {
			defaultBrowseHeaderHeight
		}
		val layoutParams = fullSearchBrowseHeader.layoutParams
		if (layoutParams.height != targetHeight) {
			layoutParams.height = targetHeight
			fullSearchBrowseHeader.layoutParams = layoutParams
		}
		if (!isCompactHeader) {
			fullSearchBrowseToolbar.title = ""
		}
	}

	private fun isMyDataMode(): Boolean {
		return when (searchState.quickPresetType) {
			StarMapSearchQuickPresetType.MY_DATA_FAVORITES,
			StarMapSearchQuickPresetType.MY_DATA_DAILY_PATH,
			StarMapSearchQuickPresetType.MY_DATA_DIRECTIONS -> true
			else -> false
		}
	}

	private fun getSelectedMyDataTabIndex(): Int? {
		return when (searchState.quickPresetType) {
			StarMapSearchQuickPresetType.MY_DATA_FAVORITES -> 0
			StarMapSearchQuickPresetType.MY_DATA_DAILY_PATH -> 1
			StarMapSearchQuickPresetType.MY_DATA_DIRECTIONS -> 2
			else -> null
		}
	}

	private fun updateMyDataTabs() {
		if (!::myDataTabs.isInitialized) {
			return
		}
		val visible = currentMode == ScreenMode.FULL_SEARCH && currentFullSearchMode == FullSearchMode.BROWSE && isMyDataMode()
		myDataTabs.isVisible = visible
		if (!visible) {
			return
		}
		val selectedIndex = getSelectedMyDataTabIndex() ?: return
		if (myDataTabs.selectedTabPosition != selectedIndex) {
			myDataTabs.getTabAt(selectedIndex)?.select()
		}
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
				pendingSearchHideTarget = if (searchState.hasBrowseContext()) HideTarget.BROWSE else HideTarget.EXPLORE
				getSearchView(currentInputPresentation).hide()
			} else {
				handleBrowseBackNavigation()
			}
			true
		} else {
			false
		}
	}

	private fun handleBrowseBackNavigation() {
		if (!restoreCatalogsListIfNeeded()) {
			showExploreMode()
		}
	}

	private fun restoreCatalogsListIfNeeded(): Boolean {
		val backState = catalogsBackState ?: return false
		if (searchState.quickPresetType != StarMapSearchQuickPresetType.CATALOG_WID) {
			return false
		}
		searchState.quickPresetType = StarMapSearchQuickPresetType.CATALOGS
		searchState.quickPresetCatalogWid = null
		searchState.query = backState.query
		searchState.sortMode = backState.sortMode
		searchState.typeFilter = StarMapSearchTypeFilter.SHOW_ALL
		searchState.nakedEyeOnly = false
		searchState.selectedCategories.clear()
		searchState.selectedCategories.add(StarMapSearchCategoryFilter.ALL)
		currentFullSearchMode = FullSearchMode.BROWSE
		clearCatalogsBackState()
		applyMode(ScreenMode.FULL_SEARCH, requestKeyboard = false)
		syncSearchQuery()
		applyFiltersAndSort(scrollToTop = true)
		return true
	}

	private fun clearCatalogsBackState() {
		catalogsBackState = null
	}

	private fun configureSearchView(presentation: InputPresentation, requestKeyboard: Boolean) {
		val searchView = getSearchView(presentation)
		searchView.setupWithSearchBar(
			if (presentation == InputPresentation.EXPLORE_BAR) exploreSearchBar else fullSearchAnchorBar
		)
		searchView.elevation = 0f
		searchView.translationZ = 0f
		searchView.setMenuItemsAnimated(false)
		searchView.findViewById<View>(MaterialR.id.open_search_view_divider)?.isVisible = false
		searchView.toolbar.elevation = 0f
		searchView.toolbar.translationZ = 0f
		searchView.toolbar.menu.clear()
		val iconColor = ColorUtilities.getDefaultIconColor(requireContext(), nightMode)
		searchView.toolbar.navigationIcon?.mutate()?.setTint(iconColor)
		if (presentation == InputPresentation.EXPLORE_BAR) {
			bindExploreSearchBarListeners()
		} else {
			fullSearchAnchorBar.setText(searchState.query)
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
		when (pendingSearchHideTarget ?: if (searchState.hasBrowseContext()) HideTarget.BROWSE else HideTarget.EXPLORE) {
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
		if (exploreSearchEditText.text.toString() != searchState.query) {
			exploreSearchEditText.setText(searchState.query)
		}
		if (fullSearchEditText.text.toString() != searchState.query) {
			fullSearchEditText.setText(searchState.query)
		}
		fullSearchAnchorBar.setText(searchState.query)
		exploreSearchInputView.setText(searchState.query)
		fullSearchInputView.setText(searchState.query)
		val editText = getSearchEditText(currentInputPresentation)
		if ((editText.text?.length ?: 0) > 0) {
			editText.setSelection(editText.length())
		}
		suppressQueryDispatch = false
	}

	private fun refreshPreparedEntries() {
		preparedEntries.clear()
		preparedCatalogEntries.clear()
		widToDisplayName.clear()
		riseSetCache.clear()
		visibleTonightCache.clear()
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
				StarMapSearchEntry(
					objectRef = obj,
					displayName = obj.localizedName ?: obj.name,
					magnitude = obj.magnitude,
					category = mapStarMapSearchCategory(obj),
					iconRes = AstroUtils.getObjectTypeIcon(obj.type),
					iconColor = if (obj.type.isSunSystem()) obj.color else ColorUtilities.getPrimaryIconColor(
						requireContext(),
						nightMode
					),
					catalogWids = obj.catalogs.mapTo(linkedSetOf()) { it.wid }
				)
			)
		}
		preparedCatalogEntries += buildCatalogEntries()
	}

	private fun buildCatalogEntries(): List<StarMapCatalogEntry> {
		val context = requireContext()
		val objectCountByCatalogWid = preparedEntries
			.flatMap { entry -> entry.catalogWids }
			.groupingBy { it }
			.eachCount()
		return dataProvider.getCatalogs(context).map { catalog ->
			StarMapCatalogEntry(
				catalog = catalog,
				displayName = catalog.name,
				description = dataProvider.getAstroArticle(context, catalog.wid)?.description,
				objectCount = objectCountByCatalogWid[catalog.wid] ?: 0
			)
		}
	}

	private fun shouldShowInfoHeader(): Boolean {
		return currentMode == ScreenMode.FULL_SEARCH &&
			currentFullSearchMode == FullSearchMode.BROWSE &&
			!shouldShowCatalogEntries() &&
			searchState.categoryPreset() != null
	}

	private fun getBrowseTitle(): String {
		return when (searchState.quickPresetType) {
			StarMapSearchQuickPresetType.WATCH_NOW -> getString(R.string.astro_explore_watch_now)
			StarMapSearchQuickPresetType.CATALOGS -> getString(R.string.astro_catalogs)
			StarMapSearchQuickPresetType.CATEGORY_SOLAR_SYSTEM -> getString(R.string.astro_solar_system)
			StarMapSearchQuickPresetType.CATEGORY_CONSTELLATIONS -> getString(R.string.astro_constellations)
			StarMapSearchQuickPresetType.CATEGORY_STARS -> getString(R.string.astro_stars)
			StarMapSearchQuickPresetType.CATEGORY_NEBULAS -> getString(R.string.astro_nebulas)
			StarMapSearchQuickPresetType.CATEGORY_STAR_CLUSTERS -> getString(R.string.astro_star_clusters)
			StarMapSearchQuickPresetType.CATEGORY_DEEP_SKY -> getString(R.string.astro_deep_sky)
			StarMapSearchQuickPresetType.MY_DATA_FAVORITES,
			StarMapSearchQuickPresetType.MY_DATA_DAILY_PATH,
			StarMapSearchQuickPresetType.MY_DATA_DIRECTIONS -> getString(R.string.astro_explore_my_data)
			StarMapSearchQuickPresetType.CATALOG_WID -> {
				val catalog = dataProvider.getCatalogs(requireContext()).firstOrNull { it.wid == searchState.quickPresetCatalogWid }
				catalog?.name ?: getString(R.string.shared_string_search)
			}
			StarMapSearchQuickPresetType.NONE -> getString(R.string.shared_string_search)
		}
	}

	private fun shouldShowCatalogEntries(): Boolean {
		return searchState.quickPresetType == StarMapSearchQuickPresetType.CATALOGS
	}

	private fun updateResultsAdapter() {
		if (!::searchRecycler.isInitialized) {
			return
		}
		val expectedAdapter = if (shouldShowCatalogEntries()) catalogsAdapter else searchAdapter
		if (searchRecycler.adapter !== expectedAdapter) {
			searchRecycler.adapter = expectedAdapter
		}
	}

	private fun getCurrentResultsCount(): Int {
		return if (shouldShowCatalogEntries()) visibleCatalogEntries.size else visibleEntries.size
	}

	private fun updateInfoCard() {
		if (!::searchAdapter.isInitialized) {
			return
		}
		val isInfoHeaderVisible = shouldShowInfoHeader()
		when {
			wasInfoHeaderVisible && isInfoHeaderVisible -> searchAdapter.notifyItemChanged(0)
			wasInfoHeaderVisible -> searchAdapter.notifyItemRemoved(0)
			isInfoHeaderVisible -> searchAdapter.notifyItemInserted(0)
		}
		wasInfoHeaderVisible = isInfoHeaderVisible
	}

	@SuppressLint("NotifyDataSetChanged")
	private fun applyFiltersAndSort(scrollToTop: Boolean) {
		filterAndSortJob?.cancel()
		val requestId = ++filterAndSortRequestId
		val stateSnapshot = searchState.snapshot()
		val isCatalogsMode = shouldShowCatalogEntries()
		val preparedEntriesSnapshot = preparedEntries.toList()
		val preparedCatalogEntriesSnapshot = preparedCatalogEntries.toList()
		val observerSnapshot = observerForComputations
		val nowSnapshot = nowForComputations
		val duskSnapshot = duskForComputations
		val dawnSnapshot = dawnForComputations
		updateResultsAdapter()
		updateSortControls()
		updateFilterControls()
		updateSortProgressVisibility(true)
		if (::emptyStateContainer.isInitialized) {
			emptyStateContainer.isVisible = false
		}
		filterAndSortJob = viewLifecycleOwner.lifecycleScope.launch {
			try {
				if (isCatalogsMode) {
					val filteredCatalogs = withContext(Dispatchers.Default) {
						filterAndSortCatalogs(stateSnapshot, preparedCatalogEntriesSnapshot)
					}
					if (requestId != filterAndSortRequestId || view == null) {
						return@launch
					}
					visibleCatalogEntries.clear()
					visibleCatalogEntries.addAll(filteredCatalogs)
					catalogsAdapter.notifyDataSetChanged()
				} else {
					val filteredEntries = withContext(Dispatchers.Default) {
						stateSnapshot.filterAndSort(
							preparedEntries = preparedEntriesSnapshot.map { it.copy() },
							visibleTonightProvider = { entry ->
								getVisibleTonight(
									entry,
									observerSnapshot,
									duskSnapshot,
									dawnSnapshot
								)
							},
							riseSortValueProvider = { entry ->
								getRiseSortValue(
									entry,
									observerSnapshot,
									nowSnapshot
								)
							},
							setSortValueProvider = { entry ->
								getSetSortValue(
									entry,
									observerSnapshot,
									nowSnapshot
								)
							}
						)
					}
					if (requestId != filterAndSortRequestId || view == null) {
						return@launch
					}
					visibleEntries.clear()
					visibleEntries.addAll(filteredEntries)
					searchAdapter.notifyDataSetChanged()
				}
				if (scrollToTop && ::searchRecycler.isInitialized && getCurrentResultsCount() > 0) {
					searchRecycler.scrollToPosition(0)
				}
				if (scrollToTop && currentMode == ScreenMode.FULL_SEARCH && currentFullSearchMode == FullSearchMode.BROWSE) {
					fullSearchAppBar.setExpanded(true, false)
				}
				updateEmptyStateVisibility()
			} finally {
				if (requestId == filterAndSortRequestId && view != null) {
					updateSortProgressVisibility(false)
				}
			}
		}
	}

	private fun filterAndSortCatalogs(
		stateSnapshot: StarMapSearchStateSnapshot,
		preparedCatalogEntries: List<StarMapCatalogEntry>
	): List<StarMapCatalogEntry> {
		val queryLower = stateSnapshot.query.trim().lowercase(Locale.getDefault())
		val filteredEntries = preparedCatalogEntries.filter { entry ->
			if (queryLower.isEmpty()) {
				true
			} else {
				entry.displayName.lowercase(Locale.getDefault()).contains(queryLower) ||
					entry.description.orEmpty().lowercase(Locale.getDefault()).contains(queryLower)
			}
		}
		return when (stateSnapshot.sortMode) {
			StarMapSearchSortMode.NAME_DESC -> filteredEntries.sortedByDescending { it.displayName.lowercase(Locale.getDefault()) }
			else -> filteredEntries.sortedBy { it.displayName.lowercase(Locale.getDefault()) }
		}
	}

	private fun updateSortProgressVisibility(isVisible: Boolean) {
		if (::sortProgress.isInitialized) {
			sortProgress.isVisible = isVisible
		}
	}

	private fun ensureRiseSet(
		entry: StarMapSearchEntry,
		observer: Observer = observerForComputations,
		now: ZonedDateTime = nowForComputations
	) {
		if (entry.riseSetCalculated) {
			return
		}
		riseSetCache[entry.objectRef.id]?.let { cachedRiseSet ->
			entry.nextRise = cachedRiseSet.nextRise
			entry.nextSet = cachedRiseSet.nextSet
			entry.riseSetCalculated = true
			return
		}
		val (rise, set) = AstroUtils.nextRiseSet(entry.objectRef, now, observer)
		entry.nextRise = rise
		entry.nextSet = set
		entry.riseSetCalculated = true
		riseSetCache[entry.objectRef.id] = RiseSetCacheEntry(rise, set)
	}

	private fun getVisibleTonight(
		entry: StarMapSearchEntry,
		observer: Observer = observerForComputations,
		dusk: ZonedDateTime = duskForComputations,
		dawn: ZonedDateTime = dawnForComputations
	): Boolean {
		if (entry.visibleTonightCalculated) {
			return entry.isVisibleTonight
		}
		visibleTonightCache[entry.objectRef.id]?.let { cachedVisibleTonight ->
			entry.isVisibleTonight = cachedVisibleTonight
			entry.visibleTonightCalculated = true
			return cachedVisibleTonight
		}
		val (nightRise, nightSet) = AstroUtils.nextRiseSet(
			entry.objectRef,
			dusk,
			observer,
			dusk,
			dawn
		)
		val visibleAtDusk = AstroUtils.altitude(entry.objectRef, dusk, observer) > 0
		entry.isVisibleTonight = visibleAtDusk || nightRise != null || nightSet != null
		entry.visibleTonightCalculated = true
		visibleTonightCache[entry.objectRef.id] = entry.isVisibleTonight
		return entry.isVisibleTonight
	}

	private fun getRiseSortValue(
		entry: StarMapSearchEntry,
		observer: Observer = observerForComputations,
		now: ZonedDateTime = nowForComputations
	): Long {
		ensureRiseSet(entry, observer, now)
		return entry.nextRise?.toInstant()?.toEpochMilli() ?: Long.MAX_VALUE
	}

	private fun getSetSortValue(
		entry: StarMapSearchEntry,
		observer: Observer = observerForComputations,
		now: ZonedDateTime = nowForComputations
	): Long {
		ensureRiseSet(entry, observer, now)
		return entry.nextSet?.toInstant()?.toEpochMilli() ?: Long.MAX_VALUE
	}

	private fun updateSortControls() {
		if (!::sortText.isInitialized || !::sortIcon.isInitialized) return
		val (iconRes, textRes) = when (searchState.sortMode) {
			StarMapSearchSortMode.NAME_ASC -> R.drawable.ic_action_sort_by_name_ascending to R.string.sort_name_ascending
			StarMapSearchSortMode.NAME_DESC -> R.drawable.ic_action_sort_by_name_descending to R.string.sort_name_descending
			StarMapSearchSortMode.BRIGHTEST_FIRST -> R.drawable.ic_action_sort_short_to_long to R.string.astro_sort_brightest_first
			StarMapSearchSortMode.FAINTEST_FIRST -> R.drawable.ic_action_sort_long_to_short to R.string.astro_sort_faintest_first
			StarMapSearchSortMode.RISES_SOONEST -> R.drawable.ic_action_sort_date_1 to R.string.astro_sort_rises_soonest
			StarMapSearchSortMode.SETS_SOONEST -> R.drawable.ic_action_sort_date_31 to R.string.astro_sort_sets_soonest
		}
		sortText.setText(textRes)
		sortIcon.setImageDrawable(app.uiUtilities.getIcon(iconRes, ColorUtilities.getActiveIconColorId(nightMode)))
	}

	private fun updateFilterControls() {
		if (!::filterText.isInitialized || !::filterButton.isInitialized) return
		filterButton.isVisible = !shouldShowCatalogEntries()
		filterText.text = getString(R.string.filter_tracks_count, searchState.calculateFilterCount())
	}

	private fun updateEmptyStateContent() {
		if (!::emptyStateTitle.isInitialized || !::emptyStateDescription.isInitialized || !::emptyStateResetButton.isInitialized) {
			return
		}
		val resetButton = emptyStateResetButton as? TextView
		if (isMyDataMode()) {
			val (iconRes, titleRes, descriptionRes) = when (searchState.quickPresetType) {
				StarMapSearchQuickPresetType.MY_DATA_DIRECTIONS -> Triple(
					R.drawable.ic_direction_arrow,
					R.string.astro_my_data_no_directions_title,
					R.string.astro_my_data_no_directions_description
				)
				StarMapSearchQuickPresetType.MY_DATA_DAILY_PATH -> Triple(
					R.drawable.ic_action_target_path_off,
					R.string.astro_my_data_no_daily_paths_title,
					R.string.astro_my_data_no_daily_paths_description
				)
				else -> Triple(
					R.drawable.ic_action_bookmark,
					R.string.astro_my_data_no_favorites_title,
					R.string.astro_my_data_no_favorites_description
				)
			}
			emptyStateIcon.setImageDrawable(app.uiUtilities.getIcon(iconRes, ColorUtilities.getDefaultIconColorId(nightMode)))
			emptyStateTitle.setText(titleRes)
			emptyStateDescription.setText(descriptionRes)
			resetButton?.setText(R.string.astro_go_to_map)
		} else {
			emptyStateIcon.setImageDrawable(app.uiUtilities.getIcon(R.drawable.ic_action_ufo, ColorUtilities.getDefaultIconColorId(nightMode)))
			emptyStateTitle.setText(R.string.nothing_found)
			emptyStateDescription.setText(R.string.astro_search_empty_description)
			resetButton?.setText(R.string.shared_string_reset)
		}
	}

	private fun updateEmptyStateVisibility() {
		if (!::emptyStateContainer.isInitialized) return
		emptyStateContainer.isVisible = currentMode == ScreenMode.FULL_SEARCH && getCurrentResultsCount() == 0
	}

	private fun handleEmptyStateAction() {
		if (isMyDataMode()) {
			dismiss()
		} else {
			resetAllSearchParams()
		}
	}

	private fun resetAllSearchParams() {
		if (shouldShowCatalogEntries()) {
			searchState.query = ""
			searchState.sortMode = StarMapSearchSortMode.NAME_ASC
			currentFullSearchMode = if (currentMode == ScreenMode.FULL_SEARCH && currentFullSearchMode == FullSearchMode.BROWSE) {
				FullSearchMode.BROWSE
			} else {
				FullSearchMode.INPUT
			}
			syncSearchQuery()
			if (currentMode == ScreenMode.FULL_SEARCH && currentFullSearchMode == FullSearchMode.INPUT) {
				showInputMode(InputPresentation.STANDALONE, requestKeyboard = false)
			} else if (currentMode == ScreenMode.FULL_SEARCH) {
				showBrowseMode()
			}
		} else {
			searchState.reset()
			currentFullSearchMode = FullSearchMode.INPUT
			syncSearchQuery()
			if (currentMode == ScreenMode.FULL_SEARCH) {
				showInputMode(InputPresentation.STANDALONE, requestKeyboard = false)
			}
		}
		applyFiltersAndSort(scrollToTop = true)
	}

	private fun addRecentChip(label: String) {
		searchState.addRecentChip(label)
		renderRecentChips()
	}

	private fun renderRecentChips() {
		if (!::recentChipsContainer.isInitialized) return
		recentChipsContainer.removeAllViews()
		recentChipsScroll.isVisible = searchState.recentChips.isNotEmpty()
		if (searchState.recentChips.isEmpty()) return

		searchState.recentChips.forEach { chipTitle ->
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
				searchState.selectQuickPreset(StarMapSearchQuickPresetType.NONE, null)
				currentFullSearchMode = FullSearchMode.INPUT
				searchState.query = chipTitle
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

	private fun onSearchEntrySelected(entry: StarMapSearchEntry) {
		addRecentChip(entry.displayName)
		onObjectSelected?.invoke(entry.objectRef)
		parentFragmentManager.beginTransaction()
			.hide(this)
			.addToBackStack(TAG)
			.commit()
	}

	private fun onCatalogSelected(entry: StarMapCatalogEntry) {
		catalogsBackState = CatalogsBackState(
			query = searchState.query,
			sortMode = searchState.sortMode
		)
		openFullSearch(StarMapSearchQuickPresetType.CATALOG_WID, entry.catalog.wid)
	}

	private fun resolveEventText(entry: StarMapSearchEntry): CharSequence {
		ensureRiseSet(entry)
		val rise = entry.nextRise
		val set = entry.nextSet
		val eventText = when {
			rise != null && set != null -> {
				if (rise.isBefore(set)) {
					formatEvent(rise, isRise = true)
				} else {
					formatEvent(set, isRise = false)
				}
			}
			rise != null -> formatEvent(rise, isRise = true)
			set != null -> formatEvent(set, isRise = false)
			entry.objectRef.altitude > 0 -> {
				getString(R.string.astro_search_always_up)
			}
			else -> {
				getString(R.string.astro_search_never_rises)
			}
		}
		return replaceEventArrowWithIcon(eventText)
	}

	private fun replaceEventArrowWithIcon(text: String): CharSequence {
		val (arrow, iconRes) = when {
			text.contains(RISE_ARROW) -> RISE_ARROW to R.drawable.ic_action_arrow_top_right_16
			text.contains(SET_ARROW) -> SET_ARROW to R.drawable.ic_action_arrow_bottom_right_16
			text.contains(UP_ARROW) -> UP_ARROW to R.drawable.ic_action_arrow_up_16
			text.contains(DOWN_ARROW) -> DOWN_ARROW to R.drawable.ic_action_arrow_down_16
			else -> return text
		}
		val icon = app.uiUtilities.getIcon(iconRes, ColorUtilities.getSecondaryIconColorId(nightMode))
		icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
		return AndroidUtils.replaceCharsWithIcon(text, icon, arrayOf(arrow))
	}

	private fun formatEvent(time: ZonedDateTime, isRise: Boolean): String {
		val formattedTime = AstroUtils.formatLocalTime(
			Time.Companion.fromMillisecondsSince1970(time.toInstant().toEpochMilli())
		)
		val daysBetween = ChronoUnit.DAYS.between(nowForComputations.toLocalDate(), time.toLocalDate())
		return if (daysBetween == 1L) {
			val tomorrow = getString(R.string.tomorrow)
			if (isRise) {
				getString(R.string.astro_search_rises_tomorrow, tomorrow, formattedTime)
			} else {
				getString(R.string.astro_search_sets_tomorrow, tomorrow, formattedTime)
			}
		} else if (isRise) {
			getString(R.string.astro_search_rises_at, formattedTime)
		} else {
			getString(R.string.astro_search_sets_at, formattedTime)
		}
	}

	private fun showSortPopup(anchor: View) {
		dismissSortPopup()
		val activeColor = ColorUtilities.getActiveColor(app, nightMode)
		val secondaryTextColor = ColorUtilities.getSecondaryTextColor(requireContext(), nightMode)
		val items = if (shouldShowCatalogEntries()) {
			listOf(
				createPopupHeaderItem(getString(R.string.sort_by), secondaryTextColor),
				createRadioPopupItem(
					getString(R.string.sort_name_ascending),
					searchState.sortMode == StarMapSearchSortMode.NAME_ASC,
					activeColor
				) {
					searchState.sortMode = StarMapSearchSortMode.NAME_ASC
					updateSortControls()
					applyFiltersAndSort(scrollToTop = true)
				},
				createRadioPopupItem(
					getString(R.string.sort_name_descending),
					searchState.sortMode == StarMapSearchSortMode.NAME_DESC,
					activeColor
				) {
					searchState.sortMode = StarMapSearchSortMode.NAME_DESC
					updateSortControls()
					applyFiltersAndSort(scrollToTop = true)
				}
			)
		} else {
			listOf(
				createPopupHeaderItem(getString(R.string.sort_by), secondaryTextColor),
				createRadioPopupItem(getString(R.string.sort_name_ascending), searchState.sortMode == StarMapSearchSortMode.NAME_ASC, activeColor) {
					searchState.sortMode = StarMapSearchSortMode.NAME_ASC
					updateSortControls()
					applyFiltersAndSort(scrollToTop = true)
				},
				createRadioPopupItem(getString(R.string.sort_name_descending), searchState.sortMode == StarMapSearchSortMode.NAME_DESC, activeColor) {
					searchState.sortMode = StarMapSearchSortMode.NAME_DESC
					updateSortControls()
					applyFiltersAndSort(scrollToTop = true)
				},
				createRadioPopupItem(
					getString(R.string.astro_sort_brightest_first),
					searchState.sortMode == StarMapSearchSortMode.BRIGHTEST_FIRST,
					activeColor,
					showTopDivider = true
				) {
					searchState.sortMode = StarMapSearchSortMode.BRIGHTEST_FIRST
					updateSortControls()
					applyFiltersAndSort(scrollToTop = true)
				},
				createRadioPopupItem(
					getString(R.string.astro_sort_faintest_first),
					searchState.sortMode == StarMapSearchSortMode.FAINTEST_FIRST,
					activeColor
				) {
					searchState.sortMode = StarMapSearchSortMode.FAINTEST_FIRST
					updateSortControls()
					applyFiltersAndSort(scrollToTop = true)
				},
				createRadioPopupItem(
					getString(R.string.astro_sort_rises_soonest),
					searchState.sortMode == StarMapSearchSortMode.RISES_SOONEST,
					activeColor,
					showTopDivider = true
				) {
					searchState.sortMode = StarMapSearchSortMode.RISES_SOONEST
					updateSortControls()
					applyFiltersAndSort(scrollToTop = true)
				},
				createRadioPopupItem(
					getString(R.string.astro_sort_sets_soonest),
					searchState.sortMode == StarMapSearchSortMode.SETS_SOONEST,
					activeColor
				) {
					searchState.sortMode = StarMapSearchSortMode.SETS_SOONEST
					updateSortControls()
					applyFiltersAndSort(scrollToTop = true)
				}
			)
		}
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
		if (shouldShowCatalogEntries()) {
			return
		}
		dismissFilterPopup()
		val activeColor = ColorUtilities.getActiveColor(app, nightMode)
		val secondaryTextColor = ColorUtilities.getSecondaryTextColor(requireContext(), nightMode)
		val items = mutableListOf(
			createPopupHeaderItem(getString(R.string.shared_string_type), secondaryTextColor),
			createRadioPopupItem(
				getString(R.string.astro_filter_show_all),
				searchState.typeFilter == StarMapSearchTypeFilter.SHOW_ALL,
				activeColor
			) {
				searchState.typeFilter = StarMapSearchTypeFilter.SHOW_ALL
				applyFiltersAndSort(scrollToTop = true)
			},
			createRadioPopupItem(
				getString(R.string.astro_filter_visible_now),
				searchState.typeFilter == StarMapSearchTypeFilter.VISIBLE_NOW,
				activeColor
			) {
				searchState.typeFilter = StarMapSearchTypeFilter.VISIBLE_NOW
				applyFiltersAndSort(scrollToTop = true)
			},
			createRadioPopupItem(
				getString(R.string.astro_filter_visible_tonight),
				searchState.typeFilter == StarMapSearchTypeFilter.VISIBLE_TONIGHT,
				activeColor
			) {
				searchState.typeFilter = StarMapSearchTypeFilter.VISIBLE_TONIGHT
				applyFiltersAndSort(scrollToTop = true)
			},
			createCheckPopupItem(getString(R.string.astro_filter_naked_eye), searchState.nakedEyeOnly, activeColor, showTopDivider = true) {
				searchState.nakedEyeOnly = !searchState.nakedEyeOnly
				applyFiltersAndSort(scrollToTop = true)
			}
		)
		if (!searchState.isCategoryPreset()) {
			items += listOf(
				createPopupHeaderItem(getString(R.string.favourites_edit_dialog_category), secondaryTextColor, showTopDivider = true),
				createCheckPopupItem(
					getString(R.string.shared_string_all),
					searchState.selectedCategories.contains(StarMapSearchCategoryFilter.ALL),
					activeColor
				) {
					searchState.toggleCategoryFilter(StarMapSearchCategoryFilter.ALL)
					applyFiltersAndSort(scrollToTop = true)
				},
				createCheckPopupItem(
					getString(R.string.astro_solar_system),
					searchState.selectedCategories.contains(StarMapSearchCategoryFilter.SOLAR_SYSTEM),
					activeColor
				) {
					searchState.toggleCategoryFilter(StarMapSearchCategoryFilter.SOLAR_SYSTEM)
					applyFiltersAndSort(scrollToTop = true)
				},
				createCheckPopupItem(
					getString(R.string.astro_constellations),
					searchState.selectedCategories.contains(StarMapSearchCategoryFilter.CONSTELLATIONS),
					activeColor
				) {
					searchState.toggleCategoryFilter(StarMapSearchCategoryFilter.CONSTELLATIONS)
					applyFiltersAndSort(scrollToTop = true)
				},
				createCheckPopupItem(
					getString(R.string.astro_stars),
					searchState.selectedCategories.contains(StarMapSearchCategoryFilter.STARS),
					activeColor
				) {
					searchState.toggleCategoryFilter(StarMapSearchCategoryFilter.STARS)
					applyFiltersAndSort(scrollToTop = true)
				},
				createCheckPopupItem(
					getString(R.string.astro_nebulas),
					searchState.selectedCategories.contains(StarMapSearchCategoryFilter.NEBULAS),
					activeColor
				) {
					searchState.toggleCategoryFilter(StarMapSearchCategoryFilter.NEBULAS)
					applyFiltersAndSort(scrollToTop = true)
				},
				createCheckPopupItem(
					getString(R.string.astro_star_clusters),
					searchState.selectedCategories.contains(StarMapSearchCategoryFilter.STAR_CLUSTERS),
					activeColor
				) {
					searchState.toggleCategoryFilter(StarMapSearchCategoryFilter.STAR_CLUSTERS)
					applyFiltersAndSort(scrollToTop = true)
				},
				createCheckPopupItem(
					getString(R.string.astro_deep_sky),
					searchState.selectedCategories.contains(StarMapSearchCategoryFilter.DEEP_SKY),
					activeColor
				) {
					searchState.toggleCategoryFilter(StarMapSearchCategoryFilter.DEEP_SKY)
					applyFiltersAndSort(scrollToTop = true)
				}
			)
		}
		filterPopup = PopUpMenu.showAndGet(createPopupDisplayData(anchor, items, alignEnd = true, limitHeight = true))
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
}
