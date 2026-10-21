package net.osmand.plus.gallery.ui

import android.graphics.Rect
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.AndroidUtils
import kotlin.math.roundToInt

class SectionGridGeometry(
	val span: Int,
	contentWidth: Int,
	private val outer: Int,
	private val columnGap: Int,
	private val rowGap: Int
) {
	val cellSize: Int = ((contentWidth - 2 * outer - (span - 1) * columnGap) / span).coerceAtLeast(1)

	val columnBorders: IntArray = columnBorders(span, contentWidth)

	fun cellInsets(index: Int, last: Int, rtl: Boolean, out: Rect) {
		val column = index % span
		val spread = columnGap - 2 * outer
		val leading = outer + column * spread / span
		val trailing = outer + (span - column - 1) * spread / span
		if (rtl) {
			out.left = trailing
			out.right = leading
		} else {
			out.left = leading
			out.right = trailing
		}
		out.top = if (index < span) outer else rowGap
		out.bottom = if (index / span == last / span) outer else 0
	}

	companion object {
		const val COLUMN_GAP_DP = 8f
		const val ROW_GAP_DP = 6f

		fun of(app: OsmandApplication, span: Int, contentWidth: Int) = SectionGridGeometry(
			span, contentWidth,
			app.resources.getDimensionPixelSize(R.dimen.content_padding),
			AndroidUtils.dpToPxF(app, COLUMN_GAP_DP).roundToInt(),
			AndroidUtils.dpToPxF(app, ROW_GAP_DP).roundToInt()
		)

		fun columnBorders(span: Int, totalSpace: Int): IntArray {
			val borders = IntArray(span + 1)
			val sizePerSpan = totalSpace / span
			val remainder = totalSpace % span
			var consumed = 0
			var additional = 0
			for (index in 1..span) {
				var size = sizePerSpan
				additional += remainder
				if (additional > 0 && span - additional < remainder) {
					size += 1
					additional -= span
				}
				consumed += size
				borders[index] = consumed
			}
			return borders
		}
	}
}
