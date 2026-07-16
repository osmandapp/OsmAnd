package net.osmand.plus.plugins.astronomy

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import io.github.cosinekitty.astronomy.EclipseKind
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.SolarEclipseMapCoordinate
import io.github.cosinekitty.astronomy.SolarEclipsePhase
import io.github.cosinekitty.astronomy.SolarEclipseShadowPoint
import io.github.cosinekitty.astronomy.SolarEclipseState
import io.github.cosinekitty.astronomy.Time
import net.osmand.Location
import net.osmand.core.jni.PointI
import net.osmand.map.IMapLocationListener
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseFullScreenFragment
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.astronomy.AstronomyPluginSettings.CommonConfig
import net.osmand.plus.plugins.astronomy.AstronomyPluginSettings.StarMapConfig
import net.osmand.plus.plugins.astronomy.search.StarMapSearchDialogFragment
import net.osmand.plus.plugins.astronomy.utils.StarMapARModeHelper
import net.osmand.plus.plugins.astronomy.utils.StarMapCameraHelper
import net.osmand.plus.plugins.astronomy.views.DateTimeSelectionView
import net.osmand.plus.plugins.astronomy.views.StarCompassButton
import net.osmand.plus.plugins.astronomy.views.StarMapButton
import net.osmand.plus.plugins.astronomy.views.StarMapResetButton
import net.osmand.plus.plugins.astronomy.views.StarMapTimeControlButton
import net.osmand.plus.plugins.astronomy.views.StarView
import net.osmand.plus.plugins.astronomy.views.StarViewCameraState
import net.osmand.plus.plugins.astronomy.views.SolarEclipseTimelineView
import net.osmand.plus.plugins.astronomy.views.contextmenu.AstroBottomSheetBehavior
import net.osmand.plus.plugins.astronomy.views.contextmenu.AstroContextMenuFragment
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.coordinates.CoordinateFormatFormatter
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.utils.InsetsUtils
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.dialogbutton.DialogButtonType
import net.osmand.shared.util.LoggerFactory
import net.osmand.util.MapUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min


