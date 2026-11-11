package net.osmand.plus.plugins.topoprint.render

import android.graphics.Bitmap
import android.util.Base64
import net.osmand.PlatformUtil
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.plugins.topoprint.PrintParameters
import net.osmand.plus.plugins.topoprint.compose.Geomagnetism
import net.osmand.plus.plugins.topoprint.compose.PrintGeometry
import net.osmand.plus.plugins.topoprint.compose.SvgMapComposer
import net.osmand.plus.plugins.topoprint.compose.UtmMath
import net.osmand.plus.plugins.topoprint.export.SvgToPngRenderer
import org.apache.commons.logging.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * End-to-end map pipeline: geometry → declination → offscreen render with
 * the current OsmAnd style → SVG composition → PNG rasterization.
 *
 * Must be started from a background thread ([run]); progress and completion
 * are delivered on the UI thread.
 */
class PrintMapTask(
	private val app: OsmandApplication,
	private val params: PrintParameters,
	private val center: LatLon,
	private val label: String,
	private var outputDir: File,
	private val listener: Listener
) {

	interface Listener {
		/** Progress message, delivered on the UI thread. */
		fun onProgress(message: String)

		/** Delivered on the UI thread; null on failure. */
		fun onCompleted(result: Result?)

		/** Some target files already exist. Delivered on the UI thread; call
		 *  [decision].onDecision(true) to overwrite them, false to cancel. */
		fun onOverwriteQuery(existing: List<File>, decision: OverwriteDecision)

		/** The user declined the overwrite. Delivered on the UI thread. */
		fun onCancelled()
	}

	interface OverwriteDecision {
		fun onDecision(overwrite: Boolean)
	}

	data class Result(
		val svgFile: File,
		/** Empty when the device could not rasterize the SVG in-memory. */
		val pngFile: File
	)

	private val renderer = OffscreenMapRenderer(app)
	private val log: Log = PlatformUtil.getLog(PrintMapTask::class.java)

	@Volatile
	private var cancelled = false

	/**
	 * Requests cancellation: the render aborts at the next stage boundary and
	 * [Listener.onCancelled] is delivered instead of [Listener.onCompleted].
	 * Safe to call from any thread, idempotent.
	 */
	fun cancel() {
		cancelled = true
		renderer.cancel()
	}

	fun run() {
		// A user-configured directory may be unwritable (scoped storage);
		// fall back to the app-private topoprint folder instead of failing.
		if (!outputDir.isDirectory && !outputDir.mkdirs()) {
			val fallback = File(app.getExternalFilesDir(null), "topoprint")
			if (fallback.isDirectory || fallback.mkdirs()) {
				log.warn("output dir " + outputDir + " unavailable, falling back to " + fallback)
				outputDir = fallback
			} else {
				complete(null)
				return
			}
		}
		notifyProgress(app.getString(R.string.topoprint_progress_prepare))

		val declination = Geomagnetism.declinationDegrees(
			center.latitude, center.longitude, 0.0, System.currentTimeMillis())
		val geometry = PrintGeometry.compute(params, center.latitude, center.longitude, declination)
		log.info("geometry: center=$center declination=$declination gamma=${geometry.convergenceDeg} " +
				"k=${geometry.kMm} bbox=${geometry.bbox} grid=${geometry.grid} " +
				"coverage=${geometry.rotatedCoverageWidthMeters}x${geometry.rotatedCoverageHeightMeters}m")

		val safeLabel = label.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").ifBlank { "map" }
		// File name: {title}_{size}_{scale}_{orientation}, e.g. diag_A4_25K_portrait.svg
		val fileBase = safeLabel + "_" + params.sizeTag + "_" + params.scaleTag + "_" +
				params.orientation.name.lowercase(Locale.US)
		val svgFile = File(outputDir, "$fileBase.svg")
		val pngFile = File(outputDir, "$fileBase.png")
		val basemapFile = File(outputDir, "${fileBase}_basemap.png")

		// Ask before clobbering previous outputs — checked before the
		// (potentially minutes-long) render, not after it.
		val existing = listOf(svgFile, pngFile, basemapFile).filter { it.exists() }
		if (existing.isNotEmpty()) {
			val latch = CountDownLatch(1)
			var overwrite = false
			app.runInUIThread {
				listener.onOverwriteQuery(existing, object : OverwriteDecision {
					override fun onDecision(overwriteGranted: Boolean) {
						overwrite = overwriteGranted
						latch.countDown()
					}
				})
			}
			// Bounded wait: if the decision callback is lost (e.g. the
			// activity was destroyed before the dialog could show), an
			// indefinite await would hang the whole modal pipeline forever.
			val decided = latch.await(5, TimeUnit.MINUTES)
			if (!decided || !overwrite) {
				app.runInUIThread { listener.onCancelled() }
				return
			}
		}

		if (cancelled) {
			app.runInUIThread { listener.onCancelled() }
			return
		}

		renderer.render(params, center,
			geometry.rotatedCoverageWidthMeters, geometry.rotatedCoverageHeightMeters,
			{ message -> notifyProgress(message) }) { renderResult ->
			if (cancelled) {
				// The user cancelled while the render was running: discard the
				// result and report cancellation instead of failure.
				renderResult?.bitmap?.recycle()
				app.runInUIThread { listener.onCancelled() }
				return@render
			}
			if (renderResult == null) {
				complete(null)
				return@render
			}
			notifyProgress(app.getString(R.string.topoprint_progress_capture))
			Thread {
				val result = compose(geometry, renderResult, svgFile, basemapFile, pngFile)
				// Cancellation during composition: the (complete) artifacts
				// stay on disk, but the task reports cancelled, not done.
				if (cancelled) {
					app.runInUIThread { listener.onCancelled() }
				} else {
					complete(result)
				}
			}.start()
		}
	}

	private fun compose(geometry: PrintGeometry, renderResult: OffscreenMapRenderer.RenderResult,
	                    svgFile: File, basemapFile: File, pngFile: File): Result? {
		try {
			val georef = renderResult.georef
			log.info("georef: px=${georef.widthPx}x${georef.heightPx} tl=${georef.topLeft} tr=${georef.topRight} " +
					"br=${georef.bottomRight} bl=${georef.bottomLeft}")
			// All map geometry is expressed in the centre's UTM zone and
			// hemisphere; forcing both keeps a print extent near a zone
			// boundary or across the equator consistent (northings stay
			// continuous instead of jumping by the 10M false northing).
			val zone = geometry.bbox.center.zoneNumber
			val isNorthern = geometry.bbox.center.isNorthern
			val topLeft = UtmMath.fromLatLon(georef.topLeft.latitude, georef.topLeft.longitude, zone, isNorthern)
			val topRight = UtmMath.fromLatLon(georef.topRight.latitude, georef.topRight.longitude, zone, isNorthern)
			val bottomLeft = UtmMath.fromLatLon(georef.bottomLeft.latitude, georef.bottomLeft.longitude, zone, isNorthern)
			val bottomRight = UtmMath.fromLatLon(georef.bottomRight.latitude, georef.bottomRight.longitude, zone, isNorthern)
			log.info("raster utm: tl=${topLeft.easting},${topLeft.northing} " +
					"br=${bottomRight.easting},${bottomRight.northing} (zone $zone)")
			val raster = SvgMapComposer.Raster(
				pngFileName = basemapFile.name,
				eastingTopLeft = topLeft.easting,
				northingTopLeft = topLeft.northing,
				eastingTopRight = topRight.easting,
				northingTopRight = topRight.northing,
				eastingBottomLeft = bottomLeft.easting,
				northingBottomLeft = bottomLeft.northing,
				eastingBottomRight = bottomRight.easting,
				northingBottomRight = bottomRight.northing
			)

			if (!saveBasemap(renderResult.bitmap, basemapFile)) {
				return null
			}
			val composer = SvgMapComposer(geometry, label, raster,
				System.currentTimeMillis())
			val svgText = composer.compose()
			log.info("grid-vs-raster residuals: ${composer.gridVsRasterResiduals(sampleKmIntersections(geometry))}")
			svgFile.writeText(svgText)

			val paperWidthPx = params.paperWidthPx
			val paperHeightPx = params.paperHeightPx
			if (!SvgToPngRenderer.fitsMemoryBudget(app, paperWidthPx, paperHeightPx)) {
				// The SVG is still usable on a PC even when the device cannot
				// rasterize it in-memory; the embedded basemap keeps it complete.
				embedBasemap(svgFile, basemapFile)
				return Result(svgFile, File(""))
			}
			notifyProgress(app.getString(R.string.topoprint_rasterizing))
			if (!SvgToPngRenderer.renderToPng(svgFile, paperWidthPx, paperHeightPx, pngFile, params.dpi,
					app.cacheDir)) {
				return null
			}
			// Rasterization done: embed the basemap as a base64 data URI so the
			// SVG is fully self-contained.
			embedBasemap(svgFile, basemapFile)
			return Result(svgFile, pngFile)
		} catch (e: Exception) {
			log.error("compose failed", e)
			return null
		} finally {
			renderResult.bitmap.recycle()
		}
	}

	/**
	 * Replaces the plain SVG 2 `href="…basemap.png"` in the SVG with a
	 * base64 data URI (the `xlink:href` file reference is left as-is,
	 * matching the hand-embedded reference maps). AndroidSVG cannot
	 * resolve data URIs during rasterization, so this runs only after the
	 * PNG has been written.
	 * <p>
	 * Streams the output line by line and encodes the bitmap in 3-byte-aligned
	 * chunks: a 600 dpi basemap is a ~100 MB PNG, and holding the base64 text
	 * plus the replaced document in Java strings at once blows the heap.
	 */
	private fun embedBasemap(svgFile: File, basemapFile: File) {
		// Stage the temp file in the app-private cache dir: the output dir
		// may be shared storage, where unknown MIME types (".tmp" ->
		// application/octet-stream) are rejected by MediaProvider, and a
		// lost SAF grant makes every write there fail with EPERM.
		val tmpFile = File(app.cacheDir, svgFile.name + ".tmp")
		try {
			BufferedReader(InputStreamReader(FileInputStream(svgFile), Charsets.UTF_8)).use { reader ->
				BufferedWriter(OutputStreamWriter(FileOutputStream(tmpFile), Charsets.UTF_8)).use { writer ->
					// Leading space is part of the match: ` href="…"` never
					// collides with `xlink:href="…"`, whose predecessor is a
					// colon — only the SVG 2 form is replaced.
					val ref = " href=\"" + basemapFile.name + "\""
					val dataUri = " href=\"data:image/png;base64,"
					val chunk = ByteArray(32760)
					reader.forEachLine { line ->
						var pos = 0
						while (true) {
							val idx = line.indexOf(ref, pos)
							if (idx < 0) {
								writer.write(line, pos, line.length - pos)
								break
							}
							writer.write(line, pos, idx - pos)
							writer.write(dataUri)
							FileInputStream(basemapFile).use { input ->
								while (true) {
									val read = input.read(chunk)
									if (read <= 0) break
									val encoded = Base64.encode(chunk, 0, read, Base64.NO_WRAP)
									writer.write(String(encoded, 0, encoded.size, Charsets.US_ASCII))
								}
							}
							writer.write("\"")
							pos = idx + ref.length
						}
						writer.write("\n")
					}
				}
			}
			// Cross-directory move: renameTo() fails across mount points
			// (cache dir vs shared storage), so stream-copy over the original
			// SVG instead. A mid-copy failure truncates the old SVG (which is
			// still reproducible) while the PNG — the primary deliverable —
			// is already safe on disk.
			java.io.FileInputStream(tmpFile).use { input ->
				java.io.FileOutputStream(svgFile).use { output ->
					input.copyTo(output)
				}
			}
		} finally {
			tmpFile.delete()
		}
		basemapFile.delete()
		}

	private fun saveBasemap(bitmap: Bitmap, file: File): Boolean = try {
		file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
		file.isFile
	} catch (e: Exception) {
		false
	}

	private fun notifyProgress(message: String) {
		app.runInUIThread { listener.onProgress(message) }
	}

	private fun complete(result: Result?) {
		app.runInUIThread { listener.onCompleted(result) }
	}

	/** Whole-kilometre UTM intersections sampled for the grid-vs-raster self-check: centre plus four extreme intersections. */
	private fun sampleKmIntersections(geometry: PrintGeometry): List<Pair<Double, Double>> {
		val grid = geometry.grid
		val eFrom = Math.ceil(grid.left / 1000.0).toLong() * 1000
		val eTo = Math.floor(grid.right / 1000.0).toLong() * 1000
		val nFrom = Math.ceil(grid.bottom / 1000.0).toLong() * 1000
		val nTo = Math.floor(grid.top / 1000.0).toLong() * 1000
		if (eFrom > eTo || nFrom > nTo) {
			return emptyList()
		}
		val midE = (eFrom + eTo) / 2.0
		val midN = (nFrom + nTo) / 2.0
		return listOf(
			midE to midN,
			eFrom.toDouble() to nFrom.toDouble(),
			eTo.toDouble() to nTo.toDouble(),
			eFrom.toDouble() to nTo.toDouble(),
			eTo.toDouble() to nFrom.toDouble()
		)
	}
}
