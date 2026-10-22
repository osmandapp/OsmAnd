package net.osmand.plus.wear

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.util.Log

import com.google.android.gms.tasks.Tasks

import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable

import net.osmand.plus.OsmandApplication
import net.osmand.wear.api.WearProtocol

import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Renders the map at the watch's size and streams it there, over a channel because frames are
 * a stream and the Data Layer would dedupe and retain them.
 */
class WearMapStreamer(private val app: OsmandApplication) {

	private val channelClient = Wearable.getChannelClient(app)

	private var source: WearMapSource? = null
	private var channel: ChannelClient.Channel? = null
	private var output: OutputStream? = null
	private var pump: Thread? = null

	@Volatile
	private var running = false

	@Volatile
	private var paused = false

	/** Dragging breaks it and a double tap restores it. Zoom stays the watch's own either way. */
	@Volatile
	private var following = true

	/**
	 * Counts frames of the source it was set against, so it must be cleared with that source:
	 * a count left over from a previous one waits for frames the pump will never ask for.
	 */
	@Volatile
	private var awaitFrame = 0

	/** Written into every frame, so the watch can tell an answer from a frame already in flight. */
	@Volatile
	private var appliedSeq = 0


	/**
	 * Woken when a gesture lands rather than slept through: measured, those waits were two
	 * thirds of the second and a half a gesture took to answer.
	 */
	private val gestureArrived = java.lang.Object()

	private val overlay by lazy { WearMapOverlay(app) }

	/**
	 * When the watch last said anything. A watch that goes flat or is force stopped never sends
	 * StopMapStream, and the renderer it left behind would hold its memory until OsmAnd dies.
	 */
	@Volatile
	private var lastHeardFrom = 0L

	@Synchronized
	fun start(nodeId: String, width: Int, height: Int, density: Float) {
		// A stream that ended badly can leave the flag set, and a start that quietly does nothing
		// leaves the watch reading "loading" until the phone app is killed.
		if (running) {
			stop()
			pump?.join(RESTART_TIMEOUT_MS)
		}
		if (app.carNavigationSession != null) {
			LOG.info("Watch asked for the map while a car display is connected; refused")
			return
		}
		lastHeardFrom = SystemClock.elapsedRealtime()
		running = true
		val frameWidth = (width * OVERSCAN).toInt()
		val frameHeight = (height * OVERSCAN).toInt()
		pump = Thread({ pump(nodeId, frameWidth, frameHeight, density) }, "WearMapStreamer").also {
			it.start()
		}
	}

	/**
	 * Only the preference is written here. Reopening the stream from this thread would wait for
	 * the pump to finish while holding the lock the pump needs in order to finish.
	 */
	fun setLegacyRenderer(legacy: Boolean) {
		lastHeardFrom = SystemClock.elapsedRealtime()
		legacyRenderer(app).set(legacy)
		nudge()
	}

	fun setPaused(value: Boolean) {
		paused = value
		lastHeardFrom = SystemClock.elapsedRealtime()
		nudge()
	}

	@Synchronized
	fun stop() {
		running = false
		paused = false
		following = true
		appliedSeq = 0
		awaitFrame = 0
		pump?.interrupt()
	}

