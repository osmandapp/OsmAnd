package net.osmand.plus.widgets.ui

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import net.osmand.plus.R

/**
 * Rows of a settings group are separate surfaces with a small gap between them, not one card
 * with dividers. The corner radius of a row depends on its position in the group, and there is
 * no MDC component for that - so the shape is chosen here.
 */
object SegmentedList {

	/**
	 * Applies the background and the gap to every direct child of [container].
	 * Children with visibility GONE are skipped, so hiding a row keeps the group shape correct.
	 */
	@JvmStatic
	fun apply(container: ViewGroup) {
		val visible = ArrayList<View>()
		for (i in 0 until container.childCount) {
			val child = container.getChildAt(i)
			if (child.visibility != View.GONE) {
				visible.add(child)
			}
		}
		val gap = container.resources.getDimensionPixelSize(R.dimen.ui_segment_gap)
		for ((index, child) in visible.withIndex()) {
			child.setBackgroundResource(backgroundFor(index, visible.size))
			val params = child.layoutParams
			if (params is LinearLayout.LayoutParams) {
				params.topMargin = if (index == 0) 0 else gap
				child.layoutParams = params
			}
		}
	}

	private fun backgroundFor(index: Int, count: Int): Int = when {
		count == 1 -> R.drawable.bg_ui_segment_single
		index == 0 -> R.drawable.bg_ui_segment_first
		index == count - 1 -> R.drawable.bg_ui_segment_last
		else -> R.drawable.bg_ui_segment_middle
	}
}
