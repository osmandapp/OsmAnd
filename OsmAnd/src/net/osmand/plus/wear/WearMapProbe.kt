package net.osmand.plus.wear

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Debug
import android.os.SystemClock
import android.util.Log

import net.osmand.core.android.AtlasMapRendererView
import net.osmand.core.android.MapRendererContext
import net.osmand.core.android.MapRendererView
import net.osmand.core.android.MapRendererView.MapRendererViewListener
import net.osmand.core.jni.MapStylesCollection
import net.osmand.core.jni.PointI
import net.osmand.core.jni.ZoomLevel
import net.osmand.plus.OsmandApplication
import net.osmand.plus.views.corenative.NativeCoreContext

import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A spike, not a feature: stands a second map renderer up next to the one the phone is already
 * using and reports what it costs, so the watch map screen can be designed against measurements
 * instead of guesses.
 *
 * Runs from the development plugin and tears everything down before it returns.
 */
object WearMapProbe {

	private const val TAG = "OsmAndWearProbe"
	private const val FRAME_TIMEOUT_MS = 15000L
	private const val CONTENT_TIMEOUT_MS = 20000L
	/** Anything smaller is a blank sheet: an empty 384x384 webp lands well under this. */
	private const val BLANK_FRAME_BYTES = 2048

	fun run(app: OsmandApplication, width: Int, height: Int, density: Float, holdSeconds: Int,
			onResult: (String) -> Unit) {
		Thread({
			val result = measure(app, width, height, density, holdSeconds)
			// Written to a file as well as the log: on an emulator the log buffer rotates away
			// before the run finishes.
			runCatching { app.getAppPath("wear_map_probe_result.txt").writeText(result) }
			onResult(result)
		}, "WearMapProbe").start()
	}

	private fun measure(app: OsmandApplication, width: Int, height: Int, density: Float,
			holdSeconds: Int): String {
		if (!NativeCoreContext.isInit()) {
			return "OpenGL core is not initialised — switch the rendering engine to OpenGL first"
		}
		val collections = NativeCoreContext.getObfsCollections()
			?: return "No obf collections; the core is not ready"

		// What the app itself is rendering with, so the probe can say whether standing a second
		// renderer up stole the first one the way Android Auto deliberately does.
		val ownBefore = ownRenderer(app)
		val before = sample(app)
		var context: MapRendererContext? = null
		var view: AtlasMapRendererView? = null
		try {
			context = MapRendererContext(app, density)
			context.setupObfMap(MapStylesCollection(), collections)

			view = AtlasMapRendererView(app)
			context.presetMapRendererOptions(view, false)
			view.setupRenderer(app, width, height, null)

			val mapView = app.osmandMap.mapView
			view.setMinZoomLevel(ZoomLevel.swigToEnum(mapView.minZoom))
			view.setMaxZoomLevel(ZoomLevel.swigToEnum(mapView.maxZoom))
			view.setAzimuth(0f)
			view.setElevationAngle(90f)
			view.setZoom(mapView.zoom.toFloat())
			view.setTarget(PointI(mapView.getCurrentRotatedTileBox().center31X, mapView.getCurrentRotatedTileBox().center31Y))
			view.setMaximumFrameRate(4)
			context.setMapRendererView(view)

			// Waiting on the renderer's own callback, not on getBitmap(): that hands back the
			// buffer as soon as it exists, so polling it reports a blank frame in no time at all.
			val rendered = CountDownLatch(1)
			view.addListener(object : MapRendererViewListener {
				override fun onUpdateFrame(v: MapRendererView) {}
				override fun onFrameReady(v: MapRendererView) = rendered.countDown()
			})
			val startedAt = SystemClock.elapsedRealtime()
			val arrived = rendered.await(FRAME_TIMEOUT_MS, TimeUnit.MILLISECONDS)
			val firstFrameMs = SystemClock.elapsedRealtime() - startedAt
			// The first frame arrives before any tile has loaded, so it is a blank sheet that
			// compresses to nothing. Keep sampling until the frame carries actual map.
			var encoded = if (arrived) view.bitmap?.let { encode(it) } ?: 0 else 0
			var contentMs = -1L
			val deadline = SystemClock.elapsedRealtime() + CONTENT_TIMEOUT_MS
			while (arrived && encoded < BLANK_FRAME_BYTES && SystemClock.elapsedRealtime() < deadline) {
				Thread.sleep(300)
				encoded = view.bitmap?.let { encode(it) } ?: 0
			}
			if (encoded >= BLANK_FRAME_BYTES) {
				contentMs = SystemClock.elapsedRealtime() - startedAt
			}

			// Held rather than torn down, so a battery reading has a steady state to measure.
			// The bitmap is pulled at the rate the stream would run at, which is what costs.
			val holdUntil = SystemClock.elapsedRealtime() + holdSeconds * 1000L
			while (SystemClock.elapsedRealtime() < holdUntil) {
				Thread.sleep(250)
				view.bitmap
			}

			val after = sample(app)
			val ownAfter = ownRenderer(app)
			return report(before, after, ownBefore, ownAfter, arrived, encoded, firstFrameMs,
				contentMs, width, height, holdSeconds)
		} catch (error: Throwable) {
			Log.e(TAG, "Probe failed", error)
			return "Probe failed: " + error.javaClass.simpleName + " " + error.message
		} finally {
			try {
				context?.releaseMapRendererView(view)
				view?.stopRenderer()
			} catch (error: Throwable) {
				Log.e(TAG, "Teardown failed", error)
			}
		}
	}

