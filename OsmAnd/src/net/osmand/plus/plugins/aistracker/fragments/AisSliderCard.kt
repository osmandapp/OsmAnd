package net.osmand.plus.plugins.aistracker.fragments

import android.view.View
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.google.android.material.slider.Slider
import net.osmand.plus.R
import net.osmand.plus.widgets.ui.GroupFooterView

/**
 * Binds a slider card ([R.layout.ui_slider_card]) to a non-linear scale.
 *
 * The handle is mapped to the index of [values], not to a value range, and the value is committed
 * immediately - there is no Save button on these screens.
 */
class AisSliderCard<T>(
	val view: View,
	private val footer: GroupFooterView,
	private val values: List<T>,
	private val formatValue: (T) -> String,
	private val formatFooter: (T) -> CharSequence,
	private val onValueChanged: (T) -> Unit
) {

	private val titleView: TextView = view.findViewById(R.id.title)
	private val valueView: TextView = view.findViewById(R.id.value)
	private val slider: Slider = view.findViewById(R.id.slider)

	init {
		slider.valueFrom = 0f
		slider.valueTo = (values.size - 1).toFloat()
		slider.stepSize = 1f
		slider.addOnChangeListener { _, value, fromUser ->
			val selected = values[value.toInt()]
			updateLabels(selected)
			if (fromUser) {
				onValueChanged(selected)
			}
		}
	}

	fun setTitle(titleId: Int) {
		titleView.setText(titleId)
	}

	/**
	 * Puts the handle on [value]. A value that is not on the scale - written by an older version
	 * or an imported profile - is replaced by the first stop, and the replacement is committed,
	 * so the preference and the screen never disagree.
	 */
	fun setValue(value: T) {
		var index = values.indexOf(value)
		if (index < 0) {
			index = 0
			onValueChanged(values[index])
		}
		slider.value = index.toFloat()
		updateLabels(values[index])
	}

	fun refreshFooter() {
		updateLabels(values[slider.value.toInt()])
	}

	private fun updateLabels(value: T) {
		val formatted = formatValue(value)
		valueView.text = formatted
		/* TalkBack must read "5 minutes", not the index the handle sits on */
		ViewCompat.setStateDescription(slider, formatted)
		footer.setText(formatFooter(value))
	}
}
