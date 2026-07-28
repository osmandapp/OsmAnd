package net.osmand.plus.views.mapwidgets.appearance

import android.content.res.ColorStateList
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.views.mapwidgets.OutlinedTextContainer

object PanelAppearanceApplier {

	@JvmStatic
	@JvmOverloads
	fun applyBackground(
		view: View,
		appearance: ResolvedPanelAppearance,
		shape: WidgetBackgroundShape = WidgetBackgroundShape.RECTANGLE
	) {
		val background = appearance.background
		val drawableRes = background.getDrawableRes(shape)
		if (!background.isTinted) {
			view.setBackgroundResource(drawableRes)
			return
		}
		val drawable = AppCompatResources.getDrawable(view.context, drawableRes)
		if (drawable == null) {
			view.setBackgroundResource(drawableRes)
			return
		}
		val independentDrawable = DrawableCompat.wrap(drawable).mutate()
		DrawableCompat.setTintList(independentDrawable, background.tintColors)
		ViewCompat.setBackground(view, independentDrawable)
	}

	@JvmStatic
	fun applyDivider(view: View?, appearance: ResolvedPanelAppearance) {
		view?.setBackgroundColor(appearance.dividerColor)
	}

	@JvmStatic
	fun applyStandaloneDivider(view: View?, appearance: ResolvedPanelAppearance) {
		view?.setBackgroundColor(appearance.standaloneDividerColor)
	}

	@JvmStatic
	fun applyPrimaryText(text: OutlinedTextContainer?, appearance: ResolvedPanelAppearance) {
		applyOutlinedText(text, null, appearance.primaryTextColor, appearance)
	}

	@JvmStatic
	fun applyPrimaryText(
		text: OutlinedTextContainer?,
		shadow: TextView?,
		appearance: ResolvedPanelAppearance
	) {
		applyOutlinedText(text, shadow, appearance.primaryTextColor, appearance)
	}

	@JvmStatic
	fun applySecondaryText(text: OutlinedTextContainer?, appearance: ResolvedPanelAppearance) {
		applyOutlinedText(text, null, appearance.secondaryTextColor, appearance)
	}

	@JvmStatic
	fun applySecondaryText(
		text: OutlinedTextContainer?,
		shadow: TextView?,
		appearance: ResolvedPanelAppearance
	) {
		applyOutlinedText(text, shadow, appearance.secondaryTextColor, appearance)
	}

	@JvmStatic
	fun applyPrimaryText(text: TextView?, shadow: TextView?, appearance: ResolvedPanelAppearance) {
		applyPrimaryText(text, shadow, appearance, appearance.textShadowRadius)
	}

	@JvmStatic
	fun applyPrimaryText(
		text: TextView?,
		shadow: TextView?,
		appearance: ResolvedPanelAppearance,
		shadowRadius: Int
	) {
		applyTextView(text, shadow, appearance.primaryTextColor, appearance, shadowRadius)
	}

	@JvmStatic
	fun applySecondaryText(text: TextView?, shadow: TextView?, appearance: ResolvedPanelAppearance) {
		applyTextView(text, shadow, appearance.secondaryTextColor, appearance)
	}

	@JvmStatic
	fun applySecondaryIcon(icon: ImageView?, appearance: ResolvedPanelAppearance) {
		applyIcon(icon, appearance.secondaryTextColor)
	}

	@JvmStatic
	fun applyOutline(text: OutlinedTextContainer?, appearance: ResolvedPanelAppearance) {
		if (text == null) {
			return
		}
		if (appearance.textShadowRadius > 0) {
			text.setStrokeWidth(appearance.textShadowRadius)
			text.setStrokeColor(appearance.textShadowColor)
			text.showOutline(true)
		} else {
			text.showOutline(false)
		}
		text.invalidateTextViews()
	}

	private fun applyOutlinedText(
		text: OutlinedTextContainer?,
		shadow: TextView?,
		color: Int,
		appearance: ResolvedPanelAppearance
	) {
		applyTextShadow(shadow, appearance)
		if (text != null) {
			val typefaceStyle = if (appearance.boldText) Typeface.BOLD else Typeface.NORMAL
			text.setTextColor(color)
			text.setTypeface(Typeface.DEFAULT, typefaceStyle)
			if (shadow != null) {
				text.showOutline(false)
				text.invalidateTextViews()
			} else {
				applyOutline(text, appearance)
			}
		}
	}

	private fun applyTextView(
		text: TextView?,
		shadow: TextView?,
		color: Int,
		appearance: ResolvedPanelAppearance,
		shadowRadius: Int = appearance.textShadowRadius
	) {
		val typefaceStyle = if (appearance.boldText) Typeface.BOLD else Typeface.NORMAL
		applyTextShadow(shadow, appearance, shadowRadius)
		if (text != null) {
			text.setTextColor(color)
			text.setTypeface(Typeface.DEFAULT, typefaceStyle)
		}
	}

	private fun applyTextShadow(
		shadow: TextView?,
		appearance: ResolvedPanelAppearance,
		shadowRadius: Int = appearance.textShadowRadius
	) {
		if (shadow == null) {
			return
		}
		if (shadowRadius > 0) {
			val typefaceStyle = if (appearance.boldText) Typeface.BOLD else Typeface.NORMAL
			AndroidUiHelper.updateVisibility(shadow, true)
			shadow.setTypeface(Typeface.DEFAULT, typefaceStyle)
			shadow.paint.strokeWidth = shadowRadius.toFloat()
			shadow.paint.style = Paint.Style.STROKE
			shadow.setTextColor(appearance.textShadowColor)
			shadow.invalidate()
		} else {
			AndroidUiHelper.updateVisibility(shadow, false)
		}
	}

	private fun applyIcon(icon: ImageView?, color: Int) {
		icon?.let { ImageViewCompat.setImageTintList(it, ColorStateList.valueOf(color)) }
	}
}