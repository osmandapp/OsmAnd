package net.osmand.plus.settings.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.osmand.plus.R

enum class PanelSizeMode(
	@StringRes val titleId: Int,
	@DrawableRes val iconId: Int,
	val widgetSize: WidgetSize?
) {

	ORIGINAL(R.string.shared_string_original, 0, null),
	SMALL(R.string.rendering_value_small_name, R.drawable.ic_action_item_size_s, WidgetSize.SMALL),
	MEDIUM(R.string.rendering_value_medium_w_name, R.drawable.ic_action_item_size_m, WidgetSize.MEDIUM),
	LARGE(R.string.shared_string_large, R.drawable.ic_action_item_size_l, WidgetSize.LARGE)
}
