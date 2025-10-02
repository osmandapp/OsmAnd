package net.osmand.plus.exploreplaces

import android.animation.ValueAnimator
import android.os.AsyncTask.Status.RUNNING
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import net.osmand.CallbackWithObject
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.data.Amenity
import net.osmand.map.IMapLocationListener
import net.osmand.plus.AppInitializeListener
import net.osmand.plus.AppInitializer
import net.osmand.plus.OsmAndConstants.EXPLORE_PLACES_UPDATE
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener
import net.osmand.plus.OsmAndTaskManager
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseFullScreenFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.poi.PoiUIFilter
import net.osmand.plus.search.NearbyPlacesAdapter.NearbyItemClickListener
import net.osmand.plus.search.listitems.QuickSearchListItem
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.views.OsmandMapTileView.MapZoomChangeListener
import net.osmand.plus.views.controls.maphudbuttons.MyLocationButton
import net.osmand.plus.views.controls.maphudbuttons.ZoomInButton
import net.osmand.plus.views.controls.maphudbuttons.ZoomOutButton
import net.osmand.plus.views.mapwidgets.TopToolbarView
import net.osmand.plus.widgets.EmptyStateRecyclerView
import net.osmand.plus.wikipedia.WikipediaPlugin
import net.osmand.util.MapUtils
import org.apache.commons.logging.Log
import java.util.concurrent.Executors
import kotlin.math.abs

