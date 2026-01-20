package net.osmand.plus.plugins.astro

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
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
import net.osmand.shared.util.LoggerFactory
import net.osmand.util.MapUtils
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class StarMapFragment : BaseFullScreenFragment(), IMapLocationListener, OsmAndLocationListener,
	OsmAndCompassListener {

	internal lateinit var starView: StarView
	private lateinit var timeSelectionView: DateTimeSelectionView
	private lateinit var timeControlBtn: MaterialButton
	private lateinit var resetTimeButton: ImageButton

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

	private var compassButton: StarCompassButton? = null
	private var systemBottomInset: Int = 0
	private var manualAzimuth: Boolean = true
	private var lastResetRotationToNorth = 0L
	private var lastUpdatedLocation: Location? = null
	private var lastUpdatedAzimuth: Double = -1.0

	internal lateinit var starMapViewModel: StarObjectsViewModel
	private lateinit var starChartViewModel: StarObjectsViewModel
	private var selectedObject: SkyObject? = null

	private val dataProvider: AstroDataProvider by lazy {
		PluginsHelper.requirePlugin(StarWatcherPlugin::class.java).astroDataProvider
	}

	private val swSettings: StarWatcherSettings by lazy {
		PluginsHelper.requirePlugin(StarWatcherPlugin::class.java).swSettings
	}

	private lateinit var arModeHelper: StarMapARModeHelper
	private lateinit var cameraHelper: StarMapCameraHelper
	private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

	private var previousAltitude: Double = 45.0
	private var previousAzimuth: Double = 0.0
	private var previousViewAngle: Double = 150.0
	private var showMagnitudeFilter = false

	companion object {
		val TAG: String = StarMapFragment::class.java.simpleName
		private val LOG = LoggerFactory.getLogger(TAG)

		private const val MAX_MAGNITUDE = 7.0f

		@JvmStatic

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
			this, StarMapObjectsViewModel.Factory(app, swSettings, dataProvider))[StarMapObjectsViewModel::class.java]
		starChartViewModel = ViewModelProvider(
			this, StarChartObjectsViewModel.Factory(app, swSettings, dataProvider))[StarChartObjectsViewModel::class.java]

		starView = view.findViewById(R.id.star_view)
		timeSelectionView = view.findViewById(R.id.time_selection_view)
		timeControlBtn = view.findViewById(R.id.time_control_button)
		resetTimeButton = view.findViewById(R.id.reset_time_button)

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
					starView.magnitudeFilter = magnitude.toFloat()
					starView.invalidate()
				}
			}
			override fun onStartTrackingTouch(seekBar: SeekBar?) {}
			override fun onStopTrackingTouch(seekBar: SeekBar?) {}
		})

		resetMagnitudeButton.setOnClickListener {
			val objects = starMapViewModel.skyObjects.value ?: emptyList()
			if (objects.isNotEmpty()) {
				magnitudeSlider.progress = ((MAX_MAGNITUDE + 1.0) * 10.0).toInt()
				starView.magnitudeFilter = null
				starView.invalidate()
			}
		}

		updateArModeUI(arModeHelper.isArModeEnabled)
		updateCameraUI(cameraHelper.isCameraOverlayEnabled)

		val timeControlCard: View = view.findViewById(R.id.time_control_card)
		applyWindowInsets(timeControlCard)

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
			applyWindowInsets(this)
		}
		view.findViewById<ImageButton>(R.id.star_chart_button).apply {
			setOnClickListener {
				if (starChartsView.isVisible) updateStarChartVisibility(false)
				else { updateStarChartVisibility(true); updateStarChart() }
				saveCommonSettings()
			}
		}
		view.findViewById<ImageButton>(R.id.close_button).apply {
			setOnClickListener { requireActivity().onBackPressed() }
		}
		view.findViewById<ImageButton>(R.id.search_button).apply {
			setOnClickListener {
				val dialog = StarMapSearchDialogFragment()
				dialog.setObjects(getSearchableObjects())
				dialog.onObjectSelected = { obj ->
					if (obj.type == SkyObject.Type.CONSTELLATION) {
						val constellations = dataProvider.getConstellations(requireContext())
						constellations.find { it.name == obj.name }?.let { c ->
							manualAzimuth = true
							starView.setSelectedConstellation(c, center = true, animate = true)
							showConstellationInfo(c)
						}
					} else {
						manualAzimuth = true
						starView.setSelectedObject(obj, center = true, animate = true)
						showObjectInfo(obj)
					}
				}
				dialog.show(childFragmentManager, StarMapSearchDialogFragment.TAG)
			}
		}

		timeControlBtn.setOnClickListener {
			timeSelectionView.isVisible = !timeSelectionView.isVisible
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
			starView.showNebulae = config.showNebulae
			starView.showOpenClusters = config.showOpenClusters
			starView.showGlobularClusters = config.showGlobularClusters
			starView.showGalaxyClusters = config.showGalaxyClusters
			starView.showSun = config.showSun
			starView.showMoon = config.showMoon
			starView.showPlanets = config.showPlanets
			showMagnitudeFilter = config.showMagnitudeFilter
			starView.magnitudeFilter = config.magnitudeFilter?.toFloat()
			starView.is2DMode = config.is2DMode
			if (config.magnitudeFilter != null) {
				magnitudeValueText.text = String.format(Locale.getDefault(), "%.1f", config.magnitudeFilter)
			}
		}

		updateMagnitudeFilterVisibility()
		updateStarMap(true)
		
		view.findViewById<StarCompassButton>(R.id.star_map_compass_button)?.let {
			it.onSingleTap = { setAzimuth(0.0, true)}
			it.setMapActivity(requireMapActivity())
			compassButton = it
		}

		previousAltitude = starView.getAltitude()
		previousAzimuth = starView.getAzimuth()
		previousViewAngle = starView.getViewAngle()
		apply2DMode(starView.is2DMode)

		val bottomSheetContainer = view.findViewById<View>(R.id.bottom_sheet_container)
		bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetContainer)
		bottomSheetBehavior.skipCollapsed = true
		bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
		bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
			override fun onStateChanged(bottomSheet: View, newState: Int) {
				if (newState == BottomSheetBehavior.STATE_HIDDEN) {
					starView.setSelectedObject(null)
					starView.setSelectedConstellation(null)
					starView.invalidate()
				}
			}
			override fun onSlide(bottomSheet: View, slideOffset: Float) {}
		})

		return view
	}

	private fun applyWindowInsets(view: View) {
		ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
				val baseMarginBottom = v.resources.getDimensionPixelSize(R.dimen.content_padding)
				bottomMargin = insets.bottom + baseMarginBottom
			}
			systemBottomInset = insets.bottom
			windowInsets
		}
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
		lastUpdatedAzimuth = azimuth
	}

	override fun updateLocation(location: Location?) {
		if (location == null) return
		arModeHelper.updateGeomagneticField(location)

		val isMapLinked = app.mapViewTrackingUtilities.isMapLinkedToLocation
		val rotateMode = settings.ROTATE_MAP.get()
		val isRotateBearing = rotateMode == OsmandSettings.ROTATE_MAP_BEARING

		var needsAzimuthUpdate = false
		if (!manualAzimuth && !arModeHelper.isArModeEnabled && isRotateBearing && location.hasBearing() && location.bearing != 0f) {
			val bearing = location.bearing.toDouble()
			if (lastUpdatedAzimuth == -1.0 || abs(MapUtils.degreesDiff(bearing, lastUpdatedAzimuth)) >= 1.0) {
				needsAzimuthUpdate = true
			}
		}

		val locationThreshold = 500.0 // meters
		var needsLocationUpdate = false
		if (isMapLinked) {
			val lastLoc = lastUpdatedLocation
			if (lastLoc == null || location.distanceTo(lastLoc) >= locationThreshold) {
				needsLocationUpdate = true
			}
		}

		if (needsAzimuthUpdate || needsLocationUpdate) {
			app.runInUIThread {
				if (needsAzimuthUpdate) {
					val bearing = location.bearing.toDouble()
					setAzimuth(bearing, true)
				}
				if (needsLocationUpdate) {
					updateStarMap()
					updateStarChart()
					lastUpdatedLocation = Location(location)
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
		updateMagnitudeFilterVisibility()
	}

	private fun updateStarChartVisibility(visible: Boolean) {
		starChartsView.visibility = if (visible) View.VISIBLE else View.GONE
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
			showNebulae = starView.showNebulae,
			showOpenClusters = starView.showOpenClusters,
			showGlobularClusters = starView.showGlobularClusters,
			showGalaxyClusters = starView.showGalaxyClusters,
			is2DMode = starView.is2DMode,
			magnitudeFilter = starView.magnitudeFilter?.toDouble()
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
			if (cameraHelper.isCameraOverlayEnabled) cameraHelper.toggleCameraOverlay()
			cameraButton.visibility = View.GONE
			if (arModeHelper.isArModeEnabled) arModeHelper.toggleArMode(false)
			arModeButton.visibility = View.GONE
			manualAzimuth = true
		} else {
			starView.is2DMode = false
			starView.setCenter(previousAzimuth, previousAltitude)
			starView.setViewAngle(previousViewAngle)
			cameraButton.visibility = View.VISIBLE
			arModeButton.visibility = View.VISIBLE
		}
		starView.invalidate()
		update2DModeIcon()
	}

	private fun update2DModeIcon() {
		val iconId = if (starView.is2DMode) R.drawable.ic_action_globe_view else R.drawable.ic_action_celestial_path
		mode2dButton.setImageDrawable(getIcon(iconId, ColorUtilities.getPrimaryIconColorId(nightMode)))
	}

	private fun setupObservers() {
		starMapViewModel.currentTime.observe(viewLifecycleOwner) { time ->
			starView.setDateTime(time, animate = true)
			updateBottomSheetInfo()
		}
		starMapViewModel.currentCalendar.observe(viewLifecycleOwner) { calendar ->
			timeSelectionView.setDateTime(calendar)
			val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
			timeControlBtn.text = timeFormat.format(calendar.time)
		}
		starChartViewModel.currentTime.observe(viewLifecycleOwner) { updateStarChart() }
		starMapViewModel.skyObjects.observe(viewLifecycleOwner) { objects ->
			starView.setSkyObjects(objects)
			if (objects.isNotEmpty()) {
				val maxMag = MAX_MAGNITUDE
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
		starMapViewModel.constellations.observe(viewLifecycleOwner) { constellations ->
			starView.setConstellations(constellations)
		}
		starChartViewModel.skyObjects.observe(viewLifecycleOwner) { objects ->
			starVisiblityView.setChartObjects(objects)
			starAltitudeView.setChartObjects(objects)
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
				if (starView.getSelectedConstellationItem() == null) hideBottomSheet()
			}
		}
		starView.onConstellationClickListener = { constellation ->
			if (constellation != null) {
				showConstellationInfo(constellation)
			} else {
				if (selectedObject == null) hideBottomSheet()
			}
		}
		starView.onAnimationFinished = { updateBottomSheetInfo() }
		starView.onAzimuthManualChangeListener = { azimuth ->
			if (!cameraHelper.isCameraOverlayEnabled) {
				if (arModeHelper.isArModeEnabled) arModeHelper.toggleArMode()
				manualAzimuth = true
				compassButton?.update(-azimuth.toFloat())
			}
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

	fun hideBottomSheet() {
		bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
	}

	private fun updateBottomSheetInfo() {
		(childFragmentManager.findFragmentById(R.id.bottom_sheet_container) as? SkyObjectInfoFragment)?.let {
			selectedObject?.let { obj -> it.updateObjectInfo(obj) }
		}
	}

	private fun showConstellationInfo(c: Constellation) {
		val existing = childFragmentManager.findFragmentById(R.id.bottom_sheet_container) as? ConstellationInfoFragment
		if (existing == null || existing.arguments?.getString("name") != c.name) {
			childFragmentManager.beginTransaction()
				.replace(R.id.bottom_sheet_container, ConstellationInfoFragment.newInstance(c))
				.commitNow()
		}
		bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
	}

	private fun showObjectInfo(obj: SkyObject) {
		val existing = childFragmentManager.findFragmentById(R.id.bottom_sheet_container) as? SkyObjectInfoFragment
		if (existing == null || existing.arguments?.getString("skyObjectName") != obj.name) {
			childFragmentManager.beginTransaction()
				.replace(R.id.bottom_sheet_container, SkyObjectInfoFragment.newInstance(obj))
				.commitNow()
		} else {
			existing.updateObjectInfo(obj)
		}
		bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
	}

	private fun getSearchableObjects(): List<SkyObject> {
		val objects = starMapViewModel.skyObjects.value?.toMutableList() ?: mutableListOf()
		val constellations = dataProvider.getConstellations(requireContext())
		val skyObjectMap = objects.associateBy { it.hip }
		constellations.forEach { c ->
			val center = AstroUtils.calculateConstellationCenter(c, skyObjectMap)
			objects.add(SkyObject(
				id = "const_${c.name}",
				hip = -1,
				wid = c.wid,
				type = SkyObject.Type.CONSTELLATION,
				body = null,
				name = c.name,
				ra = center?.first ?: 0.0,
				dec = center?.second ?: 0.0,
				magnitude = 2.0f,
				color = Color.WHITE,
				localizedName = c.localizedName
			))
		}
		return objects
	}
}
