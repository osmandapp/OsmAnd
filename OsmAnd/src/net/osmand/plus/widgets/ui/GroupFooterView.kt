package net.osmand.plus.widgets.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import net.osmand.plus.R

/**
 * Explanatory text below a card, placed on the surface. It belongs to the card directly above it.
 * Never inside a card, never between two cards of the same group.
 *
 * The asymmetric 8dp / 16dp vertical padding is intentional: it ties the footer to the card
 * above it and keeps the gap to the next group.
 */
class GroupFooterView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

	private val textView: TextView

	init {
		val horizontal = resources.getDimensionPixelSize(R.dimen.content_padding)
		val top = resources.getDimensionPixelSize(R.dimen.ui_group_footer_padding_top)
		val bottom = resources.getDimensionPixelSize(R.dimen.content_padding)
		setPaddingRelative(horizontal, top, horizontal, bottom)

		inflate(context, R.layout.ui_group_footer, this)
		textView = findViewById(R.id.text)

		attrs?.let { attributeSet ->
			val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.GroupFooterView)
			try {
				setText(typedArray.getString(R.styleable.GroupFooterView_groupFooterText))
			} finally {
				typedArray.recycle()
			}
		}
	}

	fun setText(text: CharSequence?) {
		textView.text = text
	}

	fun setText(textId: Int) {
		textView.setText(textId)
	}
}
