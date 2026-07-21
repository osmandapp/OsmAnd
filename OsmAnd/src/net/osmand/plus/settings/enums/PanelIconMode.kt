package net.osmand.plus.settings.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.osmand.plus.R

enum class PanelIconMode(
	@StringRes val titleId: Int,
	@DrawableRes val iconId: Int
) {
	ORIGINAL(R.string.shared_string_original, 0),
	ON(R.string.shared_string_on, R.drawable.ic_action_widget_icon_on),
	OFF(R.string.shared_string_off, R.drawable.ic_action_widget_icon_off)
}
