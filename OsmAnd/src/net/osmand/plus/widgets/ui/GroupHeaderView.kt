package net.osmand.plus.widgets.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.view.ViewCompat
import com.google.android.material.button.MaterialButton
import net.osmand.plus.R
import net.osmand.plus.widgets.TextViewEx

/**
 * Title above a card, placed on the surface. Its scope is the card directly below it.
 * Never place it inside a card.
 */
class GroupHeaderView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

	private val titleView: TextViewEx
	private val trailingActionView: MaterialButton

	init {
		orientation = HORIZONTAL
		minimumHeight = resources.getDimensionPixelSize(R.dimen.ui_group_header_min_height)
		val horizontal = resources.getDimensionPixelSize(R.dimen.content_padding)
		val vertical = resources.getDimensionPixelSize(R.dimen.ui_group_header_padding_vertical)
		setPaddingRelative(horizontal, vertical, horizontal, vertical)

		inflate(context, R.layout.ui_group_header, this)
		titleView = findViewById(R.id.title)
		trailingActionView = findViewById(R.id.trailing_action)
		ViewCompat.setAccessibilityHeading(titleView, true)

		attrs?.let { attributeSet ->
			val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.GroupHeaderView)
			try {
				setTitle(typedArray.getString(R.styleable.GroupHeaderView_groupHeaderTitle))
				val icon = typedArray.getResourceId(
					R.styleable.GroupHeaderView_groupHeaderTrailingIcon, 0)
				if (icon != 0) {
					setTrailingAction(
						icon,
						typedArray.getString(
							R.styleable.GroupHeaderView_groupHeaderTrailingContentDescription),
						null)
				}
			} finally {
				typedArray.recycle()
			}
		}
	}

	fun setTitle(title: CharSequence?) {
		titleView.text = title
	}

	fun setTitle(titleId: Int) {
		titleView.setText(titleId)
	}

	/**
	 * @param contentDescription is required by the design - the icon alone never explains the action.
	 */
	fun setTrailingAction(
		@DrawableRes iconId: Int,
		contentDescription: CharSequence?,
		listener: View.OnClickListener?
	) {
		trailingActionView.setIconResource(iconId)
		trailingActionView.contentDescription = contentDescription
		trailingActionView.setOnClickListener(listener)
		trailingActionView.visibility = View.VISIBLE
	}

	fun hideTrailingAction() {
		trailingActionView.setOnClickListener(null)
		trailingActionView.visibility = View.GONE
	}
}
