package net.osmand.plus.plugins.externalsensors.devices.sensors

import android.content.Context
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.plugins.externalsensors.WheelDeviceSettings
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.util.Algorithms
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class DeviceChangeableProperty(val displayNameResId: Int, val inputType: Int) {
	NAME(R.string.shared_string_name, EditorInfo.TYPE_CLASS_TEXT),
	WHEEL_CIRCUMFERENCE(R.string.wheel_circumference, EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);

	fun getFormattedValue(context: Context, value: String?): String {
		val app = context.applicationContext as OsmandApplication
		var res: String
		if (this === WHEEL_CIRCUMFERENCE) {
			var floatValue = WheelDeviceSettings.DEFAULT_WHEEL_CIRCUMFERENCE
			if (value != null && Algorithms.isFloat(value, true)) {
				floatValue = value.replace(",", ".").toFloat()
			}
			if (app.settings.METRIC_SYSTEM.get().shouldUseFeet()) {
				floatValue *= OsmAndFormatter.INCHES_IN_ONE_METER
			}
			res = DecimalFormat("#.####", DecimalFormatSymbols(Locale.US)).format(floatValue.toDouble())
		} else if (value != null) {
			res = value
		} else {
			res = ""
		}
		return res
	}

	fun normalizeValue(context: Context, value: String): String {
		val app = context.applicationContext as OsmandApplication
		var res: String
		if (this === WHEEL_CIRCUMFERENCE && Algorithms.isFloat(value, true)) {
			var floatValue = value.replace(",", ".").toFloat()
			if (app.settings.METRIC_SYSTEM.get().shouldUseFeet()) {
				floatValue /= OsmAndFormatter.INCHES_IN_ONE_METER
			}
			res = DecimalFormat("#.####", DecimalFormatSymbols(Locale.US)).format(floatValue.toDouble())
		} else {
			res = value.trim()
		}
		return res
	}

	@StringRes
	open fun getUnitsResId(context: Context, shortForm: Boolean): Int {
		val app = context.applicationContext as OsmandApplication
		var res = 0
		if (this === WHEEL_CIRCUMFERENCE) {
			res =
				if (app.settings.METRIC_SYSTEM.get().shouldUseFeet())
					(if (shortForm) R.string.inch else R.string.shared_string_inches)
				else (if (shortForm) R.string.m else R.string.shared_string_meters)
		}
		return res
	}
}