package net.osmand.plus.plugins.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.defineStar
import io.github.cosinekitty.astronomy.searchRiseSet
import net.osmand.map.IMapLocationListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseFullScreenFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.astro.AstroUtils.toZoned
import net.osmand.plus.plugins.astro.StarChartState.StarChartType
import net.osmand.plus.plugins.astro.StarWatcherSettings.SkyObjectConfig
import net.osmand.plus.plugins.astro.views.CelestialPathView
import net.osmand.plus.plugins.astro.views.DateTimeSelectionView
import net.osmand.plus.plugins.astro.views.SkyObject
import net.osmand.plus.plugins.astro.views.StarAltitudeChartView
import net.osmand.plus.plugins.astro.views.StarChartView
import net.osmand.plus.plugins.astro.views.StarView
import net.osmand.plus.plugins.astro.views.StarVisiblityChartView
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.views.controls.maphudbuttons.MapButton
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class StarMapFragment : BaseFullScreenFragment(), IMapLocationListener {

	private lateinit var starView: StarView
	private lateinit var timeSelectionView: DateTimeSelectionView
	private lateinit var bottomSheet: View
	private lateinit var sheetTitle: TextView
	private lateinit var sheetCoords: TextView
	private lateinit var sheetDetails: TextView
	private lateinit var resetTimeButton: Button

	private lateinit var starChartsView: View
	private lateinit var starVisiblityView: StarVisiblityChartView
	private lateinit var starAltitudeView: StarAltitudeChartView
	private lateinit var celestialPathView: CelestialPathView
	private lateinit var starChartState: StarChartState

	private val mapButtons = mutableListOf<MapButton>()
	private var rulerWidget: RulerWidget? = null
	private var systemBottomInset: Int = 0

	private lateinit var starMapViewModel: StarObjectsViewModel
	private lateinit var starChartViewModel: StarObjectsViewModel
	private var selectedObject: SkyObject? = null
	private val swSettings: StarWatcherSettings by lazy {
		PluginsHelper.requirePlugin(StarWatcherPlugin::class.java).swSettings
	}

	companion object {
		val TAG: String = StarMapFragment::class.java.simpleName

		fun showInstance(manager: FragmentManager) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				manager.beginTransaction()
					.replace(R.id.fragmentContainer, StarMapFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss()
			}
		}
	}

	override fun getStatusBarColorId(): Int = ColorUtilities.getStatusBarSecondaryColorId(nightMode)

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val view = themedInflater.inflate(R.layout.fragment_star_map, container, false)

		val app = requireActivity().application as OsmandApplication
		starMapViewModel = ViewModelProvider(
			this, StarMapObjectsViewModel.Factory(app, swSettings))[StarMapObjectsViewModel::class.java]
		starChartViewModel = ViewModelProvider(
			this, StarChartObjectsViewModel.Factory(app, swSettings))[StarChartObjectsViewModel::class.java]

		starView = view.findViewById(R.id.star_view)
		timeSelectionView = view.findViewById(R.id.time_selection_view)
		resetTimeButton = view.findViewById(R.id.reset_time_button)
		bottomSheet = view.findViewById(R.id.bottom_sheet)
		sheetTitle = view.findViewById(R.id.sheet_title)
		sheetCoords = view.findViewById(R.id.sheet_coords)
		sheetDetails = view.findViewById(R.id.sheet_details)
		ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { v, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.updatePadding(bottom = v.paddingTop + insets.bottom)
			windowInsets
		}
		val mapControlsContainer = view.findViewById<View>(R.id.map_controls_container)
		ViewCompat.setOnApplyWindowInsetsListener(mapControlsContainer) { v, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			systemBottomInset = insets.bottom
			updateMapControlsPadding()
			windowInsets
		}

		starChartsView = view.findViewById(R.id.star_charts_view)
		ViewCompat.setOnApplyWindowInsetsListener(starChartsView) { v, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.updatePadding(bottom = insets.bottom)
			windowInsets
		}
		starVisiblityView = view.findViewById(R.id.star_visiblity_view)
		starAltitudeView = view.findViewById(R.id.star_altitude_view)
		celestialPathView = view.findViewById(R.id.celestial_path_view)
		starChartState = StarChartState(app)

		view.findViewById<AppCompatImageView>(R.id.chart_settings_button).apply {
			setOnClickListener {
				StarChartView.showFilterDialog(context, starChartViewModel) { updateStarChart() }
			}
		}
		view.findViewById<AppCompatImageView>(R.id.switch_chart_button).apply {
			setOnClickListener { starChartState.changeToNextState(); updateStarChart() }
		}
		view.findViewById<ImageButton>(R.id.star_map_button).apply {
			setOnClickListener {
				updateStarMapVisibility(!starView.isVisible)
				saveCommonSettings()
			}
		}
		view.findViewById<ImageButton>(R.id.star_chart_button).apply {
			setOnClickListener {
				if (starChartsView.isVisible) {
					updateStarChartVisibility(false)
				} else {
					updateStarChartVisibility(true)
					updateStarChart()
				}
				saveCommonSettings()
			}
		}
		view.findViewById<ImageButton>(R.id.settings_button).apply {
			setOnClickListener { showFilterDialog() }
		}

		resetTimeButton.setOnClickListener {
			starMapViewModel.resetTime()
			starChartViewModel.resetTime()
			resetTimeButton.visibility = View.GONE
		}

		// Apply initial settings to View
		swSettings.getCommonConfig().let { config ->
			updateStarMapVisibility(config.showStarMap)
			updateStarChartVisibility(config.showStarChart)
		}
		swSettings.getStarMapConfig().let { config ->
			starView.showAzimuthalGrid = config.showAzimuthalGrid
			starView.showEquatorialGrid = config.showEquatorialGrid
			starView.showEclipticLine = config.showEclipticLine
		}

		updateStarMap()
		setupToolBar(view)
		buildZoomButtons(view)

		return view
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		setupObservers()
		setupListeners()
	}

	override fun onResume() {
		super.onResume()
		app.osmandMap.mapView.addMapLocationListener(this)
		val mapActivity = requireMapActivity()
		mapActivity.disableDrawer()
		updateWidgetsVisibility(mapActivity, View.GONE)
		mapActivity.refreshMap()
	}

	override fun onPause() {
		super.onPause()
		app.osmandMap.mapView.removeMapLocationListener(this)
		val mapActivity = requireMapActivity()
		mapActivity.enableDrawer()
		updateWidgetsVisibility(mapActivity, View.VISIBLE)
		mapActivity.refreshMap()
	}

	override fun locationChanged(p0: Double, p1: Double, p2: Any?) {
		app.runInUIThread { updateStarMap(); updateStarChart() }
	}

	private fun updateStarMapVisibility(visible: Boolean) {
		starView.visibility = if (visible) View.VISIBLE else View.GONE
	}

	private fun updateStarChartVisibility(visible: Boolean) {
		starChartsView.visibility = if (visible) View.VISIBLE else View.GONE
		rulerWidget?.visibility = if (visible) View.VISIBLE else View.GONE
		updateMapControlsPadding()
	}

	private fun updateMapControlsPadding() {
		val mapControls = view?.findViewById<View>(R.id.map_controls_container) ?: return
		if (starChartsView.isVisible) {
			mapControls.updatePadding(bottom = 0)
		} else {
			mapControls.updatePadding(bottom = systemBottomInset)
		}
	}

	private fun saveCommonSettings() {
		val config = StarWatcherSettings.CommonConfig(
			showStarMap = starView.isVisible,
			showStarChart = starChartsView.isVisible,
		)
		swSettings.setCommonConfig(config)
	}

	private fun updateWidgetsVisibility(activity: MapActivity, visibility: Int) {
		AndroidUiHelper.setVisibility(activity, visibility, R.id.map_left_widgets_panel,
			R.id.map_right_widgets_panel, R.id.map_center_info)
	}

	private fun setupToolBar(view: View) {
		val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
		toolbar.setTitleTextColor(app.getColor(ColorUtilities.getPrimaryTextColorId(nightMode)))
		toolbar.setNavigationIcon(getIcon(R.drawable.ic_arrow_back,
			ColorUtilities.getPrimaryIconColorId(nightMode)))
		toolbar.setNavigationContentDescription(R.string.shared_string_close)
		toolbar.setNavigationOnClickListener { _: View? ->
			requireActivity().onBackPressed()
		}
		toolbar.setBackgroundColor(app.getColor(if (nightMode) R.color.activity_background_color_dark else R.color.list_background_color_light))
	}

	private fun setupObservers() {
		// Observe Time changes
		starMapViewModel.currentTime.observe(viewLifecycleOwner) { time ->
			// Pass true for animate if it's a user interaction
			starView.setDateTime(time, animate = true)
			// Update Bottom Sheet if something is selected
			if (selectedObject != null) {
				showObjectInfo(selectedObject!!)
			}
		}
		// Observe Calendar changes to update UI controls
		starMapViewModel.currentCalendar.observe(viewLifecycleOwner) { calendar ->
			timeSelectionView.setDateTime(calendar)
		}
		starChartViewModel.currentTime.observe(viewLifecycleOwner) { _ ->
			updateStarChart()
		}

		// Observe Sky Objects
		starMapViewModel.skyObjects.observe(viewLifecycleOwner) { objects ->
			starView.setSkyObjects(objects)
		}
		starChartViewModel.skyObjects.observe(viewLifecycleOwner) { objects ->
			starVisiblityView.setChartObjects(objects)
			starAltitudeView.setChartObjects(objects)
			celestialPathView.setChartObjects(objects)
		}
	}

	private fun setupListeners() {
		timeSelectionView.setOnDateTimeChangeListener { calendar ->
			starMapViewModel.updateTime(calendar)
			starChartViewModel.updateTime(calendar)
			resetTimeButton.visibility = View.VISIBLE
		}

		starView.setOnObjectClickListener { obj ->
			selectedObject = obj
			if (obj != null) {
				showObjectInfo(obj)
			} else {
				bottomSheet.visibility = View.GONE
			}
		}

		starView.onAnimationFinished = {
			if (selectedObject != null) {
				showObjectInfo(selectedObject!!)
			}
		}
	}

	private fun updateStarMap() {
		val tileBox = app.osmandMap.mapView.rotatedTileBox
		val location = tileBox.centerLatLon
		starView.setObserverLocation(location.latitude, location.longitude, 0.0)
		starView.setAzimuth(-tileBox.rotate.toDouble())
	}

	private fun updateStarChart() {
		// Get current map center
		val location = app.osmandMap.mapView.currentRotatedTileBox.centerLatLon

		// Update visibility and push data to the active view
		val chartType = starChartState.getStarChartType()
		val calendar = starChartViewModel.currentCalendar.value
		val zoneId: ZoneId
		val localDate: LocalDate
		if (calendar != null) {
			zoneId = calendar.timeZone.toZoneId()
			localDate = calendar.toInstant().atZone(zoneId).toLocalDate()
		} else {
			zoneId = TimeZone.getDefault().toZoneId()
			localDate = LocalDate.now()
		}
		when (chartType) {
			StarChartType.STAR_VISIBLITY -> {
				starVisiblityView.visibility = View.VISIBLE
				starAltitudeView.visibility = View.GONE
				celestialPathView.visibility = View.GONE
				starVisiblityView.updateData(location.latitude, location.longitude, localDate)
			}
			StarChartType.STAR_ALTITUDE -> {
				starVisiblityView.visibility = View.GONE
				starAltitudeView.visibility = View.VISIBLE
				celestialPathView.visibility = View.GONE
				starAltitudeView.updateData(location.latitude, location.longitude, localDate)
			}
			StarChartType.CELESTIAL_PATH -> {
				starVisiblityView.visibility = View.GONE
				starAltitudeView.visibility = View.GONE
				celestialPathView.visibility = View.VISIBLE
				celestialPathView.updateData(location.latitude, location.longitude, localDate)
				starChartViewModel.currentTime.value?.let {
					celestialPathView.setMoment(it.toZoned(zoneId))
				}
			}
		}
	}

	private fun buildZoomButtons(view: View) {
		val activity = requireMapActivity()
		val mapLayers = activity.mapLayers
		val layer = mapLayers.mapControlsLayer

		view.findViewById<MapButton?>(R.id.map_zoom_in_button)?.let {
			mapButtons.add(it)
		}
		view.findViewById<MapButton?>(R.id.map_zoom_out_button)?.let {
			mapButtons.add(it)
		}
		view.findViewById<MapButton?>(R.id.map_my_location_button)?.let {
			mapButtons.add(it)
		}
		layer.addCustomizedDefaultMapButtons(mapButtons)
		view.findViewById<View?>(R.id.map_hud_controls)?.let {
			AndroidUiHelper.updateVisibility(it, true)
		}

		val mapInfoLayer = mapLayers.mapInfoLayer
		rulerWidget = mapInfoLayer.setupRulerWidget(view.findViewById(R.id.map_ruler_layout))

		view.findViewById<MapButton?>(R.id.map_compass_button)?.let {
			layer.addCustomMapButton(it)
			mapButtons.add(it)
		}
	}

	private fun showObjectInfo(obj: SkyObject) {
		sheetTitle.text = obj.name
		val az = String.format(Locale.getDefault(), "%.1f°", obj.azimuth)
		val alt = String.format(Locale.getDefault(), "%.1f°", obj.altitude)
		sheetCoords.text = "${getString(R.string.shared_string_azimuth)}: $az  |  ${getString(R.string.altitude)}: $alt"

		var details = "${getString(R.string.shared_string_magnitude)}: ${obj.magnitude}"
		if (obj.type != SkyObject.Type.STAR) {
			details += "\n${getString(R.string.distance)}: %.3f AU".format(obj.distAu)
		}

		// Rise / Set calculation
		val observer = starView.observer
		val currentTime = starView.currentTime

		val bodyToCheck: Body? = if (obj.type == SkyObject.Type.STAR) {
			defineStar(Body.Star2, obj.ra, obj.dec, 1000.0)
			Body.Star2
		} else {
			obj.body
		}

		if (bodyToCheck != null) {
			val riseTime = searchRiseSet(bodyToCheck, observer, Direction.Rise, currentTime, 1.0)
			val setTime = searchRiseSet(bodyToCheck, observer, Direction.Set, currentTime, 1.0)

			if (riseTime != null) {
				details += "\n${getString(R.string.astro_rise)}: ↑${formatLocalTime(riseTime)}"
			}
			if (setTime != null) {
				details += "\n${getString(R.string.astro_set)}: ↓${formatLocalTime(setTime)}"
			}
		}

		sheetDetails.text = details
		bottomSheet.visibility = View.VISIBLE
	}

	private fun formatLocalTime(astronomyTime: Time): String {
		val calendar = Calendar.getInstance(TimeZone.getDefault())
		calendar.timeInMillis = astronomyTime.toMillisecondsSince1970()
		return String.format(Locale.getDefault(), "%02d:%02d",
			calendar.get(Calendar.HOUR_OF_DAY),
			calendar.get(Calendar.MINUTE))
	}

	private fun showFilterDialog() {
		val toggleItems = arrayOf(getString(R.string.azimuthal_grid), getString(R.string.equatorial_grid), getString(R.string.ecliptic_line))

		var tempAzimuthal = starView.showAzimuthalGrid
		var tempEquatorial = starView.showEquatorialGrid
		var tempEcliptic = starView.showEclipticLine

		val toggleChecked = booleanArrayOf(tempAzimuthal, tempEquatorial, tempEcliptic)

		val currentObjects = starMapViewModel.skyObjects.value ?: emptyList()
		val objectNames = currentObjects.map { it.name }.toTypedArray()
		val objectChecked = currentObjects.map { it.isVisible }.toBooleanArray()

		val allItems = toggleItems + objectNames
		val allChecked = toggleChecked + objectChecked

		AlertDialog.Builder(requireContext())
			.setTitle(R.string.visible_layers_and_objects)
			.setMultiChoiceItems(allItems, allChecked) { _, which, isChecked ->
				if (which < toggleItems.size) {
					when (which) {
						0 -> tempAzimuthal = isChecked
						1 -> tempEquatorial = isChecked
						2 -> tempEcliptic = isChecked
					}
				} else {
					val objIndex = which - toggleItems.size
					if (objIndex in objectChecked.indices) {
						objectChecked[objIndex] = isChecked
					}
				}
			}
			.setPositiveButton(R.string.shared_string_apply) { _, _ ->
				starView.showAzimuthalGrid = tempAzimuthal
				starView.showEquatorialGrid = tempEquatorial
				starView.showEclipticLine = tempEcliptic

				currentObjects.forEachIndexed { index, skyObject ->
					skyObject.isVisible = objectChecked[index]
				}
				starView.updateVisibility()

				val itemsConfig = currentObjects.map { SkyObjectConfig(it.id, it.isVisible) }
				val config = StarWatcherSettings.StarMapConfig(
					showAzimuthalGrid = tempAzimuthal,
					showEquatorialGrid = tempEquatorial,
					showEclipticLine = tempEcliptic,
					items = itemsConfig
				)
				swSettings.setStarMapConfig(config)
			}
			.setNegativeButton(R.string.shared_string_cancel, null)
			.show()
	}
}