class ExplorePlacesFragment : BaseFullScreenFragment(), NearbyItemClickListener,
	OsmAndLocationListener, OsmAndCompassListener, IMapLocationListener, MapZoomChangeListener {

	private val log: Log = PlatformUtil.getLog(ExplorePlacesFragment::class.java)

	private val plugin = PluginsHelper.requirePlugin(WikipediaPlugin::class.java)

	private val singleThreadExecutor = Executors.newSingleThreadExecutor()
	private var convertAmenitiesTask: ConvertAmenitiesTask? = null

	private var poiUIFilter: PoiUIFilter? = null
	private var adapter: ExplorePlacesAdapter? = null

	private var visiblePlaces: List<Amenity>? = null
	private var location: Location? = null
	private var mainContent: LinearLayout? = null
	private var recyclerView: EmptyStateRecyclerView? = null
	private var showListContainer: View? = null
	private var frameLayout: CoordinatorLayout? = null
	private var lastCompassUpdate = 0L
	private var bottomSheetBehavior: BottomSheetBehavior<*>? = null
	private var lastHeading = 0f
	private var showOnMapContainer: View? = null
	private var zoomButtonsView: View? = null
	private var isPortrait: Boolean = false

	override fun getContentStatusBarNightMode(): Boolean {
		return nightMode
	}

	@ColorRes
	override fun getStatusBarColorId(): Int {
		AndroidUiHelper.setStatusBarContentColor(view, nightMode)
		return if (nightMode) R.color.status_bar_main_dark else R.color.status_bar_main_light
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState != null) {
			val filterId = savedInstanceState.getString(POI_UI_FILTER_ID)
			poiUIFilter = app.poiFilters.getFilterById(filterId)
			if (poiUIFilter == null && app.appInitializer.isAppInitializing) {
				app.appInitializer.addListener(object : AppInitializeListener {
					override fun onFinish(init: AppInitializer) {
						init.removeListener(this)
						poiUIFilter = app.poiFilters.getFilterById(filterId)
						if (isAdded) {
							adapter?.setPoiUIFilter(poiUIFilter)
							poiUIFilter?.let {
								app.poiFilters.replaceSelectedPoiFilters(it)
							}
						}
					}
				})
			}
		}
		isPortrait = AndroidUiHelper.isOrientationPortrait(requireActivity())
	}

	fun onBackPress(): Boolean {
		if (bottomSheetBehavior?.state == STATE_HIDDEN || (!isPortrait && !isLandScapeVisible())) {
			return if (mapActivity?.contextMenu?.isVisible == true) {
				mapActivity?.contextMenu?.hideMenus()
				true
			} else {
				false
			}
		} else {
			hideList()
			return true
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View? {
		updateNightMode()
		AndroidUiHelper.updateVisibility(showOnMapContainer, false)

		val view = themedInflater.inflate(R.layout.fragment_nearby_places, container, false)
		if (!AndroidUiHelper.isOrientationPortrait(view.context)) {
			val rtl = AndroidUtils.isLayoutRtl(mapActivity)
			val attrId = if (rtl) R.attr.right_menu_view_bg else R.attr.left_menu_view_bg
			val width = resources.getDimensionPixelSize(R.dimen.dashboard_land_width)

			view.setBackgroundResource(AndroidUtils.resolveAttribute(view.context, attrId))
			view.layoutParams = LinearLayout.LayoutParams(width, MATCH_PARENT)
		}

		AndroidUtils.addStatusBarPadding21v(requireActivity(), view)
		mainContent = view.findViewById(R.id.main_content)
		showListContainer = view.findViewById(R.id.show_list_container)
		frameLayout = view.findViewById(R.id.frame_layout)

		setupRecyclerView(view)
		buildZoomButtons(view)
		updatePoints()

        if (isPortrait) {
            bottomSheetBehavior = BottomSheetBehavior.from(mainContent!!)
            bottomSheetBehavior?.state = STATE_HIDDEN
            bottomSheetBehavior?.peekHeight =
                resources.getDimensionPixelSize(R.dimen.bottom_sheet_menu_peek_height)
            bottomSheetBehavior?.isHideable = true
            bottomSheetBehavior?.isDraggable = AndroidUiHelper.isOrientationPortrait(view.context)
        } else {
			AndroidUiHelper.updateVisibility(showListContainer, false)

			val isRtl = AndroidUtils.isLayoutRtl(requireActivity())
			val fragmentWidth = resources.getDimensionPixelSize(R.dimen.dashboard_land_width).toFloat()

			view.translationX = if (isRtl) {
				fragmentWidth
			} else {
				-fragmentWidth
			}

            getToolbar()?.saveInitialViewParams()
            getToolbar()?.setupAnimationParams()
        }

		updateMapControls()
		return view
	}

	fun isListHidden(): Boolean {
		return view == null ||
				isPortrait && bottomSheetBehavior?.state == STATE_HIDDEN ||
				!isPortrait && !isLandScapeVisible()
	}

	private fun setupRecyclerView(view: View) {
		adapter = ExplorePlacesAdapter(view.context, poiUIFilter, this, nightMode)
		recyclerView = view.findViewById(R.id.vertical_nearby_list)
		recyclerView?.layoutManager = LinearLayoutManager(view.context)
		recyclerView?.adapter = adapter
		recyclerView?.setEmptyView(view.findViewById(R.id.empty_view))
	}

	private fun buildZoomButtons(view: View) {
		zoomButtonsView = view.findViewById(R.id.map_hud_controls)
		mapActivity?.let { activity ->
			val mapLayers = activity.mapLayers
			val layer = mapLayers.mapControlsLayer
			val zoomInBtn = view.findViewById<ZoomInButton>(R.id.map_zoom_in_button)
			if (zoomInBtn != null) {
				layer.addCustomizedDefaultMapButton(zoomInBtn)
			}
			val zoomOutBtn = view.findViewById<ZoomOutButton>(R.id.map_zoom_out_button)
			if (zoomOutBtn != null) {
				layer.addCustomizedDefaultMapButton(zoomOutBtn)
			}
			val myLocationBtn = view.findViewById<MyLocationButton>(R.id.map_my_location_button)
			if (myLocationBtn != null) {
				layer.addCustomizedDefaultMapButton(myLocationBtn)
			}
		}
	}

	private val bottomSheetCallback = object : BottomSheetCallback() {
		override fun onStateChanged(bottomSheet: View, newState: Int) {
			updateBottomSheetHeight()
			updateMapControls()
		}

		override fun onSlide(bottomSheet: View, slideOffset: Float) {
		}
	}

	private fun updateBottomSheetHeight() {
		if (!isPortrait) {
			return
		}

		val minPeekHeight =
			resources.getDimensionPixelSize(R.dimen.bottom_sheet_min_peek_height)
		val defaultPeekHeight =
			resources.getDimensionPixelSize(R.dimen.bottom_sheet_menu_peek_height)
		bottomSheetBehavior?.peekHeight =
			if (adapter?.itemCount == 0) {
				minPeekHeight
			} else {
				defaultPeekHeight
			}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (savedInstanceState == null) {
			hideList()
		}
		updateMapControls()
	}

	fun updateMapControls() {
		if (!isPortrait) {
			return
		}

		val state = bottomSheetBehavior?.state
		AndroidUiHelper.updateVisibility(
			showListContainer, state == STATE_COLLAPSED
		)
		val params = zoomButtonsView?.layoutParams as ViewGroup.MarginLayoutParams
		params.bottomMargin =
			if (state == STATE_COLLAPSED) bottomSheetBehavior?.peekHeight ?: 0 else 0
		zoomButtonsView?.layoutParams = params
		zoomButtonsView?.requestLayout()
	}

	private fun updatePoints() {
		if (app.osmandMap.mapView.isMapInteractionActive || poiUIFilter == null || isListHidden()) {
			return
		}
		val visiblePlaces = app.osmandMap.mapLayers.poiMapLayer.visiblePlaces
		if (visiblePlaces != null && visiblePlaces != this.visiblePlaces) {
			this.visiblePlaces = visiblePlaces

			val callback = CallbackWithObject<List<QuickSearchListItem>> { result ->
				if (isAdded) {
					adapter?.setItems(result)
					updateBottomSheetHeight()
					updateMapControls()
				}
				true
			}
			stopConvertAmenitiesTask()
			convertAmenitiesTask =
				ConvertAmenitiesTask(
					app,
					visiblePlaces,
					poiUIFilter?.isTopImagesFilter == true,
					app.locationProvider,
					callback)
			convertAmenitiesTask?.let {
				OsmAndTaskManager.executeTask(it, singleThreadExecutor, null)
			}
		}
	}

	private fun stopConvertAmenitiesTask() {
		if (convertAmenitiesTask?.status == RUNNING) {
			convertAmenitiesTask?.cancel(false)
		}
	}

	override fun onResume() {
		super.onResume()

		startHandler()
		mapActivity?.disableDrawer()
		bottomSheetBehavior?.addBottomSheetCallback(bottomSheetCallback)

		app.locationProvider.addLocationListener(this)
		app.locationProvider.addCompassListener(this)
		app.osmandMap.mapView.addMapLocationListener(this)
		app.osmandMap.mapView.addMapZoomChangeListener(this)
	}

	override fun onPause() {
		super.onPause()

		stopConvertAmenitiesTask()
		mapActivity?.enableDrawer()
		bottomSheetBehavior?.removeBottomSheetCallback(bottomSheetCallback)

		app.locationProvider.removeLocationListener(this)
		app.locationProvider.removeCompassListener(this)
		app.osmandMap.mapView.removeMapLocationListener(this)
		app.osmandMap.mapView.removeMapZoomChangeListener(this)
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putString(POI_UI_FILTER_ID, poiUIFilter?.filterId)
	}

	override fun updateLocation(location: Location?) {
		if (!MapUtils.areLatLonEqual(this.location, location)) {
			this.location = location
			updateAdapter()
		}
	}

	override fun updateCompassValue(heading: Float) {
		val now = System.currentTimeMillis()
		if (now - lastCompassUpdate > COMPASS_UPDATE_PERIOD && abs(
				MapUtils.degreesDiff(lastHeading.toDouble(), heading.toDouble())
			) > 5
		) {
			lastHeading = heading
			lastCompassUpdate = now
			updateAdapter()
		}
	}

	private fun updateAdapter() {
		app.runInUIThreadAndCancelPrevious(REFRESH_UI_ID, {
			adapter?.notifyDataSetChanged()
		}, 0)
	}

	private fun startHandler() {
		val handler = Handler(Looper.getMainLooper())
		handler.postDelayed({
			if (view != null && isResumed) {
				updatePoints()
				startHandler()
			}
		}, LIST_UPDATE_PERIOD.toLong())
	}

	private fun showPointInContextMenu(amenity: Amenity) {
		mapActivity?.apply {
			val contextMenuLayer = mapLayers.contextMenuLayer
			val poiMapLayer = mapLayers.poiMapLayer
			contextMenuLayer.showContextMenu(
				amenity.location,
				poiMapLayer.getObjectName(amenity),
				amenity,
				poiMapLayer)
		}
	}

	override fun onNearbyItemClicked(amenity: Amenity) {
		mapActivity?.let {
			showPointInContextMenu(amenity)
			hideList()
		}
	}

	fun hideList() {
		if (isPortrait) {
			bottomSheetBehavior?.state = STATE_HIDDEN
		} else {
			hideLandscapeFragment()
		}
	}

	override fun locationChanged(p0: Double, p1: Double, p2: Any?) {
		updatePoints()
	}

	override fun onMapZoomChanged(manual: Boolean) {
		if (manual) {
			updatePoints()
		}
	}

	fun toggleState() {
		if (isPortrait) {
			when (bottomSheetBehavior?.state) {
				STATE_HIDDEN -> bottomSheetBehavior?.state = STATE_COLLAPSED
				STATE_COLLAPSED, STATE_EXPANDED -> bottomSheetBehavior?.state = STATE_HIDDEN
			}
		} else {
			toggleFragmentSlide()
		}
	}

	private fun hideLandscapeFragment() {
		mapActivity?.let {
			if (isLandScapeVisible()) {
				slideLandscapeFragment(it, requireView())
			}
		}
	}

	private fun toggleFragmentSlide() {
		mapActivity?.let {
			slideLandscapeFragment(it, requireView())
		}
	}

	private fun isLandScapeVisible(): Boolean {
		val isRtl = AndroidUtils.isLayoutRtl(requireActivity())
		val translation = requireView().translationX
		return if (isRtl) {
			!(translation > 0f)
		} else {
			!(translation < 0f)
		}
	}

	private fun slideLandscapeFragment(mapActivity: MapActivity, viewContainer: View) {
		val density = app.resources.displayMetrics.density
		val fragmentWidthPx =
			resources.getDimensionPixelSize(R.dimen.dashboard_land_width).toFloat() * density
		val isLandScapeVisible = isLandScapeVisible()
		if (!settings.DO_NOT_USE_ANIMATIONS.get()) {
			val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
			valueAnimator.duration =
				app.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
			valueAnimator.addUpdateListener { animator ->
				val fraction = animator.animatedValue as Float

				val currentTranslationX = if (AndroidUtils.isLayoutRtl(requireActivity())) {
					if (!isLandScapeVisible) {
						fragmentWidthPx - fragmentWidthPx * fraction
					} else {
						fragmentWidthPx * fraction
					}
				} else {
					if (!isLandScapeVisible) {
						-fragmentWidthPx + fragmentWidthPx * fraction
					} else {
						-fragmentWidthPx * fraction
					}
				}
				viewContainer.translationX = currentTranslationX
				getToolbar()?.adjustForOverlay(viewContainer)
			}

			valueAnimator.start()
		} else {
			viewContainer.translationX = if (AndroidUtils.isLayoutRtl(requireActivity())) {
				if (!isLandScapeVisible()) 0f else fragmentWidthPx
			} else {
				if (!isLandScapeVisible()) 0f else -fragmentWidthPx
			}
			getToolbar()?.adjustForOverlay(viewContainer)
		}
	}

	private fun getToolbar(): TopToolbarView? {
		return mapActivity?.mapLayers?.mapInfoLayer?.topToolbarView
	}

	override fun onDestroy() {
		super.onDestroy()
		if (!isPortrait) {
			getToolbar()?.restoreSavedParams()
		}
	}

	fun closeFragment() {
		mapActivity?.let { activity ->
			val fragment = activity.fragmentsHelper.explorePlacesFragment
			if (fragment != null) {
				activity.fragmentsHelper.dismissFragment(TAG)
			}
		}
	}

	companion object {

		val TAG: String = ExplorePlacesFragment::class.java.simpleName

		private const val COMPASS_UPDATE_PERIOD = 300
		private const val LIST_UPDATE_PERIOD = 1000
		private const val REFRESH_UI_ID = EXPLORE_PLACES_UPDATE + 1
		private const val POI_UI_FILTER_ID = "poi_ui_filter_id"

		fun showInstance(manager: FragmentManager, poiUIFilter: PoiUIFilter) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = ExplorePlacesFragment()
				fragment.poiUIFilter = poiUIFilter
				manager.beginTransaction()
					.addToBackStack(TAG)
					.replace(R.id.fragmentContainer, fragment, TAG)
					.commitAllowingStateLoss()
			}
		}
	}
}