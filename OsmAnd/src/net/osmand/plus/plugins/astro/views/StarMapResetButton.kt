package net.osmand.plus.plugins.astro.views

import android.content.Context
import android.util.AttributeSet
import net.osmand.plus.utils.ColorUtilities

class StarMapResetButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : StarMapButton(context, attrs, defStyleAttr) {

    override fun updateTheme() {
        super.updateTheme()
        setColorFilter(ColorUtilities.getLinksColor(context, nightMode))
    }
}
