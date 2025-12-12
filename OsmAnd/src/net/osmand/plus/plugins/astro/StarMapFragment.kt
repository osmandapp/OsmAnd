package net.osmand.plus.plugins.astro

import android.content.Context
import android.graphics.Color
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.views.controls.maphudbuttons.MapButton
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.asin
import kotlin.math.atan2

class StarMapFragment : BaseFullScreenFragment(), IMapLocationListener, OsmAndLocationListener,
	OsmAndCompassListener, SensorEventListener {

	private lateinit var starView: StarView
	private lateinit var timeSelectionView: DateTimeSelectionView
	private lateinit var bottomSheet: View
	private lateinit var sheetTitle: TextView
	private lateinit var sheetCoords: TextView
	private lateinit var sheetDetails: TextView
	private lateinit var resetTimeButton: Button
	private lateinit var arModeButton: ImageButton

	private lateinit var starChartsView: View
	private lateinit var starVisiblityView: StarVisiblityChartView
	private lateinit var starAltitudeView: StarAltitudeChartView
	private lateinit var celestialPathView: CelestialPathView
	private lateinit var starChartState: StarChartState

	private val mapButtons = mutableListOf<MapButton>()
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

	// --- AR Mode / Sensors ---
	private var isArModeEnabled = false
	private lateinit var sensorManager: SensorManager
	private var sensorRotation: Sensor? = null
	private var sensorAccelerometer: Sensor? = null
	private var sensorMagnetic: Sensor? = null

	private val rotationMatrix = FloatArray(9)
	private val remappedRotationMatrix = FloatArray(9)
	private val accelerometerReading = FloatArray(3)
	private val magnetometerReading = FloatArray(3)

	private var hasAccelerometer = false
	private var hasMagnetometer = false
	private var geomagneticField: GeomagneticField? = null

	// Low pass filter for smoothing
	private var smoothedAzimuth = 0.0
	private var smoothedAltitude = 45.0
	private val filterAlpha = 0.1 // Smoothing factor

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

		// Initialize Sensor Manager
		sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
		sensorRotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
		if (sensorRotation == null) {
			sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
			sensorMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
		}

		starView = view.findViewById(R.id.star_view)
		timeSelectionView = view.findViewById(R.id.time_selection_view)
		resetTimeButton = view.findViewById(R.id.reset_time_button)
		bottomSheet = view.findViewById(R.id.bottom_sheet)
		sheetTitle = view.findViewById(R.id.sheet_title)
		sheetCoords = view.findViewById(R.id.sheet_coords)
		sheetDetails = view.findViewById(R.id.sheet_details)
		arModeButton = view.findViewById(R.id.ar_mode_button)

		ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { v, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.updatePadding(bottom = v.paddingTop + insets.bottom)
			windowInsets
		}

		val starMapControlsContainer = view.findViewById<View>(R.id.star_map_controls_container)
		val mapControlsContainer = view.findViewById<View>(R.id.map_controls_container)

		val insetsListener = androidx.core.view.OnApplyWindowInsetsListener { v, windowInsets ->
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

		// AR Mode Toggle
		arModeButton.setOnClickListener {
			toggleArMode()
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
			starView.showConstellations = config.showConstellations
		}
		starView.setConstellations(AstroDataProvider.getConstellations())

		updateStarMap(true)
		setupToolBar(view)
		buildZoomButtons(view)
		updateArButtonState()

		return view
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

		if (isArModeEnabled) {
			registerSensors()
		}

		val mapActivity = requireMapActivity()
		mapActivity.disableDrawer()
		updateWidgetsVisibility(mapActivity, View.GONE)
		mapActivity.refreshMap()
	}

	override fun onPause() {
		super.onPause()
		app.locationProvider.removeLocationListener(this)
		app.locationProvider.removeCompassListener(this)
		app.osmandMap.mapView.removeMapLocationListener(this)
		unregisterSensors()

		val mapActivity = requireMapActivity()
		mapActivity.enableDrawer()
		updateWidgetsVisibility(mapActivity, View.VISIBLE)
		mapActivity.refreshMap()
	}

	override fun updateCompassValue(value: Float) {
		// If AR mode is ON, ignore standard compass updates
		if (isArModeEnabled) return

		val lastResetRotationToNorth = app.mapViewTrackingUtilities.lastResetRotationToNorth
		if (this.lastResetRotationToNorth < lastResetRotationToNorth) {
			this.lastResetRotationToNorth = lastResetRotationToNorth
			manualAzimuth = false
		}
		if (manualAzimuth) return

		val rotateMode = settings.ROTATE_MAP.get()
		if (rotateMode == OsmandSettings.ROTATE_MAP_COMPASS) {
			starView.setAzimuth(value.toDouble())
		} else if (rotateMode != OsmandSettings.ROTATE_MAP_BEARING) {
			starView.setAzimuth(-app.osmandMap.mapView.rotate.toDouble())
		}
	}

	override fun updateLocation(location: Location?) {
		if (location == null) return

		// Update Geomagnetic Field for Declination
		geomagneticField = GeomagneticField(
			location.latitude.toFloat(),
			location.longitude.toFloat(),
			location.altitude.toFloat(),
			System.currentTimeMillis()
		)

		if (app.mapViewTrackingUtilities.isMapLinkedToLocation) {
			app.runInUIThread {
				if (!manualAzimuth && !isArModeEnabled) {
					if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING) {
						if (location.hasBearing() && location.bearing != 0f) {
							starView.setAzimuth(location.bearing.toDouble(), true)
						}
					}
				}
				updateStarMap();
				updateStarChart()
			}
		} else if (!manualAzimuth && !isArModeEnabled) {
			app.runInUIThread {
				if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING) {
					if (location.hasBearing() && location.bearing != 0f) {
						starView.setAzimuth(location.bearing.toDouble(), true)
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

	// --- AR Mode Logic ---

	private fun toggleArMode() {
		isArModeEnabled = !isArModeEnabled
		if (isArModeEnabled) {
			registerSensors()
			Toast.makeText(context, "AR Mode Enabled", Toast.LENGTH_SHORT).show()
		} else {
			unregisterSensors()
			manualAzimuth = true // Stop auto-rotating back immediately
			Toast.makeText(context, "AR Mode Disabled", Toast.LENGTH_SHORT).show()
		}
		updateArButtonState()
	}

	private fun updateArButtonState() {
		if (isArModeEnabled) {
			arModeButton.setColorFilter(Color.BLUE)
		} else {
			arModeButton.setColorFilter(Color.parseColor("#5f6e7c")) // Dark grey for visibility on white background
		}
	}

	private fun registerSensors() {
		if (sensorRotation != null) {
			sensorManager.registerListener(this, sensorRotation, SensorManager.SENSOR_DELAY_GAME)
		} else if (sensorAccelerometer != null && sensorMagnetic != null) {
			sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_GAME)
			sensorManager.registerListener(this, sensorMagnetic, SensorManager.SENSOR_DELAY_GAME)
		} else {
			Toast.makeText(context, "Sensors not available for AR", Toast.LENGTH_SHORT).show()
			isArModeEnabled = false
			updateArButtonState()
		}
	}

	private fun unregisterSensors() {
		sensorManager.unregisterListener(this)
	}

	override fun onSensorChanged(event: SensorEvent) {
		if (!isArModeEnabled) return

		var success = false

		if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
			SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
			success = true
		} else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
			System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
			hasAccelerometer = true
		} else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
			System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
			hasMagnetometer = true
		}

		if (hasAccelerometer && hasMagnetometer && event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) {
			success = SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
		}

		if (success) {
			val windowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
			val rotation = windowManager.defaultDisplay.rotation

			var axisX = SensorManager.AXIS_X
			var axisY = SensorManager.AXIS_Y

			when (rotation) {
				Surface.ROTATION_0 -> {
					axisX = SensorManager.AXIS_X
					axisY = SensorManager.AXIS_Y
				}
				Surface.ROTATION_90 -> {
					axisX = SensorManager.AXIS_Y
					axisY = SensorManager.AXIS_MINUS_X
				}
				Surface.ROTATION_180 -> {
					axisX = SensorManager.AXIS_MINUS_X
					axisY = SensorManager.AXIS_MINUS_Y
				}
				Surface.ROTATION_270 -> {
					axisX = SensorManager.AXIS_MINUS_Y
					axisY = SensorManager.AXIS_X
				}
			}

			SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedRotationMatrix)

			// We need the direction the "back camera" is pointing.
			// In the remapped coordinate system (aligned with screen):
			// X is Right, Y is Up, Z is Out of Screen.
			// The back camera points in the -Z direction: Vector(0, 0, -1).
			// We transform this vector to World coordinates using the rotation matrix R.
			// V_world = R * V_device
			// V_world = R * [0, 0, -1]^T
			//
			// R indices:
			// 0 1 2
			// 3 4 5
			// 6 7 8
			//
			// Vx = R0*0 + R1*0 + R2*-1 = -R2
			// Vy = R3*0 + R4*0 + R5*-1 = -R5
			// Vz = R6*0 + R7*0 + R8*-1 = -R8

			val vX = -remappedRotationMatrix[2]
			val vY = -remappedRotationMatrix[5]
			val vZ = -remappedRotationMatrix[8]

			// Calculate Azimuth (Angle around Z axis, from Y towards X)
			// atan2(x, y) returns angle from Y axis towards X axis (which matches Azimuth definition: 0=N, 90=E)
			val azimuthRad = atan2(vX.toDouble(), vY.toDouble())

			// Calculate Altitude (Angle above Horizon)
			// asin(z) gives angle from -pi/2 (Nadir) to +pi/2 (Zenith)
			val altitudeRad = asin(vZ.toDouble())

			var azimuthDeg = Math.toDegrees(azimuthRad)
			val altitudeDeg = Math.toDegrees(altitudeRad)

			// Normalize Azimuth 0..360
			if (azimuthDeg < 0) azimuthDeg += 360

			// Apply magnetic declination if available
			if (geomagneticField != null) {
				azimuthDeg += geomagneticField!!.declination
			}
			// Normalize again
			if (azimuthDeg >= 360) azimuthDeg -= 360
			if (azimuthDeg < 0) azimuthDeg += 360

			// Smoothing (Low Pass Filter)
			// Handle 360 wrap-around for azimuth
			val azDiff = azimuthDeg - smoothedAzimuth
			var azDelta = azDiff
			if (azDiff > 180) azDelta = azDiff - 360
			else if (azDiff < -180) azDelta = azDiff + 360

			smoothedAzimuth += azDelta * filterAlpha
			// Normalize smoothed azimuth
			if (smoothedAzimuth >= 360) smoothedAzimuth -= 360
			if (smoothedAzimuth < 0) smoothedAzimuth += 360

			smoothedAltitude += (altitudeDeg - smoothedAltitude) * filterAlpha

			// Update StarView
			starView.setCenter(smoothedAzimuth, smoothedAltitude)
		}
	}

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
		// No-op
	}

	private fun updateStarMapVisibility(visible: Boolean) {
		starView.visibility = if (visible) View.VISIBLE else View.GONE
		val starMapControls = view?.findViewById<View>(R.id.star_map_controls_container)
		val mapControls = view?.findViewById<View>(R.id.map_controls_container)

		starMapControls?.visibility = if (visible) View.VISIBLE else View.GONE
		mapControls?.visibility = if (!visible) View.VISIBLE else View.GONE
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

		starView.onAzimuthManualChangeListener = { azimuth ->
			// If user pans manually, we might want to disable AR or just let it happen.
			// Currently, we just stop auto-map-rotation logic.
			manualAzimuth = true
			app.osmandMap.mapView.rotateToAnimate(-azimuth.toFloat())
		}
	}

	private fun updateStarMap(updateAzimuth: Boolean = false) {
		val tileBox = app.osmandMap.mapView.rotatedTileBox
		val location = tileBox.centerLatLon
		starView.setObserverLocation(location.latitude, location.longitude, 0.0)
		if (updateAzimuth && !isArModeEnabled) starView.setAzimuth(-tileBox.rotate.toDouble())
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

		fun addButtons(container: View, starMap: Boolean): Boolean? {
			container.findViewById<MapButton?>(R.id.map_zoom_in_button)?.let {
				mapButtons.add(it)
				if (starMap) {
					it.setOnClickListener { starView.zoomIn() }
					it.setOnLongClickListener(null)
				}
			}
			container.findViewById<MapButton?>(R.id.map_zoom_out_button)?.let {
				mapButtons.add(it)
				if (starMap) {
					it.setOnClickListener { starView.zoomOut() }
					it.setOnLongClickListener(null)
				}
			}
			container.findViewById<MapButton?>(R.id.map_my_location_button)?.let {
				mapButtons.add(it)
			}
			return container.findViewById<View?>(R.id.map_hud_controls)?.let {
				AndroidUiHelper.updateVisibility(it, true)
			}
		}

		view.findViewById<MapButton?>(R.id.map_compass_button)?.let {
			layer.addCustomMapButton(it)
			mapButtons.add(it)
		}
		view.findViewById<View>(R.id.star_map_controls_container)?.let { container ->
			addButtons(container, true)
		}
		view.findViewById<View>(R.id.map_controls_container)?.let { container ->
			addButtons(container, false)
		}

		layer.addCustomizedDefaultMapButtons(mapButtons)

		val mapInfoLayer = mapLayers.mapInfoLayer
		rulerWidget = mapInfoLayer.setupRulerWidget(view.findViewById(R.id.map_ruler_layout))
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
		val toggleItems = arrayOf(getString(R.string.azimuthal_grid), getString(R.string.equatorial_grid), getString(R.string.ecliptic_line), "Constellations")

		var tempAzimuthal = starView.showAzimuthalGrid
		var tempEquatorial = starView.showEquatorialGrid
		var tempEcliptic = starView.showEclipticLine
		var tempConstellations = starView.showConstellations

		val toggleChecked = booleanArrayOf(tempAzimuthal, tempEquatorial, tempEcliptic, tempConstellations)

		val currentObjects = (starMapViewModel.skyObjects.value ?: emptyList()).take(30)
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
						3 -> tempConstellations = isChecked
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
				starView.showConstellations = tempConstellations

				currentObjects.forEachIndexed { index, skyObject ->
					skyObject.isVisible = objectChecked[index]
				}
				starView.updateVisibility()

				val itemsConfig = currentObjects.map { SkyObjectConfig(it.id, it.isVisible) }
				val config = StarWatcherSettings.StarMapConfig(
					showAzimuthalGrid = tempAzimuthal,
					showEquatorialGrid = tempEquatorial,
					showEclipticLine = tempEcliptic,
					showConstellations = tempConstellations,
					items = itemsConfig
				)
				swSettings.setStarMapConfig(config)
			}
			.setNegativeButton(R.string.shared_string_cancel, null)
			.show()
	}
}