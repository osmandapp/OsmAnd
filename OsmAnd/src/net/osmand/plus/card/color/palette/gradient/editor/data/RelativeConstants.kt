package net.osmand.plus.card.color.palette.gradient.editor.data

import net.osmand.shared.util.Localization
import kotlin.math.roundToInt

enum class RelativeConstants(
	val value: Int,
	val shortNameResId: String,
	val longNameResId: String,
	val summaryResId: String
) {
	MIN(
		value = 0,
		shortNameResId = "shared_string_min",
		longNameResId = "shared_string_minimum",
		summaryResId = "relative_gradient_point_min_summary"
	),

	AVERAGE(
		value = 50,
		shortNameResId = "average",
		longNameResId = "average",
		summaryResId = "relative_gradient_point_avg_summary"
	),

	MAX(
		value = 100,
		shortNameResId = "shared_string_max",
		longNameResId = "shared_string_maximum",
		summaryResId = "relative_gradient_point_max_summary"
	);

	fun getName(useFullName: Boolean): String {
		return Localization.getString(if (useFullName) longNameResId else shortNameResId)
	}

	fun getSummary() = Localization.getString(summaryResId)

	companion object {

		fun valueOfRatio(ratio: Float): RelativeConstants? {
			val targetValue = (ratio * 100).roundToInt()
			return entries.find { it.value == targetValue }
		}
	}
}