package net.osmand.plus.plugins.astro

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import net.osmand.shared.util.LoggerFactory
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.tan

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
	private lateinit var cameraButton: ImageButton
	private lateinit var cameraTextureView: TextureView
	private lateinit var sliderContainer: FrameLayout
	private lateinit var transparencySlider: SeekBar
	private lateinit var resetFovButton: ImageButton

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
	
	// Adaptive Smoothing
	private val minAlpha = 0.03
	private val maxAlpha = 0.3
	private val jitterThresh = 0.5
	private val moveThresh = 2.0
	
	private var lastAccuracyWarningTime = 0L

	// --- Camera Overlay ---
	private var isCameraOverlayEnabled = false
	private var cameraDevice: android.hardware.camera2.CameraDevice? = null
	private var captureSession: android.hardware.camera2.CameraCaptureSession? = null
	private var calculatedFov = 60.0 // Default fallback
	private var previewSize: Size? = null
	private var baseTransformMatrix: Matrix? = null

	companion object {
		private val log = LoggerFactory.getLogger("StarMapFragment")
		val TAG: String = StarMapFragment::class.java.simpleName

		private const val PERMISSION_REQUEST_CAMERA = 1001

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
		cameraTextureView = view.findViewById(R.id.camera_view)
		timeSelectionView = view.findViewById(R.id.time_selection_view)
		resetTimeButton = view.findViewById(R.id.reset_time_button)
		bottomSheet = view.findViewById(R.id.bottom_sheet)
		sheetTitle = view.findViewById(R.id.sheet_title)
		sheetCoords = view.findViewById(R.id.sheet_coords)
		sheetDetails = view.findViewById(R.id.sheet_details)

		arModeButton = view.findViewById(R.id.ar_mode_button)
		cameraButton = view.findViewById(R.id.camera_button)
		sliderContainer = view.findViewById(R.id.slider_container)
		transparencySlider = view.findViewById(R.id.transparency_slider)
		resetFovButton = view.findViewById(R.id.reset_fov_button)

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

		// Camera Overlay Toggle
		cameraButton.setOnClickListener {
			toggleCameraOverlay()
		}

		// Transparency Slider
		transparencySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				cameraTextureView.alpha = progress / 100f
			}
			override fun onStartTrackingTouch(seekBar: SeekBar?) {}
			override fun onStopTrackingTouch(seekBar: SeekBar?) {}
		})

		// Reset FOV Button
		resetFovButton.setOnClickListener {
			starView.setViewAngle(calculatedFov)
			Toast.makeText(context, "FOV reset to ${String.format("%.1f", calculatedFov)}°", Toast.LENGTH_SHORT).show()
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
		updateCameraButtonState()

		// Calculate FOV initially if camera permission is already granted (best effort)
		if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
			calculatedFov = calculateCameraFov()
		}

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
		if (isCameraOverlayEnabled) {
			openCamera()
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
		closeCamera()

		val mapActivity = requireMapActivity()
		mapActivity.enableDrawer()
		updateWidgetsVisibility(mapActivity, View.VISIBLE)
		mapActivity.refreshMap()
	}

	// ... [Existing Compass/Location methods are unchanged] ...
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
				updateStarMap()
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
			starView.roll = 0.0
			Toast.makeText(context, "AR Mode Disabled", Toast.LENGTH_SHORT).show()
		}
		updateArButtonState()
	}

	private fun updateArButtonState() {
		if (isArModeEnabled) {
			arModeButton.setColorFilter(Color.BLUE)
		} else {
			arModeButton.setColorFilter("#5f6e7c".toColorInt())
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

			val vX = -remappedRotationMatrix[2]
			val vY = -remappedRotationMatrix[5]
			val vZ = -remappedRotationMatrix[8]

			val azimuthRad = atan2(vX.toDouble(), vY.toDouble())
			val altitudeRad = asin(vZ.toDouble())

			var azimuthDeg = Math.toDegrees(azimuthRad)
			val altitudeDeg = Math.toDegrees(altitudeRad)

			if (azimuthDeg < 0) azimuthDeg += 360

			if (geomagneticField != null) {
				azimuthDeg += geomagneticField!!.declination
			}
			if (azimuthDeg >= 360) azimuthDeg -= 360
			if (azimuthDeg < 0) azimuthDeg += 360

			val azDiff = azimuthDeg - smoothedAzimuth
			var azDelta = azDiff
			if (azDiff > 180) azDelta = azDiff - 360
			else if (azDiff < -180) azDelta = azDiff + 360

			// Adaptive Smoothing for Azimuth
			val alphaAz = calculateAdaptiveAlpha(azDelta)
			smoothedAzimuth += azDelta * alphaAz
			if (smoothedAzimuth >= 360) smoothedAzimuth -= 360
			if (smoothedAzimuth < 0) smoothedAzimuth += 360

			// Adaptive Smoothing for Altitude
			val altDelta = altitudeDeg - smoothedAltitude
			val alphaAlt = calculateAdaptiveAlpha(altDelta)
			smoothedAltitude += altDelta * alphaAlt

			// Calculate Roll (Projected Zenith angle on Screen)
			val zenithX = remappedRotationMatrix[6]
			val zenithY = remappedRotationMatrix[7]
			val rollDeg = Math.toDegrees(atan2(zenithX.toDouble(), zenithY.toDouble()))

			starView.setCenter(smoothedAzimuth, smoothedAltitude)
			starView.roll = rollDeg
			
			// Compass Calibration Check
			if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR || event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
				if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
					val currentTime = System.currentTimeMillis()
					if (currentTime - lastAccuracyWarningTime > 10000) { // Warn every 10s max
						Toast.makeText(context, "Compass calibration needed", Toast.LENGTH_SHORT).show()
						lastAccuracyWarningTime = currentTime
					}
				}
			}
		}
	}
	
	private fun calculateAdaptiveAlpha(delta: Double): Double {
		val absDelta = kotlin.math.abs(delta)
		return when {
			absDelta < jitterThresh -> minAlpha // Very stable for jitter
			absDelta > moveThresh -> maxAlpha   // Fast response
			else -> minAlpha + (absDelta - jitterThresh) * (maxAlpha - minAlpha) / (moveThresh - jitterThresh)
		}
	}

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

	// --- Camera Overlay Logic ---

	private fun toggleCameraOverlay() {
		if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CAMERA)
			return
		}

		isCameraOverlayEnabled = !isCameraOverlayEnabled
		if (isCameraOverlayEnabled) {
			// Auto-enable AR mode if it was off
			if (!isArModeEnabled) {
				toggleArMode()
			}
			
			// Initial best guess, will be refined in configureTransform
			calculatedFov = calculateSensorFov()
			openCamera()
			cameraTextureView.visibility = View.VISIBLE
			sliderContainer.visibility = View.VISIBLE
			resetFovButton.visibility = View.VISIBLE
		} else {
			closeCamera()
			cameraTextureView.visibility = View.GONE
			sliderContainer.visibility = View.GONE
			resetFovButton.visibility = View.GONE
		}
		updateCameraButtonState()
	}

	private fun updateCameraButtonState() {
		if (isCameraOverlayEnabled) {
			cameraButton.setColorFilter(Color.BLUE)
		} else {
			cameraButton.setColorFilter("#5f6e7c".toColorInt())
		}
	}

	private fun openCamera() {
		if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			return
		}

		if (cameraTextureView.isAvailable) {
			startCameraSession()
		} else {
			cameraTextureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
				@RequiresPermission(Manifest.permission.CAMERA)
				override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
					startCameraSession()
				}
				override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
					configureTransform(width, height)
				}
				override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
				override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
			}
		}
	}

	@RequiresPermission(Manifest.permission.CAMERA)
	private fun startCameraSession() {
		val manager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
		try {
			// Find back camera
			var cameraId: String? = null
			for (id in manager.cameraIdList) {
				val characteristics = manager.getCameraCharacteristics(id)
				if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
					cameraId = id

					// Calculate optimal preview size
					val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
					if (map != null) {
						previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
							cameraTextureView.width, cameraTextureView.height)
						// Apply transform immediately based on selected size
						configureTransform(cameraTextureView.width, cameraTextureView.height)
					}
					break
				}
			}

			if (cameraId == null) return

			manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
				override fun onOpened(camera: CameraDevice) {
					cameraDevice = camera
					createCaptureSession()
				}
				override fun onDisconnected(camera: CameraDevice) {
					camera.close()
					cameraDevice = null
				}
				override fun onError(camera: CameraDevice, error: Int) {
					camera.close()
					cameraDevice = null
				}
			}, null)
		} catch (e: Exception) {
			log.error("Failed to open camera", e)
		}
	}

	private fun chooseOptimalSize(choices: Array<Size>, viewWidth: Int, viewHeight: Int): Size {
		// Prefer sizes that match the aspect ratio of the TextureView to minimize cropping
		val targetRatio = if (viewWidth < viewHeight) {
			viewHeight.toFloat() / viewWidth
		} else {
			viewWidth.toFloat() / viewHeight
		}

		val tolerance = 0.1f
		val matchAspect = choices.filter {
			val ratio = if (it.height > 0) it.width.toFloat() / it.height else 0f
			kotlin.math.abs(ratio - targetRatio) < tolerance
		}

		val candidates = if (matchAspect.isNotEmpty()) matchAspect else choices.toList()

		// Pick largest available size within candidates to ensure quality
		return candidates.maxByOrNull { it.width * it.height } ?: choices[0]
	}

	private fun configureTransform(viewWidth: Int, viewHeight: Int) {
		val activity = activity ?: return
		if (null == previewSize || null == cameraTextureView) {
			return
		}
		val rotation = activity.windowManager.defaultDisplay.rotation
		val matrix = Matrix()
		val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
		val bufferRect = RectF(0f, 0f, previewSize!!.height.toFloat(), previewSize!!.width.toFloat())
		val centerX = viewRect.centerX()
		val centerY = viewRect.centerY()

		var scaleX = 1f
		var scaleY = 1f

		val sensorInfo = getSensorInfo()
		val fovW = sensorInfo?.fovWidth ?: 60.0
		val sensorRatio = sensorInfo?.aspectRatio ?: (4.0/3.0)
		// FOV Height: tan(Ah/2) = tan(Aw/2) / ratio
		val fovH = Math.toDegrees(2 * atan(tan(Math.toRadians(fovW/2.0)) / sensorRatio))

		var baseFovForX = fovW

		if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
			// Landscape: Screen X aligns with Sensor Width
			baseFovForX = fovW
			
			bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
			matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
			val scale = kotlin.math.max(
				viewHeight.toFloat() / previewSize!!.height,
				viewWidth.toFloat() / previewSize!!.width
			)
			matrix.postScale(scale, scale, centerX, centerY)
			matrix.postRotate(90f * (rotation - 2), centerX, centerY)

			scaleX = (previewSize!!.width * scale) / viewWidth
			scaleY = (previewSize!!.height * scale) / viewHeight
		} else if (Surface.ROTATION_180 == rotation) {
			// Upside Down Portrait: Screen X aligns with Sensor Height
			baseFovForX = fovH

			bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
			matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
			val scale = kotlin.math.max(
				viewHeight.toFloat() / previewSize!!.height,
				viewWidth.toFloat() / previewSize!!.width
			)
			matrix.postScale(scale, scale, centerX, centerY)
			matrix.postRotate(180f, centerX, centerY)

			scaleX = (previewSize!!.height * scale) / viewWidth
			scaleY = (previewSize!!.width * scale) / viewHeight
		} else if (Surface.ROTATION_0 == rotation) {
			// Portrait: Screen X aligns with Sensor Height
			baseFovForX = fovH
			
			// Portrait mode: Camera sensor is usually landscape, so we need to rotate 90 degrees
			bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
			matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
			val scale = kotlin.math.max(
				viewHeight.toFloat() / previewSize!!.height,
				viewWidth.toFloat() / previewSize!!.width
			)
			matrix.postScale(scale, scale, centerX, centerY)

			// Update scales for FOV Refinement
			// effectively: how much did we zoom in relative to the full preview image fitting the screen?
			// The preview image is rotated. Height becomes Width.
			scaleX = (previewSize!!.height * scale) / viewWidth
			scaleY = (previewSize!!.width * scale) / viewHeight
		}
		baseTransformMatrix = Matrix(matrix)
		cameraTextureView.setTransform(matrix)

		// Update FOV based on Aspect Ratio Crop
		updateEffectiveFov(scaleX, baseFovForX)
	}

	private fun createCaptureSession() {
		try {
			val texture = cameraTextureView.surfaceTexture!!
			if (previewSize != null) {
				texture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
			} else {
				texture.setDefaultBufferSize(cameraTextureView.width, cameraTextureView.height)
			}
			val surface = Surface(texture)

			val builder = cameraDevice?.createCaptureRequest(android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW)
			builder?.addTarget(surface)

			cameraDevice?.createCaptureSession(listOf(surface), object : android.hardware.camera2.CameraCaptureSession.StateCallback() {
				override fun onConfigured(session: android.hardware.camera2.CameraCaptureSession) {
					captureSession = session
					builder?.build()?.let {
						session.setRepeatingRequest(it, null, null)
					}
				}
				override fun onConfigureFailed(session: android.hardware.camera2.CameraCaptureSession) {}
			}, null)
		} catch (e: Exception) {
			log.error("Failed to create capture session", e)
		}
	}

	private fun closeCamera() {
		captureSession?.close()
		captureSession = null
		cameraDevice?.close()
		cameraDevice = null
	}

	private fun calculateCameraFov(): Double {
		return calculateSensorFov()
	}

	private fun updateEffectiveFov(scaleX: Float, baseFov: Double) {
		// scaleX is the zoom factor along the screen X axis.
		// baseFov is the FOV of the full sensor along the dimension corresponding to screen X.
		
		val halfFovRad = Math.toRadians(baseFov / 2.0)
		val tanHalfFov = tan(halfFovRad)
		val effectiveRad = 2 * atan(tanHalfFov / scaleX)
		calculatedFov = Math.toDegrees(effectiveRad)
		
		app.runInUIThread {
			if (isCameraOverlayEnabled) {
				starView.setViewAngle(calculatedFov)
			}
		}
	}

	private data class SensorInfo(val fovWidth: Double, val aspectRatio: Double)

	private fun getSensorInfo(): SensorInfo? {
		val manager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
		try {
			for (id in manager.cameraIdList) {
				val characteristics = manager.getCameraCharacteristics(id)
				if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
					val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
					val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

					if (sensorSize != null && focalLengths != null && focalLengths.isNotEmpty()) {
						val w = sensorSize.width
						val h = sensorSize.height
						val f = focalLengths[0]
						val fovRad = 2 * atan(w / (2 * f))
						val fovW = Math.toDegrees(fovRad.toDouble())
						return SensorInfo(fovW, w.toDouble() / h)
					}
				}
			}
		} catch (e: Exception) {
			log.error("Failed to calculate camera FOV", e)
		}
		return null
	}
	
	private fun calculateSensorFov(): Double {
		return getSensorInfo()?.fovWidth ?: 60.0
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == PERMISSION_REQUEST_CAMERA) {
			if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				toggleCameraOverlay() // Retry enabling
			} else {
				Toast.makeText(context, "Camera permission required for overlay", Toast.LENGTH_SHORT).show()
			}
		}
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
			if (isArModeEnabled) {
				toggleArMode()
			}
			manualAzimuth = true
			app.osmandMap.mapView.rotateToAnimate(-azimuth.toFloat())
		}

		starView.onViewAngleChangeListener = { fov ->
			updateCameraZoom(fov)
		}
	}

	private fun updateCameraZoom(fov: Double) {
		if (!isCameraOverlayEnabled || baseTransformMatrix == null || cameraTextureView.width == 0) return

		// scale = tan(baseFov/2) / tan(targetFov/2)
		// calculatedFov is our base effective FOV at 1x scale
		val baseRad = Math.toRadians(calculatedFov / 2.0)
		val targetRad = Math.toRadians(fov / 2.0)
		
		val scale = (tan(baseRad) / tan(targetRad)).toFloat()

		val matrix = Matrix(baseTransformMatrix)
		val centerX = cameraTextureView.width / 2f
		val centerY = cameraTextureView.height / 2f

		matrix.postScale(scale, scale, centerX, centerY)
		cameraTextureView.setTransform(matrix)
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