class StarMapFragment : BaseFullScreenFragment(), IMapLocationListener, OsmAndLocationListener,
	OsmAndCompassListener, DownloadEvents {

	private val REGULAR_MAP_HEIGHT = 300f
	private val REGULAR_MAP_HEIGHT_LANDSCAPE = 110f

	internal lateinit var mainLayout: View
	internal lateinit var starView: StarView
	private lateinit var mapControlsContainer: View
	private lateinit var timeSelectionView: DateTimeSelectionView
	private lateinit var timeControlBtn: StarMapTimeControlButton
	private lateinit var resetTimeButton: StarMapResetButton

	private lateinit var arModeButton: StarMapButton
	private lateinit var cameraButton: StarMapButton
	private lateinit var transparencySlider: SeekBar
	private lateinit var sliderContainer: View
	private lateinit var resetFovButton: StarMapButton

	private lateinit var magnitudeFilterButton: MaterialCardView
	private lateinit var magnitudeFilterIcon: ImageView
	private lateinit var magnitudeFilterText: TextView
	private lateinit var magnitudeSliderCard: MaterialCardView
	private lateinit var magnitudeSlider: SeekBar
	private lateinit var magnitudeSliderValue: TextView

	private lateinit var timeControlCard: MaterialCardView
	private lateinit var closeButton: StarMapButton
	private lateinit var searchButton: StarMapButton
	private lateinit var settingsButton: StarMapButton

	private lateinit var compassButton: StarCompassButton
	private lateinit var eclipseCard: MaterialCardView
	private lateinit var eclipseLoading: ProgressBar
	private lateinit var eclipseErrorContainer: View
	private lateinit var eclipseContent: View
	private lateinit var eclipsePrevious: ImageButton
	private lateinit var eclipseNext: ImageButton
	private lateinit var eclipseClose: ImageButton
	private lateinit var eclipseKind: TextView
	private lateinit var eclipseEventDate: TextView
	private lateinit var eclipseCurrentTime: TextView
	private lateinit var eclipseCurrentDateZone: TextView
	private lateinit var eclipseLocalStatus: TextView
	private lateinit var eclipseMapCenterLocation: TextView
	private lateinit var eclipseTimeline: SolarEclipseTimelineView
	private lateinit var eclipseStartTime: TextView
	private lateinit var eclipseMaximumTime: TextView
	private lateinit var eclipseEndTime: TextView
	private lateinit var eclipseFitPath: View
	private lateinit var eclipseToggleMap: View
	private lateinit var eclipseRetry: MaterialButton
	private var manualAzimuth: Boolean = true
	private var lastResetRotationToNorth = 0L
	private var lastUpdatedLocation: Location? = null
	private var lastUpdatedAzimuth: Double = -1.0

	internal lateinit var viewModel: StarObjectsViewModel
	private var selectedObject: SkyObject? = null
	private var solarEclipseSun: SkyObject? = null
	var regularMapVisible = false
		private set

	private val dataProvider: AstroDataProvider by lazy {
		PluginsHelper.requirePlugin(AstronomyPlugin::class.java).dataProvider
	}
	private val astronomyPlugin: AstronomyPlugin by lazy {
		PluginsHelper.requirePlugin(AstronomyPlugin::class.java)
	}

	val astroSettings: AstronomyPluginSettings by lazy {
		PluginsHelper.requirePlugin(AstronomyPlugin::class.java).astroSettings
	}

	private lateinit var arModeHelper: StarMapARModeHelper
	private lateinit var cameraHelper: StarMapCameraHelper
	private lateinit var bottomSheetContainer: View
	private lateinit var bottomSheetBehavior: AstroBottomSheetBehavior<View>

	private var previousAltitude: Double = 45.0
	private var previousAzimuth: Double = 0.0
	private var previousViewAngle: Double = 150.0
	private var eclipseRestoreState: EclipseRestoreState? = null
	private var restoredActiveEclipseCameraState: StarViewCameraState? = null
	private var lastFocusedEclipseRequestId = -1L
	private var keepSunCenteredForMapMove = false
	private var eclipseMapShown = false
	private var pendingEclipseMapFit = false
	private var pendingSliderTimeMillis: Long? = null
	private var lastBoundEclipseEventKey: Double? = null
	private var lastBoundEclipseSelectedTime = Double.NaN
	private var lastBoundLocalEclipseState: SolarEclipseState? = null
	private var lastDisplayedEclipseLatitude = Double.NaN
	private var lastDisplayedEclipseLongitude = Double.NaN
	private var lastEclipseButtonMapLoading: Boolean? = null
	private var lastEclipseButtonMapVisible: Boolean? = null
	private var lastEclipseButtonNightMode: Boolean? = null
	private var lastEclipseUiActive = false
	private val applySliderTimeRunnable = Runnable {
		val millis = pendingSliderTimeMillis ?: return@Runnable
		pendingSliderTimeMillis = null
		applySolarEclipseSliderTime(millis)
	}
	private val monitorEclipseMapMoveRunnable = object : Runnable {
		override fun run() {
			if (!keepSunCenteredForMapMove || !::starView.isInitialized) return
			if (app.osmandMap.mapView.animatedDraggingThread.isAnimating) {
				starView.postOnAnimation(this)
			} else {
				updateStarMap()
				keepSunCenteredForMapMove = false
				centerSunAtSelectedEclipseTime()
			}
		}
	}

	private var systemBottomInset: Int = 0
	private var systemTopInset: Int = 0
	private var systemLeftInset: Int = 0
	private var systemRightInset: Int = 0

	private data class EclipseRestoreState(
		val time: Time,
		val autoTime: Boolean,
		val cameraState: StarViewCameraState,
		val arEnabled: Boolean,
		val cameraEnabled: Boolean,
		val manualAzimuth: Boolean,
		val timeEditorVisible: Boolean,
		val regularMapVisible: Boolean,
		val mapLatitude: Double,
		val mapLongitude: Double,
		val mapZoom: Int,
		val mapZoomFloatPart: Float,
		val mapRatioCustom: Boolean,
		val mapRatioX: Float,
		val mapRatioY: Float
	)

	private val backPressedCallback = object : OnBackPressedCallback(false) {
			override fun handleOnBackPressed() {
			if (::bottomSheetBehavior.isInitialized && bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
				hideBottomSheet()
				if (childFragmentManager.backStackEntryCount > 0) {
					childFragmentManager.popBackStack()
				}
			} else if (childFragmentManager.backStackEntryCount > 0) {
				childFragmentManager.popBackStack()
			} else if (isSolarEclipseModeActive()) {
				exitSolarEclipseMode()
			} else {
				isEnabled = false
				requireActivity().onBackPressedDispatcher.onBackPressed()
				isEnabled = true
			}
		}
	}

	private val autoTimeUpdateRunnable = object : Runnable {
		override fun run() {
			if (view == null || !isResumed || !isTimeAutoUpdateEnabled()) return
			viewModel.resetTime()
			scheduleAutoTimeUpdate()
		}
	}

	companion object {
		val TAG: String = StarMapFragment::class.java.simpleName
		private val LOG = LoggerFactory.getLogger(TAG)
		private val RED_FILTER_MATRIX = ColorMatrix(
			floatArrayOf(
				0.33f, 0.33f, 0.33f, 0f, 0f,
				0f, 0f, 0f, 0f, 0f,
				0f, 0f, 0f, 0f, 0f,
				0f, 0f, 0f, 1f, 0f
			)
		)

		private const val AUTO_TIME_UPDATE_INTERVAL_MS = 60_000L
		private const val MAX_MAGNITUDE = 7.0f
		private const val ECLIPSE_MAP_FIT_HALF_FRACTION = 0.4
		private const val STATE_ECLIPSE_ACTIVE = "eclipse_active"
		private const val STATE_ECLIPSE_TIME_UT = "eclipse_time_ut"
		private const val STATE_ECLIPSE_AUTO_TIME = "eclipse_auto_time"
		private const val STATE_ECLIPSE_CAMERA_AZ = "eclipse_camera_az"
		private const val STATE_ECLIPSE_CAMERA_ALT = "eclipse_camera_alt"
		private const val STATE_ECLIPSE_CAMERA_FOV = "eclipse_camera_fov"
		private const val STATE_ECLIPSE_CAMERA_2D = "eclipse_camera_2d"
		private const val STATE_ECLIPSE_CAMERA_PAN_X = "eclipse_camera_pan_x"
		private const val STATE_ECLIPSE_CAMERA_PAN_Y = "eclipse_camera_pan_y"
		private const val STATE_ECLIPSE_CAMERA_ROLL = "eclipse_camera_roll"
		private const val STATE_ECLIPSE_AR = "eclipse_ar"
		private const val STATE_ECLIPSE_CAMERA_OVERLAY = "eclipse_camera_overlay"
		private const val STATE_ECLIPSE_MANUAL_AZIMUTH = "eclipse_manual_azimuth"
		private const val STATE_ECLIPSE_TIME_EDITOR = "eclipse_time_editor"
		private const val STATE_ECLIPSE_MAP_VISIBLE = "eclipse_map_visible"
		private const val STATE_ECLIPSE_CURRENT_MAP_VISIBLE = "eclipse_current_map_visible"
		private const val STATE_ECLIPSE_MAP_SHOWN = "eclipse_map_shown"
		private const val STATE_ECLIPSE_MAP_LAT = "eclipse_map_lat"
		private const val STATE_ECLIPSE_MAP_LON = "eclipse_map_lon"
		private const val STATE_ECLIPSE_MAP_ZOOM = "eclipse_map_zoom"
		private const val STATE_ECLIPSE_MAP_ZOOM_PART = "eclipse_map_zoom_part"
		private const val STATE_ECLIPSE_MAP_RATIO_CUSTOM = "eclipse_map_ratio_custom"
		private const val STATE_ECLIPSE_MAP_RATIO_X = "eclipse_map_ratio_x"
		private const val STATE_ECLIPSE_MAP_RATIO_Y = "eclipse_map_ratio_y"
		private const val STATE_ECLIPSE_ACTIVE_CAMERA_AZ = "eclipse_active_camera_az"
		private const val STATE_ECLIPSE_ACTIVE_CAMERA_ALT = "eclipse_active_camera_alt"
		private const val STATE_ECLIPSE_ACTIVE_CAMERA_FOV = "eclipse_active_camera_fov"
		private const val STATE_ECLIPSE_ACTIVE_CAMERA_2D = "eclipse_active_camera_2d"
		private const val STATE_ECLIPSE_ACTIVE_CAMERA_PAN_X = "eclipse_active_camera_pan_x"
		private const val STATE_ECLIPSE_ACTIVE_CAMERA_PAN_Y = "eclipse_active_camera_pan_y"
		private const val STATE_ECLIPSE_ACTIVE_CAMERA_ROLL = "eclipse_active_camera_roll"

		@JvmStatic
		fun applyRedFilterToViews(enabled: Boolean, vararg views: View?) {
			val layerType = if (enabled) View.LAYER_TYPE_HARDWARE else View.LAYER_TYPE_NONE
			val paint = if (enabled) {
				Paint().apply { colorFilter = ColorMatrixColorFilter(RED_FILTER_MATRIX) }
			} else {
				null
			}
			views.forEach { view ->
				view?.setLayerType(layerType, paint)
			}
		}

		fun showInstance(manager: FragmentManager) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				manager.beginTransaction()
					.replace(R.id.fragmentContainer, StarMapFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss()
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState?.getBoolean(STATE_ECLIPSE_ACTIVE) == true) {
			eclipseRestoreState = EclipseRestoreState(
				time = Time(savedInstanceState.getDouble(STATE_ECLIPSE_TIME_UT)),
				autoTime = savedInstanceState.getBoolean(STATE_ECLIPSE_AUTO_TIME),
				cameraState = StarViewCameraState(
					azimuth = savedInstanceState.getDouble(STATE_ECLIPSE_CAMERA_AZ),
					altitude = savedInstanceState.getDouble(STATE_ECLIPSE_CAMERA_ALT),
					viewAngle = savedInstanceState.getDouble(STATE_ECLIPSE_CAMERA_FOV),
					is2DMode = savedInstanceState.getBoolean(STATE_ECLIPSE_CAMERA_2D),
					panX = savedInstanceState.getFloat(STATE_ECLIPSE_CAMERA_PAN_X),
					panY = savedInstanceState.getFloat(STATE_ECLIPSE_CAMERA_PAN_Y),
					roll = savedInstanceState.getDouble(STATE_ECLIPSE_CAMERA_ROLL)
				),
				arEnabled = savedInstanceState.getBoolean(STATE_ECLIPSE_AR),
				cameraEnabled = savedInstanceState.getBoolean(STATE_ECLIPSE_CAMERA_OVERLAY),
				manualAzimuth = savedInstanceState.getBoolean(STATE_ECLIPSE_MANUAL_AZIMUTH),
				timeEditorVisible = savedInstanceState.getBoolean(STATE_ECLIPSE_TIME_EDITOR),
				regularMapVisible = savedInstanceState.getBoolean(STATE_ECLIPSE_MAP_VISIBLE),
				mapLatitude = savedInstanceState.getDouble(STATE_ECLIPSE_MAP_LAT),
				mapLongitude = savedInstanceState.getDouble(STATE_ECLIPSE_MAP_LON),
				mapZoom = savedInstanceState.getInt(STATE_ECLIPSE_MAP_ZOOM),
				mapZoomFloatPart = savedInstanceState.getFloat(STATE_ECLIPSE_MAP_ZOOM_PART),
				mapRatioCustom = savedInstanceState.getBoolean(STATE_ECLIPSE_MAP_RATIO_CUSTOM),
				mapRatioX = savedInstanceState.getFloat(STATE_ECLIPSE_MAP_RATIO_X, 0.5f),
				mapRatioY = savedInstanceState.getFloat(STATE_ECLIPSE_MAP_RATIO_Y, 0.5f)
			)
			regularMapVisible = savedInstanceState.getBoolean(STATE_ECLIPSE_CURRENT_MAP_VISIBLE)
			eclipseMapShown = savedInstanceState.getBoolean(STATE_ECLIPSE_MAP_SHOWN)
			if (savedInstanceState.containsKey(STATE_ECLIPSE_ACTIVE_CAMERA_FOV)) {
				restoredActiveEclipseCameraState = StarViewCameraState(
					azimuth = savedInstanceState.getDouble(STATE_ECLIPSE_ACTIVE_CAMERA_AZ),
					altitude = savedInstanceState.getDouble(STATE_ECLIPSE_ACTIVE_CAMERA_ALT),
					viewAngle = savedInstanceState.getDouble(STATE_ECLIPSE_ACTIVE_CAMERA_FOV),
					is2DMode = savedInstanceState.getBoolean(STATE_ECLIPSE_ACTIVE_CAMERA_2D),
					panX = savedInstanceState.getFloat(STATE_ECLIPSE_ACTIVE_CAMERA_PAN_X),
					panY = savedInstanceState.getFloat(STATE_ECLIPSE_ACTIVE_CAMERA_PAN_Y),
					roll = savedInstanceState.getDouble(STATE_ECLIPSE_ACTIVE_CAMERA_ROLL)
				)
			}
		}
		requireActivity().onBackPressedDispatcher.addCallback(this, backPressedCallback)
		childFragmentManager.addOnBackStackChangedListener {
			updateBackPressedCallback()
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		val restore = eclipseRestoreState ?: return
		outState.putBoolean(STATE_ECLIPSE_ACTIVE, true)
		outState.putDouble(STATE_ECLIPSE_TIME_UT, restore.time.ut)
		outState.putBoolean(STATE_ECLIPSE_AUTO_TIME, restore.autoTime)
		outState.putDouble(STATE_ECLIPSE_CAMERA_AZ, restore.cameraState.azimuth)
		outState.putDouble(STATE_ECLIPSE_CAMERA_ALT, restore.cameraState.altitude)
		outState.putDouble(STATE_ECLIPSE_CAMERA_FOV, restore.cameraState.viewAngle)
		outState.putBoolean(STATE_ECLIPSE_CAMERA_2D, restore.cameraState.is2DMode)
		outState.putFloat(STATE_ECLIPSE_CAMERA_PAN_X, restore.cameraState.panX)
		outState.putFloat(STATE_ECLIPSE_CAMERA_PAN_Y, restore.cameraState.panY)
		outState.putDouble(STATE_ECLIPSE_CAMERA_ROLL, restore.cameraState.roll)
		outState.putBoolean(STATE_ECLIPSE_AR, restore.arEnabled)
		outState.putBoolean(STATE_ECLIPSE_CAMERA_OVERLAY, restore.cameraEnabled)
		outState.putBoolean(STATE_ECLIPSE_MANUAL_AZIMUTH, restore.manualAzimuth)
		outState.putBoolean(STATE_ECLIPSE_TIME_EDITOR, restore.timeEditorVisible)
		outState.putBoolean(STATE_ECLIPSE_MAP_VISIBLE, restore.regularMapVisible)
		outState.putBoolean(STATE_ECLIPSE_CURRENT_MAP_VISIBLE, regularMapVisible)
		outState.putBoolean(STATE_ECLIPSE_MAP_SHOWN, eclipseMapShown)
		outState.putDouble(STATE_ECLIPSE_MAP_LAT, restore.mapLatitude)
		outState.putDouble(STATE_ECLIPSE_MAP_LON, restore.mapLongitude)
		outState.putInt(STATE_ECLIPSE_MAP_ZOOM, restore.mapZoom)
		outState.putFloat(STATE_ECLIPSE_MAP_ZOOM_PART, restore.mapZoomFloatPart)
		outState.putBoolean(STATE_ECLIPSE_MAP_RATIO_CUSTOM, restore.mapRatioCustom)
		outState.putFloat(STATE_ECLIPSE_MAP_RATIO_X, restore.mapRatioX)
		outState.putFloat(STATE_ECLIPSE_MAP_RATIO_Y, restore.mapRatioY)
		val activeCamera = if (::starView.isInitialized) {
			starView.captureCameraState()
		} else {
			restoredActiveEclipseCameraState
		}
		if (activeCamera != null) {
			outState.putDouble(STATE_ECLIPSE_ACTIVE_CAMERA_AZ, activeCamera.azimuth)
			outState.putDouble(STATE_ECLIPSE_ACTIVE_CAMERA_ALT, activeCamera.altitude)
			outState.putDouble(STATE_ECLIPSE_ACTIVE_CAMERA_FOV, activeCamera.viewAngle)
			outState.putBoolean(STATE_ECLIPSE_ACTIVE_CAMERA_2D, activeCamera.is2DMode)
			outState.putFloat(STATE_ECLIPSE_ACTIVE_CAMERA_PAN_X, activeCamera.panX)
			outState.putFloat(STATE_ECLIPSE_ACTIVE_CAMERA_PAN_Y, activeCamera.panY)
			outState.putDouble(STATE_ECLIPSE_ACTIVE_CAMERA_ROLL, activeCamera.roll)
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val view = themedInflater.inflate(R.layout.fragment_star_map, container, false)

		val app = requireActivity().application as OsmandApplication
		viewModel = ViewModelProvider(this,
			StarObjectsViewModel.Factory(app, astroSettings, dataProvider))[StarObjectsViewModel::class.java]

		mainLayout = view.findViewById(R.id.main_layout)
		starView = view.findViewById(R.id.star_view)
		starView.useUnrefractedSolarPositions = eclipseRestoreState != null
		mapControlsContainer = view.findViewById(R.id.map_controls_container)
		timeSelectionView = view.findViewById(R.id.time_selection_view)
		timeControlBtn = view.findViewById(R.id.time_control_button)
		resetTimeButton = view.findViewById(R.id.reset_time_button)

		arModeButton = view.findViewById(R.id.ar_mode_button)
		cameraButton = view.findViewById(R.id.camera_button)
		transparencySlider = view.findViewById(R.id.transparency_slider)
		sliderContainer = view.findViewById(R.id.slider_container)
		resetFovButton = view.findViewById(R.id.reset_fov_button)

		magnitudeFilterButton = view.findViewById(R.id.magnitude_filter_button)
		magnitudeFilterIcon = view.findViewById(R.id.magnitude_filter_icon)
		magnitudeFilterText = view.findViewById(R.id.magnitude_filter_text)
		magnitudeSliderCard = view.findViewById(R.id.magnitude_slider_card)
		magnitudeSlider = view.findViewById(R.id.magnitude_slider)
		magnitudeSliderValue = view.findViewById(R.id.magnitude_slider_value)

		magnitudeFilterButton.setOnClickListener {
			magnitudeSliderCard.isVisible = !magnitudeSliderCard.isVisible
			updateMagnitudeFilterTheme()
		}

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
				val magnitudeStr = String.format(Locale.getDefault(), "%.1f", magnitude)
				magnitudeFilterText.text = magnitudeStr
				magnitudeSliderValue.text = magnitudeStr
				if (fromUser) {
					starView.magnitudeFilter = magnitude.toFloat()
					starView.invalidate()
				}
			}
			override fun onStartTrackingTouch(seekBar: SeekBar?) {}
			override fun onStopTrackingTouch(seekBar: SeekBar?) {}
		})

		updateArModeUI(arModeHelper.isArModeEnabled)
		updateCameraUI(cameraHelper.isCameraOverlayEnabled)

		timeControlCard = view.findViewById(R.id.time_control_card)
		timeControlCard.apply {
			updateTimeControlTheme(timeControlCard, timeControlBtn, resetTimeButton)
		}

		closeButton = view.findViewById(R.id.close_button)
		closeButton.apply {
			setOnClickListener {
				if (isSolarEclipseModeActive()) exitSolarEclipseMode()
				backPressedCallback.isEnabled = false
				requireActivity().onBackPressedDispatcher.onBackPressed()
			}
		}
		searchButton = view.findViewById(R.id.search_button)
		searchButton.apply {
			setOnClickListener {
				showSearchDialog()
			}
		}
		settingsButton = view.findViewById(R.id.settings_button)
		settingsButton.apply {
			setOnClickListener {
				val sheet = AstroConfigureViewBottomSheet()
				sheet.show(
					childFragmentManager,
					AstroConfigureViewBottomSheet.TAG
				)
			}
		}

		timeControlBtn.setOnClickListener {
			timeSelectionView.isVisible = !timeSelectionView.isVisible
			updateTimeControlTheme(timeControlCard, timeControlBtn, resetTimeButton)
		}

		resetTimeButton.setOnClickListener {
			setTimeAutoUpdateEnabled(true)
		}

		eclipseCard = view.findViewById(R.id.solar_eclipse_timeline_card)
		eclipseLoading = eclipseCard.findViewById(R.id.eclipse_loading)
		eclipseErrorContainer = eclipseCard.findViewById(R.id.eclipse_error_container)
		eclipseContent = eclipseCard.findViewById(R.id.eclipse_content)
		eclipsePrevious = eclipseCard.findViewById(R.id.eclipse_previous)
		eclipseNext = eclipseCard.findViewById(R.id.eclipse_next)
		eclipseClose = eclipseCard.findViewById(R.id.eclipse_close)
		eclipseKind = eclipseCard.findViewById(R.id.eclipse_kind)
		eclipseEventDate = eclipseCard.findViewById(R.id.eclipse_event_date)
		eclipseCurrentTime = eclipseCard.findViewById(R.id.eclipse_current_time)
		eclipseCurrentDateZone = eclipseCard.findViewById(R.id.eclipse_current_date_zone)
		eclipseLocalStatus = eclipseCard.findViewById(R.id.eclipse_local_status)
		eclipseMapCenterLocation = eclipseCard.findViewById(R.id.eclipse_map_center_location)
		eclipseTimeline = eclipseCard.findViewById(R.id.eclipse_timeline)
		eclipseStartTime = eclipseCard.findViewById(R.id.eclipse_start_time)
		eclipseMaximumTime = eclipseCard.findViewById(R.id.eclipse_maximum_time)
		eclipseEndTime = eclipseCard.findViewById(R.id.eclipse_end_time)
		eclipseFitPath = eclipseCard.findViewById(R.id.eclipse_fit_path)
		eclipseToggleMap = eclipseCard.findViewById(R.id.eclipse_toggle_map)
		eclipseRetry = eclipseCard.findViewById(R.id.eclipse_retry)
		resetEclipseUiCache()
		eclipsePrevious.setOnClickListener {
			prepareForEclipseNavigation()
			viewModel.loadPreviousSolarEclipse()
		}
		eclipseNext.setOnClickListener {
			prepareForEclipseNavigation()
			viewModel.loadNextSolarEclipse()
		}
		eclipseClose.setOnClickListener { exitSolarEclipseMode() }
		eclipseRetry.setOnClickListener {
			viewModel.retrySolarEclipseSearch(viewModel.currentTime.value ?: starView.currentTime)
		}
		eclipseFitPath.setOnClickListener { viewModel.requestSolarEclipseShadowPoint() }
		eclipseToggleMap.setOnClickListener {
			if (regularMapVisible) hideEclipseMap() else showEclipseMapWithoutMoving()
		}
		updateEclipseMapButtons(false)
		eclipseTimeline.setOnTimeChangedListener { millis, fromUser ->
			if (fromUser) scheduleSolarEclipseSliderTime(millis)
		}
		val rtl = view.layoutDirection == View.LAYOUT_DIRECTION_RTL
		eclipsePrevious.scaleX = if (rtl) 1f else -1f
		eclipseNext.scaleX = if (rtl) -1f else 1f

		view.findViewById<StarCompassButton>(R.id.star_map_compass_button)?.let {
			it.onSingleTap = { setAzimuth(0.0, true)}
			it.setMapActivity(requireMapActivity())
			compassButton = it
		}

		val initialRegularMapVisibility = if (eclipseRestoreState != null) {
			regularMapVisible
		} else {
			astroSettings.getCommonConfig().showRegularMap
		}
		updateRegularMapVisibility(initialRegularMapVisibility)
		astroSettings.getStarMapConfig().let { config ->
			starView.showAzimuthalGrid = config.showAzimuthalGrid
			starView.showEquatorialGrid = config.showEquatorialGrid
			starView.showEclipticLine = config.showEclipticLine
			starView.showMeridianLine = config.showMeridianLine
			starView.showEquatorLine = config.showEquatorLine
			starView.showGalacticLine = config.showGalacticLine
			starView.showFavorites = config.showFavorites
			starView.showDirections = config.showDirections
			starView.showCelestialPaths = config.showCelestialPaths
			starView.showRedFilter = config.showRedFilter
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
			starView.magnitudeFilter = config.magnitudeFilter?.toFloat()
			starView.is2DMode = config.is2DMode
			if (config.magnitudeFilter != null) {
				val text = String.format(Locale.getDefault(), "%.1f", config.magnitudeFilter)
				magnitudeFilterText.text = text
				magnitudeSliderValue.text = text
			}
			updateRedMode(config.showRedFilter)
		}

		updateStarMap(true)

		previousAltitude = starView.getAltitude()
		previousAzimuth = starView.getAzimuth()
		previousViewAngle = starView.getViewAngle()
		apply2DMode(starView.is2DMode)

		bottomSheetContainer = view.findViewById(R.id.bottom_sheet_container)
		bottomSheetBehavior = (BottomSheetBehavior.from(bottomSheetContainer) as? AstroBottomSheetBehavior<View>)
			?: throw IllegalStateException("bottom_sheet_container must use AstroBottomSheetBehavior")
		bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
		bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
			override fun onStateChanged(bottomSheet: View, newState: Int) {
				updateMapControlsVisibility()
				if (newState == BottomSheetBehavior.STATE_HIDDEN) {
					clearSelectedObject()
				}
				updateBackPressedCallback()
			}
			override fun onSlide(bottomSheet: View, slideOffset: Float) {}
		})
		updateMapControlsVisibility()
		applyRedFilterToViews(starView.showRedFilter, bottomSheetContainer)

		return view
	}

	override fun onApplyInsets(insets: WindowInsetsCompat) {
		super.onApplyInsets(insets)
		val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
		val sysBars = InsetsUtils.getSysBars(requireContext(), insets)
		
		systemBottomInset = maxOf(sysBars?.bottom ?: 0, cutout.bottom)
		systemTopInset = maxOf(sysBars?.top ?: 0, cutout.top)
		systemLeftInset = maxOf(sysBars?.left ?: 0, cutout.left)
		systemRightInset = maxOf(sysBars?.right ?: 0, cutout.right)

		applyBottomInsets()
		applyTopInsets()
		applySideInsets()
	}

	private fun applyBottomInsets() {
		applyBottomWindowInsets(timeControlCard, regularMapVisible)
		if (::eclipseCard.isInitialized) applyBottomWindowInsets(eclipseCard, regularMapVisible)
		val eclipseOffset = if (::eclipseCard.isInitialized && eclipseCard.isVisible) {
			eclipseCard.height + resources.getDimensionPixelSize(R.dimen.content_padding)
		} else 0
		applyBottomWindowInsets(searchButton, regularMapVisible, eclipseOffset)
		applyBottomWindowInsets(settingsButton, regularMapVisible, eclipseOffset)
	}

	private fun applyTopInsets() {
		applyTopWindowInsets(compassButton)
		applyTopWindowInsets(closeButton)
	}

	private fun applySideInsets() {
		applySideWindowInsets(compassButton, true)
		applySideWindowInsets(closeButton, false)
		applySideWindowInsets(settingsButton, false)
	}

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = super.getInsetTargets()
		collection.removeType(InsetTarget.Type.ROOT_INSET)
		return collection
	}

	private fun handleSearchObjectSelected(obj: SkyObject) {
		manualAzimuth = true
		selectedObject = obj
		starView.setSelectedObject(obj, center = true, animate = true)
		showObjectInfo(obj)
	}

	internal fun showSearchDialog(initialCatalogWid: String? = null) {
		clearPreviousSearchDialog()
		StarMapSearchDialogFragment.newInstance(initialCatalogWid).apply {
			onObjectSelected = { obj ->
				handleSearchObjectSelected(obj)
			}
		}.show(childFragmentManager, StarMapSearchDialogFragment.TAG)
	}

	private fun clearPreviousSearchDialog() {
		childFragmentManager.popBackStackImmediate(
			StarMapSearchDialogFragment.TAG,
			FragmentManager.POP_BACK_STACK_INCLUSIVE
		)
		val existingDialog =
			childFragmentManager.findFragmentByTag(StarMapSearchDialogFragment.TAG) as? StarMapSearchDialogFragment
		existingDialog?.dismissAllowingStateLoss()
	}

	private fun applyBottomWindowInsets(view: View, reset: Boolean = false, extraBottom: Int = 0) {
		val baseMarginBottom = view.resources.getDimensionPixelSize(R.dimen.content_padding)
		val bottomMargin = baseMarginBottom + extraBottom + if (reset) 0 else systemBottomInset
		val layoutParams = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
		if (layoutParams.bottomMargin == bottomMargin) return
		view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			this.bottomMargin = bottomMargin
		}
	}

	private fun applyTopWindowInsets(view: View) {
		val baseMarginTop = view.resources.getDimensionPixelSize(R.dimen.content_padding)
		if (systemTopInset > 0) {
			view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
				topMargin = baseMarginTop + systemTopInset
			}
			return
		}
		view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			topMargin = baseMarginTop
		}
	}

	private fun applySideWindowInsets(view: View, isLeft: Boolean) {
		val baseMargin = view.resources.getDimensionPixelSize(R.dimen.content_padding)
		view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			if (isLeft) {
				marginStart = baseMargin + systemLeftInset
			} else {
				marginEnd = baseMargin + systemRightInset
			}
		}
	}

	private fun updateArModeUI(enabled: Boolean) {
		if (enabled) {
			arModeButton.setColorFilter(Color.BLUE)
			cameraButton.visibility = View.VISIBLE
		} else {
			arModeButton.setColorFilter(ColorUtilities.getMapButtonIconColor(requireContext(), nightMode))
			if (cameraHelper.isCameraOverlayEnabled) cameraHelper.toggleCameraOverlay()
			cameraButton.visibility = View.GONE
			updateCameraUI(false)
		}
	}

	private fun updateCameraUI(enabled: Boolean) {
		if (enabled) {
			cameraButton.setColorFilter(Color.BLUE)
			sliderContainer.visibility = View.VISIBLE
			resetFovButton.visibility = View.VISIBLE
		} else {
			cameraButton.setColorFilter(ColorUtilities.getMapButtonIconColor(requireContext(), nightMode))
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
		updateNightMode()
		updateButtonsNightMode()
		updateStarMap(!isSolarEclipseModeActive())
		app.locationProvider.addLocationListener(this)
		app.locationProvider.addCompassListener(this)
		app.osmandMap.mapView.addMapLocationListener(this)
		arModeHelper.onResume(); cameraHelper.onResume()
		val mapActivity = requireMapActivity()
		mapActivity.disableDrawer()
		updateWidgetsVisibility(mapActivity, View.GONE)
		if (regularMapVisible) updateRegularMapVisibility(true)
		mapActivity.refreshMap()
		updateBackPressedCallback()
		updateMapControlsVisibility()
		restoredActiveEclipseCameraState?.let { state ->
			apply2DMode(state.is2DMode)
			starView.restoreCameraState(state)
			lastFocusedEclipseRequestId = viewModel.solarEclipseModeState.value?.requestId ?: -1L
			restoredActiveEclipseCameraState = null
			eclipseCard.post { centerSunAtSelectedEclipseTime() }
		}
		if (isTimeAutoUpdateEnabled()) {
			viewModel.resetTime()
			scheduleAutoTimeUpdate()
		}
	}

	private fun updateButtonsNightMode() {
		if (view == null) return

		val currentNightMode = nightMode
		arModeButton.nightMode = currentNightMode
		cameraButton.nightMode = currentNightMode
		resetFovButton.nightMode = currentNightMode
		closeButton.nightMode = currentNightMode
		searchButton.nightMode = currentNightMode
		settingsButton.nightMode = currentNightMode
		compassButton.setNightMode(currentNightMode)

		updateMagnitudeFilterTheme()

		if (::timeControlCard.isInitialized) {
			updateTimeControlTheme(timeControlCard, timeControlBtn, resetTimeButton)
		}
		if (::eclipseFitPath.isInitialized) {
			updateEclipseMapButtons(viewModel.solarEclipseModeState.value?.mapLoading == true)
		}
	}

	override fun onPause() {
		super.onPause()
		stopAutoTimeUpdate()
		saveStarMapSettings()
		app.locationProvider.removeLocationListener(this)
		app.locationProvider.removeCompassListener(this)
		app.osmandMap.mapView.removeMapLocationListener(this)
		arModeHelper.onPause(); cameraHelper.onPause()
		val mapActivity = requireMapActivity()
		mapActivity.resetMapViewPaddings()
		eclipseRestoreState?.let { restoreEclipseMapDisplayRatio(it) }
		mapActivity.enableDrawer()
		updateWidgetsVisibility(mapActivity, View.VISIBLE)
		mapActivity.refreshMap()
	}

	override fun onDestroyView() {
		stopAutoTimeUpdate()
		if (::eclipseTimeline.isInitialized) {
			eclipseTimeline.removeCallbacks(applySliderTimeRunnable)
		}
		if (::starView.isInitialized) {
			starView.removeCallbacks(monitorEclipseMapMoveRunnable)
		}
		if (::viewModel.isInitialized) {
			astronomyPlugin.setSolarEclipseMapData(false, null, null, null, null)
		}
		eclipseRestoreState?.let { restoreEclipseMapDisplayRatio(it) }
		super.onDestroyView()
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
		compassButton.update(-azimuth.toFloat(), animate)
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
				if (isAdded && view != null && ::starView.isInitialized) {
					if (needsAzimuthUpdate) {
						val bearing = location.bearing.toDouble()
						setAzimuth(bearing, true)
					}
					if (needsLocationUpdate) {
						updateStarMap()
						lastUpdatedLocation = Location(location)
					}
				}
			}
		}
	}

	override fun locationChanged(p0: Double, p1: Double, p2: Any?) {
		if (!app.mapViewTrackingUtilities.isMapLinkedToLocation) {
			app.runInUIThread {
				if (isAdded && view != null && ::starView.isInitialized) {
					updateStarMap()
				}
			}
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		cameraHelper.onRequestPermissionsResult(requestCode, grantResults)
	}

	private fun updateRegularMapVisibility(visible: Boolean) {
		regularMapVisible = visible
		val mapActivity = requireMapActivity()
		val mapView = app.osmandMap.mapView
		val preservedCenter = mapView.rotatedTileBox.centerLatLon
		var mapHeightPx = 0
		if (visible) {
			val mapHeight =
				if (InsetsUtils.isLandscape(app)) REGULAR_MAP_HEIGHT_LANDSCAPE
				else REGULAR_MAP_HEIGHT
			mapHeightPx = dpToPx(mapHeight)
			mainLayout.setPadding(0, 0, 0, mapHeightPx)
			val display = AndroidUtils.getDisplay(app)
			val screenDimensions = Point(0, 0)
			display.getSize(screenDimensions)
			mapActivity.setMapViewPaddings(0, screenDimensions.y - mapHeightPx, 0, 0)
			mapActivity.refreshMap()
		} else {
			mainLayout.setPadding(0, 0, 0, 0)
			mapActivity.resetMapViewPaddings()
		}
		if (isSolarEclipseModeActive()) {
			mapView.view?.doOnPreDraw {
				if (regularMapVisible != visible || !isAdded || view == null) return@doOnPreDraw
				reanchorEclipseMapCenter(
					preservedCenter.latitude,
					preservedCenter.longitude,
					visible,
					mapHeightPx
				)
			}
		}
		applyBottomInsets()
		if (isSolarEclipseModeActive() && ::eclipseCard.isInitialized) {
			eclipseCard.post { centerSunAtSelectedEclipseTime() }
		}
	}

	private fun reanchorEclipseMapCenter(
		latitude: Double,
		longitude: Double,
		mapVisible: Boolean,
		mapHeightPx: Int
	) {
		if (!isSolarEclipseModeActive()) return
		val mapView = app.osmandMap.mapView
		val rendererView = mapView.view ?: return
		val ratioY = if (mapVisible && mapHeightPx > 0) {
			val rendererLocation = IntArray(2)
			val mainLocation = IntArray(2)
			rendererView.getLocationOnScreen(rendererLocation)
			mainLayout.getLocationOnScreen(mainLocation)
			val rendererTop = rendererLocation[1].toFloat()
			val rendererBottom = rendererTop + rendererView.height
			val visibleTop = max(
				rendererTop,
				(mainLocation[1] + mainLayout.height - mapHeightPx).toFloat()
			)
			val visibleCenter = (visibleTop + rendererBottom) / 2f
			((visibleCenter - rendererTop) / rendererView.height.coerceAtLeast(1))
				.coerceIn(0f, 1f)
		} else {
			0.5f
		}
		app.mapViewTrackingUtilities.mapDisplayPositionManager
			.setCustomMapRatio(0.5f, ratioY)
		mapView.setLatLon(latitude, longitude, 0.5f, ratioY)
		mapView.mapRenderer?.let { renderer ->
			val target31 = NativeUtilities.calculateTarget31(renderer, latitude, longitude, false)
			val targetPixel = PointI(
				(rendererView.width * 0.5f).toInt(),
				(rendererView.height * ratioY).toInt()
			)
			renderer.setMapTarget(targetPixel, target31)
			mapView.refreshMap()
		}
	}

	private fun restoreEclipseMapDisplayRatio(restore: EclipseRestoreState) {
		val manager = app.mapViewTrackingUtilities.mapDisplayPositionManager
		if (restore.mapRatioCustom) {
			manager.setCustomMapRatio(restore.mapRatioX, restore.mapRatioY)
		} else {
			manager.restoreMapRatio()
		}
	}

	fun applyRedFilter(enabled: Boolean) {
		starView.showRedFilter = enabled
		updateRedMode(enabled)
		saveStarMapSettings()
	}

	fun setRegularMapVisibility(enabled: Boolean) {
		updateRegularMapVisibility(enabled)
		if (isSolarEclipseModeActive()) {
			eclipseMapShown = enabled
			if (!enabled) pendingEclipseMapFit = false
			updateEclipseMapButtons(false)
		}
		saveCommonSettings()
	}

	private fun saveCommonSettings() {
		val config = CommonConfig(
			showRegularMap = regularMapVisible,
		)
		astroSettings.setCommonConfig(config)
	}

	private fun saveStarMapSettings() {
		astroSettings.updateStarMapConfig { current ->
			current.copy(
				showAzimuthalGrid = starView.showAzimuthalGrid,
				showEquatorialGrid = starView.showEquatorialGrid,
				showEclipticLine = starView.showEclipticLine,
				showMeridianLine = starView.showMeridianLine,
				showEquatorLine = starView.showEquatorLine,
				showGalacticLine = starView.showGalacticLine,
				showFavorites = starView.showFavorites,
				showDirections = starView.showDirections,
				showCelestialPaths = starView.showCelestialPaths,
				showRedFilter = starView.showRedFilter,
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
		}
	}

	fun setStarMapSettings(newConfig: StarMapConfig) {
		starView.showAzimuthalGrid = newConfig.showAzimuthalGrid
		starView.showEquatorialGrid = newConfig.showEquatorialGrid
		starView.showEclipticLine = newConfig.showEclipticLine
		starView.showMeridianLine = newConfig.showMeridianLine
		starView.showEquatorLine = newConfig.showEquatorLine
		starView.showGalacticLine = newConfig.showGalacticLine
		starView.showFavorites = newConfig.showFavorites
		starView.showDirections = newConfig.showDirections
		starView.showCelestialPaths = newConfig.showCelestialPaths
		starView.showRedFilter = newConfig.showRedFilter

		starView.showSun = newConfig.showSun
		starView.showMoon = newConfig.showMoon
		starView.showPlanets = newConfig.showPlanets

		starView.showConstellations = newConfig.showConstellations
		starView.showStars = newConfig.showStars
		starView.showGalaxies = newConfig.showGalaxies
		starView.showNebulae = newConfig.showNebulae
		starView.showOpenClusters = newConfig.showOpenClusters
		starView.showGlobularClusters = newConfig.showGlobularClusters
		starView.showGalaxyClusters = newConfig.showGalaxyClusters
		starView.showBlackHoles = newConfig.showBlackHoles

		starView.updateVisibility()

		astroSettings.updateStarMapConfig { current ->
			newConfig.copy(
				favorites = current.favorites,
				directions = current.directions,
				celestialPaths = current.celestialPaths
			)
		}
	}

	private fun updateWidgetsVisibility(activity: MapActivity, visibility: Int) {
		AndroidUiHelper.setVisibility(activity, visibility, R.id.map_left_widgets_panel,
			R.id.map_right_widgets_panel, R.id.map_center_info)
	}

	fun apply2DMode(is2D: Boolean) {
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
			cameraButton.visibility = if (arModeHelper.isArModeEnabled) View.VISIBLE else View.GONE
			arModeButton.visibility = View.VISIBLE
		}
		starView.invalidate()
	}

	private fun setupObservers() {
		viewModel.currentTime.observe(viewLifecycleOwner) { time ->
			starView.setDateTime(time, animate = !isSolarEclipseModeActive())
			getAstroContextMenuFragment()?.onTimeChanged()
		}
		viewModel.solarEclipseModeState.observe(viewLifecycleOwner) { state ->
			handleSolarEclipseModeState(state)
		}
		viewModel.currentCalendar.observe(viewLifecycleOwner) { calendar ->
			timeSelectionView.setDateTime(calendar)

			val now = Calendar.getInstance()
			val isToday = now.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
					now.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
			val locale = Locale.getDefault()
			val formatString = if (isToday) {
				"HH:mm"
			} else {
				val shortInstance = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale) as SimpleDateFormat
				shortInstance.toPattern().replace("y+".toRegex(), "yy")
			}
			val timeFormat = SimpleDateFormat(formatString, locale)
			timeControlBtn.text = timeFormat.format(calendar.time)
		}
		viewModel.isTimeAutoUpdateEnabled.observe(viewLifecycleOwner) { enabled ->
			resetTimeButton.visibility = if (enabled) View.GONE else View.VISIBLE
			if (enabled) {
				viewModel.resetTime()
				scheduleAutoTimeUpdate()
			} else {
				stopAutoTimeUpdate()
			}
		}
		viewModel.skyObjects.observe(viewLifecycleOwner) { objects ->
			solarEclipseSun = objects.firstOrNull { it.type == SkyObject.Type.SUN }
			starView.setSkyObjects(objects)
			viewModel.solarEclipseModeState.value
				?.takeIf { it.active && it.event != null }
				?.let { handleSolarEclipseModeState(it) }
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
				val text = String.format(Locale.getDefault(), "%.1f", filterToUse)
				magnitudeFilterText.text = text
				magnitudeSliderValue.text = text
			}
		}
		viewModel.constellations.observe(viewLifecycleOwner) { constellations ->
			starView.setConstellations(constellations)
		}
	}

	private fun setupListeners() {
		timeSelectionView.setOnDateTimeChangeListener { calendar ->
			setTimeAutoUpdateEnabled(false)
			viewModel.updateTime(calendar)
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
				selectedObject = constellation
				showObjectInfo(constellation)
			} else {
				if (selectedObject == null) hideBottomSheet()
			}
		}
		starView.onAnimationFinished = null
		starView.onAzimuthManualChangeListener = { azimuth ->
			if (!cameraHelper.isCameraOverlayEnabled) {
				if (arModeHelper.isArModeEnabled) arModeHelper.toggleArMode()
				manualAzimuth = true
				compassButton.update(-azimuth.toFloat())
			}
		}
		starView.onViewAngleChangeListener = { fov -> cameraHelper.updateCameraZoom(fov) }
	}

	private fun setTimeAutoUpdateEnabled(enabled: Boolean) {
		viewModel.setTimeAutoUpdateEnabled(enabled)
	}

	fun isSolarEclipseModeActive(): Boolean =
		eclipseRestoreState != null ||
			(::viewModel.isInitialized && viewModel.solarEclipseModeState.value?.active == true)

	fun toggleSolarEclipseMode() {
		if (isSolarEclipseModeActive()) exitSolarEclipseMode() else enterSolarEclipseMode()
	}

	private fun enterSolarEclipseMode() {
		if (eclipseRestoreState != null) return
		val mapView = app.osmandMap.mapView
		val displayPositionManager = app.mapViewTrackingUtilities.mapDisplayPositionManager
		val mapRatio = displayPositionManager.mapRatio
		val center = mapView.rotatedTileBox.centerLatLon
		val displayedTime = viewModel.currentTime.value ?: starView.currentTime
		eclipseRestoreState = EclipseRestoreState(
			time = displayedTime,
			autoTime = isTimeAutoUpdateEnabled(),
			cameraState = starView.captureCameraState(),
			arEnabled = arModeHelper.isArModeEnabled,
			cameraEnabled = cameraHelper.isCameraOverlayEnabled,
			manualAzimuth = manualAzimuth,
			timeEditorVisible = timeSelectionView.isVisible,
			regularMapVisible = regularMapVisible,
			mapLatitude = center.latitude,
			mapLongitude = center.longitude,
			mapZoom = mapView.zoom,
			mapZoomFloatPart = mapView.zoomFloatPart,
			mapRatioCustom = displayPositionManager.hasCustomMapRatio(),
			mapRatioX = mapRatio.x,
			mapRatioY = mapRatio.y
		)
		if (cameraHelper.isCameraOverlayEnabled) cameraHelper.toggleCameraOverlay()
		if (arModeHelper.isArModeEnabled) arModeHelper.toggleArMode(false)
		manualAzimuth = true
		eclipseMapShown = regularMapVisible
		pendingEclipseMapFit = false
		updateEclipseMapButtons(false)
		starView.useUnrefractedSolarPositions = true
		timeSelectionView.isVisible = false
		setTimeAutoUpdateEnabled(false)
		eclipseCard.isVisible = true
		timeControlCard.isVisible = false
		if (regularMapVisible) updateRegularMapVisibility(true)
		lastFocusedEclipseRequestId = -1L
		updateBackPressedCallback()
		eclipseCard.post { applyBottomInsets() }
		viewModel.enterSolarEclipseMode(starView.observer, displayedTime)
	}

	private fun exitSolarEclipseMode() {
		val restore = eclipseRestoreState ?: return
		eclipseRestoreState = null
		pendingSliderTimeMillis = null
		keepSunCenteredForMapMove = false
		eclipseMapShown = false
		pendingEclipseMapFit = false
		eclipseTimeline.removeCallbacks(applySliderTimeRunnable)
		if (::starView.isInitialized) starView.removeCallbacks(monitorEclipseMapMoveRunnable)
		viewModel.exitSolarEclipseMode()
		astronomyPlugin.setSolarEclipseMapData(false, null, null, null, null)
		starView.useUnrefractedSolarPositions = false
		eclipseCard.isVisible = false
		timeControlCard.isVisible = true
		apply2DMode(restore.cameraState.is2DMode)
		starView.restoreCameraState(restore.cameraState)
		manualAzimuth = restore.manualAzimuth
		viewModel.updateTime(restore.time)
		timeSelectionView.isVisible = restore.timeEditorVisible
		updateTimeControlTheme(timeControlCard, timeControlBtn, resetTimeButton)
		updateRegularMapVisibility(restore.regularMapVisible)
		restoreEclipseMapDisplayRatio(restore)
		saveCommonSettings()
		app.osmandMap.mapView.animatedDraggingThread.startMoving(
			restore.mapLatitude,
			restore.mapLongitude,
			restore.mapZoom,
			restore.mapZoomFloatPart
		)
		if (restore.arEnabled && !arModeHelper.isArModeEnabled) arModeHelper.toggleArMode(true)
		if (restore.cameraEnabled && !cameraHelper.isCameraOverlayEnabled) cameraHelper.toggleCameraOverlay()
		setTimeAutoUpdateEnabled(restore.autoTime)
		lastFocusedEclipseRequestId = -1L
		applyBottomInsets()
		updateBackPressedCallback()
	}

	private fun scheduleSolarEclipseSliderTime(millis: Long) {
		pendingSliderTimeMillis = millis
		eclipseTimeline.removeCallbacks(applySliderTimeRunnable)
		eclipseTimeline.postOnAnimation(applySliderTimeRunnable)
	}

	private fun applySolarEclipseSliderTime(millis: Long) {
		val time = Time(millis / 86_400_000.0 - 10_957.5)
		viewModel.selectSolarEclipseTime(time)
		findSun()?.let { sun ->
			val center = getSolarVisibleCenter()
			starView.showTimeAndCenterObject(time, sun, center.first, center.second)
		}
	}

	private fun prepareForEclipseNavigation() {
		manualAzimuth = true
		keepSunCenteredForMapMove = false
		starView.removeCallbacks(monitorEclipseMapMoveRunnable)
		app.osmandMap.mapView.animatedDraggingThread.stopAnimating()
		if (eclipseMapShown) {
			pendingEclipseMapFit = true
		}
	}

	private fun hideEclipseMap() {
		keepSunCenteredForMapMove = false
		starView.removeCallbacks(monitorEclipseMapMoveRunnable)
		app.osmandMap.mapView.animatedDraggingThread.stopAnimating()
		eclipseMapShown = false
		pendingEclipseMapFit = false
		updateRegularMapVisibility(false)
		updateEclipseMapButtons(false)
	}

	private fun showEclipseMapWithoutMoving() {
		keepSunCenteredForMapMove = false
		pendingEclipseMapFit = false
		eclipseMapShown = true
		updateRegularMapVisibility(true)
		updateEclipseMapButtons(false)
	}

	private fun updateEclipseMapButtons(mapLoading: Boolean) {
		if (!::eclipseFitPath.isInitialized || !::eclipseToggleMap.isInitialized) return
		if (lastEclipseButtonMapLoading == mapLoading &&
			lastEclipseButtonMapVisible == regularMapVisible &&
			lastEclipseButtonNightMode == nightMode
		) {
			return
		}
		lastEclipseButtonMapLoading = mapLoading
		lastEclipseButtonMapVisible = regularMapVisible
		lastEclipseButtonNightMode = nightMode
		UiUtilities.setupDialogButton(
			nightMode,
			eclipseFitPath,
			DialogButtonType.PRIMARY,
			getString(
				if (mapLoading) R.string.astro_show_eclipse_on_map_loading
				else R.string.astro_fit_eclipse_path
			),
			R.drawable.ic_show_on_map_outlined
		)
		UiUtilities.setupDialogButton(
			nightMode,
			eclipseToggleMap,
			DialogButtonType.SECONDARY,
			getString(if (regularMapVisible) R.string.astro_hide_map else R.string.astro_show_map),
			R.drawable.ic_action_map_outlined
		)
	}

	private fun centerSunAtSelectedEclipseTime() {
		if (!isSolarEclipseModeActive()) return
		findSun()?.let { sun ->
			val center = getSolarVisibleCenter()
			starView.centerObject(sun, center.first, center.second)
		}
	}

	private fun getSolarVisibleCenter(): Pair<Float, Float> {
		if (!::eclipseCard.isInitialized || !eclipseCard.isVisible ||
			starView.width <= 0 || starView.height <= 0 || eclipseCard.height <= 0
		) {
			return starView.width / 2f to starView.height / 2f
		}
		val starLocation = IntArray(2)
		val cardLocation = IntArray(2)
		starView.getLocationOnScreen(starLocation)
		eclipseCard.getLocationOnScreen(cardLocation)
		val margin = dpToPx(16f).toFloat()
		val visibleTop = (systemTopInset - starLocation[1]).coerceAtLeast(0).toFloat() + margin
		val visibleBottom = (cardLocation[1] - starLocation[1]).toFloat() - margin
		val centerY = if (visibleBottom > visibleTop) {
			(visibleTop + visibleBottom) / 2f
		} else {
			starView.height / 2f
		}
		return starView.width / 2f to centerY.coerceIn(0f, starView.height.toFloat())
	}

	private fun findSun(): SkyObject? = solarEclipseSun

	private fun handleSolarEclipseModeState(state: SolarEclipseModeState) {
		if (!::eclipseCard.isInitialized) return
		val becameActive = state.active && !lastEclipseUiActive
		lastEclipseUiActive = state.active
		astronomyPlugin.setSolarEclipseMapData(
			active = state.active,
			eventKey = state.event?.peak?.ut,
			eventKind = state.event?.kind,
			track = state.track,
			frame = state.mapFrame
		)
		starView.useUnrefractedSolarPositions = state.active
		eclipseCard.isVisible = state.active
		timeControlCard.isVisible = !state.active
		if (!state.active) {
			resetEclipseUiCache()
			applyBottomInsets()
			return
		}

		eclipseLoading.isVisible = state.loading && state.event == null
		eclipseErrorContainer.isVisible = state.error && state.event == null && !state.loading
		eclipseContent.isVisible = state.event != null && state.window != null
		eclipsePrevious.isEnabled = !state.loading && state.event != null
		eclipseNext.isEnabled = !state.loading && state.event != null
		if (state.event == null) {
			eclipseKind.setText(
				if (state.loading) R.string.astro_loading_solar_eclipse
				else R.string.astro_solar_eclipse
			)
			eclipseEventDate.text = ""
		}

		val event = state.event
		val window = state.window
		val selectedTime = state.selectedTime
		if (event != null && window != null && selectedTime != null) {
			if (!state.loading && !state.error && state.requestId != lastFocusedEclipseRequestId) {
				val focusRequestId = state.requestId
				eclipseCard.doOnPreDraw {
					val latestState = viewModel.solarEclipseModeState.value
					if (focusRequestId == lastFocusedEclipseRequestId || latestState?.active != true ||
						latestState.requestId != focusRequestId || latestState.selectedTime?.ut != selectedTime.ut
					) {
						return@doOnPreDraw
					}
					findSun()?.let { sun ->
						manualAzimuth = true
						val center = getSolarVisibleCenter()
						starView.showTimeAndFocusObject(
							selectedTime,
							sun,
							targetX = center.first,
							targetY = center.second
						)
						lastFocusedEclipseRequestId = focusRequestId
					}
				}
			}
			val eventChanged = lastBoundEclipseEventKey != event.peak.ut
			if (eventChanged) {
				eclipseKind.setText(when (event.kind) {
					EclipseKind.Total -> R.string.astro_total_solar_eclipse
					EclipseKind.Annular -> R.string.astro_annular_solar_eclipse
					else -> R.string.astro_partial_solar_eclipse
				})
				eclipseEventDate.text = formatEclipseDate(event.peak)
				eclipseStartTime.text = formatEclipseColumn(window.start)
				eclipseMaximumTime.text = formatEclipseColumn(event.peak)
				eclipseEndTime.text = formatEclipseColumn(window.end)
				lastBoundEclipseEventKey = event.peak.ut
			}
			if (lastBoundEclipseSelectedTime != selectedTime.ut) {
				val selectedMillis = selectedTime.toMillisecondsSince1970()
				val currentTimeText = formatClockWithSeconds(selectedMillis)
				val currentDateZoneText = formatEclipseCurrentDateZone(selectedTime)
				eclipseCurrentTime.text = currentTimeText
				eclipseCurrentDateZone.text = currentDateZoneText
				eclipseTimeline.contentDescription =
					"${getString(R.string.astro_eclipse_timeline_description)} " +
						"$currentDateZoneText $currentTimeText"
				lastBoundEclipseSelectedTime = selectedTime.ut
			}
			if (eventChanged || lastBoundLocalEclipseState !== state.localState) {
				eclipseLocalStatus.text = formatLocalEclipseStatus(state)
				lastBoundLocalEclipseState = state.localState
			}
			state.observer?.let { observer ->
				updateEclipseMapCenterLocation(observer.latitude, observer.longitude)
			}

			val startMillis = window.start.toMillisecondsSince1970()
			val endMillis = window.end.toMillisecondsSince1970()
			eclipseTimeline.setRange(
				startMillis,
				endMillis,
				event.peak.toMillisecondsSince1970(),
				selectedTime.toMillisecondsSince1970()
			)
			val insideWindow = selectedTime.ut in window.start.ut..window.end.ut
			eclipseFitPath.isEnabled = insideWindow && !state.mapLoading
			eclipseToggleMap.isEnabled = true
			updateEclipseMapButtons(state.mapLoading)
		}

		state.shadowPoint?.let { point ->
			eclipseMapShown = true
			pendingEclipseMapFit = true
			if (!regularMapVisible) updateRegularMapVisibility(true)
			updateEclipseMapButtons(false)
			manualAzimuth = true
			keepSunCenteredForMapMove = true
			centerSunAtSelectedEclipseTime()
			fitEclipseMapIfReady(state, point)
			viewModel.consumeSolarEclipseShadowPoint(state.mapRequestId)
		}
		if (state.mapError) {
			app.showToastMessage(R.string.astro_eclipse_map_error)
			viewModel.consumeSolarEclipseShadowPoint(state.mapRequestId)
		}
		if (eclipseMapShown && pendingEclipseMapFit) {
			fitEclipseMapIfReady(state, state.mapFrame?.shadowPoint)
		}
		eclipseCard.post { applyBottomInsets() }
		updateBackPressedCallback()
		if (becameActive) applyRedFilterToViews(starView.showRedFilter, eclipseCard)
	}

	private fun resetEclipseUiCache() {
		lastBoundEclipseEventKey = null
		lastBoundEclipseSelectedTime = Double.NaN
		lastBoundLocalEclipseState = null
		lastDisplayedEclipseLatitude = Double.NaN
		lastDisplayedEclipseLongitude = Double.NaN
		lastEclipseButtonMapLoading = null
		lastEclipseButtonMapVisible = null
		lastEclipseButtonNightMode = null
		lastEclipseUiActive = false
	}

	private fun fitEclipseMapIfReady(
		state: SolarEclipseModeState,
		fallbackPoint: SolarEclipseShadowPoint?
	) {
		if (!pendingEclipseMapFit || !eclipseMapShown) return
		val event = state.event ?: return
		if (event.kind != EclipseKind.Partial && state.trackLoading) return
		val fitPoints = if (event.kind == EclipseKind.Partial) {
			state.mapFrame?.penumbralFootprintPolygons?.flatten().orEmpty()
		} else {
			state.track?.corridorPolygons?.flatten().orEmpty()
		}
		val mapView = app.osmandMap.mapView
		if (fallbackPoint == null) return
		if (fitPoints.isEmpty()) {
			pendingEclipseMapFit = false
			mapView.animatedDraggingThread.startMoving(
				fallbackPoint.latitude,
				fallbackPoint.longitude,
				mapView.zoom,
				mapView.zoomFloatPart
			)
			monitorEclipseMapMove()
			return
		}

		pendingEclipseMapFit = false
		view?.post {
			if (!eclipseMapShown || !isSolarEclipseModeActive()) return@post
			val mapHeight = if (InsetsUtils.isLandscape(app)) {
				REGULAR_MAP_HEIGHT_LANDSCAPE
			} else {
				REGULAR_MAP_HEIGHT
			}
			val horizontalMargin = dpToPx(16f)
			val availableWidth = (mapView.rotatedTileBox.pixWidth - horizontalMargin * 2)
				.coerceAtLeast(1)
			val availableHeight = (dpToPx(mapHeight) - dpToPx(16f)).coerceAtLeast(1)
			val fitZoom = calculateEclipseFitZoom(
				fitPoints,
				fallbackPoint,
				availableWidth,
				availableHeight
			)
			manualAzimuth = true
			keepSunCenteredForMapMove = true
			centerSunAtSelectedEclipseTime()
			mapView.animatedDraggingThread.startMoving(
				fallbackPoint.latitude,
				fallbackPoint.longitude,
				fitZoom.first,
				fitZoom.second
			)
			monitorEclipseMapMove()
		}
	}

	private fun calculateEclipseFitZoom(
		points: List<SolarEclipseMapCoordinate>,
		center: SolarEclipseShadowPoint,
		availableWidth: Int,
		availableHeight: Int
	): Pair<Int, Float> {
		val mapView = app.osmandMap.mapView
		val tileBox = mapView.rotatedTileBox
		val centerX = normalizedWorldX(center.longitude)
		val centerY = MapUtils.getTileNumberY(0f, center.latitude)
		val rotateCos = tileBox.rotateCos
		val rotateSin = tileBox.rotateSin
		var maxRotatedX = 0.0
		var maxRotatedY = 0.0

		for (point in points) {
			val deltaX = wrappedWorldDelta(normalizedWorldX(point.longitude) - centerX)
			val deltaY = MapUtils.getTileNumberY(0f, point.latitude) - centerY
			maxRotatedX = max(maxRotatedX, abs(rotateCos * deltaX - rotateSin * deltaY))
			maxRotatedY = max(maxRotatedY, abs(rotateSin * deltaX + rotateCos * deltaY))
		}

		// Keep the complete path inside an 80% viewport while the selected-time
		// shadow point remains at the actual map center.
		val halfWidth = availableWidth * ECLIPSE_MAP_FIT_HALF_FRACTION
		val halfHeight = availableHeight * ECLIPSE_MAP_FIT_HALF_FRACTION
		val horizontalScale = if (maxRotatedX > 0.0) halfWidth / maxRotatedX else Double.POSITIVE_INFINITY
		val verticalScale = if (maxRotatedY > 0.0) halfHeight / maxRotatedY else Double.POSITIVE_INFINITY
		val pixelScale = min(horizontalScale, verticalScale)
		val unclampedZoom = if (pixelScale.isFinite() && pixelScale > 0.0) {
			log2(pixelScale / (256.0 * tileBox.mapDensity))
		} else {
			mapView.zoom + mapView.zoomFloatPart.toDouble()
		}
		val fullZoom = unclampedZoom.coerceIn(mapView.minZoom.toDouble(), mapView.maxZoom.toDouble())
		val baseZoom = floor(fullZoom).toInt()
		return Pair(baseZoom, (fullZoom - baseZoom).toFloat())
	}

	private fun normalizedWorldX(longitude: Double): Double =
		((longitude + 180.0) / 360.0).let { it - floor(it) }

	private fun wrappedWorldDelta(delta: Double): Double = when {
		delta > 0.5 -> delta - 1.0
		delta < -0.5 -> delta + 1.0
		else -> delta
	}

	private fun monitorEclipseMapMove() {
		starView.removeCallbacks(monitorEclipseMapMoveRunnable)
		starView.postOnAnimation(monitorEclipseMapMoveRunnable)
	}

	private fun formatLocalEclipseStatus(state: SolarEclipseModeState): String {
		val local = state.localState
			?: return getString(R.string.astro_eclipse_calculating_local_state)
		if (local.phase == SolarEclipsePhase.None) {
			return getString(
				if (local.correctedSunAltitude <= 0.0) R.string.astro_eclipse_not_visible_below_horizon
				else R.string.astro_eclipse_not_visible_here,
				local.correctedSunAltitude
			)
		}
		val phase = getString(when (local.phase) {
			SolarEclipsePhase.Total -> R.string.astro_eclipse_phase_total
			SolarEclipsePhase.Annular -> R.string.astro_eclipse_phase_annular
			else -> R.string.astro_eclipse_phase_partial
		})
		return if (local.correctedSunAltitude <= 0.0) {
			getString(R.string.astro_eclipse_below_horizon, phase, local.correctedSunAltitude)
		} else {
			getString(
				R.string.astro_eclipse_local_status,
				phase,
				local.obscuration * 100.0,
				local.correctedSunAltitude
			)
		}
	}

	private fun updateEclipseMapCenterLocation(latitude: Double, longitude: Double) {
		if (view == null || !::eclipseMapCenterLocation.isInitialized) return
		if (lastDisplayedEclipseLatitude == latitude && lastDisplayedEclipseLongitude == longitude) return
		lastDisplayedEclipseLatitude = latitude
		lastDisplayedEclipseLongitude = longitude
		val coordinates = CoordinateFormatFormatter.formatPrimary(app, latitude, longitude)
		eclipseMapCenterLocation.text = app.getString(
			R.string.astro_map_center_location,
			coordinates
		)
	}

	private fun formatEclipseDate(time: Time): String =
		DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
			.format(Date(time.toMillisecondsSince1970()))

	private fun formatEclipseCurrentDateZone(time: Time): String {
		val millis = time.toMillisecondsSince1970()
		val date = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(millis))
		val zoneName = SimpleDateFormat("zzz", Locale.getDefault()).format(Date(millis))
		val zoneOffset = SimpleDateFormat("XXX", Locale.getDefault()).format(Date(millis))
		val hasNumericOffset = Regex("[+-]\\d{1,2}(?::?\\d{2})?").containsMatchIn(zoneName)
		val zone = if (hasNumericOffset) zoneName else "$zoneName ($zoneOffset)"
		return "$date • $zone"
	}

	private fun formatEclipseColumn(time: Time): String {
		val millis = time.toMillisecondsSince1970()
		val date = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(Date(millis))
		return "$date\n${formatClockWithSeconds(millis)}"
	}

	private fun formatClockWithSeconds(millis: Long): String {
		val pattern = if (android.text.format.DateFormat.is24HourFormat(requireContext())) {
			"HH:mm:ss"
		} else {
			"h:mm:ss a"
		}
		return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
	}

	private fun scheduleAutoTimeUpdate() {
		stopAutoTimeUpdate()
		if (!isTimeAutoUpdateEnabled() || !isResumed || !::timeControlCard.isInitialized) return

		val currentTime = System.currentTimeMillis()
		val delay = AUTO_TIME_UPDATE_INTERVAL_MS - (currentTime % AUTO_TIME_UPDATE_INTERVAL_MS) + 200L
		timeControlCard.postDelayed(autoTimeUpdateRunnable, delay)
	}

	private fun stopAutoTimeUpdate() {
		if (::timeControlCard.isInitialized) {
			timeControlCard.removeCallbacks(autoTimeUpdateRunnable)
		}
	}

	private fun isTimeAutoUpdateEnabled(): Boolean = viewModel.isTimeAutoUpdateEnabled.value != false

	private fun updateStarMap(updateAzimuth: Boolean = false) {
		if (!isAdded || view == null || !::starView.isInitialized || !::viewModel.isInitialized) return
		val tileBox = app.osmandMap.mapView.rotatedTileBox
		val location = tileBox.centerLatLon
		starView.setObserverLocation(location.latitude, location.longitude, 0.0)
		viewModel.updateSolarEclipseObserver(Observer(location.latitude, location.longitude, 0.0))
		updateEclipseMapCenterLocation(location.latitude, location.longitude)
		if (isSolarEclipseModeActive()) centerSunAtSelectedEclipseTime()
		if (updateAzimuth && !arModeHelper.isArModeEnabled && !starView.is2DMode) {
			setAzimuth(-tileBox.rotate.toDouble())
		}
	}

	private fun updateMapControlsVisibility() {
		if (!::mapControlsContainer.isInitialized) {
			return
		}
		mapControlsContainer.visibility = if (::bottomSheetBehavior.isInitialized &&
			bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN
		) {
			View.INVISIBLE
		} else {
			View.VISIBLE
		}
	}

	private fun clearSelectedObject() {
		selectedObject = null
		starView.setSelectedObject(null)
		starView.setSelectedConstellation(null)
		starView.invalidate()
	}

	fun hideBottomSheet() {
		bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
	}

	private fun showObjectInfo(obj: SkyObject) {
		val existing = childFragmentManager.findFragmentById(R.id.bottom_sheet_container) as? AstroContextMenuFragment
		if (existing == null) {
			val created = AstroContextMenuFragment.newInstance(obj)
			childFragmentManager.beginTransaction()
				.replace(
					R.id.bottom_sheet_container,
					created,
					AstroContextMenuFragment.TAG
				)
				.commitNow()
		} else {
			existing.updateObjectInfo(obj)
		}

		bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
	}

	private fun getAstroContextMenuFragment(): AstroContextMenuFragment? {
		return childFragmentManager.findFragmentById(R.id.bottom_sheet_container) as? AstroContextMenuFragment
	}

	private fun dispatchDownloadEvent(dispatch: (DownloadEvents) -> Unit) {
		for (fragment in childFragmentManager.fragments) {
			if (fragment is DownloadEvents && fragment.isAdded) {
				dispatch(fragment)
			}
		}
	}

	override fun onUpdatedIndexesList() {
		dispatchDownloadEvent { it.onUpdatedIndexesList() }
	}

	override fun downloadInProgress() {
		dispatchDownloadEvent { it.downloadInProgress() }
	}

	override fun downloadHasFinished() {
		dispatchDownloadEvent { it.downloadHasFinished() }
	}

	internal fun getTrackableObjects(): List<SkyObject> {
		val objects = mutableListOf<SkyObject>()
		objects.addAll(viewModel.skyObjects.value.orEmpty())
		objects.addAll(viewModel.constellations.value.orEmpty())
		return objects
	}

	internal fun findTrackableObjectById(id: String): SkyObject? {
		viewModel.skyObjects.value?.firstOrNull { it.id == id }?.let { return it }
		return viewModel.constellations.value?.firstOrNull { it.id == id }
	}

	private fun updateBackPressedCallback() {
		backPressedCallback.isEnabled = childFragmentManager.backStackEntryCount > 0 ||
				(::bottomSheetBehavior.isInitialized && bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) ||
				isSolarEclipseModeActive()
	}

	internal fun getSearchableObjects(): List<SkyObject> {
		return getTrackableObjects()
	}

	private fun updateTimeControlTheme(card: MaterialCardView, button: StarMapTimeControlButton, resetBtn: StarMapResetButton) {
		val context = card.context
		val bgColor = ColorUtilities.getMapButtonBackgroundColor(context, nightMode)
		val strokeColor = ColorUtilities.getColor(context, if (nightMode) R.color.map_widget_dark_stroke else R.color.map_widget_light_trans)
		val strokeWidth = AndroidUtils.dpToPx(context, 1f)

		if (timeSelectionView.isVisible) {
			card.setCardBackgroundColor(ColorUtilities.getActiveColor(context, nightMode))
			button.active = true
			resetBtn.active = true
			card.strokeWidth = 0
		} else {
			card.setCardBackgroundColor(bgColor)
			button.active = false
			resetBtn.active = false
			card.strokeWidth = strokeWidth
		}
		card.strokeColor = strokeColor

		button.nightMode = nightMode
		resetBtn.nightMode = nightMode
	}

	private fun updateMagnitudeFilterTheme() {
		val context = requireContext()
		val isSliderVisible = magnitudeSliderCard.isVisible

		val cardBgColor = ColorUtilities.getMapButtonBackgroundColor(context, nightMode)
		val textColor = ColorUtilities.getColor(context, if (nightMode) R.color.text_color_primary_dark else R.color.text_color_primary_light)
		val valueColor = ColorUtilities.getMapButtonIconColor(context, nightMode)
		val strokeColor = ColorUtilities.getColor(context, if (nightMode) R.color.map_widget_dark_stroke else R.color.map_widget_light_trans)
		val strokeWidth = AndroidUtils.dpToPx(context, 1f)

		if (isSliderVisible) {
			magnitudeFilterButton.setCardBackgroundColor(ColorUtilities.getActiveColor(context, nightMode))
			magnitudeFilterIcon.setColorFilter(Color.WHITE)
			magnitudeFilterText.setTextColor(Color.WHITE)
			magnitudeFilterButton.strokeWidth = 0
		} else {
			magnitudeFilterButton.setCardBackgroundColor(cardBgColor)
			magnitudeFilterIcon.setColorFilter(valueColor)
			magnitudeFilterText.setTextColor(valueColor)
			magnitudeFilterButton.strokeColor = strokeColor
			magnitudeFilterButton.strokeWidth = strokeWidth
		}

		magnitudeSliderCard.setCardBackgroundColor(cardBgColor)
		magnitudeSliderCard.strokeColor = strokeColor
		magnitudeSliderCard.strokeWidth = strokeWidth

		view?.findViewById<TextView>(R.id.magnitude_slider_label)?.setTextColor(textColor)
		magnitudeSliderValue.setTextColor(valueColor)
	}

	private fun updateRedMode(enabled: Boolean) {
		val viewsToFilter = mutableListOf<View>()
		if (::timeControlCard.isInitialized) viewsToFilter.add(timeControlCard)
		if (::timeSelectionView.isInitialized) viewsToFilter.add(timeSelectionView)
		if (::arModeButton.isInitialized) viewsToFilter.add(arModeButton)
		if (::cameraButton.isInitialized) viewsToFilter.add(cameraButton)
		if (::eclipseCard.isInitialized) viewsToFilter.add(eclipseCard)
		if (::resetFovButton.isInitialized) viewsToFilter.add(resetFovButton)
		if (::magnitudeFilterButton.isInitialized) viewsToFilter.add(magnitudeFilterButton)
		if (::magnitudeSliderCard.isInitialized) viewsToFilter.add(magnitudeSliderCard)
		if (::compassButton.isInitialized) viewsToFilter.add(compassButton)
		if (::closeButton.isInitialized) viewsToFilter.add(closeButton)
		if (::searchButton.isInitialized) viewsToFilter.add(searchButton)
		if (::settingsButton.isInitialized) viewsToFilter.add(settingsButton)
		if (::sliderContainer.isInitialized) viewsToFilter.add(sliderContainer)
		if (::bottomSheetContainer.isInitialized) viewsToFilter.add(bottomSheetContainer)

		applyRedFilterToViews(enabled, *viewsToFilter.toTypedArray())
		(childFragmentManager.findFragmentByTag(AstroConfigureViewBottomSheet.TAG) as? AstroConfigureViewBottomSheet)
			?.applyRedFilter(enabled)
		(childFragmentManager.findFragmentByTag(StarMapSearchDialogFragment.TAG) as? StarMapSearchDialogFragment)
			?.applyRedFilter(enabled)
	}

}
