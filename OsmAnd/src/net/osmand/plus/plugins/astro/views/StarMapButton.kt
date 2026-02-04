package net.osmand.plus.plugins.astro.views

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import net.osmand.plus.R
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities

open class StarMapButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageButton(context, attrs, defStyleAttr) {

    var nightMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateTheme()
            }
        }

    init {
        scaleType = ScaleType.CENTER
        updateTheme()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateTheme()
    }

    protected open fun updateTheme() {
        if (isInEditMode) {
            setBackgroundResource(R.drawable.btn_circle)
            return
        }

        val backgroundColor = ColorUtilities.getMapButtonBackgroundColor(context, nightMode)
        val backgroundPressedColor = ColorUtilities.getMapButtonBackgroundPressedColor(context, nightMode)
        val strokeWidth = AndroidUtils.dpToPx(context, 1f)

        val normal = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(backgroundColor)
            if (nightMode) {
                setStroke(strokeWidth, ColorUtilities.getColor(context, R.color.map_widget_dark_stroke))
            }
        }

        val pressed = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(backgroundPressedColor)
            setStroke(strokeWidth, ColorUtilities.getColor(context, if (nightMode) R.color.map_widget_dark_stroke else R.color.map_widget_light_pressed))
        }

        background = AndroidUtils.createPressedStateListDrawable(normal, pressed)
        setColorFilter(ColorUtilities.getMapButtonIconColor(context, nightMode))
    }
}
