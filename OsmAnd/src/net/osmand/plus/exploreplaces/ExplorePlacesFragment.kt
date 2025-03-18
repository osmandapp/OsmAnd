package net.osmand.plus.exploreplaces

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.data.Amenity
import net.osmand.data.PointDescription
import net.osmand.map.IMapLocationListener
import net.osmand.osm.MapPoiTypes
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseOsmAndFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.poi.PoiUIFilter
import net.osmand.plus.search.NearbyPlacesAdapter.NearbyItemClickListener
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment
import net.osmand.plus.search.listitems.QuickSearchListItem
import net.osmand.plus.search.listitems.QuickSearchWikiItem
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.views.OsmandMapTileView.MapZoomChangeListener
import net.osmand.plus.views.controls.maphudbuttons.MyLocationButton
import net.osmand.plus.views.controls.maphudbuttons.ZoomInButton
import net.osmand.plus.views.controls.maphudbuttons.ZoomOutButton
import net.osmand.plus.wikipedia.WikipediaPlugin
import net.osmand.search.core.SearchCoreFactory
import net.osmand.search.core.SearchPhrase
import net.osmand.util.Algorithms
import net.osmand.util.MapUtils
import org.apache.commons.logging.Log
import java.util.Collections
import kotlin.math.abs

class ExplorePlacesFragment : BaseOsmAndFragment(), NearbyItemClickListener,
	OsmAndLocationListener, OsmAndCompassListener, IMapLocationListener, MapZoomChangeListener {

	private val log: Log = PlatformUtil.getLog(ExplorePlacesFragment::class.java)

	private val plugin = PluginsHelper.requirePlugin(WikipediaPlugin::class.java)

	private lateinit var poiUIFilter: PoiUIFilter
	private lateinit var adapter: ExplorePlacesAdapter

	private var amenities: List<Amenity>? = null
	private var location: Location? = null
	private var mainContent: LinearLayout? = null
	private var recyclerView: RecyclerView? = null
	private var showListContainer: View? = null
	private var frameLayout: CoordinatorLayout? = null
	private var lastCompassUpdate = 0L
	private var lastPointListRectUpdate = 0L
	private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
	private var lastHeading = 0f
	private var showOnMapContainer: View? = null
	private var zoomButtonsView: View? = null

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

		savedInstanceState?.let {
			val filterId = savedInstanceState.getString(POI_UI_FILTER_ID)
			poiUIFilter = app.poiFilters.getFilterById(filterId)
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

		setupShowAll(view)
		setupRecyclerView(view)
		buildZoomButtons(view)
		updatePoints()

		bottomSheetBehavior = BottomSheetBehavior.from(mainContent!!)
		bottomSheetBehavior.state = STATE_HIDDEN
		bottomSheetBehavior.peekHeight =
			resources.getDimensionPixelSize(R.dimen.bottom_sheet_menu_peek_height)
		bottomSheetBehavior.isHideable = true
		bottomSheetBehavior.isDraggable = true

		return view
	}

	private fun setupRecyclerView(view: View) {
		adapter = ExplorePlacesAdapter(view.context, poiUIFilter, this, nightMode)

		recyclerView = view.findViewById(R.id.vertical_nearby_list)
		recyclerView?.layoutManager = LinearLayoutManager(view.context)
		recyclerView?.adapter = adapter
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

	private val bottomSheetCallback = object : BottomSheetCallback() {
		override fun onStateChanged(bottomSheet: View, newState: Int) {
			recyclerView?.let { recyclerView ->
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
			updateMapControls()
		}

		override fun onSlide(bottomSheet: View, slideOffset: Float) {
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		updateMapControls()
	}

	private fun setupShowAll(view: View) {
		showOnMapContainer = view.findViewById(R.id.show_on_map_container)
		AndroidUiHelper.updateVisibility(showOnMapContainer, false)
		view.findViewById<ImageView>(R.id.location_icon)
			.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_marker_dark, nightMode))
		view.findViewById<View>(R.id.show_on_map).setOnClickListener {
			hideList()
		}
	}

	fun updateMapControls() {
		val state = bottomSheetBehavior.state
		AndroidUiHelper.updateVisibility(
			showListContainer, state == STATE_HIDDEN || state == STATE_COLLAPSED
		)
		val lp = zoomButtonsView?.layoutParams as ViewGroup.MarginLayoutParams
		lp.bottomMargin =
			if (state == STATE_COLLAPSED) resources.getDimensionPixelSize(R.dimen.bottom_sheet_menu_peek_height) else 0
		zoomButtonsView?.layoutParams = lp
		zoomButtonsView?.requestLayout()
	}

	private fun updateAdapter() {
		val items = app.osmandMap.mapLayers.poiMapLayer.currentResults ?: Collections.emptyList()
		val collection = QuickSearchDialogFragment.createSearchResultCollection(app, items)

		val rows = ArrayList<QuickSearchListItem>()
		if (!Algorithms.isEmpty(collection.currentSearchResults)) {
			for (searchResult in collection.currentSearchResults) {
				if (poiUIFilter.isTopImagesFilter) {
					rows.add(QuickSearchWikiItem(app, searchResult))
				} else {
					rows.add(QuickSearchListItem(app, searchResult))
				}
			}
		}
		adapter.setItems(rows)
	}

	private fun updatePoints() {
		val now = System.currentTimeMillis()
		val results = app.osmandMap.mapLayers.poiMapLayer.currentResults
		if (results != null && results != amenities) {
			lastPointListRectUpdate = now
			amenities = results
			app.runInUIThread {
				if (isAdded) {
					updateAdapter()
					updateMapControls()
				}
			}
		}
	}

	override fun onResume() {
		super.onResume()

		startHandler();
		bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)

		app.locationProvider.addLocationListener(this)
		app.locationProvider.addCompassListener(this)
		app.osmandMap.mapView.addMapLocationListener(this)
		app.osmandMap.mapView.addMapZoomChangeListener(this)
	}

	override fun onPause() {
		super.onPause()

		bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback)

		app.locationProvider.removeLocationListener(this)
		app.locationProvider.removeCompassListener(this)
		app.osmandMap.mapView.removeMapLocationListener(this)
		app.osmandMap.mapView.removeMapZoomChangeListener(this)
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putString(POI_UI_FILTER_ID, poiUIFilter.filterId)
	}

	override fun updateLocation(location: Location?) {
		if (!MapUtils.areLatLonEqual(this.location, location)) {
			this.location = location
			adapter.notifyDataSetChanged()
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
			adapter.notifyDataSetChanged()
		}
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

	private fun showPointInContextMenu(mapActivity: MapActivity, point: Amenity) {
		val latitude = point.location.latitude
		val longitude = point.location.longitude
		val sr = SearchCoreFactory.createSearchResult(
			point, SearchPhrase.emptyPhrase(),
			MapPoiTypes.getDefault()
		)
		val pair = QuickSearchListItem.getPointDescriptionObject(app, sr)
		app.settings.setMapLocationToShow(
			latitude,
			longitude,
			SearchCoreFactory.PREFERRED_NEARBY_POINT_ZOOM,
			pair.first as PointDescription,
			true,
			point
		)
		MapActivity.launchMapActivityMoveToTop(mapActivity)
	}

	override fun onNearbyItemClicked(amenity: Amenity) {
		mapActivity?.let {
			showPointInContextMenu(it, amenity)
			hideList()
		}
	}

	private fun hideList() {
		bottomSheetBehavior.state = STATE_HIDDEN
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
		when (bottomSheetBehavior.state) {
			STATE_HIDDEN -> bottomSheetBehavior.state = STATE_COLLAPSED
			STATE_COLLAPSED, STATE_EXPANDED -> bottomSheetBehavior.state = STATE_HIDDEN
		}
	}

	fun closeFragment() {
		app.getPoiFilters().restoreSelectedPoiFilters()
		mapActivity?.let { activity ->
			val fragment = activity.fragmentsHelper.explorePlacesFragment
			if (fragment != null) {
				activity.fragmentsHelper.dismissFragment(TAG)
			}
		}
	}

	val mapActivity: MapActivity?
		get() {
			return activity as? MapActivity
		}

	companion object {

		val TAG: String = ExplorePlacesFragment::class.java.simpleName

		private const val COMPASS_UPDATE_PERIOD = 300
		private const val LIST_UPDATE_PERIOD = 1000
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