package net.osmand.test.junit

import android.Manifest
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import net.osmand.plus.OsmandApplication
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.astronomy.utils.StarMapCameraHelper
import net.osmand.plus.plugins.astronomy.views.StarView
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Regression test for issue #25880 - `IllegalStateException: CameraDevice was already closed`
 * thrown from `StarMapCameraHelper.createCaptureSession$1.onConfigured()`.
 *
 * `StarMapCameraHelper.createCaptureSession()` blocks its looper thread inside the framework's
 * `configureStreamsChecked()` and only then posts `onConfigured` back to that looper. Anything
 * that closes the camera in the meantime - `onPause()`, or the overlay button being pressed
 * again - is dispatched first, so `onConfigured()` then calls `setRepeatingRequest()` on a
 * session whose device is already closed.
 *
 * The test drives [StarMapCameraHelper] directly instead of going through `StarMapFragment`:
 * the star map is behind a paid feature, and the fragment adds nothing the race needs. The
 * helper is given a fragment that is not attached to an activity, which keeps `configureTransform()`
 * out of the way, and a real [TextureView] hosted by [MapActivity], because only an attached,
 * hardware accelerated view produces the [SurfaceTexture] the capture session needs.
 *
 * The helper is resumed and paused on a dedicated looper thread rather than the main one. The
 * ordering that produces the crash is identical - Camera2 delivers its callbacks to the looper of
 * the thread that called `openCamera()`, because the helper passes a `null` handler - but an
 * uncaught exception on that thread can be recorded and asserted on, instead of killing the
 * process and reporting the whole instrumentation run as crashed.
 *
 * Each cycle waits for `onCameraUnavailable()`, which is the framework announcing that the device
 * has just been opened, and then schedules the close a few milliseconds later, so it lands inside
 * the configuration window. How long that window is depends on the device, so the cycles sweep a
 * range of delays around it.
 *
 * Before the fix this fails on a Pixel 8 Pro / Android 17 within the first few cycles; after it,
 * all cycles pass.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class StarMapCameraOverlayRaceTest {

	companion object {
		/**
		 * Delay between the camera device being opened and the overlay being closed. The
		 * configuration window is tens of milliseconds wide and its length differs per device,
		 * so the cycles sweep across it.
		 */
		private val CLOSE_DELAYS_MS = longArrayOf(5, 10, 15, 20, 25, 30, 40, 50, 60, 80, 15, 25, 35, 45, 55)

		private const val SURFACE_TIMEOUT_SEC = 10L
		private const val CAMERA_OPEN_TIMEOUT_SEC = 10L
		private const val SESSION_SETTLE_MS = 2500L
		private const val CYCLE_SETTLE_MS = 1200L
		private const val PREVIEW_SIZE_PX = 320
		private const val APP_INIT_TIMEOUT_SEC = 120L
	}

	@get:Rule
	val scenarioRule = ActivityScenarioRule(MapActivity::class.java)

	@get:Rule
	val cameraPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

	private val uncaught = AtomicReference<Throwable?>(null)

	private var cameraThread: HandlerThread? = null
	private var cameraHandler: Handler? = null
	private var helper: StarMapCameraHelper? = null
	private var textureView: TextureView? = null

	private lateinit var app: OsmandApplication

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(APP_INIT_TIMEOUT_SEC)
		while (app.isApplicationInitializing && System.currentTimeMillis() < deadline) {
			Thread.sleep(200)
		}
	}

	@Test
	fun sessionConfiguredAfterCameraClosedDoesNotCrash() {
		val manager = app.getSystemService(Context.CAMERA_SERVICE) as CameraManager
		val cameraId = findBackCameraId(manager)
		assumeTrue("no back camera on this device", cameraId != null)

		startCameraThread()
		createHelper()
		armOverlay()

		for ((cycle, closeDelayMs) in CLOSE_DELAYS_MS.withIndex()) {
			runRaceCycle(manager, cameraId!!, cycle, closeDelayMs)

			val throwable = uncaught.get()
			if (throwable != null) {
				fail("Camera callback threw after the camera was closed" +
						" (cycle $cycle, close scheduled ${closeDelayMs}ms after the device was opened):\n" +
						stackTraceOf(throwable))
			}
		}
	}

	@After
	fun releaseCamera() {
		val handler = cameraHandler
		if (handler != null && cameraThread?.isAlive == true) {
			runOnCameraThread(handler) { helper?.onPause() }
		}
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			textureView?.let { (it.parent as? ViewGroup)?.removeView(it) }
		}
		cameraThread?.quitSafely()
		cameraThread = null
		cameraHandler = null
	}

	private fun findBackCameraId(manager: CameraManager): String? {
		return manager.cameraIdList.firstOrNull { id ->
			val lensFacing = manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
			lensFacing == CameraCharacteristics.LENS_FACING_BACK
		}
	}

	private fun startCameraThread() {
		val thread = HandlerThread("star-map-camera-race")
		thread.start()
		thread.setUncaughtExceptionHandler { _, throwable -> uncaught.compareAndSet(null, throwable) }
		cameraThread = thread
		cameraHandler = Handler(thread.looper)
	}

	/**
	 * Hosts a real [TextureView] in the activity and builds the helper around it. The fragment is
	 * deliberately detached: the helper only needs a context from it, and a null activity keeps
	 * `configureTransform()` - the one part that touches views - from running off the main thread.
	 */
	private fun createHelper() {
		val surfaceReady = CountDownLatch(1)
		scenarioRule.scenario.onActivity { activity ->
			val view = TextureView(activity)
			view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
				override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
					surfaceReady.countDown()
				}

				override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
				override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
				override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
			}
			val content = activity.findViewById<ViewGroup>(android.R.id.content)
			content.addView(view, FrameLayout.LayoutParams(PREVIEW_SIZE_PX, PREVIEW_SIZE_PX))
			textureView = view
		}
		assertTrue("the texture view never produced a surface",
			surfaceReady.await(SURFACE_TIMEOUT_SEC, TimeUnit.SECONDS))

		scenarioRule.scenario.onActivity { activity ->
			val view = textureView!!
			view.surfaceTextureListener = null
			helper = StarMapCameraHelper(DetachedFragment(app), StarView(activity), view) {}
		}
	}

	/**
	 * Turns the overlay on once from the main thread so that `isCameraOverlayEnabled` is set, lets
	 * that first session configure fully, then closes it. From here on the race is driven with
	 * [StarMapCameraHelper.onResume] and [StarMapCameraHelper.onPause] on the camera thread.
	 */
	private fun armOverlay() {
		InstrumentationRegistry.getInstrumentation().runOnMainSync { helper!!.toggleCameraOverlay() }
		assertTrue("the overlay did not turn on", helper!!.isCameraOverlayEnabled)
		Thread.sleep(SESSION_SETTLE_MS)
		InstrumentationRegistry.getInstrumentation().runOnMainSync { helper!!.onPause() }
		Thread.sleep(CYCLE_SETTLE_MS)
	}

	private fun runRaceCycle(manager: CameraManager, cameraId: String, cycle: Int, closeDelayMs: Long) {
		val handler = cameraHandler!!
		val deviceOpened = CountDownLatch(1)
		val availabilityCallback = object : CameraManager.AvailabilityCallback() {
			override fun onCameraUnavailable(id: String) {
				if (id == cameraId) {
					deviceOpened.countDown()
				}
			}
		}
		manager.registerAvailabilityCallback(availabilityCallback, Handler(Looper.getMainLooper()))
		try {
			handler.post { helper?.onResume() }
			assertTrue("cycle $cycle: the camera was never opened",
				deviceOpened.await(CAMERA_OPEN_TIMEOUT_SEC, TimeUnit.SECONDS))

			handler.postDelayed({ helper?.onPause() }, closeDelayMs)
			Thread.sleep(CYCLE_SETTLE_MS)
		} finally {
			manager.unregisterAvailabilityCallback(availabilityCallback)
		}
		if (cameraThread?.isAlive == true) {
			runOnCameraThread(handler) { helper?.onPause() }
		}
	}

	private fun runOnCameraThread(handler: Handler, action: () -> Unit) {
		val done = CountDownLatch(1)
		handler.post {
			try {
				action()
			} finally {
				done.countDown()
			}
		}
		done.await(CAMERA_OPEN_TIMEOUT_SEC, TimeUnit.SECONDS)
	}

	private fun stackTraceOf(throwable: Throwable): String {
		val writer = StringWriter()
		throwable.printStackTrace(PrintWriter(writer))
		return writer.toString()
	}

	/**
	 * A fragment that hands out a context without ever being attached, so that
	 * [Fragment.getActivity] stays null.
	 */
	private class DetachedFragment(private val ctx: Context) : Fragment() {
		override fun getContext(): Context = ctx
	}
}
