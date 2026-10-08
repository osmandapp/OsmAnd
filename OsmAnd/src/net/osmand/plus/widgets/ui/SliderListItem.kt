package net.osmand.plus.widgets.ui

import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.TickVisibilityMode
import net.osmand.plus.R

/**
 * Binds a row of a segmented settings list with a title, the value on the right and a slider
 * below ([R.layout.item_ui_slider_list_item]). The row has no background of its own -
 * [SegmentedList] gives it the shape of its position, like any other row, so a slider can follow
 * the switch it depends on in the same group. Never put a slider into a card of its own.
 *
 * - The value is committed immediately, there is no Save button: the listener is called while the
 *   handle moves.
 * - The screen formats the value and its units, the row hardcodes neither. TalkBack reads the same
 *   text, never the position of the handle.
 * - Stops ([setTickVisible]) only for a discrete scale - a fixed set of values. A continuous scale
 *   has no stops.
 */
class SliderListItem(val view: View) {

	private val titleView: TextView = view.findViewById(R.id.title)
	private val valueView: TextView = view.findViewById(R.id.value)
	private val slider: RangeSlider = view.findViewById(R.id.slider)

	init {
		/* the screen sets the value from its preferences - restored view state would land in
		 * another row, because all rows of a screen share the child ids */
		slider.isSaveEnabled = false
		setTickVisible(false)
	}

	fun setTitle(title: CharSequence?) {
		titleView.text = title
	}

	fun setTitle(@StringRes titleId: Int) {
		titleView.setText(titleId)
	}

	/**
	 * Stops are drawn only when all of them fit on the track. A track too short for every stop
	 * shows none, rather than fewer stops than the values the handle snaps to.
	 */
	fun setTickVisible(visible: Boolean) {
		slider.tickVisibilityMode = if (visible) {
			TickVisibilityMode.TICK_VISIBILITY_AUTO_HIDE
		} else {
			TickVisibilityMode.TICK_VISIBILITY_HIDDEN
		}
	}

	/**
	 * A fixed list of at least two values. The handle sits on an index of [stops], so the steps
	 * don't have to be even. A [value] that is not on the scale - written by an older version or an
	 * imported profile - is replaced by the first stop, and the replacement is committed, so the
	 * preference and the screen never disagree.
	 *
	 * @param onValueChanged commits the value, called when the user moves the handle.
	 */
	fun <T> setStops(
		stops: List<T>,
		value: T,
		formatValue: (T) -> CharSequence,
		onValueChanged: (T) -> Unit
	) {
		var index = stops.indexOf(value)
		if (index < 0) {
			index = 0
			onValueChanged(stops[index])
		}
		bind(0f, (stops.size - 1).toFloat(), 1f, listOf(index.toFloat()),
			{ values -> formatValue(stops[values[0].toInt()]) },
			{ values -> onValueChanged(stops[values[0].toInt()]) })
	}

	/**
	 * One handle on a continuous scale. With [stepSize] 0 the handle moves freely and the screen
	 * rounds the value. Call it again to move the bounds, e.g. when they depend on another row -
	 * but not from its own [onValueChanged].
	 *
	 * @param onValueChanged commits the value, called when the user moves the handle.
	 */
	fun setContinuous(
		from: Float,
		to: Float,
		stepSize: Float,
		value: Float,
		formatValue: (Float) -> CharSequence,
		onValueChanged: (Float) -> Unit
	) {
		bind(from, to, stepSize, listOf(value),
			{ values -> formatValue(values[0]) },
			{ values -> onValueChanged(values[0]) })
	}

	private fun bind(
		from: Float,
		to: Float,
		stepSize: Float,
		values: List<Float>,
		formatValues: (List<Float>) -> CharSequence,
		onValuesChanged: (List<Float>) -> Unit
	) {
		slider.clearOnChangeListeners()
		slider.valueFrom = from
		slider.valueTo = to
		slider.stepSize = stepSize
		slider.values = values.map { it.coerceIn(from, to) }
		updateValueText(formatValues(slider.values))
		slider.addOnChangeListener { rangeSlider, _, fromUser ->
			if (fromUser) {
				onValuesChanged(rangeSlider.values)
			}
			updateValueText(formatValues(rangeSlider.values))
		}
	}

	private fun updateValueText(text: CharSequence) {
		valueView.text = text
		/* TalkBack must read "15 min", not the index the handle sits on */
		ViewCompat.setStateDescription(slider, text)
	}
}
