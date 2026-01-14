package net.osmand.plus.plugins.astro.views

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.quickaction.ButtonAppearanceParams
import net.osmand.plus.render.RenderingIcons
import net.osmand.plus.settings.enums.CompassMode
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.views.controls.maphudbuttons.CompassDrawable
import net.osmand.plus.views.controls.maphudbuttons.MapButton
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState
import net.osmand.shared.grid.ButtonPositionSize
import net.osmand.util.Algorithms

class StarCompassButton @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : MapButton(context, attrs, defStyleAttr) {

	private val buttonState = StarCompassButtonState(app)
	var onSingleTap: (() -> Unit)? = null

	class StarCompassButtonState(app: OsmandApplication) :
		MapButtonState(app, "starmap.view.compass") {

		override fun getName() = app.getString(R.string.map_widget_compass)

		override fun getDescription() = app.getString(R.string.key_event_action_change_map_orientation)

		override fun isEnabled() = true

		override fun getVisibilityPref() = error("Not implemented")

		override fun getDefaultLayoutId() = R.layout.star_map_compass_button

		override fun createAppearanceParams(nightMode: Boolean?): ButtonAppearanceParams {
			return ButtonAppearanceParams("ic_compass_white", 48, 1.0f, 24)
		}

		override fun getDefaultIconName(nightMode: Boolean?): String {
			val compassMode = CompassMode.MANUALLY_ROTATED
			val mode = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP)
			return app.getResources().getResourceEntryName(compassMode.iconId.getIconId(mode))
		}

		override fun getIcon(@DrawableRes iconId: Int, @ColorInt color: Int, nightMode: Boolean,
			mapIcon: Boolean): Drawable? {
			val drawable = super.getIcon(iconId, color, nightMode, mapIcon)
			return if (mapIcon && drawable != null)
				CompassDrawable(drawable).apply { linkedToMapRotation = false }
			else drawable
		}

		protected override fun setupButtonPosition(position: ButtonPositionSize) =
			setupButtonPosition(position, ButtonPositionSize.POS_LEFT, ButtonPositionSize.POS_TOP, false, true)
	}

	init {
		setupTouchListener()
	}

	override fun getButtonState() = buttonState

	fun update(mapRotation: Float, animated: Boolean = false) {
		super.update()
		val drawable = imageView.drawable
		if (drawable is CompassDrawable) {
			if (drawable.mapRotation != mapRotation) {
				if (animated) {
					// Calculate the shortest rotation distance
					var diff = mapRotation - drawable.mapRotation
					if (diff > 180f) diff -= 360f
					if (diff < -180f) diff += 360f

					// Animate the rotation
					val animator = ValueAnimator.ofFloat(drawable.mapRotation, drawable.mapRotation + diff)
					animator.duration = 400 // Set duration in milliseconds
					animator.addUpdateListener { animation ->
						drawable.mapRotation = animation.animatedValue as Float
						imageView.invalidate()
					}
					animator.start()
				} else {
					// Update immediately without animation
					drawable.mapRotation = mapRotation
					imageView.invalidate()
				}
			}
		}
	}

	override fun updateColors(nightMode: Boolean) {
		setBackgroundColors(
			ColorUtilities.getMapButtonBackgroundColor(context, nightMode),
			ColorUtilities.getMapButtonBackgroundPressedColor(context, nightMode)
		)
	}

	override fun updateIcon() {
		val iconName = appearanceParams.iconName
		var iconId = AndroidUtils.getDrawableId(app, iconName)
		if (iconId == 0) {
			iconId = RenderingIcons.getBigIconResourceId(iconName)
		}
		val customIcon = !CompassMode.isCompassIconId(iconId)
		setIconColor(
			if (customIcon) ColorUtilities.getMapButtonIconColor(
				context,
				nightMode
			) else 0
		)

		super.updateIcon()
	}

	@SuppressLint("ClickableViewAccessibility")
	private fun setupTouchListener() {
		setOnClickListener {
			onSingleTap?.invoke()
		}
	}

	override fun shouldShow() = true
}