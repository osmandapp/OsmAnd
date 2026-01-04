package net.osmand.plus.plugins.astro

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.toColorInt
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
import net.osmand.Location
import net.osmand.map.IMapLocationListener
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseFullScreenFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.astro.AstroDataProvider.Constellation
import net.osmand.plus.plugins.astro.StarChartState.StarChartType
import net.osmand.plus.plugins.astro.utils.AstroUtils
import net.osmand.plus.plugins.astro.utils.StarMapARModeHelper
import net.osmand.plus.plugins.astro.utils.StarMapCameraHelper
import net.osmand.plus.plugins.astro.views.DateTimeSelectionView
import net.osmand.plus.plugins.astro.views.StarAltitudeChartView
import net.osmand.plus.plugins.astro.views.StarChartView
import net.osmand.plus.plugins.astro.views.StarCompassButton
import net.osmand.plus.plugins.astro.views.StarView
import net.osmand.plus.plugins.astro.views.StarVisiblityChartView
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.views.controls.maphudbuttons.MapButton
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget
import net.osmand.shared.util.LoggerFactory
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

class StarMapFragment : BaseFullScreenFragment(), IMapLocationListener, OsmAndLocationListener,
	OsmAndCompassListener {

	private lateinit var starView: StarView
	private lateinit var timeSelectionView: DateTimeSelectionView
	private lateinit var bottomSheet: View
	private lateinit var sheetTitle: TextView
	private lateinit var sheetCoords: TextView
	private lateinit var resetTimeButton: Button

	private lateinit var sheetPinButton: CheckBox

	private var sheetMagnitude: TextView? = null
	private var sheetDistance: TextView? = null
	private var sheetRiseTime: TextView? = null
	private var sheetSetTime: TextView? = null
	private var sheetWikiButton: View? = null

	private lateinit var arModeButton: ImageButton
	private lateinit var cameraButton: ImageButton
	private lateinit var transparencySlider: SeekBar
	private lateinit var sliderContainer: View
	private lateinit var resetFovButton: View
	private lateinit var mode2dButton: ImageButton
	
	private lateinit var magnitudeSlider: SeekBar
	private lateinit var magnitudeValueText: TextView
	private lateinit var magnitudeSliderContainer: View
	private lateinit var resetMagnitudeButton: ImageButton

	private lateinit var starChartsView: View
	private lateinit var starVisiblityView: StarVisiblityChartView
	private lateinit var starAltitudeView: StarAltitudeChartView
	private lateinit var starChartState: StarChartState

	private val mapButtons = mutableListOf<MapButton>()
	private var compassButton: StarCompassButton? = null
	private var rulerWidget: RulerWidget? = null
	private var systemBottomInset: Int = 0
	private var manualAzimuth: Boolean = false
	private var lastResetRotationToNorth = 0L

	private lateinit var starMapViewModel: StarObjectsViewModel
	private lateinit var starChartViewModel: StarObjectsViewModel
	private var selectedObject: SkyObject? = null
	private val swSettings: StarWatcherSettings by lazy {
		PluginsHelper.requirePlugin(StarWatcherPlugin::class.java).swSettings
	}

	private lateinit var arModeHelper: StarMapARModeHelper
	private lateinit var cameraHelper: StarMapCameraHelper

	private var previousAltitude: Double = 45.0
	private var previousAzimuth: Double = 0.0
	private var previousViewAngle: Double = 150.0
	private var showMagnitudeFilter = false

	companion object {
		private val log = LoggerFactory.getLogger("StarMapFragment")
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

		sheetPinButton = view.findViewById(R.id.sheet_pin_button)

		// Attempt to find new structured views
		sheetMagnitude = view.findViewById(R.id.sheet_magnitude)
		sheetDistance = view.findViewById(R.id.sheet_distance)
		sheetRiseTime = view.findViewById(R.id.sheet_rise_time)
		sheetSetTime = view.findViewById(R.id.sheet_set_time)
		sheetWikiButton = view.findViewById(R.id.sheet_wiki_button)

		arModeButton = view.findViewById(R.id.ar_mode_button)
		cameraButton = view.findViewById(R.id.camera_button)
		transparencySlider = view.findViewById(R.id.transparency_slider)
		sliderContainer = view.findViewById(R.id.slider_container)
		resetFovButton = view.findViewById(R.id.reset_fov_button)
		mode2dButton = view.findViewById(R.id.mode2d_button)
		
		magnitudeSlider = view.findViewById(R.id.magnitude_slider)
		magnitudeValueText = view.findViewById(R.id.magnitude_value_text)
		magnitudeSliderContainer = view.findViewById(R.id.magnitude_slider_container)
		resetMagnitudeButton = view.findViewById(R.id.reset_magnitude_button)

		arModeHelper = StarMapARModeHelper(requireContext(), starView) { enabled ->
			updateArModeUI(enabled)
			if (!enabled) manualAzimuth = true
		}

		cameraHelper = StarMapCameraHelper(this, starView, view.findViewById(R.id.camera_view)) { enabled ->
			updateCameraUI(enabled)
			if (enabled && !arModeHelper.isArModeEnabled) arModeHelper.toggleArMode(true)
		}

		arModeButton.setOnClickListener { arModeHelper.toggleArMode() }
		cameraButton.setOnClickListener { cameraHelper.toggleCameraOverlay() }
		resetFovButton.setOnClickListener { cameraHelper.resetFov() }

		transparencySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				cameraHelper.setTransparency(progress)
			}
			override fun onStartTrackingTouch(seekBar: SeekBar?) {}
			override fun onStopTrackingTouch(seekBar: SeekBar?) {}
		})

		magnitudeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				val magnitude = progress / 10.0 - 1.0
				magnitudeValueText.text = String.format(Locale.getDefault(), "%.1f", magnitude)
				if (fromUser) {
					starView.magnitudeFilter = magnitude
					starView.invalidate()
				}
			}
			override fun onStartTrackingTouch(seekBar: SeekBar?) {}
			override fun onStopTrackingTouch(seekBar: SeekBar?) {}
		})

		resetMagnitudeButton.setOnClickListener {
			val objects = starMapViewModel.skyObjects.value ?: emptyList()
			if (objects.isNotEmpty()) {
				val maxMag = calculateMaxMagnitude(objects).toDouble()
				magnitudeSlider.progress = ((maxMag + 1.0) * 10.0).toInt()
				starView.magnitudeFilter = null
				starView.invalidate()
			}
		}

		updateArModeUI(arModeHelper.isArModeEnabled)
		updateCameraUI(cameraHelper.isCameraOverlayEnabled)

		ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { v, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.updatePadding(bottom = v.paddingTop + insets.bottom)
			windowInsets
		}

		val starMapControlsContainer = view.findViewById<View>(R.id.star_map_controls_container)
		val mapControlsContainer = view.findViewById<View>(R.id.map_controls_container)

		val insetsListener = androidx.core.view.OnApplyWindowInsetsListener { _, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			systemBottomInset = insets.bottom
			updateMapControlsPadding()
			windowInsets
		}

		ViewCompat.setOnApplyWindowInsetsListener(starMapControlsContainer, insetsListener)
		if (mapControlsContainer != null) {
			ViewCompat.setOnApplyWindowInsetsListener(mapControlsContainer, insetsListener)
		}

		starChartsView = view.findViewById(R.id.star_charts_view)
		ViewCompat.setOnApplyWindowInsetsListener(starChartsView) { v, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.updatePadding(bottom = insets.bottom)
			windowInsets
		}
		starVisiblityView = view.findViewById(R.id.star_visiblity_view)
		starAltitudeView = view.findViewById(R.id.star_altitude_view)
		starChartState = StarChartState(app)

		view.findViewById<AppCompatImageView>(R.id.chart_settings_button).apply {
			setOnClickListener { StarChartView.showFilterDialog(context, starChartViewModel) { updateStarChart() } }
		}
		view.findViewById<AppCompatImageView>(R.id.switch_chart_button).apply {
			setOnClickListener { starChartState.changeToNextState(); updateStarChart() }
		}
		view.findViewById<ImageButton>(R.id.star_map_button).apply {
			setOnClickListener { updateStarMapVisibility(!starView.isVisible); saveCommonSettings() }
		}
		view.findViewById<ImageButton>(R.id.star_chart_button).apply {
			setOnClickListener {
				if (starChartsView.isVisible) updateStarChartVisibility(false)
				else { updateStarChartVisibility(true); updateStarChart() }
				saveCommonSettings()
			}
		}
		view.findViewById<ImageButton>(R.id.settings_button).apply {
			setOnClickListener {
				AstroUtils.showStarMapOptionsDialog(context, starView, swSettings) {
					showMagnitudeFilter = it.showMagnitudeFilter
					updateMagnitudeFilterVisibility()
				}
			}
		}
		mode2dButton.setOnClickListener { toggle2DMode() }

		resetTimeButton.setOnClickListener {
			starMapViewModel.resetTime(); starChartViewModel.resetTime()
			resetTimeButton.visibility = View.GONE
		}

		swSettings.getCommonConfig().let { config ->
			updateStarMapVisibility(config.showStarMap)
			updateStarChartVisibility(config.showStarChart)
		}
		swSettings.getStarMapConfig().let { config ->
			starView.showAzimuthalGrid = config.showAzimuthalGrid
			starView.showEquatorialGrid = config.showEquatorialGrid
			starView.showEclipticLine = config.showEclipticLine
			starView.showConstellations = config.showConstellations
			starView.showStars = config.showStars
			starView.showGalaxies = config.showGalaxies
			starView.showBlackHoles = config.showBlackHoles
			starView.showSun = config.showSun
			starView.showMoon = config.showMoon
			starView.showPlanets = config.showPlanets
			showMagnitudeFilter = config.showMagnitudeFilter
			starView.magnitudeFilter = config.magnitudeFilter
			starView.is2DMode = config.is2DMode
			if (config.magnitudeFilter != null) {
				magnitudeValueText.text = String.format(Locale.getDefault(), "%.1f", config.magnitudeFilter)
			}
		}
		starView.setConstellations(AstroDataProvider.getConstellations(view.context))

		updateMagnitudeFilterVisibility()
		updateStarMap(true)
		setupToolBar(view)
		buildZoomButtons(view)

		previousAltitude = starView.getAltitude()
		previousAzimuth = starView.getAzimuth()
		previousViewAngle = starView.getViewAngle()
		apply2DMode(starView.is2DMode)

		return view
	}

	private fun updateArModeUI(enabled: Boolean) {
		if (enabled) arModeButton.setColorFilter(Color.BLUE)
		else arModeButton.setColorFilter("#5f6e7c".toColorInt())
	}

	private fun updateCameraUI(enabled: Boolean) {
		if (enabled) {
			cameraButton.setColorFilter(Color.BLUE)
			sliderContainer.visibility = View.VISIBLE
			resetFovButton.visibility = View.VISIBLE
		} else {
			cameraButton.setColorFilter("#5f6e7c".toColorInt())
			sliderContainer.visibility = View.GONE
			resetFovButton.visibility = View.GONE
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		setupObservers()
		setupListeners()
	}

	override fun onResume() {
		super.onResume()
		updateStarMap(true)
		app.locationProvider.addLocationListener(this)
		app.locationProvider.addCompassListener(this)
		app.osmandMap.mapView.addMapLocationListener(this)
		arModeHelper.onResume(); cameraHelper.onResume()
		val mapActivity = requireMapActivity()
		mapActivity.disableDrawer()
		updateWidgetsVisibility(mapActivity, View.GONE)
		mapActivity.refreshMap()
	}

	override fun onPause() {
		super.onPause()
		saveStarMapSettings()
		app.locationProvider.removeLocationListener(this)
		app.locationProvider.removeCompassListener(this)
		app.osmandMap.mapView.removeMapLocationListener(this)
		arModeHelper.onPause(); cameraHelper.onPause()
		val mapActivity = requireMapActivity()
		mapActivity.enableDrawer()
		updateWidgetsVisibility(mapActivity, View.VISIBLE)
		mapActivity.refreshMap()
	}

	override fun updateCompassValue(value: Float) {
		if (arModeHelper.isArModeEnabled || starView.is2DMode) return
		val lastResetRotationToNorth = app.mapViewTrackingUtilities.lastResetRotationToNorth
		if (this.lastResetRotationToNorth < lastResetRotationToNorth) {
			this.lastResetRotationToNorth = lastResetRotationToNorth
			manualAzimuth = false
		}
		if (manualAzimuth) return
		val rotateMode = settings.ROTATE_MAP.get()
		if (rotateMode == OsmandSettings.ROTATE_MAP_COMPASS) {
			setAzimuth(value.toDouble())
		} else if (rotateMode != OsmandSettings.ROTATE_MAP_BEARING) {
			setAzimuth(-app.osmandMap.mapView.rotate.toDouble())
		}
	}

	private fun setAzimuth(azimuth: Double, animate: Boolean = false) {
		starView.setAzimuth(azimuth, animate)
		compassButton?.update(-azimuth.toFloat(), animate)
	}

	override fun updateLocation(location: Location?) {
		if (location == null) return
		arModeHelper.updateGeomagneticField(location)
		if (app.mapViewTrackingUtilities.isMapLinkedToLocation) {
			app.runInUIThread {
				if (!manualAzimuth && !arModeHelper.isArModeEnabled) {
					if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING) {
						if (location.hasBearing() && location.bearing != 0f) {
							setAzimuth(location.bearing.toDouble(), true)
						}
					}
				}
				if (starView.is2DMode) {
					starView.setCenter(180.0, 90.0)
				}
				updateStarMap(); updateStarChart()
			}
		} else if (!manualAzimuth && !arModeHelper.isArModeEnabled) {
			app.runInUIThread {
				if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING) {
					if (location.hasBearing() && location.bearing != 0f) {
						setAzimuth(location.bearing.toDouble(), true)
					}
				}
			}
		}
	}

	override fun locationChanged(p0: Double, p1: Double, p2: Any?) {
		if (!app.mapViewTrackingUtilities.isMapLinkedToLocation) {
			app.runInUIThread { updateStarMap(); updateStarChart() }
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		cameraHelper.onRequestPermissionsResult(requestCode, grantResults)
	}

	private fun updateStarMapVisibility(visible: Boolean) {
		starView.visibility = if (visible) View.VISIBLE else View.GONE
		val starMapControls = view?.findViewById<View>(R.id.star_map_controls_container)
		val mapControls = view?.findViewById<View>(R.id.map_controls_container)
		starMapControls?.visibility = if (visible) View.VISIBLE else View.GONE
		mapControls?.visibility = if (!visible) View.VISIBLE else View.GONE
		updateMagnitudeFilterVisibility()
	}

	private fun updateStarChartVisibility(visible: Boolean) {
		starChartsView.visibility = if (visible) View.VISIBLE else View.GONE
		rulerWidget?.visibility = if (visible) View.VISIBLE else View.GONE
		updateMapControlsPadding()
	}

	private fun updateMapControlsPadding() {
		val starMapControls = view?.findViewById<View>(R.id.star_map_controls_container)
		val mapControls = view?.findViewById<View>(R.id.map_controls_container)
		val bottomPadding = if (starChartsView.isVisible) 0 else systemBottomInset
		starMapControls?.updatePadding(bottom = bottomPadding)
		mapControls?.updatePadding(bottom = bottomPadding)
	}

	private fun saveCommonSettings() {
		val config = StarWatcherSettings.CommonConfig(
			showStarMap = starView.isVisible,
			showStarChart = starChartsView.isVisible,
		)
		swSettings.setCommonConfig(config)
	}

	private fun saveStarMapSettings() {
		val current = swSettings.getStarMapConfig()
		val config = current.copy(
			showAzimuthalGrid = starView.showAzimuthalGrid,
			showEquatorialGrid = starView.showEquatorialGrid,
			showEclipticLine = starView.showEclipticLine,
			showSun = starView.showSun,
			showMoon = starView.showMoon,
			showPlanets = starView.showPlanets,
			showConstellations = starView.showConstellations,
			showStars = starView.showStars,
			showGalaxies = starView.showGalaxies,
			showBlackHoles = starView.showBlackHoles,
			is2DMode = starView.is2DMode,
			magnitudeFilter = starView.magnitudeFilter
		)
		swSettings.setStarMapConfig(config)
	}

	private fun updateMagnitudeFilterVisibility() {
		val visible = starView.showStars && starView.isVisible && showMagnitudeFilter
		magnitudeSliderContainer.visibility = if (visible) View.VISIBLE else View.GONE
		magnitudeValueText.visibility = if (visible) View.VISIBLE else View.GONE
		resetMagnitudeButton.visibility = if (visible) View.VISIBLE else View.GONE
	}

	private fun updateWidgetsVisibility(activity: MapActivity, visibility: Int) {
		AndroidUiHelper.setVisibility(activity, visibility, R.id.map_left_widgets_panel,
			R.id.map_right_widgets_panel, R.id.map_center_info)
	}

	private fun setupToolBar(view: View) {
		val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
		toolbar.setTitleTextColor(app.getColor(ColorUtilities.getPrimaryTextColorId(nightMode)))
		toolbar.setNavigationIcon(getIcon(R.drawable.ic_arrow_back, ColorUtilities.getPrimaryIconColorId(nightMode)))
		toolbar.setNavigationContentDescription(R.string.shared_string_close)
		toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
		toolbar.setBackgroundColor(app.getColor(if (nightMode) R.color.activity_background_color_dark else R.color.list_background_color_light))
	}

	private fun toggle2DMode() {
		apply2DMode(!starView.is2DMode)
	}

	private fun apply2DMode(is2D: Boolean) {
		if (is2D) {
			previousAltitude = starView.getAltitude()
			previousAzimuth = starView.getAzimuth()
			previousViewAngle = starView.getViewAngle()
			starView.is2DMode = true
			starView.setCenter(180.0, 90.0)
			if (arModeHelper.isArModeEnabled) arModeHelper.toggleArMode(false)
			manualAzimuth = true
		} else {
			starView.is2DMode = false
			starView.setCenter(previousAzimuth, previousAltitude)
			starView.setViewAngle(previousViewAngle)
		}
		starView.invalidate()
		update2DModeIcon()
	}

	private fun update2DModeIcon() {
		val iconId = if (starView.is2DMode) R.drawable.ic_action_3d else R.drawable.ic_action_2d
		mode2dButton.setImageDrawable(getIcon(iconId, ColorUtilities.getPrimaryIconColorId(nightMode)))
	}

	private fun setupObservers() {
		starMapViewModel.currentTime.observe(viewLifecycleOwner) { time ->
			starView.setDateTime(time, animate = true)
			if (selectedObject != null) showObjectInfo(selectedObject!!)
		}
		starMapViewModel.currentCalendar.observe(viewLifecycleOwner) { calendar ->
			timeSelectionView.setDateTime(calendar)
		}
		starChartViewModel.currentTime.observe(viewLifecycleOwner) { updateStarChart() }
		starMapViewModel.skyObjects.observe(viewLifecycleOwner) { objects ->
			starView.setSkyObjects(objects)
			if (objects.isNotEmpty()) {
				val maxMag = calculateMaxMagnitude(objects).toDouble()
				val maxSliderVal = ((maxMag + 1.0) * 10.0).toInt()
				magnitudeSlider.max = maxSliderVal

				val currentFilter = starView.magnitudeFilter
				if (currentFilter == null || currentFilter > maxMag) {
					starView.magnitudeFilter = maxMag
				}

				val filterToUse = starView.magnitudeFilter ?: maxMag
				magnitudeSlider.progress = ((filterToUse + 1.0) * 10.0).toInt()
				magnitudeValueText.text = String.format(Locale.getDefault(), "%.1f", filterToUse)
			}
		}
		starChartViewModel.skyObjects.observe(viewLifecycleOwner) { objects ->
			starVisiblityView.setChartObjects(objects)
			starAltitudeView.setChartObjects(objects)
		}
	}

	private fun calculateMaxMagnitude(objects: List<SkyObject>): Float {
		return ceil(objects.filter { it.type == SkyObject.Type.STAR }.maxOfOrNull { it.magnitude } ?: 10.0f)
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
			} else if (starView.getSelectedConstellationItem() == null) {
				bottomSheet.visibility = View.GONE
			}
		}
		starView.onConstellationClickListener = { constellation ->
			if (constellation != null) {
				showConstellationInfo(constellation)
			} else if (selectedObject == null) {
				bottomSheet.visibility = View.GONE
			}
		}
		starView.onAnimationFinished = { if (selectedObject != null) showObjectInfo(selectedObject!!) }
		starView.onAzimuthManualChangeListener = { azimuth ->
			if (arModeHelper.isArModeEnabled) arModeHelper.toggleArMode()
			manualAzimuth = true
			compassButton?.update(-azimuth.toFloat())
		}
		starView.onViewAngleChangeListener = { fov -> cameraHelper.updateCameraZoom(fov) }
	}

	private fun updateStarMap(updateAzimuth: Boolean = false) {
		val tileBox = app.osmandMap.mapView.rotatedTileBox
		val location = tileBox.centerLatLon
		starView.setObserverLocation(location.latitude, location.longitude, 0.0)
		if (updateAzimuth && !arModeHelper.isArModeEnabled && !starView.is2DMode) {
			setAzimuth(-tileBox.rotate.toDouble())
		}
	}

	private fun updateStarChart() {
		val location = app.osmandMap.mapView.currentRotatedTileBox.centerLatLon
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
				starVisiblityView.visibility = View.VISIBLE; starAltitudeView.visibility = View.GONE
				starVisiblityView.updateData(location.latitude, location.longitude, localDate)
			}
			StarChartType.STAR_ALTITUDE -> {
				starVisiblityView.visibility = View.GONE; starAltitudeView.visibility = View.VISIBLE
				starAltitudeView.updateData(location.latitude, location.longitude, localDate)
			}
		}
	}

	private fun buildZoomButtons(view: View) {
		val activity = requireMapActivity()
		val mapLayers = activity.mapLayers
		val layer = mapLayers.mapControlsLayer
		fun addButtons(container: View, starMap: Boolean): Boolean? {
			container.findViewById<MapButton?>(R.id.map_zoom_in_button)?.let {
				mapButtons.add(it)
				if (starMap) { it.setOnClickListener { starView.zoomIn() }; it.setOnLongClickListener(null) }
			}
			container.findViewById<MapButton?>(R.id.map_zoom_out_button)?.let {
				mapButtons.add(it)
				if (starMap) { it.setOnClickListener { starView.zoomOut() }; it.setOnLongClickListener(null) }
			}
			container.findViewById<MapButton?>(R.id.map_my_location_button)?.let { mapButtons.add(it) }
			return container.findViewById<View?>(R.id.map_hud_controls)?.let { AndroidUiHelper.updateVisibility(it, true) }
		}
		view.findViewById<StarCompassButton?>(R.id.star_map_compass_button)?.let {
			it.onSingleTap = { setAzimuth(0.0, true)}
			compassButton = it
			layer.addCustomMapButton(it)
			mapButtons.add(it)
		}
		view.findViewById<View>(R.id.star_map_controls_container)?.let { addButtons(it, true) }
		view.findViewById<View>(R.id.map_controls_container)?.let { addButtons(it, false) }
		layer.addCustomizedDefaultMapButtons(mapButtons)
		val mapInfoLayer = mapLayers.mapInfoLayer
		rulerWidget = mapInfoLayer.setupRulerWidget(view.findViewById(R.id.map_ruler_layout))
	}

	private fun showConstellationInfo(c: Constellation) {
		sheetTitle.text = c.name
		sheetCoords.text = getString(R.string.astro_constellation)

		sheetPinButton.visibility = View.GONE

		sheetMagnitude?.isVisible = false
		sheetDistance?.isVisible = false
		sheetRiseTime?.isVisible = false
		sheetSetTime?.isVisible = false

		if (c.wid.isNotEmpty()) {
			sheetWikiButton?.isVisible = true
			sheetWikiButton?.setOnClickListener {
				val uri = Uri.parse("https://www.wikidata.org/wiki/${c.wid}")
				val intent = Intent(Intent.ACTION_VIEW, uri)
				try {
					startActivity(intent)
				} catch (_: Exception) {
				}
			}
		} else {
			sheetWikiButton?.isVisible = false
		}

		bottomSheet.visibility = View.VISIBLE
	}

	private fun showObjectInfo(obj: SkyObject) {
		sheetTitle.text = obj.name
		val az = String.format(Locale.getDefault(), "%.1f°", obj.azimuth)
		val alt = String.format(Locale.getDefault(), "%.1f°", obj.altitude)
		val coordsText = "${getString(R.string.shared_string_azimuth)}: $az  •  ${getString(R.string.altitude)}: $alt"
		sheetCoords.text = coordsText

		// Show and configure Pin button
		sheetPinButton.visibility = View.VISIBLE
		sheetPinButton.setOnCheckedChangeListener(null) // Prevent recursive trigger
		sheetPinButton.isChecked = starView.isObjectPinned(obj)
		sheetPinButton.setOnCheckedChangeListener { _, isChecked ->
			starView.setObjectPinned(obj, isChecked)
		}

		sheetMagnitude?.text = "${getString(R.string.shared_string_magnitude)}: ${obj.magnitude}"
		sheetMagnitude?.isVisible = true

		if (obj.type.isSunSystem()) {
			sheetDistance?.isVisible = true
			sheetDistance?.text =
				"${getString(R.string.distance)}: %.3f AU".format(Locale.getDefault(), obj.distAu)
		} else {
			sheetDistance?.isVisible = false
		}

		val observer = starView.observer
		val currentTime = starView.currentTime
		val bodyToCheck: Body? = if (!obj.type.isSunSystem()) {
			defineStar(Body.Star2, obj.ra, obj.dec, 1000.0); Body.Star2
		} else obj.body

		if (bodyToCheck != null) {
			val calendar = (starMapViewModel.currentCalendar.value ?: Calendar.getInstance()).clone() as Calendar
			calendar.set(Calendar.HOUR_OF_DAY, 0)
			calendar.set(Calendar.MINUTE, 0)
			calendar.set(Calendar.SECOND, 0)
			calendar.set(Calendar.MILLISECOND, 0)
			val searchStart = Time.fromMillisecondsSince1970(calendar.timeInMillis)

			val riseTime = searchRiseSet(bodyToCheck, observer, Direction.Rise, searchStart, 1.2)
			val setTime = searchRiseSet(bodyToCheck, observer, Direction.Set, searchStart, 1.2)

			if (riseTime != null) {
				sheetRiseTime?.text = "Rise: ↑${AstroUtils.formatLocalTime(riseTime)}"
				sheetRiseTime?.isVisible = true
			} else {
				sheetRiseTime?.isVisible = false
			}

			if (setTime != null) {
				sheetSetTime?.text = "Set: ↓${AstroUtils.formatLocalTime(setTime)}"
				sheetSetTime?.isVisible = true
			} else {
				sheetSetTime?.isVisible = false
			}
		} else {
			sheetRiseTime?.isVisible = false
			sheetSetTime?.isVisible = false
		}

		if (obj.wid.isNotEmpty()) {
			sheetWikiButton?.isVisible = true
			sheetWikiButton?.setOnClickListener {
				val uri = Uri.parse("https://www.wikidata.org/wiki/${obj.wid}")
				val intent = Intent(Intent.ACTION_VIEW, uri)
				try {
					startActivity(intent)
				} catch (_: Exception) {
				}
			}
		} else {
			sheetWikiButton?.isVisible = false
		}

		bottomSheet.visibility = View.VISIBLE
	}
}