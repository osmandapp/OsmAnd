package net.osmand.plus.auto

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.car.app.HandshakeInfo
import androidx.car.app.OnDoneCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.PaneTemplate
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.testing.SessionController
import androidx.car.app.testing.TestCarContext
import androidx.car.app.testing.TestScreenManager
import androidx.car.app.versioning.CarAppApiLevels
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.Location
import net.osmand.data.LatLon
import net.osmand.data.ValueHolder
import net.osmand.plus.OsmAndLocationProvider
import net.osmand.plus.OsmandApplication
import net.osmand.plus.Version
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.auto.screens.LandingScreen
import net.osmand.plus.auto.screens.NavigationScreen
import net.osmand.plus.auto.screens.RoutePreviewScreen
import net.osmand.plus.auto.screens.SettingsScreen
import net.osmand.plus.inapp.InAppPurchaseUtils
import net.osmand.plus.routing.IRouteInformationListener
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.enums.TrackApproximationType
import net.osmand.plus.track.data.GPXInfo
import net.osmand.plus.views.corenative.NativeCoreContext
import net.osmand.search.core.ObjectType
import net.osmand.search.core.SearchResult
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.primitives.Track
import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.util.MapUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * A route preview tilts the map to 2D, turns it north up and unlinks it from the location. None of that may
 * reach the navigation view or the saved tilt. The route comes from a GPX, so no offline map is needed.
 */
@RunWith(AndroidJUnit4::class)
@SuppressLint("RestrictedApi")
class CarNavigationMapStateTest {

	companion object {
		private const val USER_TILT = 45f
		private const val PREVIEW_TILT = 90f
		private const val TILT_DELTA = 1f
		private const val USER_ROTATION = -35f
		private const val ROTATION_DELTA = 1f
		private const val START_LAT = 43.9180
		private const val START_LON = 12.4360
		private const val TIMEOUT_SEC = 15L
		private const val CUSTOM_LOCATION_MS = 300_000L
		private const val PREVIEW_ANIMATION_MS = 2500L
		private const val RECORDING_HOLD_MS = 3000L
		private const val MAX_CENTER_DISTANCE_M = 100.0
		private const val SURFACE_WIDTH = 800
		private const val SURFACE_HEIGHT = 480
		private const val SURFACE_DPI = 160
	}

	private lateinit var app: OsmandApplication
	private lateinit var carContext: TestCarContext
	private lateinit var session: NavigationSession
	private lateinit var controller: SessionController
	private lateinit var screenManager: TestScreenManager

	private val mode = ApplicationMode.CAR
	private var savedMode: ApplicationMode? = null
	private var savedTilt = 0f
	private var savedGuidance: TrackApproximationType? = null
	private var savedOpenGl = false
	private var targetPointsBackup: CarTestUtils.TargetPointsBackup? = null
	private var imageReader: ImageReader? = null
	private var carFrames: CarFrames? = null

	@get:Rule
	val testName = TestName()

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		CarTestUtils.waitForAppInitialization(app)
		assumeTrue("Car navigation session must not be active before test execution", app.carNavigationSession == null)
		assumeTrue("Android Auto feature must be available", InAppPurchaseUtils.isAndroidAutoAvailable(app))
		assumeTrue("Location permission must be available", OsmAndLocationProvider.isLocationPermissionAvailable(app))
		val routingHelper = app.routingHelper
		val targetPointsHelper = app.targetPointsHelper
		assumeTrue("Destination point must not be set before test execution", targetPointsHelper.pointToNavigate == null)
		assumeTrue(
			"Navigation must not be active or in planning mode",
			!routingHelper.isFollowingMode && !routingHelper.isPauseNavigation && !routingHelper.isRoutePlanningMode
		)

		val settings = app.settings
		savedMode = settings.applicationMode
		savedGuidance = settings.DETAILED_TRACK_GUIDANCE.getModeValue(mode)
		savedOpenGl = settings.USE_OPENGL_RENDER.get()
		targetPointsBackup = CarTestUtils.saveTargetPointsBackup(app)
		initOpenGlRenderer()

