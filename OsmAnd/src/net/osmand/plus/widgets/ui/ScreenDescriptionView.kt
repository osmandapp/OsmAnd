package net.osmand.plus.widgets.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import net.osmand.plus.R

/**
 * Intro text at the top of a screen, above all cards. One per screen, never inside a card.
 * It explains what the screen is for - it never repeats the title and never carries a value,
 * a state or a warning.
 */
class ScreenDescriptionView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

	private val textView: TextView

	init {
		val padding = resources.getDimensionPixelSize(R.dimen.content_padding)
		setPaddingRelative(padding, padding, padding, padding)

		inflate(context, R.layout.ui_screen_description, this)
		textView = findViewById(R.id.text)

		attrs?.let { attributeSet ->
			val typedArray =
				context.obtainStyledAttributes(attributeSet, R.styleable.ScreenDescriptionView)
			try {
				setText(typedArray.getString(R.styleable.ScreenDescriptionView_screenDescriptionText))
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
