package net.osmand.plus.wear

import android.graphics.Bitmap

import net.osmand.data.QuadRect
import net.osmand.data.RotatedTileBox

/** Where the watch's map frames come from: either of OsmAnd's two renderers. */
interface WearMapSource {

	val drawn: Int

	/** Frames that may already be on their way when a gesture lands, and cannot contain it. */
	val framesInFlight: Int

	/** False when this renderer cannot run here, leaving the stream to say so rather than hang. */
	fun open(width: Int, height: Int, density: Float): Boolean

	fun frame(): Bitmap?

	/** Where the last frame looks, for drawing over it. Null when this renderer cannot say. */
	fun overlayBox(): RotatedTileBox? = null

	fun fit(bounds: QuadRect) {}

	/** A scale factor, not zoom levels: the watch previews the gesture and the two must agree. */
	fun zoom(factor: Float)

	fun pan(dx: Float, dy: Float)

	fun followPhone()

	fun close()
}
