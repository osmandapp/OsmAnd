package net.osmand.plus.wear

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log

import com.google.android.gms.tasks.Tasks

import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable

import net.osmand.core.android.AtlasMapRendererView
import net.osmand.core.android.MapRendererContext
import net.osmand.core.android.MapRendererView
import net.osmand.core.jni.MapStylesCollection
import net.osmand.core.jni.PointI
import net.osmand.core.jni.ZoomLevel
import net.osmand.plus.OsmandApplication
import net.osmand.plus.views.corenative.NativeCoreContext
import net.osmand.wear.api.WearProtocol

import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow

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

	private var context: MapRendererContext? = null
	private var view: AtlasMapRendererView? = null
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
	 * only ask; the frame already in flight still carries the old view, and sending it would have
	 * the watch drop its own preview of the gesture and snap back before the real one arrived.
	 */
	@Volatile
	private var framesRendered = 0

	@Volatile
	private var awaitFrame = 0

	/**
	 * The gesture the renderer has been told about, written into every frame so the watch can
	 * tell a frame that answers its last gesture from one that was already being drawn.
	 */
	@Volatile
	private var appliedSeq = 0

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
		if (!NativeCoreContext.isInit()) {
			LOG.warn("Watch asked for the map, but the OpenGL core is not initialised")
			return
		}
		if (app.carNavigationSession != null) {
			LOG.info("Watch asked for the map while a car display is connected; refused")
			return
		}
		lastHeardFrom = SystemClock.elapsedRealtime()
		val collections = NativeCoreContext.getObfsCollections() ?: return
		running = true
		// Drawn wider than the watch so that a drag reveals map rather than black: the watch
		// shows the middle and slides the surplus into view while the finger moves.
		val frameWidth = (width * OVERSCAN).toInt()
		val frameHeight = (height * OVERSCAN).toInt()
		pump = Thread({ pump(nodeId, frameWidth, frameHeight, density, collections) }, "WearMapStreamer").also {
			it.start()
		}
	}

	fun setPaused(value: Boolean) {
		paused = value
		lastHeardFrom = SystemClock.elapsedRealtime()
	}

	@Synchronized
	fun stop() {
		running = false
		paused = false
		following = true
		appliedSeq = 0
		pump?.interrupt()
	}

	private fun pump(
		nodeId: String, width: Int, height: Int, density: Float,
		collections: Map<MapRendererContext.ProviderType, net.osmand.core.jni.ObfsCollection>
	) {
		try {
			openRenderer(width, height, density, collections)
			openChannel(nodeId)
			val stream = output ?: return
			var lastSent = ByteArray(0)
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
					Thread.sleep(FRAME_INTERVAL_MS)
					continue
				}
				if (framesRendered < awaitFrame) {
					Thread.sleep(RENDER_INTERVAL_MS)
					continue
				}
				if (following) {
					val box = app.osmandMap.mapView.currentRotatedTileBox
					view?.setTarget(PointI(box.center31X, box.center31Y))
				}
				val bitmap = view?.bitmap
				if (bitmap != null) {
					val grabbedAt = SystemClock.elapsedRealtime()
					val encoded = encode(bitmap)
					val encodedAt = SystemClock.elapsedRealtime()
					// An unchanged frame is not worth the radio: a still map at four frames a
					// second would otherwise spend as much power as a moving one.
					if (!encoded.contentEquals(lastSent)) {
						writeFrame(stream, encoded, appliedSeq)
						lastSent = encoded
						budget.record(encoded.size, encodedAt - grabbedAt,
							SystemClock.elapsedRealtime() - encodedAt)
					}
				}
				val elapsed = SystemClock.elapsedRealtime() - frameStartedAt
				val wait = FRAME_INTERVAL_MS - elapsed
				if (wait > 0) {
					Thread.sleep(wait)
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
		val renderer = view ?: return
		if (factor <= 0f) {
			return
		}
		val wanted = log2(scaleOf(renderer.zoom) * factor)
		val level = floor(wanted)
		val visual = 2.0.pow(wanted - level).toFloat()
		renderer.setZoom((level.toFloat() + visual - 1f).coerceIn(MIN_ZOOM, MAX_ZOOM))
		awaitFrame = framesRendered + FRAMES_IN_FLIGHT
	}

	/** How much ground a float zoom shows, in the renderer's own level-plus-visualZoom terms. */
	private fun scaleOf(zoom: Float): Double {
		val level = floor(zoom.toDouble())
		return 2.0.pow(level) * (1.0 + (zoom - level))
	}

	/**
	 * A drag on the watch moves the phone's viewport by the same distance on the ground: the
	 * screen point the finger started from is asked for its location, and the target moves by
	 * the difference. Doing the arithmetic in 31-coordinates would need the zoom and the
	 * projection, which the renderer already holds.
	 */
	fun pan(dx: Float, dy: Float, seq: Int) {
		lastHeardFrom = SystemClock.elapsedRealtime()
		appliedSeq = maxOf(appliedSeq, seq)
		following = false
		val renderer = view ?: return
		val centre = renderer.targetScreenPosition ?: return
		val from = PointI()
		val to = PointI()
		if (renderer.getLocationFromScreenPoint(centre, from)
				&& renderer.getLocationFromScreenPoint(
					PointI(centre.x - dx.toInt(), centre.y - dy.toInt()), to)) {
			val target = renderer.target ?: return
			renderer.setTarget(PointI(
				target.x + (to.x - from.x),
				target.y + (to.y - from.y)))
			awaitFrame = framesRendered + FRAMES_IN_FLIGHT
		}
	}

	fun recenter(seq: Int) {
		lastHeardFrom = SystemClock.elapsedRealtime()
		appliedSeq = maxOf(appliedSeq, seq)
		following = true
		val renderer = view ?: return
		val box = app.osmandMap.mapView.currentRotatedTileBox
		renderer.setTarget(PointI(box.center31X, box.center31Y))
		awaitFrame = framesRendered + FRAMES_IN_FLIGHT
	}

	private fun openRenderer(
		width: Int, height: Int, density: Float,
		collections: Map<MapRendererContext.ProviderType, net.osmand.core.jni.ObfsCollection>
	) {
		val rendererContext = MapRendererContext(app, density)
		rendererContext.setupObfMap(MapStylesCollection(), collections)

		val mapView = app.osmandMap.mapView
		val rendererView = AtlasMapRendererView(app)
		rendererContext.presetMapRendererOptions(rendererView, false)
		rendererView.setupRenderer(app, width, height, null)
		rendererView.setMinZoomLevel(ZoomLevel.swigToEnum(mapView.minZoom))
		rendererView.setMaxZoomLevel(ZoomLevel.swigToEnum(mapView.maxZoom))
		rendererView.setAzimuth(0f)
		rendererView.setElevationAngle(90f)
		rendererView.setZoom(mapView.zoom.toFloat())
		val box = mapView.currentRotatedTileBox
		rendererView.setTarget(PointI(box.center31X, box.center31Y))
		// Drawn far more often than it is sent: redrawing is GPU work that the measurements
		// found cheap, and it is what decides how soon a gesture is answered. Encoding, which
		// is the expensive half, still happens only FRAMES_PER_SECOND times a second.
		rendererView.setMaximumFrameRate(RENDER_FRAMES_PER_SECOND)
		rendererView.addListener(object : MapRendererView.MapRendererViewListener {
			override fun onUpdateFrame(view: MapRendererView) {}
			override fun onFrameReady(view: MapRendererView) {
				framesRendered++
			}
		})
		rendererContext.setMapRendererView(rendererView)

		context = rendererContext
		view = rendererView
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

		fun record(size: Int, encode: Long, write: Long) {
			frames++
			bytes += size
			encodeMs += encode
			writeMs += write
			val elapsed = SystemClock.elapsedRealtime() - since
			if (elapsed < REPORT_INTERVAL_MS) {
				return
			}
			LOG.info("Map stream: $frames frames in ${elapsed}ms"
					+ ", ${bytes / 1024} KB (${bytes * 1000 / elapsed / 1024} KB/s)"
					+ ", ${bytes / frames} B/frame"
					+ ", encode ${encodeMs / frames}ms/frame"
					+ ", write ${writeMs / frames}ms/frame"
					+ ", link busy ${writeMs * 100 / elapsed}%")
			since = SystemClock.elapsedRealtime()
			frames = 0
			bytes = 0
			encodeMs = 0
			writeMs = 0
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
		runCatching { context?.releaseMapRendererView(view) }
		runCatching { view?.stopRenderer() }
		output = null
		channel = null
		view = null
		context = null
		running = false
	}

	private companion object {
		private val LOG = net.osmand.PlatformUtil.getLog(WearMapStreamer::class.java)
		const val FRAMES_PER_SECOND = 4
		const val FRAME_INTERVAL_MS = 1000L / FRAMES_PER_SECOND
		const val FRAME_QUALITY = 60
		const val RENDER_FRAMES_PER_SECOND = 15
		const val RENDER_INTERVAL_MS = 1000L / RENDER_FRAMES_PER_SECOND
		/** One frame may already be on its way to the GPU when a gesture lands; the next is ours. */
		const val FRAMES_IN_FLIGHT = 2
		const val SILENCE_TIMEOUT_MS = 60_000L
		const val RESTART_TIMEOUT_MS = 2_000L
		/**
		 * How much wider than the watch each frame is drawn, to give a drag something to show.
		 * Half a screen of surplus on each side: a drag stops where the surplus ends, and at 1.5
		 * that came after a quarter of a screen, which had the hand repeating the gesture
		 * constantly. Covering a whole screen in one drag would need 3, and nine times the
		 * pixels of the watch's own screen to encode for every frame.
		 */
		const val OVERSCAN = 2.0f
		const val MIN_ZOOM = 3f
		const val MAX_ZOOM = 21f
	}
}
