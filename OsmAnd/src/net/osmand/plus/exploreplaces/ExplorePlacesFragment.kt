package net.osmand.plus.exploreplaces

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Slide
import androidx.transition.TransitionManager
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.data.ExploreTopPlacePoint
import net.osmand.data.QuadRect
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseOsmAndFragment
import net.osmand.plus.dashboard.DashboardType
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.search.NearbyPlacesAdapter
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.views.controls.maphudbuttons.MyLocationButton
import net.osmand.plus.views.controls.maphudbuttons.ZoomInButton
import net.osmand.plus.views.controls.maphudbuttons.ZoomOutButton
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget
import net.osmand.plus.widgets.TextViewEx
import org.apache.commons.logging.Log

class ExplorePlacesFragment : BaseOsmAndFragment(), NearbyPlacesAdapter.NearbyItemClickListener,
	OsmAndLocationListener, OsmAndCompassListener {

	private val HIDE_LIST_DURATION = 150L
	private val SHOW_LIST_DURATION = 150L
	private lateinit var visiblePlacesRect: QuadRect
	private val log: Log = PlatformUtil.getLog(
		ExplorePlacesFragment::class.java)

	private lateinit var verticalNearbyAdapter: NearbyPlacesAdapter
	private var location: Location? = null
	private var heading: Float? = null
	private var mainContent: LinearLayout? = null
	private var showListContainer: View? = null
	private var frameLayout: FrameLayout? = null
	private var rulerWidget: RulerWidget? = null

	override fun getContentStatusBarNightMode(): Boolean {
		return nightMode
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?): View? {
		location = app.locationProvider.lastKnownLocation
		updateNightMode()
		return themedInflater.inflate(R.layout.fragment_nearby_places, container, false)
	}

	private fun buildZoomButtons(view: View) {
		val zoomButtonsView = view.findViewById<View>(R.id.map_hud_controls)
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
			val mapInfoLayer = mapLayers.mapInfoLayer
			rulerWidget =
				mapInfoLayer.setupRulerWidget(view.findViewById(R.id.map_ruler_layout))
			activity.mapLayers.mapControlsLayer.addCustomMapButton(view.findViewById(R.id.map_compass_button))
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		AndroidUtils.addStatusBarPadding21v(requireActivity(), view)
		arguments?.let {
			val left = it.getDouble("left")
			val right = it.getDouble("right")
			val top = it.getDouble("top")
			val bottom = it.getDouble("bottom")
			visiblePlacesRect = QuadRect(left, top, right, bottom) // Create QuadRect
		}
		mainContent = view.findViewById(R.id.main_content)
		showListContainer = view.findViewById(R.id.show_list_container)
		frameLayout = view.findViewById(R.id.frame_layout)
		setupShowAll(view)
		setupToolBar(view)
		setupVerticalNearbyList(view)
		buildZoomButtons(view)
		val dialogFragment = mapActivity?.fragmentsHelper?.quickSearchDialogFragment
		dialogFragment?.hide()
	}

	fun onBackPress(): Boolean {
		if (mainContent?.visibility == View.GONE) {
			if (mapActivity?.contextMenu?.isVisible == true) {
				mapActivity?.contextMenu?.hideMenus()
			} else {
				showList()
			}
		} else {
			val quickSearchFragment = mapActivity?.fragmentsHelper?.quickSearchDialogFragment
			quickSearchFragment?.show()
			activity?.supportFragmentManager?.beginTransaction()
				?.remove(this@ExplorePlacesFragment)
				?.commit()
		}
		return true
	}

	override fun onResume() {
		super.onResume()
		val app = requireActivity().application as OsmandApplication
		app.locationProvider.addLocationListener(this)
		app.locationProvider.addCompassListener(this)
		mapActivity?.dashboard?.setDashboardVisibility(true, DashboardType.EXPLORE_NEARBY_PLACES)
	}

	override fun onPause() {
		super.onPause()
		val app = requireActivity().application as OsmandApplication
		app.locationProvider.removeLocationListener(this)
		app.locationProvider.removeCompassListener(this)
	}

	override fun updateLocation(location: Location?) {
		this.location = location
		verticalNearbyAdapter.updateLocation(location, heading)
	}

	override fun updateCompassValue(value: Float) {
		this.heading = value
		verticalNearbyAdapter.updateLocation(location, heading)
	}

	private fun setupShowAll(view: View) {
		view.findViewById<ImageView>(R.id.location_icon)
			.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_marker_dark, nightMode))
		val showList = view.findViewById<TextViewEx>(R.id.show_list)
		view.findViewById<View>(R.id.show_on_map).setOnClickListener {
			app.osmandMap.mapLayers.explorePlacesLayer.enableLayer(true)
			hideList()
		}
		showList.setOnClickListener {
			showList()
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
		val verticalNearbyList = view.findViewById<RecyclerView>(R.id.vertical_nearby_list)
		val nearbyData = app.explorePlacesProvider.getDataCollection(visiblePlacesRect)
		verticalNearbyAdapter = NearbyPlacesAdapter(requireActivity(), nearbyData, true, this)
		verticalNearbyList.layoutManager = LinearLayoutManager(requireContext())
		verticalNearbyList.adapter = verticalNearbyAdapter
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
		fun showInstance(manager: FragmentManager, visiblePlacesRect: QuadRect) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = ExplorePlacesFragment()
				val bundle = Bundle()
				bundle.putDouble("left", visiblePlacesRect.left)
				bundle.putDouble("right", visiblePlacesRect.right)
				bundle.putDouble("top", visiblePlacesRect.top)
				bundle.putDouble("bottom", visiblePlacesRect.bottom)
				fragment.arguments = bundle
				manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.commitAllowingStateLoss()
			}
		}
	}

	override fun onNearbyItemClicked(item: ExploreTopPlacePoint) {
		mapActivity?.let {
			app.explorePlacesProvider.showPointInContextMenu(it, item)
			hideList()
		}
	}

	private fun hideList() {
		frameLayout?.let {
			TransitionManager.beginDelayedTransition(it, Slide(Gravity.BOTTOM).apply {
				duration = HIDE_LIST_DURATION
				interpolator = AccelerateInterpolator()
			})
		}
		mainContent?.visibility = View.GONE
		showListContainer?.visibility = View.VISIBLE
	}

	private fun showList() {
		frameLayout?.let {
			TransitionManager.beginDelayedTransition(it, Slide(Gravity.BOTTOM).apply {
				duration = SHOW_LIST_DURATION
				interpolator = AccelerateInterpolator()
			})
		}
		mainContent?.visibility = View.VISIBLE
		showListContainer?.visibility = View.GONE
	}
}