	private fun pump(nodeId: String, width: Int, height: Int, density: Float) {
		try {
			var legacy = legacyRenderer(app).get()
			var renderer = openSource(legacy, width, height, density) ?: return
			useSource(renderer)
			openChannel(nodeId)
			val stream = output ?: return
			var lastSent = ByteArray(0)
			var lastSentSeq = -1
			val budget = FrameBudget()
			var fitted = false
			while (running && !Thread.currentThread().isInterrupted) {
				val frameStartedAt = SystemClock.elapsedRealtime()
				if (app.carNavigationSession != null) {
					// A car display took over while the watch was looking at the map.
					return
				}
				if (frameStartedAt - lastHeardFrom > SILENCE_TIMEOUT_MS) {
					LOG.info("No word from the watch for a while; dropping the map renderer")
					return
				}
				if (paused) {
					idle(FRAME_INTERVAL_MS)
					continue
				}
				if (legacyRenderer(app).get() != legacy) {
					renderer.close()
					legacy = legacyRenderer(app).get()
					renderer = openSource(legacy, width, height, density) ?: return
					useSource(renderer)
					lastSent = ByteArray(0)
				}
				if (renderer.drawn < awaitFrame) {
					Thread.sleep(RENDER_INTERVAL_MS)
					continue
				}
					// Framed once, when it appears: doing it every frame would undo any looking around.
				val previewing = app.routingHelper.isRoutePlanningMode &&
						app.routingHelper.isRouteCalculated
				if (previewing && !fitted) {
					val bounds = routeBounds()
					LOG.info("Preview fit: points="
							+ (app.routingHelper.route?.immutableAllLocations?.size ?: -1)
							+ " bounds=" + bounds)
					bounds?.let {
						renderer.fit(it)
						fitted = true
					}
				} else if (!previewing) {
					fitted = false
					if (following) {
						renderer.followPhone()
					}
				}
					// Read before drawing: a gesture landing mid-frame is not in that frame,
					// however early it set the sequence.
				val seqForFrame = appliedSeq
				val bitmap = renderer.frame()
				if (bitmap != null) {
						// Drawn here, not on the watch: only this side knows where the camera was.
					val overlayMs = renderer.overlayBox()?.let { box ->
						overlay.draw(Canvas(bitmap), box, LAYERS)
					} ?: 0L
					val grabbedAt = SystemClock.elapsedRealtime()
					val encoded = encode(bitmap)
					val encodedAt = SystemClock.elapsedRealtime()
						// Unchanged frames are not worth the radio, except as the answer to a
						// gesture: the watch holds its preview until told the phone has it.
					if (!encoded.contentEquals(lastSent) || seqForFrame != lastSentSeq) {
						writeFrame(stream, encoded, seqForFrame)
						lastSentSeq = seqForFrame
						lastSent = encoded
						budget.record(encoded.size, encodedAt - grabbedAt,
							SystemClock.elapsedRealtime() - encodedAt, overlayMs)
					}
				}
				val elapsed = SystemClock.elapsedRealtime() - frameStartedAt
				val wait = FRAME_INTERVAL_MS - elapsed
				if (wait > 0) {
					idle(wait)
				}
			}
		} catch (interrupted: InterruptedException) {
			// Asked to stop.
		} catch (error: Throwable) {
			LOG.error("Map stream failed", error)
		} finally {
			closeAll()
		}
	}

	/**
	 * The watch asks for a scale factor; this turns it into the renderer's zoom.
	 *
	 * The two are not the same thing. The renderer's float zoom is a level plus visualZoom - 1,
	 * so the ground it shows is linear in visualZoom, not in powers of two: adding a tenth to
	 * the zoom enlarges by 1.1, not by 1.072. Doing the arithmetic in log2 of the scale and
	 * encoding the result back is what makes the watch's own preview of the gesture land on the
	 * frame that follows it.
	 */
	fun zoom(factor: Float, seq: Int) {
		lastHeardFrom = SystemClock.elapsedRealtime()
		appliedSeq = maxOf(appliedSeq, seq)
		val renderer = source ?: return
		renderer.zoom(factor)
		awaitFrame = renderer.drawn + renderer.framesInFlight
		nudge()
	}

	fun pan(dx: Float, dy: Float, seq: Int) {
		lastHeardFrom = SystemClock.elapsedRealtime()
		appliedSeq = maxOf(appliedSeq, seq)
		following = false
		val renderer = source ?: return
		renderer.pan(dx, dy)
		awaitFrame = renderer.drawn + renderer.framesInFlight
		nudge()
	}

	fun recenter(seq: Int) {
		lastHeardFrom = SystemClock.elapsedRealtime()
		appliedSeq = maxOf(appliedSeq, seq)
		following = true
		val renderer = source ?: return
		renderer.followPhone()
		awaitFrame = renderer.drawn + renderer.framesInFlight
		nudge()
	}

	private fun openChannel(nodeId: String) {
		val opened = Tasks.await(channelClient.openChannel(nodeId, WearProtocol.PATH_MAP_STREAM))
		channel = opened
		output = Tasks.await(channelClient.getOutputStream(opened))
	}

	private fun writeFrame(stream: OutputStream, frame: ByteArray, seq: Int) {
		writeInt(stream, frame.size)
		writeInt(stream, seq)
		stream.write(frame)
		stream.flush()
	}

	private fun writeInt(stream: OutputStream, value: Int) {
		stream.write(value ushr 24)
		stream.write(value ushr 16 and 0xFF)
		stream.write(value ushr 8 and 0xFF)
		stream.write(value and 0xFF)
	}

	/** What a frame costs, so the frame rate can be argued from measurements. */
	private class FrameBudget {

		private var since = SystemClock.elapsedRealtime()
		private var frames = 0
		private var bytes = 0L
		private var encodeMs = 0L
		private var writeMs = 0L
		private var overlayMs = 0L

