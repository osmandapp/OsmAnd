package net.osmand.plus.plugins.astronomy.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import net.osmand.plus.utils.ColorUtilities

class StarMapTimeControlButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialButton(context, attrs, defStyleAttr) {

    var active: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateTheme()
            }
        }

    var nightMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateTheme()
            }
        }

    init {
        updateTheme()
    }

    private fun updateTheme() {
        if (isInEditMode) return

        val iconColor = if (active) Color.WHITE else ColorUtilities.getLinksColor(context, nightMode)
        setTextColor(iconColor)
        iconTint = ColorStateList.valueOf(iconColor)
        rippleColor = ColorStateList.valueOf(ColorUtilities.getColorWithAlpha(iconColor, 0.15f))
    }
}