		CarTestUtils.onMain {
			settings.USE_OPENGL_RENDER.set(true)
			settings.setApplicationMode(mode, false)
			settings.DETAILED_TRACK_GUIDANCE.setModeValue(mode, TrackApproximationType.MANUAL)
			val location = Location("test", START_LAT, START_LON)
			location.accuracy = 5f
			location.time = System.currentTimeMillis()
			app.locationProvider.setCustomLocation(location, CUSTOM_LOCATION_MS)

			carContext = TestCarContext.createCarContext(app)
			carContext.updateHandshakeInfo(HandshakeInfo(CarTestUtils.TEST_HOST_PACKAGE, CarAppApiLevels.LEVEL_5))
			session = NavigationSession()
			controller = SessionController(session, carContext, Intent())
			controller.moveToState(Lifecycle.State.RESUMED)
			carContext.lifecycleOwner.registry.currentState = Lifecycle.State.RESUMED
			attachCarSurface()
			// with a started session this is the tilt of the car map, which is kept apart from the phone one
			savedTilt = settings.getLastKnownMapElevation(mode)
			settings.setLastKnownMapElevation(mode, USER_TILT)
			app.osmandMap.mapView.setElevationAngle(USER_TILT)
		}
		assumeTrue("Needs the OpenGL renderer of the car map", CarTestUtils.onMain {
			session.navigationCarSurface.hasOffscreenRenderer()
		})
		screenManager = carContext.getCarService(TestScreenManager::class.java)
		assumeTrue(
			"Initial top screen must be LandingScreen (not purchase or permission screen)",
			CarTestUtils.onMain { screenManager.top } is LandingScreen
		)
	}

	@After
	fun tearDown() {
		val instrumentation = InstrumentationRegistry.getInstrumentation()
		if (::controller.isInitialized) {
			instrumentation.runOnMainSync {
				app.stopNavigation()
				session.stopCarNavigation()
				app.settings.setLastKnownMapElevation(mode, savedTilt)
			}
			instrumentation.waitForIdleSync()
			instrumentation.runOnMainSync {
				// screens follow TestCarContext, not the session: left alive, they stay route listeners in the next test
				screenManager.popToRoot()
				controller.moveToState(Lifecycle.State.DESTROYED)
			}
			imageReader?.close()
			carFrames?.close()
		}
		targetPointsBackup?.let { backup ->
			instrumentation.runOnMainSync {
				app.osmandMap.mapActions.setGPXRouteParams(null)
				app.targetPointsHelper.clearAllPoints(false)
				app.settings.DETAILED_TRACK_GUIDANCE.setModeValue(mode, savedGuidance)
				app.settings.USE_OPENGL_RENDER.set(savedOpenGl)
				savedMode?.let { app.settings.setApplicationMode(it, false) }
				app.locationProvider.setCustomLocation(null, 0)
			}
			CarTestUtils.restoreTargetPointsBackup(app, backup)
		}
	}

	@Test
	fun startOnPhoneLeavesNoPreviewThatResetsNavigationOnRecalculation() {
		val preview = buildRouteAndWaitForPreview()

		startNavigationOnPhone()

		recalculateRoute()
		assertNavigationView()
		assertFalse("The preview must not stay under navigation", CarTestUtils.onMain { previewInStack() })
		assertEquals(Lifecycle.State.DESTROYED, CarTestUtils.onMain { preview.lifecycle.currentState })
	}

	@Test
	fun newDestinationDuringNavigationKeepsNavigationView() {
		buildRouteAndWaitForPreview()
		startNavigationOnPhone()

		// a new destination while navigating: the route arrives with its preview on top
		CarTestUtils.onMain {
			val result = SearchResult()
			result.location = LatLon(START_LAT, START_LON)
			// a track without a file builds nothing: the preview just waits for the route, like one that calculates it
			result.objectType = ObjectType.GPX_TRACK
			result.relatedObject = GPXInfo("test", null)
			screenManager.push(RoutePreviewScreen(carContext, session.settingsAction, result, true))
		}
		val previewOpened = waitFor { screenManager.top is RoutePreviewScreen }
		assertTrue("The new preview must open, " + CarTestUtils.onMain { mapState() }, previewOpened)

		recalculateRoute()
		val navigationOpened = waitFor { screenManager.top is NavigationScreen }
		assertTrue("Navigation must return, " + CarTestUtils.onMain { mapState() }, navigationOpened)
		assertFalse("The preview must not stay under navigation", CarTestUtils.onMain { previewInStack() })
		assertNavigationView()
	}

	@Test
	fun closingSettingsDuringNavigationKeepsMapOnLocation() {
		buildRouteAndWaitForPreview()
		startNavigationOnPhone()
		assertTrue("The map must follow the location", CarTestUtils.onMain { isMapLinked() })

		CarTestUtils.onMain {
			screenManager.push(SettingsScreen(carContext))
			screenManager.pop()
		}

		assertTrue("Closing settings must not unlink the map from the location", CarTestUtils.onMain { isMapLinked() })
	}

	@Test
	fun startInCarAfterPanningFollowsLocation() {
		CarTestUtils.onMain { app.mapViewTrackingUtilities.isMapLinkedToLocation = false }
		val preview = buildRouteAndWaitForPreview()

		tapStart(preview)

		assertTrue(waitFor { !previewInStack() && screenManager.top is NavigationScreen })
		assertTrue("Navigation must follow the location", waitFor { isMapLinked() })
		val tiltReturned = waitFor { Math.abs(app.osmandMap.mapView.elevationAngle - USER_TILT) <= TILT_DELTA }
		assertTrue("The tilt must return for navigation, " + CarTestUtils.onMain { mapState() }, tiltReturned)
	}

	@Test
	fun pausingPhoneMapDuringCarPreviewKeepsSavedTilt() {
		buildRouteAndWaitForPreview()

		ActivityScenario.launch<MapActivity>(Intent(app, MapActivity::class.java)).use { scenario ->
			// the phone map opens with the saved tilt, the car map is what is on preview at this moment
			CarTestUtils.onMain { app.osmandMap.mapView.setElevationAngle(PREVIEW_TILT) }
			scenario.moveToState(Lifecycle.State.CREATED)

			assertEquals(USER_TILT, CarTestUtils.onMain { app.settings.getLastKnownMapElevation(mode) }, TILT_DELTA)
		}
	}

	/**
	 * A debug apk keeps its native libraries packed, so checkOpenGlExists() finds none in the library
	 * directory and the renderer stays off although the library loads from the apk.
	 */
	private fun initOpenGlRenderer() {
		val openGlExists = Version::class.java.getDeclaredField("openGlExists")
		openGlExists.isAccessible = true
		openGlExists.set(null, true)
		NativeCoreContext.init(app)
	}

	/** The car host gives the app a surface. An image reader does it here, so the real offscreen renderer runs. */
	private fun attachCarSurface() {
		val reader = ImageReader.newInstance(SURFACE_WIDTH, SURFACE_HEIGHT, PixelFormat.RGBA_8888, 2)
		val frames = CarFrames.createIfRequested(app, testName.methodName)
		carFrames = frames
		reader.setOnImageAvailableListener({ source ->
			source.acquireLatestImage()?.use { frames?.save(it) }
		}, Handler(frames?.looper ?: Looper.getMainLooper()))
		imageReader = reader
		val surfaceRenderer = session.navigationCarSurface
		val callback = surfaceRenderer.mSurfaceCallback
		callback.onSurfaceAvailable(SurfaceContainer(reader.surface, SURFACE_WIDTH, SURFACE_HEIGHT, SURFACE_DPI))
		callback.onVisibleAreaChanged(Rect(0, 0, SURFACE_WIDTH, SURFACE_HEIGHT))
		callback.onStableAreaChanged(Rect(0, 0, SURFACE_WIDTH, SURFACE_HEIGHT))
		session.setMapView(app.osmandMap.mapView)
	}

	private fun assertNavigationView() {
		// the move of a preview takes its time, so a reset would not show right after the recalculation
		Thread.sleep(PREVIEW_ANIMATION_MS)
		holdForRecording()
		assertEquals(
			"Recalculation must not change the tilt, " + CarTestUtils.onMain { mapState() },
			USER_TILT, CarTestUtils.onMain { app.osmandMap.mapView.elevationAngle }, TILT_DELTA
		)
		assertTrue("Recalculation must not unlink the map, " + CarTestUtils.onMain { mapState() }, CarTestUtils.onMain { isMapLinked() })
		assertTrue("The map must stay on the location, " + CarTestUtils.onMain { mapState() }, CarTestUtils.onMain { mapCenterDistance() } < MAX_CENTER_DISTANCE_M)
		val rotated = CarTestUtils.onMain { rotationDiff() <= ROTATION_DELTA }
		assertTrue("Recalculation must not turn the map, " + CarTestUtils.onMain { mapState() }, rotated)
		assertEquals(USER_TILT, CarTestUtils.onMain { app.settings.getLastKnownMapElevation(mode) }, TILT_DELTA)
	}

	private fun buildRouteAndWaitForPreview(): RoutePreviewScreen {
		CarTestUtils.onMain { app.osmandMap.mapActions.buildRouteByGivenGpx(createTrack()) }
		assertTrue("The route must be calculated", waitFor { app.routingHelper.isRouteCalculated })
		assertTrue("The preview must open", waitFor { screenManager.top is RoutePreviewScreen })
		val previewIn2D = waitFor { app.osmandMap.mapView.mapRenderer?.elevationAngle == PREVIEW_TILT }
		assertTrue("The preview must show the route in 2D, " + CarTestUtils.onMain { mapState() }, previewIn2D)
		return CarTestUtils.onMain { screenManager.top as RoutePreviewScreen }
	}

	/** The car side of a start on the phone is what [net.osmand.plus.NavigationService] calls. */
	private fun startNavigationOnPhone() {
		CarTestUtils.onMain {
			app.osmandMap.mapActions.startNavigation()
			session.startCarNavigation()
		}
		assertTrue("Navigation must open", waitFor { screenManager.top is NavigationScreen })
		val tiltReturned = waitFor { Math.abs(app.osmandMap.mapView.elevationAngle - USER_TILT) <= TILT_DELTA }
		assertTrue("The tilt must return for navigation, " + CarTestUtils.onMain { mapState() }, tiltReturned)
		assertTrue("The map must return to the location", waitFor { isMapLinked() })
		assertTrue("The map must settle", waitFor { !app.osmandMap.mapView.animatedDraggingThread.isAnimating })
		CarTestUtils.onMain { app.osmandMap.mapView.setRotate(USER_ROTATION, true) }
		assertTrue("The map must turn, " + CarTestUtils.onMain { mapState() }, waitFor { rotationDiff() <= ROTATION_DELTA })
		holdForRecording()
	}

	private fun holdForRecording() {
		if (carFrames != null) {
			Thread.sleep(RECORDING_HOLD_MS)
		}
	}

	private fun tapStart(preview: RoutePreviewScreen) {
		CarTestUtils.onMain {
			val template = preview.onGetTemplate() as MapWithContentTemplate
			val pane = (template.contentTemplate as PaneTemplate).pane
			pane.actions.first().onClickDelegate!!.sendClick(object : OnDoneCallback {})
		}
	}

	private fun recalculateRoute() {
		var finished = false
		val listener = object : IRouteInformationListener {
			override fun newRouteIsCalculated(newRoute: Boolean, showToast: ValueHolder<Boolean>) {
				finished = true
			}

			override fun routeWasCancelled() {
			}

			override fun routeWasFinished() {
			}
		}
		// the last listener, so everybody else has reacted when it is called
		CarTestUtils.onMain {
			app.routingHelper.addListener(listener)
			app.routingHelper.recalculateRouteDueToSettingsChange(false)
		}
		try {
			assertTrue("The route must be recalculated", waitFor { finished })
		} finally {
			CarTestUtils.onMain { app.routingHelper.removeListener(listener) }
		}
	}

	private fun rotationDiff(): Double {
		return Math.abs(MapUtils.degreesDiff(app.osmandMap.mapView.rotate.toDouble(), USER_ROTATION.toDouble()))
	}

	private fun mapCenterDistance(): Double {
		val mapView = app.osmandMap.mapView
		return MapUtils.getDistance(mapView.latitude, mapView.longitude, START_LAT, START_LON)
	}

	private fun mapState(): String {
		val mapView = app.osmandMap.mapView
		return "center=${mapCenterDistance().toInt()}m view=${mapView.elevationAngle} renderer=${mapView.mapRenderer?.elevationAngle} rotate=${mapView.rotate}" +
				" saved=${app.settings.getLastKnownMapElevation(mode)} mode=${app.settings.applicationMode.stringKey}" +
				" routingMode=${app.routingHelper.appMode.stringKey} linked=${isMapLinked()}" +
				" stack=${screenManager.screenStack.map { it.javaClass.simpleName }}"
	}

	private fun previewInStack(): Boolean {
		return screenManager.screenStack.any { it is RoutePreviewScreen }
	}

	private fun isMapLinked(): Boolean {
		return app.mapViewTrackingUtilities.isMapLinkedToLocation
	}

	private fun createTrack(): GpxFile {
		val segment = TrkSegment()
		val startTime = System.currentTimeMillis()
		for (i in 0 until 20) {
			val point = WptPt(START_LAT + i * 0.0005, START_LON + i * 0.0005)
			point.time = startTime + i * TimeUnit.SECONDS.toMillis(5)
			segment.points.add(point)
		}
		val track = Track()
		track.segments.add(segment)
		val gpxFile = GpxFile("test")
		gpxFile.tracks.add(track)
		return gpxFile
	}

	private fun waitFor(condition: () -> Boolean): Boolean {
		val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SEC)
		while (System.currentTimeMillis() < deadline) {
			if (CarTestUtils.onMain(condition)) {
				return true
			}
			Thread.sleep(CarTestUtils.INIT_POLL_MS)
		}
		return false
	}
}

