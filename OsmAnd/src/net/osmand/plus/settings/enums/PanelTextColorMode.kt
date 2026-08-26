package net.osmand.plus.settings.enums

import androidx.annotation.StringRes
import net.osmand.plus.R

enum class PanelTextColorMode(@StringRes val titleId: Int) {

	DEFAULT(R.string.shared_string_default),
	AUTOMATIC(R.string.shared_string_automatic),
	CUSTOM(R.string.shared_string_custom)
}