		fun record(size: Int, encode: Long, write: Long, overlay: Long) {
			frames++
			bytes += size
			encodeMs += encode
			writeMs += write
			overlayMs += overlay
			val elapsed = SystemClock.elapsedRealtime() - since
			if (elapsed < REPORT_INTERVAL_MS) {
				return
			}
			LOG.info("Map stream: $frames frames in ${elapsed}ms"
					+ ", ${bytes / 1024} KB (${bytes * 1000 / elapsed / 1024} KB/s)"
					+ ", ${bytes / frames} B/frame"
					+ ", overlay ${overlayMs / frames}ms/frame"
					+ ", encode ${encodeMs / frames}ms/frame"
					+ ", write ${writeMs / frames}ms/frame"
					+ ", link busy ${writeMs * 100 / elapsed}%")
			since = SystemClock.elapsedRealtime()
			frames = 0
			bytes = 0
			encodeMs = 0
			writeMs = 0
			overlayMs = 0
		}

		private companion object {
			const val REPORT_INTERVAL_MS = 5_000L
		}
	}

	private fun encode(bitmap: Bitmap): ByteArray {
		val out = ByteArrayOutputStream()
		@Suppress("DEPRECATION")
		bitmap.compress(Bitmap.CompressFormat.WEBP, FRAME_QUALITY, out)
		return out.toByteArray()
	}

	@Synchronized
	private fun closeAll() {
		runCatching { output?.close() }
		runCatching { channel?.let { channelClient.close(it) } }
		runCatching { source?.close() }
		output = null
		channel = null
		source = null
		running = false
	}

	private fun useSource(renderer: WearMapSource) {
		source = renderer
		awaitFrame = 0
	}

	/** What the route covers, with room at the edges so it does not touch them. */
	private fun routeBounds(): net.osmand.data.QuadRect? {
		val points = app.routingHelper.route?.immutableAllLocations ?: return null
		if (points.isEmpty()) {
			return null
		}
		var left = points[0].longitude
		var right = left
		var top = points[0].latitude
		var bottom = top
		for (point in points) {
			left = minOf(left, point.longitude)
			right = maxOf(right, point.longitude)
			top = maxOf(top, point.latitude)
			bottom = minOf(bottom, point.latitude)
		}
		val padX = (right - left) * FIT_MARGIN
		val padY = (top - bottom) * FIT_MARGIN
		return net.osmand.data.QuadRect(left - padX, top + padY, right + padX, bottom - padY)
	}

	private fun openSource(
		legacy: Boolean, width: Int, height: Int, density: Float
	): WearMapSource? {
		val opened = if (legacy) WearLegacyMapSource(app) else WearGlMapSource(app)
		return if (opened.open(width, height, density)) opened else null
	}

	private fun idle(millis: Long) {
		synchronized(gestureArrived) { gestureArrived.wait(millis) }
	}

	private fun nudge() {
		synchronized(gestureArrived) { gestureArrived.notifyAll() }
	}

	companion object {

		/** Which of OsmAnd's two renderers draws the watch's map. */
		fun legacyRenderer(app: OsmandApplication) =
			app.settings.registerBooleanPreference("wear_legacy_map_renderer", true)
				// Global: left to the default it is a profile setting, and the choice would
				// quietly revert whenever the profile changed.
				.makeGlobal()

		private val LOG = net.osmand.PlatformUtil.getLog(WearMapStreamer::class.java)
		const val FRAMES_PER_SECOND = 4
		const val FRAME_INTERVAL_MS = 1000L / FRAMES_PER_SECOND
		const val FRAME_QUALITY = 60

		/** Room left around a previewed route, as a share of its own extent. */
		const val FIT_MARGIN = 0.12

		/** Everything for now, so the cost of each can be measured before any is made optional. */
		val LAYERS = setOf(WearMapLayer.ROUTE, WearMapLayer.MARKERS, WearMapLayer.MY_LOCATION)
		/** How often the pump looks again while waiting for a gesture to be drawn. */
		const val RENDER_INTERVAL_MS = 1000L / 15
		const val SILENCE_TIMEOUT_MS = 60_000L
		const val RESTART_TIMEOUT_MS = 2_000L
		/**
		 * How much wider than the watch each frame is drawn. A drag is not stopped when the
		 * surplus runs out, so this decides how often background is seen, not how far it moves.
		 */
		const val OVERSCAN = 1.5f
	}
}
