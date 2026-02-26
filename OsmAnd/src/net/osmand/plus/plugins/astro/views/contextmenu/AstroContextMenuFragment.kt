package net.osmand.plus.plugins.astro.views.contextmenu

import android.content.res.ColorStateList
import android.os.AsyncTask
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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.tabs.TabLayout
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.defineStar
import io.github.cosinekitty.astronomy.searchRiseSet
import net.osmand.data.LatLon
import net.osmand.plus.OsmAndTaskManager
import net.osmand.plus.R
import net.osmand.plus.base.BaseMaterialFragment
import net.osmand.plus.mapcontextmenu.builders.cards.AbstractCard
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard
import net.osmand.plus.mapcontextmenu.builders.cards.NoImagesCard
import net.osmand.plus.mapcontextmenu.gallery.GalleryController
import net.osmand.plus.mapcontextmenu.gallery.ImageCardType
import net.osmand.plus.mapcontextmenu.gallery.ImageCardsHolder
import net.osmand.plus.mapcontextmenu.gallery.PhotoCacheManager
import net.osmand.plus.mapcontextmenu.gallery.tasks.CacheReadTask
import net.osmand.plus.mapcontextmenu.gallery.tasks.CacheWriteTask
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.astro.AstroArticle
import net.osmand.plus.plugins.astro.SkyObject
import net.osmand.plus.plugins.astro.StarMapFragment
import net.osmand.plus.plugins.astro.StarWatcherPlugin
import net.osmand.plus.plugins.astro.utils.AstroUtils
import net.osmand.plus.plugins.astro.views.AstroScheduleCardModel
import net.osmand.plus.plugins.astro.views.AstroVisibilityCardModel
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.wikipedia.WikiImageCard
import net.osmand.shared.wiki.WikiCoreHelper
import net.osmand.shared.wiki.WikiImage
import net.osmand.util.Algorithms
import java.time.Instant
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class AstroContextMenuFragment : BaseMaterialFragment() {

	private var galleryController: GalleryController? = null

	private var skyObject: SkyObject? = null
	private var article: AstroArticle? = null
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

	private var getAstroImagesTask: GetAstroImagesTask? = null

	private var visibilityCardModel: AstroVisibilityCardModel? = null
	private var scheduleCardModel: AstroScheduleCardModel? = null
	private var descriptionCardModel: AstroDescriptionCardModel? = null
	private var catalogCardModel: AstroCatalogsCardModel? = null
	private var galleryCardModel: AstroGalleryCardModel? = null

	private val imageCardListener: GetAstroImagesTask.GetImageCardsListener =
		object : GetAstroImagesTask.GetImageCardsListener {
			override fun onTaskStarted() {

			}

			override fun onFinish(images: List<WikiImage>?) {
				if (skyObject == null || images == null) {
					return
				}

				val latLon = LatLon(0.0, 0.0)
				val params = HashMap<String, String>()
				params["wikidataId"] = skyObject!!.wid
				galleryController?.currentCardsHolder = ImageCardsHolder(latLon, params).apply {
					for (wikiImage in images) {
						mapActivity?.let {
							this.addCard(
								ImageCardType.ASTRONOMY,
								WikiImageCard(it, wikiImage)
							)
						}
					}
					setOnlinePhotosCards(this.astronomyCards)
				}
			}
		}


	companion object {
		val TAG: String = AstroContextMenuFragment::class.java.simpleName
		private const val TAB_OVERVIEW = 0
		private const val TAB_VISIBILITY = 1
		private const val TAB_SCHEDULE = 2
		private const val SHEET_CORNER_RADIUS_DP = 12f
		private const val COLLAPSED_EXTRA_DP = 100f

		fun newInstance(skyObject: SkyObject): AstroContextMenuFragment {
			val fragment = AstroContextMenuFragment()
			val args = Bundle()
			args.putString("skyObjectName", skyObject.name)
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
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val view = inflater.inflate(R.layout.astro_context_menu_fragment, container, false)

		initializeViews(view)
		setupRecyclerView()
		loadCards()

		arguments?.getString("skyObjectName")?.let { name ->
			parent.viewModel.skyObjects.value?.find { it.name == name }?.let {
				updateObjectInfo(it)
			}
		}

		return view
	}

	fun updateObjectInfo(obj: SkyObject) {
		this.skyObject = obj
		if (!isAdded) {
			return
		}

		adapter.skyObject = obj
		article =
			PluginsHelper.requirePlugin(StarWatcherPlugin::class.java).astroDataProvider.getAstroArticle(
				app,
				obj.wid,
				app.localeHelper.language
			)

		setTitle(obj.localizedName ?: obj.name)
		val typeName = AstroUtils.getAstroTypeName(app, obj.type.titleKey)
		var parentGroup: String = app.getString(R.string.astro_deep_sky)
		val type = obj.type
		if (type == SkyObject.Type.MOON) {
			parentGroup = app.getString(R.string.astro_type_earth)
		} else if (type.isSunSystem()) {
			parentGroup = app.getString(R.string.astro_solar_system)
		} else if (type == SkyObject.Type.STAR) {
			parentGroup = parent.viewModel.constellations.value
				?.firstOrNull { constellation ->
					constellation.lines.any { (a, b) -> a == obj.hip || b == obj.hip }
				}
				?.let { it.localizedName ?: it.name }.toString()
		}
		if (parentGroup == null || parentGroup.isEmpty() || parentGroup == "null") {
			parentGroup = app.getString(R.string.astro_deep_sky)
		}

		headerType.text =
			app.getString(R.string.ltr_or_rtl_combine_via_bold_point, typeName, parentGroup)

		updateMetrics(obj)
		updateButtons(obj)
		updateVisibilityCard(obj)
		updateScheduleCard(obj)

		descriptionCardModel = descriptionCardModel?.apply {
			updateCard(obj, article)
		} ?: AstroDescriptionCardModel(app, obj, article)

		obj.catalogs.takeIf { it.isNotEmpty() }?.let { catalogs ->
			catalogCardModel = catalogCardModel?.apply { this.catalogs = catalogs }
				?: AstroCatalogsCardModel(app, catalogs)
		}

		galleryCardModel = galleryCardModel?.apply { this.wid = obj.wid }
			?: AstroGalleryCardModel(app, obj.wid)

		loadCards()
	}

	private fun setOnlinePhotosCards(onlinePhotosCards: MutableList<ImageCard?>) {
		val cards: MutableList<AbstractCard?> = ArrayList(onlinePhotosCards)
		if (onlinePhotosCards.isEmpty() && mapActivity != null) {
			cards.add(NoImagesCard(mapActivity))
		}

		galleryCardModel?.state = AstroGalleryCardModel.GalleryState.Ready(cards)
		adapter.notifyItemChanged(
			adapter.getItemPosition<AstroGalleryCardModel>(),
			AstroContextMenuAdapter.Companion.PAYLOAD_GALLERY_CONTENT
		)
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
				uiUtilities.getIcon(
					if (obj.showDirection) R.drawable.ic_direction_arrow else R.drawable.ic_direction_arrow,
					ColorUtilities.getActiveIconColorId(nightMode)
				)
			)
			directionTitle.text = app.getString(R.string.astro_direction)

			pathIcon.setImageDrawable(
				uiUtilities.getIcon(
					if (obj.showCelestialPath) R.drawable.ic_action_target_path_on else R.drawable.ic_action_target_path_off,
					ColorUtilities.getActiveIconColorId(nightMode)
				)
			)
			pathTitle.text = app.getString(R.string.astro_path)
		}

		saveButton.setOnClickListener {
			obj.isFavorite = !obj.isFavorite
			val swSettings = PluginsHelper.requirePlugin(StarWatcherPlugin::class.java).swSettings
			if (obj.isFavorite) swSettings.addFavorite(obj.id) else swSettings.removeFavorite(obj.id)
			parent.starView.invalidate()

			bindButtons()
		}

		locationButton.setOnClickListener {
			parent.starView.setSelectedObject(obj, center = true, animate = true)
			bindButtons()
		}

		directionButton.setOnClickListener {
			obj.showDirection = !obj.showDirection
			val swSettings = PluginsHelper.requirePlugin(StarWatcherPlugin::class.java).swSettings
			if (obj.showDirection) swSettings.addDirection(obj.id) else swSettings.removeDirection(
				obj.id
			)
			parent.starView.invalidate()
			bindButtons()
		}

		pathButton.setOnClickListener {
			obj.showCelestialPath = !obj.showCelestialPath
			val swSettings = PluginsHelper.requirePlugin(StarWatcherPlugin::class.java).swSettings
			if (obj.showCelestialPath) swSettings.addCelestialPath(obj.id) else swSettings.removeCelestialPath(
				obj.id
			)
			parent.starView.setObjectPinned(obj, obj.showCelestialPath, true)
			parent.starView.invalidate()
			bindButtons()
		}

		bindButtons()
	}

	private fun updateMetrics(obj: SkyObject) {
		val metrics = ArrayList<MetricsAdapter.MetricUi>()

		val az = String.format(Locale.getDefault(), "%.1f°", obj.azimuth)
		metrics.add(MetricsAdapter.MetricUi(az, getString(R.string.shared_string_azimuth)))

		val alt = String.format(Locale.getDefault(), "%.1f°", obj.altitude)
		metrics.add(MetricsAdapter.MetricUi(alt, getString(R.string.altitude)))

		metrics.add(
			MetricsAdapter.MetricUi(
				obj.magnitude.toString(),
				getString(R.string.shared_string_magnitude)
			)
		)

		val observer = parent.starView.observer
		val bodyToCheck: Body? = if (!obj.type.isSunSystem()) {
			defineStar(Body.Star2, obj.ra, obj.dec, 1000.0); Body.Star2
		} else obj.body

		if (bodyToCheck != null) {
			val calendar = (parent.viewModel.currentCalendar.value
				?: Calendar.getInstance()).clone() as Calendar
			calendar.set(Calendar.HOUR_OF_DAY, 0)
			calendar.set(Calendar.MINUTE, 0)
			calendar.set(Calendar.SECOND, 0)
			calendar.set(Calendar.MILLISECOND, 0)
			val searchStart = Time.fromMillisecondsSince1970(calendar.timeInMillis)

			val riseTime = searchRiseSet(bodyToCheck, observer, Direction.Rise, searchStart, 1.2)
			val setTime = searchRiseSet(bodyToCheck, observer, Direction.Set, searchStart, 1.2)

			if (riseTime != null) {
				metrics.add(
					MetricsAdapter.MetricUi(
						AstroUtils.formatLocalTime(riseTime),
						getString(R.string.astro_rise)
					)
				)
			}

			if (setTime != null) {
				metrics.add(
					MetricsAdapter.MetricUi(
						AstroUtils.formatLocalTime(setTime),
						getString(R.string.astro_set)
					)
				)
			}
		}

		metricsAdapter.submit(metrics)
	}

	private fun setTitle(name: String) {
		collapsedTitle.text = name
		headerTitle.text = name
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		setupBottomSheet()

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

	override fun onApplyInsets(insets: WindowInsetsCompat) {
		super.onApplyInsets(insets)
		val systemInsets =
			insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
		systemTopInset = systemInsets.top

		updateTopInsetReveal()
		bottomTabsContainer.updatePadding(bottom = tabsContainerBaseBottomPadding + systemInsets.bottom)
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
		val currentCalendar =
			(parent.viewModel.currentCalendar.value ?: Calendar.getInstance()).clone() as Calendar
		val graphZoneId = currentCalendar.timeZone.toZoneId()
		val graphDate =
			Instant.ofEpochMilli(currentCalendar.timeInMillis).atZone(graphZoneId).toLocalDate()
		visibilityCardModel = visibilityCardModel?.apply {
			updateCard(
				skyObject = obj,
				observer = parent.starView.observer,
				date = graphDate,
				zoneId = graphZoneId
			)
		} ?: AstroVisibilityCardModel(app).apply {
			updateCard(
				skyObject = obj,
				observer = parent.starView.observer,
				date = graphDate,
				zoneId = graphZoneId
			)
		}
		visibilityCardModel?.onDataChanged = { notifyVisibilityCardChanged() }
	}

	private fun notifyVisibilityCardChanged() {
		if (!isAdded || !::adapter.isInitialized) {
			return
		}
		val position = adapter.getItemPosition<AstroVisibilityCardModel>()
		if (position >= 0) {
			adapter.notifyItemChanged(position)
		}
	}

	private fun updateScheduleCard(obj: SkyObject, periodStartOverride: LocalDate? = null) {
		val currentCalendar =
			(parent.viewModel.currentCalendar.value ?: Calendar.getInstance()).clone() as Calendar
		val graphZoneId = currentCalendar.timeZone.toZoneId()
		val defaultStartDate =
			Instant.ofEpochMilli(currentCalendar.timeInMillis).atZone(graphZoneId).toLocalDate()
		val periodStart = periodStartOverride ?: scheduleCardModel?.periodStart ?: defaultStartDate
		scheduleCardModel = scheduleCardModel?.apply {
			updateCard(
				skyObject = obj,
				observer = parent.starView.observer,
				periodStart = periodStart,
				zoneId = graphZoneId
			)
		} ?: AstroScheduleCardModel(app).apply {
			updateCard(
				skyObject = obj,
				observer = parent.starView.observer,
				periodStart = periodStart,
				zoneId = graphZoneId
			)
		}
	}

	private fun shiftSchedulePeriod(daysDelta: Int) {
		val obj = skyObject ?: return
		val currentStart = scheduleCardModel?.periodStart ?: return
		updateScheduleCard(obj, currentStart.plusDays(daysDelta.toLong()))
		notifyScheduleCardChanged()
	}

	private fun resetScheduleToCurrentPeriod() {
		val obj = skyObject ?: return
		val currentCalendar = (parent.viewModel.currentCalendar.value
			?: Calendar.getInstance()).clone() as Calendar
		val zoneId = currentCalendar.timeZone.toZoneId()
		val today = LocalDate.now(zoneId)
		updateScheduleCard(obj, today)
		notifyScheduleCardChanged()
	}

	private fun notifyScheduleCardChanged() {
		if (!::adapter.isInitialized) {
			return
		}
		val position = adapter.getItemPosition<AstroScheduleCardModel>()
		if (position >= 0) {
			adapter.notifyItemChanged(position)
		}
	}

	private fun setupRecyclerView() {
		adapter = AstroContextMenuAdapter(
			app,
			requireMapActivity(),
			nightMode,
			this,
			galleryController,
			onGalleryToggle = { wid ->
				onGalleryToggle(wid)
			},
			onUpdateImage = {
				startLoadingImagesTask()
			},
			onScheduleResetPeriod = {
				resetScheduleToCurrentPeriod()
			},
			onScheduleShiftPeriod = { daysDelta ->
				shiftSchedulePeriod(daysDelta)
			})

		adapter.skyObject = skyObject

		val layoutManager = LinearLayoutManager(requireContext())
		recyclerView.layoutManager = layoutManager
		recyclerView.adapter = adapter

		recyclerView.isNestedScrollingEnabled = true
		recyclerScrollListener?.let { recyclerView.removeOnScrollListener(it) }
		recyclerScrollListener = object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				updateBottomTabSelectionByScroll()
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
				.setIcon(R.drawable.ic_action_planet_outlined),
			true
		)
		bottomTabs.addTab(
			bottomTabs.newTab()
				.setText(R.string.gpx_visibility_txt)
				.setIcon(R.drawable.ic_action_ais_object_visibility)
		)
		bottomTabs.addTab(
			bottomTabs.newTab()
				.setText(R.string.astronomy_schedule)
				.setIcon(R.drawable.ic_action_calendar_month)
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

	private fun scrollToSelectedTab(tabPosition: Int) {
		when (tabPosition) {
			TAB_OVERVIEW -> {
				appBarLayout.setExpanded(true, true)
				scrollToAdapterPosition(0)
			}

			TAB_VISIBILITY -> {
				collapseHeaderCard()
				val visibilityPosition = adapter.getItemPosition<AstroVisibilityCardModel>()
				if (visibilityPosition >= 0) {
					scrollToAdapterPosition(visibilityPosition)
				}
			}

			TAB_SCHEDULE -> {
				collapseHeaderCard()
				val schedulePosition =
					adapter.getItemPosition<AstroScheduleCardModel>().let { cardPosition ->
						if (cardPosition >= 0) {
							cardPosition
						} else {
							(adapter.itemCount - 1).coerceAtLeast(0)
						}
					}
				scrollToAdapterPosition(schedulePosition)
			}
		}
	}

	private fun collapseHeaderCard() {
		appBarLayout.setExpanded(false, false)
	}

	private fun scrollToAdapterPosition(preferredPosition: Int) {
		if (!::adapter.isInitialized || adapter.itemCount == 0) {
			return
		}
		val position = preferredPosition.coerceIn(0, adapter.itemCount - 1)
		recyclerView.smoothScrollToPosition(position)
	}

	private fun updateBottomTabSelectionByScroll() {
		if (!::adapter.isInitialized || !::bottomTabs.isInitialized || bottomTabs.tabCount == 0) {
			return
		}
		val visibilityPosition = adapter.getItemPosition<AstroVisibilityCardModel>()
		if (visibilityPosition < 0) {
			return
		}
		val schedulePosition = adapter.getItemPosition<AstroScheduleCardModel>()
		val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
		val visibilityView = layoutManager.findViewByPosition(visibilityPosition)
		val scheduleView =
			if (schedulePosition >= 0) layoutManager.findViewByPosition(schedulePosition) else null
		val viewportTop = recyclerView.paddingTop
		val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
		val topBoundaryY = viewportTop

		val visibilityReached = if (visibilityView != null) {
			visibilityView.top <= topBoundaryY
		} else {
			firstVisiblePosition >= visibilityPosition
		}
		val scheduleReached = if (schedulePosition >= 0) {
			if (scheduleView != null) {
				scheduleView.top <= topBoundaryY
			} else {
				firstVisiblePosition >= schedulePosition
			}
		} else {
			false
		}

		val targetTab = when {
			scheduleReached -> TAB_SCHEDULE
			visibilityReached -> TAB_VISIBILITY
			else -> TAB_OVERVIEW
		}
		selectBottomTabWithoutScroll(targetTab)
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
		recyclerView.updatePadding(bottom = recyclerBaseBottomPadding + tabsHeight)
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
		bottomSheetContainer ?: return
		val revealProgress = if (systemTopInset > 0) {
			(systemTopInset - sheetTop).coerceIn(0, systemTopInset).toFloat() / systemTopInset
		} else {
			val progress = resolveBottomSheetExpansionProgress(sheetTop)
			if (progress >= 1f) 1f else 0f
		}
		updateBottomSheetCorner(bottomSheetCornerRadiusPx * (1f - revealProgress))
		currentSheetTop = sheetTop
		updateTopInsetReveal()
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
		visibilityCardModel?.cancelPendingLookups()
		visibilityCardModel?.onDataChanged = null
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
		bottomSheetCallback?.let { callback ->
			bottomSheetBehavior?.removeBottomSheetCallback(callback)
		}
		bottomSheetCallback = null
		bottomSheetContainer = null
		bottomSheetBehavior = null
		bottomSheetBackground = null
		super.onDestroyView()
	}

	private fun onGalleryToggle(wid: String) {
		val model = galleryCardModel ?: return

		when (model.state) {
			AstroGalleryCardModel.GalleryState.Collapsed -> {
				model.state = AstroGalleryCardModel.GalleryState.Loading
				adapter.notifyItemChanged(
					adapter.getItemPosition<AstroGalleryCardModel>(),
					AstroContextMenuAdapter.PAYLOAD_GALLERY_STATE
				)

				startLoadingImagesTask()
			}

			is AstroGalleryCardModel.GalleryState.Ready -> {
				model.state = AstroGalleryCardModel.GalleryState.Collapsed
				adapter.notifyItemChanged(
					adapter.getItemPosition<AstroGalleryCardModel>(),
					AstroContextMenuAdapter.PAYLOAD_GALLERY_STATE
				)
			}

			AstroGalleryCardModel.GalleryState.Loading -> Unit
			is AstroGalleryCardModel.GalleryState.Error -> {
				model.state = AstroGalleryCardModel.GalleryState.Loading
				adapter.notifyItemChanged(
					adapter.getItemPosition<AstroGalleryCardModel>(),
					AstroContextMenuAdapter.PAYLOAD_GALLERY_STATE
				)
				startLoadingImagesTask()
			}
		}
	}

	private fun startLoadingImagesTask() {
		if (galleryController == null || skyObject == null) {
			return
		}

		val latLon = LatLon(0.0, 0.0)
		val params = HashMap<String, String>()
		params["wikidataId"] = skyObject!!.wid
		val cacheManager = PhotoCacheManager(app)
		val builder = StringBuilder()
		builder.append("wikidataId=").append(skyObject!!.wid)
		val rawKey = builder.toString()

		if (galleryController!!.isCurrentHolderEquals(latLon, params)) {
			galleryController?.currentCardsHolder?.apply {
				setOnlinePhotosCards(this.astronomyCards)
			}
		} else if (!app.getSettings().isInternetConnectionAvailable) {
			loadFromCache(cacheManager, rawKey)
		} else {
			stopLoadingImagesTask()
			galleryController!!.clearHolder()

			val holder = ImageCardsHolder(latLon, params)
			getAstroImagesTask = GetAstroImagesTask(
				app, holder, skyObject!!.wid,
				imageCardListener, { response: String? ->
					savePhotoListToCache(
						cacheManager,
						rawKey,
						response
					)
				})

			OsmAndTaskManager.executeTask<Void?, GetAstroImagesTask?>(getAstroImagesTask as GetAstroImagesTask)
		}
	}

	private fun savePhotoListToCache(
		cacheManager: PhotoCacheManager,
		rawKey: String,
		response: String?
	) {
		if (!Algorithms.isEmpty(response)) {
			val cacheWriteTask = CacheWriteTask(cacheManager, rawKey, response!!)
			OsmAndTaskManager.executeTask<Void?, CacheWriteTask?>(cacheWriteTask)
		}
	}

	private fun stopLoadingImagesTask() {
		if (getAstroImagesTask != null && getAstroImagesTask?.status == AsyncTask.Status.RUNNING) {
			getAstroImagesTask?.cancel(false)
		}
	}

	private fun loadFromCache(
		cacheManager: PhotoCacheManager, rawKey: String
	) {
		if (cacheManager.exists(rawKey)) {
			imageCardListener.onTaskStarted()
			val cacheReadTask =
				CacheReadTask(cacheManager, rawKey) { json: String? ->
					if (!Algorithms.isEmpty(json)) {
						val wikimediaImageList: List<WikiImage> =
							WikiCoreHelper.getAstroImagesFromJson(json!!)

						imageCardListener.onFinish(wikimediaImageList)
					} else {
						imageCardListener.onFinish(null)
					}
					true
				}
			OsmAndTaskManager.executeTask<Void?, CacheReadTask?>(cacheReadTask)
		} else {
			val cards: MutableList<AbstractCard?> = ArrayList()
			galleryCardModel?.state = AstroGalleryCardModel.GalleryState.Ready(cards)
			adapter.notifyItemChanged(
				adapter.getItemPosition<AstroGalleryCardModel>(),
				AstroContextMenuAdapter.Companion.PAYLOAD_GALLERY_CONTENT
			)
		}
	}

	private fun loadCards() {
		val items = ArrayList<AstroContextCard>()

		descriptionCardModel?.let {
			items.add(it)
		}

		catalogCardModel?.let {
			items.add(it)
		}

		galleryCardModel?.let {
			items.add(it)
		}

		visibilityCardModel?.let {
			items.add(it)
		}

		scheduleCardModel?.let {
			items.add(it)
		}

		adapter.setItems(items)
		updateBottomTabSelectionByScroll()
	}
}