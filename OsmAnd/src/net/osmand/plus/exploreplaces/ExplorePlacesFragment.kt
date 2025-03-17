package net.osmand.plus.exploreplaces

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.data.Amenity
import net.osmand.data.PointDescription
import net.osmand.data.QuadRect
import net.osmand.data.RotatedTileBox
import net.osmand.map.IMapLocationListener
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseOsmAndFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.search.NearbyPlacesAdapter
import net.osmand.plus.search.ShowQuickSearchMode
import net.osmand.plus.search.dialogs.ExplorePlacesNearbyToolbarController
import net.osmand.plus.search.listitems.QuickSearchListItem
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.views.OsmandMapTileView
import net.osmand.plus.views.controls.maphudbuttons.MyLocationButton
import net.osmand.plus.views.controls.maphudbuttons.ZoomInButton
import net.osmand.plus.views.controls.maphudbuttons.ZoomOutButton
import net.osmand.plus.wikipedia.WikipediaPlugin
import net.osmand.search.core.SearchCoreFactory
import net.osmand.search.core.SearchPhrase
import net.osmand.util.MapUtils
import org.apache.commons.logging.Log
import java.util.Collections
import kotlin.math.abs

class ExplorePlacesFragment : BaseOsmAndFragment(), NearbyPlacesAdapter.NearbyItemClickListener,
	OsmAndLocationListener, OsmAndCompassListener, IMapLocationListener,
	OsmandMapTileView.ManualZoomListener {

	private val COMPASS_UPDATE_PERIOD = 300
	private var visiblePlacesRect = QuadRect()
	private val log: Log = PlatformUtil.getLog(
		ExplorePlacesFragment::class.java)

	private lateinit var verticalNearbyAdapter: NearbyPlacesAdapter
	private var location: Location? = null
	private var mainContent: LinearLayout? = null
	private var verticalNearbyList: RecyclerView? = null
	private var showListContainer: View? = null
	private var frameLayout: CoordinatorLayout? = null
	private var lastCompassUpdate = 0L
	private var lastPointListRectUpdate = 0L
	private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
	private var isMapVisible = false
	private var lastHeading = 0f
	private var explorePlacesToolbarController: ExplorePlacesNearbyToolbarController? = null
	private var showOnMapContainer: View? = null
	private var zoomButtonsView: View? = null

	override fun getContentStatusBarNightMode(): Boolean {
		return nightMode
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?): View? {
		location = app.locationProvider.lastKnownLocation
		updateNightMode()
		AndroidUiHelper.updateVisibility(
			showOnMapContainer,
			false)

		return themedInflater.inflate(R.layout.fragment_nearby_places, container, false)
	}

	private fun buildZoomButtons(view: View) {
		zoomButtonsView = view.findViewById(R.id.map_hud_controls)
		mapActivity?.let { activity ->
			val mapLayers = activity.mapLayers
			val layer = mapLayers.mapControlsLayer
			val zoomInBtn = view.findViewById<ZoomInButton>(R.id.map_zoom_in_button)
			if (zoomInBtn != null) {
				layer.addCustomMapButton(zoomInBtn)
			}
			val zoomOutBtn = view.findViewById<ZoomOutButton>(R.id.map_zoom_out_button)
			if (zoomOutBtn != null) {
				layer.addCustomMapButton(zoomOutBtn)
			}
			val myLocationBtn = view.findViewById<MyLocationButton>(R.id.map_my_location_button)
			if (myLocationBtn != null) {
				layer.addCustomMapButton(myLocationBtn)
			}
			AndroidUiHelper.updateVisibility(zoomButtonsView, true)
			activity.mapLayers.mapControlsLayer.addCustomMapButton(view.findViewById(R.id.map_compass_button))
		}
	}

	private val bottomSheetCallback = object :
		BottomSheetBehavior.BottomSheetCallback() {
		override fun onStateChanged(bottomSheet: View, newState: Int) {
			verticalNearbyList?.let { recyclerView ->
				val minPeekHeight =
					resources.getDimensionPixelSize(R.dimen.bottom_sheet_min_peek_height)
				val defaultPeekHeight =
					resources.getDimensionPixelSize(R.dimen.bottom_sheet_menu_peek_height)
				bottomSheetBehavior.peekHeight =
					if (recyclerView.measuredHeight < defaultPeekHeight) {
						minPeekHeight
					} else {
						defaultPeekHeight
					}
			}
			isMapVisible = newState != BottomSheetBehavior.STATE_EXPANDED
			toggleWikipediaLayer(isMapVisible)
			updateMapControls()
		}

		override fun onSlide(bottomSheet: View, slideOffset: Float) {
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		AndroidUtils.addStatusBarPadding21v(requireActivity(), view)
		mainContent = view.findViewById(R.id.main_content)
		showListContainer = view.findViewById(R.id.show_list_container)
		frameLayout = view.findViewById(R.id.frame_layout)
		setupShowAll(view)
		setupToolBar(view)
		setupVerticalNearbyList(view)
		buildZoomButtons(view)
		updatePointsList()
		val dialogFragment = mapActivity?.fragmentsHelper?.quickSearchDialogFragment
		dialogFragment?.hide()
		bottomSheetBehavior = BottomSheetBehavior.from(mainContent!!)
		bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
		bottomSheetBehavior.peekHeight =
			resources.getDimensionPixelSize(R.dimen.bottom_sheet_menu_peek_height)
		bottomSheetBehavior.isHideable = true
		bottomSheetBehavior.isDraggable = true
		updateMapControls()
	}

	private fun toggleWikipediaLayer(enable: Boolean) {
		val wikiPlugin: WikipediaPlugin? = PluginsHelper.getPlugin(WikipediaPlugin::class.java)
		wikiPlugin?.toggleWikipediaPoi(enable, null)
	}

	private fun setupShowAll(view: View) {
		showOnMapContainer = view.findViewById(R.id.show_on_map_container)
		AndroidUiHelper.updateVisibility(
			showOnMapContainer,
			false)
		view.findViewById<ImageView>(R.id.location_icon)
			.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_marker_dark, nightMode))
		view.findViewById<View>(R.id.show_on_map).setOnClickListener {
			hideList()
		}
	}

	fun updateMapControls() {
		val state = bottomSheetBehavior.state
		AndroidUiHelper.updateVisibility(
			showListContainer,
			state == BottomSheetBehavior.STATE_HIDDEN ||
					state == BottomSheetBehavior.STATE_COLLAPSED)
		val lp = zoomButtonsView?.layoutParams as ViewGroup.MarginLayoutParams
		lp.bottomMargin =
			if (state == BottomSheetBehavior.STATE_COLLAPSED) resources.getDimensionPixelSize(R.dimen.bottom_sheet_menu_peek_height) else 0
		zoomButtonsView?.layoutParams = lp
		zoomButtonsView?.requestLayout()
	}

	override fun onResume() {
		super.onResume()
		bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
		app.locationProvider.addLocationListener(this)
		app.locationProvider.addCompassListener(this)
		app.osmandMap.mapView.addMapLocationListener(this)
		app.osmandMap.mapView.addManualZoomChangeListener(this)
		showToolbar()
	}

	private fun updatePointsList() {
		mapActivity?.let {
			val now = System.currentTimeMillis()
			val tileBox = it.mapView.rotatedTileBox
			val rect = tileBox.latLonBounds
			val extended: RotatedTileBox = tileBox.copy()
			extended.increasePixelDimensions(tileBox.pixWidth / 4, tileBox.pixHeight / 4)
			val extendedRect = extended.latLonBounds
			if (!extendedRect.contains(visiblePlacesRect) && now - lastPointListRectUpdate > 1000) {
				lastPointListRectUpdate = now
				visiblePlacesRect = rect
				verticalNearbyAdapter.items =
					app.osmandMap.mapLayers.poiMapLayer.currentResults ?: Collections.emptyList()
				app.runInUIThread {
					verticalNearbyAdapter.notifyDataSetChanged()
					updateMapControls()
				}
			}
		}
	}

	override fun onPause() {
		super.onPause()
		bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback)
		app.locationProvider.removeLocationListener(this)
		app.locationProvider.removeCompassListener(this)
		app.osmandMap.mapView.removeMapLocationListener(this)
		app.osmandMap.mapView.removeManualZoomListener(this)
	}

	override fun updateLocation(location: Location?) {
		this.location = location
		verticalNearbyAdapter.updateLocation(location)
	}

	override fun updateCompassValue(heading: Float) {
		val now = System.currentTimeMillis()
		if (now - lastCompassUpdate > COMPASS_UPDATE_PERIOD && abs(
				MapUtils.degreesDiff(
					lastHeading.toDouble(),
					heading.toDouble())) > 5) {
			lastHeading = heading
			lastCompassUpdate = now
			updateLocation(location)
		}
	}

	@ColorRes
	override fun getStatusBarColorId(): Int {
		AndroidUiHelper.setStatusBarContentColor(view, nightMode)
		return if (nightMode) R.color.status_bar_main_dark else R.color.status_bar_main_light
	}

	private fun setupToolBar(view: View) {
		val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
		toolbar.setTitleTextColor(app.getColor(ColorUtilities.getPrimaryTextColorId(nightMode)))
		toolbar.navigationIcon =
			getIcon(R.drawable.ic_arrow_back, ColorUtilities.getPrimaryIconColorId(nightMode))
		toolbar.setNavigationContentDescription(R.string.shared_string_close)
		toolbar.setNavigationOnClickListener { v: View? ->
			requireActivity().onBackPressed()
		}
	}

	private fun setupVerticalNearbyList(view: View) {
		verticalNearbyList = view.findViewById(R.id.vertical_nearby_list)
		val nearbyData =
			app.osmandMap.mapLayers.poiMapLayer.currentResults ?: Collections.emptyList()
		verticalNearbyAdapter = NearbyPlacesAdapter(view.context, nearbyData, true, this)
		verticalNearbyList?.layoutManager = LinearLayoutManager(requireContext())
		verticalNearbyList?.adapter = verticalNearbyAdapter
		verticalNearbyAdapter.notifyDataSetChanged()
	}

	val mapActivity: MapActivity?
		get() {
			val activity = activity
			return if (activity is MapActivity) {
				activity
			} else {
				null
			}
		}

	companion object {
		val TAG: String = ExplorePlacesFragment::class.java.simpleName
		fun showInstance(manager: FragmentManager) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = ExplorePlacesFragment()
				manager.beginTransaction()
					.addToBackStack(null)
					.replace(R.id.fragmentContainer, fragment, TAG)
					.commitAllowingStateLoss()
			}
		}
	}

	private fun showPointInContextMenu(mapActivity: MapActivity, point: Amenity) {
		val latitude = point.location.latitude
		val longitude = point.location.longitude
		val sr = SearchCoreFactory.createAmenitySearchResult(SearchPhrase.emptyPhrase(), point)
		val pair = QuickSearchListItem.getPointDescriptionObject(app, sr)
		app.settings.setMapLocationToShow(
			latitude,
			longitude,
			SearchCoreFactory.PREFERRED_NEARBY_POINT_ZOOM,
			pair.first as PointDescription,
			true,
			point)
		MapActivity.launchMapActivityMoveToTop(mapActivity)
	}

	override fun onNearbyItemClicked(amenity: Amenity) {
		mapActivity?.let {
			isMapVisible = true
			showPointInContextMenu(it, amenity)
			hideList()
		}
	}

	private fun hideList() {
		bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
	}

	override fun locationChanged(p0: Double, p1: Double, p2: Any?) {
		updatePointsList()
	}

	override fun onManualZoomChange() {
		updatePointsList()
	}

	private fun getToolbar(): ExplorePlacesNearbyToolbarController? {
		if (explorePlacesToolbarController == null) {
			mapActivity?.let { activity ->
				val toolbar =
					ExplorePlacesNearbyToolbarController(app, activity.supportFragmentManager)
				toolbar.setOnBackButtonClickListener { _: View? ->
					onBackPressed()
				}
				toolbar.setOnTitleClickListener { _: View? ->
					onBackPressed()
				}
				toolbar.setOnCloseButtonClickListener { _: View? ->
					closeFragment()
				}
				toolbar.setOnRefreshButtonClickListener { onShowListClicked() }
				toolbar.setOnCloseToolbarListener {
					bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
				}
				explorePlacesToolbarController = toolbar
			}
		}
		return explorePlacesToolbarController
	}

	private fun onShowListClicked() {
		when (bottomSheetBehavior.state) {
			BottomSheetBehavior.STATE_HIDDEN -> bottomSheetBehavior.state =
				BottomSheetBehavior.STATE_COLLAPSED

			BottomSheetBehavior.STATE_COLLAPSED, BottomSheetBehavior.STATE_EXPANDED -> bottomSheetBehavior.state =
				BottomSheetBehavior.STATE_HIDDEN
		}
	}

	private fun hideToolbar() {
		mapActivity?.let { activity ->
			getToolbar()?.let { toolbar ->
				activity.hideTopToolbar(toolbar)
				activity.updateStatusBarColor()
			}
		}
	}

	fun onBackPressed() {
		mapActivity?.let { activity ->
			activity.fragmentsHelper.showQuickSearch(ShowQuickSearchMode.CURRENT, false)
			closeFragment()
		}
	}

	private fun closeFragment() {
		toggleWikipediaLayer(false)
		mapActivity?.let { activity ->
			val nearbyPlacesFragment = activity.fragmentsHelper.explorePlacesFragment
			if (nearbyPlacesFragment != null) {
				activity.fragmentsHelper.dismissFragment(null)
			}
			hideToolbar()
		}
	}

	private fun showToolbar() {
		mapActivity?.let { activity ->
			getToolbar()?.let { toolbar ->
				activity.showTopToolbar(toolbar)
				activity.updateStatusBarColor()
				activity.refreshMap()
				val dialogFragment = activity.fragmentsHelper.quickSearchDialogFragment
				dialogFragment?.hide()
			}
		}
	}
}