/**
 * The car map goes to an image reader, so a screen recording of the device does not show it. Run with
 * `-e carFrames true` to get its frames in files/car_frames/<test> of the external storage.
 */
private class CarFrames(private val dir: File) {

	companion object {
		private const val FRAME_INTERVAL_MS = 200L
		private const val JPEG_QUALITY = 80

		fun createIfRequested(app: OsmandApplication, test: String): CarFrames? {
			if (InstrumentationRegistry.getArguments().getString("carFrames") == null) {
				return null
			}
			val dir = File(app.getExternalFilesDir(null), "car_frames/$test")
			dir.deleteRecursively()
			dir.mkdirs()
			return CarFrames(dir)
		}
	}

	private val thread = HandlerThread("CarFrames").also { it.start() }
	private var index = 0
	private var lastSavedMs = 0L

	val looper: Looper
		get() = thread.looper

	fun save(image: Image) {
		val now = System.currentTimeMillis()
		if (now - lastSavedMs < FRAME_INTERVAL_MS) {
			return
		}
		lastSavedMs = now
		val plane = image.planes[0]
		val bitmap = Bitmap.createBitmap(plane.rowStride / plane.pixelStride, image.height, Bitmap.Config.ARGB_8888)
		bitmap.copyPixelsFromBuffer(plane.buffer)
		File(dir, "%05d.jpg".format(index++)).outputStream().use {
			bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
		}
		bitmap.recycle()
	}

	fun close() {
		thread.quitSafely()
	}
}
