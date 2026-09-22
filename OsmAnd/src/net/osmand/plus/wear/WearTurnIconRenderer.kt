package net.osmand.plus.wear

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff

import net.osmand.plus.OsmandApplication
import net.osmand.plus.views.TurnPathHelper.RouteDrawable
import net.osmand.router.TurnType

import java.io.ByteArrayOutputStream

/**
 * Renders manoeuvre arrows for the watch with the drawable the Turn-by-turn card already uses,
 * so the glyphs stay identical to the ones in route details — including roundabout exits and
 * every turn variant — instead of a second hand-drawn set that would drift from the app.
 */
class WearTurnIconRenderer(private val app: OsmandApplication) {

	private val drawable by lazy { RouteDrawable(app, true) }
	private val density = app.resources.displayMetrics.density

	private val size: Int
		get() = (ICON_SIZE_DP * density).toInt()

	/** PNG bytes of the arrow for [turnType], or null if it could not be drawn. */
	fun render(turnType: TurnType): ByteArray? = try {
		val side = size
		val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
		drawable.setBounds(0, 0, side, side)
		drawable.setRouteType(turnType)

		val canvas = Canvas(bitmap)
		canvas.drawColor(0, PorterDuff.Mode.CLEAR)
		drawable.draw(canvas)

		ByteArrayOutputStream().use { out ->
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
			bitmap.recycle()
			out.toByteArray()
		}
	} catch (e: Exception) {
		null
	}

	companion object {
		private const val ICON_SIZE_DP = 36f
	}
}
