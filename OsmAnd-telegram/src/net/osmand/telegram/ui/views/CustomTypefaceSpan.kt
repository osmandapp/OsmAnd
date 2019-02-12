package net.osmand.telegram.ui.views

import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan

class CustomTypefaceSpan(val font: Typeface) : MetricAffectingSpan() {

	override fun updateMeasureState(textPaint: TextPaint) = update(textPaint)


	override fun updateDrawState(textPaint: TextPaint) = update(textPaint)

	private fun update(textPaint: TextPaint) {
		textPaint.apply {
			val old = typeface
			val oldStyle = old?.style ?: 0
			typeface = Typeface.create(font, oldStyle)
		}
	}
}