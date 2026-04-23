package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.tabs.TabLayout
import net.osmand.plus.R
import net.osmand.plus.base.BaseMaterialFragment
import net.osmand.plus.chooseplan.ChoosePlanFragment
import net.osmand.plus.chooseplan.OsmAndFeature
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents
import net.osmand.plus.download.DownloadValidationManager
import net.osmand.plus.mapcontextmenu.gallery.GalleryController
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.astronomy.AstroArticle
import net.osmand.plus.plugins.astronomy.AstronomyPlugin
import net.osmand.plus.plugins.astronomy.Catalog
import net.osmand.plus.plugins.astronomy.SkyObject
import net.osmand.plus.plugins.astronomy.StarMapFragment
import net.osmand.plus.plugins.astronomy.utils.AstroUtils
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.utils.InsetsUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class AstroContextMenuFragment : BaseMaterialFragment(), DownloadEvents {

	private var galleryController: GalleryController? = null
	private var galleryLoader: AstroGalleryLoader? = null

	private var skyObject: SkyObject? = null
	private var article: AstroArticle? = null
	private var uiState = AstroContextUiState()
	private val visibilityController by lazy { AstroVisibilityCardController(app) }
	private val scheduleController by lazy { AstroScheduleCardController(app) }
	private val knowledgeBaseController by lazy { AstroKnowledgeBaseController(app) }
	private val cardFactory by lazy { AstroContextCardFactory() }
	private val parent: StarMapFragment
		get() = requireParentFragment() as StarMapFragment

	private lateinit var appBarLayout: AppBarLayout
	private lateinit var recyclerView: RecyclerView
	private lateinit var metricsList: RecyclerView
	private lateinit var metricsAdapter: MetricsAdapter
	private lateinit var adapter: AstroContextMenuAdapter
	private lateinit var headerCard: MaterialCardView
	private lateinit var collapsedToolbar: View

	private lateinit var headerTitle: TextView
	private lateinit var headerType: TextView
	private lateinit var headerCloseButton: View
	private lateinit var collapsedTitle: TextView
	private lateinit var collapsedCloseButton: View

	private lateinit var saveButton: View
	private lateinit var saveTitle: TextView
	private lateinit var saveIcon: ImageView

	private lateinit var locationButton: View
	private lateinit var locationTitle: TextView
	private lateinit var locationIcon: ImageView

	private lateinit var directionButton: View
	private lateinit var directionTitle: TextView
	private lateinit var directionIcon: ImageView

	private lateinit var pathButton: View
	private lateinit var pathTitle: TextView
	private lateinit var pathIcon: ImageView

	private lateinit var bottomTabsContainer: View
	private lateinit var bottomTabs: TabLayout

	private var selectedBottomTab: Int = 0
	private var appBarBaseTopPadding = 0
	private var tabsContainerBaseBottomPadding = 0
	private var recyclerBaseBottomPadding = 0
	private var systemTopInset = 0
	private var currentSheetTop = Int.MAX_VALUE
	private var lastHeaderHeight = 0
	private var lastTabsHeight = 0

	private var bottomSheetContainer: View? = null
	private var bottomSheetBehavior: AstroBottomSheetBehavior<View>? = null
	private var bottomSheetBackground: MaterialShapeDrawable? = null
	private var bottomSheetCornerRadiusPx = 0f
	private var bottomSheetCallback: BottomSheetBehavior.BottomSheetCallback? = null

	private var tabSelectedListener: TabLayout.OnTabSelectedListener? = null
	private var recyclerScrollListener: RecyclerView.OnScrollListener? = null
	private var isSyncingTabSelection = false
	private var isProgrammaticSectionScroll = false
	private var pendingProgrammaticSectionTab: Int? = null
	private var programmaticSectionScrollToken = 0

	companion object {
		val TAG: String = AstroContextMenuFragment::class.java.simpleName
		private const val ARG_SKY_OBJECT_ID = "skyObjectId"
		private const val TAB_OVERVIEW = 0
		private const val TAB_VISIBILITY = 1
		private const val TAB_SCHEDULE = 2
		private const val SHEET_CORNER_RADIUS_DP = 12f
		private const val COLLAPSED_EXTRA_DP = 100f

		fun newInstance(skyObject: SkyObject): AstroContextMenuFragment {
			val fragment = AstroContextMenuFragment()
			val args = Bundle()
			args.putString(ARG_SKY_OBJECT_ID, skyObject.id)
			fragment.arguments = args
			return fragment
		}
	}

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = super.getInsetTargets()

		return collection
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val dialogManager = app.dialogManager
		galleryController =
			dialogManager.findController(GalleryController.PROCESS_ID) as GalleryController?
		if (galleryController == null) {
			dialogManager.register(GalleryController.PROCESS_ID, GalleryController(app))
			galleryController =
				dialogManager.findController(GalleryController.PROCESS_ID) as GalleryController?
		}
		galleryController?.let { controller ->
			galleryLoader = AstroGalleryLoader(
				app = app,
				galleryController = controller,
				mapActivityProvider = { mapActivity },
				onStateChanged = ::onGalleryStateChanged
			)
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val view = inflater.inflate(R.layout.astro_context_menu_fragment, container, false)

		initializeViews(view)
		setupRecyclerView()
		bindControllerCallbacks()
		submitCards()

		val resolvedObject = arguments?.getString(ARG_SKY_OBJECT_ID)
			?.let(parent::findTrackableObjectById)
			?: arguments?.getString("skyObjectName")?.let { name ->
				parent.getTrackableObjects().firstOrNull { it.name == name }
			}
		resolvedObject?.let(::updateObjectInfo)

		return view
	}

	fun updateObjectInfo(obj: SkyObject) {
		this.skyObject = obj
		if (!isAdded) {
			return
		}
		val currentTime = getCurrentGraphTime()
		val currentDate = currentTime.toLocalDate()
		val objectChanged = uiState.selectedObjectId != obj.id
		if (objectChanged) {
			resetOverviewStateForNewObject()
		}
		uiState = if (objectChanged) {
			galleryLoader?.cancel()
			AstroContextUiState(
				selectedObjectId = obj.id,
				currentLocalDate = currentDate,
				visibilityCursorReferenceTimeMillis = currentTime.toInstant().toEpochMilli(),
				schedulePeriodStart = currentDate
			)
		} else {
			uiState.copy(
				selectedObjectId = obj.id,
				currentLocalDate = currentDate,
				visibilityCursorReferenceTimeMillis = uiState.visibilityCursorReferenceTimeMillis
					?: currentTime.toInstant().toEpochMilli(),
				schedulePeriodStart = uiState.schedulePeriodStart ?: currentDate
			)
		}
		article =
			PluginsHelper.requirePlugin(AstronomyPlugin::class.java).dataProvider.getAstroArticle(
				app,
				obj.wid,
				app.localeHelper.language
			)

		setTitle(obj.niceName())
		headerType.text = buildHeaderTypeText(obj)
		updateBottomTabIcons(obj.type)

		updateMetrics(obj)
		updateButtons(obj)
		updateVisibilityCard(obj)
		updateScheduleCard(obj)
		ensureKnowledgeCardPrerequisites()
		if (uiState.galleryState == AstroGalleryCardState.Loading) {
			galleryLoader?.startLoading(obj.wid)
		}
		submitCards()
	}

	fun onTimeChanged() {
		val obj = skyObject ?: return
		if (!isAdded) {
			return
		}
		updateMetrics(obj, useTargetCoordinates = true)

		val currentDate = getCurrentGraphTime().toLocalDate()
		val previousDate = uiState.currentLocalDate
		if (previousDate == currentDate) {
			return
		}

		val currentScheduleStart = uiState.schedulePeriodStart
		val shouldShiftSchedulePeriod =
			currentScheduleStart == null || currentScheduleStart == previousDate
		uiState = uiState.copy(
			currentLocalDate = currentDate,
			schedulePeriodStart = if (shouldShiftSchedulePeriod) currentDate else currentScheduleStart
		)

		if (uiState.selectedVisibilityDateOverride == null) {
			updateVisibilityCard(obj)
		}
		if (shouldShiftSchedulePeriod) {
			updateScheduleCard(obj, currentDate)
		}
		submitCards()
	}

	private fun buildHeaderTypeText(obj: SkyObject): String {
		val typeName = AstroUtils.getAstroTypeName(app, obj.type.titleKey)
		val type = obj.type
		val parentGroup = when {
			type == SkyObject.Type.MOON -> app.getString(R.string.astro_type_earth)
			type.isSunSystem() -> app.getString(R.string.astro_solar_system)
			type == SkyObject.Type.STAR -> {
				parent.viewModel.constellations.value
					?.firstOrNull { constellation ->
						constellation.lines.any { (a, b) -> a == obj.hip || b == obj.hip }
					}
					?.let { it.localizedName ?: it.name }
			}

			else -> null
		}.takeUnless { it.isNullOrBlank() || it == "null" } ?: app.getString(R.string.astro_deep_sky)

		return app.getString(R.string.ltr_or_rtl_combine_via_bold_point, typeName, parentGroup)
	}

	private fun updateButtons(obj: SkyObject) {
		fun bindButtons() {
			saveIcon.setImageDrawable(
				uiUtilities.getIcon(
					if (obj.isFavorite) R.drawable.ic_action_bookmark_filled else R.drawable.ic_action_bookmark,
					ColorUtilities.getActiveIconColorId(nightMode)
				)
			)
			saveTitle.text = app.getString(R.string.shared_string_save)

			locationIcon.setImageDrawable(
				uiUtilities.getIcon(
					R.drawable.ic_action_location_16,
					ColorUtilities.getActiveIconColorId(nightMode)
				)
			)
			locationTitle.text = app.getString(R.string.astro_locate)

			directionIcon.setImageDrawable(
				AndroidUtils.getDrawableForDirection(
					app,
					uiUtilities.getIcon(
						if (obj.showDirection) {
							R.drawable.ic_action_target_direction_on
						} else {
							R.drawable.ic_action_target_direction_off
						},
						ColorUtilities.getActiveIconColorId(nightMode)
					)
				)
			)
			directionTitle.text = app.getString(R.string.astro_direction)

			pathIcon.setImageDrawable(
				AndroidUtils.getDrawableForDirection(
					app,
					uiUtilities.getIcon(
						if (obj.showCelestialPath) R.drawable.ic_action_target_path_on else R.drawable.ic_action_target_path_off,
						ColorUtilities.getActiveIconColorId(nightMode)
					)
				)
			)
			pathTitle.text = app.getString(R.string.astro_path)
		}

		saveButton.setOnClickListener {
			obj.isFavorite = !obj.isFavorite
			val swSettings = PluginsHelper.requirePlugin(AstronomyPlugin::class.java).astroSettings
			if (obj.isFavorite) swSettings.addFavorite(obj.id) else swSettings.removeFavorite(obj.id)
			parent.viewModel.refreshSkyObjects()
			parent.starView.invalidate()

			bindButtons()
		}

		locationButton.setOnClickListener {
			parent.starView.setSelectedObject(obj, center = true, animate = true)
			bindButtons()
		}

		directionButton.setOnClickListener {
			obj.showDirection = !obj.showDirection
			val swSettings = PluginsHelper.requirePlugin(AstronomyPlugin::class.java).astroSettings
			if (obj.showDirection) {
				obj.colorIndex = swSettings.addDirection(obj.id)
			} else {
				swSettings.removeDirection(obj.id)
			}
			parent.viewModel.refreshSkyObjects()
			parent.starView.invalidate()
			bindButtons()
		}

		pathButton.setOnClickListener {
			obj.showCelestialPath = !obj.showCelestialPath
			val swSettings = PluginsHelper.requirePlugin(AstronomyPlugin::class.java).astroSettings
			if (obj.showCelestialPath) {
				swSettings.addCelestialPath(obj.id)
			} else {
				swSettings.removeCelestialPath(obj.id)
			}
			parent.starView.setObjectPinned(obj, obj.showCelestialPath, true)
			parent.viewModel.refreshSkyObjects()
			parent.starView.invalidate()
			bindButtons()
		}

		bindButtons()
	}

	private fun updateMetrics(obj: SkyObject, useTargetCoordinates: Boolean = false) {
		val metrics = ArrayList<MetricsAdapter.MetricUi>()

		val azimuth = if (useTargetCoordinates) obj.targetAzimuth else obj.azimuth
		val altitude = if (useTargetCoordinates) obj.targetAltitude else obj.altitude
		val az = String.format(Locale.getDefault(), "%.1f°", azimuth)
		metrics.add(MetricsAdapter.MetricUi(az, getString(R.string.shared_string_azimuth)))

		val alt = String.format(Locale.getDefault(), "%.1f°", altitude)
		metrics.add(MetricsAdapter.MetricUi(alt, getString(R.string.altitude)))

		metrics.add(
			MetricsAdapter.MetricUi(
				obj.magnitude.toString(),
				getString(R.string.shared_string_magnitude)
			)
		)

		val currentTime = getCurrentGraphTime()
		val timeFormatter = createUiTimeFormatter()
		val startLocal = currentTime.toLocalDate().atTime(12, 0).atZone(currentTime.zone)
		val endLocal = startLocal.plusDays(1)
		val observer = parent.starView.observer
		val (riseTime, setTime) = AstroUtils.nextRiseSet(
			obj = obj,
			startSearch = startLocal,
			obs = observer,
			windowStart = startLocal,
			windowEnd = endLocal
		)

		if (riseTime != null) {
			metrics.add(
				MetricsAdapter.MetricUi(
					riseTime.format(timeFormatter),
					getString(R.string.astro_rise)
				)
			)
		}

		if (setTime != null) {
			metrics.add(
				MetricsAdapter.MetricUi(
					setTime.format(timeFormatter),
					getString(R.string.astro_set)
				)
			)
		}

		metricsAdapter.submit(metrics)
	}

	private fun createUiTimeFormatter(): DateTimeFormatter {
		return if (android.text.format.DateFormat.is24HourFormat(app)) {
			DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
		} else {
			DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
		}
	}

	private fun setTitle(name: String) {
		collapsedTitle.text = name
		headerTitle.text = name
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		setupBottomSheet()
		applyLegacyInsetsFallbackIfNeeded()

		appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBar, offset ->

			val total = appBar.totalScrollRange.toFloat()
			if (total == 0f) return@OnOffsetChangedListener

			val progress = abs(offset) / total
			val clamped = progress.coerceIn(0f, 1f)

			headerCard.alpha = 1f - clamped

			collapsedToolbar.alpha = clamped

			headerCard.isClickable = clamped < 0.9f
			collapsedToolbar.isClickable = clamped > 0.1f
		})

		headerCard.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
			val height = headerCard.height
			if (height > 0 && lastHeaderHeight != height) {
				lastHeaderHeight = height
				updateBottomSheetPeekHeightFromContent()
			}
		}
		bottomTabsContainer.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
			val height = bottomTabsContainer.height
			if (height > 0 && lastTabsHeight != height) {
				lastTabsHeight = height
				updateRecyclerBottomPadding()
				updateBottomSheetPeekHeightFromContent()
			}
		}
		updateRecyclerBottomPadding()
		updateBottomSheetPeekHeightFromContent()
	}

	override fun onResume() {
		super.onResume()
		if (!::adapter.isInitialized) {
			return
		}
		ensureKnowledgeCardPrerequisites()
		submitCards()
	}

	override fun onApplyInsets(insets: WindowInsetsCompat) {
		super.onApplyInsets(insets)
		val systemInsets =
			insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
		applyResolvedSystemInsets(systemInsets.top, systemInsets.bottom)
	}

	private fun applyLegacyInsetsFallbackIfNeeded() {
		if (InsetsUtils.isEdgeToEdgeSupported()) {
			return
		}
		val systemInsets = InsetsUtils.getSysBars(requireContext(), lastRootInsets) ?: return
		applyResolvedSystemInsets(systemInsets.top, systemInsets.bottom)
	}

	private fun applyResolvedSystemInsets(topInset: Int, bottomInset: Int) {
		systemTopInset = topInset
		updateTopInsetReveal()
		bottomTabsContainer.updatePadding(bottom = tabsContainerBaseBottomPadding + bottomInset)
		updateRecyclerBottomPadding()
		updateBottomSheetPeekHeightFromContent()
	}

	private fun initializeViews(view: View) {
		appBarLayout = view.findViewById(R.id.appBarLayout)
		recyclerView = view.findViewById(R.id.cardsRecyclerView)
		headerCard = view.findViewById(R.id.headerCard)
		collapsedToolbar = view.findViewById(R.id.collapsedToolbar)
		metricsList = view.findViewById(R.id.metrics_list)
		bottomTabsContainer = view.findViewById(R.id.bottom_buttons_container)

		headerTitle = view.findViewById(R.id.header_title)
		headerType = view.findViewById(R.id.header_subtitle)
		headerCloseButton = view.findViewById(R.id.header_close_button)
		collapsedTitle = view.findViewById(R.id.collapsed_title)
		collapsedCloseButton = view.findViewById(R.id.collapsed_close_button)

		bottomTabs = view.findViewById(R.id.bottomTabs)

		saveButton = view.findViewById(R.id.save_button)
		saveTitle = view.findViewById(R.id.save_title)
		saveIcon = view.findViewById(R.id.save_icon)

		locationButton = view.findViewById(R.id.locate_button)
		locationTitle = view.findViewById(R.id.locate_title)
		locationIcon = view.findViewById(R.id.locate_icon)

		directionButton = view.findViewById(R.id.direction_button)
		directionTitle = view.findViewById(R.id.direction_title)
		directionIcon = view.findViewById(R.id.direction_icon)

		pathButton = view.findViewById(R.id.path_button)
		pathTitle = view.findViewById(R.id.path_title)
		pathIcon = view.findViewById(R.id.path_icon)

		headerCloseButton.setOnClickListener {
			parent.hideBottomSheet()
		}
		collapsedCloseButton.setOnClickListener {
			parent.hideBottomSheet()
		}

		appBarBaseTopPadding = appBarLayout.paddingTop
		tabsContainerBaseBottomPadding = bottomTabsContainer.paddingBottom
		recyclerBaseBottomPadding = recyclerView.paddingBottom
		updateTopInsetReveal()
		setupBottomTabs()
	}

	private fun updateVisibilityCard(obj: SkyObject) {
		val currentTime = getCurrentGraphTime()
		val graphZoneId = currentTime.zone
		val graphDate = uiState.selectedVisibilityDateOverride ?: currentTime.toLocalDate()
		val isTodayVisibility = graphDate == currentTime.toLocalDate()
		val cursorReferenceTimeMillis =
			uiState.visibilityCursorReferenceTimeMillis ?: currentTime.toInstant().toEpochMilli()
		visibilityController.update(
			skyObject = obj,
			observer = parent.starView.observer,
			date = graphDate,
			zoneId = graphZoneId,
			cursorReferenceTimeMillis = cursorReferenceTimeMillis,
			isTodayVisibility = isTodayVisibility
		)
	}

	private fun onVisibilityCursorChanged(referenceTimeMillis: Long) {
		uiState = uiState.copy(visibilityCursorReferenceTimeMillis = referenceTimeMillis)
	}

	private fun updateScheduleCard(obj: SkyObject, periodStartOverride: LocalDate? = null) {
		val currentTime = getCurrentGraphTime()
		val graphZoneId = currentTime.zone
		val defaultStartDate = currentTime.toLocalDate()
		val periodStart =
			periodStartOverride ?: uiState.schedulePeriodStart ?: scheduleController.periodStart
			?: defaultStartDate
		uiState = uiState.copy(schedulePeriodStart = periodStart, currentLocalDate = defaultStartDate)
		scheduleController.update(
			skyObject = obj,
			observer = parent.starView.observer,
			periodStart = periodStart,
			zoneId = graphZoneId,
			showResetPeriodButton = periodStart != defaultStartDate
		)
	}

	private fun shiftSchedulePeriod(daysDelta: Int) {
		val obj = skyObject ?: return
		val currentStart = uiState.schedulePeriodStart ?: scheduleController.periodStart
		updateScheduleCard(obj, currentStart.plusDays(daysDelta.toLong()))
	}

	private fun resetScheduleToCurrentPeriod() {
		val obj = skyObject ?: return
		val today = getCurrentGraphTime().toLocalDate()
		updateScheduleCard(obj, today)
	}

	private fun selectVisibilityDate(date: LocalDate) {
		val currentDate = getCurrentGraphTime().toLocalDate()
		uiState = uiState.copy(
			selectedVisibilityDateOverride = if (date == currentDate) null else date
		)
		skyObject?.let(::updateVisibilityCard)
		submitCards()
	}

	private fun resetVisibilityToToday() {
		if (uiState.selectedVisibilityDateOverride == null) {
			return
		}
		uiState = uiState.copy(selectedVisibilityDateOverride = null)
		skyObject?.let(::updateVisibilityCard)
		submitCards()
	}

	private fun getCurrentGraphTime(): ZonedDateTime {
		val currentCalendar =
			(parent.viewModel.currentCalendar.value ?: Calendar.getInstance()).clone() as Calendar
		val zoneId = currentCalendar.timeZone.toZoneId()
		return Instant.ofEpochMilli(currentCalendar.timeInMillis).atZone(zoneId)
	}

	private fun setupRecyclerView() {
		adapter = AstroContextMenuAdapter(
			app,
			requireMapActivity(),
			nightMode,
			galleryController,
			onDescriptionRead = { item ->
				openDescriptionCard(item)
			},
			onGalleryToggle = { wid ->
				onGalleryToggle(wid)
			},
			onUpdateImage = {
				skyObject?.wid?.let(::loadGallery)
			},
			onKnowledgeCardAction = {
				onKnowledgeCardAction()
			},
			onVisibilityResetToToday = {
				resetVisibilityToToday()
			},
			onVisibilityCursorChanged = { referenceTimeMillis ->
				onVisibilityCursorChanged(referenceTimeMillis)
			},
			onScheduleResetPeriod = {
				resetScheduleToCurrentPeriod()
			},
			onScheduleShiftPeriod = { daysDelta ->
				shiftSchedulePeriod(daysDelta)
			},
			onScheduleSelectDate = { date ->
				selectVisibilityDate(date)
			},
			onCatalogsToggleExpanded = {
				toggleCatalogsExpanded()
			},
			onCatalogClick = { catalog ->
				openCatalogSearch(catalog)
			})

		val layoutManager = LinearLayoutManager(requireContext())
		recyclerView.layoutManager = layoutManager
		recyclerView.adapter = adapter
		(recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

		recyclerView.isNestedScrollingEnabled = true
		recyclerScrollListener?.let { recyclerView.removeOnScrollListener(it) }
		recyclerScrollListener = object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				syncBottomTabSelection()
			}

			override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
				if (!isProgrammaticSectionScroll) {
					return
				}
				if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
					cancelProgrammaticSectionScroll(syncBottomTabSelection = true)
				} else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
					finishProgrammaticSectionScroll(programmaticSectionScrollToken)
				}
			}
		}
		recyclerView.addOnScrollListener(recyclerScrollListener!!)

		metricsList.layoutManager =
			LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
		metricsAdapter = MetricsAdapter()
		metricsList.adapter = metricsAdapter
		metricsList.isNestedScrollingEnabled = false
	}

	private fun setupBottomTabs() {
		val activeColor = ColorUtilities.getActiveIconColor(app, nightMode)
		val inactiveIconColor = ColorUtilities.getDefaultIconColor(app, nightMode)
		val inactiveTextColor = ColorUtilities.getSecondaryTextColor(app, nightMode)
		val iconTint = ColorStateList(
			arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
			intArrayOf(activeColor, inactiveIconColor)
		)

		bottomTabs.removeAllTabs()
		bottomTabs.tabIconTint = iconTint
		bottomTabs.setTabTextColors(inactiveTextColor, activeColor)

		bottomTabs.addTab(
			bottomTabs.newTab()
				.setText(R.string.shared_string_overview)
				.setIcon(getOverviewTabIconRes(skyObject?.type)),
			true
		)
		bottomTabs.addTab(
			bottomTabs.newTab()
				.setText(R.string.gpx_visibility_txt)
				.setIcon(R.drawable.ic_action_telescope_colored)
		)
		bottomTabs.addTab(
			bottomTabs.newTab()
				.setText(R.string.astronomy_schedule)
				.setIcon(R.drawable.ic_action_date_start)
		)

		tabSelectedListener?.let { bottomTabs.removeOnTabSelectedListener(it) }
		tabSelectedListener = object : TabLayout.OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab) {
				if (isSyncingTabSelection) {
					return
				}
				selectedBottomTab = tab.position
				scrollToSelectedTab(tab.position)
			}

			override fun onTabUnselected(tab: TabLayout.Tab) = Unit

			override fun onTabReselected(tab: TabLayout.Tab) {
				if (isSyncingTabSelection) {
					return
				}
				scrollToSelectedTab(tab.position)
			}
		}
		bottomTabs.addOnTabSelectedListener(tabSelectedListener!!)
		bottomTabs.getTabAt(selectedBottomTab.coerceIn(0, bottomTabs.tabCount - 1))?.select()
	}

	private fun updateBottomTabIcons(type: SkyObject.Type?) {
		if (!::bottomTabs.isInitialized || bottomTabs.tabCount < 2) {
			return
		}
		bottomTabs.getTabAt(TAB_OVERVIEW)?.setIcon(getOverviewTabIconRes(type))
	}

	private fun getOverviewTabIconRes(type: SkyObject.Type?): Int {
		return when (type) {
			null -> R.drawable.ic_action_planet_outlined
			SkyObject.Type.SUN,
			SkyObject.Type.MOON,
			SkyObject.Type.PLANET -> R.drawable.ic_action_planet_outlined
			SkyObject.Type.CONSTELLATION -> R.drawable.ic_action_constellations
			SkyObject.Type.STAR -> R.drawable.ic_action_stars
			SkyObject.Type.NEBULA -> R.drawable.ic_action_nebulas
			SkyObject.Type.OPEN_CLUSTER,
			SkyObject.Type.GLOBULAR_CLUSTER -> R.drawable.ic_action_star_clusters
			SkyObject.Type.GALAXY,
			SkyObject.Type.GALAXY_CLUSTER,
			SkyObject.Type.BLACK_HOLE -> R.drawable.ic_action_galaxy
		}
	}

	private fun resetOverviewStateForNewObject() {
		selectedBottomTab = TAB_OVERVIEW
		if (!::recyclerView.isInitialized || !::appBarLayout.isInitialized) {
			return
		}
		cancelProgrammaticSectionScroll(syncBottomTabSelection = false)
		recyclerView.stopScroll()
		selectBottomTabWithoutScroll(TAB_OVERVIEW)
		scrollToAdapterPositionExactly(0)
		appBarLayout.setExpanded(true, false)
		headerCard.alpha = 1f
		headerCard.isClickable = true
		collapsedToolbar.alpha = 0f
		collapsedToolbar.isClickable = false
		bottomSheetContainer?.let { updateBottomSheetVisuals(it.top) }
	}

	private fun scrollToSelectedTab(tabPosition: Int) {
		val targetPosition = resolveAdapterPositionForTab(tabPosition) ?: return
		cancelProgrammaticSectionScroll(syncBottomTabSelection = false)
		recyclerView.stopScroll()
		val scrollToken = beginProgrammaticSectionScroll(tabPosition)
		when (tabPosition) {
			TAB_OVERVIEW -> {
				scrollToAdapterPositionExactly(targetPosition)
				recyclerView.post {
					if (!isCurrentProgrammaticSectionScroll(scrollToken)) {
						return@post
					}
					appBarLayout.setExpanded(true, true)
					finishProgrammaticSectionScroll(scrollToken)
				}
			}

			TAB_VISIBILITY,
			TAB_SCHEDULE -> {
				collapseHeaderCard()
				recyclerView.post {
					if (!isCurrentProgrammaticSectionScroll(scrollToken)) {
						return@post
					}
					smoothScrollToAdapterPosition(targetPosition, scrollToken)
				}
			}
		}
	}

	private fun collapseHeaderCard() {
		appBarLayout.setExpanded(false, false)
	}

	private fun resolveAdapterPositionForTab(tabPosition: Int): Int? {
		if (!::adapter.isInitialized || adapter.itemCount == 0) {
			return null
		}
		return when (tabPosition) {
			TAB_OVERVIEW -> 0
			TAB_VISIBILITY -> adapter.getItemPosition(AstroContextCardKey.VISIBILITY).takeIf { it >= 0 }
			TAB_SCHEDULE -> {
				adapter.getItemPosition(AstroContextCardKey.SCHEDULE).let { cardPosition ->
					if (cardPosition >= 0) {
						cardPosition
					} else {
						(adapter.itemCount - 1).coerceAtLeast(0)
					}
				}
			}

			else -> null
		}
	}

	private fun beginProgrammaticSectionScroll(tabPosition: Int): Int {
		programmaticSectionScrollToken += 1
		isProgrammaticSectionScroll = true
		pendingProgrammaticSectionTab = tabPosition
		selectBottomTabWithoutScroll(tabPosition)
		return programmaticSectionScrollToken
	}

	private fun cancelProgrammaticSectionScroll(syncBottomTabSelection: Boolean) {
		if (!isProgrammaticSectionScroll) {
			return
		}
		programmaticSectionScrollToken += 1
		isProgrammaticSectionScroll = false
		pendingProgrammaticSectionTab = null
		if (syncBottomTabSelection) {
			syncBottomTabSelection()
		}
	}

	private fun isCurrentProgrammaticSectionScroll(scrollToken: Int): Boolean {
		return isProgrammaticSectionScroll && programmaticSectionScrollToken == scrollToken
	}

	private fun smoothScrollToAdapterPosition(preferredPosition: Int, scrollToken: Int) {
		if (!isCurrentProgrammaticSectionScroll(scrollToken) || !::adapter.isInitialized || adapter.itemCount == 0) {
			return
		}
		val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
		val position = preferredPosition.coerceIn(0, adapter.itemCount - 1)
		val targetView = layoutManager.findViewByPosition(position)
		if (targetView != null) {
			val scrollDelta = layoutManager.getDecoratedTop(targetView) - recyclerView.paddingTop
			if (scrollDelta == 0) {
				recyclerView.post {
					finishProgrammaticSectionScroll(scrollToken)
				}
			} else {
				recyclerView.smoothScrollBy(0, scrollDelta)
			}
			return
		}
		val smoothScroller = object : LinearSmoothScroller(recyclerView.context) {
			override fun getVerticalSnapPreference(): Int = SNAP_TO_START
		}
		smoothScroller.targetPosition = position
		layoutManager.startSmoothScroll(smoothScroller)
	}

	private fun scrollToAdapterPositionExactly(preferredPosition: Int) {
		if (!::adapter.isInitialized || adapter.itemCount == 0) {
			return
		}
		val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
		val position = preferredPosition.coerceIn(0, adapter.itemCount - 1)
		layoutManager.scrollToPositionWithOffset(position, 0)
	}

	private fun finishProgrammaticSectionScroll(scrollToken: Int) {
		if (!isCurrentProgrammaticSectionScroll(scrollToken)) {
			return
		}
		recyclerView.post {
			if (!isCurrentProgrammaticSectionScroll(scrollToken)) {
				return@post
			}
			isProgrammaticSectionScroll = false
			pendingProgrammaticSectionTab = null
			syncBottomTabSelection()
		}
	}

	private fun syncBottomTabSelection() {
		val pendingTabPosition = pendingProgrammaticSectionTab
		if (isProgrammaticSectionScroll && pendingTabPosition != null) {
			selectBottomTabWithoutScroll(pendingTabPosition)
		} else {
			updateBottomTabSelectionByScroll()
		}
	}

	private fun updateBottomTabSelectionByScroll() {
		if (!::adapter.isInitialized || !::bottomTabs.isInitialized || bottomTabs.tabCount == 0) {
			return
		}
		val visibilityPosition = adapter.getItemPosition(AstroContextCardKey.VISIBILITY)
		if (visibilityPosition < 0) {
			return
		}
		val schedulePosition = adapter.getItemPosition(AstroContextCardKey.SCHEDULE)
		val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
		val anchorY = getSectionSelectionAnchorY()
		val visibilityReached = hasCardReachedAnchor(layoutManager, visibilityPosition, anchorY)
		val scheduleReached = when {
			schedulePosition < 0 -> false
			!recyclerView.canScrollVertically(1) && isCardVisible(layoutManager, schedulePosition) -> true
			else -> hasCardReachedAnchor(layoutManager, schedulePosition, anchorY)
		}

		val targetTab = when {
			scheduleReached -> TAB_SCHEDULE
			visibilityReached -> TAB_VISIBILITY
			else -> TAB_OVERVIEW
		}
		selectBottomTabWithoutScroll(targetTab)
	}

	private fun getSectionSelectionAnchorY(): Int {
		val visibleHeight = (recyclerView.height - recyclerView.paddingTop - recyclerView.paddingBottom)
			.coerceAtLeast(0)
		return recyclerView.paddingTop + visibleHeight / 2
	}

	private fun hasCardReachedAnchor(
		layoutManager: LinearLayoutManager,
		position: Int,
		anchorY: Int
	): Boolean {
		if (position < 0) {
			return false
		}
		val targetView = layoutManager.findViewByPosition(position)
		return if (targetView != null) {
			layoutManager.getDecoratedTop(targetView) <= anchorY
		} else {
			layoutManager.findFirstVisibleItemPosition() >= position
		}
	}

	private fun isCardVisible(layoutManager: LinearLayoutManager, position: Int): Boolean {
		if (position < 0) {
			return false
		}
		val targetView = layoutManager.findViewByPosition(position)
		if (targetView != null) {
			return layoutManager.getDecoratedBottom(targetView) > recyclerView.paddingTop &&
				layoutManager.getDecoratedTop(targetView) < recyclerView.height - recyclerView.paddingBottom
		}
		val firstVisible = layoutManager.findFirstVisibleItemPosition()
		val lastVisible = layoutManager.findLastVisibleItemPosition()
		return firstVisible != RecyclerView.NO_POSITION &&
			lastVisible != RecyclerView.NO_POSITION &&
			position in firstVisible..lastVisible
	}

	private fun selectBottomTabWithoutScroll(tabPosition: Int) {
		if (!::bottomTabs.isInitialized || bottomTabs.tabCount <= tabPosition || tabPosition < 0) {
			return
		}
		if (selectedBottomTab == tabPosition && bottomTabs.selectedTabPosition == tabPosition) {
			return
		}
		selectedBottomTab = tabPosition
		if (bottomTabs.selectedTabPosition == tabPosition) {
			return
		}
		isSyncingTabSelection = true
		try {
			bottomTabs.getTabAt(tabPosition)?.select()
		} finally {
			isSyncingTabSelection = false
		}
	}

	private fun updateRecyclerBottomPadding() {
		val tabsHeight = if (::bottomTabsContainer.isInitialized) bottomTabsContainer.height else 0
		val visibleRect = Rect()
		val hiddenHeight = if (recyclerView.height > 0 && recyclerView.getGlobalVisibleRect(visibleRect)) {
			(recyclerView.height - visibleRect.height()).coerceAtLeast(0)
		} else {
			0
		}
		val bottomPadding = recyclerBaseBottomPadding + tabsHeight + hiddenHeight
		if (recyclerView.paddingBottom != bottomPadding) {
			recyclerView.updatePadding(bottom = bottomPadding)
		}
	}

	private fun updateTopInsetReveal() {
		val topInsetOffset = if (systemTopInset > 0) {
			(systemTopInset - currentSheetTop).coerceIn(0, systemTopInset)
		} else {
			0
		}
		appBarLayout.updatePadding(top = appBarBaseTopPadding + topInsetOffset)
	}

	private fun getPreferredCollapsedVisibleHeightPx(): Int {
		val extraPx = (COLLAPSED_EXTRA_DP * resources.displayMetrics.density).roundToInt()
		val headerHeight = if (::headerCard.isInitialized) headerCard.height else 0
		val tabsHeight = if (::bottomTabsContainer.isInitialized) bottomTabsContainer.height else 0
		return headerHeight + tabsHeight + extraPx
	}

	private fun setupBottomSheet() {
		if (bottomSheetContainer != null && bottomSheetBehavior != null) {
			return
		}
		val container = view?.parent as? View ?: return
		val behavior =
			(BottomSheetBehavior.from(container) as? AstroBottomSheetBehavior<View>) ?: return

		bottomSheetContainer = container
		bottomSheetBehavior = behavior
		if (bottomSheetCornerRadiusPx == 0f) {
			bottomSheetCornerRadiusPx = SHEET_CORNER_RADIUS_DP * resources.displayMetrics.density
		}

		ensureBottomSheetBackground()
		updateBottomSheetCorner(bottomSheetCornerRadiusPx)
		container.background = bottomSheetBackground
		container.clipToOutline = true
		container.updateLayoutParams<ViewGroup.LayoutParams> {
			height = ViewGroup.LayoutParams.MATCH_PARENT
		}
		behavior.apply {
			setLockedNestedScrollTargetId(R.id.cardsRecyclerView)
			isHideable = true
			skipCollapsed = false
			isFitToContents = true
			expandedOffset = 0
			isDraggable = true
		}

		bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
			override fun onStateChanged(bottomSheet: View, newState: Int) {
				if (newState == BottomSheetBehavior.STATE_HIDDEN) {
					resetBottomSheetVisualState()
					return
				}
				updateBottomSheetVisuals(bottomSheet.top)
			}

			override fun onSlide(bottomSheet: View, slideOffset: Float) {
				updateBottomSheetVisuals(bottomSheet.top)
			}
		}
		behavior.addBottomSheetCallback(bottomSheetCallback!!)

		updateBottomSheetVisuals(container.top)
	}

	private fun updateBottomSheetPeekHeightFromContent(): Boolean {
		if (bottomSheetContainer == null || bottomSheetBehavior == null) {
			setupBottomSheet()
		}
		val behavior = bottomSheetBehavior ?: return false
		val container = bottomSheetContainer ?: return false
		val parentHeight = (container.parent as? View)?.height ?: return false
		if (parentHeight <= 0) {
			return false
		}
		val preferredPeekHeight = getPreferredCollapsedVisibleHeightPx().coerceIn(1, parentHeight)
		if (preferredPeekHeight <= 1) {
			return false
		}
		val changed = behavior.peekHeight != preferredPeekHeight
		if (changed) {
			behavior.peekHeight = preferredPeekHeight
			if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
				behavior.state = BottomSheetBehavior.STATE_COLLAPSED
			}
		}
		updateBottomSheetVisuals(container.top)
		return true
	}

	private fun resolveBottomSheetExpansionProgress(sheetTop: Int): Float {
		val container = bottomSheetContainer ?: return 0f
		val behavior = bottomSheetBehavior ?: return 0f
		val parentHeight = (container.parent as? View)?.height ?: return 0f
		val peekHeight = behavior.peekHeight
		if (peekHeight <= 0) {
			return if (sheetTop <= behavior.expandedOffset) 1f else 0f
		}
		val collapsedTop = (parentHeight - peekHeight).coerceAtLeast(0)
		val expandedTop = behavior.expandedOffset.coerceAtLeast(0)
		if (collapsedTop <= expandedTop) {
			return if (sheetTop <= expandedTop) 1f else 0f
		}
		return ((collapsedTop - sheetTop).toFloat() / (collapsedTop - expandedTop)).coerceIn(0f, 1f)
	}

	private fun updateBottomSheetVisuals(sheetTop: Int) {
		val container = bottomSheetContainer ?: return
		ensureBottomSheetBackground()
		if (container.background !== bottomSheetBackground) {
			container.background = bottomSheetBackground
		}
		container.clipToOutline = true
		val revealProgress = if (systemTopInset > 0) {
			(systemTopInset - sheetTop).coerceIn(0, systemTopInset).toFloat() / systemTopInset
		} else {
			val progress = resolveBottomSheetExpansionProgress(sheetTop)
			if (progress >= 1f) 1f else 0f
		}
		updateBottomSheetCorner(bottomSheetCornerRadiusPx * (1f - revealProgress))
		currentSheetTop = sheetTop
		updateTopInsetReveal()
		updateRecyclerBottomPadding()
	}

	private fun resetBottomSheetVisualState() {
		currentSheetTop = Int.MAX_VALUE
		updateTopInsetReveal()
		updateBottomSheetCorner(bottomSheetCornerRadiusPx)
		bottomSheetContainer?.background = null
		bottomSheetContainer?.clipToOutline = false
	}

	private fun ensureBottomSheetBackground() {
		if (bottomSheetBackground == null) {
			bottomSheetBackground = MaterialShapeDrawable(
				buildBottomSheetShape(
					bottomSheetCornerRadiusPx
				)
			).apply {
				fillColor = ColorStateList.valueOf(
					ColorUtilities.getActivityBgColor(
						requireContext(),
						nightMode
					)
				)
			}
		} else {
			bottomSheetBackground?.fillColor = ColorStateList.valueOf(
				ColorUtilities.getActivityBgColor(requireContext(), nightMode)
			)
		}
	}

	private fun updateBottomSheetCorner(radius: Float) {
		bottomSheetBackground?.shapeAppearanceModel = buildBottomSheetShape(radius)
	}

	private fun buildBottomSheetShape(radius: Float): ShapeAppearanceModel {
		return ShapeAppearanceModel.builder()
			.setTopLeftCorner(CornerFamily.ROUNDED, radius)
			.setTopRightCorner(CornerFamily.ROUNDED, radius)
			.setBottomLeftCorner(CornerFamily.ROUNDED, 0f)
			.setBottomRightCorner(CornerFamily.ROUNDED, 0f)
			.build()
	}

	override fun onDestroyView() {
		galleryLoader?.cancel()
		visibilityController.cancelPendingWork()
		scheduleController.cancelPendingWork()
		unbindControllerCallbacks()
		tabSelectedListener?.let { listener ->
			if (::bottomTabs.isInitialized) {
				bottomTabs.removeOnTabSelectedListener(listener)
			}
		}
		recyclerScrollListener?.let { listener ->
			if (::recyclerView.isInitialized) {
				recyclerView.removeOnScrollListener(listener)
			}
		}
		tabSelectedListener = null
		recyclerScrollListener = null
		isSyncingTabSelection = false
		isProgrammaticSectionScroll = false
		pendingProgrammaticSectionTab = null
		programmaticSectionScrollToken += 1
		bottomSheetCallback?.let { callback ->
			bottomSheetBehavior?.removeBottomSheetCallback(callback)
		}
		bottomSheetCallback = null
		bottomSheetContainer = null
		bottomSheetBehavior = null
		bottomSheetBackground = null
		super.onDestroyView()
	}

	private fun bindControllerCallbacks() {
		visibilityController.onDataChanged = { submitCards() }
		scheduleController.onDataChanged = { submitCards() }
	}

	private fun unbindControllerCallbacks() {
		visibilityController.onDataChanged = null
		scheduleController.onDataChanged = null
	}

	private fun onGalleryToggle(wid: String) {
		when (uiState.galleryState) {
			AstroGalleryCardState.Collapsed -> {
				loadGallery(wid)
			}

			is AstroGalleryCardState.Ready -> {
				uiState = uiState.copy(galleryState = AstroGalleryCardState.Collapsed)
				submitCards()
			}

			AstroGalleryCardState.Loading -> Unit
		}
	}

	private fun loadGallery(wid: String) {
		uiState = uiState.copy(galleryState = AstroGalleryCardState.Loading)
		submitCards()
		galleryLoader?.startLoading(wid) ?: run {
			onGalleryStateChanged(wid, AstroGalleryCardState.Ready(emptyList()))
		}
	}

	private fun onGalleryStateChanged(wid: String, state: AstroGalleryCardState) {
		if (skyObject?.wid != wid || uiState.galleryState == state) {
			return
		}
		uiState = uiState.copy(galleryState = state)
		submitCards()
	}

	private fun submitCards() {
		if (!::adapter.isInitialized) {
			return
		}
		val items = cardFactory.buildCards(
			skyObject = skyObject,
			article = article,
			uiState = uiState,
			knowledgeItem = buildKnowledgeCardItem(),
			visibilityItem = visibilityController.buildItem(),
			scheduleItem = scheduleController.buildItem()
		)
		if (adapter.currentList == items) {
			syncBottomTabSelection()
			return
		}
		adapter.submitItems(items) {
			syncBottomTabSelection()
		}
	}

	private fun toggleCatalogsExpanded() {
		uiState = uiState.copy(catalogsExpanded = !uiState.catalogsExpanded)
		submitCards()
	}

	private fun openUri(uri: Uri) {
		val intent = Intent(Intent.ACTION_VIEW, uri)
		try {
			startActivity(intent)
		} catch (_: Exception) {
		}
	}

	private fun openDescriptionCard(item: AstroDescriptionCardItem) {
		if (item.hasOfflineArticle && showOfflineArticle()) {
			return
		}
		item.readMoreUri?.let(::openUri)
	}

	private fun showOfflineArticle(): Boolean {
		val currentArticle = article ?: return false
		if (!currentArticle.hasOfflineContent()) {
			return false
		}
		return AstroArticleDialogFragment.showInstance(
			parentFragmentManager,
			currentArticle.wikidata,
			currentArticle.lang
		)
	}

	private fun currentKnowledgeCardState(): AstroKnowledgeCardState? {
		return knowledgeBaseController.currentState()
	}

	private fun buildKnowledgeCardItem(): AstroKnowledgeCardItem? {
		return knowledgeBaseController.buildCardItem()
	}

	private fun ensureKnowledgeCardPrerequisites() {
		if (currentKnowledgeCardState() == AstroKnowledgeCardState.DOWNLOAD) {
			knowledgeBaseController.ensureIndexesLoaded()
		}
	}

	private fun wasKnowledgeActionDisabled(): Boolean {
		if (!::adapter.isInitialized) {
			return false
		}
		val position = adapter.getItemPosition(AstroContextCardKey.KNOWLEDGE)
		val item = if (position >= 0) adapter.currentList[position] as? AstroKnowledgeCardItem else null
		return item?.actionEnabled == false
	}

	private fun onKnowledgeCardAction() {
		if (!isAdded) {
			return
		}
		when (currentKnowledgeCardState()) {
			AstroKnowledgeCardState.UPSELL -> {
				ChoosePlanFragment.showInstance(requireMapActivity(), OsmAndFeature.ASTRONOMY)
			}

			AstroKnowledgeCardState.DOWNLOAD -> {
				val indexItem = knowledgeBaseController.findDownloadItem()
				if (indexItem == null) {
					knowledgeBaseController.ensureIndexesLoaded()
					app.showToastMessage(
						if (!app.downloadThread.indexes.isDownloadedFromInternet) {
							R.string.getting_list_of_required_maps
						} else {
							R.string.no_index_file_to_download
						}
					)
					return
				}
				DownloadValidationManager(app).startDownload(requireMapActivity(), indexItem)
			}

			null -> Unit
		}
	}

	override fun onUpdatedIndexesList() {
		if (!isAdded) {
			return
		}
		knowledgeBaseController.resetIndexesReloadFlag()
		submitCards()
	}

	override fun downloadHasFinished() {
		if (!isAdded) {
			return
		}
		val shouldUpdateKnowledgeCard =
			knowledgeBaseController.shouldRefreshAfterDownload(wasKnowledgeActionDisabled())
		if (!shouldUpdateKnowledgeCard) {
			return
		}
		knowledgeBaseController.resetIndexesReloadFlag()
		skyObject?.let(::updateObjectInfo) ?: run {
			submitCards()
		}
	}

	override fun downloadInProgress() {
		if (!isAdded || currentKnowledgeCardState() != AstroKnowledgeCardState.DOWNLOAD) {
			return
		}
		if (knowledgeBaseController.findActiveDownload() == null) {
			return
		}
		submitCards()
	}

	private fun openCatalogSearch(catalog: Catalog) {
		parent.showSearchDialog(catalog.wid)
	}
}
