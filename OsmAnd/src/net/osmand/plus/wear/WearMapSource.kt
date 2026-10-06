package net.osmand.plus.wear

import android.graphics.Bitmap

import net.osmand.data.QuadRect
import net.osmand.data.RotatedTileBox

/**
 * Where the watch's map frames come from.
 *
 * OsmAnd has two renderers and they suit the watch differently, so the stream is written against
 * neither. The OpenGL one draws what the phone draws, at the cost of a second core context; the
 * legacy one draws straight into a bitmap of whatever size is asked for, which is the shape this
 * feature needs and far less memory, but it is the older renderer and not what the phone shows.
 */
interface WearMapSource {

	/**
	 * Frames drawn so far. A gesture must not be answered with a frame that was drawn before the
	 * phone heard it, and this is how the stream tells one from the other.
	 */
	val drawn: Int

	/**
	 * How many frames may already be on their way when a gesture lands. Drawing that happens on
	 * this thread answers a gesture immediately; drawing handed to a GPU does not.
	 */
	val framesInFlight: Int

	/** False when this renderer cannot run here, leaving the stream to say so rather than hang. */
	fun open(width: Int, height: Int, density: Float): Boolean

	fun frame(): Bitmap?

	/**
	 * Where the last frame is looking, for drawing the route and the rest over it. Null when
	 * this renderer cannot say in terms a canvas understands, and the frame then goes out
	 * without overlays rather than with wrong ones.
	 */
	fun overlayBox(): RotatedTileBox? = null

	/** Frames the given ground so all of it is in view. Ignored by renderers that cannot. */
	fun fit(bounds: QuadRect) {}

	/** A scale factor, not zoom levels: the watch previews the gesture and the two must agree. */
	fun zoom(factor: Float)

	/** A drag in frame pixels, moving the ground under it by the same distance. */
	fun pan(dx: Float, dy: Float)

	/** Puts the view back on whatever the phone's own map is centred on. */
	fun followPhone()

	fun close()
}
