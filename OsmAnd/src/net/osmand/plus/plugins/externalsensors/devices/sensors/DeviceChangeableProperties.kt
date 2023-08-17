package net.osmand.plus.plugins.externalsensors.devices.sensors

import android.view.inputmethod.EditorInfo
import net.osmand.plus.R

enum class DeviceChangeableProperties(val displayNameResId: Int, val inputType: Int) {
	NAME(R.string.shared_string_name, EditorInfo.TYPE_CLASS_TEXT),
	WHEEL_CIRCUMFERENCE(R.string.wheel_circumference, EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_DECIMAL)
}