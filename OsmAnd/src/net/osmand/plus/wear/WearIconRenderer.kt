package net.osmand.plus.wear

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable

import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

import net.osmand.plus.OsmandApplication
import net.osmand.plus.views.TurnPathHelper.RouteDrawable
import net.osmand.router.TurnType

import java.io.ByteArrayOutputStream

/**
 * Renders the glyphs the watch shows, using the very drawables the phone app uses, so the two
 * never drift apart. Everything is drawn white; the watch tints it to whatever its own palette
 * calls for.
 */
class WearIconRenderer(private val app: OsmandApplication) {

	private val turnDrawable by lazy { RouteDrawable(app, true) }
	private val density = app.resources.displayMetrics.density

	private val size: Int
		get() = (ICON_SIZE_DP * density).toInt()

	/** PNG bytes of the manoeuvre arrow for [turnType], or null if it could not be drawn. */
	fun renderTurn(turnType: TurnType): ByteArray? = render { canvas, side ->
		turnDrawable.setBounds(0, 0, side, side)
		turnDrawable.setRouteType(turnType)
		turnDrawable.draw(canvas)
	}

	/** PNG bytes of an ordinary drawable resource, such as a profile glyph. */
	fun renderDrawable(@DrawableRes resId: Int): ByteArray? {
		val drawable: Drawable = ContextCompat.getDrawable(app, resId) ?: return null
		return render { canvas, side ->
			drawable.setBounds(0, 0, side, side)
			drawable.setTint(android.graphics.Color.WHITE)
			drawable.draw(canvas)
		}
	}

	private inline fun render(draw: (Canvas, Int) -> Unit): ByteArray? = try {
		val side = size
		val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(bitmap)
		canvas.drawColor(0, PorterDuff.Mode.CLEAR)
		draw(canvas, side)
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
