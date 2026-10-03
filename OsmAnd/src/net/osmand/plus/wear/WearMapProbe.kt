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
import net.osmand.core.jni.MapStylesCollection
import net.osmand.core.jni.PointI
import net.osmand.core.jni.ZoomLevel
import net.osmand.plus.OsmandApplication
import net.osmand.plus.views.corenative.NativeCoreContext

import java.io.ByteArrayOutputStream

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

	fun run(app: OsmandApplication, width: Int, height: Int, density: Float, onResult: (String) -> Unit) {
		Thread({
			val result = measure(app, width, height, density)
			// Written to a file as well as the log: on an emulator the log buffer rotates away
			// before the run finishes.
			runCatching { java.io.File(app.filesDir, "wear_map_probe_result.txt").writeText(result) }
			onResult(result)
		}, "WearMapProbe").start()
	}

	private fun measure(app: OsmandApplication, width: Int, height: Int, density: Float): String {
		if (!NativeCoreContext.isInit()) {
			return "OpenGL core is not initialised — switch the rendering engine to OpenGL first"
		}
		val collections = NativeCoreContext.getObfsCollections()
			?: return "No obf collections; the core is not ready"

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

			val startedAt = SystemClock.elapsedRealtime()
			val bitmap = awaitFrame(view)
			val firstFrameMs = SystemClock.elapsedRealtime() - startedAt

			val after = sample(app)
			return report(before, after, bitmap, firstFrameMs, width, height)
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

	private fun awaitFrame(view: MapRendererView): Bitmap? {
		val deadline = SystemClock.elapsedRealtime() + FRAME_TIMEOUT_MS
		while (SystemClock.elapsedRealtime() < deadline) {
			val bitmap = view.bitmap
			if (bitmap != null) {
				return bitmap
			}
			Thread.sleep(200)
		}
		return null
	}

	private fun report(
		before: Sample, after: Sample, bitmap: Bitmap?, firstFrameMs: Long, width: Int, height: Int
	): String {
		val encoded = bitmap?.let { encode(it) } ?: -1
		val lines = listOf(
			"second renderer at ${width}x$height",
			"first frame: ${if (bitmap == null) "never arrived" else "$firstFrameMs ms"}",
			"frame as webp q60: ${if (encoded < 0) "n/a" else "${encoded / 1024} KB"}",
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
