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
 * Renders the map at the watch's size and streams it there.
 *
 * A second renderer rather than a share of the phone's: the watch has its own viewport, its own
 * zoom and its own heading, and the core is happy to run two — measured on a Pixel Watch paired
 * with a Galaxy S23, the phone's own map keeps rendering throughout. It is not free, so the
 * renderer exists only while the watch has the map screen open: about 87 MB of pss, most of it
 * the second set of tile and symbol providers rather than the frame buffers.
 *
 * Frames go over a channel rather than as data items: they are a stream, and the Data Layer
 * would dedupe and retain them. Each is a four byte length then that many bytes of WebP, about
 * 15 KB for a 384x384 frame at quality 60.
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

	/**
	 * Whether the watch is still showing whatever the phone's map shows. Dragging on the watch
	 * breaks it, the way dragging on the phone unpins it from your position there, and a double
	 * tap pins it back. Zoom stays the watch's own either way: its screen is not the phone's, and
	 * a bezel turn that the next frame undid would be worse than no bezel at all.
	 */
	@Volatile
	private var following = true

	/**
	 * Renderer frames seen so far, and the count a gesture is waiting for. setZoom and setTarget
	 * only ask; a frame already being drawn still carries the old view, and sending it would have
	 * the watch drop its own preview of the gesture and snap back before the real one arrived.
	 *
	 * It counts frames of the source it was set against, so it means nothing once that source is
	 * gone and has to be cleared with it. A count left over from a previous one is a wait that
	 * never ends: the legacy renderer only draws when the pump asks, and the pump is waiting.
	 */
	@Volatile
	private var awaitFrame = 0

	/**
	 * The gesture the renderer has been told about, written into every frame so the watch can
	 * tell a frame that answers its last gesture from one that was already being drawn.
	 */
	@Volatile
	private var appliedSeq = 0


	/**
	 * Woken when a gesture lands, rather than slept through. The pump spends most of its life
	 * waiting - for the frame interval, or for the watch to lift its finger - and a gesture
	 * arriving in the middle of that used to wait the rest of it out. Measured, those waits
	 * were around two thirds of a second of the second and a half a gesture took to answer,
	 * against fifty milliseconds to actually draw the map.
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
		// Restarted rather than ignored when one is already up. A stream that ended badly — the
		// link dropped, the watch slept, the channel died — can leave the flag set, and a start
		// that quietly does nothing leaves the watch reading "loading" until the phone app is
		// killed. Asking twice must always give a working stream.
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
		// Drawn wider than the watch so that a drag reveals map rather than black: the watch
		// shows the middle and slides the surplus into view while the finger moves.
		val frameWidth = (width * OVERSCAN).toInt()
		val frameHeight = (height * OVERSCAN).toInt()
		pump = Thread({ pump(nodeId, frameWidth, frameHeight, density) }, "WearMapStreamer").also {
			it.start()
		}
	}

	/**
	 * Swaps the renderer under a running stream. The choice lives on the phone because the
	 * renderer does, and the watch only asks; changing it has to take effect now rather than
	 * the next time the map screen is opened, or the setting cannot be compared.
	 *
	 * Only the preference is written here. Reopening the stream from this thread would have it
	 * wait for the pump to finish while holding the very lock the pump needs to finish on, so
	 * the pump picks the change up on its own next turn instead.
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
				if (following) {
					renderer.followPhone()
				}
				// Read before drawing, not after: a gesture landing while the frame is being
				// drawn is not in it, however early it set the sequence. Stamping with the
				// later number had the watch take such a frame for the answer, drop its
				// preview of the gesture, and jump back until the real answer arrived.
				val seqForFrame = appliedSeq
				val bitmap = renderer.frame()
				if (bitmap != null) {
					// Drawn onto the rendered frame rather than composed on the watch: the
					// route has to line up with the map to the pixel, and only this side knows
					// where the camera was when the frame was drawn.
					val overlayMs = renderer.overlayBox()?.let { box ->
						overlay.draw(Canvas(bitmap), box, LAYERS)
					} ?: 0L
					val grabbedAt = SystemClock.elapsedRealtime()
					val encoded = encode(bitmap)
					val encodedAt = SystemClock.elapsedRealtime()
					// An unchanged frame is not worth the radio: a still map at four frames a
					// second would otherwise spend as much power as a moving one. A gesture is
					// the exception, and must be answered even when it changed nothing on screen:
					// the watch is holding its own preview of it and waiting to be told the phone
					// has it, and silence there is a map that never moves again.
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

	/**
	 * What a frame actually costs, so the frame rate can be argued from measurements. Encoding is
	 * this thread's own CPU; the write is how long the Bluetooth link takes the bytes, which is the
	 * ceiling on how often frames can be sent at all.
	 */
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

		/**
		 * Which of OsmAnd's two renderers draws the watch's map. The legacy one draws into a
		 * bitmap of any size on the asking thread, which is the shape this feature needs and
		 * costs no second core context; the OpenGL one shows exactly what the phone shows.
		 */
		fun legacyRenderer(app: OsmandApplication) =
			app.settings.registerBooleanPreference("wear_legacy_map_renderer", true)
				// Global, because it is a fact about the watch rather than about how you are
				// travelling. Left to the default it would be a profile setting, and the choice
				// would quietly revert whenever the profile changed.
				.makeGlobal()

		private val LOG = net.osmand.PlatformUtil.getLog(WearMapStreamer::class.java)
		const val FRAMES_PER_SECOND = 4
		const val FRAME_INTERVAL_MS = 1000L / FRAMES_PER_SECOND
		const val FRAME_QUALITY = 60

		/** Everything for now, so the cost of each can be measured before any is made optional. */
		val LAYERS = setOf(WearMapLayer.ROUTE, WearMapLayer.MARKERS, WearMapLayer.MY_LOCATION)
		/** How often the pump looks again while waiting for a gesture to be drawn. */
		const val RENDER_INTERVAL_MS = 1000L / 15
		const val SILENCE_TIMEOUT_MS = 60_000L
		const val RESTART_TIMEOUT_MS = 2_000L
		/**
		 * How much wider than the watch each frame is drawn, to give a drag something to show.
		 * A quarter of a screen of surplus on each side. A drag is not stopped when it runs out,
		 * only left showing background until the phone answers, so this decides how often that is
		 * seen rather than how far the map may be moved. More of it is more pixels to encode and
		 * send on a link that already takes over a second to answer a gesture.
		 */
		const val OVERSCAN = 1.5f
	}
}