	private fun ownRenderer(app: OsmandApplication): String {
		val attached = app.osmandMap.mapView.mapRenderer != null
		val inContext = NativeCoreContext.getMapRendererContext()?.mapRendererView != null
		return "mapView=" + (if (attached) "attached" else "detached") +
				" context=" + (if (inContext) "set" else "null")
	}

	private fun report(
		before: Sample, after: Sample, ownBefore: String, ownAfter: String, arrived: Boolean,
		encoded: Int, firstFrameMs: Long, contentMs: Long, width: Int, height: Int,
		holdSeconds: Int
	): String {
		val lines = listOf(
			"second renderer at ${width}x$height, held ${holdSeconds}s",
			"app renderer before: $ownBefore",
			"app renderer after:  $ownAfter",
			"first frame: ${if (arrived) "$firstFrameMs ms" else "never arrived"}",
			"frame with map: ${if (contentMs < 0) "never arrived" else "$contentMs ms"}",
			"frame as webp q60: $encoded bytes",
			"java heap: ${delta(before.javaKb, after.javaKb)}",
			"native heap: ${delta(before.nativeKb, after.nativeKb)}",
			"graphics: ${delta(before.graphicsKb, after.graphicsKb)}",
			"total pss: ${delta(before.pssKb, after.pssKb)}"
		)
		val text = lines.joinToString("\n")
		Log.d(TAG, text)
		return text
	}

	private fun encode(bitmap: Bitmap): Int {
		val stream = ByteArrayOutputStream()
		@Suppress("DEPRECATION")
		bitmap.compress(Bitmap.CompressFormat.WEBP, 60, stream)
		return stream.size()
	}

	private fun delta(before: Long, after: Long): String {
		val diff = after - before
		val sign = if (diff >= 0) "+" else ""
		return "${before / 1024} MB -> ${after / 1024} MB ($sign${diff / 1024} MB)"
	}

	private data class Sample(val javaKb: Long, val nativeKb: Long, val graphicsKb: Long, val pssKb: Long)

	private fun sample(app: OsmandApplication): Sample {
		val runtime = Runtime.getRuntime()
		val manager = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
		val info: Debug.MemoryInfo = manager
			.getProcessMemoryInfo(intArrayOf(android.os.Process.myPid()))[0]
		return Sample(
			javaKb = (runtime.totalMemory() - runtime.freeMemory()) / 1024,
			nativeKb = Debug.getNativeHeapAllocatedSize() / 1024,
			graphicsKb = info.getMemoryStat("summary.graphics")?.toLongOrNull() ?: 0L,
			pssKb = info.totalPss.toLong()
		)
	}
}
