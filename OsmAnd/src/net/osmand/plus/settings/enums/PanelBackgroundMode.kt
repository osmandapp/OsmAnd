package net.osmand.plus.settings.enums

import androidx.annotation.StringRes
import net.osmand.plus.R

enum class PanelBackgroundMode(@StringRes val titleId: Int) {

	DEFAULT(R.string.shared_string_default),
	TRANSPARENT(R.string.shared_string_transparent),
	CUSTOM(R.string.shared_string_custom)
}