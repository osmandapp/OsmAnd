package net.osmand.telegram.ui.views

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import net.osmand.telegram.R
import net.osmand.telegram.helpers.FontCache

class TextViewEx : AppCompatTextView {

	constructor(context: Context) : super(context)

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		parseAttrs(attrs, 0)
	}

	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
		context,
		attrs,
		defStyleAttr
	) {
		parseAttrs(attrs, defStyleAttr)
	}

	private fun parseAttrs(attrs: AttributeSet?, defStyleAttr: Int) {
		if (attrs == null) {
			return
		}

		val resolvedAttrs =
			context.theme.obtainStyledAttributes(attrs, R.styleable.TextViewEx, defStyleAttr, 0)
		applyAttrTypeface(resolvedAttrs)
		resolvedAttrs.recycle()
	}

	private fun applyAttrTypeface(resolvedAttrs: TypedArray) {
		if (isInEditMode || !resolvedAttrs.hasValue(R.styleable.TextViewEx_typeface)) {
			return
		}

		val typefaceName = resolvedAttrs.getString(R.styleable.TextViewEx_typeface)
		val typeface = FontCache.getFont(context, typefaceName!!)
		if (typeface != null) {
			val style = getTypeface()?.style ?: 0
			setTypeface(typeface, style)
		}
	}
}
